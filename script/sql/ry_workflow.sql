-- ----------------------------
-- 0、warm-flow-all.sql，地址：https://gitee.com/dromara/warm-flow/blob/master/sql/mysql/warm-flow-all.sql
-- ----------------------------
CREATE TABLE `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `model_value`     varchar(40)     NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `def_config`      text                     COMMENT '流程定义配置（自动审批等引擎配置与自定义扩展属性json）',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)                  DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程定义表';

CREATE TABLE `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100)    NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200)  DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             text          COMMENT '节点扩展属性',
    `node_config`     text          COMMENT '节点配置（服务任务、事件等引擎节点配置json）',
    `event_type`      varchar(40)   DEFAULT NULL COMMENT '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）',
    `event_name`      varchar(100)  DEFAULT NULL COMMENT '事件名称（消息事件使用）',
    `attached_to_node_code` varchar(100) DEFAULT NULL COMMENT '边界事件附着的任务节点编码',
    `interrupting`    tinyint(1)    DEFAULT NULL COMMENT '边界事件是否中断型',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100)    NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `next_node_code` varchar(100)    NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='节点跳转关联表';

CREATE TABLE `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `node_code`       varchar(100) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '实例状态（draft待提交 waiting审批中 termination终止 invalid作废 cancel撤销 finish已完成 back已退回 restart重启）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程实例表';

CREATE TABLE `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `flow_status`   varchar(20)  NOT NULL COMMENT '任务状态（pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送；任务结果，勿套用实例状态编码）',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20)                   NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)                   NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)                   NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)                   NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)                 DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code` varchar(200)                 DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   varchar(20)                  NOT NULL DEFAULT 'none' COMMENT '协作方式（none无 transfer转办 delegate委派 add_signer加签 remove_signer减签；行为维度，抄送/会签/票签不落此列，结果见 flow_status）',
    `collaborator`     varchar(500)                 DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)                  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)                  NOT NULL COMMENT '任务状态（pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送；任务结果，勿套用实例状态编码，唯一例外是撤回归档写 cancel）',
    `form_custom`      char(1)                      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)                 DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variable`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              TEXT                         DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `node_config`      TEXT                         DEFAULT NULL COMMENT '节点配置（服务任务等引擎配置与自定义扩展属性json）',
    `create_time`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)                      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限 4待办任务的抄送人）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '关联id（待办任务id；抄送人记录为 flow_his_task.id）',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程用户表';

-- 事件等待表：定时 / 消息等需要“等待”的事件统一落在此表，状态独立于 flow_status。
-- 事件能力默认关闭（warm-flow.timer-enabled=false），未开启时引擎不扫描本表。
CREATE TABLE `flow_event_wait`
(
    `id`               bigint       NOT NULL COMMENT '主键id',
    `tenant_id`        varchar(40)  DEFAULT NULL COMMENT '租户id',
    `del_flag`         char(1)      DEFAULT '0' COMMENT '删除标志',
    `definition_id`    bigint       DEFAULT NULL COMMENT '流程定义id（定时启动事件必须有）',
    `instance_id`      bigint       DEFAULT NULL COMMENT '流程实例id（实例等待事件必须有）',
    `node_code`        varchar(100) DEFAULT NULL COMMENT '事件节点编码（定位沿用 definition_id + node_code）',
    `event_type`       varchar(40)  NOT NULL COMMENT '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）',
    `event_name`       varchar(100) DEFAULT NULL COMMENT '事件名称（消息事件使用）',
    `attached_task_id` bigint       DEFAULT NULL COMMENT '边界事件关联的待办任务id',
    `trigger_type`     varchar(40)  DEFAULT NULL COMMENT '触发策略类型（AFTER FIXED POINT_IN_TIME）',
    `trigger_interval` varchar(100) DEFAULT NULL COMMENT '触发策略原始配置',
    `next_trigger_at`  datetime     DEFAULT NULL COMMENT '下次触发时间',
    `timezone`         varchar(40)  DEFAULT NULL COMMENT '定时触发使用的时区',
    `repeat_count`     int          DEFAULT NULL COMMENT '已触发次数',
    `max_repeat_count` int          DEFAULT NULL COMMENT '最大触发次数',
    `status`           varchar(20)  NOT NULL COMMENT '等待状态（WAITING RUNNING CANCELLED RETRY DEAD）',
    `retry_count`      int          DEFAULT NULL COMMENT '已重试次数',
    `max_retry_count`  int          DEFAULT NULL COMMENT '最大重试次数',
    `retry_at`         datetime     DEFAULT NULL COMMENT '下次重试时间',
    `lock_owner`       varchar(64)  DEFAULT NULL COMMENT '抢占持有者标识',
    `lock_until`       datetime     DEFAULT NULL COMMENT '抢占租约到期时间',
    `version`          int          DEFAULT NULL COMMENT '乐观锁版本号',
    `payload`          text         COMMENT '消息事件接收的业务载荷',
    `last_error`       varchar(500) DEFAULT NULL COMMENT '最近一次失败原因摘要',
    `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `event_wait_status_next` (`status`, `next_trigger_at`, `id`) USING BTREE,
    KEY `event_wait_status_retry` (`status`, `retry_at`, `id`) USING BTREE,
    KEY `event_wait_tenant_status_next` (`tenant_id`, `status`, `next_trigger_at`, `id`) USING BTREE,
    KEY `event_wait_tenant_status_retry` (`tenant_id`, `status`, `retry_at`, `id`) USING BTREE,
    KEY `event_wait_instance_task` (`instance_id`, `attached_task_id`) USING BTREE,
    KEY `event_wait_name_instance_status` (`event_name`, `instance_id`, `status`) USING BTREE,
    KEY `event_wait_def_node_status` (`definition_id`, `node_code`, `status`) USING BTREE
) ENGINE = InnoDB COMMENT ='事件等待表';

-- ----------------------------
-- 流程分类表
-- ----------------------------
create table flow_category
(
    category_id   bigint(20)  not null comment '流程分类ID',
    parent_id     bigint(20)   default 0 comment '父流程分类id',
    ancestors     varchar(500) default '' comment '祖级列表',
    category_name varchar(30) not null comment '流程分类名称',
    order_num     int(4)       default 0 comment '显示顺序',
    del_flag      char(1)      default '0' comment '删除标志（0代表存在 1代表删除）',
    create_dept   bigint(20)  null comment '创建部门',
    create_by     bigint(20)  null comment '创建者',
    create_time   datetime    null comment '创建时间',
    update_by     bigint(20)  null comment '更新者',
    update_time   datetime    null comment '更新时间',
    primary key (category_id)
) engine = innodb comment = '流程分类';

INSERT INTO flow_category values (1762300000000000100, 0, '0', 'OA审批', 0, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000101, 1762300000000000100, '0,1762300000000000100', '假勤管理', 0, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000102, 1762300000000000100, '0,1762300000000000100', '人事管理', 1, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000103, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '请假', 0, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000104, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '出差', 1, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000105, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '加班', 2, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000106, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '换班', 3, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000107, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '外出', 4, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000108, 1762300000000000102, '0,1762300000000000100,1762300000000000102', '转正', 1, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
INSERT INTO flow_category values (1762300000000000109, 1762300000000000102, '0,1762300000000000100,1762300000000000102', '离职', 2, '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);

-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------

CREATE TABLE flow_spel (
    id bigint(20) NOT NULL COMMENT '主键id',
    component_name varchar(255) DEFAULT NULL COMMENT '组件名称',
    method_name varchar(255) DEFAULT NULL COMMENT '方法名',
    method_params varchar(255) DEFAULT NULL COMMENT '参数',
    view_spel varchar(255) DEFAULT NULL COMMENT '预览spel表达式',
    remark varchar(255) DEFAULT NULL COMMENT '备注',
    status char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag char(1) DEFAULT '0' COMMENT '删除标志',
    create_dept bigint(20) DEFAULT NULL COMMENT '创建部门',
    create_by bigint(20) DEFAULT NULL COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by bigint(20) DEFAULT NULL COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT='流程spel表达式定义表';

INSERT INTO flow_spel VALUES (1762400000000000001, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate());
INSERT INTO flow_spel VALUES (1762400000000000002, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate());

-- ----------------------------
-- 流程实例业务扩展表
-- ----------------------------

create table flow_instance_biz_ext (
    id             bigint                       not null comment '主键id',
    create_dept    bigint                       null comment '创建部门',
    create_by      bigint                       null comment '创建者',
    create_time    datetime                     null comment '创建时间',
    update_by      bigint                       null comment '更新者',
    update_time    datetime                     null comment '更新时间',
    business_code  varchar(255)                 null comment '业务编码',
    business_title varchar(1000)                null comment '业务标题',
    del_flag       char        default '0'      null comment '删除标志（0代表存在 1代表删除）',
    instance_id    bigint                       null comment '流程实例Id',
    business_id    varchar(255)                 null comment '业务Id',
    PRIMARY KEY (id)
)  ENGINE = InnoDB COMMENT '流程实例业务扩展表';

-- ----------------------------
-- 请假单信息
-- ----------------------------

create table test_leave
(
    id          bigint(20)   not null comment 'id',
    apply_code  varchar(50)  not null comment '申请编号',
    leave_type  varchar(255) not null comment '请假类型',
    start_date  datetime     not null comment '开始时间',
    end_date    datetime     not null comment '结束时间',
    leave_days  int(10)      not null comment '请假天数',
    remark      varchar(255) null comment '请假原因',
    status      varchar(255) null comment '状态',
    create_dept bigint       null comment '创建部门',
    create_by   bigint       null comment '创建者',
    create_time datetime     null comment '创建时间',
    update_by   bigint       null comment '更新者',
    update_time datetime     null comment '更新时间',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '请假申请表';

insert into sys_menu values (1761400000000011616, '工作流', 0, 6, 'workflow', '', '', 'N', 'Y', 'M', '0', '0', '', 'workflow', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011618, '我的任务', 0, 7, 'task', '', '', 'N', 'Y', 'M', '0', '0', '', 'my-task', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011619, '我的待办', 1761400000000011618, 2, 'taskWaiting', 'workflow/task/taskWaiting', '', 'N', 'N', 'C', '0', '0', '', 'waiting', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011632, '我的已办', 1761400000000011618, 3, 'taskFinish', 'workflow/task/taskFinish', '', 'N', 'N', 'C', '0', '0', '', 'finish', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011633, '我的抄送', 1761400000000011618, 4, 'taskCopyList', 'workflow/task/taskCopyList', '', 'N', 'N', 'C', '0', '0', '', 'my-copy', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011620, '流程定义', 1761400000000011616, 3, 'processDefinition', 'workflow/processDefinition/index', '', 'N', 'N', 'C', '0', '0', 'workflow:definition:list', 'process-definition', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011621, '流程实例', 1761400000000011630, 1, 'processInstance', 'workflow/processInstance/index', '', 'N', 'N', 'C', '0', '0', 'workflow:instance:list', 'tree-table', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011622, '流程分类', 1761400000000011616, 1, 'category', 'workflow/category/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:category:list', 'category', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011801, '流程表达式', 1761400000000011616, 2, 'spel', 'workflow/spel/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:spel:list', 'input', '', '', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate(), '流程达式定义菜单');
insert into sys_menu values (1761400000000011629, '我发起的', 1761400000000011618, 1, 'myDocument', 'workflow/task/myDocument', '', 'N', 'N', 'C', '0', '0', 'workflow:instance:currentList', 'guide', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011630, '流程监控', 1761400000000011616, 4, 'processMonitor', '', '', 'N', 'Y', 'M', '0', '0', '', 'monitor', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011631, '待办任务', 1761400000000011630, 2, 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', 'N', 'N', 'C', '0', '0', 'workflow:task:list', 'waiting', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu values (1761400000000011660, '待办任务修改', 1761400000000011631, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:task:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011700, '流程设计', 1761400000000011616, 5, 'design/index', 'workflow/processDefinition/design', '', 'N', 'N', 'C', '1', '0', 'workflow:leave:edit', '#', '/workflow/processDefinition', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011701, '请假申请', 1761400000000011616, 6, 'leaveEdit/index', 'workflow/leave/leaveEdit', '', 'N', 'N', 'C', '1', '0', 'workflow:leave:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 流程分类管理相关按钮
insert into sys_menu values (1761400000000011623, '流程分类查询', 1761400000000011622, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011624, '流程分类新增', 1761400000000011622, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011625, '流程分类修改', 1761400000000011622, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011626, '流程分类删除', 1761400000000011622, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011627, '流程分类导出', 1761400000000011622, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 流程实例管理相关按钮
insert into sys_menu values (1761400000000011653, '流程实例查询', 1761400000000011621, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011654, '流程变量查询', 1761400000000011621, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variableQuery', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011655, '流程变量修改', 1761400000000011621, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variable', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011656, '流程实例激活/挂起', 1761400000000011621, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:active', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011657, '流程实例删除', 1761400000000011621, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011658, '流程实例作废', 1761400000000011621, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:invalid', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011659, '流程实例撤销', 1761400000000011621, 7, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:cancel', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 流程定义管理相关按钮
insert into sys_menu values (1761400000000011644, '流程定义查询', 1761400000000011620, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011645, '流程定义新增', 1761400000000011620, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011646, '流程定义修改', 1761400000000011620, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011647, '流程定义删除', 1761400000000011620, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011648, '流程定义导出', 1761400000000011620, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011649, '流程定义导入', 1761400000000011620, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:import', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011650, '流程定义发布/取消发布', 1761400000000011620, 7, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:publish', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011651, '流程定义复制', 1761400000000011620, 8, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:copy', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011652, '流程定义激活/挂起', 1761400000000011620, 9, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:active', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 流程表达式管理相关按钮
INSERT INTO sys_menu VALUES (1761400000000011802, '流程达式定义查询', 1761400000000011801, 1, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011803, '流程达式定义新增', 1761400000000011801, 2, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011804, '流程达式定义修改', 1761400000000011801, 3, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011805, '流程达式定义删除', 1761400000000011801, 4, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011806, '流程达式定义导出', 1761400000000011801, 5, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');

-- 定时任务（事件等待记录）菜单与按钮
insert into sys_menu values (1761400000000011901, '定时任务', 1761400000000011630, 3, 'timer', 'workflow/timer/index', '', 'N', 'N', 'C', '0', '0', 'workflow:eventWait:list', 'time', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '定时任务与事件等待监控菜单');
insert into sys_menu values (1761400000000011902, '定时任务查询', 1761400000000011901, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:eventWait:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011903, '定时任务手动补发', 1761400000000011901, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:eventWait:trigger', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 请假测试相关按钮
insert into sys_menu VALUES (1761400000000011638, '请假申请', 1761400000000000005, 1, 'leave', 'workflow/leave/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:leave:list', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '请假申请菜单');
insert into sys_menu VALUES (1761400000000011639, '请假申请查询', 1761400000000011638, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (1761400000000011640, '请假申请新增', 1761400000000011638, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (1761400000000011641, '请假申请修改', 1761400000000011638, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (1761400000000011642, '请假申请删除', 1761400000000011638, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (1761400000000011643, '请假申请导出', 1761400000000011638, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (1761500000000000013, '业务状态', 'wf_business_status', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (1761500000000000014, '表单类型', 'wf_form_type', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (1761500000000000015, '任务状态', 'wf_task_status', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (1761600000000000039, 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000040, 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (1761600000000000041, 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (1761600000000000042, 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已完成');
INSERT INTO sys_dict_data VALUES (1761600000000000043, 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已作废');
INSERT INTO sys_dict_data VALUES (1761600000000000044, 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已退回');
INSERT INTO sys_dict_data VALUES (1761600000000000045, 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已终止');
INSERT INTO sys_dict_data VALUES (1761600000000000063, 8, '重启', 'restart', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '重启');
INSERT INTO sys_dict_data VALUES (1761600000000000046, 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data VALUES (1761600000000000047, 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '动态表单');
INSERT INTO sys_dict_data VALUES (1761600000000000060, 1, '待审批', 'pending', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '待审批');
INSERT INTO sys_dict_data VALUES (1761600000000000049, 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (1761600000000000061, 3, '自动审批', 'auto_pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '自动审批');
INSERT INTO sys_dict_data VALUES (1761600000000000056, 4, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (1761600000000000052, 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (1761600000000000062, 6, '拿回', 'take_back', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '拿回');
INSERT INTO sys_dict_data VALUES (1761600000000000051, 7, '失效', 'invalid', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '失效');
INSERT INTO sys_dict_data VALUES (1761600000000000048, 8, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000064, 9, '转办', 'transfer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (1761600000000000065, 10, '委派', 'delegate', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '委派');
INSERT INTO sys_dict_data VALUES (1761600000000000066, 11, '加签', 'add_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (1761600000000000067, 12, '减签', 'remove_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '减签');

