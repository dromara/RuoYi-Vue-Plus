-- er_distributed_lock
CREATE TABLE er_distributed_lock
(
    id         bigint PRIMARY KEY IDENTITY,
    name       nvarchar(64)  NOT NULL,
    lock_until datetime2     NOT NULL DEFAULT GETDATE(),
    locked_at  datetime2     NOT NULL DEFAULT GETDATE(),
    locked_by  nvarchar(255) NOT NULL,
    create_dt  datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt  datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_name ON distributed_lock (name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁名称',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定时长',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'lock_until'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定时间',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'locked_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定者',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'locked_by'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'锁定表',
     'SCHEMA', N'dbo',
     'TABLE', N'distributed_lock'
GO

-- er_group_config
CREATE TABLE er_group_config
(
    id                bigint PRIMARY KEY IDENTITY,
    namespace_id      nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        nvarchar(64)  NOT NULL DEFAULT '',
    description       nvarchar(256) NOT NULL DEFAULT '',
    token             nvarchar(64)  NOT NULL DEFAULT 'ER_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',
    group_status      tinyint       NOT NULL DEFAULT '0',
    version           int           NOT NULL,
    group_partition   int           NOT NULL,
    id_generator_mode tinyint       NOT NULL DEFAULT '1',
    init_scene        tinyint       NOT NULL DEFAULT '0',
    bucket_index      int           NOT NULL DEFAULT '0',
    create_dt         datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt         datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_namespace_id_group_name ON group_config (namespace_id ASC, group_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组描述',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'token',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'token'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'group_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'分区',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'group_partition'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'唯一id生成模式 默认号段模式',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'id_generator_mode'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'是否初始化场景 0:否 1:是',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'init_scene'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'bucket',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'bucket_index'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组配置',
     'SCHEMA', N'dbo',
     'TABLE', N'group_config'
GO

INSERT INTO er_group_config VALUES (N'1', N'dev', N'ruoyi_group', N'', N'ER_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', N'1', N'1', N'0', N'1', N'1', N'4', getdate(), getdate());
GO

-- er_job
CREATE TABLE er_job
(
    id               bigint PRIMARY KEY IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       nvarchar(64)  NOT NULL,
    job_name         nvarchar(64)  NOT NULL,
    args_str         nvarchar(max) NULL,
    args_type        tinyint       NOT NULL DEFAULT '1',
    next_trigger_at  bigint        NOT NULL,
    job_status       tinyint       NOT NULL DEFAULT '1',
    task_type        tinyint       NOT NULL DEFAULT '1',
    route_key        tinyint       NOT NULL DEFAULT '4',
    executor_type    tinyint       NOT NULL DEFAULT '1',
    executor_info    nvarchar(255) NULL     DEFAULT '',
    trigger_type     tinyint       NOT NULL,
    trigger_interval nvarchar(255) NOT NULL,
    block_strategy   tinyint       NOT NULL DEFAULT '1',
    executor_timeout int           NOT NULL DEFAULT '0',
    max_retry_times  int           NOT NULL DEFAULT '0',
    parallel_num     int           NOT NULL DEFAULT '1',
    retry_interval   int           NOT NULL DEFAULT '0',
    bucket_index     int           NOT NULL DEFAULT '0',
    resident         tinyint       NOT NULL DEFAULT '0',
    description      nvarchar(256) NOT NULL DEFAULT '',
    ext_attrs        nvarchar(256) NULL     DEFAULT '',
    create_dt        datetime2              DEFAULT GETDATE(),
    update_dt        datetime2              DEFAULT GETDATE(),
    deleted          BIT           NOT NULL DEFAULT 0
)
GO

CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON job (namespace_id ASC, group_name ASC)
GO
CREATE NONCLUSTERED INDEX idx_job_status_bucket_index ON job (job_status ASC, bucket_index ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON job (create_dt ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'job_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'参数类型 ',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'args_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'下次触发时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'next_trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务状态 0、关闭、1、开启',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'job_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、集群 2、广播 3、切片',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'路由策略',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'route_key'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器类型',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'executor_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'executor_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'触发类型 1.CRON 表达式 2. 固定时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'trigger_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'间隔时长',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'trigger_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'阻塞策略 1、丢弃 2、覆盖 3、并行',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'block_strategy'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行超时时间，单位秒',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'executor_timeout'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'最大重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'max_retry_times'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'并行数',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'parallel_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试间隔(s)',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'retry_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'bucket',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'bucket_index'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'是否是常驻任务',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'resident'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'job',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息',
     'SCHEMA', N'dbo',
     'TABLE', N'job'
GO


-- er_job_log_message
CREATE TABLE er_job_log_message
(
    id            bigint PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    nvarchar(64)  NOT NULL,
    job_id        bigint        NOT NULL,
    task_batch_id bigint        NOT NULL,
    task_id       bigint        NOT NULL,
    message       nvarchar(max) NOT NULL,
    log_num       int           NOT NULL DEFAULT '1',
    real_time     bigint        NOT NULL DEFAULT '0',
    create_dt     datetime2     NOT NULL DEFAULT GETDATE(),
    ext_attrs     nvarchar(256) NULL     DEFAULT ''
)
GO

CREATE NONCLUSTERED INDEX idx_task_batch_id_task_id ON job_log_message (task_batch_id ASC, task_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON job_log_message (create_dt ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON job_log_message (namespace_id ASC, group_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'task_batch_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'task_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度信息',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'message'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'log_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'上报时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'real_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度日志',
     'SCHEMA', N'dbo',
     'TABLE', N'job_log_message'
GO


-- er_job_notify_config
CREATE TABLE er_job_notify_config
(
    id                     bigint PRIMARY KEY IDENTITY,
    namespace_id           nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             nvarchar(64)  NOT NULL,
    job_id                 bigint        NOT NULL,
    notify_status          tinyint       NOT NULL DEFAULT '0',
    notify_type            tinyint       NOT NULL DEFAULT '0',
    notify_attribute       nvarchar(512) NOT NULL,
    notify_threshold       int           NOT NULL DEFAULT '0',
    notify_scene           tinyint       NOT NULL DEFAULT '0',
    rate_limiter_status    tinyint       NOT NULL DEFAULT '0',
    rate_limiter_threshold int           NOT NULL DEFAULT '0',
    description            nvarchar(256) NOT NULL DEFAULT '',
    create_dt              datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt              datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_job_id ON job_notify_config (namespace_id ASC, group_name ASC, job_id ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'notify_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知类型 1、钉钉 2、邮件 3、企业微信',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'notify_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'配置属性',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'notify_attribute'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知阈值',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'notify_threshold'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知场景',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'notify_scene'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'限流状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'rate_limiter_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'每秒限流阈值',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'rate_limiter_threshold'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'job通知配置',
     'SCHEMA', N'dbo',
     'TABLE', N'job_notify_config'
GO


-- er_job_summary
CREATE TABLE er_job_summary
(
    id               bigint PRIMARY KEY IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       nvarchar(64)  NOT NULL,
    business_id      bigint        NOT NULL,
    system_task_type tinyint       NOT NULL DEFAULT '3',
    trigger_at       datetime2     NOT NULL,
    success_num      int           NOT NULL DEFAULT '0',
    fail_num         int           NOT NULL DEFAULT '0',
    fail_reason      nvarchar(512) NOT NULL DEFAULT '',
    stop_num         int           NOT NULL DEFAULT '0',
    stop_reason      nvarchar(512) NOT NULL DEFAULT '',
    cancel_num       int           NOT NULL DEFAULT '0',
    cancel_reason    nvarchar(512) NOT NULL DEFAULT '',
    create_dt        datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt        datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_trigger_at_system_task_type_business_id ON job_summary (trigger_at ASC, system_task_type ASC, business_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_business_id ON job_summary (namespace_id ASC, group_name ASC, business_id ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务id (job_id或workflow_id)',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'business_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 3、JOB任务 4、WORKFLOW任务',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'system_task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'统计时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行成功-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'success_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行失败-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'fail_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败原因',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'fail_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行失败-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'stop_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败原因',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'stop_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行失败-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'cancel_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败原因',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'cancel_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'DashBoard_Job',
     'SCHEMA', N'dbo',
     'TABLE', N'job_summary'
GO


-- er_job_task
CREATE TABLE er_job_task
(
    id             bigint PRIMARY KEY IDENTITY,
    namespace_id   nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name     nvarchar(64)  NOT NULL,
    job_id         bigint        NOT NULL,
    task_batch_id  bigint        NOT NULL,
    parent_id      bigint        NOT NULL DEFAULT '0',
    task_status    tinyint       NOT NULL DEFAULT '0',
    retry_count    int           NOT NULL DEFAULT '0',
    client_info    nvarchar(128) NULL,
    result_message nvarchar(max) NOT NULL,
    args_str       nvarchar(max) NULL,
    args_type      tinyint       NOT NULL DEFAULT '1',
    ext_attrs      nvarchar(256) NULL     DEFAULT '',
    create_dt      datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt      datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE NONCLUSTERED INDEX idx_task_batch_id_task_status ON job_task (task_batch_id ASC, task_status ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON job_task (create_dt ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON job_task (namespace_id ASC, group_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'调度任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'task_batch_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'父执行器id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'parent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行的状态 0、失败 1、成功',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'task_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'retry_count'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'客户端地址 clientId#ip:port',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'client_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行结果',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'result_message'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'参数类型 ',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'args_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务实例',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task'
GO

-- er_job_task_batch
CREATE TABLE er_job_task_batch
(
    id                      bigint PRIMARY KEY IDENTITY,
    namespace_id            nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name              nvarchar(64)  NOT NULL,
    job_id                  bigint        NOT NULL,
    workflow_node_id        bigint        NOT NULL DEFAULT '0',
    parent_workflow_node_id bigint        NOT NULL DEFAULT '0',
    workflow_task_batch_id  bigint        NOT NULL DEFAULT '0',
    task_batch_status       tinyint       NOT NULL DEFAULT '0',
    operation_reason        tinyint       NOT NULL DEFAULT '0',
    execution_at            bigint        NOT NULL DEFAULT '0',
    system_task_type        tinyint       NOT NULL DEFAULT '3',
    parent_id               nvarchar(64)  NOT NULL DEFAULT '',
    create_dt               datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt               datetime2     NOT NULL DEFAULT GETDATE(),
    deleted                 BIT                    DEFAULT 0,
    ext_attrs               nvarchar(256) NULL     DEFAULT ''
)
GO

CREATE NONCLUSTERED INDEX idx_job_id_task_batch_status ON job_task_batch (job_id ASC, task_batch_status ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON job_task_batch (create_dt ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON job_task_batch (namespace_id ASC, group_name ASC)
GO
CREATE NONCLUSTERED INDEX idx_workflow_task_batch_id_workflow_node_id ON job_task_batch (workflow_task_batch_id ASC, workflow_node_id ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流节点id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'workflow_node_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流任务父批次id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'parent_workflow_node_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流任务批次id',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'workflow_task_batch_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次状态 0、失败 1、成功',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'task_batch_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'操作原因',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'operation_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'execution_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 3、JOB任务 4、WORKFLOW任务',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'system_task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'父节点',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'parent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次',
     'SCHEMA', N'dbo',
     'TABLE', N'job_task_batch'
GO

-- er_namespace
CREATE TABLE er_namespace
(
    id          bigint PRIMARY KEY IDENTITY,
    name        nvarchar(64)  NOT NULL,
    unique_id   nvarchar(64)  NOT NULL,
    description nvarchar(256) NOT NULL DEFAULT '',
    create_dt   datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt   datetime2     NOT NULL DEFAULT GETDATE(),
    deleted     BIT           NOT NULL DEFAULT 0
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_unique_id ON namespace (unique_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_name ON namespace (name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'名称',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'唯一id',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间',
     'SCHEMA', N'dbo',
     'TABLE', N'namespace'
GO

INSERT INTO er_namespace VALUES (N'1', N'Development', N'dev', N'', getdate(), getdate(), N'0');
INSERT INTO er_namespace VALUES (N'2', N'Production', N'prod', N'', getdate(), getdate(), N'0');
GO

-- er_notify_config
CREATE TABLE er_notify_config
(
    id                     bigint PRIMARY KEY IDENTITY,
    namespace_id           nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             nvarchar(64)  NOT NULL,
    scene_name             nvarchar(64)  NOT NULL,
    notify_status          tinyint       NOT NULL DEFAULT '0',
    notify_type            tinyint       NOT NULL DEFAULT '0',
    notify_attribute       nvarchar(512) NOT NULL,
    notify_threshold       int           NOT NULL DEFAULT '0',
    notify_scene           tinyint       NOT NULL DEFAULT '0',
    rate_limiter_status    tinyint       NOT NULL DEFAULT '0',
    rate_limiter_threshold int           NOT NULL DEFAULT '0',
    description            nvarchar(256) NOT NULL DEFAULT '',
    create_dt              datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt              datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_scene_name ON notify_config (namespace_id ASC, group_name ASC, scene_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'notify_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知类型 1、钉钉 2、邮件 3、企业微信',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'notify_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'配置属性',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'notify_attribute'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知阈值',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'notify_threshold'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知场景',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'notify_scene'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'限流状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'rate_limiter_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'每秒限流阈值',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'rate_limiter_threshold'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'通知配置',
     'SCHEMA', N'dbo',
     'TABLE', N'notify_config'
GO

-- er_retry_dead_letter_0
CREATE TABLE er_retry_dead_letter_0
(
    id            bigint PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    unique_id     nvarchar(64)  NOT NULL,
    group_name    nvarchar(64)  NOT NULL,
    scene_name    nvarchar(64)  NOT NULL,
    idempotent_id nvarchar(64)  NOT NULL,
    biz_no        nvarchar(64)  NOT NULL DEFAULT '',
    executor_name nvarchar(512) NOT NULL DEFAULT '',
    args_str      nvarchar(max) NOT NULL,
    ext_attrs     nvarchar(max) NOT NULL,
    task_type     tinyint       NOT NULL DEFAULT '1',
    create_dt     datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_namespace_id_group_name_unique_id ON retry_dead_letter_0 (namespace_id ASC, group_name ASC, unique_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_scene_name ON retry_dead_letter_0 (namespace_id ASC, group_name ASC, scene_name ASC)
GO
CREATE NONCLUSTERED INDEX idx_idempotent_id ON retry_dead_letter_0 (idempotent_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_biz_no ON retry_dead_letter_0 (biz_no ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON retry_dead_letter_0 (create_dt ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'幂等id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'idempotent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务编号',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'biz_no'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'executor_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、重试数据 2、回调数据',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'死信队列表',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_dead_letter_0'
GO


-- er_retry_summary
CREATE TABLE er_retry_summary
(
    id            bigint PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    nvarchar(64) NOT NULL DEFAULT '',
    scene_name    nvarchar(50) NOT NULL DEFAULT '',
    trigger_at    datetime2    NOT NULL DEFAULT GETDATE(),
    running_num   int          NOT NULL DEFAULT '0',
    finish_num    int          NOT NULL DEFAULT '0',
    max_count_num int          NOT NULL DEFAULT '0',
    suspend_num   int          NOT NULL DEFAULT '0',
    create_dt     datetime2    NOT NULL DEFAULT GETDATE(),
    update_dt     datetime2    NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_scene_name_trigger_at ON retry_summary (namespace_id ASC, group_name ASC,
                                                                            scene_name ASC, trigger_at ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'统计时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试中-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'running_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试完成-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'finish_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试到达最大次数-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'max_count_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'暂停重试-日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'suspend_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'DashBoard_Retry',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_summary'
GO

-- er_retry_task_0
CREATE TABLE er_retry_task_0
(
    id              bigint PRIMARY KEY IDENTITY,
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
    retry_count     int           NOT NULL DEFAULT '0',
    retry_status    tinyint       NOT NULL DEFAULT '0',
    task_type       tinyint       NOT NULL DEFAULT '1',
    create_dt       datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt       datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_name_unique_id ON retry_task_0 (namespace_id ASC, group_name ASC, unique_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_scene_name ON retry_task_0 (namespace_id ASC, group_name ASC, scene_name ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_task_type ON retry_task_0 (namespace_id ASC, group_name ASC, task_type ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_retry_status ON retry_task_0 (namespace_id ASC, group_name ASC, retry_status ASC)
GO
CREATE NONCLUSTERED INDEX idx_idempotent_id ON retry_task_0 (idempotent_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_biz_no ON retry_task_0 (biz_no ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON retry_task_0 (create_dt ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'幂等id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'idempotent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务编号',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'biz_no'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'executor_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'下次触发时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'next_trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'retry_count'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试状态 0、重试中 1、成功 2、最大重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'retry_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、重试数据 2、回调数据',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务表',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_0'
GO

-- er_retry_task_log
CREATE TABLE er_retry_task_log
(
    id            bigint PRIMARY KEY IDENTITY,
    namespace_id  nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    unique_id     nvarchar(64)  NOT NULL,
    group_name    nvarchar(64)  NOT NULL,
    scene_name    nvarchar(64)  NOT NULL,
    idempotent_id nvarchar(64)  NOT NULL,
    biz_no        nvarchar(64)  NOT NULL DEFAULT '',
    executor_name nvarchar(512) NOT NULL DEFAULT '',
    args_str      nvarchar(max) NOT NULL,
    ext_attrs     nvarchar(max) NOT NULL,
    retry_status  tinyint       NOT NULL DEFAULT '0',
    task_type     tinyint       NOT NULL DEFAULT '1',
    create_dt     datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt     datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE NONCLUSTERED INDEX idx_group_name_scene_name ON retry_task_log (namespace_id ASC, group_name ASC, scene_name ASC)
GO
CREATE NONCLUSTERED INDEX idx_retry_status ON retry_task_log (retry_status ASC)
GO
CREATE NONCLUSTERED INDEX idx_idempotent_id ON retry_task_log (idempotent_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_unique_id ON retry_task_log (unique_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_biz_no ON retry_task_log (biz_no ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON retry_task_log (create_dt ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'幂等id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'idempotent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'业务编号',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'biz_no'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行器名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'executor_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'执行方法参数',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'args_str'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'重试状态 0、重试中 1、成功 2、最大次数',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'retry_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务类型 1、重试数据 2、回调数据',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'task_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务日志基础信息表',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log'
GO

-- er_retry_task_log_message
CREATE TABLE er_retry_task_log_message
(
    id           bigint PRIMARY KEY IDENTITY,
    namespace_id nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   nvarchar(64)  NOT NULL,
    unique_id    nvarchar(64)  NOT NULL,
    create_dt    datetime2     NOT NULL DEFAULT GETDATE(),
    message      nvarchar(max) NOT NULL,
    log_num      int           NOT NULL DEFAULT 1,
    real_time    bigint        NOT NULL DEFAULT 0
)
GO

CREATE NONCLUSTERED INDEX idx_namespace_id_group_name_scene_name ON retry_task_log_message (namespace_id ASC, group_name ASC, unique_id ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON retry_task_log_message (create_dt ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'同组下id唯一',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'unique_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'异常信息',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'message'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'日志数量',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'log_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'上报时间',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message',
     'COLUMN', N'real_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务调度日志信息记录表',
     'SCHEMA', N'dbo',
     'TABLE', N'retry_task_log_message'
GO

-- er_scene_config
CREATE TABLE er_scene_config
(
    id               bigint IDENTITY,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    scene_name       nvarchar(64)  NOT NULL,
    group_name       nvarchar(64)  NOT NULL,
    scene_status     tinyint       NOT NULL DEFAULT '0',
    max_retry_count  int           NOT NULL DEFAULT '5',
    back_off         tinyint       NOT NULL DEFAULT '1',
    trigger_interval nvarchar(16)  NOT NULL DEFAULT '',
    deadline_request bigint        NOT NULL DEFAULT '60000',
    executor_timeout int           NOT NULL DEFAULT '5',
    route_key        tinyint       NOT NULL DEFAULT '4',
    description      nvarchar(256) NOT NULL DEFAULT '',
    create_dt        datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt        datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_namespace_id_group_name_scene_name ON scene_config (namespace_id ASC, group_name ASC, scene_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景名称',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'scene_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组状态 0、未启用 1、启用',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'scene_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'最大重试次数',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'max_retry_count'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'1、默认等级 2、固定间隔时间 3、CRON 表达式',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'back_off'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'间隔时长',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'trigger_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'Deadline Request 调用链超时 单位毫秒',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'deadline_request'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行超时时间，单位秒',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'executor_timeout'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'路由策略',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'route_key'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'场景配置',
     'SCHEMA', N'dbo',
     'TABLE', N'scene_config'
GO

-- er_sequence_alloc
CREATE TABLE er_sequence_alloc
(
    id           bigint PRIMARY KEY IDENTITY,
    namespace_id nvarchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   nvarchar(64) NOT NULL DEFAULT '',
    max_id       bigint       NOT NULL DEFAULT '1',
    step         int          NOT NULL DEFAULT '100',
    update_dt    datetime2    NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_namespace_id_group_name ON sequence_alloc (namespace_id ASC, group_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'最大id',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc',
     'COLUMN', N'max_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'步长',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc',
     'COLUMN', N'step'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'更新时间',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'号段模式序号ID分配表',
     'SCHEMA', N'dbo',
     'TABLE', N'sequence_alloc'
GO

-- er_server_node
CREATE TABLE er_server_node
(
    id           bigint PRIMARY KEY IDENTITY,
    namespace_id nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   nvarchar(64)  NOT NULL,
    host_id      nvarchar(64)  NOT NULL,
    host_ip      nvarchar(64)  NOT NULL,
    context_path nvarchar(256) NOT NULL DEFAULT '/',
    host_port    int           NOT NULL,
    expire_at    datetime2     NOT NULL,
    node_type    tinyint       NOT NULL,
    ext_attrs    nvarchar(256) NULL     DEFAULT '',
    create_dt    datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt    datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_host_id_host_ip ON server_node (host_id ASC, host_ip ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON server_node (namespace_id ASC, group_name ASC)
GO
CREATE NONCLUSTERED INDEX idx_expire_at_node_type ON server_node (expire_at ASC, node_type ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主机id',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'host_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'机器ip',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'host_ip'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'客户端上下文路径 server.servlet.context-path',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'context_path'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'机器端口',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'host_port'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'过期时间',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'expire_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'节点类型 1、客户端 2、是服务端',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'服务器节点',
     'SCHEMA', N'dbo',
     'TABLE', N'server_node'
GO

-- er_system_user_
CREATE TABLE er_system_user_
(
    id        bigint PRIMARY KEY IDENTITY,
    username  nvarchar(64)  NOT NULL,
    password  nvarchar(128) NOT NULL,
    role      tinyint       NOT NULL DEFAULT '0',
    create_dt datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt datetime2     NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_username ON system_user_ (username ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'账号',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_',
     'COLUMN', N'username'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'密码',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_',
     'COLUMN', N'password'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'角色：1-普通用户、2-管理员',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_',
     'COLUMN', N'role'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'系统用户表',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_'
GO

-- pwd: admin
INSERT INTO er_system_user_ VALUES (N'1', N'admin', N'465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', N'2', getdate(), getdate());
GO

-- er_system_user_permission
CREATE TABLE er_system_user_permission
(
    id             bigint PRIMARY KEY IDENTITY,
    group_name     nvarchar(64) NOT NULL,
    namespace_id   nvarchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    system_user_id bigint       NOT NULL,
    create_dt      datetime2    NOT NULL DEFAULT GETDATE(),
    update_dt      datetime2    NOT NULL DEFAULT GETDATE()
)
GO

CREATE UNIQUE NONCLUSTERED INDEX uk_namespace_id_group_name_system_user_id ON system_user_permission (namespace_id ASC, group_name ASC, system_user_id ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'系统用户id',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission',
     'COLUMN', N'system_user_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'系统用户权限表',
     'SCHEMA', N'dbo',
     'TABLE', N'system_user_permission'
GO


-- er_workflow
CREATE TABLE er_workflow
(
    id               bigint IDENTITY,
    workflow_name    nvarchar(64)  NOT NULL,
    namespace_id     nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       nvarchar(64)  NOT NULL,
    workflow_status  tinyint       NOT NULL DEFAULT '1',
    trigger_type     tinyint       NOT NULL,
    trigger_interval nvarchar(255) NOT NULL,
    next_trigger_at  bigint        NOT NULL,
    block_strategy   tinyint       NOT NULL DEFAULT '1',
    executor_timeout int           NOT NULL DEFAULT '0',
    description      nvarchar(256) NOT NULL DEFAULT '',
    flow_info        nvarchar(max) NULL     DEFAULT NULL,
    bucket_index     int           NOT NULL DEFAULT '0',
    version          int           NOT NULL,
    create_dt        datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt        datetime2     NOT NULL DEFAULT GETDATE(),
    deleted          BIT                    DEFAULT 0,
    ext_attrs        nvarchar(256) NULL
)
GO

CREATE NONCLUSTERED INDEX idx_create_dt ON workflow (create_dt ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON workflow (namespace_id ASC, group_name ASC)
GO


EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流名称',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'workflow_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流状态 0、关闭、1、开启',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'workflow_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'触发类型 1.CRON 表达式 2. 固定时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'trigger_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'间隔时长',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'trigger_interval'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'下次触发时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'next_trigger_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'阻塞策略 1、丢弃 2、覆盖 3、并行',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'block_strategy'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行超时时间，单位秒',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'executor_timeout'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'描述',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'description'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'流程信息',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'flow_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'bucket',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'bucket_index'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow'
GO

-- er_workflow_node
CREATE TABLE er_workflow_node
(
    id                   bigint PRIMARY KEY IDENTITY,
    namespace_id         nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    node_name            nvarchar(64)  NOT NULL,
    group_name           nvarchar(64)  NOT NULL,
    job_id               bigint        NOT NULL,
    workflow_id          bigint        NOT NULL,
    node_type            tinyint       NOT NULL DEFAULT '1',
    expression_type      tinyint       NOT NULL DEFAULT '0',
    fail_strategy        tinyint       NOT NULL DEFAULT '1',
    workflow_node_status tinyint       NOT NULL DEFAULT '1',
    priority_level       int           NOT NULL DEFAULT '1',
    node_info            nvarchar(max) NULL     DEFAULT NULL,
    version              int           NOT NULL,
    create_dt            datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt            datetime2     NOT NULL DEFAULT GETDATE(),
    deleted              BIT                    DEFAULT 0,
    ext_attrs            nvarchar(256) NULL     DEFAULT ''
)
GO

CREATE NONCLUSTERED INDEX idx_create_dt ON workflow_node (create_dt ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON workflow_node (namespace_id ASC, group_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'节点名称',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务信息id',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'job_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流ID',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'workflow_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'1、任务节点 2、条件节点',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'1、SpEl、2、Aviator 3、QL',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'expression_type'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'失败策略 1、跳过 2、阻塞',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'fail_strategy'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流节点状态 0、关闭、1、开启',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'workflow_node_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'优先级',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'priority_level'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'节点信息 ',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'node_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本号',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流节点',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_node'
GO

-- er_workflow_task_batch
CREATE TABLE er_workflow_task_batch
(
    id                bigint PRIMARY KEY IDENTITY,
    namespace_id      nvarchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        nvarchar(64)  NOT NULL,
    workflow_id       bigint        NOT NULL,
    task_batch_status tinyint       NOT NULL DEFAULT '0',
    operation_reason  tinyint       NOT NULL DEFAULT '0',
    flow_info         nvarchar(max) NULL     DEFAULT NULL,
    execution_at      bigint        NOT NULL DEFAULT '0',
    create_dt         datetime2     NOT NULL DEFAULT GETDATE(),
    update_dt         datetime2     NOT NULL DEFAULT GETDATE(),
    deleted           BIT                    DEFAULT 0,
    ext_attrs         nvarchar(256) NULL     DEFAULT ''
)
GO

CREATE NONCLUSTERED INDEX idx_job_id_task_batch_status ON workflow_task_batch (workflow_id ASC, task_batch_status ASC)
GO
CREATE NONCLUSTERED INDEX idx_create_dt ON workflow_task_batch (create_dt ASC)
GO
CREATE NONCLUSTERED INDEX idx_namespace_id_group_name ON workflow_task_batch (namespace_id ASC, group_name ASC)
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'命名空间id',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'namespace_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'组名称',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'group_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流任务id',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'workflow_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务批次状态 0、失败 1、成功',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'task_batch_status'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'操作原因',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'operation_reason'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'流程信息',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'flow_info'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'任务执行时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'execution_at'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'create_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'修改时间',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'update_dt'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'逻辑删除 1、删除',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'deleted'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'扩展字段',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch',
     'COLUMN', N'ext_attrs'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'工作流批次',
     'SCHEMA', N'dbo',
     'TABLE', N'workflow_task_batch'
GO
