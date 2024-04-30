-- PostgreSQL DDL

CREATE TABLE er_namespace
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    unique_id   VARCHAR(64)  NOT NULL,
    description VARCHAR(256) NOT NULL DEFAULT '',
    create_dt   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_namespace_unique_id ON er_namespace (unique_id);

COMMENT ON COLUMN er_namespace.id IS '主键';
COMMENT ON COLUMN er_namespace.name IS '名称';
COMMENT ON COLUMN er_namespace.unique_id IS '唯一id';
COMMENT ON COLUMN er_namespace.description IS '描述';
COMMENT ON COLUMN er_namespace.create_dt IS '创建时间';
COMMENT ON COLUMN er_namespace.update_dt IS '修改时间';
COMMENT ON COLUMN er_namespace.deleted IS '逻辑删除 1、删除';
COMMENT ON TABLE er_namespace IS '命名空间';

INSERT INTO er_namespace VALUES (1, 'Development', 'dev', '', now(), now(), 0);
INSERT INTO er_namespace VALUES (2, 'Production', 'prod', '', now(), now(), 0);

CREATE TABLE er_group_config
(
    id                BIGSERIAL PRIMARY KEY,
    namespace_id      VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        VARCHAR(64) NOT NULL,
    description       VARCHAR(256)         DEFAULT '',
    token             VARCHAR(64)          DEFAULT 'ER_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',
    group_status      SMALLINT    NOT NULL DEFAULT 0,
    version           INT         NOT NULL,
    group_partition   INT         NOT NULL,
    id_generator_mode SMALLINT    NOT NULL DEFAULT 1,
    init_scene        SMALLINT    NOT NULL DEFAULT 0,
    bucket_index      INT         NOT NULL DEFAULT 0,
    create_dt         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_namespace_id_group_name_group_config ON er_group_config (namespace_id, group_name);

COMMENT ON COLUMN er_group_config.id IS '主键';
COMMENT ON COLUMN er_group_config.namespace_id IS '命名空间';
COMMENT ON COLUMN er_group_config.group_name IS '组名称';
COMMENT ON COLUMN er_group_config.description IS '组描述';
COMMENT ON COLUMN er_group_config.token IS 'token';
COMMENT ON COLUMN er_group_config.group_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN er_group_config.version IS '版本号';
COMMENT ON COLUMN er_group_config.group_partition IS '分区';
COMMENT ON COLUMN er_group_config.id_generator_mode IS '唯一id生成模式 默认号段模式';
COMMENT ON COLUMN er_group_config.init_scene IS '是否初始化场景 0:否 1:是';
COMMENT ON COLUMN er_group_config.bucket_index IS 'bucket';
COMMENT ON COLUMN er_group_config.create_dt IS '创建时间';
COMMENT ON COLUMN er_group_config.update_dt IS '修改时间';
COMMENT ON TABLE er_group_config IS '组配置';

INSERT INTO er_group_config VALUES (1, 'dev', 'ruoyi_group', '', 'ER_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, 4, now(), now());

CREATE TABLE er_notify_config
(
    id                     BIGSERIAL PRIMARY KEY,
    namespace_id           VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             VARCHAR(64)  NOT NULL,
    scene_name             VARCHAR(64)  NOT NULL,
    notify_status          SMALLINT     NOT NULL DEFAULT 0,
    notify_type            SMALLINT     NOT NULL DEFAULT 0,
    notify_attribute       VARCHAR(512) NOT NULL,
    notify_threshold       INT          NOT NULL DEFAULT 0,
    notify_scene           SMALLINT     NOT NULL DEFAULT 0,
    rate_limiter_status    SMALLINT     NOT NULL DEFAULT 0,
    rate_limiter_threshold INT          NOT NULL DEFAULT 0,
    description            VARCHAR(256) NOT NULL DEFAULT '',
    create_dt              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_namespace_id_group_name_notify_config ON er_notify_config (namespace_id, group_name);

COMMENT ON COLUMN er_notify_config.id IS '主键';
COMMENT ON COLUMN er_notify_config.group_name IS '组名称';
COMMENT ON COLUMN er_notify_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_notify_config.scene_name IS '场景名称';
COMMENT ON COLUMN er_notify_config.notify_status IS '通知状态 0、未启用 1、启用';
COMMENT ON COLUMN er_notify_config.notify_type IS '通知类型 1、钉钉 2、邮件 3、企业微信';
COMMENT ON COLUMN er_notify_config.notify_attribute IS '配置属性';
COMMENT ON COLUMN er_notify_config.notify_threshold IS '通知阈值';
COMMENT ON COLUMN er_notify_config.notify_scene IS '通知场景';
COMMENT ON COLUMN er_notify_config.rate_limiter_status IS '限流状态 0、未启用 1、启用';
COMMENT ON COLUMN er_notify_config.rate_limiter_threshold IS '每秒限流阈值';
COMMENT ON COLUMN er_notify_config.description IS '描述';
COMMENT ON COLUMN er_notify_config.create_dt IS '创建时间';
COMMENT ON COLUMN er_notify_config.update_dt IS '修改时间';
COMMENT ON TABLE er_notify_config IS '通知配置';


CREATE TABLE er_retry_dead_letter_0
(
    id            BIGSERIAL PRIMARY KEY,
    unique_id     VARCHAR(64)  NOT NULL,
    namespace_id  VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    VARCHAR(64)  NOT NULL,
    scene_name    VARCHAR(64)  NOT NULL,
    idempotent_id VARCHAR(64)  NOT NULL,
    biz_no        VARCHAR(64)  NOT NULL DEFAULT '',
    executor_name VARCHAR(512) NOT NULL DEFAULT '',
    args_str      TEXT         NOT NULL,
    ext_attrs     TEXT         NOT NULL,
    task_type     SMALLINT     NOT NULL DEFAULT 1,
    create_dt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_namespace_id_group_name_unique_id_retry_dead_letter ON er_retry_dead_letter_0 (namespace_id, group_name, unique_id);
CREATE INDEX idx_namespace_id_group_name_scene_name_retry_dead_letter ON er_retry_dead_letter_0 (namespace_id, group_name, scene_name);
CREATE INDEX idx_idempotent_id_retry_dead_letter ON er_retry_dead_letter_0 (idempotent_id);
CREATE INDEX idx_biz_no_retry_dead_letter ON er_retry_dead_letter_0 (biz_no);
CREATE INDEX idx_create_dt_retry_dead_letter ON er_retry_dead_letter_0 (create_dt);

COMMENT ON COLUMN er_retry_dead_letter_0.id IS '主键';
COMMENT ON COLUMN er_retry_dead_letter_0.unique_id IS '同组下id唯一';
COMMENT ON COLUMN er_retry_dead_letter_0.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_retry_dead_letter_0.group_name IS '组名称';
COMMENT ON COLUMN er_retry_dead_letter_0.scene_name IS '场景名称';
COMMENT ON COLUMN er_retry_dead_letter_0.idempotent_id IS '幂等id';
COMMENT ON COLUMN er_retry_dead_letter_0.biz_no IS '业务编号';
COMMENT ON COLUMN er_retry_dead_letter_0.executor_name IS '执行器名称';
COMMENT ON COLUMN er_retry_dead_letter_0.args_str IS '执行方法参数';
COMMENT ON COLUMN er_retry_dead_letter_0.ext_attrs IS '扩展字段';
COMMENT ON COLUMN er_retry_dead_letter_0.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN er_retry_dead_letter_0.create_dt IS '创建时间';
COMMENT ON TABLE er_retry_dead_letter_0 IS '死信队列表';

CREATE TABLE er_retry_task_0
(
    id              BIGSERIAL PRIMARY KEY,
    unique_id       VARCHAR(64)  NOT NULL,
    namespace_id    VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name      VARCHAR(64)  NOT NULL,
    scene_name      VARCHAR(64)  NOT NULL,
    idempotent_id   VARCHAR(64)  NOT NULL,
    biz_no          VARCHAR(64)  NOT NULL DEFAULT '',
    executor_name   VARCHAR(512) NOT NULL DEFAULT '',
    args_str        TEXT         NOT NULL,
    ext_attrs       TEXT         NOT NULL,
    next_trigger_at TIMESTAMP    NOT NULL,
    retry_count     INT          NOT NULL DEFAULT 0,
    retry_status    SMALLINT     NOT NULL DEFAULT 0,
    task_type       SMALLINT     NOT NULL DEFAULT 1,
    create_dt       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_name_unique_id_retry_task ON er_retry_task_0 (namespace_id, group_name, unique_id);
CREATE INDEX idx_namespace_id_group_name_scene_name_retry_task ON er_retry_task_0 (namespace_id, group_name, scene_name);
CREATE INDEX idx_namespace_id_group_name_retry_status_retry_task ON er_retry_task_0 (namespace_id, group_name, retry_status);
CREATE INDEX idx_idempotent_id_retry_task ON er_retry_task_0 (idempotent_id);
CREATE INDEX idx_biz_no_retry_task ON er_retry_task_0 (biz_no);
CREATE INDEX idx_create_dt_retry_task ON er_retry_task_0 (create_dt);

COMMENT ON COLUMN er_retry_task_0.id IS '主键';
COMMENT ON COLUMN er_retry_task_0.unique_id IS '同组下id唯一';
COMMENT ON COLUMN er_retry_task_0.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_retry_task_0.group_name IS '组名称';
COMMENT ON COLUMN er_retry_task_0.scene_name IS '场景名称';
COMMENT ON COLUMN er_retry_task_0.idempotent_id IS '幂等id';
COMMENT ON COLUMN er_retry_task_0.biz_no IS '业务编号';
COMMENT ON COLUMN er_retry_task_0.executor_name IS '执行器名称';
COMMENT ON COLUMN er_retry_task_0.args_str IS '执行方法参数';
COMMENT ON COLUMN er_retry_task_0.ext_attrs IS '扩展字段';
COMMENT ON COLUMN er_retry_task_0.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN er_retry_task_0.retry_count IS '重试次数';
COMMENT ON COLUMN er_retry_task_0.retry_status IS '重试状态 0、重试中 1、成功 2、最大重试次数';
COMMENT ON COLUMN er_retry_task_0.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN er_retry_task_0.create_dt IS '创建时间';
COMMENT ON COLUMN er_retry_task_0.update_dt IS '修改时间';
COMMENT ON TABLE er_retry_task_0 IS '任务表';

CREATE TABLE er_retry_task_log
(
    id            BIGSERIAL PRIMARY KEY,
    unique_id     VARCHAR(64)  NOT NULL,
    namespace_id  VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    VARCHAR(64)  NOT NULL,
    scene_name    VARCHAR(64)  NOT NULL,
    idempotent_id VARCHAR(64)  NOT NULL,
    biz_no        VARCHAR(64)  NOT NULL DEFAULT '',
    executor_name VARCHAR(512) NOT NULL DEFAULT '',
    args_str      TEXT         NOT NULL,
    ext_attrs     TEXT         NOT NULL,
    retry_status  SMALLINT     NOT NULL DEFAULT 0,
    task_type     SMALLINT     NOT NULL DEFAULT 1,
    create_dt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP

);

CREATE INDEX idx_group_name_scene_name_retry_task_log ON er_retry_task_log (namespace_id, group_name, scene_name);
CREATE INDEX idx_retry_status_retry_task_log ON er_retry_task_log (retry_status);
CREATE INDEX idx_idempotent_id_retry_task_log ON er_retry_task_log (idempotent_id);
CREATE INDEX idx_unique_id ON er_retry_task_log (namespace_id, group_name, unique_id);
CREATE INDEX idx_biz_no_retry_task_log ON er_retry_task_log (biz_no);
CREATE INDEX idx_create_dt_retry_task_log ON er_retry_task_log (create_dt);

COMMENT ON COLUMN er_retry_task_log.id IS '主键';
COMMENT ON COLUMN er_retry_task_log.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_retry_task_log.unique_id IS '同组下id唯一';
COMMENT ON COLUMN er_retry_task_log.group_name IS '组名称';
COMMENT ON COLUMN er_retry_task_log.scene_name IS '场景名称';
COMMENT ON COLUMN er_retry_task_log.idempotent_id IS '幂等id';
COMMENT ON COLUMN er_retry_task_log.biz_no IS '业务编号';
COMMENT ON COLUMN er_retry_task_log.executor_name IS '执行器名称';
COMMENT ON COLUMN er_retry_task_log.args_str IS '执行方法参数';
COMMENT ON COLUMN er_retry_task_log.ext_attrs IS '扩展字段';
COMMENT ON COLUMN er_retry_task_log.retry_status IS '重试状态 0、重试中 1、成功 2、最大次数';
COMMENT ON COLUMN er_retry_task_log.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN er_retry_task_log.create_dt IS '创建时间';
COMMENT ON COLUMN er_retry_task_log.update_dt IS '修改时间';
COMMENT ON TABLE er_retry_task_log IS '任务日志基础信息表';

CREATE TABLE er_retry_task_log_message
(
    id           BIGSERIAL PRIMARY KEY,
    namespace_id VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   VARCHAR(64) NOT NULL,
    unique_id    VARCHAR(64) NOT NULL,
    create_dt    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message      TEXT        NOT NULL,
    log_num      INT         NOT NULL DEFAULT 1,
    real_time    BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_namespace_id_group_name_unique_id ON er_retry_task_log_message (namespace_id, group_name, unique_id);
CREATE INDEX idx_create_dt ON er_retry_task_log_message (create_dt);
COMMENT ON COLUMN er_retry_task_log_message.id IS '主键';
COMMENT ON COLUMN er_retry_task_log_message.namespace_id IS '命名空间';
COMMENT ON COLUMN er_retry_task_log_message.group_name IS '组名称';
COMMENT ON COLUMN er_retry_task_log_message.unique_id IS '同组下id唯一';
COMMENT ON COLUMN er_retry_task_log_message.create_dt IS '创建时间';
COMMENT ON COLUMN er_retry_task_log_message.message IS '异常信息';
COMMENT ON COLUMN er_retry_task_log_message.log_num IS '日志条数';
COMMENT ON COLUMN er_retry_task_log_message.real_time IS '实际时间';
COMMENT ON TABLE er_retry_task_log_message IS '任务调度日志信息记录表';

CREATE TABLE er_scene_config
(
    id               BIGSERIAL PRIMARY KEY,
    namespace_id     VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    scene_name       VARCHAR(64)  NOT NULL,
    group_name       VARCHAR(64)  NOT NULL,
    scene_status     SMALLINT     NOT NULL DEFAULT 0,
    max_retry_count  INT          NOT NULL DEFAULT 5,
    back_off         SMALLINT     NOT NULL DEFAULT 1,
    trigger_interval VARCHAR(16)  NOT NULL DEFAULT '',
    deadline_request BIGINT       NOT NULL DEFAULT 60000,
    route_key        SMALLINT     NOT NULL DEFAULT 4,
    executor_timeout INT          NOT NULL DEFAULT 5,
    description      VARCHAR(256) NOT NULL DEFAULT '',
    create_dt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_namespace_id_group_name_scene_name ON er_scene_config (namespace_id, group_name, scene_name);
COMMENT ON COLUMN er_scene_config.id IS '主键';
COMMENT ON COLUMN er_scene_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_scene_config.scene_name IS '场景名称';
COMMENT ON COLUMN er_scene_config.group_name IS '组名称';
COMMENT ON COLUMN er_scene_config.scene_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN er_scene_config.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN er_scene_config.back_off IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN er_scene_config.trigger_interval IS '间隔时长';
COMMENT ON COLUMN er_scene_config.deadline_request IS 'Deadline Request 调用链超时 单位毫秒';
COMMENT ON COLUMN er_scene_config.description IS '描述';
COMMENT ON COLUMN er_scene_config.route_key IS '路由策略';
COMMENT ON COLUMN er_scene_config.executor_timeout IS '超时时间';
COMMENT ON COLUMN er_scene_config.create_dt IS '创建时间';
COMMENT ON COLUMN er_scene_config.update_dt IS '修改时间';
COMMENT ON TABLE er_scene_config IS '场景配置';

CREATE TABLE er_server_node
(
    id           BIGSERIAL PRIMARY KEY,
    namespace_id VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   VARCHAR(64)  NOT NULL,
    host_id      VARCHAR(64)  NOT NULL,
    host_ip      VARCHAR(64)  NOT NULL,
    context_path VARCHAR(256) NOT NULL DEFAULT '/',
    host_port    INT          NOT NULL,
    expire_at    TIMESTAMP    NOT NULL,
    node_type    SMALLINT     NOT NULL,
    ext_attrs    VARCHAR(256)          DEFAULT '',
    create_dt    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_host_id_host_ip ON er_server_node (host_id, host_ip);
CREATE INDEX idx_expire_at_node_type ON er_server_node (expire_at, node_type);
CREATE INDEX idx_namespace_id_group_name_server_node ON er_server_node (namespace_id, group_name);

COMMENT ON COLUMN er_server_node.id IS '主键';
COMMENT ON COLUMN er_server_node.group_name IS '组名称';
COMMENT ON COLUMN er_server_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_server_node.host_id IS '主机id';
COMMENT ON COLUMN er_server_node.host_ip IS '机器ip';
COMMENT ON COLUMN er_server_node.context_path IS '客户端上下文路径 server.servlet.context-path';
COMMENT ON COLUMN er_server_node.host_port IS '机器端口';
COMMENT ON COLUMN er_server_node.expire_at IS '过期时间';
COMMENT ON COLUMN er_server_node.node_type IS '节点类型 1、客户端 2、是服务端';
COMMENT ON COLUMN er_server_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN er_server_node.create_dt IS '创建时间';
COMMENT ON COLUMN er_server_node.update_dt IS '修改时间';
COMMENT ON TABLE er_server_node IS '服务器节点';

CREATE TABLE er_distributed_lock
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by  VARCHAR(255) NOT NULL,
    create_dt  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE er_distributed_lock
    ADD CONSTRAINT uk_name UNIQUE (name);

COMMENT ON COLUMN er_distributed_lock.id IS '主键';
COMMENT ON COLUMN er_distributed_lock.name IS '锁名称';
COMMENT ON COLUMN er_distributed_lock.lock_until IS '锁定时长';
COMMENT ON COLUMN er_distributed_lock.locked_at IS '锁定时间';
COMMENT ON COLUMN er_distributed_lock.locked_by IS '锁定者';
COMMENT ON COLUMN er_distributed_lock.create_dt IS '创建时间';
COMMENT ON COLUMN er_distributed_lock.update_dt IS '修改时间';
COMMENT ON TABLE er_distributed_lock IS '锁定表';


CREATE TABLE er_system_user
(
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(64)  NOT NULL,
    password  VARCHAR(128) NOT NULL,
    role      SMALLINT     NOT NULL DEFAULT 0,
    create_dt TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_username ON er_system_user (username);
COMMENT ON COLUMN er_system_user.id IS '主键';
COMMENT ON COLUMN er_system_user.username IS '账号';
COMMENT ON COLUMN er_system_user.password IS '密码';
COMMENT ON COLUMN er_system_user.role IS '角色：1-普通用户、2-管理员';
COMMENT ON COLUMN er_system_user.create_dt IS '创建时间';
COMMENT ON COLUMN er_system_user.update_dt IS '修改时间';
COMMENT ON TABLE er_system_user IS '系统用户表';

-- pwd: admin
INSERT INTO er_system_user VALUES (1, 'admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2, now(), now());

CREATE TABLE er_system_user_permission
(
    id             BIGSERIAL PRIMARY KEY,
    namespace_id   VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name     VARCHAR(64) NOT NULL,
    system_user_id BIGINT      NOT NULL,
    create_dt      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_namespace_id_group_name_system_user_id ON er_system_user_permission (namespace_id, group_name, system_user_id);
COMMENT ON COLUMN er_system_user_permission.id IS '主键';
COMMENT ON COLUMN er_system_user_permission.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_system_user_permission.group_name IS '组名称';
COMMENT ON COLUMN er_system_user_permission.system_user_id IS '系统用户id';
COMMENT ON COLUMN er_system_user_permission.create_dt IS '创建时间';
COMMENT ON COLUMN er_system_user_permission.update_dt IS '修改时间';
COMMENT ON TABLE er_system_user_permission IS '系统用户权限表';


CREATE TABLE er_sequence_alloc
(
    id           BIGSERIAL PRIMARY KEY,
    namespace_id VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   VARCHAR(64) NOT NULL DEFAULT '',
    max_id       BIGINT      NOT NULL DEFAULT 1,
    step         INT         NOT NULL DEFAULT 100,
    update_dt    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_namespace_id_group_name ON er_sequence_alloc (namespace_id, group_name);

COMMENT ON COLUMN er_sequence_alloc.id IS '主键';
COMMENT ON COLUMN er_sequence_alloc.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_sequence_alloc.group_name IS '组名称';
COMMENT ON COLUMN er_sequence_alloc.max_id IS '最大id';
COMMENT ON COLUMN er_sequence_alloc.step IS '步长';
COMMENT ON COLUMN er_sequence_alloc.update_dt IS '更新时间';
COMMENT ON TABLE er_sequence_alloc IS '号段模式序号ID分配表';

-- 分布式调度DDL
CREATE TABLE er_job
(
    id               BIGSERIAL PRIMARY KEY,
    namespace_id     VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       VARCHAR(64)  NOT NULL,
    job_name         VARCHAR(64)  NOT NULL,
    args_str         TEXT         NOT NULL DEFAULT '',
    args_type        SMALLINT     NOT NULL DEFAULT 1,
    next_trigger_at  BIGINT       NOT NULL,
    job_status       SMALLINT     NOT NULL DEFAULT 1,
    task_type        SMALLINT     NOT NULL DEFAULT 1,
    route_key        SMALLINT     NOT NULL DEFAULT 4,
    executor_type    SMALLINT     NOT NULL DEFAULT 1,
    executor_info    VARCHAR(255)          DEFAULT NULL,
    trigger_type     SMALLINT     NOT NULL,
    trigger_interval VARCHAR(255) NOT NULL,
    block_strategy   SMALLINT     NOT NULL DEFAULT 1,
    executor_timeout INT          NOT NULL DEFAULT 0,
    max_retry_times  INT          NOT NULL DEFAULT 0,
    parallel_num     INT          NOT NULL DEFAULT 1,
    retry_interval   INT          NOT NULL DEFAULT 0,
    bucket_index     INT          NOT NULL DEFAULT 0,
    resident         SMALLINT     NOT NULL DEFAULT 0,
    description      VARCHAR(256) NOT NULL DEFAULT '',
    ext_attrs        VARCHAR(256)          DEFAULT NULL,
    create_dt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_namespace_id_group_name_job ON er_job (namespace_id, group_name);
CREATE INDEX idx_job_status_bucket_index_job ON er_job (job_status, bucket_index);
CREATE INDEX idx_create_dt_job ON er_job (create_dt);

COMMENT ON COLUMN er_job.id IS '主键';
COMMENT ON COLUMN er_job.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_job.group_name IS '组名称';
COMMENT ON COLUMN er_job.job_name IS '名称';
COMMENT ON COLUMN er_job.args_str IS '执行方法参数';
COMMENT ON COLUMN er_job.args_type IS '参数类型';
COMMENT ON COLUMN er_job.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN er_job.job_status IS '重试状态 0、关闭、1、开启';
COMMENT ON COLUMN er_job.task_type IS '任务类型 1、集群 2、广播 3、切片';
COMMENT ON COLUMN er_job.route_key IS '路由策略';
COMMENT ON COLUMN er_job.executor_type IS '执行器类型';
COMMENT ON COLUMN er_job.executor_info IS '执行器名称';
COMMENT ON COLUMN er_job.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN er_job.trigger_interval IS '间隔时长';
COMMENT ON COLUMN er_job.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN er_job.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN er_job.max_retry_times IS '最大重试次数';
COMMENT ON COLUMN er_job.parallel_num IS '并行数';
COMMENT ON COLUMN er_job.retry_interval IS '更新重试间隔(s)';
COMMENT ON COLUMN er_job.bucket_index IS 'bucket';
COMMENT ON COLUMN er_job.resident IS '是否是常驻任务';
COMMENT ON COLUMN er_job.description IS '描述';
COMMENT ON COLUMN er_job.ext_attrs IS '扩展字段';
COMMENT ON COLUMN er_job.create_dt IS '创建时间';
COMMENT ON COLUMN er_job.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN er_job.update_dt IS '更新时间';
COMMENT ON TABLE er_job IS '任务信息';

INSERT INTO er_job VALUES (1, 'dev', 'ruoyi_group', 'demo-job', null, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3, 1, 1, 116, 0, '', '', now(), now(), 0);

CREATE TABLE er_job_log_message
(
    id            BIGSERIAL PRIMARY KEY,
    namespace_id  VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    VARCHAR(64) NOT NULL,
    job_id        BIGINT      NOT NULL,
    task_batch_id BIGINT      NOT NULL,
    task_id       BIGINT      NOT NULL,
    message       TEXT        NOT NULL,
    log_num       INT         NOT NULL DEFAULT 1,
    real_time     BIGINT      NOT NULL DEFAULT 0,
    ext_attrs     VARCHAR(256)         DEFAULT '',
    create_dt     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_namespace_id_group_name_to_job_log_message ON er_job_log_message (namespace_id, group_name);
CREATE INDEX idx_task_batch_id_task_id_to_job_log_message ON er_job_log_message (task_batch_id, task_id);
CREATE INDEX idx_create_dt_to_job_log_message ON er_job_log_message (create_dt);

COMMENT ON COLUMN er_job_log_message.id IS '主键';
COMMENT ON COLUMN er_job_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_job_log_message.group_name IS '组名称';
COMMENT ON COLUMN er_job_log_message.job_id IS '任务信息id';
COMMENT ON COLUMN er_job_log_message.task_batch_id IS '任务批次id';
COMMENT ON COLUMN er_job_log_message.task_id IS '调度任务id';
COMMENT ON COLUMN er_job_log_message.message IS '调度信息';
COMMENT ON COLUMN er_job_log_message.log_num IS '日志序号';
COMMENT ON COLUMN er_job_log_message.real_time IS '实际时间';
COMMENT ON COLUMN er_job_log_message.create_dt IS '创建时间';
COMMENT ON COLUMN er_job_log_message.ext_attrs IS '扩展字段';
COMMENT ON TABLE er_job_log_message IS '调度日志';

CREATE TABLE er_job_task
(
    id             BIGSERIAL PRIMARY KEY,
    namespace_id   VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name     VARCHAR(64) NOT NULL,
    job_id         BIGINT      NOT NULL,
    task_batch_id  BIGINT      NOT NULL,
    parent_id      BIGINT      NOT NULL DEFAULT 0,
    task_status    SMALLINT    NOT NULL DEFAULT 0,
    retry_count    INT         NOT NULL DEFAULT 0,
    client_info    VARCHAR(128)         DEFAULT NULL,
    result_message TEXT        NOT NULL,
    args_str       TEXT        NOT NULL,
    args_type      SMALLINT    NOT NULL DEFAULT 1,
    ext_attrs      VARCHAR(256)         DEFAULT '',
    create_dt      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_namespace_id_group_name_to_job_task ON er_job_task (namespace_id, group_name);
CREATE INDEX idx_task_batch_id_task_status_to_job_task ON er_job_task (task_batch_id, task_status);
CREATE INDEX idx_create_dt_to_job_task ON er_job_task (create_dt);
COMMENT ON COLUMN er_job_task.id IS '主键';
COMMENT ON COLUMN er_job_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_job_task.group_name IS '组名称';
COMMENT ON COLUMN er_job_task.job_id IS '任务信息id';
COMMENT ON COLUMN er_job_task.task_batch_id IS '任务批次id';
COMMENT ON COLUMN er_job_task.parent_id IS '父执行器id';
COMMENT ON COLUMN er_job_task.task_status IS '执行的状态 0、失败 1、成功';
COMMENT ON COLUMN er_job_task.retry_count IS '重试次数';
COMMENT ON COLUMN er_job_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN er_job_task.result_message IS '调度信息';
COMMENT ON COLUMN er_job_task.args_str IS '执行方法参数';
COMMENT ON COLUMN er_job_task.args_type IS '参数类型';
COMMENT ON COLUMN er_job_task.create_dt IS '创建时间';
COMMENT ON COLUMN er_job_task.update_dt IS '创建时间';
COMMENT ON COLUMN er_job_task.ext_attrs IS '扩展字段';
COMMENT ON TABLE er_job_task IS '任务实例';

CREATE TABLE er_job_task_batch
(
    id                      BIGSERIAL PRIMARY KEY,
    namespace_id            VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name              VARCHAR(64) NOT NULL,
    job_id                  BIGINT      NOT NULL,
    workflow_node_id        BIGINT      NOT NULL DEFAULT 0,
    parent_workflow_node_id BIGINT      NOT NULL DEFAULT 0,
    workflow_task_batch_id  BIGINT      NOT NULL DEFAULT 0,
    parent_id               VARCHAR(64) NOT NULL DEFAULT '',
    task_batch_status       SMALLINT    NOT NULL DEFAULT 0,
    operation_reason        SMALLINT    NOT NULL DEFAULT 0,
    execution_at            BIGINT      NOT NULL DEFAULT 0,
    system_task_type        SMALLINT    NOT NULL DEFAULT 3,
    ext_attrs               VARCHAR(256)         DEFAULT '',
    deleted                 SMALLINT    NOT NULL DEFAULT 0,
    create_dt               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_namespace_id_group_name_to_job_task_batch ON er_job_task_batch (namespace_id, group_name);
CREATE INDEX idx_job_id_task_batch_status_to_job_task_batch ON er_job_task_batch (job_id, task_batch_status);
CREATE INDEX idx_create_dt_to_job_task_batch ON er_job_task_batch (create_dt);
CREATE INDEX idx_workflow_task_batch_id_workflow_node_id ON er_job_task_batch (workflow_task_batch_id, workflow_node_id);

COMMENT ON COLUMN er_job_task_batch.id IS '主键';
COMMENT ON COLUMN er_job_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_job_task_batch.group_name IS '组名称';
COMMENT ON COLUMN er_job_task_batch.job_id IS '任务信息id';
COMMENT ON COLUMN er_job_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN er_job_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN er_job_task_batch.workflow_node_id IS '工作流节点id';
COMMENT ON COLUMN er_job_task_batch.parent_workflow_node_id IS '父节点';
COMMENT ON COLUMN er_job_task_batch.workflow_task_batch_id IS '任务批次id';
COMMENT ON COLUMN er_job_task_batch.system_task_type IS '任务类型 0、系统任务 1、业务任务';
COMMENT ON COLUMN er_job_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN er_job_task_batch.parent_id IS '父节点';
COMMENT ON COLUMN er_job_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN er_job_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN er_job_task_batch.update_dt IS '创建时间';
COMMENT ON COLUMN er_job_task_batch.ext_attrs IS '扩展字段';
COMMENT ON TABLE er_job_task_batch IS '任务批次';

CREATE TABLE er_job_notify_config
(
    id                     BIGSERIAL PRIMARY KEY,
    namespace_id           VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             VARCHAR(64)  NOT NULL,
    job_id                 BIGINT       NOT NULL,
    notify_status          SMALLINT     NOT NULL DEFAULT 0,
    notify_type            SMALLINT     NOT NULL DEFAULT 0,
    notify_attribute       VARCHAR(512) NOT NULL,
    notify_threshold       INT          NOT NULL DEFAULT 0,
    notify_scene           SMALLINT     NOT NULL DEFAULT 0,
    rate_limiter_status    SMALLINT     NOT NULL DEFAULT 0,
    rate_limiter_threshold INT          NOT NULL DEFAULT 0,
    description            VARCHAR(256) NOT NULL DEFAULT '',
    create_dt              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_namespace_id_group_name_job_id_job_notify_config ON er_job_notify_config (namespace_id, group_name, job_id);

COMMENT ON COLUMN er_job_notify_config.id IS '主键';
COMMENT ON COLUMN er_job_notify_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_job_notify_config.group_name IS '组名称';
COMMENT ON COLUMN er_job_notify_config.job_id IS '任务信息id';
COMMENT ON COLUMN er_job_notify_config.notify_status IS '通知状态 0、未启用 1、启用';
COMMENT ON COLUMN er_job_notify_config.notify_type IS '通知类型 1、钉钉 2、邮件 3、企业微信';
COMMENT ON COLUMN er_job_notify_config.notify_attribute IS '配置属性';
COMMENT ON COLUMN er_job_notify_config.notify_threshold IS '通知阈值';
COMMENT ON COLUMN er_job_notify_config.notify_scene IS '通知场景';
COMMENT ON COLUMN er_job_notify_config.rate_limiter_status IS '限流状态 0、未启用 1、启用';
COMMENT ON COLUMN er_job_notify_config.rate_limiter_threshold IS '每秒限流阈值';
COMMENT ON COLUMN er_job_notify_config.description IS '描述';
COMMENT ON COLUMN er_job_notify_config.create_dt IS '创建时间';
COMMENT ON COLUMN er_job_notify_config.update_dt IS '修改时间';
COMMENT ON TABLE er_job_notify_config IS '通知配置';

CREATE TABLE er_retry_summary
(
    id            BIGSERIAL PRIMARY KEY,
    namespace_id  VARCHAR(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    VARCHAR(64) NOT NULL DEFAULT '',
    scene_name    VARCHAR(50) NOT NULL DEFAULT '',
    trigger_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    running_num   INT         NOT NULL DEFAULT 0,
    finish_num    INT         NOT NULL DEFAULT 0,
    max_count_num INT         NOT NULL DEFAULT 0,
    suspend_num   INT         NOT NULL DEFAULT 0,
    create_dt     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_scene_name_trigger_at ON er_retry_summary (namespace_id, group_name, scene_name, trigger_at);

COMMENT ON COLUMN er_retry_summary.id IS '主键';
COMMENT ON COLUMN er_retry_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_retry_summary.group_name IS '组名称';
COMMENT ON COLUMN er_retry_summary.scene_name IS '场景名称';
COMMENT ON COLUMN er_retry_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN er_retry_summary.running_num IS '重试中-日志数量';
COMMENT ON COLUMN er_retry_summary.finish_num IS '重试完成-日志数量';
COMMENT ON COLUMN er_retry_summary.max_count_num IS '重试到达最大次数-日志数量';
COMMENT ON COLUMN er_retry_summary.suspend_num IS '暂停重试-日志数量';
COMMENT ON COLUMN er_retry_summary.create_dt IS '创建时间';
COMMENT ON COLUMN er_retry_summary.update_dt IS '修改时间';
COMMENT ON TABLE er_retry_summary IS 'DashBoard_Retry';

CREATE TABLE er_job_summary
(
    id               BIGSERIAL PRIMARY KEY,
    namespace_id     VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       VARCHAR(64)  NOT NULL DEFAULT '',
    business_id      BIGINT       NOT NULL,
    system_task_type SMALLINT     NOT NULL DEFAULT '3',
    trigger_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success_num      INT          NOT NULL DEFAULT 0,
    fail_num         INT          NOT NULL DEFAULT 0,
    fail_reason      VARCHAR(512) NOT NULL DEFAULT '',
    stop_num         INT          NOT NULL DEFAULT 0,
    stop_reason      VARCHAR(512) NOT NULL DEFAULT '',
    cancel_num       INT          NOT NULL DEFAULT 0,
    cancel_reason    VARCHAR(512) NOT NULL DEFAULT '',
    create_dt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_trigger_at_system_task_type_business_id ON er_job_summary (trigger_at, system_task_type, business_id);
CREATE INDEX idx_namespace_id_group_name_job_id ON er_job_summary (namespace_id, group_name, business_id);

COMMENT ON COLUMN er_job_summary.id IS '主键';
COMMENT ON COLUMN er_job_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_job_summary.group_name IS '组名称';
COMMENT ON COLUMN er_job_summary.business_id IS '业务id (job_id或workflow_id)';
COMMENT ON COLUMN er_job_summary.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN er_job_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN er_job_summary.success_num IS '执行成功-日志数量';
COMMENT ON COLUMN er_job_summary.fail_num IS '执行失败-日志数量';
COMMENT ON COLUMN er_job_summary.fail_reason IS '失败原因';
COMMENT ON COLUMN er_job_summary.stop_num IS '执行失败-日志数量';
COMMENT ON COLUMN er_job_summary.stop_reason IS '失败原因';
COMMENT ON COLUMN er_job_summary.cancel_num IS '执行失败-日志数量';
COMMENT ON COLUMN er_job_summary.cancel_reason IS '失败原因';
COMMENT ON TABLE er_job_summary IS 'DashBoard_Job';

CREATE TABLE er_workflow
(
    id               BIGSERIAL PRIMARY KEY,
    workflow_name    varchar(64)  NOT NULL,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL,
    workflow_status  smallint     NOT NULL DEFAULT 1,
    trigger_type     smallint     NOT NULL,
    trigger_interval varchar(255) NOT NULL,
    next_trigger_at  BIGINT       NOT NULL,
    block_strategy   smallint     NOT NULL DEFAULT 1,
    executor_timeout INT          NOT NULL DEFAULT 0,
    description      varchar(256) NOT NULL DEFAULT '',
    flow_info        text                  DEFAULT NULL,
    bucket_index     INT          NOT NULL DEFAULT 0,
    version          INT          NOT NULL,
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          smallint     NOT NULL DEFAULT 0,
    ext_attrs        varchar(256) NULL     DEFAULT ''
);


CREATE INDEX idx_create_dt_workflow ON er_workflow (create_dt);
CREATE INDEX idx_namespace_id_group_name_workflow ON er_workflow (namespace_id, group_name);

COMMENT ON TABLE er_workflow IS '工作流';
COMMENT ON COLUMN er_workflow.id IS '主键';
COMMENT ON COLUMN er_workflow.workflow_name IS '工作流名称';
COMMENT ON COLUMN er_workflow.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_workflow.group_name IS '组名称';
COMMENT ON COLUMN er_workflow.workflow_status IS '工作流状态 0、关闭、1、开启';
COMMENT ON COLUMN er_workflow.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN er_workflow.trigger_interval IS '间隔时长';
COMMENT ON COLUMN er_workflow.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN er_workflow.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN er_workflow.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN er_workflow.description IS '描述';
COMMENT ON COLUMN er_workflow.flow_info IS '流程信息';
COMMENT ON COLUMN er_workflow.bucket_index IS 'bucket';
COMMENT ON COLUMN er_workflow.version IS '版本号';
COMMENT ON COLUMN er_workflow.create_dt IS '创建时间';
COMMENT ON COLUMN er_workflow.update_dt IS '修改时间';
COMMENT ON COLUMN er_workflow.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN er_workflow.ext_attrs IS '扩展字段';

CREATE TABLE er_workflow_node
(
    id                   BIGSERIAL PRIMARY KEY,
    namespace_id         VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    node_name            VARCHAR(64)  NOT NULL,
    group_name           VARCHAR(64)  NOT NULL,
    job_id               BIGINT       NOT NULL,
    workflow_id          BIGINT       NOT NULL,
    node_type            SMALLINT     NOT NULL DEFAULT 1,
    expression_type      SMALLINT     NOT NULL DEFAULT 0,
    fail_strategy        SMALLINT     NOT NULL DEFAULT 1,
    workflow_node_status SMALLINT     NOT NULL DEFAULT 1,
    priority_level       INT          NOT NULL DEFAULT 1,
    node_info            TEXT                  DEFAULT NULL,
    version              INT          NOT NULL,
    create_dt            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              SMALLINT     NOT NULL DEFAULT 0,
    ext_attrs            VARCHAR(256) NULL     DEFAULT ''
);

CREATE INDEX idx_create_dt_workflow_node ON er_workflow_node (create_dt);
CREATE INDEX idx_namespace_id_group_name_workflow_node ON er_workflow_node (namespace_id, group_name);

COMMENT ON TABLE er_workflow_node IS '工作流节点';
COMMENT ON COLUMN er_workflow_node.id IS '主键';
COMMENT ON COLUMN er_workflow_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_workflow_node.node_name IS '节点名称';
COMMENT ON COLUMN er_workflow_node.group_name IS '组名称';
COMMENT ON COLUMN er_workflow_node.job_id IS '任务信息id';
COMMENT ON COLUMN er_workflow_node.workflow_id IS '工作流ID';
COMMENT ON COLUMN er_workflow_node.node_type IS '1、任务节点 2、条件节点';
COMMENT ON COLUMN er_workflow_node.expression_type IS '1、SpEl、2、Aviator 3、QL';
COMMENT ON COLUMN er_workflow_node.fail_strategy IS '失败策略 1、跳过 2、阻塞';
COMMENT ON COLUMN er_workflow_node.workflow_node_status IS '工作流节点状态 0、关闭、1、开启';
COMMENT ON COLUMN er_workflow_node.priority_level IS '优先级';
COMMENT ON COLUMN er_workflow_node.node_info IS '节点信息';
COMMENT ON COLUMN er_workflow_node.version IS '版本号';
COMMENT ON COLUMN er_workflow_node.create_dt IS '创建时间';
COMMENT ON COLUMN er_workflow_node.update_dt IS '修改时间';
COMMENT ON COLUMN er_workflow_node.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN er_workflow_node.ext_attrs IS '扩展字段';

CREATE TABLE er_workflow_task_batch
(
    id                BIGSERIAL PRIMARY KEY,
    namespace_id      VARCHAR(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        VARCHAR(64)  NOT NULL,
    workflow_id       BIGINT       NOT NULL,
    task_batch_status SMALLINT     NOT NULL DEFAULT 0,
    operation_reason  SMALLINT     NOT NULL DEFAULT 0,
    flow_info         TEXT                  DEFAULT NULL,
    execution_at      BIGINT       NOT NULL DEFAULT 0,
    create_dt         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    ext_attrs         VARCHAR(256) NULL     DEFAULT ''
);

CREATE INDEX idx_job_id_task_batch_status_workflow_task_batch ON er_workflow_task_batch (workflow_id, task_batch_status);
CREATE INDEX idx_create_dt_workflow_task_batch ON er_workflow_task_batch (create_dt);
CREATE INDEX idx_namespace_id_group_name_workflow_task_batch ON er_workflow_task_batch (namespace_id, group_name);

COMMENT ON TABLE er_workflow_task_batch IS '工作流批次';
COMMENT ON COLUMN er_workflow_task_batch.id IS '主键';
COMMENT ON COLUMN er_workflow_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN er_workflow_task_batch.group_name IS '组名称';
COMMENT ON COLUMN er_workflow_task_batch.workflow_id IS '工作流任务id';
COMMENT ON COLUMN er_workflow_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN er_workflow_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN er_workflow_task_batch.flow_info IS '流程信息';
COMMENT ON COLUMN er_workflow_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN er_workflow_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN er_workflow_task_batch.update_dt IS '修改时间';
COMMENT ON COLUMN er_workflow_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN er_workflow_task_batch.ext_attrs IS '扩展字段';
