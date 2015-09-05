import time, collections, json, requests, re
import MySQLdb, _mysql_exceptions
import logging
import configparser
import argparse
import os.path
from functools import wraps

log = logging.getLogger(__name__)


class NoSuchMojangUser(Exception):
    """Raised when a user can't be found for a given UUID."""


class UserIdCache:
    def __init__(self, db):
        self.db = db
        self.name_to_uuid = {}
        self.uuid_to_id = {}
        self.missing_names = {}

        log.info("Loading existing converted user IDs...")
        with self.db.cursor(MySQLdb.cursors.DictCursor) as cur:
            cur.execute("SELECT * FROM `user_id`")
            for i in range(cur.rowcount):
                row = cur.fetchone()
                self.name_to_uuid[row['name'].lower()] = row['uuid']
                self.uuid_to_id[row['uuid']] = row['id']

        if os.path.exists("missing_names.txt"):
            with open("missing_names.txt", "rb") as f:
                for line in f.readlines():
                    self.missing_names[line.decode("utf-8").strip().lower()] = True

        self.missing_names_fp = open("missing_names.txt", "a")

    def close(self):
        self.missing_names_fp.close()

    def get_uuid(self, name, t=0):
        if name.lower() in self.name_to_uuid:
            return self.name_to_uuid[name.lower()]
        elif name.lower() in self.missing_names:
            raise NoSuchMojangUser()
        else:
            # Invalid names
            if not re.match("^[A-Za-z0-9_]+$", name):
                raise NoSuchMojangUser()

            try:
                uuid = fetch_uuid_for_name(name, t)
                self.name_to_uuid[name.lower()] = uuid
                return uuid
            except NoSuchMojangUser:
                self.missing_names_fp.write(name.lower() + "\n")
                self.missing_names_fp.flush()
                raise

    def get_or_create(self, name, uuid=None, t=0):
        if name is None: raise ValueError("null name")
        if not uuid:  # get a uuid if one is not known
            uuid = self.get_uuid(name, t)
        else:
            if not "-" in uuid: raise ValueError("uuid needs dashes")
            self.name_to_uuid[name.lower()] = uuid

        if uuid in self.uuid_to_id:
            return self.uuid_to_id[uuid]
        else:
            with self.db.cursor() as ins_cur:
                ins_cur.execute("INSERT INTO `user_id` (`uuid`, `name`) VALUES (%s, %s)", (uuid, name))
                cache_id = ins_cur.lastrowid
                self.uuid_to_id[uuid] = cache_id
                log.info("Stored user_id for {0} (id={1})".format(name, cache_id))
            return cache_id


def retry(exception, tries=4, delay=3, backoff=2, logger=None):
    """Retry calling the decorated function using an exponential backoff.

    http://www.saltycrane.com/blog/2009/11/trying-out-retry-decorator-python/
    original from: http://wiki.python.org/moin/PythonDecoratorLibrary#Retry

    :param exception: the exception to check. may be a tuple of
        exceptions to check
    :type exception: Exception or tuple
    :param tries: number of times to try (not retry) before giving up
    :type tries: int
    :param delay: initial delay between retries in seconds
    :type delay: int
    :param backoff: backoff multiplier e.g. value of 2 will double the delay
        each retry
    :type backoff: int
    :param logger: logger to use. If None, print
    :type logger: logging.Logger instance
    """
    def deco_retry(f):

        @wraps(f)
        def f_retry(*args, **kwargs):
            mtries, mdelay = tries, delay
            while mtries > 1:
                try:
                    return f(*args, **kwargs)
                except exception as e:
                    msg = "{0}, Retrying in {1} seconds...".format(str(e), mdelay)
                    if logger:
                        logger.warning(msg)
                    else:
                        print(msg)
                    time.sleep(mdelay)
                    mtries -= 1
                    mdelay *= backoff
            return f(*args, **kwargs)

        return f_retry  # true decorator

    return deco_retry


def rate_limit(max_per_period, period=60):
    """
    Rate limits a function call, sleeping until the function can be called.
    Not thread safe.
    """
    def decorator(func):
        recent = collections.deque(maxlen=max_per_period)

        def wrapped(*args, **kwargs):
            now = time.time()
            active = list(filter(lambda t: t >= now - period, recent))
            if len(active) >= max_per_period:
                delay = period - (now - active[0])
                log.info("Sleeping for " + str(delay) + " seconds")
                time.sleep(delay)
            recent.append(time.time())
            return func(*args, **kwargs)

        return wrapped

    return decorator


@retry(exception=KeyError, tries=4, delay=60, backoff=2, logger=log)
@rate_limit(25, period=30)
def fetch_uuid_for_name(username, t=0):
    """Attempt to grab the UUID for a name at T time, otherwise direct UUID to name,
    otherwise throw NoSuchMojangUser()
    """
    log.debug("Fetching UUID for {0} from Mojang at t={1}".format(username, t))
    response = requests.get("https://api.mojang.com/users/profiles/minecraft/" + str(username),
                            params={'at': str(t)})
    if response.text == "":
        profiles = requests.post("https://api.mojang.com/profiles/minecraft",
                                 data=json.dumps([username]),
                                 headers={'Content-Type': 'application/json'}).json()
        if len(profiles):
            return add_uuid_dashes(profiles[0]['id'])
        else:
            raise NoSuchMojangUser()
    else:
        return add_uuid_dashes(response.json()['id'])


def add_uuid_dashes(uuid):
    """Convert a plain UUID (i.e. "65ea51744ce54bee...") to a dashed
    UUID (i.e. 65ea5174-4ce5-4bee-...)
    """
    if uuid is None:
        return None
    else:
        p = re.compile(r"(\w{8})(\w{4})(\w{4})(\w{4})(\w{12})")
        return p.sub(r"\1-\2-\3-\4-\5", uuid)


def connect_db(c, section):
    return MySQLdb.connect(c.get(section, "host", fallback="localhost"),
                           c.get(section, "user"),
                           c.get(section, "pass"),
                           c.get(section, "database"),
                           port=c.getint(section, "port", fallback=3306))


def convert(old, new):
    old_cur = old.cursor(MySQLdb.cursors.DictCursor)
    new_cur = new.cursor()

    def old_row_to_user_id(name, uuid=None, time=None):
        if not name:
            return None
        try:
            return user_ids.get_or_create(name, add_uuid_dashes(uuid), int(time.timestamp()) if time else 0)
        except NoSuchMojangUser:
            log.warning("Can't find user with name '{0}'".format(name))

    # ######################################################
    # Clear data
    # ######################################################

    # Reset all the new tables just in case
    log.info("Removing existing data from target database...")
    new_cur.execute("SET foreign_key_checks = 0")
    new_cur.execute("TRUNCATE TABLE `claim`")
    new_cur.execute("TRUNCATE TABLE `party_member`")
    new_cur.execute("TRUNCATE TABLE `party`")
    new_cur.execute("TRUNCATE TABLE `ban`")
    new_cur.execute("TRUNCATE TABLE `user`")
    new_cur.execute("TRUNCATE TABLE `group_parent`")
    new_cur.execute("TRUNCATE TABLE `group`")
    new_cur.execute("TRUNCATE TABLE `user_group`")
    new_cur.execute("SET foreign_key_checks = 1")

    # ######################################################
    # Populate the user_id table and ID cache
    # ######################################################

    old_cur.execute("SELECT * FROM `users` WHERE expiration_date IS NULL ORDER BY `join_date` ASC")
    users = old_cur.fetchall()

    log.info("Building user_id table of name <-> uuid...")
    for user in users:
        old_row_to_user_id(user["name"], user["uuid"], user["last_online"])

    # ######################################################
    # Convert users
    # ######################################################

    log.info("Converting users...")
    for user in users:
        if user["alt_login"]: continue
        if user["expiration_date"]: continue

        user_id = old_row_to_user_id(user["name"], user["uuid"], user["last_online"])
        referrer_id = old_row_to_user_id(user["invited_by_name"], time=user["join_date"])

        if user_id:
            try:
                new_cur.execute("INSERT INTO `user` VALUES (%s, %s, %s, %s, %s)",
                            (user_id, referrer_id, user["join_date"], user["last_online"], user["host_key"]))
                log.debug("Converted user {0}".format(user['name']))
            except _mysql_exceptions.IntegrityError as e:
                log.warn("More than one row found for '{0}', most likely due to a name change and an invite of the new name".format(user['name']))

    # ######################################################
    # Convert groups
    # ######################################################

    group_ids = {}

    old_cur.execute("SELECT * FROM `groups`")
    groups = old_cur.fetchall()

    log.info("Converting groups...")
    for group in groups:
        new_cur.execute("INSERT INTO `group` (`name`, `permissions`, `auto_join`) VALUES (%s, %s, %s)",
                        (group['name'], group['permissions'], 1 if group['name'] == 'default' else 0))
        group_ids[group['name']] = new_cur.lastrowid

    old_cur.execute("SELECT * FROM `groups_parents`")
    parents = old_cur.fetchall()

    log.info("Converting group parents...")
    for parent in parents:
        group_id = group_ids[parent['groupName']]
        parent_id = group_ids[parent['parentGroupName']]

        new_cur.execute("INSERT INTO `group_parent` (`parent_id`, `group_id`) VALUES (%s, %s)",
                        (parent_id, group_id))

    # ######################################################
    # Convert group memberships
    # ######################################################

    old_cur.execute("SELECT * FROM `users_groups`")
    members = old_cur.fetchall()

    log.info("Converting group membership...")
    for member in members:
        user_id = old_row_to_user_id(member["userName"])
        if user_id:
            try:
                new_cur.execute("INSERT INTO `user_group` (`user_id`, `group_id`) VALUES (%s, %s)",
                                (user_id, group_ids[member['groupName']]))
            except _mysql_exceptions.IntegrityError as e:
                log.warn("More than one row found for '{0}', most likely due to a name change and an invite of the new name".format(member['userName']))

    # ######################################################
    # Convert bans
    # ######################################################

    old_cur.execute("SELECT * FROM `bans`")
    bans = old_cur.fetchall()

    # Convert bans
    # TODO make this work if someone changed their name BEFORE ban
    log.info("Converting bans...")
    for ban in bans:
        user_id = old_row_to_user_id(ban["name"], time=ban["issue_time"])
        issuer_id = old_row_to_user_id(ban["issued_by"], time=ban["issue_time"])
        pardoner_id = old_row_to_user_id(ban["pardoned_by"], time=ban["issue_time"])

        if user_id:
            new_cur.execute("INSERT INTO `ban` VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                            (ban["id"], user_id, ban["issue_time"], issuer_id, ban["source_server"], ban["reason"],
                             ban["heuristic"], ban["expire_time"], pardoner_id, ban["pardon_reason"]))
            log.debug("Converted ban for user_id {0} ({1}) (id={2})".format(user_id, ban["name"], new_cur.lastrowid))

    # ######################################################
    # Convert friend lists -> parties
    # ######################################################

    # Lists first
    old_cur.execute("SELECT * FROM `friend_lists`")
    friend_lists = old_cur.fetchall()

    log.info("Converting friend lists...")
    for friend_list in friend_lists:
        new_cur.execute("INSERT INTO `party` VALUES (%s, %s)", (friend_list["id"], friend_list["created_date"]))
        log.debug("Added party '{0}'".format(friend_list["id"]))

    # Then friends
    old_cur.execute("SELECT * FROM `friends`")
    friends = old_cur.fetchall()

    log.info("Converting friends...")
    for friend in friends:
        user_id = old_row_to_user_id(friend["name"])
        if user_id:
            try:
                new_cur.execute("INSERT INTO `party_member` VALUES (%s, %s, %s)",
                                (friend["list"], user_id, friend["rank"]))
                log.debug("Converted party member {0} for party {1})".format(user_id, friend["list"]))
            except _mysql_exceptions.IntegrityError as e:
                log.warn("More than one row found for '{0}', most likely due to a name change and an invite of the new name".format(friend['name']))

    # ######################################################
    # Convert claims
    # ######################################################

    old_cur.execute("SELECT * FROM `chunk_owners` WHERE `active` = 1")
    claims = old_cur.fetchall()

    log.info("Converting claims...")
    for claim in claims:
        owner_id = old_row_to_user_id(claim["owner"], time=claim["issue_time"])
        if owner_id:
            new_cur.execute("INSERT INTO `claim` VALUES (%s, %s, %s, %s, %s, %s, %s)",
                            (claim["server"], claim["world"], claim["x"], claim["z"],
                             owner_id, claim["friends_list_id"], claim["issue_time"]))
            log.debug("Converted claim at ({0}, {1}) for user {2})".format(claim["x"], claim["z"], claim['owner']))

    # ######################################################

    log.warning("Complete!")
    old.close()
    new.close()


if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, format="[%(levelname)s] %(message)s")  # Log configuration
    logging.getLogger("requests").setLevel(logging.WARNING) # Don't show messages from requests

    parser = argparse.ArgumentParser(description='Convert the old database tables to the Plume ones')
    parser.add_argument('config', help='the config file')
    args = parser.parse_args()

    config = configparser.ConfigParser()
    config.read(args.config)

    old = connect_db(config, "from")
    new = connect_db(config, "to")
    new.autocommit(True)

    user_ids = UserIdCache(new)  # ID Cache object
    try:
        convert(old, new)
    finally:
        user_ids.close()

