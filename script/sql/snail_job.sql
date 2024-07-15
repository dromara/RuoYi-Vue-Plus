SET NAMES utf8mb4;

CREATE TABLE `sj_namespace`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(64)         NOT NULL COMMENT '名称',
    `unique_id`   varchar(64)         NOT NULL COMMENT '唯一id',
    `description` varchar(256)        NOT NULL DEFAULT '' COMMENT '描述',
    `deleted`     tinyint(4)          NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
    `create_dt`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`   datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    UNIQUE KEY `uk_unique_id` (`unique_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='命名空间';

INSERT INTO `sj_namespace` VALUES (1, 'Development', 'dev', '', 0, now(), now());
INSERT INTO `sj_namespace` VALUES (2, 'Production', 'prod', '', 0, now(), now());

CREATE TABLE `sj_group_config`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`      varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`        varchar(64)         NOT NULL DEFAULT '' COMMENT '组名称',
    `description`       varchar(256)        NOT NULL DEFAULT '' COMMENT '组描述',
    `token`             varchar(64)         NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT' COMMENT 'token',
    `group_status`      tinyint(4)          NOT NULL DEFAULT 0 COMMENT '组状态 0、未启用 1、启用',
    `version`           int(11)             NOT NULL COMMENT '版本号',
    `group_partition`   int(11)             NOT NULL COMMENT '分区',
    `id_generator_mode` tinyint(4)          NOT NULL DEFAULT 1 COMMENT '唯一id生成模式 默认号段模式',
    `init_scene`        tinyint(4)          NOT NULL DEFAULT 0 COMMENT '是否初始化场景 0:否 1:是',
    `bucket_index`      int(11)             NOT NULL DEFAULT 0 COMMENT 'bucket',
    `create_dt`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='组配置';

INSERT INTO `sj_group_config` VALUES (1, 'dev', 'ruoyi_group', '', 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, 4, now(), now());

CREATE TABLE `sj_notify_config`
(
    `id`                     bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`           varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`             varchar(64)         NOT NULL COMMENT '组名称',
    `business_id`            varchar(64)         NOT NULL COMMENT '业务id (job_id或workflow_id或scene_name)',
    `system_task_type`       tinyint(4)          NOT NULL DEFAULT 3 COMMENT '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务',
    `notify_status`          tinyint(4)          NOT NULL DEFAULT 0 COMMENT '通知状态 0、未启用 1、启用',
    `recipient_ids`          varchar(128)        NOT NULL COMMENT '接收人id列表',
    `notify_threshold`       int(11)             NOT NULL DEFAULT 0 COMMENT '通知阈值',
    `notify_scene`           tinyint(4)          NOT NULL DEFAULT 0 COMMENT '通知场景',
    `rate_limiter_status`    tinyint(4)          NOT NULL DEFAULT 0 COMMENT '限流状态 0、未启用 1、启用',
    `rate_limiter_threshold` int(11)             NOT NULL DEFAULT 0 COMMENT '每秒限流阈值',
    `description`            varchar(256)        NOT NULL DEFAULT '' COMMENT '描述',
    `create_dt`              datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`              datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`, `group_name`, `business_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知配置';

CREATE TABLE `sj_notify_recipient`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`     varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `recipient_name`   varchar(64)         NOT NULL COMMENT '接收人名称',
    `notify_type`      tinyint(4)          NOT NULL DEFAULT 0 COMMENT '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook',
    `notify_attribute` varchar(512)        NOT NULL COMMENT '配置属性',
    `description`      varchar(256)        NOT NULL DEFAULT '' COMMENT '描述',
    `create_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id` (`namespace_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警通知接收人';

CREATE TABLE `sj_retry_dead_letter_0`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`  varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `unique_id`     varchar(64)         NOT NULL COMMENT '同组下id唯一',
    `group_name`    varchar(64)         NOT NULL COMMENT '组名称',
    `scene_name`    varchar(64)         NOT NULL COMMENT '场景名称',
    `idempotent_id` varchar(64)         NOT NULL COMMENT '幂等id',
    `biz_no`        varchar(64)         NOT NULL DEFAULT '' COMMENT '业务编号',
    `executor_name` varchar(512)        NOT NULL DEFAULT '' COMMENT '执行器名称',
    `args_str`      text                NOT NULL COMMENT '执行方法参数',
    `ext_attrs`     text                NOT NULL COMMENT '扩展字段',
    `task_type`     tinyint(4)          NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
    `create_dt`     datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`, `group_name`, `scene_name`),
    KEY `idx_idempotent_id` (`idempotent_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_create_dt` (`create_dt`),
    UNIQUE KEY `uk_namespace_id_group_name_unique_id` (`namespace_id`, `group_name`, `unique_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='死信队列表';

CREATE TABLE `sj_retry_task_0`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`    varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `unique_id`       varchar(64)         NOT NULL COMMENT '同组下id唯一',
    `group_name`      varchar(64)         NOT NULL COMMENT '组名称',
    `scene_name`      varchar(64)         NOT NULL COMMENT '场景名称',
    `idempotent_id`   varchar(64)         NOT NULL COMMENT '幂等id',
    `biz_no`          varchar(64)         NOT NULL DEFAULT '' COMMENT '业务编号',
    `executor_name`   varchar(512)        NOT NULL DEFAULT '' COMMENT '执行器名称',
    `args_str`        text                NOT NULL COMMENT '执行方法参数',
    `ext_attrs`       text                NOT NULL COMMENT '扩展字段',
    `next_trigger_at` datetime            NOT NULL COMMENT '下次触发时间',
    `retry_count`     int(11)             NOT NULL DEFAULT 0 COMMENT '重试次数',
    `retry_status`    tinyint(4)          NOT NULL DEFAULT 0 COMMENT '重试状态 0、重试中 1、成功 2、最大重试次数',
    `task_type`       tinyint(4)          NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
    `create_dt`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`, `group_name`, `scene_name`),
    KEY `idx_namespace_id_group_name_task_type` (`namespace_id`, `group_name`, `task_type`),
    KEY `idx_namespace_id_group_name_retry_status` (`namespace_id`, `group_name`, `retry_status`),
    KEY `idx_idempotent_id` (`idempotent_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_create_dt` (`create_dt`),
    UNIQUE KEY `uk_name_unique_id` (`namespace_id`, `group_name`, `unique_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

CREATE TABLE `sj_retry_task_log`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`  varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `unique_id`     varchar(64)         NOT NULL COMMENT '同组下id唯一',
    `group_name`    varchar(64)         NOT NULL COMMENT '组名称',
    `scene_name`    varchar(64)         NOT NULL COMMENT '场景名称',
    `idempotent_id` varchar(64)         NOT NULL COMMENT '幂等id',
    `biz_no`        varchar(64)         NOT NULL DEFAULT '' COMMENT '业务编号',
    `executor_name` varchar(512)        NOT NULL DEFAULT '' COMMENT '执行器名称',
    `args_str`      text                NOT NULL COMMENT '执行方法参数',
    `ext_attrs`     text                NOT NULL COMMENT '扩展字段',
    `retry_status`  tinyint(4)          NOT NULL DEFAULT 0 COMMENT '重试状态 0、重试中 1、成功 2、最大次数',
    `task_type`     tinyint(4)          NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
    `create_dt`     datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`     datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_name_scene_name` (`namespace_id`, `group_name`, `scene_name`),
    KEY `idx_retry_status` (`retry_status`),
    KEY `idx_idempotent_id` (`idempotent_id`),
    KEY `idx_unique_id` (`unique_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_create_dt` (`create_dt`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务日志基础信息表';

CREATE TABLE `sj_retry_task_log_message`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id` varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`   varchar(64)         NOT NULL COMMENT '组名称',
    `unique_id`    varchar(64)         NOT NULL COMMENT '同组下id唯一',
    `message`      longtext            NOT NULL COMMENT '异常信息',
    `log_num`      int(11)             NOT NULL DEFAULT 1 COMMENT '日志数量',
    `real_time`    bigint(13)          NOT NULL DEFAULT 0 COMMENT '上报时间',
    `create_dt`    datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`, `group_name`, `unique_id`),
    KEY `idx_create_dt` (`create_dt`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务调度日志信息记录表';

CREATE TABLE `sj_retry_scene_config`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`     varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `scene_name`       varchar(64)         NOT NULL COMMENT '场景名称',
    `group_name`       varchar(64)         NOT NULL COMMENT '组名称',
    `scene_status`     tinyint(4)          NOT NULL DEFAULT 0 COMMENT '组状态 0、未启用 1、启用',
    `max_retry_count`  int(11)             NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    `back_off`         tinyint(4)          NOT NULL DEFAULT 1 COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
    `trigger_interval` varchar(16)         NOT NULL DEFAULT '' COMMENT '间隔时长',
    `deadline_request` bigint(20) unsigned NOT NULL DEFAULT 60000 COMMENT 'Deadline Request 调用链超时 单位毫秒',
    `executor_timeout` int(11) unsigned    NOT NULL DEFAULT 5 COMMENT '任务执行超时时间，单位秒',
    `route_key`        tinyint(4)          NOT NULL DEFAULT 4 COMMENT '路由策略',
    `description`      varchar(256)        NOT NULL DEFAULT '' COMMENT '描述',
    `create_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id_group_name_scene_name` (`namespace_id`, `group_name`, `scene_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='场景配置';

CREATE TABLE `sj_server_node`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id` varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`   varchar(64)         NOT NULL COMMENT '组名称',
    `host_id`      varchar(64)         NOT NULL COMMENT '主机id',
    `host_ip`      varchar(64)         NOT NULL COMMENT '机器ip',
    `host_port`    int(16)             NOT NULL COMMENT '机器端口',
    `expire_at`    datetime            NOT NULL COMMENT '过期时间',
    `node_type`    tinyint(4)          NOT NULL COMMENT '节点类型 1、客户端 2、是服务端',
    `ext_attrs`    varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `create_dt`    datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`    datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`),
    KEY `idx_expire_at_node_type` (`expire_at`, `node_type`),
    UNIQUE KEY `uk_host_id_host_ip` (`host_id`, `host_ip`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务器节点';

CREATE TABLE `sj_distributed_lock`
(
    `name`       varchar(64)         NOT NULL COMMENT '锁名称',
    `lock_until` timestamp(3)        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '锁定时长',
    `locked_at`  timestamp(3)        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '锁定时间',
    `locked_by`  varchar(255)        NOT NULL COMMENT '锁定者',
    `create_dt`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`  datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='锁定表';

CREATE TABLE `sj_system_user`
(
    `id`        bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`  varchar(64)         NOT NULL COMMENT '账号',
    `password`  varchar(128)        NOT NULL COMMENT '密码',
    `role`      tinyint(4)          NOT NULL DEFAULT 0 COMMENT '角色：1-普通用户、2-管理员',
    `create_dt` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt` datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- pwd: admin
INSERT INTO `sj_system_user` VALUES (1, 'admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2, now(), now());

CREATE TABLE `sj_system_user_permission`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_name`     varchar(64)         NOT NULL COMMENT '组名称',
    `namespace_id`   varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `system_user_id` bigint(20)          NOT NULL COMMENT '系统用户id',
    `create_dt`      datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`      datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id_group_name_system_user_id` (`namespace_id`, `group_name`, `system_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户权限表';

CREATE TABLE `sj_sequence_alloc`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id` varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`   varchar(64)         NOT NULL DEFAULT '' COMMENT '组名称',
    `max_id`       bigint(20)          NOT NULL DEFAULT 1 COMMENT '最大id',
    `step`         int(11)             NOT NULL DEFAULT 100 COMMENT '步长',
    `update_dt`    datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='号段模式序号ID分配表';

-- 分布式调度DDL
CREATE TABLE `sj_job`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`     varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`       varchar(64)         NOT NULL COMMENT '组名称',
    `job_name`         varchar(64)         NOT NULL COMMENT '名称',
    `args_str`         text                         DEFAULT NULL COMMENT '执行方法参数',
    `args_type`        tinyint(4)          NOT NULL DEFAULT 1 COMMENT '参数类型 ',
    `next_trigger_at`  bigint(13)          NOT NULL COMMENT '下次触发时间',
    `job_status`       tinyint(4)          NOT NULL DEFAULT 1 COMMENT '任务状态 0、关闭、1、开启',
    `task_type`        tinyint(4)          NOT NULL DEFAULT 1 COMMENT '任务类型 1、集群 2、广播 3、切片',
    `route_key`        tinyint(4)          NOT NULL DEFAULT 4 COMMENT '路由策略',
    `executor_type`    tinyint(4)          NOT NULL DEFAULT 1 COMMENT '执行器类型',
    `executor_info`    varchar(255)                 DEFAULT NULL COMMENT '执行器名称',
    `trigger_type`     tinyint(4)          NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
    `trigger_interval` varchar(255)        NOT NULL COMMENT '间隔时长',
    `block_strategy`   tinyint(4)          NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
    `executor_timeout` int(11)             NOT NULL DEFAULT 0 COMMENT '任务执行超时时间，单位秒',
    `max_retry_times`  int(11)             NOT NULL DEFAULT 0 COMMENT '最大重试次数',
    `parallel_num`     int(11)             NOT NULL DEFAULT 1 COMMENT '并行数',
    `retry_interval`   int(11)             NOT NULL DEFAULT 0 COMMENT '重试间隔(s)',
    `bucket_index`     int(11)             NOT NULL DEFAULT 0 COMMENT 'bucket',
    `resident`         tinyint(4)          NOT NULL DEFAULT 0 COMMENT '是否是常驻任务',
    `description`      varchar(256)        NOT NULL DEFAULT '' COMMENT '描述',
    `ext_attrs`        varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `deleted`          tinyint(4)          NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
    `create_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`),
    KEY `idx_job_status_bucket_index` (`job_status`, `bucket_index`),
    KEY `idx_create_dt` (`create_dt`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务信息';

INSERT INTO `sj_job` VALUES (1, 'dev', 'ruoyi_group', 'demo-job', null, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3, 1, 1, 116, 0, '', '', 0 , now(), now());

CREATE TABLE `sj_job_log_message`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`  varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`    varchar(64)         NOT NULL COMMENT '组名称',
    `job_id`        bigint(20)          NOT NULL COMMENT '任务信息id',
    `task_batch_id` bigint(20)          NOT NULL COMMENT '任务批次id',
    `task_id`       bigint(20)          NOT NULL COMMENT '调度任务id',
    `message`       longtext            NOT NULL COMMENT '调度信息',
    `log_num`       int(11)             NOT NULL DEFAULT 1 COMMENT '日志数量',
    `real_time`     bigint(13)          NOT NULL DEFAULT 0 COMMENT '上报时间',
    `ext_attrs`     varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `create_dt`     datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_batch_id_task_id` (`task_batch_id`, `task_id`),
    KEY `idx_create_dt` (`create_dt`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='调度日志';

CREATE TABLE `sj_job_task`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`   varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`     varchar(64)         NOT NULL COMMENT '组名称',
    `job_id`         bigint(20)          NOT NULL COMMENT '任务信息id',
    `task_batch_id`  bigint(20)          NOT NULL COMMENT '调度任务id',
    `parent_id`      bigint(20)          NOT NULL DEFAULT 0 COMMENT '父执行器id',
    `task_status`    tinyint             NOT NULL DEFAULT 0 COMMENT '执行的状态 0、失败 1、成功',
    `retry_count`    int(11)             NOT NULL DEFAULT 0 COMMENT '重试次数',
    `mr_stage`       tinyint                      DEFAULT NULL COMMENT '动态分片所处阶段 1:map 2:reduce 3:mergeReduce',
    `leaf`           tinyint             NOT NULL DEFAULT '1' COMMENT '叶子节点',
    `task_name`      varchar(255)        NOT NULL DEFAULT '' COMMENT '任务名称',
    `client_info`    varchar(128)                 DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
    `wf_context`     text                         DEFAULT NULL COMMENT '工作流全局上下文',
    `result_message` text                NOT NULL COMMENT '执行结果',
    `args_str`       text                         DEFAULT NULL COMMENT '执行方法参数',
    `args_type`      tinyint             NOT NULL DEFAULT 1 COMMENT '参数类型 ',
    `ext_attrs`      varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `create_dt`      datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`      datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_batch_id_task_status` (`task_batch_id`, `task_status`),
    KEY `idx_create_dt` (`create_dt`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务实例';

CREATE TABLE `sj_job_task_batch`
(
    `id`                      bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`            varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`              varchar(64)         NOT NULL COMMENT '组名称',
    `job_id`                  bigint(20)          NOT NULL COMMENT '任务id',
    `workflow_node_id`        bigint(20)          NOT NULL DEFAULT 0 COMMENT '工作流节点id',
    `parent_workflow_node_id` bigint(20)          NOT NULL DEFAULT 0 COMMENT '工作流任务父批次id',
    `workflow_task_batch_id`  bigint(20)          NOT NULL DEFAULT 0 COMMENT '工作流任务批次id',
    `task_batch_status`       tinyint(4)          NOT NULL DEFAULT 0 COMMENT '任务批次状态 0、失败 1、成功',
    `operation_reason`        tinyint(4)          NOT NULL DEFAULT 0 COMMENT '操作原因',
    `execution_at`            bigint(13)          NOT NULL DEFAULT 0 COMMENT '任务执行时间',
    `system_task_type`        tinyint(4)          NOT NULL DEFAULT 3 COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
    `parent_id`               varchar(64)         NOT NULL DEFAULT '' COMMENT '父节点',
    `ext_attrs`               varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `deleted`                 tinyint(4)          NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
    `create_dt`               datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`               datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_task_batch_status` (`job_id`, `task_batch_status`),
    KEY `idx_create_dt` (`create_dt`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`),
    KEY `idx_workflow_task_batch_id_workflow_node_id` (`workflow_task_batch_id`, `workflow_node_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务批次';

CREATE TABLE `sj_job_summary`
(
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`     VARCHAR(64)     NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`       VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '组名称',
    `business_id`      bigint          NOT NULL COMMENT '业务id (job_id或workflow_id)',
    `system_task_type` tinyint(4)      NOT NULL DEFAULT 3 COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
    `trigger_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
    `success_num`      int             NOT NULL DEFAULT 0 COMMENT '执行成功-日志数量',
    `fail_num`         int             NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
    `fail_reason`      varchar(512)    NOT NULL DEFAULT '' COMMENT '失败原因',
    `stop_num`         int             NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
    `stop_reason`      varchar(512)    NOT NULL DEFAULT '' COMMENT '失败原因',
    `cancel_num`       int             NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
    `cancel_reason`    varchar(512)    NOT NULL DEFAULT '' COMMENT '失败原因',
    `create_dt`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_namespace_id_group_name_business_id` (`namespace_id`, `group_name`, business_id),
    UNIQUE KEY `uk_trigger_at_system_task_type_business_id` (`trigger_at`, `system_task_type`, `business_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4 COMMENT ='DashBoard_Job';

CREATE TABLE `sj_retry_summary`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`  VARCHAR(64)     NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`    VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '组名称',
    `scene_name`    VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '场景名称',
    `trigger_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
    `running_num`   int             NOT NULL DEFAULT 0 COMMENT '重试中-日志数量',
    `finish_num`    int             NOT NULL DEFAULT 0 COMMENT '重试完成-日志数量',
    `max_count_num` int             NOT NULL DEFAULT 0 COMMENT '重试到达最大次数-日志数量',
    `suspend_num`   int             NOT NULL DEFAULT 0 COMMENT '暂停重试-日志数量',
    `create_dt`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_trigger_at` (`trigger_at`),
    UNIQUE KEY `uk_scene_name_trigger_at` (`namespace_id`, `group_name`, `scene_name`, `trigger_at`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4 COMMENT ='DashBoard_Retry';

CREATE TABLE `sj_workflow`
(
    `id`               bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `workflow_name`    varchar(64)         NOT NULL COMMENT '工作流名称',
    `namespace_id`     varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`       varchar(64)         NOT NULL COMMENT '组名称',
    `workflow_status`  tinyint(4)          NOT NULL DEFAULT 1 COMMENT '工作流状态 0、关闭、1、开启',
    `trigger_type`     tinyint(4)          NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
    `trigger_interval` varchar(255)        NOT NULL COMMENT '间隔时长',
    `next_trigger_at`  bigint(13)          NOT NULL COMMENT '下次触发时间',
    `block_strategy`   tinyint(4)          NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
    `executor_timeout` int(11)             NOT NULL DEFAULT 0 COMMENT '任务执行超时时间，单位秒',
    `description`      varchar(256)        NOT NULL DEFAULT '' COMMENT '描述',
    `flow_info`        text                         DEFAULT NULL COMMENT '流程信息',
    `wf_context`       text                         DEFAULT NULL COMMENT '上下文',
    `bucket_index`     int(11)             NOT NULL DEFAULT 0 COMMENT 'bucket',
    `version`          int(11)             NOT NULL COMMENT '版本号',
    `ext_attrs`        varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `deleted`          tinyint(4)          NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
    `create_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`        datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_dt` (`create_dt`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='工作流';

CREATE TABLE `sj_workflow_node`
(
    `id`                   bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`         varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `node_name`            varchar(64)         NOT NULL COMMENT '节点名称',
    `group_name`           varchar(64)         NOT NULL COMMENT '组名称',
    `job_id`               bigint(20)          NOT NULL COMMENT '任务信息id',
    `workflow_id`          bigint(20)          NOT NULL COMMENT '工作流ID',
    `node_type`            tinyint(4)          NOT NULL DEFAULT 1 COMMENT '1、任务节点 2、条件节点',
    `expression_type`      tinyint(4)          NOT NULL DEFAULT 0 COMMENT '1、SpEl、2、Aviator 3、QL',
    `fail_strategy`        tinyint(4)          NOT NULL DEFAULT 1 COMMENT '失败策略 1、跳过 2、阻塞',
    `workflow_node_status` tinyint(4)          NOT NULL DEFAULT 1 COMMENT '工作流节点状态 0、关闭、1、开启',
    `priority_level`       int(11)             NOT NULL DEFAULT 1 COMMENT '优先级',
    `node_info`            text                         DEFAULT NULL COMMENT '节点信息 ',
    `version`              int(11)             NOT NULL COMMENT '版本号',
    `ext_attrs`            varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `deleted`              tinyint(4)          NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
    `create_dt`            datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`            datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_dt` (`create_dt`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='工作流节点';

CREATE TABLE `sj_workflow_task_batch`
(
    `id`                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `namespace_id`      varchar(64)         NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
    `group_name`        varchar(64)         NOT NULL COMMENT '组名称',
    `workflow_id`       bigint(20)          NOT NULL COMMENT '工作流任务id',
    `task_batch_status` tinyint(4)          NOT NULL DEFAULT 0 COMMENT '任务批次状态 0、失败 1、成功',
    `operation_reason`  tinyint(4)          NOT NULL DEFAULT 0 COMMENT '操作原因',
    `flow_info`         text                         DEFAULT NULL COMMENT '流程信息',
    `wf_context`        text                         DEFAULT NULL COMMENT '全局上下文',
    `execution_at`      bigint(13)          NOT NULL DEFAULT 0 COMMENT '任务执行时间',
    `ext_attrs`         varchar(256)        NULL     DEFAULT '' COMMENT '扩展字段',
    `version`           int(11)              NOT NULL DEFAULT 1 COMMENT '版本号',
    `deleted`           tinyint(4)          NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
    `create_dt`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_dt`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_task_batch_status` (`workflow_id`, `task_batch_status`),
    KEY `idx_create_dt` (`create_dt`),
    KEY `idx_namespace_id_group_name` (`namespace_id`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='工作流批次';
