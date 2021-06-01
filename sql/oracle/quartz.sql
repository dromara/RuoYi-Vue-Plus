-- ----------------------------
-- 1、存储每一个已配置的 jobDetail 的详细信息
-- ----------------------------
create table qrtz_job_details (
    sched_name varchar2(120) not null,
    job_name  varchar2(200) not null,
    job_group varchar2(200) not null,
    description varchar2(250) null,
    job_class_name   varchar2(250) not null,
    is_durable varchar2(1) not null,
    is_nonconcurrent varchar2(1) not null,
    is_update_data varchar2(1) not null,
    requests_recovery varchar2(1) not null,
    job_data blob null,
    constraint qrtz_job_details_pk primary key (sched_name,job_name,job_group)
);

-- ----------------------------
-- 2、 存储已配置的 Trigger 的信息
-- ----------------------------
create table qrtz_triggers (
    sched_name varchar2(120) not null,
    trigger_name varchar2(200) not null,
    trigger_group varchar2(200) not null,
    job_name  varchar2(200) not null,
    job_group varchar2(200) not null,
    description varchar2(250) null,
    next_fire_time number(13) null,
    prev_fire_time number(13) null,
    priority number(13) null,
    trigger_state varchar2(16) not null,
    trigger_type varchar2(8) not null,
    start_time number(13) not null,
    end_time number(13) null,
    calendar_name varchar2(200) null,
    misfire_instr number(2) null,
    job_data blob null,
    constraint qrtz_triggers_pk primary key (sched_name,trigger_name,trigger_group),
    constraint qrtz_trigger_to_jobs_fk foreign key (sched_name,job_name,job_group)
      references qrtz_job_details(sched_name,job_name,job_group)
);

-- ----------------------------
-- 3、 存储简单的 Trigger，包括重复次数，间隔，以及已触发的次数
-- ----------------------------
create table qrtz_simple_triggers (
    sched_name varchar2(120) not null,
    trigger_name varchar2(200) not null,
    trigger_group varchar2(200) not null,
    repeat_count number(7) not null,
    repeat_interval number(12) not null,
    times_triggered number(10) not null,
    constraint qrtz_simple_trig_pk primary key (sched_name,trigger_name,trigger_group),
    constraint qrtz_simple_trig_to_trig_fk foreign key (sched_name,trigger_name,trigger_group)
      references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

-- ----------------------------
-- 4、 存储 Cron Trigger，包括 Cron 表达式和时区信息
-- ----------------------------
create table qrtz_cron_triggers (
    sched_name varchar2(120) not null,
    trigger_name varchar2(200) not null,
    trigger_group varchar2(200) not null,
    cron_expression varchar2(120) not null,
    time_zone_id varchar2(80),
    constraint qrtz_cron_trig_pk primary key (sched_name,trigger_name,trigger_group),
    constraint qrtz_cron_trig_to_trig_fk foreign key (sched_name,trigger_name,trigger_group)
      references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

-- ----------------------------
-- 5、 Trigger 作为 Blob 类型存储(用于 Quartz 用户用 JDBC 创建他们自己定制的 Trigger 类型，JobStore 并不知道如何存储实例的时候)
-- ----------------------------
create table qrtz_blob_triggers (
    sched_name varchar2(120) not null,
    trigger_name varchar2(200) not null,
    trigger_group varchar2(200) not null,
    blob_data blob null,
    constraint qrtz_blob_trig_pk primary key (sched_name,trigger_name,trigger_group),
    constraint qrtz_blob_trig_to_trig_fk foreign key (sched_name,trigger_name,trigger_group)
      references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

-- ----------------------------
-- 6、 以 Blob 类型存储存放日历信息， quartz可配置一个日历来指定一个时间范围
-- ----------------------------
create table qrtz_calendars (
    sched_name varchar2(120) not null,
    calendar_name  varchar2(200) not null,
    calendar blob not null,
    constraint qrtz_calendars_pk primary key (sched_name,calendar_name)
);

-- ----------------------------
-- 7、 存储已暂停的 Trigger 组的信息
-- ----------------------------
create table qrtz_paused_trigger_grps (
    sched_name varchar2(120) not null,
    trigger_group  varchar2(200) not null,
    constraint qrtz_paused_trig_grps_pk primary key (sched_name,trigger_group)
);

-- ----------------------------
-- 8、 存储与已触发的 Trigger 相关的状态信息，以及相联 Job 的执行信息
-- ----------------------------
create table qrtz_fired_triggers (
    sched_name varchar2(120) not null,
    entry_id varchar2(95) not null,
    trigger_name varchar2(200) not null,
    trigger_group varchar2(200) not null,
    instance_name varchar2(200) not null,
    fired_time number(13) not null,
    sched_time number(13) not null,
    priority number(13) not null,
    state varchar2(16) not null,
    job_name varchar2(200) null,
    job_group varchar2(200) null,
    is_nonconcurrent varchar2(1) null,
    requests_recovery varchar2(1) null,
    constraint qrtz_fired_trigger_pk primary key (sched_name,entry_id)
);

-- ----------------------------
-- 9、 存储少量的有关 Scheduler 的状态信息，假如是用于集群中，可以看到其他的 Scheduler 实例
-- ----------------------------
create table qrtz_scheduler_state (
    sched_name varchar2(120) not null,
    instance_name varchar2(200) not null,
    last_checkin_time number(13) not null,
    checkin_interval number(13) not null,
    constraint qrtz_scheduler_state_pk primary key (sched_name,instance_name)
);

-- ----------------------------
-- 10、 存储程序的悲观锁的信息(假如使用了悲观锁)
-- ----------------------------
create table qrtz_locks (
    sched_name varchar2(120) not null,
    lock_name  varchar2(40) not null,
    constraint qrtz_locks_pk primary key (sched_name,lock_name)
);

create table qrtz_simprop_triggers (
    sched_name varchar2(120) not null,
    trigger_name varchar2(200) not null,
    trigger_group varchar2(200) not null,
    str_prop_1 varchar2(512) null,
    str_prop_2 varchar2(512) null,
    str_prop_3 varchar2(512) null,
    int_prop_1 number(10) null,
    int_prop_2 number(10) null,
    long_prop_1 number(13) null,
    long_prop_2 number(13) null,
    dec_prop_1 numeric(13,4) null,
    dec_prop_2 numeric(13,4) null,
    bool_prop_1 varchar2(1) null,
    bool_prop_2 varchar2(1) null,
    constraint qrtz_simprop_trig_pk primary key (sched_name,trigger_name,trigger_group),
    constraint qrtz_simprop_trig_to_trig_fk foreign key (sched_name,trigger_name,trigger_group)
      references qrtz_triggers(sched_name,trigger_name,trigger_group)
);


create index idx_qrtz_j_req_recovery on qrtz_job_details(sched_name,requests_recovery);
create index idx_qrtz_j_grp on qrtz_job_details(sched_name,job_group);

create index idx_qrtz_t_j on qrtz_triggers(sched_name,job_name,job_group);
create index idx_qrtz_t_jg on qrtz_triggers(sched_name,job_group);
create index idx_qrtz_t_c on qrtz_triggers(sched_name,calendar_name);
create index idx_qrtz_t_g on qrtz_triggers(sched_name,trigger_group);
create index idx_qrtz_t_state on qrtz_triggers(sched_name,trigger_state);
create index idx_qrtz_t_n_state on qrtz_triggers(sched_name,trigger_name,trigger_group,trigger_state);
create index idx_qrtz_t_n_g_state on qrtz_triggers(sched_name,trigger_group,trigger_state);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(sched_name,next_fire_time);
create index idx_qrtz_t_nft_st on qrtz_triggers(sched_name,trigger_state,next_fire_time);
create index idx_qrtz_t_nft_misfire on qrtz_triggers(sched_name,misfire_instr,next_fire_time);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(sched_name,misfire_instr,next_fire_time,trigger_state);
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(sched_name,misfire_instr,next_fire_time,trigger_group,trigger_state);

create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(sched_name,instance_name);
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(sched_name,instance_name,requests_recovery);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(sched_name,job_name,job_group);
create index idx_qrtz_ft_jg on qrtz_fired_triggers(sched_name,job_group);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(sched_name,trigger_name,trigger_group);

create index idx_qrtz_ft_tg on qrtz_fired_triggers(sched_name,trigger_group);

commit;
