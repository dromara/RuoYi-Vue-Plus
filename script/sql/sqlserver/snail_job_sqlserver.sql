/*
 SnailJob Database Transfer Tool
 Source Server Type    : MySQL
 Target Server Type    : Microsoft SQL Server
 Date: 2024-07-06 12:55:47
*/


-- sj_namespace
CREATE TABLE sj_namespace
(
    id          bigint        NOT NULL PRIMARY KEY IDENTITY,
    name        nvarchar(64)  NOT NULL,
    unique_id   nvarchar(64)  NOT NULL,
    description nvarchar(256) NOT NULL DEFAULT '',
    deleted     tinyint       NOT NULL DEFAULT 0,
    create_dt   datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt   datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_namespace_01 ON sj_namespace (name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'唯一id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_namespace'
GO

INSERT INTO sj_namespace(name, unique_id, description, deleted, create_dt, update_dt) VALUES (N'Development', N'dev', N'', 0, getdate(), getdate())
GO
INSERT INTO sj_namespace(name, unique_id, description, deleted, create_dt, update_dt) VALUES (N'Production', N'prod', N'', 0, getdate(), getdate())
GO

-- sj_group_config
CREATE TABLE sj_group_config
(
    id                bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id      nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        nvarchar(64)  NOT NULL DEFAULT '',
    description       nvarchar(256) NOT NULL DEFAULT '',
    token             nvarchar(64)  NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',
    group_status      tinyint       NOT NULL DEFAULT 0,
    version           int           NOT NULL,
    group_partition   int           NOT NULL,
    id_generator_mode tinyint       NOT NULL DEFAULT 1,
    init_scene        tinyint       NOT NULL DEFAULT 0,
    bucket_index      int           NOT NULL DEFAULT 0,
    create_dt         datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_group_config_01 ON sj_group_config (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'token',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'token'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'group_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'分区',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'group_partition'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'唯一id生成模式 默认号段模式',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'id_generator_mode'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'是否初始化场景 0:否 1:是',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'init_scene'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'bucket',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'bucket_index'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组配置',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_group_config'
GO

INSERT INTO sj_group_config(namespace_id, group_name, description, token, group_status, version, group_partition, id_generator_mode, init_scene, bucket_index, create_dt, update_dt) VALUES (N'dev', N'ruoyi_group', N'', N'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', N'1', N'1', N'0', N'1', N'1', N'4', getdate(), getdate())
GO

-- sj_notify_config
CREATE TABLE sj_notify_config
(
    id                     bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id           nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             nvarchar(64)  NOT NULL,
    business_id            nvarchar(64)  NOT NULL,
    system_task_type       tinyint       NOT NULL DEFAULT 3,
    notify_status          tinyint       NOT NULL DEFAULT 0,
    recipient_ids          nvarchar(128) NOT NULL,
    notify_threshold       int           NOT NULL DEFAULT 0,
    notify_scene           tinyint       NOT NULL DEFAULT 0,
    rate_limiter_status    tinyint       NOT NULL DEFAULT 0,
    rate_limiter_threshold int           NOT NULL DEFAULT 0,
    description            nvarchar(256) NOT NULL DEFAULT '',
    create_dt              datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt              datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_notify_config_01 ON sj_notify_config (namespace_id, group_name, business_id)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务id  ( job_id或workflow_id或scene_name ) ',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'business_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'system_task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'notify_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'接收人id列表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'recipient_ids'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知阈值',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'notify_threshold'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知场景',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'notify_scene'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'限流状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'rate_limiter_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'每秒限流阈值',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'rate_limiter_threshold'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知配置',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_config'
GO

-- sj_notify_recipient
CREATE TABLE sj_notify_recipient
(
    id               bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    recipient_name   nvarchar(64)  NOT NULL,
    notify_type      tinyint       NOT NULL DEFAULT 0,
    notify_attribute nvarchar(512) NOT NULL,
    description      nvarchar(256) NOT NULL DEFAULT '',
    create_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_notify_recipient_01 ON sj_notify_recipient (namespace_id)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'接收人名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'recipient_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'notify_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'配置属性',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'notify_attribute'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'告警通知接收人',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_notify_recipient'
GO

-- sj_retry_dead_letter_0
CREATE TABLE sj_retry_dead_letter_0
(
    id            bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    unique_id     nvarchar(64)  NOT NULL,
    group_name    nvarchar(64)  NOT NULL,
    scene_name    nvarchar(64)  NOT NULL,
    idempotent_id nvarchar(64)  NOT NULL,
    biz_no        nvarchar(64)  NOT NULL DEFAULT '',
    executor_name nvarchar(512) NOT NULL DEFAULT '',
    args_str      nvarchar(max) NOT NULL,
    ext_attrs     nvarchar(max) NOT NULL,
    task_type     tinyint       NOT NULL DEFAULT 1,
    create_dt     datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_retry_dead_letter_0_01 ON sj_retry_dead_letter_0 (namespace_id, group_name, unique_id)
GO

CREATE INDEX idx_sj_retry_dead_letter_0_01 ON sj_retry_dead_letter_0 (namespace_id, group_name, scene_name)
GO
CREATE INDEX idx_sj_retry_dead_letter_0_02 ON sj_retry_dead_letter_0 (idempotent_id)
GO
CREATE INDEX idx_sj_retry_dead_letter_0_03 ON sj_retry_dead_letter_0 (biz_no)
GO
CREATE INDEX idx_sj_retry_dead_letter_0_04 ON sj_retry_dead_letter_0 (create_dt)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'幂等id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'idempotent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务编号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'biz_no'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'executor_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、重试数据 2、回调数据',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'死信队列表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_dead_letter_0'
GO

-- sj_retry_task_0
CREATE TABLE sj_retry_task_0
(
    id              bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id    nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    unique_id       nvarchar(64)  NOT NULL,
    group_name      nvarchar(64)  NOT NULL,
    scene_name      nvarchar(64)  NOT NULL,
    idempotent_id   nvarchar(64)  NOT NULL,
    biz_no          nvarchar(64)  NOT NULL DEFAULT '',
    executor_name   nvarchar(512) NOT NULL DEFAULT '',
    args_str        nvarchar(max) NOT NULL,
    ext_attrs       nvarchar(max) NOT NULL,
    next_trigger_at datetime2     NOT NULL,
    retry_count     int           NOT NULL DEFAULT 0,
    retry_status    tinyint       NOT NULL DEFAULT 0,
    task_type       tinyint       NOT NULL DEFAULT 1,
    create_dt       datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt       datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_retry_task_0_01 ON sj_retry_task_0 (namespace_id, group_name, unique_id)
GO

CREATE INDEX idx_sj_retry_task_0_01 ON sj_retry_task_0 (namespace_id, group_name, scene_name)
GO
CREATE INDEX idx_sj_retry_task_0_02 ON sj_retry_task_0 (namespace_id, group_name, task_type)
GO
CREATE INDEX idx_sj_retry_task_0_03 ON sj_retry_task_0 (namespace_id, group_name, retry_status)
GO
CREATE INDEX idx_sj_retry_task_0_04 ON sj_retry_task_0 (idempotent_id)
GO
CREATE INDEX idx_sj_retry_task_0_05 ON sj_retry_task_0 (biz_no)
GO
CREATE INDEX idx_sj_retry_task_0_06 ON sj_retry_task_0 (create_dt)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'幂等id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'idempotent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务编号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'biz_no'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'executor_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'下次触发时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'next_trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'retry_count'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试状态 0、重试中 1、成功 2、最大重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'retry_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、重试数据 2、回调数据',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_0'
GO

-- sj_retry_task_log
CREATE TABLE sj_retry_task_log
(
    id            bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    unique_id     nvarchar(64)  NOT NULL,
    group_name    nvarchar(64)  NOT NULL,
    scene_name    nvarchar(64)  NOT NULL,
    idempotent_id nvarchar(64)  NOT NULL,
    biz_no        nvarchar(64)  NOT NULL DEFAULT '',
    executor_name nvarchar(512) NOT NULL DEFAULT '',
    args_str      nvarchar(max) NOT NULL,
    ext_attrs     nvarchar(max) NOT NULL,
    retry_status  tinyint       NOT NULL DEFAULT 0,
    task_type     tinyint       NOT NULL DEFAULT 1,
    create_dt     datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_retry_task_log_01 ON sj_retry_task_log (namespace_id, group_name, scene_name)
GO
CREATE INDEX idx_sj_retry_task_log_02 ON sj_retry_task_log (retry_status)
GO
CREATE INDEX idx_sj_retry_task_log_03 ON sj_retry_task_log (idempotent_id)
GO
CREATE INDEX idx_sj_retry_task_log_04 ON sj_retry_task_log (unique_id)
GO
CREATE INDEX idx_sj_retry_task_log_05 ON sj_retry_task_log (biz_no)
GO
CREATE INDEX idx_sj_retry_task_log_06 ON sj_retry_task_log (create_dt)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'幂等id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'idempotent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务编号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'biz_no'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'executor_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试状态 0、重试中 1、成功 2、最大次数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'retry_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、重试数据 2、回调数据',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务日志基础信息表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log'
GO

-- sj_retry_task_log_message
CREATE TABLE sj_retry_task_log_message
(
    id           bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   nvarchar(64)  NOT NULL,
    unique_id    nvarchar(64)  NOT NULL,
    message      nvarchar(max) NOT NULL,
    log_num      int           NOT NULL DEFAULT 1,
    real_time    bigint        NOT NULL DEFAULT 0,
    create_dt    datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_retry_task_log_message_01 ON sj_retry_task_log_message (namespace_id, group_name, unique_id)
GO
CREATE INDEX idx_sj_retry_task_log_message_02 ON sj_retry_task_log_message (create_dt)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'异常信息',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'message'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'log_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'上报时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'real_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务调度日志信息记录表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_task_log_message'
GO

-- sj_retry_scene_config
CREATE TABLE sj_retry_scene_config
(
    id               bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    scene_name       nvarchar(64)  NOT NULL,
    group_name       nvarchar(64)  NOT NULL,
    scene_status     tinyint       NOT NULL DEFAULT 0,
    max_retry_count  int           NOT NULL DEFAULT 5,
    back_off         tinyint       NOT NULL DEFAULT 1,
    trigger_interval nvarchar(16)  NOT NULL DEFAULT '',
    deadline_request bigint        NOT NULL DEFAULT 60000,
    executor_timeout int           NOT NULL DEFAULT 5,
    route_key        tinyint       NOT NULL DEFAULT 4,
    description      nvarchar(256) NOT NULL DEFAULT '',
    create_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_retry_scene_config_01 ON sj_retry_scene_config (namespace_id, group_name, scene_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'scene_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'最大重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'max_retry_count'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'1、默认等级 2、固定间隔时间 3、CRON 表达式',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'back_off'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'间隔时长',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'trigger_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'Deadline Request 调用链超时 单位毫秒',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'deadline_request'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行超时时间，单位秒',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'executor_timeout'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'路由策略',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'route_key'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景配置',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_scene_config'
GO

-- sj_server_node
CREATE TABLE sj_server_node
(
    id           bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   nvarchar(64)  NOT NULL,
    host_id      nvarchar(64)  NOT NULL,
    host_ip      nvarchar(64)  NOT NULL,
    host_port    int           NOT NULL,
    expire_at    datetime2     NOT NULL,
    node_type    tinyint       NOT NULL,
    ext_attrs    nvarchar(256) NULL     DEFAULT '',
    create_dt    datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt    datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_server_node_01 ON sj_server_node (host_id, host_ip)
GO

CREATE INDEX idx_sj_server_node_01 ON sj_server_node (namespace_id, group_name)
GO
CREATE INDEX idx_sj_server_node_02 ON sj_server_node (expire_at, node_type)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主机id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'host_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'机器ip',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'host_ip'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'机器端口',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'host_port'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'过期时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'expire_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'节点类型 1、客户端 2、是服务端',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'服务器节点',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_server_node'
GO

-- sj_distributed_lock
CREATE TABLE sj_distributed_lock
(
    name       nvarchar(64)  NOT NULL PRIMARY KEY,
    lock_until datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at  datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by  nvarchar(255) NOT NULL,
    create_dt  datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt  datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock',
     'COLUMN', N'name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定时长',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock',
     'COLUMN', N'lock_until'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock',
     'COLUMN', N'locked_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定者',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock',
     'COLUMN', N'locked_by'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_distributed_lock'
GO

-- sj_system_user
CREATE TABLE sj_system_user
(
    id        bigint        NOT NULL PRIMARY KEY IDENTITY,
    username  nvarchar(64)  NOT NULL,
    password  nvarchar(128) NOT NULL,
    role      tinyint       NOT NULL DEFAULT 0,
    create_dt datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'账号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user',
     'COLUMN', N'username'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'密码',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user',
     'COLUMN', N'password'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'角色：1-普通用户、2-管理员',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user',
     'COLUMN', N'role'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'系统用户表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user'
GO

-- pwd: admin
INSERT INTO sj_system_user(username, password, role, create_dt, update_dt) VALUES (N'admin', N'465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', N'2', getdate(), getdate())
GO

-- sj_system_user_permission
CREATE TABLE sj_system_user_permission
(
    id             bigint       NOT NULL PRIMARY KEY IDENTITY,
    group_name     nvarchar(64) NOT NULL,
    namespace_id   nvarchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    system_user_id bigint       NOT NULL,
    create_dt      datetime2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      datetime2    NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_system_user_permission_01 ON sj_system_user_permission (namespace_id, group_name, system_user_id)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'系统用户id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission',
     'COLUMN', N'system_user_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'系统用户权限表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_system_user_permission'
GO

-- sj_sequence_alloc
CREATE TABLE sj_sequence_alloc
(
    id           bigint       NOT NULL PRIMARY KEY IDENTITY,
    namespace_id nvarchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   nvarchar(64) NOT NULL DEFAULT '',
    max_id       bigint       NOT NULL DEFAULT 1,
    step         int          NOT NULL DEFAULT 100,
    update_dt    datetime2    NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_sequence_alloc_01 ON sj_sequence_alloc (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'最大id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc',
     'COLUMN', N'max_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'步长',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc',
     'COLUMN', N'step'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'更新时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'号段模式序号ID分配表',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_sequence_alloc'
GO

-- sj_job
CREATE TABLE sj_job
(
    id               bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       nvarchar(64)  NOT NULL,
    job_name         nvarchar(64)  NOT NULL,
    args_str         nvarchar(max) NULL     DEFAULT NULL,
    args_type        tinyint       NOT NULL DEFAULT 1,
    next_trigger_at  bigint        NOT NULL,
    job_status       tinyint       NOT NULL DEFAULT 1,
    task_type        tinyint       NOT NULL DEFAULT 1,
    route_key        tinyint       NOT NULL DEFAULT 4,
    executor_type    tinyint       NOT NULL DEFAULT 1,
    executor_info    nvarchar(255) NULL     DEFAULT NULL,
    trigger_type     tinyint       NOT NULL,
    trigger_interval nvarchar(255) NOT NULL,
    block_strategy   tinyint       NOT NULL DEFAULT 1,
    executor_timeout int           NOT NULL DEFAULT 0,
    max_retry_times  int           NOT NULL DEFAULT 0,
    parallel_num     int           NOT NULL DEFAULT 1,
    retry_interval   int           NOT NULL DEFAULT 0,
    bucket_index     int           NOT NULL DEFAULT 0,
    resident         tinyint       NOT NULL DEFAULT 0,
    description      nvarchar(256) NOT NULL DEFAULT '',
    ext_attrs        nvarchar(256) NULL     DEFAULT '',
    deleted          tinyint       NOT NULL DEFAULT 0,
    create_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_job_01 ON sj_job (namespace_id, group_name)
GO
CREATE INDEX idx_sj_job_02 ON sj_job (job_status, bucket_index)
GO
CREATE INDEX idx_sj_job_03 ON sj_job (create_dt)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'job_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'参数类型 ',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'args_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'下次触发时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'next_trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务状态 0、关闭、1、开启',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'job_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、集群 2、广播 3、切片',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'路由策略',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'route_key'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器类型',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'executor_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'executor_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'触发类型 1.CRON 表达式 2. 固定时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'trigger_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'间隔时长',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'trigger_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'阻塞策略 1、丢弃 2、覆盖 3、并行',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'block_strategy'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行超时时间，单位秒',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'executor_timeout'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'最大重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'max_retry_times'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'并行数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'parallel_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试间隔 ( s ) ',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'retry_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'bucket',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'bucket_index'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'是否是常驻任务',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'resident'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job'
GO

INSERT INTO sj_job (namespace_id, group_name, job_name, args_str, args_type, next_trigger_at, job_status, task_type, route_key, executor_type, executor_info, trigger_type, trigger_interval, block_strategy,executor_timeout, max_retry_times, parallel_num, retry_interval, bucket_index, resident, description, ext_attrs, deleted, create_dt, update_dt) VALUES (N'dev', N'ruoyi_group', N'demo-job', null, 1, 1710344035622, 1, 1, 4, 1, N'testJobExecutor', 2, N'60', 1, 60, 3, 1, 1, 116, 0, N'', N'', 0, getdate(), getdate())
GO

-- sj_job_log_message
CREATE TABLE sj_job_log_message
(
    id            bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    nvarchar(64)  NOT NULL,
    job_id        bigint        NOT NULL,
    task_batch_id bigint        NOT NULL,
    task_id       bigint        NOT NULL,
    message       nvarchar(max) NOT NULL,
    log_num       int           NOT NULL DEFAULT 1,
    real_time     bigint        NOT NULL DEFAULT 0,
    ext_attrs     nvarchar(256) NULL     DEFAULT '',
    create_dt     datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_job_log_message_01 ON sj_job_log_message (task_batch_id, task_id)
GO
CREATE INDEX idx_sj_job_log_message_02 ON sj_job_log_message (create_dt)
GO
CREATE INDEX idx_sj_job_log_message_03 ON sj_job_log_message (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'task_batch_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'task_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度信息',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'message'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'log_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'上报时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'real_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度日志',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_log_message'
GO

-- sj_job_task
CREATE TABLE sj_job_task
(
    id             bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id   nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name     nvarchar(64)  NOT NULL,
    job_id         bigint        NOT NULL,
    task_batch_id  bigint        NOT NULL,
    parent_id      bigint        NOT NULL DEFAULT 0,
    task_status    tinyint       NOT NULL DEFAULT 0,
    retry_count    int           NOT NULL DEFAULT 0,
    mr_stage       tinyint       NULL     DEFAULT NULL,
    leaf           tinyint       NOT NULL DEFAULT '1',
    task_name      nvarchar(255) NOT NULL DEFAULT '',
    client_info    nvarchar(128) NULL     DEFAULT NULL,
    wf_context     nvarchar(max) NULL     DEFAULT NULL,
    result_message nvarchar(max) NOT NULL,
    args_str       nvarchar(max) NULL     DEFAULT NULL,
    args_type      tinyint       NOT NULL DEFAULT 1,
    ext_attrs      nvarchar(256) NULL     DEFAULT '',
    create_dt      datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_job_task_01 ON sj_job_task (task_batch_id, task_status)
GO
CREATE INDEX idx_sj_job_task_02 ON sj_job_task (create_dt)
GO
CREATE INDEX idx_sj_job_task_03 ON sj_job_task (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'task_batch_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'父执行器id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'parent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行的状态 0、失败 1、成功',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'task_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'retry_count'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'动态分片所处阶段 1:map 2:reduce 3:mergeReduce',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'mr_stage'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'叶子节点',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'leaf'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'task_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'客户端地址 clientId#ip:port',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'client_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流全局上下文',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'wf_context'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行结果',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'result_message'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'参数类型 ',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'args_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务实例',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task'
GO

-- sj_job_task_batch
CREATE TABLE sj_job_task_batch
(
    id                      bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id            nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name              nvarchar(64)  NOT NULL,
    job_id                  bigint        NOT NULL,
    workflow_node_id        bigint        NOT NULL DEFAULT 0,
    parent_workflow_node_id bigint        NOT NULL DEFAULT 0,
    workflow_task_batch_id  bigint        NOT NULL DEFAULT 0,
    task_batch_status       tinyint       NOT NULL DEFAULT 0,
    operation_reason        tinyint       NOT NULL DEFAULT 0,
    execution_at            bigint        NOT NULL DEFAULT 0,
    system_task_type        tinyint       NOT NULL DEFAULT 3,
    parent_id               nvarchar(64)  NOT NULL DEFAULT '',
    ext_attrs               nvarchar(256) NULL     DEFAULT '',
    deleted                 tinyint       NOT NULL DEFAULT 0,
    create_dt               datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt               datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_job_task_batch_01 ON sj_job_task_batch (job_id, task_batch_status)
GO
CREATE INDEX idx_sj_job_task_batch_02 ON sj_job_task_batch (create_dt)
GO
CREATE INDEX idx_sj_job_task_batch_03 ON sj_job_task_batch (namespace_id, group_name)
GO
CREATE INDEX idx_sj_job_task_batch_04 ON sj_job_task_batch (workflow_task_batch_id, workflow_node_id)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流节点id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'workflow_node_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流任务父批次id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'parent_workflow_node_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流任务批次id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'workflow_task_batch_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次状态 0、失败 1、成功',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'task_batch_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'操作原因',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'operation_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'execution_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 3、JOB任务 4、WORKFLOW任务',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'system_task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'父节点',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'parent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_task_batch'
GO

-- sj_job_summary
CREATE TABLE sj_job_summary
(
    id               bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       nvarchar(64)  NOT NULL DEFAULT '',
    business_id      bigint        NOT NULL,
    system_task_type tinyint       NOT NULL DEFAULT 3,
    trigger_at       datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success_num      int           NOT NULL DEFAULT 0,
    fail_num         int           NOT NULL DEFAULT 0,
    fail_reason      nvarchar(512) NOT NULL DEFAULT '',
    stop_num         int           NOT NULL DEFAULT 0,
    stop_reason      nvarchar(512) NOT NULL DEFAULT '',
    cancel_num       int           NOT NULL DEFAULT 0,
    cancel_reason    nvarchar(512) NOT NULL DEFAULT '',
    create_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_job_summary_01 ON sj_job_summary (trigger_at, system_task_type, business_id)
GO

CREATE INDEX idx_sj_job_summary_01 ON sj_job_summary (namespace_id, group_name, business_id)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务id  ( job_id或workflow_id ) ',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'business_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 3、JOB任务 4、WORKFLOW任务',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'system_task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'统计时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行成功-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'success_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行失败-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'fail_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败原因',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'fail_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行失败-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'stop_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败原因',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'stop_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行失败-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'cancel_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败原因',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'cancel_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'DashBoard_Job',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_job_summary'
GO

-- sj_retry_summary
CREATE TABLE sj_retry_summary
(
    id            bigint       NOT NULL PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    nvarchar(64) NOT NULL DEFAULT '',
    scene_name    nvarchar(50) NOT NULL DEFAULT '',
    trigger_at    datetime2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    running_num   int          NOT NULL DEFAULT 0,
    finish_num    int          NOT NULL DEFAULT 0,
    max_count_num int          NOT NULL DEFAULT 0,
    suspend_num   int          NOT NULL DEFAULT 0,
    create_dt     datetime2    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     datetime2    NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE UNIQUE INDEX uk_sj_retry_summary_01 ON sj_retry_summary (namespace_id, group_name, scene_name, trigger_at)
GO

CREATE INDEX idx_sj_retry_summary_01 ON sj_retry_summary (trigger_at)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'统计时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试中-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'running_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试完成-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'finish_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试到达最大次数-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'max_count_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'暂停重试-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'suspend_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'DashBoard_Retry',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_retry_summary'
GO

-- sj_workflow
CREATE TABLE sj_workflow
(
    id               bigint        NOT NULL PRIMARY KEY IDENTITY,
    workflow_name    nvarchar(64)  NOT NULL,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       nvarchar(64)  NOT NULL,
    workflow_status  tinyint       NOT NULL DEFAULT 1,
    trigger_type     tinyint       NOT NULL,
    trigger_interval nvarchar(255) NOT NULL,
    next_trigger_at  bigint        NOT NULL,
    block_strategy   tinyint       NOT NULL DEFAULT 1,
    executor_timeout int           NOT NULL DEFAULT 0,
    description      nvarchar(256) NOT NULL DEFAULT '',
    flow_info        nvarchar(max) NULL     DEFAULT NULL,
    wf_context       nvarchar(max) NULL     DEFAULT NULL,
    bucket_index     int           NOT NULL DEFAULT 0,
    version          int           NOT NULL,
    ext_attrs        nvarchar(256) NULL     DEFAULT '',
    deleted          tinyint       NOT NULL DEFAULT 0,
    create_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_workflow_01 ON sj_workflow (create_dt)
GO
CREATE INDEX idx_sj_workflow_02 ON sj_workflow (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'workflow_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流状态 0、关闭、1、开启',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'workflow_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'触发类型 1.CRON 表达式 2. 固定时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'trigger_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'间隔时长',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'trigger_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'下次触发时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'next_trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'阻塞策略 1、丢弃 2、覆盖 3、并行',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'block_strategy'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行超时时间，单位秒',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'executor_timeout'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'流程信息',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'flow_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'上下文',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'wf_context'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'bucket',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'bucket_index'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow'
GO

-- sj_workflow_node
CREATE TABLE sj_workflow_node
(
    id                   bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id         nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    node_name            nvarchar(64)  NOT NULL,
    group_name           nvarchar(64)  NOT NULL,
    job_id               bigint        NOT NULL,
    workflow_id          bigint        NOT NULL,
    node_type            tinyint       NOT NULL DEFAULT 1,
    expression_type      tinyint       NOT NULL DEFAULT 0,
    fail_strategy        tinyint       NOT NULL DEFAULT 1,
    workflow_node_status tinyint       NOT NULL DEFAULT 1,
    priority_level       int           NOT NULL DEFAULT 1,
    node_info            nvarchar(max) NULL     DEFAULT NULL,
    version              int           NOT NULL,
    ext_attrs            nvarchar(256) NULL     DEFAULT '',
    deleted              tinyint       NOT NULL DEFAULT 0,
    create_dt            datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt            datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_workflow_node_01 ON sj_workflow_node (create_dt)
GO
CREATE INDEX idx_sj_workflow_node_02 ON sj_workflow_node (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'节点名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流ID',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'workflow_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'1、任务节点 2、条件节点',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'1、SpEl、2、Aviator 3、QL',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'expression_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败策略 1、跳过 2、阻塞',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'fail_strategy'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流节点状态 0、关闭、1、开启',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'workflow_node_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'优先级',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'priority_level'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'节点信息 ',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'node_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流节点',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_node'
GO

-- sj_workflow_task_batch
CREATE TABLE sj_workflow_task_batch
(
    id                bigint        NOT NULL PRIMARY KEY IDENTITY,
    namespace_id      nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        nvarchar(64)  NOT NULL,
    workflow_id       bigint        NOT NULL,
    task_batch_status tinyint       NOT NULL DEFAULT 0,
    operation_reason  tinyint       NOT NULL DEFAULT 0,
    flow_info         nvarchar(max) NULL     DEFAULT NULL,
    wf_context        nvarchar(max) NULL     DEFAULT NULL,
    execution_at      bigint        NOT NULL DEFAULT 0,
    ext_attrs         nvarchar(256) NULL     DEFAULT '',
    version           int           NOT NULL DEFAULT 1,
    deleted           tinyint       NOT NULL DEFAULT 0,
    create_dt         datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         datetime2     NOT NULL DEFAULT CURRENT_TIMESTAMP
)
GO

CREATE INDEX idx_sj_workflow_task_batch_01 ON sj_workflow_task_batch (workflow_id, task_batch_status)
GO
CREATE INDEX idx_sj_workflow_task_batch_02 ON sj_workflow_task_batch (create_dt)
GO
CREATE INDEX idx_sj_workflow_task_batch_03 ON sj_workflow_task_batch (namespace_id, group_name)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'workflow_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次状态 0、失败 1、成功',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'task_batch_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'操作原因',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'operation_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'流程信息',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'flow_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'全局上下文',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'wf_context'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'execution_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流批次',
     'SCHEMA', N'dbo',
     'TABLE', N'sj_workflow_task_batch'
GO

