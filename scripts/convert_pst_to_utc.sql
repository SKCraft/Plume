update user set last_online = CONVERT_TZ(last_online, '-08:00', '+0:00'), join_date = CONVERT_TZ(join_date, '-08:00', '+0:00');
update ban set issue_time = CONVERT_TZ(issue_time, '-08:00', '+0:00'), expire_time = CONVERT_TZ(expire_time, '-08:00', '+0:00');
update claim set issue_time = CONVERT_TZ(issue_time, '-08:00', '+0:00');
update party set create_time = CONVERT_TZ(create_time, '-08:00', '+0:00');