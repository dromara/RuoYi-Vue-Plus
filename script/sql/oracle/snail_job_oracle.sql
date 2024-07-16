/*
 SnailJob Database Transfer Tool
 Source Server Type    : MySQL
 Target Server Type    : Oracle
 Date: 2024-07-06 12:49:36
*/


-- sj_namespace
CREATE TABLE sj_namespace
(
    id          number GENERATED ALWAYS AS IDENTITY,
    name        varchar2(64)                            NULL,
    unique_id   varchar2(64)                            NULL,
    description varchar2(256) DEFAULT ''                NULL,
    deleted     smallint      DEFAULT 0                 NOT NULL,
    create_dt   date          DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_dt   date          DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE sj_namespace
    ADD CONSTRAINT pk_sj_namespace PRIMARY KEY (id);

CREATE INDEX idx_sj_namespace_01 ON sj_namespace (name);

COMMENT ON COLUMN sj_namespace.id IS '主键';
COMMENT ON COLUMN sj_namespace.name IS '名称';
COMMENT ON COLUMN sj_namespace.unique_id IS '唯一id';
COMMENT ON COLUMN sj_namespace.description IS '描述';
COMMENT ON COLUMN sj_namespace.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_namespace.create_dt IS '创建时间';
COMMENT ON COLUMN sj_namespace.update_dt IS '修改时间';
COMMENT ON TABLE sj_namespace IS '命名空间';

INSERT INTO sj_namespace(name, unique_id, description, deleted, create_dt, update_dt) VALUES ('Development', 'dev', '', 0, sysdate, sysdate);
INSERT INTO sj_namespace(name, unique_id, description, deleted, create_dt, update_dt) VALUES ('Production', 'prod', '', 0, sysdate, sysdate);

-- sj_group_config
CREATE TABLE sj_group_config
(
    id                number GENERATED ALWAYS AS IDENTITY,
    namespace_id      varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a'    NULL,
    group_name        varchar2(64)  DEFAULT ''                                    NULL,
    description       varchar2(256) DEFAULT ''                                    NULL,
    token             varchar2(64)  DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT' NULL,
    group_status      smallint      DEFAULT 0                                     NOT NULL,
    version           number                                                      NOT NULL,
    group_partition   number                                                      NOT NULL,
    id_generator_mode smallint      DEFAULT 1                                     NOT NULL,
    init_scene        smallint      DEFAULT 0                                     NOT NULL,
    bucket_index      number        DEFAULT 0                                     NOT NULL,
    create_dt         date          DEFAULT CURRENT_TIMESTAMP                     NOT NULL,
    update_dt         date          DEFAULT CURRENT_TIMESTAMP                     NOT NULL
);

ALTER TABLE sj_group_config
    ADD CONSTRAINT pk_sj_group_config PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_group_config_01 ON sj_group_config (namespace_id, group_name);

COMMENT ON COLUMN sj_group_config.id IS '主键';
COMMENT ON COLUMN sj_group_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_group_config.group_name IS '组名称';
COMMENT ON COLUMN sj_group_config.description IS '组描述';
COMMENT ON COLUMN sj_group_config.token IS 'token';
COMMENT ON COLUMN sj_group_config.group_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_group_config.version IS '版本号';
COMMENT ON COLUMN sj_group_config.group_partition IS '分区';
COMMENT ON COLUMN sj_group_config.id_generator_mode IS '唯一id生成模式 默认号段模式';
COMMENT ON COLUMN sj_group_config.init_scene IS '是否初始化场景 0:否 1:是';
COMMENT ON COLUMN sj_group_config.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_group_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_group_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_group_config IS '组配置';

INSERT INTO sj_group_config (namespace_id, group_name, description, token, group_status, version, group_partition, id_generator_mode, init_scene, bucket_index, create_dt, update_dt) VALUES ('dev', 'ruoyi_group', '', 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, 4, sysdate, sysdate);

-- sj_notify_config
CREATE TABLE sj_notify_config
(
    id                     number GENERATED ALWAYS AS IDENTITY,
    namespace_id           varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name             varchar2(64)                                             NULL,
    business_id            varchar2(64)                                             NULL,
    system_task_type       smallint      DEFAULT 3                                  NOT NULL,
    notify_status          smallint      DEFAULT 0                                  NOT NULL,
    recipient_ids          varchar2(128)                                            NULL,
    notify_threshold       number        DEFAULT 0                                  NOT NULL,
    notify_scene           smallint      DEFAULT 0                                  NOT NULL,
    rate_limiter_status    smallint      DEFAULT 0                                  NOT NULL,
    rate_limiter_threshold number        DEFAULT 0                                  NOT NULL,
    description            varchar2(256) DEFAULT ''                                 NULL,
    create_dt              date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt              date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_notify_config
    ADD CONSTRAINT pk_sj_notify_config PRIMARY KEY (id);

CREATE INDEX idx_sj_notify_config_01 ON sj_notify_config (namespace_id, group_name, business_id);

COMMENT ON COLUMN sj_notify_config.id IS '主键';
COMMENT ON COLUMN sj_notify_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_config.group_name IS '组名称';
COMMENT ON COLUMN sj_notify_config.business_id IS '业务id  ( job_id或workflow_id或scene_name ) ';
COMMENT ON COLUMN sj_notify_config.system_task_type IS '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_notify_config.notify_status IS '通知状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.recipient_ids IS '接收人id列表';
COMMENT ON COLUMN sj_notify_config.notify_threshold IS '通知阈值';
COMMENT ON COLUMN sj_notify_config.notify_scene IS '通知场景';
COMMENT ON COLUMN sj_notify_config.rate_limiter_status IS '限流状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.rate_limiter_threshold IS '每秒限流阈值';
COMMENT ON COLUMN sj_notify_config.description IS '描述';
COMMENT ON COLUMN sj_notify_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_notify_config IS '通知配置';

-- sj_notify_recipient
CREATE TABLE sj_notify_recipient
(
    id               number GENERATED ALWAYS AS IDENTITY,
    namespace_id     varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    recipient_name   varchar2(64)                                             NULL,
    notify_type      smallint      DEFAULT 0                                  NOT NULL,
    notify_attribute varchar2(512)                                            NULL,
    description      varchar2(256) DEFAULT ''                                 NULL,
    create_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_notify_recipient
    ADD CONSTRAINT pk_sj_notify_recipient PRIMARY KEY (id);

CREATE INDEX idx_sj_notify_recipient_01 ON sj_notify_recipient (namespace_id);

COMMENT ON COLUMN sj_notify_recipient.id IS '主键';
COMMENT ON COLUMN sj_notify_recipient.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_recipient.recipient_name IS '接收人名称';
COMMENT ON COLUMN sj_notify_recipient.notify_type IS '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook';
COMMENT ON COLUMN sj_notify_recipient.notify_attribute IS '配置属性';
COMMENT ON COLUMN sj_notify_recipient.description IS '描述';
COMMENT ON COLUMN sj_notify_recipient.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_recipient.update_dt IS '修改时间';
COMMENT ON TABLE sj_notify_recipient IS '告警通知接收人';

-- sj_retry_dead_letter_0
CREATE TABLE sj_retry_dead_letter_0
(
    id            number GENERATED ALWAYS AS IDENTITY,
    namespace_id  varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    unique_id     varchar2(64)                                             NULL,
    group_name    varchar2(64)                                             NULL,
    scene_name    varchar2(64)                                             NULL,
    idempotent_id varchar2(64)                                             NULL,
    biz_no        varchar2(64)  DEFAULT ''                                 NULL,
    executor_name varchar2(512) DEFAULT ''                                 NULL,
    args_str      clob                                                     NULL,
    ext_attrs     clob                                                     NULL,
    task_type     smallint      DEFAULT 1                                  NOT NULL,
    create_dt     date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_retry_dead_letter_0
    ADD CONSTRAINT pk_sj_retry_dead_letter_0 PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_retry_dead_letter_0_01 ON sj_retry_dead_letter_0 (namespace_id, group_name, unique_id);

CREATE INDEX idx_sj_retry_dead_letter_0_01 ON sj_retry_dead_letter_0 (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_dead_letter_0_02 ON sj_retry_dead_letter_0 (idempotent_id);
CREATE INDEX idx_sj_retry_dead_letter_0_03 ON sj_retry_dead_letter_0 (biz_no);
CREATE INDEX idx_sj_retry_dead_letter_0_04 ON sj_retry_dead_letter_0 (create_dt);

COMMENT ON COLUMN sj_retry_dead_letter_0.id IS '主键';
COMMENT ON COLUMN sj_retry_dead_letter_0.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_dead_letter_0.unique_id IS '同组下id唯一';
COMMENT ON COLUMN sj_retry_dead_letter_0.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_dead_letter_0.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_dead_letter_0.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry_dead_letter_0.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry_dead_letter_0.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry_dead_letter_0.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry_dead_letter_0.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_dead_letter_0.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry_dead_letter_0.create_dt IS '创建时间';
COMMENT ON TABLE sj_retry_dead_letter_0 IS '死信队列表';

-- sj_retry_task_0
CREATE TABLE sj_retry_task_0
(
    id              number GENERATED ALWAYS AS IDENTITY,
    namespace_id    varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    unique_id       varchar2(64)                                             NULL,
    group_name      varchar2(64)                                             NULL,
    scene_name      varchar2(64)                                             NULL,
    idempotent_id   varchar2(64)                                             NULL,
    biz_no          varchar2(64)  DEFAULT ''                                 NULL,
    executor_name   varchar2(512) DEFAULT ''                                 NULL,
    args_str        clob                                                     NULL,
    ext_attrs       clob                                                     NULL,
    next_trigger_at date                                                     NOT NULL,
    retry_count     number        DEFAULT 0                                  NOT NULL,
    retry_status    smallint      DEFAULT 0                                  NOT NULL,
    task_type       smallint      DEFAULT 1                                  NOT NULL,
    create_dt       date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt       date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_retry_task_0
    ADD CONSTRAINT pk_sj_retry_task_0 PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_retry_task_0_01 ON sj_retry_task_0 (namespace_id, group_name, unique_id);

CREATE INDEX idx_sj_retry_task_0_01 ON sj_retry_task_0 (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_task_0_02 ON sj_retry_task_0 (namespace_id, group_name, task_type);
CREATE INDEX idx_sj_retry_task_0_03 ON sj_retry_task_0 (namespace_id, group_name, retry_status);
CREATE INDEX idx_sj_retry_task_0_04 ON sj_retry_task_0 (idempotent_id);
CREATE INDEX idx_sj_retry_task_0_05 ON sj_retry_task_0 (biz_no);
CREATE INDEX idx_sj_retry_task_0_06 ON sj_retry_task_0 (create_dt);

COMMENT ON COLUMN sj_retry_task_0.id IS '主键';
COMMENT ON COLUMN sj_retry_task_0.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task_0.unique_id IS '同组下id唯一';
COMMENT ON COLUMN sj_retry_task_0.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task_0.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_task_0.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry_task_0.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry_task_0.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry_task_0.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry_task_0.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_task_0.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_retry_task_0.retry_count IS '重试次数';
COMMENT ON COLUMN sj_retry_task_0.retry_status IS '重试状态 0、重试中 1、成功 2、最大重试次数';
COMMENT ON COLUMN sj_retry_task_0.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry_task_0.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_task_0.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_task_0 IS '任务表';

-- sj_retry_task_log
CREATE TABLE sj_retry_task_log
(
    id            number GENERATED ALWAYS AS IDENTITY,
    namespace_id  varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    unique_id     varchar2(64)                                             NULL,
    group_name    varchar2(64)                                             NULL,
    scene_name    varchar2(64)                                             NULL,
    idempotent_id varchar2(64)                                             NULL,
    biz_no        varchar2(64)  DEFAULT ''                                 NULL,
    executor_name varchar2(512) DEFAULT ''                                 NULL,
    args_str      clob                                                     NULL,
    ext_attrs     clob                                                     NULL,
    retry_status  smallint      DEFAULT 0                                  NOT NULL,
    task_type     smallint      DEFAULT 1                                  NOT NULL,
    create_dt     date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt     date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_retry_task_log
    ADD CONSTRAINT pk_sj_retry_task_log PRIMARY KEY (id);

CREATE INDEX idx_sj_retry_task_log_01 ON sj_retry_task_log (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_task_log_02 ON sj_retry_task_log (retry_status);
CREATE INDEX idx_sj_retry_task_log_03 ON sj_retry_task_log (idempotent_id);
CREATE INDEX idx_sj_retry_task_log_04 ON sj_retry_task_log (unique_id);
CREATE INDEX idx_sj_retry_task_log_05 ON sj_retry_task_log (biz_no);
CREATE INDEX idx_sj_retry_task_log_06 ON sj_retry_task_log (create_dt);

COMMENT ON COLUMN sj_retry_task_log.id IS '主键';
COMMENT ON COLUMN sj_retry_task_log.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task_log.unique_id IS '同组下id唯一';
COMMENT ON COLUMN sj_retry_task_log.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task_log.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_task_log.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry_task_log.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry_task_log.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry_task_log.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry_task_log.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_task_log.retry_status IS '重试状态 0、重试中 1、成功 2、最大次数';
COMMENT ON COLUMN sj_retry_task_log.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry_task_log.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_task_log.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_task_log IS '任务日志基础信息表';

-- sj_retry_task_log_message
CREATE TABLE sj_retry_task_log_message
(
    id           number GENERATED ALWAYS AS IDENTITY,
    namespace_id varchar2(64) DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name   varchar2(64)                                            NULL,
    unique_id    varchar2(64)                                            NULL,
    message      clob                                                    NULL,
    log_num      number       DEFAULT 1                                  NOT NULL,
    real_time    number       DEFAULT 0                                  NOT NULL,
    create_dt    date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_retry_task_log_message
    ADD CONSTRAINT pk_sj_retry_task_log_message PRIMARY KEY (id);

CREATE INDEX idx_sj_rt_log_message_01 ON sj_retry_task_log_message (namespace_id, group_name, unique_id);
CREATE INDEX idx_sj_rt_log_message_02 ON sj_retry_task_log_message (create_dt);

COMMENT ON COLUMN sj_retry_task_log_message.id IS '主键';
COMMENT ON COLUMN sj_retry_task_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task_log_message.unique_id IS '同组下id唯一';
COMMENT ON COLUMN sj_retry_task_log_message.message IS '异常信息';
COMMENT ON COLUMN sj_retry_task_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_retry_task_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_retry_task_log_message.create_dt IS '创建时间';
COMMENT ON TABLE sj_retry_task_log_message IS '任务调度日志信息记录表';

-- sj_retry_scene_config
CREATE TABLE sj_retry_scene_config
(
    id               number GENERATED ALWAYS AS IDENTITY,
    namespace_id     varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    scene_name       varchar2(64)                                             NULL,
    group_name       varchar2(64)                                             NULL,
    scene_status     smallint      DEFAULT 0                                  NOT NULL,
    max_retry_count  number        DEFAULT 5                                  NOT NULL,
    back_off         smallint      DEFAULT 1                                  NOT NULL,
    trigger_interval varchar2(16)  DEFAULT ''                                 NULL,
    deadline_request number        DEFAULT 60000                              NOT NULL,
    executor_timeout number        DEFAULT 5                                  NOT NULL,
    route_key        smallint      DEFAULT 4                                  NOT NULL,
    description      varchar2(256) DEFAULT ''                                 NULL,
    create_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_retry_scene_config
    ADD CONSTRAINT pk_sj_retry_scene_config PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_retry_scene_config_01 ON sj_retry_scene_config (namespace_id, group_name, scene_name);

COMMENT ON COLUMN sj_retry_scene_config.id IS '主键';
COMMENT ON COLUMN sj_retry_scene_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_scene_config.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_scene_config.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_scene_config.scene_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_retry_scene_config.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN sj_retry_scene_config.back_off IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_retry_scene_config.deadline_request IS 'Deadline Request 调用链超时 单位毫秒';
COMMENT ON COLUMN sj_retry_scene_config.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_retry_scene_config.route_key IS '路由策略';
COMMENT ON COLUMN sj_retry_scene_config.description IS '描述';
COMMENT ON COLUMN sj_retry_scene_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_scene_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_scene_config IS '场景配置';

-- sj_server_node
CREATE TABLE sj_server_node
(
    id           number GENERATED ALWAYS AS IDENTITY,
    namespace_id varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name   varchar2(64)                                             NULL,
    host_id      varchar2(64)                                             NULL,
    host_ip      varchar2(64)                                             NULL,
    host_port    number                                                   NOT NULL,
    expire_at    date                                                     NOT NULL,
    node_type    smallint                                                 NOT NULL,
    ext_attrs    varchar2(256) DEFAULT ''                                 NULL,
    create_dt    date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt    date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_server_node
    ADD CONSTRAINT pk_sj_server_node PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_server_node_01 ON sj_server_node (host_id, host_ip);

CREATE INDEX idx_sj_server_node_01 ON sj_server_node (namespace_id, group_name);
CREATE INDEX idx_sj_server_node_02 ON sj_server_node (expire_at, node_type);

COMMENT ON COLUMN sj_server_node.id IS '主键';
COMMENT ON COLUMN sj_server_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_server_node.group_name IS '组名称';
COMMENT ON COLUMN sj_server_node.host_id IS '主机id';
COMMENT ON COLUMN sj_server_node.host_ip IS '机器ip';
COMMENT ON COLUMN sj_server_node.host_port IS '机器端口';
COMMENT ON COLUMN sj_server_node.expire_at IS '过期时间';
COMMENT ON COLUMN sj_server_node.node_type IS '节点类型 1、客户端 2、是服务端';
COMMENT ON COLUMN sj_server_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_server_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_server_node.update_dt IS '修改时间';
COMMENT ON TABLE sj_server_node IS '服务器节点';

-- sj_distributed_lock
CREATE TABLE sj_distributed_lock
(
    name       varchar2(64)                              NOT NULL,
    lock_until timestamp(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    locked_at  timestamp(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    locked_by  varchar2(255)                             NULL,
    create_dt  date         DEFAULT CURRENT_TIMESTAMP    NOT NULL,
    update_dt  date         DEFAULT CURRENT_TIMESTAMP    NOT NULL
);

ALTER TABLE sj_distributed_lock
    ADD CONSTRAINT pk_sj_distributed_lock PRIMARY KEY (name);

COMMENT ON COLUMN sj_distributed_lock.name IS '锁名称';
COMMENT ON COLUMN sj_distributed_lock.lock_until IS '锁定时长';
COMMENT ON COLUMN sj_distributed_lock.locked_at IS '锁定时间';
COMMENT ON COLUMN sj_distributed_lock.locked_by IS '锁定者';
COMMENT ON COLUMN sj_distributed_lock.create_dt IS '创建时间';
COMMENT ON COLUMN sj_distributed_lock.update_dt IS '修改时间';
COMMENT ON TABLE sj_distributed_lock IS '锁定表';

-- sj_system_user
CREATE TABLE sj_system_user
(
    id        number GENERATED ALWAYS AS IDENTITY,
    username  varchar2(64)                       NULL,
    password  varchar2(128)                      NULL,
    role      smallint DEFAULT 0                 NOT NULL,
    create_dt date     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_dt date     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE sj_system_user
    ADD CONSTRAINT pk_sj_system_user PRIMARY KEY (id);

COMMENT ON COLUMN sj_system_user.id IS '主键';
COMMENT ON COLUMN sj_system_user.username IS '账号';
COMMENT ON COLUMN sj_system_user.password IS '密码';
COMMENT ON COLUMN sj_system_user.role IS '角色：1-普通用户、2-管理员';
COMMENT ON COLUMN sj_system_user.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user.update_dt IS '修改时间';
COMMENT ON TABLE sj_system_user IS '系统用户表';

-- pwd: admin
INSERT INTO sj_system_user(username, password, role, create_dt, update_dt) VALUES ('admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2, sysdate, sysdate);

-- sj_system_user_permission
CREATE TABLE sj_system_user_permission
(
    id             number GENERATED ALWAYS AS IDENTITY,
    group_name     varchar2(64)                                            NULL,
    namespace_id   varchar2(64) DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    system_user_id number                                                  NOT NULL,
    create_dt      date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt      date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_system_user_permission
    ADD CONSTRAINT pk_sj_system_user_permission PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_su_permission_01 ON sj_system_user_permission (namespace_id, group_name, system_user_id);

COMMENT ON COLUMN sj_system_user_permission.id IS '主键';
COMMENT ON COLUMN sj_system_user_permission.group_name IS '组名称';
COMMENT ON COLUMN sj_system_user_permission.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_system_user_permission.system_user_id IS '系统用户id';
COMMENT ON COLUMN sj_system_user_permission.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user_permission.update_dt IS '修改时间';
COMMENT ON TABLE sj_system_user_permission IS '系统用户权限表';

-- sj_sequence_alloc
CREATE TABLE sj_sequence_alloc
(
    id           number GENERATED ALWAYS AS IDENTITY,
    namespace_id varchar2(64) DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name   varchar2(64) DEFAULT ''                                 NULL,
    max_id       number       DEFAULT 1                                  NOT NULL,
    step         number       DEFAULT 100                                NOT NULL,
    update_dt    date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_sequence_alloc
    ADD CONSTRAINT pk_sj_sequence_alloc PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_sequence_alloc_01 ON sj_sequence_alloc (namespace_id, group_name);

COMMENT ON COLUMN sj_sequence_alloc.id IS '主键';
COMMENT ON COLUMN sj_sequence_alloc.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_sequence_alloc.group_name IS '组名称';
COMMENT ON COLUMN sj_sequence_alloc.max_id IS '最大id';
COMMENT ON COLUMN sj_sequence_alloc.step IS '步长';
COMMENT ON COLUMN sj_sequence_alloc.update_dt IS '更新时间';
COMMENT ON TABLE sj_sequence_alloc IS '号段模式序号ID分配表';

-- sj_job
CREATE TABLE sj_job
(
    id               number GENERATED ALWAYS AS IDENTITY,
    namespace_id     varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name       varchar2(64)                                             NULL,
    job_name         varchar2(64)                                             NULL,
    args_str         clob          DEFAULT NULL                               NULL,
    args_type        smallint      DEFAULT 1                                  NOT NULL,
    next_trigger_at  number                                                   NOT NULL,
    job_status       smallint      DEFAULT 1                                  NOT NULL,
    task_type        smallint      DEFAULT 1                                  NOT NULL,
    route_key        smallint      DEFAULT 4                                  NOT NULL,
    executor_type    smallint      DEFAULT 1                                  NOT NULL,
    executor_info    varchar2(255) DEFAULT NULL                               NULL,
    trigger_type     smallint                                                 NOT NULL,
    trigger_interval varchar2(255)                                            NULL,
    block_strategy   smallint      DEFAULT 1                                  NOT NULL,
    executor_timeout number        DEFAULT 0                                  NOT NULL,
    max_retry_times  number        DEFAULT 0                                  NOT NULL,
    parallel_num     number        DEFAULT 1                                  NOT NULL,
    retry_interval   number        DEFAULT 0                                  NOT NULL,
    bucket_index     number        DEFAULT 0                                  NOT NULL,
    resident         smallint      DEFAULT 0                                  NOT NULL,
    description      varchar2(256) DEFAULT ''                                 NULL,
    ext_attrs        varchar2(256) DEFAULT ''                                 NULL,
    deleted          smallint      DEFAULT 0                                  NOT NULL,
    create_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_job
    ADD CONSTRAINT pk_sj_job PRIMARY KEY (id);

CREATE INDEX idx_sj_job_01 ON sj_job (namespace_id, group_name);
CREATE INDEX idx_sj_job_02 ON sj_job (job_status, bucket_index);
CREATE INDEX idx_sj_job_03 ON sj_job (create_dt);

COMMENT ON COLUMN sj_job.id IS '主键';
COMMENT ON COLUMN sj_job.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job.group_name IS '组名称';
COMMENT ON COLUMN sj_job.job_name IS '名称';
COMMENT ON COLUMN sj_job.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_job.job_status IS '任务状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_job.task_type IS '任务类型 1、集群 2、广播 3、切片';
COMMENT ON COLUMN sj_job.route_key IS '路由策略';
COMMENT ON COLUMN sj_job.executor_type IS '执行器类型';
COMMENT ON COLUMN sj_job.executor_info IS '执行器名称';
COMMENT ON COLUMN sj_job.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_job.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_job.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_job.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_job.max_retry_times IS '最大重试次数';
COMMENT ON COLUMN sj_job.parallel_num IS '并行数';
COMMENT ON COLUMN sj_job.retry_interval IS '重试间隔 ( s ) ';
COMMENT ON COLUMN sj_job.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_job.resident IS '是否是常驻任务';
COMMENT ON COLUMN sj_job.description IS '描述';
COMMENT ON COLUMN sj_job.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job.update_dt IS '修改时间';
COMMENT ON TABLE sj_job IS '任务信息';

INSERT INTO sj_job(namespace_id, group_name, job_name, args_str, args_type, next_trigger_at, job_status, task_type, route_key, executor_type, executor_info, trigger_type, trigger_interval, block_strategy,executor_timeout, max_retry_times, parallel_num, retry_interval, bucket_index, resident, description, ext_attrs, deleted, create_dt, update_dt) VALUES ('dev', 'ruoyi_group', 'demo-job', NULL, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3, 1, 1, 116, 0, '', '', 0, sysdate, sysdate);

-- sj_job_log_message
CREATE TABLE sj_job_log_message
(
    id            number GENERATED ALWAYS AS IDENTITY,
    namespace_id  varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name    varchar2(64)                                             NULL,
    job_id        number                                                   NOT NULL,
    task_batch_id number                                                   NOT NULL,
    task_id       number                                                   NOT NULL,
    message       clob                                                     NULL,
    log_num       number        DEFAULT 1                                  NOT NULL,
    real_time     number        DEFAULT 0                                  NOT NULL,
    ext_attrs     varchar2(256) DEFAULT ''                                 NULL,
    create_dt     date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_job_log_message
    ADD CONSTRAINT pk_sj_job_log_message PRIMARY KEY (id);

CREATE INDEX idx_sj_job_log_message_01 ON sj_job_log_message (task_batch_id, task_id);
CREATE INDEX idx_sj_job_log_message_02 ON sj_job_log_message (create_dt);
CREATE INDEX idx_sj_job_log_message_03 ON sj_job_log_message (namespace_id, group_name);

COMMENT ON COLUMN sj_job_log_message.id IS '主键';
COMMENT ON COLUMN sj_job_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_job_log_message.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_log_message.task_batch_id IS '任务批次id';
COMMENT ON COLUMN sj_job_log_message.task_id IS '调度任务id';
COMMENT ON COLUMN sj_job_log_message.message IS '调度信息';
COMMENT ON COLUMN sj_job_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_job_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_job_log_message.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_log_message.create_dt IS '创建时间';
COMMENT ON TABLE sj_job_log_message IS '调度日志';

-- sj_job_task
CREATE TABLE sj_job_task
(
    id             number GENERATED ALWAYS AS IDENTITY,
    namespace_id   varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name     varchar2(64)                                             NULL,
    job_id         number                                                   NOT NULL,
    task_batch_id  number                                                   NOT NULL,
    parent_id      number        DEFAULT 0                                  NOT NULL,
    task_status    smallint      DEFAULT 0                                  NOT NULL,
    retry_count    number        DEFAULT 0                                  NOT NULL,
    mr_stage       smallint      DEFAULT NULL                               NULL,
    leaf           smallint      DEFAULT '1'                                NOT NULL,
    task_name      varchar2(255) DEFAULT ''                                 NULL,
    client_info    varchar2(128) DEFAULT NULL                               NULL,
    wf_context     clob          DEFAULT NULL                               NULL,
    result_message clob                                                     NULL,
    args_str       clob          DEFAULT NULL                               NULL,
    args_type      smallint      DEFAULT 1                                  NOT NULL,
    ext_attrs      varchar2(256) DEFAULT ''                                 NULL,
    create_dt      date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt      date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_job_task
    ADD CONSTRAINT pk_sj_job_task PRIMARY KEY (id);

CREATE INDEX idx_sj_job_task_01 ON sj_job_task (task_batch_id, task_status);
CREATE INDEX idx_sj_job_task_02 ON sj_job_task (create_dt);
CREATE INDEX idx_sj_job_task_03 ON sj_job_task (namespace_id, group_name);

COMMENT ON COLUMN sj_job_task.id IS '主键';
COMMENT ON COLUMN sj_job_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_task.task_batch_id IS '调度任务id';
COMMENT ON COLUMN sj_job_task.parent_id IS '父执行器id';
COMMENT ON COLUMN sj_job_task.task_status IS '执行的状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task.retry_count IS '重试次数';
COMMENT ON COLUMN sj_job_task.mr_stage IS '动态分片所处阶段 1:map 2:reduce 3:mergeReduce';
COMMENT ON COLUMN sj_job_task.leaf IS '叶子节点';
COMMENT ON COLUMN sj_job_task.task_name IS '任务名称';
COMMENT ON COLUMN sj_job_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_job_task.wf_context IS '工作流全局上下文';
COMMENT ON COLUMN sj_job_task.result_message IS '执行结果';
COMMENT ON COLUMN sj_job_task.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job_task.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_task IS '任务实例';

-- sj_job_task_batch
CREATE TABLE sj_job_task_batch
(
    id                      number GENERATED ALWAYS AS IDENTITY,
    namespace_id            varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name              varchar2(64)                                             NULL,
    job_id                  number                                                   NOT NULL,
    workflow_node_id        number        DEFAULT 0                                  NOT NULL,
    parent_workflow_node_id number        DEFAULT 0                                  NOT NULL,
    workflow_task_batch_id  number        DEFAULT 0                                  NOT NULL,
    task_batch_status       smallint      DEFAULT 0                                  NOT NULL,
    operation_reason        smallint      DEFAULT 0                                  NOT NULL,
    execution_at            number        DEFAULT 0                                  NOT NULL,
    system_task_type        smallint      DEFAULT 3                                  NOT NULL,
    parent_id               varchar2(64)  DEFAULT ''                                 NULL,
    ext_attrs               varchar2(256) DEFAULT ''                                 NULL,
    deleted                 smallint      DEFAULT 0                                  NOT NULL,
    create_dt               date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt               date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_job_task_batch
    ADD CONSTRAINT pk_sj_job_task_batch PRIMARY KEY (id);

CREATE INDEX idx_sj_job_task_batch_01 ON sj_job_task_batch (job_id, task_batch_status);
CREATE INDEX idx_sj_job_task_batch_02 ON sj_job_task_batch (create_dt);
CREATE INDEX idx_sj_job_task_batch_03 ON sj_job_task_batch (namespace_id, group_name);
CREATE INDEX idx_sj_job_task_batch_04 ON sj_job_task_batch (workflow_task_batch_id, workflow_node_id);

COMMENT ON COLUMN sj_job_task_batch.id IS '主键';
COMMENT ON COLUMN sj_job_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task_batch.job_id IS '任务id';
COMMENT ON COLUMN sj_job_task_batch.workflow_node_id IS '工作流节点id';
COMMENT ON COLUMN sj_job_task_batch.parent_workflow_node_id IS '工作流任务父批次id';
COMMENT ON COLUMN sj_job_task_batch.workflow_task_batch_id IS '工作流任务批次id';
COMMENT ON COLUMN sj_job_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_job_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_job_task_batch.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_task_batch.parent_id IS '父节点';
COMMENT ON COLUMN sj_job_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task_batch.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_task_batch IS '任务批次';

-- sj_job_summary
CREATE TABLE sj_job_summary
(
    id               number GENERATED ALWAYS AS IDENTITY,
    namespace_id     varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name       varchar2(64)  DEFAULT ''                                 NULL,
    business_id      number                                                   NOT NULL,
    system_task_type smallint      DEFAULT 3                                  NOT NULL,
    trigger_at       date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    success_num      number        DEFAULT 0                                  NOT NULL,
    fail_num         number        DEFAULT 0                                  NOT NULL,
    fail_reason      varchar2(512) DEFAULT ''                                 NULL,
    stop_num         number        DEFAULT 0                                  NOT NULL,
    stop_reason      varchar2(512) DEFAULT ''                                 NULL,
    cancel_num       number        DEFAULT 0                                  NOT NULL,
    cancel_reason    varchar2(512) DEFAULT ''                                 NULL,
    create_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_job_summary
    ADD CONSTRAINT pk_sj_job_summary PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_job_summary_01 ON sj_job_summary (trigger_at, system_task_type, business_id);

CREATE INDEX idx_sj_job_summary_01 ON sj_job_summary (namespace_id, group_name, business_id);

COMMENT ON COLUMN sj_job_summary.id IS '主键';
COMMENT ON COLUMN sj_job_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_job_summary.business_id IS '业务id  ( job_id或workflow_id ) ';
COMMENT ON COLUMN sj_job_summary.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_job_summary.success_num IS '执行成功-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.stop_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.stop_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.cancel_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.cancel_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_summary.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_summary IS 'DashBoard_Job';

-- sj_retry_summary
CREATE TABLE sj_retry_summary
(
    id            number GENERATED ALWAYS AS IDENTITY,
    namespace_id  varchar2(64) DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name    varchar2(64) DEFAULT ''                                 NULL,
    scene_name    varchar2(50) DEFAULT ''                                 NULL,
    trigger_at    date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    running_num   number       DEFAULT 0                                  NOT NULL,
    finish_num    number       DEFAULT 0                                  NOT NULL,
    max_count_num number       DEFAULT 0                                  NOT NULL,
    suspend_num   number       DEFAULT 0                                  NOT NULL,
    create_dt     date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt     date         DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_retry_summary
    ADD CONSTRAINT pk_sj_retry_summary PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_sj_retry_summary_01 ON sj_retry_summary (namespace_id, group_name, scene_name, trigger_at);

CREATE INDEX idx_sj_retry_summary_01 ON sj_retry_summary (trigger_at);

COMMENT ON COLUMN sj_retry_summary.id IS '主键';
COMMENT ON COLUMN sj_retry_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_summary.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_retry_summary.running_num IS '重试中-日志数量';
COMMENT ON COLUMN sj_retry_summary.finish_num IS '重试完成-日志数量';
COMMENT ON COLUMN sj_retry_summary.max_count_num IS '重试到达最大次数-日志数量';
COMMENT ON COLUMN sj_retry_summary.suspend_num IS '暂停重试-日志数量';
COMMENT ON COLUMN sj_retry_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_summary.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_summary IS 'DashBoard_Retry';

-- sj_workflow
CREATE TABLE sj_workflow
(
    id               number GENERATED ALWAYS AS IDENTITY,
    workflow_name    varchar2(64)                                             NULL,
    namespace_id     varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name       varchar2(64)                                             NULL,
    workflow_status  smallint      DEFAULT 1                                  NOT NULL,
    trigger_type     smallint                                                 NOT NULL,
    trigger_interval varchar2(255)                                            NULL,
    next_trigger_at  number                                                   NOT NULL,
    block_strategy   smallint      DEFAULT 1                                  NOT NULL,
    executor_timeout number        DEFAULT 0                                  NOT NULL,
    description      varchar2(256) DEFAULT ''                                 NULL,
    flow_info        clob          DEFAULT NULL                               NULL,
    wf_context       clob          DEFAULT NULL                               NULL,
    bucket_index     number        DEFAULT 0                                  NOT NULL,
    version          number                                                   NOT NULL,
    ext_attrs        varchar2(256) DEFAULT ''                                 NULL,
    deleted          smallint      DEFAULT 0                                  NOT NULL,
    create_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt        date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_workflow
    ADD CONSTRAINT pk_sj_workflow PRIMARY KEY (id);

CREATE INDEX idx_sj_workflow_01 ON sj_workflow (create_dt);
CREATE INDEX idx_sj_workflow_02 ON sj_workflow (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow.id IS '主键';
COMMENT ON COLUMN sj_workflow.workflow_name IS '工作流名称';
COMMENT ON COLUMN sj_workflow.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow.workflow_status IS '工作流状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_workflow.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_workflow.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_workflow.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_workflow.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_workflow.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_workflow.description IS '描述';
COMMENT ON COLUMN sj_workflow.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow.wf_context IS '上下文';
COMMENT ON COLUMN sj_workflow.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_workflow.version IS '版本号';
COMMENT ON COLUMN sj_workflow.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow IS '工作流';

-- sj_workflow_node
CREATE TABLE sj_workflow_node
(
    id                   number GENERATED ALWAYS AS IDENTITY,
    namespace_id         varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    node_name            varchar2(64)                                             NULL,
    group_name           varchar2(64)                                             NULL,
    job_id               number                                                   NOT NULL,
    workflow_id          number                                                   NOT NULL,
    node_type            smallint      DEFAULT 1                                  NOT NULL,
    expression_type      smallint      DEFAULT 0                                  NOT NULL,
    fail_strategy        smallint      DEFAULT 1                                  NOT NULL,
    workflow_node_status smallint      DEFAULT 1                                  NOT NULL,
    priority_level       number        DEFAULT 1                                  NOT NULL,
    node_info            clob          DEFAULT NULL                               NULL,
    version              number                                                   NOT NULL,
    ext_attrs            varchar2(256) DEFAULT ''                                 NULL,
    deleted              smallint      DEFAULT 0                                  NOT NULL,
    create_dt            date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt            date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_workflow_node
    ADD CONSTRAINT pk_sj_workflow_node PRIMARY KEY (id);

CREATE INDEX idx_sj_workflow_node_01 ON sj_workflow_node (create_dt);
CREATE INDEX idx_sj_workflow_node_02 ON sj_workflow_node (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow_node.id IS '主键';
COMMENT ON COLUMN sj_workflow_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_node.node_name IS '节点名称';
COMMENT ON COLUMN sj_workflow_node.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_node.job_id IS '任务信息id';
COMMENT ON COLUMN sj_workflow_node.workflow_id IS '工作流ID';
COMMENT ON COLUMN sj_workflow_node.node_type IS '1、任务节点 2、条件节点';
COMMENT ON COLUMN sj_workflow_node.expression_type IS '1、SpEl、2、Aviator 3、QL';
COMMENT ON COLUMN sj_workflow_node.fail_strategy IS '失败策略 1、跳过 2、阻塞';
COMMENT ON COLUMN sj_workflow_node.workflow_node_status IS '工作流节点状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_workflow_node.priority_level IS '优先级';
COMMENT ON COLUMN sj_workflow_node.node_info IS '节点信息 ';
COMMENT ON COLUMN sj_workflow_node.version IS '版本号';
COMMENT ON COLUMN sj_workflow_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_node.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_node.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow_node IS '工作流节点';

-- sj_workflow_task_batch
CREATE TABLE sj_workflow_task_batch
(
    id                number GENERATED ALWAYS AS IDENTITY,
    namespace_id      varchar2(64)  DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' NULL,
    group_name        varchar2(64)                                             NULL,
    workflow_id       number                                                   NOT NULL,
    task_batch_status smallint      DEFAULT 0                                  NOT NULL,
    operation_reason  smallint      DEFAULT 0                                  NOT NULL,
    flow_info         clob          DEFAULT NULL                               NULL,
    wf_context        clob          DEFAULT NULL                               NULL,
    execution_at      number        DEFAULT 0                                  NOT NULL,
    ext_attrs         varchar2(256) DEFAULT ''                                 NULL,
    version           number        DEFAULT 1                                  NOT NULL,
    deleted           smallint      DEFAULT 0                                  NOT NULL,
    create_dt         date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL,
    update_dt         date          DEFAULT CURRENT_TIMESTAMP                  NOT NULL
);

ALTER TABLE sj_workflow_task_batch
    ADD CONSTRAINT pk_sj_workflow_task_batch PRIMARY KEY (id);

CREATE INDEX idx_sj_workflow_task_batch_01 ON sj_workflow_task_batch (workflow_id, task_batch_status);
CREATE INDEX idx_sj_workflow_task_batch_02 ON sj_workflow_task_batch (create_dt);
CREATE INDEX idx_sj_workflow_task_batch_03 ON sj_workflow_task_batch (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow_task_batch.id IS '主键';
COMMENT ON COLUMN sj_workflow_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_task_batch.workflow_id IS '工作流任务id';
COMMENT ON COLUMN sj_workflow_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_workflow_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_workflow_task_batch.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow_task_batch.wf_context IS '全局上下文';
COMMENT ON COLUMN sj_workflow_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_workflow_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_task_batch.version IS '版本号';
COMMENT ON COLUMN sj_workflow_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_task_batch.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow_task_batch IS '工作流批次';
