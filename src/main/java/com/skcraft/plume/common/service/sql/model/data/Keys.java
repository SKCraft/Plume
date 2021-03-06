/**
 * This class is generated by jOOQ
 */
package com.skcraft.plume.common.service.sql.model.data;


import com.skcraft.plume.common.service.sql.model.data.tables.Ban;
import com.skcraft.plume.common.service.sql.model.data.tables.Claim;
import com.skcraft.plume.common.service.sql.model.data.tables.Group;
import com.skcraft.plume.common.service.sql.model.data.tables.GroupParent;
import com.skcraft.plume.common.service.sql.model.data.tables.Party;
import com.skcraft.plume.common.service.sql.model.data.tables.PartyMember;
import com.skcraft.plume.common.service.sql.model.data.tables.User;
import com.skcraft.plume.common.service.sql.model.data.tables.UserGroup;
import com.skcraft.plume.common.service.sql.model.data.tables.UserId;
import com.skcraft.plume.common.service.sql.model.data.tables.records.BanRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.ClaimRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.GroupParentRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.GroupRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.PartyMemberRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.PartyRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.UserGroupRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.UserIdRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.UserRecord;

import javax.annotation.Generated;

import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code>data</code> 
 * schema
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.6.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

	// -------------------------------------------------------------------------
	// IDENTITY definitions
	// -------------------------------------------------------------------------

	public static final Identity<BanRecord, Integer> IDENTITY_BAN = Identities0.IDENTITY_BAN;
	public static final Identity<GroupRecord, Integer> IDENTITY_GROUP = Identities0.IDENTITY_GROUP;
	public static final Identity<UserIdRecord, Integer> IDENTITY_USER_ID = Identities0.IDENTITY_USER_ID;

	// -------------------------------------------------------------------------
	// UNIQUE and PRIMARY KEY definitions
	// -------------------------------------------------------------------------

	public static final UniqueKey<BanRecord> KEY_BAN_PRIMARY = UniqueKeys0.KEY_BAN_PRIMARY;
	public static final UniqueKey<ClaimRecord> KEY_CLAIM_PRIMARY = UniqueKeys0.KEY_CLAIM_PRIMARY;
	public static final UniqueKey<GroupRecord> KEY_GROUP_PRIMARY = UniqueKeys0.KEY_GROUP_PRIMARY;
	public static final UniqueKey<GroupRecord> KEY_GROUP_NAME = UniqueKeys0.KEY_GROUP_NAME;
	public static final UniqueKey<GroupParentRecord> KEY_GROUP_PARENT_PRIMARY = UniqueKeys0.KEY_GROUP_PARENT_PRIMARY;
	public static final UniqueKey<PartyRecord> KEY_PARTY_PRIMARY = UniqueKeys0.KEY_PARTY_PRIMARY;
	public static final UniqueKey<PartyMemberRecord> KEY_PARTY_MEMBER_PRIMARY = UniqueKeys0.KEY_PARTY_MEMBER_PRIMARY;
	public static final UniqueKey<UserRecord> KEY_USER_PRIMARY = UniqueKeys0.KEY_USER_PRIMARY;
	public static final UniqueKey<UserGroupRecord> KEY_USER_GROUP_PRIMARY = UniqueKeys0.KEY_USER_GROUP_PRIMARY;
	public static final UniqueKey<UserIdRecord> KEY_USER_ID_PRIMARY = UniqueKeys0.KEY_USER_ID_PRIMARY;
	public static final UniqueKey<UserIdRecord> KEY_USER_ID_UUID = UniqueKeys0.KEY_USER_ID_UUID;

	// -------------------------------------------------------------------------
	// FOREIGN KEY definitions
	// -------------------------------------------------------------------------

	public static final ForeignKey<BanRecord, UserIdRecord> FK_BAN_USER_ID = ForeignKeys0.FK_BAN_USER_ID;
	public static final ForeignKey<BanRecord, UserIdRecord> FK_BAN_ISSUE_BY = ForeignKeys0.FK_BAN_ISSUE_BY;
	public static final ForeignKey<BanRecord, UserIdRecord> FK_BAN_PARDON_BY = ForeignKeys0.FK_BAN_PARDON_BY;
	public static final ForeignKey<ClaimRecord, UserIdRecord> FK_CLAIM_OWNER_ID = ForeignKeys0.FK_CLAIM_OWNER_ID;
	public static final ForeignKey<ClaimRecord, PartyRecord> FK_CLAIM_PARTY_NAME = ForeignKeys0.FK_CLAIM_PARTY_NAME;
	public static final ForeignKey<GroupParentRecord, GroupRecord> FK_GROUP_PARENT_PARENT_ID = ForeignKeys0.FK_GROUP_PARENT_PARENT_ID;
	public static final ForeignKey<GroupParentRecord, GroupRecord> FK_GROUP_PARENT_GROUP_ID = ForeignKeys0.FK_GROUP_PARENT_GROUP_ID;
	public static final ForeignKey<PartyMemberRecord, PartyRecord> FK_PARTY_MEMBER_PARTY_NAME = ForeignKeys0.FK_PARTY_MEMBER_PARTY_NAME;
	public static final ForeignKey<PartyMemberRecord, UserIdRecord> FK_PARTY_MEMBER_USER_ID = ForeignKeys0.FK_PARTY_MEMBER_USER_ID;
	public static final ForeignKey<UserRecord, UserIdRecord> FK_USER_USER_ID = ForeignKeys0.FK_USER_USER_ID;
	public static final ForeignKey<UserRecord, UserIdRecord> FK_USER_REFERRER_ID = ForeignKeys0.FK_USER_REFERRER_ID;
	public static final ForeignKey<UserGroupRecord, UserIdRecord> FK_USER_GROUP_USER_ID = ForeignKeys0.FK_USER_GROUP_USER_ID;
	public static final ForeignKey<UserGroupRecord, GroupRecord> FK_USER_GROUP_GROUP_ID = ForeignKeys0.FK_USER_GROUP_GROUP_ID;

	// -------------------------------------------------------------------------
	// [#1459] distribute members to avoid static initialisers > 64kb
	// -------------------------------------------------------------------------

	private static class Identities0 extends AbstractKeys {
		public static Identity<BanRecord, Integer> IDENTITY_BAN = createIdentity(Ban.BAN, Ban.BAN.ID);
		public static Identity<GroupRecord, Integer> IDENTITY_GROUP = createIdentity(Group.GROUP, Group.GROUP.ID);
		public static Identity<UserIdRecord, Integer> IDENTITY_USER_ID = createIdentity(UserId.USER_ID, UserId.USER_ID.ID);
	}

	private static class UniqueKeys0 extends AbstractKeys {
		public static final UniqueKey<BanRecord> KEY_BAN_PRIMARY = createUniqueKey(Ban.BAN, Ban.BAN.ID);
		public static final UniqueKey<ClaimRecord> KEY_CLAIM_PRIMARY = createUniqueKey(Claim.CLAIM, Claim.CLAIM.SERVER, Claim.CLAIM.WORLD, Claim.CLAIM.X, Claim.CLAIM.Z);
		public static final UniqueKey<GroupRecord> KEY_GROUP_PRIMARY = createUniqueKey(Group.GROUP, Group.GROUP.ID);
		public static final UniqueKey<GroupRecord> KEY_GROUP_NAME = createUniqueKey(Group.GROUP, Group.GROUP.NAME);
		public static final UniqueKey<GroupParentRecord> KEY_GROUP_PARENT_PRIMARY = createUniqueKey(GroupParent.GROUP_PARENT, GroupParent.GROUP_PARENT.PARENT_ID, GroupParent.GROUP_PARENT.GROUP_ID);
		public static final UniqueKey<PartyRecord> KEY_PARTY_PRIMARY = createUniqueKey(Party.PARTY, Party.PARTY.NAME);
		public static final UniqueKey<PartyMemberRecord> KEY_PARTY_MEMBER_PRIMARY = createUniqueKey(PartyMember.PARTY_MEMBER, PartyMember.PARTY_MEMBER.PARTY_NAME, PartyMember.PARTY_MEMBER.USER_ID);
		public static final UniqueKey<UserRecord> KEY_USER_PRIMARY = createUniqueKey(User.USER, User.USER.USER_ID);
		public static final UniqueKey<UserGroupRecord> KEY_USER_GROUP_PRIMARY = createUniqueKey(UserGroup.USER_GROUP, UserGroup.USER_GROUP.USER_ID, UserGroup.USER_GROUP.GROUP_ID);
		public static final UniqueKey<UserIdRecord> KEY_USER_ID_PRIMARY = createUniqueKey(UserId.USER_ID, UserId.USER_ID.ID);
		public static final UniqueKey<UserIdRecord> KEY_USER_ID_UUID = createUniqueKey(UserId.USER_ID, UserId.USER_ID.UUID);
	}

	private static class ForeignKeys0 extends AbstractKeys {
		public static final ForeignKey<BanRecord, UserIdRecord> FK_BAN_USER_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, Ban.BAN, Ban.BAN.USER_ID);
		public static final ForeignKey<BanRecord, UserIdRecord> FK_BAN_ISSUE_BY = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, Ban.BAN, Ban.BAN.ISSUE_BY);
		public static final ForeignKey<BanRecord, UserIdRecord> FK_BAN_PARDON_BY = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, Ban.BAN, Ban.BAN.PARDON_BY);
		public static final ForeignKey<ClaimRecord, UserIdRecord> FK_CLAIM_OWNER_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, Claim.CLAIM, Claim.CLAIM.OWNER_ID);
		public static final ForeignKey<ClaimRecord, PartyRecord> FK_CLAIM_PARTY_NAME = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_PARTY_PRIMARY, Claim.CLAIM, Claim.CLAIM.PARTY_NAME);
		public static final ForeignKey<GroupParentRecord, GroupRecord> FK_GROUP_PARENT_PARENT_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_GROUP_PRIMARY, GroupParent.GROUP_PARENT, GroupParent.GROUP_PARENT.PARENT_ID);
		public static final ForeignKey<GroupParentRecord, GroupRecord> FK_GROUP_PARENT_GROUP_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_GROUP_PRIMARY, GroupParent.GROUP_PARENT, GroupParent.GROUP_PARENT.GROUP_ID);
		public static final ForeignKey<PartyMemberRecord, PartyRecord> FK_PARTY_MEMBER_PARTY_NAME = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_PARTY_PRIMARY, PartyMember.PARTY_MEMBER, PartyMember.PARTY_MEMBER.PARTY_NAME);
		public static final ForeignKey<PartyMemberRecord, UserIdRecord> FK_PARTY_MEMBER_USER_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, PartyMember.PARTY_MEMBER, PartyMember.PARTY_MEMBER.USER_ID);
		public static final ForeignKey<UserRecord, UserIdRecord> FK_USER_USER_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, User.USER, User.USER.USER_ID);
		public static final ForeignKey<UserRecord, UserIdRecord> FK_USER_REFERRER_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, User.USER, User.USER.REFERRER_ID);
		public static final ForeignKey<UserGroupRecord, UserIdRecord> FK_USER_GROUP_USER_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_USER_ID_PRIMARY, UserGroup.USER_GROUP, UserGroup.USER_GROUP.USER_ID);
		public static final ForeignKey<UserGroupRecord, GroupRecord> FK_USER_GROUP_GROUP_ID = createForeignKey(com.skcraft.plume.common.service.sql.model.data.Keys.KEY_GROUP_PRIMARY, UserGroup.USER_GROUP, UserGroup.USER_GROUP.GROUP_ID);
	}
}
