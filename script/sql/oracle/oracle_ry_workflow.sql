create table FLOW_DEFINITION
(
    ID              NUMBER(20)            not null,
    FLOW_CODE       VARCHAR2(40)          not null,
    FLOW_NAME       VARCHAR2(100)         not null,
    CATEGORY        VARCHAR2(100),
    VERSION         VARCHAR2(20)          not null,
    IS_PUBLISH      NUMBER(1)   default 0 not null,
    FORM_CUSTOM     VARCHAR2(1) default 'N',
    FORM_PATH       VARCHAR2(100),
    ACTIVITY_STATUS NUMBER(1)   default 1,
    LISTENER_TYPE   VARCHAR2(100),
    LISTENER_PATH   VARCHAR2(500),
    EXT             VARCHAR2(500),
    CREATE_TIME     DATE,
    UPDATE_TIME     DATE,
    DEL_FLAG        VARCHAR2(1) default '0',
    TENANT_ID       VARCHAR2(40)
);

alter table FLOW_DEFINITION add constraint PK_FLOW_DEFINITION primary key (ID);

comment on table FLOW_DEFINITION is '流程定义表';
comment on column FLOW_DEFINITION.ID is '主键id';
comment on column FLOW_DEFINITION.FLOW_CODE is '流程编码';
comment on column FLOW_DEFINITION.FLOW_NAME is '流程名称';
comment on column FLOW_DEFINITION.CATEGORY is '流程类别';
comment on column FLOW_DEFINITION.VERSION is '流程版本';
comment on column FLOW_DEFINITION.IS_PUBLISH is '是否发布 (0未发布 1已发布 9失效)';
comment on column FLOW_DEFINITION.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_DEFINITION.FORM_PATH is '审批表单路径';
comment on column FLOW_DEFINITION.ACTIVITY_STATUS is '流程激活状态（0挂起 1激活）';
comment on column FLOW_DEFINITION.LISTENER_TYPE is '监听器类型';
comment on column FLOW_DEFINITION.LISTENER_PATH is '监听器路径';
comment on column FLOW_DEFINITION.EXT is '扩展字段，预留给业务系统使用';
comment on column FLOW_DEFINITION.CREATE_TIME is '创建时间';
comment on column FLOW_DEFINITION.UPDATE_TIME is '更新时间';
comment on column FLOW_DEFINITION.DEL_FLAG is '删除标志';
comment on column FLOW_DEFINITION.TENANT_ID is '租户id';

create table FLOW_NODE
(
    ID              NUMBER(20)    not null,
    NODE_TYPE       NUMBER(1)     not null,
    DEFINITION_ID   NUMBER(20)    not null,
    NODE_CODE       VARCHAR2(100) not null,
    NODE_NAME       VARCHAR2(100),
    NODE_RATIO      NUMBER(6, 3),
    COORDINATE      VARCHAR2(100),
    ANY_NODE_SKIP   VARCHAR2(100),
    LISTENER_TYPE   VARCHAR2(100),
    LISTENER_PATH   VARCHAR2(500),
    HANDLER_TYPE    VARCHAR2(100),
    HANDLER_PATH    VARCHAR2(400),
    FORM_CUSTOM     VARCHAR2(1)   default 'N',
    FORM_PATH       VARCHAR2(100),
    VERSION         VARCHAR2(20),
    CREATE_TIME     DATE,
    UPDATE_TIME     DATE,
    EXT             VARCHAR2(500),
    DEL_FLAG        VARCHAR2(1)   default '0',
    TENANT_ID       VARCHAR2(40),
    PERMISSION_FLAG VARCHAR2(200)
);

alter table FLOW_NODE add constraint PK_FLOW_NODE primary key (ID);

comment on table FLOW_NODE is '流程节点表';
comment on column FLOW_NODE.ID is '主键id';
comment on column FLOW_NODE.NODE_TYPE is '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
comment on column FLOW_NODE.DEFINITION_ID is '对应flow_definition表的id';
comment on column FLOW_NODE.NODE_CODE is '流程节点编码';
comment on column FLOW_NODE.NODE_NAME is '流程节点名称';
comment on column FLOW_NODE.NODE_RATIO is '流程签署比例值';
comment on column FLOW_NODE.COORDINATE is '坐标';
comment on column FLOW_NODE.ANY_NODE_SKIP is '任意结点跳转';
comment on column FLOW_NODE.LISTENER_TYPE is '监听器类型';
comment on column FLOW_NODE.LISTENER_PATH is '监听器路径';
comment on column FLOW_NODE.HANDLER_TYPE is '处理器类型';
comment on column FLOW_NODE.HANDLER_PATH is '处理器路径';
comment on column FLOW_NODE.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_NODE.FORM_PATH is '审批表单路径';
comment on column FLOW_NODE.VERSION is '版本';
comment on column FLOW_NODE.CREATE_TIME is '创建时间';
comment on column FLOW_NODE.UPDATE_TIME is '更新时间';
comment on column FLOW_NODE.EXT is '扩展属性';
comment on column FLOW_NODE.DEL_FLAG is '删除标志';
comment on column FLOW_NODE.TENANT_ID is '租户id';
comment on column FLOW_NODE.PERMISSION_FLAG is '权限标识（权限类型:权限标识，可以多个，用逗号隔开)';

create table FLOW_SKIP
(
    ID             NUMBER(20)    not null,
    DEFINITION_ID  NUMBER(20)    not null,
    NOW_NODE_CODE  VARCHAR2(100) not null,
    NOW_NODE_TYPE  NUMBER(1),
    NEXT_NODE_CODE VARCHAR2(100) not null,
    NEXT_NODE_TYPE NUMBER(1),
    SKIP_NAME      VARCHAR2(100),
    SKIP_TYPE      VARCHAR2(40),
    SKIP_CONDITION VARCHAR2(200),
    COORDINATE     VARCHAR2(100),
    CREATE_TIME    DATE,
    UPDATE_TIME    DATE,
    DEL_FLAG       VARCHAR2(1) default '0',
    TENANT_ID      VARCHAR2(40)
);

alter table FLOW_SKIP add constraint PK_FLOW_SKIP primary key (ID);

comment on table FLOW_SKIP is '节点跳转关联表';
comment on column FLOW_SKIP.ID is '主键id';
comment on column FLOW_SKIP.DEFINITION_ID is '流程定义id';
comment on column FLOW_SKIP.NOW_NODE_CODE is '当前流程节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_SKIP.NOW_NODE_TYPE is '下一个流程节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_SKIP.NEXT_NODE_CODE is '下一个流程节点编码';
comment on column FLOW_SKIP.NEXT_NODE_TYPE is '下一个流程节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_SKIP.SKIP_NAME is '跳转名称';
comment on column FLOW_SKIP.SKIP_TYPE is '跳转类型 (PASS审批通过 REJECT退回)';
comment on column FLOW_SKIP.SKIP_CONDITION is '跳转条件';
comment on column FLOW_SKIP.COORDINATE is '坐标';
comment on column FLOW_SKIP.CREATE_TIME is '创建时间';
comment on column FLOW_SKIP.UPDATE_TIME is '更新时间';
comment on column FLOW_SKIP.DEL_FLAG is '删除标志';
comment on column FLOW_SKIP.TENANT_ID is '租户id';

create table FLOW_INSTANCE
(
    ID              NUMBER       not null,
    DEFINITION_ID   NUMBER       not null,
    BUSINESS_ID     VARCHAR2(40) not null,
    NODE_TYPE       NUMBER(1),
    NODE_CODE       VARCHAR2(100),
    NODE_NAME       VARCHAR2(100),
    VARIABLE        CLOB,
    FLOW_STATUS     VARCHAR2(20),
    ACTIVITY_STATUS NUMBER(1)    default 1,
    DEF_JSON        CLOB,
    CREATE_BY       VARCHAR2(64) default '',
    CREATE_TIME     DATE,
    UPDATE_TIME     DATE,
    EXT             VARCHAR2(500),
    DEL_FLAG        VARCHAR2(1)  default '0',
    TENANT_ID       VARCHAR2(40)
);

alter table FLOW_INSTANCE add constraint PK_FLOW_INSTANCE primary key (ID);

comment on table FLOW_INSTANCE is '流程实例表';
comment on column FLOW_INSTANCE.ID is '主键id';
comment on column FLOW_INSTANCE.DEFINITION_ID is '对应flow_definition表的id';
comment on column FLOW_INSTANCE.BUSINESS_ID is '业务id';
comment on column FLOW_INSTANCE.NODE_TYPE is '开始节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_INSTANCE.NODE_CODE is '开始节点编码';
comment on column FLOW_INSTANCE.NODE_NAME is '开始节点名称';
comment on column FLOW_INSTANCE.VARIABLE is '任务变量';
comment on column FLOW_INSTANCE.FLOW_STATUS is '流程状态（0待提交 1审批中 2 审批通过 3自动通过 4终止 5作废 6撤销 7取回  8已完成 9已退回 10失效）';
comment on column FLOW_INSTANCE.ACTIVITY_STATUS is '流程激活状态（0挂起 1激活）';
comment on column FLOW_INSTANCE.DEF_JSON is '流程定义json';
comment on column FLOW_INSTANCE.CREATE_BY is '创建者';
comment on column FLOW_INSTANCE.CREATE_TIME is '创建时间';
comment on column FLOW_INSTANCE.UPDATE_TIME is '更新时间';
comment on column FLOW_INSTANCE.EXT is '扩展字段，预留给业务系统使用';
comment on column FLOW_INSTANCE.DEL_FLAG is '删除标志';
comment on column FLOW_INSTANCE.TENANT_ID is '租户id';

create table FLOW_TASK
(
    ID            NUMBER(20) not null,
    DEFINITION_ID NUMBER(20) not null,
    INSTANCE_ID   NUMBER(20) not null,
    NODE_CODE     VARCHAR2(100),
    NODE_NAME     VARCHAR2(100),
    NODE_TYPE     NUMBER(1),
    FORM_CUSTOM   VARCHAR2(1) default 'N',
    FORM_PATH     VARCHAR2(100),
    CREATE_TIME   DATE,
    UPDATE_TIME   DATE,
    DEL_FLAG      VARCHAR2(1) default '0',
    TENANT_ID     VARCHAR2(40)
);

alter table FLOW_TASK add constraint PK_FLOW_TASK primary key (ID);

comment on table FLOW_TASK is '待办任务表';
comment on column FLOW_TASK.ID is '主键id';
comment on column FLOW_TASK.DEFINITION_ID is '对应flow_definition表的id';
comment on column FLOW_TASK.INSTANCE_ID is '对应flow_instance表的id';
comment on column FLOW_TASK.NODE_CODE is '节点编码';
comment on column FLOW_TASK.NODE_NAME is '节点名称';
comment on column FLOW_TASK.NODE_TYPE is '节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_TASK.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_TASK.FORM_PATH is '审批表单路径';
comment on column FLOW_TASK.CREATE_TIME is '创建时间';
comment on column FLOW_TASK.UPDATE_TIME is '更新时间';
comment on column FLOW_TASK.DEL_FLAG is '删除标志';
comment on column FLOW_TASK.TENANT_ID is '租户id';

create table FLOW_HIS_TASK
(
    ID               NUMBER(20) not null,
    DEFINITION_ID    NUMBER(20) not null,
    INSTANCE_ID      NUMBER(20) not null,
    TASK_ID          NUMBER(20) not null,
    NODE_CODE        VARCHAR2(100),
    NODE_NAME        VARCHAR2(100),
    NODE_TYPE        NUMBER(1),
    TARGET_NODE_CODE VARCHAR2(200),
    TARGET_NODE_NAME VARCHAR2(200),
    APPROVER         VARCHAR2(40),
    COOPERATE_TYPE   NUMBER(1)   default 0,
    COLLABORATOR     VARCHAR2(40),
    SKIP_TYPE        VARCHAR2(10),
    FLOW_STATUS      VARCHAR2(20),
    FORM_CUSTOM      VARCHAR2(1) default 'N',
    FORM_PATH        VARCHAR2(100),
    MESSAGE          VARCHAR2(500),
    VARIABLE         CLOB,
    EXT              VARCHAR2(500),
    CREATE_TIME      DATE,
    UPDATE_TIME      DATE,
    DEL_FLAG         VARCHAR2(1) default '0',
    TENANT_ID        VARCHAR2(40)

);

alter table FLOW_HIS_TASK add constraint PK_FLOW_HIS_TASK primary key (ID);

comment on table FLOW_HIS_TASK is '历史任务记录表';
comment on column FLOW_HIS_TASK.ID is '主键id';
comment on column FLOW_HIS_TASK.DEFINITION_ID is '对应flow_definition表的id';
comment on column FLOW_HIS_TASK.INSTANCE_ID is '对应flow_instance表的id';
comment on column FLOW_HIS_TASK.TASK_ID is '对应flow_task表的id';
comment on column FLOW_HIS_TASK.NODE_CODE is '开始节点编码';
comment on column FLOW_HIS_TASK.NODE_NAME is '开始节点名称';
comment on column FLOW_HIS_TASK.NODE_TYPE is '开始节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_HIS_TASK.TARGET_NODE_CODE is '目标节点编码';
comment on column FLOW_HIS_TASK.TARGET_NODE_NAME is '目标节点名称';
comment on column FLOW_HIS_TASK.SKIP_TYPE is '流转类型（PASS通过 REJECT退回 NONE无动作）';
comment on column FLOW_HIS_TASK.FLOW_STATUS is '流程状态（1审批中 2 审批通过 9已退回 10失效）';
comment on column FLOW_HIS_TASK.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_HIS_TASK.FORM_PATH is '审批表单路径';
comment on column FLOW_HIS_TASK.MESSAGE is '审批意见';
comment on column FLOW_HIS_TASK.VARIABLE is '任务变量';
comment on column FLOW_HIS_TASK.EXT is '扩展字段，预留给业务系统使用';
comment on column FLOW_HIS_TASK.CREATE_TIME is '任务开始时间';
comment on column FLOW_HIS_TASK.UPDATE_TIME is '审批完成时间';
comment on column FLOW_HIS_TASK.DEL_FLAG is '删除标志';
comment on column FLOW_HIS_TASK.TENANT_ID is '租户id';
comment on column FLOW_HIS_TASK.APPROVER is '审批者';
comment on column FLOW_HIS_TASK.COOPERATE_TYPE is '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)';
comment on column FLOW_HIS_TASK.COLLABORATOR is '协作人';

create table FLOW_USER
(
    ID           NUMBER(20)  not null,
    TYPE         VARCHAR2(1) not null,
    PROCESSED_BY VARCHAR2(80),
    ASSOCIATED   NUMBER(20)  not null,
    CREATE_TIME  DATE,
    CREATE_BY    VARCHAR2(80),
    UPDATE_TIME  DATE,
    DEL_FLAG     VARCHAR2(1) default '0',
    TENANT_ID    VARCHAR2(40)
);

alter table FLOW_USER add constraint PK_FLOW_USER primary key (ID);

comment on table FLOW_USER is '待办任务表';
comment on column FLOW_USER.ID is '主键id';
comment on column FLOW_USER.TYPE is '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）';
comment on column FLOW_USER.PROCESSED_BY is '权限人)';
comment on column FLOW_USER.ASSOCIATED is '任务表id';
comment on column FLOW_USER.CREATE_TIME is '创建时间';
comment on column FLOW_USER.CREATE_BY is '节点名称';
comment on column FLOW_USER.UPDATE_TIME is '更新时间';
comment on column FLOW_USER.DEL_FLAG is '删除标志';
comment on column FLOW_USER.TENANT_ID is '租户id';

create index USER_PROCESSED_TYPE on FLOW_USER (PROCESSED_BY, TYPE);
create index USER_ASSOCIATED_IDX on FLOW_USER (ASSOCIATED);

-- ----------------------------
-- 流程分类表
-- ----------------------------
CREATE TABLE flow_category
(
    category_id NUMBER (20) NOT NULL,
    tenant_id VARCHAR2 (20) DEFAULT '000000',
    parent_id NUMBER (20) DEFAULT 0,
    ancestors VARCHAR2 (500) DEFAULT '',
    category_name VARCHAR2 (30) NOT NULL,
    order_num NUMBER (4) DEFAULT 0,
    del_flag    CHAR(1) DEFAULT '0',
    create_dept NUMBER (20),
    create_by NUMBER (20),
    create_time DATE,
    update_by NUMBER (20),
    update_time DATE
);

alter table flow_category add constraint pk_flow_category primary key (category_id);

COMMENT ON TABLE flow_category IS '流程分类';
COMMENT ON COLUMN flow_category.category_id IS '流程分类ID';
COMMENT ON COLUMN flow_category.tenant_id IS '租户编号';
COMMENT ON COLUMN flow_category.parent_id IS '父流程分类id';
COMMENT ON COLUMN flow_category.ancestors IS '祖级列表';
COMMENT ON COLUMN flow_category.category_name IS '流程分类名称';
COMMENT ON COLUMN flow_category.order_num IS '显示顺序';
COMMENT ON COLUMN flow_category.del_flag IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN flow_category.create_dept IS '创建部门';
COMMENT ON COLUMN flow_category.create_by IS '创建者';
COMMENT ON COLUMN flow_category.create_time IS '创建时间';
COMMENT ON COLUMN flow_category.update_by IS '更新者';
COMMENT ON COLUMN flow_category.update_time IS '更新时间';

INSERT INTO flow_category VALUES (100, '000000', 0, '0', 'OA审批', 0, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (101, '000000', 100, '0,100', '假勤管理', 0, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (102, '000000', 100, '0,100', '人事管理', 1, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (103, '000000', 101, '0,100,101', '请假', 0, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (104, '000000', 101, '0,100,101', '出差', 1, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (105, '000000', 101, '0,100,101', '加班', 2, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (106, '000000', 101, '0,100,101', '换班', 3, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (107, '000000', 101, '0,100,101', '外出', 4, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (108, '000000', 102, '0,100,102', '转正', 1, '0', 103, 1, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (109, '000000', 102, '0,100,102', '离职', 2, '0', 103, 1, SYSDATE, NULL, NULL);


-- ----------------------------
-- 请假单信息
-- ----------------------------
CREATE TABLE test_leave
(
    id NUMBER (20) NOT NULL,
    tenant_id VARCHAR2 (20) DEFAULT '000000',
    leave_type VARCHAR2 (255) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    leave_days NUMBER (10) NOT NULL,
    remark VARCHAR2 (255),
    status VARCHAR2 (255),
    create_dept NUMBER (20),
    create_by NUMBER (20),
    create_time DATE,
    update_by NUMBER (20),
    update_time DATE
);

alter table test_leave add constraint pk_test_leave primary key (id);

COMMENT ON TABLE test_leave IS '请假申请表';
COMMENT ON COLUMN test_leave.id IS 'ID';
COMMENT ON COLUMN test_leave.tenant_id IS '租户编号';
COMMENT ON COLUMN test_leave.leave_type IS '请假类型';
COMMENT ON COLUMN test_leave.start_date IS '开始时间';
COMMENT ON COLUMN test_leave.end_date IS '结束时间';
COMMENT ON COLUMN test_leave.leave_days IS '请假天数';
COMMENT ON COLUMN test_leave.remark IS '请假原因';
COMMENT ON COLUMN test_leave.status IS '状态';
COMMENT ON COLUMN test_leave.create_dept IS '创建部门';
COMMENT ON COLUMN test_leave.create_by IS '创建者';
COMMENT ON COLUMN test_leave.create_time IS '创建时间';
COMMENT ON COLUMN test_leave.update_by IS '更新者';
COMMENT ON COLUMN test_leave.update_time IS '更新时间';

INSERT INTO sys_menu VALUES ('11616', '工作流', '0', '6', 'workflow', '', '', '1', '0', 'M', '0', '0', '', 'workflow', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11618', '我的任务', '0', '7', 'task', '', '', '1', '0', 'M', '0', '0', '', 'my-task', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11619', '我的待办', '11618', '2', 'taskWaiting', 'workflow/task/taskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11632', '我的已办', '11618', '3', 'taskFinish', 'workflow/task/taskFinish', '', '1', '1', 'C', '0', '0', '', 'finish', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11633', '我的抄送', '11618', '4', 'taskCopyList', 'workflow/task/taskCopyList', '', '1', '1', 'C', '0', '0', '', 'my-copy', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '', 'process-definition', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11621', '流程实例', '11630', '1', 'processInstance', 'workflow/processInstance/index', '', '1', '1', 'C', '0', '0', '', 'tree-table', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11622', '流程分类', '11616', '1', 'category', 'workflow/category/index', '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11629', '我发起的', '11618', '1', 'myDocument', 'workflow/task/myDocument', '', '1', '1', 'C', '0', '0', '', 'guide', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11630', '流程监控', '11616', '4', 'monitor', '', '', '1', '0', 'M', '0', '0', '', 'monitor', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11631', '待办任务', '11630', '2', 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, SYSDATE, NULL, NULL, '');

INSERT INTO sys_menu VALUES ('11623', '流程分类查询', '11622', '1', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:query', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11624', '流程分类新增', '11622', '2', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:add', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11625', '流程分类修改', '11622', '3', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:edit', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11626', '流程分类删除', '11622', '4', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:remove', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11627', '流程分类导出', '11622', '5', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:export', '#', 103, 1, SYSDATE, NULL, NULL, '');

INSERT INTO sys_menu VALUES ('11638', '请假申请', '5', '1', 'leave', 'workflow/leave/index', '', '1', '0', 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, SYSDATE, NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu VALUES ('11639', '请假申请查询', '11638', '1', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11640', '请假申请新增', '11638', '2', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11641', '请假申请修改', '11638', '3', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11642', '请假申请删除', '11638', '4', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11643', '请假申请导出', '11638', '5', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, SYSDATE, NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, SYSDATE, NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, SYSDATE, NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (15, '000000', '任务状态', 'wf_task_status', 103, 1, SYSDATE, NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '已撤销');
INSERT INTO sys_dict_data VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, SYSDATE, NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, SYSDATE, NULL, NULL, '已完成');
INSERT INTO sys_dict_data VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '已作废');
INSERT INTO sys_dict_data VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '已退回');
INSERT INTO sys_dict_data VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '已终止');
INSERT INTO sys_dict_data VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, SYSDATE, NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '动态表单');
INSERT INTO sys_dict_data VALUES (48, '000000', 1, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (49, '000000', 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 103, 1, SYSDATE, NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (50, '000000', 3, '待审核', 'waiting', 'wf_task_status', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (51, '000000', 4, '作废', 'invalid', 'wf_task_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '作废');
INSERT INTO sys_dict_data VALUES (52, '000000', 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (53, '000000', 6, '终止', 'termination', 'wf_task_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '终止');
INSERT INTO sys_dict_data VALUES (54, '000000', 7, '转办', 'transfer', 'wf_task_status', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (55, '000000', 8, '委托', 'depute', 'wf_task_status', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '委托');
INSERT INTO sys_dict_data VALUES (56, '000000', 9, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (57, '000000', 10, '加签', 'sign', 'wf_task_status', '', 'primary', 'N', 103, 1, SYSDATE, NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (58, '000000', 11, '减签', 'sign_off', 'wf_task_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '减签');
INSERT INTO sys_dict_data VALUES (59, '000000', 11, '超时', 'timeout', 'wf_task_status', '', 'danger', 'N', 103, 1, SYSDATE, NULL, NULL, '超时');
