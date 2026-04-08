CREATE TABLE flow_definition
(
    id              int8         NOT NULL,
    flow_code       varchar(40)  NOT NULL,
    flow_name       varchar(100) NOT NULL,
    model_value     varchar(40)  NOT NULL DEFAULT 'CLASSICS',
    category        varchar(100) NULL,
    "version"       varchar(20)  NOT NULL,
    is_publish      int2         NOT NULL DEFAULT 0,
    form_custom     bpchar(1)    NULL     DEFAULT 'N':: character varying,
    form_path       varchar(100) NULL,
    activity_status int2         NOT NULL DEFAULT 1,
    listener_type   varchar(100) NULL,
    listener_path   varchar(400) NULL,
    ext             varchar(500) NULL,
    create_time     timestamp    NULL,
    create_by       varchar(64)  NULL     DEFAULT '':: character varying,
    update_time     timestamp    NULL,
    update_by       varchar(64)  NULL     DEFAULT '':: character varying,
    del_flag        bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_definition IS '流程定义表';

COMMENT ON COLUMN flow_definition.id IS '主键id';
COMMENT ON COLUMN flow_definition.flow_code IS '流程编码';
COMMENT ON COLUMN flow_definition.flow_name IS '流程名称';
COMMENT ON COLUMN flow_definition.model_value IS '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）';
COMMENT ON COLUMN flow_definition.category IS '流程类别';
COMMENT ON COLUMN flow_definition."version" IS '流程版本';
COMMENT ON COLUMN flow_definition.is_publish IS '是否发布（0未发布 1已发布 9失效）';
COMMENT ON COLUMN flow_definition.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_definition.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_definition.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_definition.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_definition.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_definition.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_definition.create_time IS '创建时间';
COMMENT ON COLUMN flow_definition.create_by IS '创建人';
COMMENT ON COLUMN flow_definition.update_time IS '更新时间';
COMMENT ON COLUMN flow_definition.update_by IS '更新人';
COMMENT ON COLUMN flow_definition.del_flag IS '删除标志';
COMMENT ON COLUMN flow_definition.tenant_id IS '租户id';

CREATE TABLE flow_node
(
    id              int8          NOT NULL,
    node_type       int2          NOT NULL,
    definition_id   int8          NOT NULL,
    node_code       varchar(100)  NOT NULL,
    node_name       varchar(100)  NULL,
    permission_flag varchar(200)  NULL,
    node_ratio      varchar(200)  NULL,
    coordinate      varchar(100)  NULL,
    any_node_skip   varchar(100)  NULL,
    listener_type   varchar(100)  NULL,
    listener_path   varchar(400)  NULL,
    form_custom     bpchar(1)     NULL DEFAULT 'N':: character varying,
    form_path       varchar(100)  NULL,
    "version"       varchar(20)   NOT NULL,
    create_time     timestamp    NULL,
    create_by       varchar(64)  NULL DEFAULT '':: character varying,
    update_time     timestamp    NULL,
    update_by       varchar(64)  NULL DEFAULT '':: character varying,
    ext             text         NULL,
    del_flag        bpchar(1)     NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)   NULL,
    CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_node IS '流程节点表';

COMMENT ON COLUMN flow_node.id IS '主键id';
COMMENT ON COLUMN flow_node.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_node.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_node.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_node.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_node.permission_flag IS '权限标识（权限类型:权限标识，可以多个，用@@隔开)';
COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';
COMMENT ON COLUMN flow_node.coordinate IS '坐标';
COMMENT ON COLUMN flow_node.any_node_skip IS '任意结点跳转';
COMMENT ON COLUMN flow_node.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_node.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_node.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_node.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_node."version" IS '版本';
COMMENT ON COLUMN flow_node.create_time IS '创建时间';
COMMENT ON COLUMN flow_node.create_by IS '创建人';
COMMENT ON COLUMN flow_node.update_time IS '更新时间';
COMMENT ON COLUMN flow_node.update_by IS '更新人';
COMMENT ON COLUMN flow_node.ext IS '节点扩展属性';
COMMENT ON COLUMN flow_node.del_flag IS '删除标志';
COMMENT ON COLUMN flow_node.tenant_id IS '租户id';


CREATE TABLE flow_skip
(
    id             int8         NOT NULL,
    definition_id  int8         NOT NULL,
    now_node_code  varchar(100) NOT NULL,
    now_node_type  int2         NULL,
    next_node_code varchar(100) NOT NULL,
    next_node_type int2         NULL,
    skip_name      varchar(100) NULL,
    skip_type      varchar(40)  NULL,
    skip_condition varchar(200) NULL,
    coordinate     varchar(100) NULL,
    create_time    timestamp    NULL,
    create_by      varchar(64)  NULL DEFAULT '':: character varying,
    update_time    timestamp    NULL,
    update_by      varchar(64)  NULL DEFAULT '':: character varying,
    del_flag       bpchar(1)    NULL DEFAULT '0':: character varying,
    tenant_id      varchar(40)  NULL,
    CONSTRAINT flow_skip_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_skip IS '节点跳转关联表';

COMMENT ON COLUMN flow_skip.id IS '主键id';
COMMENT ON COLUMN flow_skip.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_skip.now_node_code IS '当前流程节点的编码';
COMMENT ON COLUMN flow_skip.now_node_type IS '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_skip.next_node_code IS '下一个流程节点的编码';
COMMENT ON COLUMN flow_skip.next_node_type IS '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_skip.skip_name IS '跳转名称';
COMMENT ON COLUMN flow_skip.skip_type IS '跳转类型（PASS审批通过 REJECT退回）';
COMMENT ON COLUMN flow_skip.skip_condition IS '跳转条件';
COMMENT ON COLUMN flow_skip.coordinate IS '坐标';
COMMENT ON COLUMN flow_skip.create_time IS '创建时间';
COMMENT ON COLUMN flow_skip.create_by IS '创建人';
COMMENT ON COLUMN flow_skip.update_time IS '更新时间';
COMMENT ON COLUMN flow_skip.update_by IS '更新人';
COMMENT ON COLUMN flow_skip.del_flag IS '删除标志';
COMMENT ON COLUMN flow_skip.tenant_id IS '租户id';

CREATE TABLE flow_instance
(
    id              int8         NOT NULL,
    definition_id   int8         NOT NULL,
    business_id     varchar(40)  NOT NULL,
    node_type       int2         NOT NULL,
    node_code       varchar(40)  NOT NULL,
    node_name       varchar(100) NULL,
    variable        text         NULL,
    flow_status     varchar(20)  NOT NULL,
    activity_status int2         NOT NULL DEFAULT 1,
    def_json        text         NULL,
    create_time     timestamp    NULL,
    create_by       varchar(64)  NULL DEFAULT '':: character varying,
    update_time     timestamp    NULL,
    update_by       varchar(64)  NULL DEFAULT '':: character varying,
    ext             varchar(500) NULL,
    del_flag        bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_instance_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_instance IS '流程实例表';

COMMENT ON COLUMN flow_instance.id IS '主键id';
COMMENT ON COLUMN flow_instance.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_instance.business_id IS '业务id';
COMMENT ON COLUMN flow_instance.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_instance.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_instance.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_instance.variable IS '任务变量';
COMMENT ON COLUMN flow_instance.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_instance.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_instance.def_json IS '流程定义json';
COMMENT ON COLUMN flow_instance.create_time IS '创建时间';
COMMENT ON COLUMN flow_instance.create_by IS '创建人';
COMMENT ON COLUMN flow_instance.update_time IS '更新时间';
COMMENT ON COLUMN flow_instance.update_by IS '更新人';
COMMENT ON COLUMN flow_instance.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_instance.del_flag IS '删除标志';
COMMENT ON COLUMN flow_instance.tenant_id IS '租户id';

CREATE TABLE flow_task
(
    id            int8         NOT NULL,
    definition_id int8         NOT NULL,
    instance_id   int8         NOT NULL,
    node_code     varchar(100) NOT NULL,
    node_name     varchar(100) NULL,
    node_type     int2         NOT NULL,
    flow_status   varchar(20)  NOT NULL,
    form_custom   bpchar(1)    NULL DEFAULT 'N':: character varying,
    form_path     varchar(100) NULL,
    create_time   timestamp    NULL,
    create_by     varchar(64)  NULL DEFAULT '':: character varying,
    update_time   timestamp    NULL,
    update_by     varchar(64)  NULL DEFAULT '':: character varying,
    del_flag      bpchar(1)    NULL DEFAULT '0':: character varying,
    tenant_id     varchar(40)  NULL,
    CONSTRAINT flow_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_task IS '待办任务表';

COMMENT ON COLUMN flow_task.id IS '主键id';
COMMENT ON COLUMN flow_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_task.node_code IS '节点编码';
COMMENT ON COLUMN flow_task.node_name IS '节点名称';
COMMENT ON COLUMN flow_task.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_task.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_task.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_task.create_time IS '创建时间';
COMMENT ON COLUMN flow_task.create_by IS '创建人';
COMMENT ON COLUMN flow_task.update_time IS '更新时间';
COMMENT ON COLUMN flow_task.update_by IS '更新人';
COMMENT ON COLUMN flow_task.del_flag IS '删除标志';
COMMENT ON COLUMN flow_task.tenant_id IS '租户id';

CREATE TABLE flow_his_task
(
    id               int8         NOT NULL,
    definition_id    int8         NOT NULL,
    instance_id      int8         NOT NULL,
    task_id          int8         NOT NULL,
    node_code        varchar(100) NULL,
    node_name        varchar(100) NULL,
    node_type        int2         NULL,
    target_node_code varchar(200) NULL,
    target_node_name varchar(200) NULL,
    approver         varchar(40)  NULL,
    cooperate_type   int2         NOT NULL DEFAULT 0,
    collaborator     varchar(500)  NULL,
    skip_type        varchar(10)  NULL,
    flow_status      varchar(20)  NOT NULL,
    form_custom      bpchar(1)    NULL     DEFAULT 'N':: character varying,
    form_path        varchar(100) NULL,
    ext              text         NULL,
    message          varchar(500) NULL,
    variable         text         NULL,
    create_time      timestamp    NULL,
    update_time      timestamp    NULL,
    del_flag         bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id        varchar(40)  NULL,
    CONSTRAINT flow_his_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_his_task IS '历史任务记录表';

COMMENT ON COLUMN flow_his_task.id IS '主键id';
COMMENT ON COLUMN flow_his_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_his_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_his_task.task_id IS '对应flow_task表的id';
COMMENT ON COLUMN flow_his_task.node_code IS '开始节点编码';
COMMENT ON COLUMN flow_his_task.node_name IS '开始节点名称';
COMMENT ON COLUMN flow_his_task.node_type IS '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_his_task.target_node_code IS '目标节点编码';
COMMENT ON COLUMN flow_his_task.target_node_name IS '结束节点名称';
COMMENT ON COLUMN flow_his_task.approver IS '审批者';
COMMENT ON COLUMN flow_his_task.cooperate_type IS '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)';
COMMENT ON COLUMN flow_his_task.collaborator IS '协作人';
COMMENT ON COLUMN flow_his_task.skip_type IS '流转类型（PASS通过 REJECT退回 NONE无动作）';
COMMENT ON COLUMN flow_his_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_his_task.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_his_task.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_his_task.message IS '审批意见';
COMMENT ON COLUMN flow_his_task.variable IS '任务变量';
COMMENT ON COLUMN flow_his_task.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_his_task.create_time IS '任务开始时间';
COMMENT ON COLUMN flow_his_task.update_time IS '审批完成时间';
COMMENT ON COLUMN flow_his_task.del_flag IS '删除标志';
COMMENT ON COLUMN flow_his_task.tenant_id IS '租户id';

CREATE TABLE flow_user
(
    id           int8        NOT NULL,
    "type"       bpchar(1)   NOT NULL,
    processed_by varchar(80) NULL,
    associated   int8        NOT NULL,
    create_time  timestamp   NULL,
    create_by    varchar(64)  NULL     DEFAULT '':: character varying,
    update_time  timestamp   NULL,
    update_by    varchar(64)  NULL DEFAULT '':: character varying,
    del_flag     bpchar(1)   NULL DEFAULT '0':: character varying,
    tenant_id    varchar(40) NULL,
    CONSTRAINT flow_user_pk PRIMARY KEY (id)
);
CREATE INDEX user_processed_type ON flow_user USING btree (processed_by, type);
CREATE INDEX user_associated_idx ON FLOW_USER USING btree (associated);
COMMENT ON TABLE flow_user IS '流程用户表';

COMMENT ON COLUMN flow_user.id IS '主键id';
COMMENT ON COLUMN flow_user."type" IS '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）';
COMMENT ON COLUMN flow_user.processed_by IS '权限人';
COMMENT ON COLUMN flow_user.associated IS '任务表id';
COMMENT ON COLUMN flow_user.create_time IS '创建时间';
COMMENT ON COLUMN flow_user.create_by IS '创建人';
COMMENT ON COLUMN flow_user.update_time IS '更新时间';
COMMENT ON COLUMN flow_user.update_by IS '更新人';
COMMENT ON COLUMN flow_user.del_flag IS '删除标志';
COMMENT ON COLUMN flow_user.tenant_id IS '租户id';

-- ----------------------------
-- 流程分类表
-- ----------------------------
CREATE TABLE flow_category
(
    category_id   int8         NOT NULL,
    parent_id     int8         DEFAULT 0,
    ancestors     VARCHAR(500) DEFAULT ''::varchar,
    category_name VARCHAR(30)  NOT NULL,
    order_num     INT          DEFAULT 0,
    del_flag      CHAR         DEFAULT '0'::bpchar,
    create_dept   int8,
    create_by     int8,
    create_time   TIMESTAMP,
    update_by     int8,
    update_time   TIMESTAMP,
    PRIMARY KEY (category_id)
);

COMMENT ON TABLE flow_category IS '流程分类';
COMMENT ON COLUMN flow_category.category_id IS '流程分类ID';
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

INSERT INTO flow_category VALUES (1762300000000000100, 0, '0', 'OA审批', 0, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000101, 1762300000000000100, '0,1762300000000000100', '假勤管理', 0, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000102, 1762300000000000100, '0,1762300000000000100', '人事管理', 1, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000103, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '请假', 0, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000104, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '出差', 1, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000105, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '加班', 2, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000106, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '换班', 3, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000107, 1762300000000000101, '0,1762300000000000100,1762300000000000101', '外出', 4, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000108, 1762300000000000102, '0,1762300000000000100,1762300000000000102', '转正', 1, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);
INSERT INTO flow_category VALUES (1762300000000000109, 1762300000000000102, '0,1762300000000000100,1762300000000000102', '离职', 2, '0', 1761000000000000103, 1761100000000000001, now(), NULL, NULL);

-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------
CREATE TABLE flow_spel (
    id int8 NOT NULL,
    component_name VARCHAR(255),
    method_name VARCHAR(255),
    method_params VARCHAR(255),
    view_spel VARCHAR(255),
    remark VARCHAR(255),
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept int8,
    create_by int8,
    create_time TIMESTAMP,
    update_by int8,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);

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

INSERT INTO flow_spel VALUES (1762400000000000001, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 1761000000000000103, 1761100000000000001, now(), 1761100000000000001, now());
INSERT INTO flow_spel VALUES (1762400000000000002, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 1761000000000000103, 1761100000000000001, now(), 1761100000000000001, now());

-- ----------------------------
-- 流程实例业务扩展表
-- ----------------------------
CREATE TABLE flow_instance_biz_ext (
    id             int8,
    create_dept    int8,
    create_by      int8,
    create_time    TIMESTAMP,
    update_by      int8,
    update_time    TIMESTAMP,
    business_code  VARCHAR(255),
    business_title VARCHAR(1000),
    del_flag       CHAR(1)       DEFAULT '0',
    instance_id    int8,
    business_id    VARCHAR(255),
    PRIMARY KEY (id)
);

COMMENT ON TABLE flow_instance_biz_ext IS '流程实例业务扩展表';
COMMENT ON COLUMN flow_instance_biz_ext.id  IS '主键id';
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
    id          int8         NOT NULL,
    apply_code  VARCHAR(50)  NOT NULL,
    leave_type  VARCHAR(255) NOT NULL,
    start_date  TIMESTAMP    NOT NULL,
    end_date    TIMESTAMP    NOT NULL,
    leave_days  int2          NOT NULL,
    remark      VARCHAR(255),
    status      VARCHAR(255),
    create_dept int8,
    create_by   int8,
    create_time TIMESTAMP,
    update_by   int8,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);

COMMENT ON TABLE test_leave IS '请假申请表';
COMMENT ON COLUMN test_leave.id IS 'id';
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

INSERT INTO sys_menu VALUES (1761400000000011616, '工作流', 0, 6, 'workflow', '', '', 'N', 'Y', 'M', '0', '0', '', 'workflow', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011618, '我的任务', 0, 7, 'task', '', '', 'N', 'Y', 'M', '0', '0', '', 'my-task', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011619, '我的待办', 1761400000000011618, 2, 'taskWaiting', 'workflow/task/taskWaiting', '', 'N', 'N', 'C', '0', '0', '', 'waiting', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011632, '我的已办', 1761400000000011618, 3, 'taskFinish', 'workflow/task/taskFinish', '', 'N', 'N', 'C', '0', '0', '', 'finish', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011633, '我的抄送', 1761400000000011618, 4, 'taskCopyList', 'workflow/task/taskCopyList', '', 'N', 'N', 'C', '0', '0', '', 'my-copy', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011620, '流程定义', 1761400000000011616, 3, 'processDefinition', 'workflow/processDefinition/index', '', 'N', 'N', 'C', '0', '0', 'workflow:definition:list', 'process-definition', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011621, '流程实例', 1761400000000011630, 1, 'processInstance', 'workflow/processInstance/index', '', 'N', 'N', 'C', '0', '0', 'workflow:instance:list', 'tree-table', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011622, '流程分类', 1761400000000011616, 1, 'category', 'workflow/category/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:category:list', 'category', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011629, '我发起的', 1761400000000011618, 1, 'myDocument', 'workflow/task/myDocument', '', 'N', 'N', 'C', '0', '0', 'workflow:instance:currentList', 'guide', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011630, '流程监控', 1761400000000011616, 4, 'processMonitor', '', '', 'N', 'Y', 'M', '0', '0', '', 'monitor', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011631, '待办任务', 1761400000000011630, 2, 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', 'N', 'N', 'C', '0', '0', '', 'waiting', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011700, '流程设计', 1761400000000011616, 5, 'design/index', 'workflow/processDefinition/design', '', 'N', 'N', 'C', 'N', 'Y', 'workflow:leave:edit', '#', '/workflow/processDefinition', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011701, '请假申请', 1761400000000011616, 6, 'leaveEdit/index', 'workflow/leave/leaveEdit', '', 'N', 'N', 'C', 'N', 'Y', 'workflow:leave:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');

INSERT INTO sys_menu VALUES (1761400000000011623, '流程分类查询', 1761400000000011622, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011624, '流程分类新增', 1761400000000011622, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011625, '流程分类修改', 1761400000000011622, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011626, '流程分类删除', 1761400000000011622, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011627, '流程分类导出', 1761400000000011622, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:category:export', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');

-- 流程实例管理相关按钮
INSERT INTO sys_menu VALUES (1761400000000011653, '流程实例查询', 1761400000000011621, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011654, '流程变量查询', 1761400000000011621, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variableQuery', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011655, '流程变量修改', 1761400000000011621, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variable', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011656, '流程实例激活/挂起', 1761400000000011621, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:active', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011657, '流程实例删除', 1761400000000011621, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011658, '流程实例作废', 1761400000000011621, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:invalid', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011659, '流程实例撤销', 1761400000000011621, 7, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:cancel', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');

-- 流程定义管理相关按钮
INSERT INTO sys_menu VALUES (1761400000000011644, '流程定义查询', 1761400000000011620, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011645, '流程定义新增', 1761400000000011620, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011646, '流程定义修改', 1761400000000011620, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011647, '流程定义删除', 1761400000000011620, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011648, '流程定义导出', 1761400000000011620, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:export', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011649, '流程定义导入', 1761400000000011620, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:import', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011650, '流程定义发布/取消发布', 1761400000000011620, 7, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:publish', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011651, '流程定义复制', 1761400000000011620, 8, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:copy', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011652, '流程定义激活/挂起', 1761400000000011620, 9, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:active', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');

INSERT INTO sys_menu VALUES (1761400000000011801, '流程表达式', 1761400000000011616, 2, 'spel', 'workflow/spel/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:spel:list', 'input', '', '', 1761000000000000103, 1761100000000000001, now(), 1761100000000000001, now(), '流程达式定义菜单');
INSERT INTO sys_menu VALUES (1761400000000011802, '流程spel表达式定义查询', 1761400000000011801, 1, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011803, '流程spel表达式定义新增', 1761400000000011801, 2, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011804, '流程spel表达式定义修改', 1761400000000011801, 3, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011805, '流程spel表达式定义删除', 1761400000000011801, 4, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011806, '流程spel表达式定义导出', 1761400000000011801, 5, '#', '', NULL, 'N', 'Y', 'F', '0', '0', 'workflow:spel:export', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');

INSERT INTO sys_menu VALUES (1761400000000011638, '请假申请', 1761400000000000005, 1, 'leave', 'workflow/leave/index', '', 'N', 'Y', 'C', '0', '0', 'workflow:leave:list', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu VALUES (1761400000000011639, '请假申请查询', 1761400000000011638, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011640, '请假申请新增', 1761400000000011638, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011641, '请假申请修改', 1761400000000011638, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011642, '请假申请删除', 1761400000000011638, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES (1761400000000011643, '请假申请导出', 1761400000000011638, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:leave:export', '#', '', '', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (1761500000000000013, '业务状态', 'wf_business_status', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (1761500000000000014, '表单类型', 'wf_form_type', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (1761500000000000015, '任务状态', 'wf_task_status', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (1761600000000000039, 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '已撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000040, 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (1761600000000000041, 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (1761600000000000042, 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '已完成');
INSERT INTO sys_dict_data VALUES (1761600000000000043, 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '已作废');
INSERT INTO sys_dict_data VALUES (1761600000000000044, 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '已退回');
INSERT INTO sys_dict_data VALUES (1761600000000000045, 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '已终止');
INSERT INTO sys_dict_data VALUES (1761600000000000046, 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data VALUES (1761600000000000047, 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '动态表单');
INSERT INTO sys_dict_data VALUES (1761600000000000048, 1, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000049, 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (1761600000000000050, 3, '待审核', 'waiting', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (1761600000000000051, 4, '作废', 'invalid', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '作废');
INSERT INTO sys_dict_data VALUES (1761600000000000052, 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (1761600000000000053, 6, '终止', 'termination', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '终止');
INSERT INTO sys_dict_data VALUES (1761600000000000054, 7, '转办', 'transfer', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (1761600000000000055, 8, '委托', 'depute', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '委托');
INSERT INTO sys_dict_data VALUES (1761600000000000056, 9, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (1761600000000000057, 10, '加签', 'sign', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (1761600000000000058, 11, '减签', 'sign_off', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '减签');
INSERT INTO sys_dict_data VALUES (1761600000000000059, 11, '超时', 'timeout', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, now(), NULL, NULL, '超时');

