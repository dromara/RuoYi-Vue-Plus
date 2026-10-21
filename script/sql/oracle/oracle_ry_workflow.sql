-- ----------------------------
-- 0、warm-flow-all.sql，地址：https://gitee.com/dromara/warm-flow/blob/master/sql/oracle/oracle-wram-flow-all.sql
-- ----------------------------
create table FLOW_DEFINITION
(
    ID              NUMBER(20)            not null,
    FLOW_CODE       VARCHAR2(40)          not null,
    FLOW_NAME       VARCHAR2(100)         not null,
    MODEL_VALUE     VARCHAR2(40) default 'CLASSICS' not null,
    CATEGORY        VARCHAR2(100),
    VERSION         VARCHAR2(20)          not null,
    IS_PUBLISH      NUMBER(1)   default 0 not null,
    FORM_CUSTOM     VARCHAR2(1) default 'N',
    FORM_PATH       VARCHAR2(100),
    ACTIVITY_STATUS NUMBER(1)   default 1 not null,
    LISTENER_TYPE   VARCHAR2(100),
    LISTENER_PATH   VARCHAR2(400),
    EXT             VARCHAR2(500),
    DEF_CONFIG     CLOB,
    CREATE_TIME     DATE,
    CREATE_BY       VARCHAR2(64) default '',
    UPDATE_TIME     DATE,
    UPDATE_BY       VARCHAR2(64) default '',
    DEL_FLAG        VARCHAR2(1) default '0',
    TENANT_ID       VARCHAR2(40)
);

alter table FLOW_DEFINITION
    add constraint PK_FLOW_DEFINITION primary key (ID);

comment on table FLOW_DEFINITION is '流程定义表';
comment on column FLOW_DEFINITION.ID is '主键id';
comment on column FLOW_DEFINITION.FLOW_CODE is '流程编码';
comment on column FLOW_DEFINITION.FLOW_NAME is '流程名称';
comment on column FLOW_DEFINITION.MODEL_VALUE is '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）';
comment on column FLOW_DEFINITION.CATEGORY is '流程类别';
comment on column FLOW_DEFINITION.VERSION is '流程版本';
comment on column FLOW_DEFINITION.IS_PUBLISH is '是否发布 (0未发布 1已发布 9失效)';
comment on column FLOW_DEFINITION.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_DEFINITION.FORM_PATH is '审批表单路径';
comment on column FLOW_DEFINITION.ACTIVITY_STATUS is '流程激活状态（0挂起 1激活）';
comment on column FLOW_DEFINITION.LISTENER_TYPE is '监听器类型';
comment on column FLOW_DEFINITION.LISTENER_PATH is '监听器路径';
comment on column FLOW_DEFINITION.EXT is '扩展字段，预留给业务系统使用';
comment on column FLOW_DEFINITION.DEF_CONFIG is '流程定义配置（自动审批等引擎配置与自定义扩展属性json）';
comment on column FLOW_DEFINITION.CREATE_TIME is '创建时间';
comment on column FLOW_DEFINITION.CREATE_BY is '创建人';
comment on column FLOW_DEFINITION.UPDATE_TIME is '更新时间';
comment on column FLOW_DEFINITION.UPDATE_BY is '更新人';
comment on column FLOW_DEFINITION.DEL_FLAG is '删除标志';
comment on column FLOW_DEFINITION.TENANT_ID is '租户id';

create table FLOW_NODE
(
    ID              NUMBER(20)    not null,
    NODE_TYPE       NUMBER(1)     not null,
    DEFINITION_ID   NUMBER(20)    not null,
    NODE_CODE       VARCHAR2(100) not null,
    NODE_NAME       VARCHAR2(100),
    PERMISSION_FLAG VARCHAR2(200),
    NODE_RATIO      VARCHAR2(200),
    COORDINATE      VARCHAR2(100),
    ANY_NODE_SKIP   VARCHAR2(100),
    LISTENER_TYPE   VARCHAR2(100),
    LISTENER_PATH   VARCHAR2(400),
    FORM_CUSTOM     VARCHAR2(1)   default 'N',
    FORM_PATH       VARCHAR2(100),
    CREATE_TIME     DATE,
    CREATE_BY       VARCHAR2(64) default '',
    UPDATE_TIME     DATE,
    UPDATE_BY       VARCHAR2(64) default '',
    EXT             CLOB,
    NODE_CONFIG CLOB,
    EVENT_TYPE          VARCHAR2(40),
    EVENT_NAME          VARCHAR2(100),
    ATTACHED_TO_NODE_CODE VARCHAR2(100),
    INTERRUPTING        NUMBER(1),
    DEL_FLAG        VARCHAR2(1)   default '0',
    TENANT_ID       VARCHAR2(40)
);

alter table FLOW_NODE
    add constraint PK_FLOW_NODE primary key (ID);

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
comment on column FLOW_NODE.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_NODE.FORM_PATH is '审批表单路径';
comment on column FLOW_NODE.CREATE_TIME is '创建时间';
comment on column FLOW_NODE.CREATE_BY is '创建人';
comment on column FLOW_NODE.UPDATE_TIME is '更新时间';
comment on column FLOW_NODE.UPDATE_BY is '更新人';
comment on column FLOW_NODE.EXT is '节点扩展属性';
comment on column FLOW_NODE.NODE_CONFIG is '节点配置（服务任务、事件等引擎节点配置json）';
comment on column FLOW_NODE.EVENT_TYPE is '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）';
comment on column FLOW_NODE.EVENT_NAME is '事件名称（消息事件使用）';
comment on column FLOW_NODE.ATTACHED_TO_NODE_CODE is '边界事件附着的任务节点编码';
comment on column FLOW_NODE.INTERRUPTING is '边界事件是否中断型';
comment on column FLOW_NODE.DEL_FLAG is '删除标志';
comment on column FLOW_NODE.TENANT_ID is '租户id';
comment on column FLOW_NODE.PERMISSION_FLAG is '权限标识（权限类型:权限标识，可以多个，用@@隔开)';

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
    CREATE_BY      VARCHAR2(64) default '',
    UPDATE_TIME    DATE,
    UPDATE_BY      VARCHAR2(64) default '',
    DEL_FLAG       VARCHAR2(1) default '0',
    TENANT_ID      VARCHAR2(40)
);

alter table FLOW_SKIP
    add constraint PK_FLOW_SKIP primary key (ID);

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
comment on column FLOW_SKIP.CREATE_BY is '创建人';
comment on column FLOW_SKIP.UPDATE_TIME is '更新时间';
comment on column FLOW_SKIP.UPDATE_BY is '更新人';
comment on column FLOW_SKIP.DEL_FLAG is '删除标志';
comment on column FLOW_SKIP.TENANT_ID is '租户id';

create table FLOW_INSTANCE
(
    ID              NUMBER       not null,
    DEFINITION_ID   NUMBER       not null,
    BUSINESS_ID     VARCHAR2(40) not null,
    NODE_TYPE       NUMBER(1) not null,
    NODE_CODE       VARCHAR2(100) not null,
    NODE_NAME       VARCHAR2(100),
    VARIABLE        CLOB,
    FLOW_STATUS     VARCHAR2(20) not null,
    ACTIVITY_STATUS NUMBER(1)    default 1 not null,
    DEF_JSON        CLOB,
    CREATE_TIME     DATE,
    CREATE_BY       VARCHAR2(64) default '',
    UPDATE_TIME     DATE,
    UPDATE_BY       VARCHAR2(64) default '',
    EXT             VARCHAR2(500),
    DEL_FLAG        VARCHAR2(1)  default '0',
    TENANT_ID       VARCHAR2(40)
);

alter table FLOW_INSTANCE
    add constraint PK_FLOW_INSTANCE primary key (ID);

comment on table FLOW_INSTANCE is '流程实例表';
comment on column FLOW_INSTANCE.ID is '主键id';
comment on column FLOW_INSTANCE.DEFINITION_ID is '对应flow_definition表的id';
comment on column FLOW_INSTANCE.BUSINESS_ID is '业务id';
comment on column FLOW_INSTANCE.NODE_TYPE is '开始节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_INSTANCE.NODE_CODE is '开始节点编码';
comment on column FLOW_INSTANCE.NODE_NAME is '开始节点名称';
comment on column FLOW_INSTANCE.VARIABLE is '任务变量';
comment on column FLOW_INSTANCE.FLOW_STATUS is '实例状态（draft待提交 waiting审批中 termination终止 invalid作废 cancel撤销 finish已完成 back已退回 restart重启）';
comment on column FLOW_INSTANCE.ACTIVITY_STATUS is '流程激活状态（0挂起 1激活）';
comment on column FLOW_INSTANCE.DEF_JSON is '流程定义json';
comment on column FLOW_INSTANCE.CREATE_TIME is '创建时间';
comment on column FLOW_INSTANCE.CREATE_BY is '创建人';
comment on column FLOW_INSTANCE.UPDATE_TIME is '更新时间';
comment on column FLOW_INSTANCE.UPDATE_BY is '更新人';
comment on column FLOW_INSTANCE.EXT is '扩展字段，预留给业务系统使用';
comment on column FLOW_INSTANCE.DEL_FLAG is '删除标志';
comment on column FLOW_INSTANCE.TENANT_ID is '租户id';

create table FLOW_TASK
(
    ID            NUMBER(20) not null,
    DEFINITION_ID NUMBER(20) not null,
    INSTANCE_ID   NUMBER(20) not null,
    NODE_CODE     VARCHAR2(100) not null,
    NODE_NAME     VARCHAR2(100),
    NODE_TYPE     NUMBER(1) not null,
    FLOW_STATUS   VARCHAR2(20) not null,
    FORM_CUSTOM   VARCHAR2(1) default 'N',
    FORM_PATH     VARCHAR2(100),
    CREATE_TIME   DATE,
    CREATE_BY     VARCHAR2(64) default '',
    UPDATE_TIME   DATE,
    UPDATE_BY     VARCHAR2(64) default '',
    DEL_FLAG      VARCHAR2(1) default '0',
    TENANT_ID     VARCHAR2(40)
);

alter table FLOW_TASK
    add constraint PK_FLOW_TASK primary key (ID);

comment on table FLOW_TASK is '待办任务表';
comment on column FLOW_TASK.ID is '主键id';
comment on column FLOW_TASK.DEFINITION_ID is '对应flow_definition表的id';
comment on column FLOW_TASK.INSTANCE_ID is '对应flow_instance表的id';
comment on column FLOW_TASK.NODE_CODE is '节点编码';
comment on column FLOW_TASK.NODE_NAME is '节点名称';
comment on column FLOW_TASK.NODE_TYPE is '节点类型 (0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关)';
comment on column FLOW_TASK.FLOW_STATUS is '任务状态（pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送；任务结果，勿套用实例状态编码）';
comment on column FLOW_TASK.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_TASK.FORM_PATH is '审批表单路径';
comment on column FLOW_TASK.CREATE_TIME is '创建时间';
comment on column FLOW_TASK.CREATE_BY is '创建人';
comment on column FLOW_TASK.UPDATE_TIME is '更新时间';
comment on column FLOW_TASK.UPDATE_BY is '更新人';
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
    COOPERATE_TYPE   VARCHAR2(20) default 'none' not null,
    COLLABORATOR     VARCHAR2(500),
    SKIP_TYPE        VARCHAR2(10) not null,
    FLOW_STATUS      VARCHAR2(20) not null,
    FORM_CUSTOM      VARCHAR2(1) default 'N',
    FORM_PATH        VARCHAR2(100),
    MESSAGE          VARCHAR2(500),
    VARIABLE         CLOB,
    EXT              CLOB,
    NODE_CONFIG      CLOB,
    CREATE_TIME      DATE,
    UPDATE_TIME      DATE,
    DEL_FLAG         VARCHAR2(1) default '0',
    TENANT_ID        VARCHAR2(40)

);

alter table FLOW_HIS_TASK
    add constraint PK_FLOW_HIS_TASK primary key (ID);

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
comment on column FLOW_HIS_TASK.FLOW_STATUS is '任务状态（pending待审批 pass通过 auto_pass自动审批 back退回 invalid失效 take_back拿回 copy抄送；任务结果，勿套用实例状态编码，唯一例外是撤回归档写 cancel）';
comment on column FLOW_HIS_TASK.FORM_CUSTOM is '审批表单是否自定义 (Y是 N否)';
comment on column FLOW_HIS_TASK.FORM_PATH is '审批表单路径';
comment on column FLOW_HIS_TASK.MESSAGE is '审批意见';
comment on column FLOW_HIS_TASK.VARIABLE is '任务变量';
comment on column FLOW_HIS_TASK.EXT is '扩展字段，预留给业务系统使用';
comment on column FLOW_HIS_TASK.NODE_CONFIG is '节点配置（服务任务等引擎配置与自定义扩展属性json）';
comment on column FLOW_HIS_TASK.CREATE_TIME is '任务开始时间';
comment on column FLOW_HIS_TASK.UPDATE_TIME is '审批完成时间';
comment on column FLOW_HIS_TASK.DEL_FLAG is '删除标志';
comment on column FLOW_HIS_TASK.TENANT_ID is '租户id';
comment on column FLOW_HIS_TASK.APPROVER is '审批者';
comment on column FLOW_HIS_TASK.COOPERATE_TYPE is '协作方式（none无 transfer转办 delegate委派 add_signer加签 remove_signer减签；行为维度，抄送/会签/票签不落此列，结果见 flow_status）';
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
    UPDATE_BY    VARCHAR2(64) default '',
    DEL_FLAG     VARCHAR2(1) default '0',
    TENANT_ID    VARCHAR2(40)
);

alter table FLOW_USER
    add constraint PK_FLOW_USER primary key (ID);

comment on table FLOW_USER is '待办任务表';
comment on column FLOW_USER.ID is '主键id';
comment on column FLOW_USER.TYPE is '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限 4待办任务的抄送人）';
comment on column FLOW_USER.PROCESSED_BY is '权限人)';
comment on column FLOW_USER.ASSOCIATED is '关联id（待办任务id；抄送人记录为 flow_his_task.id）';
comment on column FLOW_USER.CREATE_TIME is '创建时间';
comment on column FLOW_USER.CREATE_BY is '创建人';
comment on column FLOW_USER.UPDATE_TIME is '更新时间';
comment on column FLOW_USER.UPDATE_BY is '更新人';
comment on column FLOW_USER.DEL_FLAG is '删除标志';
comment on column FLOW_USER.TENANT_ID is '租户id';

create index USER_PROCESSED_TYPE on FLOW_USER (PROCESSED_BY, TYPE);
create index USER_ASSOCIATED_IDX on FLOW_USER (ASSOCIATED);

-- ----------------------------
-- 事件等待表
-- ----------------------------
-- 定时 / 消息等需要“等待”的事件统一落在此表，状态独立于流程状态。
create table FLOW_EVENT_WAIT
(
    ID                NUMBER(20)    not null,
    TENANT_ID         VARCHAR2(40),
    DEL_FLAG          VARCHAR2(1) default '0',
    DEFINITION_ID     NUMBER(20),
    INSTANCE_ID       NUMBER(20),
    NODE_CODE         VARCHAR2(100),
    EVENT_TYPE        VARCHAR2(40)  not null,
    EVENT_NAME        VARCHAR2(100),
    ATTACHED_TASK_ID  NUMBER(20),
    TRIGGER_TYPE      VARCHAR2(40),
    TRIGGER_INTERVAL  VARCHAR2(100),
    NEXT_TRIGGER_AT   DATE,
    TIMEZONE          VARCHAR2(40),
    REPEAT_COUNT      NUMBER(10),
    MAX_REPEAT_COUNT  NUMBER(10),
    STATUS            VARCHAR2(20)  not null,
    RETRY_COUNT       NUMBER(10),
    MAX_RETRY_COUNT   NUMBER(10),
    RETRY_AT          DATE,
    LOCK_OWNER        VARCHAR2(64),
    LOCK_UNTIL        DATE,
    VERSION           NUMBER(10),
    PAYLOAD           CLOB,
    LAST_ERROR        VARCHAR2(500),
    CREATE_TIME       DATE,
    UPDATE_TIME       DATE
);

alter table FLOW_EVENT_WAIT
    add constraint PK_FLOW_EVENT_WAIT primary key (ID);

comment on table FLOW_EVENT_WAIT is '事件等待表';
comment on column FLOW_EVENT_WAIT.ID is '主键id';
comment on column FLOW_EVENT_WAIT.TENANT_ID is '租户id';
comment on column FLOW_EVENT_WAIT.DEL_FLAG is '删除标志';
comment on column FLOW_EVENT_WAIT.DEFINITION_ID is '流程定义id（定时启动事件必须有）';
comment on column FLOW_EVENT_WAIT.INSTANCE_ID is '流程实例id（实例等待事件必须有）';
comment on column FLOW_EVENT_WAIT.NODE_CODE is '事件节点编码（定位沿用 definition_id + node_code）';
comment on column FLOW_EVENT_WAIT.EVENT_TYPE is '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）';
comment on column FLOW_EVENT_WAIT.EVENT_NAME is '事件名称（消息事件使用）';
comment on column FLOW_EVENT_WAIT.ATTACHED_TASK_ID is '边界事件关联的待办任务id';
comment on column FLOW_EVENT_WAIT.TRIGGER_TYPE is '触发策略类型（AFTER FIXED POINT_IN_TIME）';
comment on column FLOW_EVENT_WAIT.TRIGGER_INTERVAL is '触发策略原始配置';
comment on column FLOW_EVENT_WAIT.NEXT_TRIGGER_AT is '下次触发时间';
comment on column FLOW_EVENT_WAIT.TIMEZONE is '定时触发使用的时区';
comment on column FLOW_EVENT_WAIT.REPEAT_COUNT is '已触发次数';
comment on column FLOW_EVENT_WAIT.MAX_REPEAT_COUNT is '最大触发次数';
comment on column FLOW_EVENT_WAIT.STATUS is '等待状态（WAITING RUNNING CANCELLED RETRY DEAD）';
comment on column FLOW_EVENT_WAIT.RETRY_COUNT is '已重试次数';
comment on column FLOW_EVENT_WAIT.MAX_RETRY_COUNT is '最大重试次数';
comment on column FLOW_EVENT_WAIT.RETRY_AT is '下次重试时间';
comment on column FLOW_EVENT_WAIT.LOCK_OWNER is '抢占持有者标识';
comment on column FLOW_EVENT_WAIT.LOCK_UNTIL is '抢占租约到期时间';
comment on column FLOW_EVENT_WAIT.VERSION is '乐观锁版本号';
comment on column FLOW_EVENT_WAIT.PAYLOAD is '消息事件接收的业务载荷';
comment on column FLOW_EVENT_WAIT.LAST_ERROR is '最近一次失败原因摘要';
comment on column FLOW_EVENT_WAIT.CREATE_TIME is '创建时间';
comment on column FLOW_EVENT_WAIT.UPDATE_TIME is '更新时间';

create index EVENT_WAIT_STATUS_NEXT on FLOW_EVENT_WAIT (STATUS, NEXT_TRIGGER_AT, ID);
create index EVENT_WAIT_STATUS_RETRY on FLOW_EVENT_WAIT (STATUS, RETRY_AT, ID);
create index EVENT_WAIT_TENANT_NEXT on FLOW_EVENT_WAIT (TENANT_ID, STATUS, NEXT_TRIGGER_AT, ID);
create index EVENT_WAIT_TENANT_RETRY on FLOW_EVENT_WAIT (TENANT_ID, STATUS, RETRY_AT, ID);
create index EVENT_WAIT_INSTANCE_TASK on FLOW_EVENT_WAIT (INSTANCE_ID, ATTACHED_TASK_ID);
create index EVENT_WAIT_NAME_INST_STATUS on FLOW_EVENT_WAIT (EVENT_NAME, INSTANCE_ID, STATUS);
create index EVENT_WAIT_DEF_NODE_STATUS on FLOW_EVENT_WAIT (DEFINITION_ID, NODE_CODE, STATUS);

-- ----------------------------
-- 流程分类表
-- ----------------------------
CREATE TABLE flow_category
(
    category_id     NUMBER(20) NOT NULL,
    tenant_id       VARCHAR2(20) DEFAULT '000000',
    parent_id       NUMBER(20) DEFAULT 0,
    ancestors       VARCHAR2(500) DEFAULT '',
    category_name   VARCHAR2(30) NOT NULL,
    order_num       NUMBER(4) DEFAULT 0,
    del_flag        CHAR(1) DEFAULT '0',
    create_dept     NUMBER(20),
    create_by       NUMBER(20),
    create_time     DATE,
    update_by       NUMBER(20),
    update_time     DATE
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

INSERT INTO flow_category VALUES (1762300000000000100, 0, '0', 'OA审批', 0, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000101, 1762300000000000100, '0,1762300000000000100', '假勤管理', 0, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000102, 1762300000000000100, '0,1762300000000000100', '人事管理', 1, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000103, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '请假', 0, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000104, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '出差', 1, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000105, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '加班', 2, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000106, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '换班', 3, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000107, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '外出', 4, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000108, 1762300000000000102, '0,1762300000000000100,1762300000000000102', '转正', 1, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000109, 1762300000000000102, '0,1762300000000000100,1762300000000000102', '离职', 2, '0', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL);

-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------
CREATE TABLE flow_spel (
    id              NUMBER(20) NOT NULL,
    component_name  VARCHAR2(255),
    method_name     VARCHAR2(255),
    method_params   VARCHAR2(255),
    view_spel       VARCHAR2(255),
    remark          VARCHAR2(255),
    status          CHAR(1) DEFAULT '0',
    del_flag        CHAR(1) DEFAULT '0',
    create_dept     NUMBER(20),
    create_by       NUMBER(20),
    create_time     DATE,
    update_by       NUMBER(20),
    update_time     DATE
);

alter table flow_spel add constraint pk_flow_spel primary key (id);

COMMENT ON TABLE flow_spel IS '流程spel表达式定义表';
COMMENT ON COLUMN flow_spel.id IS '主键id';
COMMENT ON COLUMN flow_spel.component_name IS '组件名称';
COMMENT ON COLUMN flow_spel.method_name IS '方法名';
COMMENT ON COLUMN flow_spel.method_params IS '参数';
COMMENT ON COLUMN flow_spel.view_spel IS '预览spel表达式';
COMMENT ON COLUMN flow_spel.remark IS '备注';
COMMENT ON COLUMN flow_spel.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN flow_spel.del_flag IS '删除标志';
COMMENT ON COLUMN flow_spel.create_dept IS '创建部门';
COMMENT ON COLUMN flow_spel.create_by IS '创建者';
COMMENT ON COLUMN flow_spel.create_time IS '创建时间';
COMMENT ON COLUMN flow_spel.update_by IS '更新者';
COMMENT ON COLUMN flow_spel.update_time IS '更新时间';

INSERT INTO flow_spel VALUES (1762400000000000001, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE, 1761100000000000001, SYSDATE);
INSERT INTO flow_spel VALUES (1762400000000000002, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE, 1761100000000000001, SYSDATE);

-- ----------------------------
-- 流程实例业务扩展表
-- ----------------------------
CREATE TABLE flow_instance_biz_ext (
   id             NUMBER(20),
   tenant_id      VARCHAR2(20)  DEFAULT '000000',
   create_dept    NUMBER(20),
   create_by      NUMBER(20),
   create_time    TIMESTAMP,
   update_by      NUMBER(20),
   update_time    TIMESTAMP,
   business_code  VARCHAR2(255),
   business_title VARCHAR2(1000),
   del_flag       CHAR(1)       DEFAULT '0',
   instance_id    NUMBER(20),
   business_id    VARCHAR2(255)
);

alter table flow_instance_biz_ext add constraint pk_fi_biz_ext primary key (id);

COMMENT ON TABLE flow_instance_biz_ext IS '流程实例业务扩展表';
COMMENT ON COLUMN flow_instance_biz_ext.id  IS '主键id';
COMMENT ON COLUMN flow_instance_biz_ext.tenant_id  IS '租户编号';
COMMENT ON COLUMN flow_instance_biz_ext.create_dept  IS '创建部门';
COMMENT ON COLUMN flow_instance_biz_ext.create_by  IS '创建者';
COMMENT ON COLUMN flow_instance_biz_ext.create_time  IS '创建时间';
COMMENT ON COLUMN flow_instance_biz_ext.update_by  IS '更新者';
COMMENT ON COLUMN flow_instance_biz_ext.update_time  IS '更新时间';
COMMENT ON COLUMN flow_instance_biz_ext.business_code  IS '业务编码';
COMMENT ON COLUMN flow_instance_biz_ext.business_title  IS '业务标题';
COMMENT ON COLUMN flow_instance_biz_ext.del_flag  IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN flow_instance_biz_ext.instance_id  IS '流程实例Id';
COMMENT ON COLUMN flow_instance_biz_ext.business_id  IS '业务Id';

-- ----------------------------
-- 请假单信息
-- ----------------------------
CREATE TABLE test_leave
(
    id          NUMBER (20) NOT NULL,
    tenant_id   VARCHAR2 (20) DEFAULT '000000',
    apply_code  VARCHAR2 (50) NOT NULL,
    leave_type  VARCHAR2 (255) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    leave_days  NUMBER (10) NOT NULL,
    remark      VARCHAR2 (255),
    status      VARCHAR2 (255),
    create_dept NUMBER (20),
    create_by   NUMBER (20),
    create_time DATE,
    update_by   NUMBER (20),
    update_time DATE
);

alter table test_leave add constraint pk_test_leave primary key (id);

COMMENT ON TABLE test_leave IS '请假申请表';
COMMENT ON COLUMN test_leave.id IS 'ID';
COMMENT ON COLUMN test_leave.tenant_id IS '租户编号';
COMMENT ON COLUMN test_leave.apply_code IS '申请编号';
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

INSERT INTO sys_menu VALUES (1761400000000011616, '工作流', 0, 6, 'workflow', '', '', 'N', 'Y', 'M', '0', '0', '', 'workflow', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011618, '我的任务', 0, 7, 'task', '', '', 'N', 'Y', 'M', '0', '0', '', 'my-task', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011619, '我的待办', 1761400000000011618, 2, 'taskWaiting', 'workflow/task/taskWaiting', '', 'N', 'N', 'C', '0', '0', '', 'waiting', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011632, '我的已办', 1761400000000011618, 3, 'taskFinish', 'workflow/task/taskFinish', '', 'N', 'N', 'C', '0', '0', '', 'finish', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011633, '我的抄送', 1761400000000011618, 4, 'taskCopyList', 'workflow/task/taskCopyList', '', 'N', 'N', 'C', '0', '0', '', 'my-copy', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011620, '流程定义', 1761400000000011616, 3, 'processDefinition', 'workflow/processDefinition/index', '', 'N', 'N', 'C', '0', '0', 'workflow:definition:list', 'process-definition', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011621, '流程实例', 1761400000000011630, 1, 'processInstance', 'workflow/processInstance/index', '', 'N', 'N', 'C', '0', '0', 'workflow:instance:list', 'tree-table', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011622, '流程分类', 1761400000000011616, 1, 'category', 'workflow/category/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:category:list', 'category', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011629, '我发起的', 1761400000000011618, 1, 'myDocument', 'workflow/task/myDocument', '', 'N', 'N', 'C', '0', '0', 'workflow:instance:currentList', 'guide', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011630, '流程监控', 1761400000000011616, 4, 'processMonitor', '', '', 'N', 'Y', 'M', '0', '0', '', 'monitor', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011631, '待办任务', 1761400000000011630, 2, 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', 'N', 'N', 'C', '0', '0', 'workflow:task:list', 'waiting', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011660, '待办任务修改', 1761400000000011631, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:task:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011700, '流程设计', 1761400000000011616, 5, 'design/index', 'workflow/processDefinition/design', '', 'N', 'N', 'C', '1', '0', 'workflow:leave:edit', '#', '/workflow/processDefinition', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011701, '请假申请', 1761400000000011616, 6, 'leaveEdit/index', 'workflow/leave/leaveEdit', '', 'N', 'N', 'C', '1', '0', 'workflow:leave:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

INSERT INTO sys_menu VALUES (1761400000000011623, '流程分类查询', 1761400000000011622, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011624, '流程分类新增', 1761400000000011622, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011625, '流程分类修改', 1761400000000011622, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011626, '流程分类删除', 1761400000000011622, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011627, '流程分类导出', 1761400000000011622, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

-- 流程实例管理相关按钮
INSERT INTO sys_menu VALUES (1761400000000011653, '流程实例查询', 1761400000000011621, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011654, '流程变量查询', 1761400000000011621, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variableQuery', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011655, '流程变量修改', 1761400000000011621, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variable', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011656, '流程实例激活/挂起', 1761400000000011621, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:active', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011657, '流程实例删除', 1761400000000011621, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011658, '流程实例作废', 1761400000000011621, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:invalid', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011659, '流程实例撤销', 1761400000000011621, 7, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:cancel', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

-- 流程定义管理相关按钮
INSERT INTO sys_menu VALUES (1761400000000011644, '流程定义查询', 1761400000000011620, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011645, '流程定义新增', 1761400000000011620, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011646, '流程定义修改', 1761400000000011620, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011647, '流程定义删除', 1761400000000011620, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011648, '流程定义导出', 1761400000000011620, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011649, '流程定义导入', 1761400000000011620, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:import', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011650, '流程定义发布/取消发布', 1761400000000011620, 7, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:publish', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011651, '流程定义复制', 1761400000000011620, 8, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:copy', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011652, '流程定义激活/挂起', 1761400000000011620, 9, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:active', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

INSERT INTO sys_menu VALUES (1761400000000011801, '流程表达式', 1761400000000011616, 2, 'spel', 'workflow/spel/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:spel:list', 'input', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, 1761100000000000001, SYSDATE, '流程达式定义菜单');
INSERT INTO sys_menu VALUES (1761400000000011802, '流程spel表达式定义查询', 1761400000000011801, 1, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011803, '流程spel表达式定义新增', 1761400000000011801, 2, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011804, '流程spel表达式定义修改', 1761400000000011801, 3, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011805, '流程spel表达式定义删除', 1761400000000011801, 4, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011806, '流程spel表达式定义导出', 1761400000000011801, 5, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

-- 定时任务（事件等待记录）菜单与按钮
INSERT INTO sys_menu VALUES (1761400000000011901, '定时任务', 1761400000000011630, 3, 'timer', 'workflow/timer/index', '', 'N', 'N', 'C', '0', '0', 'workflow:eventWait:list', 'time', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '定时任务与事件等待监控菜单');
INSERT INTO sys_menu VALUES (1761400000000011902, '定时任务查询', 1761400000000011901, 1, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:eventWait:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011903, '定时任务手动补发', 1761400000000011901, 2, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:eventWait:trigger', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

INSERT INTO sys_menu VALUES (1761400000000011638, '请假申请', 1761400000000000005, 1, 'leave', 'workflow/leave/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:leave:list', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu VALUES (1761400000000011639, '请假申请查询', 1761400000000011638, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011640, '请假申请新增', 1761400000000011638, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011641, '请假申请修改', 1761400000000011638, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011642, '请假申请删除', 1761400000000011638, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011643, '请假申请导出', 1761400000000011638, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (1761500000000000013, '业务状态', 'wf_business_status', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (1761500000000000014, '表单类型', 'wf_form_type', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (1761500000000000015, '任务状态', 'wf_task_status', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (1761600000000000039, 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '已撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000040, 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (1761600000000000041, 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (1761600000000000042, 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '已完成');
INSERT INTO sys_dict_data VALUES (1761600000000000043, 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '已作废');
INSERT INTO sys_dict_data VALUES (1761600000000000044, 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '已退回');
INSERT INTO sys_dict_data VALUES (1761600000000000045, 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '已终止');
INSERT INTO sys_dict_data VALUES (1761600000000000063, 8, '重启', 'restart', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '重启');
INSERT INTO sys_dict_data VALUES (1761600000000000046, 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data VALUES (1761600000000000047, 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '动态表单');
INSERT INTO sys_dict_data VALUES (1761600000000000060, 1, '待审批', 'pending', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '待审批');
INSERT INTO sys_dict_data VALUES (1761600000000000049, 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (1761600000000000061, 3, '自动审批', 'auto_pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '自动审批');
INSERT INTO sys_dict_data VALUES (1761600000000000056, 4, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (1761600000000000052, 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (1761600000000000062, 6, '拿回', 'take_back', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '拿回');
INSERT INTO sys_dict_data VALUES (1761600000000000051, 7, '失效', 'invalid', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '失效');
INSERT INTO sys_dict_data VALUES (1761600000000000048, 8, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000064, 9, '转办', 'transfer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (1761600000000000065, 10, '委派', 'delegate', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '委派');
INSERT INTO sys_dict_data VALUES (1761600000000000066, 11, '加签', 'add_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (1761600000000000067, 12, '减签', 'remove_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, SYSDATE, NULL, NULL, '减签');
