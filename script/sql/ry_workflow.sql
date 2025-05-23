-- ----------------------------
-- 0、warm-flow-all.sql，地址：https://gitee.com/dromara/warm-flow/blob/master/sql/mysql/warm-flow-all.sql
-- ----------------------------
CREATE TABLE `flow_definition`
(
    `id`              bigint unsigned NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
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
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用逗号隔开)',
    `node_ratio`      decimal(6, 3) DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `handler_type`    varchar(100)  DEFAULT NULL COMMENT '处理器类型',
    `handler_path`    varchar(400)  DEFAULT NULL COMMENT '处理器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `ext`             text          COMMENT '扩展属性',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint unsigned NOT NULL COMMENT '主键id',
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
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
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
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2 审批通过 3自动通过 4终止 5作废 6撤销 7取回  8已完成 9已退回 10失效）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建者',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
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
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20) unsigned NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)          NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)          NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)          NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)                 DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code` varchar(200)                 DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批者',
    `cooperate_type`   tinyint(1)          NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(40)                  DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)         NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)         NOT NULL COMMENT '流程状态（1审批中 2 审批通过 9已退回 10失效）',
    `form_custom`      char(1)                      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)                 DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variable`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              varchar(500)                 DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)                      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint unsigned NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程用户表';

-- ----------------------------
-- 流程分类表
-- ----------------------------
create table flow_category
(
    category_id   bigint(20)  not null comment '流程分类ID',
    tenant_id     varchar(20)  default '000000' comment '租户编号',
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

INSERT INTO flow_category values (100, '000000', 0, '0', 'OA审批', 0, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (101, '000000', 100, '0,100', '假勤管理', 0, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (102, '000000', 100, '0,100', '人事管理', 1, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (103, '000000', 101, '0,100,101', '请假', 0, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (104, '000000', 101, '0,100,101', '出差', 1, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (105, '000000', 101, '0,100,101', '加班', 2, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (106, '000000', 101, '0,100,101', '换班', 3, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (107, '000000', 101, '0,100,101', '外出', 4, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (108, '000000', 102, '0,100,102', '转正', 1, '0', 103, 1, sysdate(), null, null);
INSERT INTO flow_category values (109, '000000', 102, '0,100,102', '离职', 2, '0', 103, 1, sysdate(), null, null);

-- ----------------------------
-- 请假单信息
-- ----------------------------
create table test_leave
(
    id          bigint(20)   not null comment 'id',
    tenant_id   varchar(20) default '000000' comment '租户编号',
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

insert into sys_menu values ('11616', '工作流', '0', '6', 'workflow', '', '', '1', '0', 'M', '0', '0', '', 'workflow', 103, 1, sysdate(),NULL, NULL, '');
insert into sys_menu values ('11618', '我的任务', '0', '7', 'task', '', '', '1', '0', 'M', '0', '0', '', 'my-task', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11619', '我的待办', '11618', '2', 'taskWaiting', 'workflow/task/taskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11632', '我的已办', '11618', '3', 'taskFinish', 'workflow/task/taskFinish', '', '1', '1', 'C', '0', '0', '', 'finish', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11633', '我的抄送', '11618', '4', 'taskCopyList', 'workflow/task/taskCopyList', '', '1', '1', 'C', '0', '0', '', 'my-copy', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '', 'process-definition', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11621', '流程实例', '11630', '1', 'processInstance', 'workflow/processInstance/index', '', '1', '1', 'C', '0', '0', '', 'tree-table', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11622', '流程分类', '11616', '1', 'category', 'workflow/category/index', '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11629', '我发起的', '11618', '1', 'myDocument', 'workflow/task/myDocument', '', '1', '1', 'C', '0', '0', '', 'guide', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11630', '流程监控', '11616', '4', 'monitor', '', '', '1', '0', 'M', '0', '0', '', 'monitor', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('11631', '待办任务', '11630', '2', 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, sysdate(), NULL, NULL, '');
-- 流程分类管理相关按钮
insert into sys_menu values ('11623', '流程分类查询', '11622', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1,sysdate(), null, null, '');
insert into sys_menu values ('11624', '流程分类新增', '11622', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add', '#', 103, 1,sysdate(), null, null, '');
insert into sys_menu values ('11625', '流程分类修改', '11622', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit', '#', 103, 1,sysdate(), null, null, '');
insert into sys_menu values ('11626', '流程分类删除', '11622', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove', '#', 103,1, sysdate(), null, null, '');
insert into sys_menu values ('11627', '流程分类导出', '11622', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export', '#', 103,1, sysdate(), null, null, '');
-- 请假测试相关按钮
insert into sys_menu VALUES (11638, '请假申请',     5,    1, 'leave', 'workflow/leave/index', '', 1, 0, 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, sysdate(), NULL, NULL, '请假申请菜单');
insert into sys_menu VALUES (11639, '请假申请查询', 11638, 1, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (11640, '请假申请新增', 11638, 2, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (11641, '请假申请修改', 11638, 3, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (11642, '请假申请删除', 11638, 4, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu VALUES (11643, '请假申请导出', 11638, 5, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, sysdate(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, sysdate(), NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (15, '000000', '任务状态', 'wf_task_status', 103, 1, sysdate(), NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL,'已撤销');
INSERT INTO sys_dict_data VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL,'待审核');
INSERT INTO sys_dict_data VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL,'已完成');
INSERT INTO sys_dict_data VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL,'已作废');
INSERT INTO sys_dict_data VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL,'已退回');
INSERT INTO sys_dict_data VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate(), NULL,NULL, '已终止');
INSERT INTO sys_dict_data VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL,'自定义表单');
INSERT INTO sys_dict_data VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL,'动态表单');
INSERT INTO sys_dict_data VALUES (48, '000000', 1, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (49, '000000', 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (50, '000000', 3, '待审核', 'waiting', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (51, '000000', 4, '作废', 'invalid', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '作废');
INSERT INTO sys_dict_data VALUES (52, '000000', 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (53, '000000', 6, '终止', 'termination', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '终止');
INSERT INTO sys_dict_data VALUES (54, '000000', 7, '转办', 'transfer', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (55, '000000', 8, '委托', 'depute', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '委托');
INSERT INTO sys_dict_data VALUES (56, '000000', 9, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (57, '000000', 10, '加签', 'sign', 'wf_task_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (58, '000000', 11, '减签', 'sign_off', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '减签');
INSERT INTO sys_dict_data VALUES (59, '000000', 11, '超时', 'timeout', 'wf_task_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '超时');

