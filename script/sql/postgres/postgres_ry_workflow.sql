-- ----------------------------
-- 0、warm-flow-all.sql，地址：https://gitee.com/dromara/warm-flow/blob/master/sql/postgresql/postgresql-warm-flow-all.sql
-- ----------------------------
CREATE TABLE flow_definition
(
    id              int8         NOT NULL,                                 -- 主键id
    flow_code       varchar(40)  NOT NULL,                                 -- 流程编码
    flow_name       varchar(100) NOT NULL,                                 -- 流程名称
    category        varchar(100) NULL,                                     -- 流程类别
    "version"       varchar(20)  NOT NULL,                                 -- 流程版本
    is_publish      int2         NOT NULL DEFAULT 0,                       -- 是否发布（0未发布 1已发布 9失效）
    form_custom     bpchar(1)    NULL     DEFAULT 'N':: character varying, -- 审批表单是否自定义（Y是 N否）
    form_path       varchar(100) NULL,                                     -- 审批表单路径
    activity_status int2         NOT NULL DEFAULT 1,                       -- 流程激活状态（0挂起 1激活）
    listener_type   varchar(100) NULL,                                     -- 监听器类型
    listener_path   varchar(400) NULL,                                     -- 监听器路径
    ext             varchar(500) NULL,                                     -- 扩展字段，预留给业务系统使用
    create_time     timestamp    NULL,                                     -- 创建时间
    update_time     timestamp    NULL,                                     -- 更新时间
    del_flag        bpchar(1)    NULL     DEFAULT '0':: character varying, -- 删除标志
    tenant_id       varchar(40)  NULL,                                     -- 租户id
    CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_definition IS '流程定义表';

COMMENT ON COLUMN flow_definition.id IS '主键id';
COMMENT ON COLUMN flow_definition.flow_code IS '流程编码';
COMMENT ON COLUMN flow_definition.flow_name IS '流程名称';
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
COMMENT ON COLUMN flow_definition.update_time IS '更新时间';
COMMENT ON COLUMN flow_definition.del_flag IS '删除标志';
COMMENT ON COLUMN flow_definition.tenant_id IS '租户id';

CREATE TABLE flow_node
(
    id              int8          NOT NULL,                             -- 主键id
    node_type       int2          NOT NULL,                             -- 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
    definition_id   int8          NOT NULL,                             -- 流程定义id
    node_code       varchar(100)  NOT NULL,                             -- 流程节点编码
    node_name       varchar(100)  NULL,                                 -- 流程节点名称
    permission_flag varchar(200)  NULL,                                 -- 权限标识（权限类型:权限标识，可以多个，用逗号隔开)
    node_ratio      numeric(6, 3) NULL,                                 -- 流程签署比例值
    coordinate      varchar(100)  NULL,                                 -- 坐标
    any_node_skip   varchar(100)  NULL,                                 -- 任意结点跳转
    listener_type   varchar(100)  NULL,                                 -- 监听器类型
    listener_path   varchar(400)  NULL,                                 -- 监听器路径
    handler_type    varchar(100)  NULL,                                 -- 处理器类型
    handler_path    varchar(400)  NULL,                                 -- 处理器路径
    form_custom     bpchar(1)     NULL DEFAULT 'N':: character varying, -- 审批表单是否自定义（Y是 N否）
    form_path       varchar(100)  NULL,                                 -- 审批表单路径
    "version"       varchar(20)   NOT NULL,                             -- 版本
    create_time     timestamp     NULL,                                 -- 创建时间
    update_time     timestamp     NULL,                                 -- 更新时间
    ext             varchar(500)  NULL,                                 -- 扩展属性
    del_flag        bpchar(1)     NULL DEFAULT '0':: character varying, -- 删除标志
    tenant_id       varchar(40)   NULL,                                 -- 租户id
    CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_node IS '流程节点表';

COMMENT ON COLUMN flow_node.id IS '主键id';
COMMENT ON COLUMN flow_node.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_node.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_node.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_node.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_node.permission_flag IS '权限标识（权限类型:权限标识，可以多个，用逗号隔开)';
COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';
COMMENT ON COLUMN flow_node.coordinate IS '坐标';
COMMENT ON COLUMN flow_node.any_node_skip IS '任意结点跳转';
COMMENT ON COLUMN flow_node.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_node.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_node.handler_type IS '处理器类型';
COMMENT ON COLUMN flow_node.handler_path IS '处理器路径';
COMMENT ON COLUMN flow_node.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_node.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_node."version" IS '版本';
COMMENT ON COLUMN flow_node.create_time IS '创建时间';
COMMENT ON COLUMN flow_node.update_time IS '更新时间';
COMMENT ON COLUMN flow_node.ext IS '扩展属性';
COMMENT ON COLUMN flow_node.del_flag IS '删除标志';
COMMENT ON COLUMN flow_node.tenant_id IS '租户id';


CREATE TABLE flow_skip
(
    id             int8         NOT NULL,                             -- 主键id
    definition_id  int8         NOT NULL,                             -- 流程定义id
    now_node_code  varchar(100) NOT NULL,                             -- 当前流程节点的编码
    now_node_type  int2         NULL,                                 -- 当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
    next_node_code varchar(100) NOT NULL,                             -- 下一个流程节点的编码
    next_node_type int2         NULL,                                 -- 下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
    skip_name      varchar(100) NULL,                                 -- 跳转名称
    skip_type      varchar(40)  NULL,                                 -- 跳转类型（PASS审批通过 REJECT退回）
    skip_condition varchar(200) NULL,                                 -- 跳转条件
    coordinate     varchar(100) NULL,                                 -- 坐标
    create_time    timestamp    NULL,                                 -- 创建时间
    update_time    timestamp    NULL,                                 -- 更新时间
    del_flag       bpchar(1)    NULL DEFAULT '0':: character varying, -- 删除标志
    tenant_id      varchar(40)  NULL,                                 -- 租户id
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
COMMENT ON COLUMN flow_skip.update_time IS '更新时间';
COMMENT ON COLUMN flow_skip.del_flag IS '删除标志';
COMMENT ON COLUMN flow_skip.tenant_id IS '租户id';

CREATE TABLE flow_instance
(
    id              int8         NOT NULL,                                 -- 主键id
    definition_id   int8         NOT NULL,                                 -- 对应flow_definition表的id
    business_id     varchar(40)  NOT NULL,                                 -- 业务id
    node_type       int2         NOT NULL,                                 -- 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
    node_code       varchar(40)  NOT NULL,                                 -- 流程节点编码
    node_name       varchar(100) NULL,                                     -- 流程节点名称
    variable        text         NULL,                                     -- 任务变量
    flow_status     varchar(20)  NOT NULL,                                 -- 流程状态（0待提交 1审批中 2 审批通过 8已完成 9已退回 10失效）
    activity_status int2         NOT NULL DEFAULT 1,                       -- 流程激活状态（0挂起 1激活）
    def_json        text         NULL,                                     -- 流程定义json
    create_by       varchar(64)  NULL     DEFAULT '':: character varying,  -- 创建者
    create_time     timestamp    NULL,                                     -- 创建时间
    update_time     timestamp    NULL,                                     -- 更新时间
    ext             varchar(500) NULL,                                     -- 扩展字段，预留给业务系统使用
    del_flag        bpchar(1)    NULL     DEFAULT '0':: character varying, -- 删除标志
    tenant_id       varchar(40)  NULL,                                     -- 租户id
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
COMMENT ON COLUMN flow_instance.flow_status IS '流程状态（0待提交 1审批中 2 审批通过 3自动通过 4终止 5作废 6撤销 7取回  8已完成 9已退回 10失效）';
COMMENT ON COLUMN flow_instance.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_instance.def_json IS '流程定义json';
COMMENT ON COLUMN flow_instance.create_by IS '创建者';
COMMENT ON COLUMN flow_instance.create_time IS '创建时间';
COMMENT ON COLUMN flow_instance.update_time IS '更新时间';
COMMENT ON COLUMN flow_instance.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_instance.del_flag IS '删除标志';
COMMENT ON COLUMN flow_instance.tenant_id IS '租户id';

CREATE TABLE flow_task
(
    id            int8         NOT NULL,                             -- 主键id
    definition_id int8         NOT NULL,                             -- 对应flow_definition表的id
    instance_id   int8         NOT NULL,                             -- 对应flow_instance表的id
    node_code     varchar(100) NOT NULL,                             -- 节点编码
    node_name     varchar(100) NULL,                                 -- 节点名称
    node_type     int2         NOT NULL,                             -- 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
    form_custom   bpchar(1)    NULL DEFAULT 'N':: character varying, -- 审批表单是否自定义（Y是 N否）
    form_path     varchar(100) NULL,                                 -- 审批表单路径
    create_time   timestamp    NULL,                                 -- 创建时间
    update_time   timestamp    NULL,                                 -- 更新时间
    del_flag      bpchar(1)    NULL DEFAULT '0':: character varying, -- 删除标志
    tenant_id     varchar(40)  NULL,                                 -- 租户id
    CONSTRAINT flow_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_task IS '待办任务表';

COMMENT ON COLUMN flow_task.id IS '主键id';
COMMENT ON COLUMN flow_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_task.node_code IS '节点编码';
COMMENT ON COLUMN flow_task.node_name IS '节点名称';
COMMENT ON COLUMN flow_task.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_task.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_task.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_task.create_time IS '创建时间';
COMMENT ON COLUMN flow_task.update_time IS '更新时间';
COMMENT ON COLUMN flow_task.del_flag IS '删除标志';
COMMENT ON COLUMN flow_task.tenant_id IS '租户id';

CREATE TABLE flow_his_task
(
    id               int8         NOT NULL,                                 -- 主键id
    definition_id    int8         NOT NULL,                                 -- 对应flow_definition表的id
    instance_id      int8         NOT NULL,                                 -- 对应flow_instance表的id
    task_id          int8         NOT NULL,                                 -- 对应flow_task表的id
    node_code        varchar(200) NULL,                                     -- 开始节点编码
    node_name        varchar(200) NULL,                                     -- 开始节点名称
    node_type        int2         NULL,                                     -- 开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
    target_node_code varchar(200) NULL,                                     -- 目标节点编码
    target_node_name varchar(200) NULL,                                     -- 结束节点名称
    approver         varchar(40)  NULL,                                     -- 审批者
    cooperate_type   int2         NOT NULL DEFAULT 0,                       -- 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
    collaborator     varchar(40)  NULL,                                     -- 协作人(只有转办、会签、票签、委派)
    skip_type        varchar(10)  NULL,                                     -- 流转类型（PASS通过 REJECT退回 NONE无动作）
    flow_status      varchar(20)  NOT NULL,                                 -- 流程状态（0待提交 1审批中 2 审批通过 8已完成 9已退回 10失效）
    form_custom      bpchar(1)    NULL     DEFAULT 'N':: character varying, -- 审批表单是否自定义（Y是 N否）
    form_path        varchar(100) NULL,                                     -- 审批表单路径
    ext              varchar(500) NULL,                                     -- 扩展字段，预留给业务系统使用
    message          varchar(500) NULL,                                     -- 审批意见
    variable         text         NULL,                                     -- 任务变量
    create_time      timestamp    NULL,                                     -- 创建时间
    update_time      timestamp    NULL,                                     -- 更新时间
    del_flag         bpchar(1)    NULL     DEFAULT '0':: character varying, -- 删除标志
    tenant_id        varchar(40)  NULL,                                     -- 租户id
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
COMMENT ON COLUMN flow_his_task.flow_status IS '流程状态（1审批中 2 审批通过 9已退回 10失效）';
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
    id           int8        NOT NULL,                             -- 主键id
    "type"       bpchar(1)   NOT NULL,                             -- 人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3流程实例的抄送人权限 4待办任务的委托人权限）
    processed_by varchar(80) NULL,                                 -- 权限人
    associated   int8        NOT NULL,                             -- 任务表id
    create_time  timestamp   NULL,                                 -- 创建时间
    create_by    varchar(80) NULL,                                 -- 创建人
    update_time  timestamp   NULL,                                 -- 更新时间
    del_flag     bpchar(1)   NULL DEFAULT '0':: character varying, -- 删除标志
    tenant_id    varchar(40) NULL,                                 -- 租户id
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
COMMENT ON COLUMN flow_user.del_flag IS '删除标志';
COMMENT ON COLUMN flow_user.tenant_id IS '租户id';

-- ----------------------------
-- 流程分类表
-- ----------------------------
CREATE TABLE flow_category
(
    category_id   int8         NOT NULL,
    tenant_id     VARCHAR(20)  DEFAULT '000000'::varchar,
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

INSERT INTO flow_category VALUES (100, '000000', 0, '0', 'OA审批', 0, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (101, '000000', 100, '0,100', '假勤管理', 0, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (102, '000000', 100, '0,100', '人事管理', 1, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (103, '000000', 101, '0,100,101', '请假', 0, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (104, '000000', 101, '0,100,101', '出差', 1, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (105, '000000', 101, '0,100,101', '加班', 2, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (106, '000000', 101, '0,100,101', '换班', 3, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (107, '000000', 101, '0,100,101', '外出', 4, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (108, '000000', 102, '0,100,102', '转正', 1, '0', 103, 1, now(), NULL, NULL);
INSERT INTO flow_category VALUES (109, '000000', 102, '0,100,102', '离职', 2, '0', 103, 1, now(), NULL, NULL);

-- ----------------------------
-- 请假单信息
-- ----------------------------
CREATE TABLE test_leave
(
    id          int8         NOT NULL,
    tenant_id   VARCHAR(20)  DEFAULT '000000'::varchar,
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

INSERT INTO sys_menu VALUES ('11616', '工作流', '0', '6', 'workflow', '', '', '1', '0', 'M', '0', '0', '', 'workflow', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11618', '我的任务', '0', '7', 'task', '', '', '1', '0', 'M', '0', '0', '', 'my-task', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11619', '我的待办', '11618', '2', 'taskWaiting', 'workflow/task/taskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11632', '我的已办', '11618', '3', 'taskFinish', 'workflow/task/taskFinish', '', '1', '1', 'C', '0', '0', '', 'finish', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11633', '我的抄送', '11618', '4', 'taskCopyList', 'workflow/task/taskCopyList', '', '1', '1', 'C', '0', '0', '', 'my-copy', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '', 'process-definition', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11621', '流程实例', '11630', '1', 'processInstance', 'workflow/processInstance/index', '', '1', '1', 'C', '0', '0', '', 'tree-table', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11622', '流程分类', '11616', '1', 'category', 'workflow/category/index', '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11629', '我发起的', '11618', '1', 'myDocument', 'workflow/task/myDocument', '', '1', '1', 'C', '0', '0', '', 'guide', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11630', '流程监控', '11616', '4', 'monitor', '', '', '1', '0', 'M', '0', '0', '', 'monitor', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11631', '待办任务', '11630', '2', 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', '1', '1', 'C', '0', '0', '', 'waiting', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11623', '流程分类查询', '11622', '1', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:query', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11624', '流程分类新增', '11622', '2', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:add', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11625', '流程分类修改', '11622', '3', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:edit', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11626', '流程分类删除', '11622', '4', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:remove', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11627', '流程分类导出', '11622', '5', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:category:export', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11638', '请假申请', '5', '1', 'leave', 'workflow/leave/index', '', '1', '0', 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, now(), NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu VALUES ('11639', '请假申请查询', '11638', '1', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11640', '请假申请新增', '11638', '2', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11641', '请假申请修改', '11638', '3', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11642', '请假申请删除', '11638', '4', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11643', '请假申请导出', '11638', '5', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, now(), NULL, NULL, '');

INSERT INTO sys_dict_type VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, now(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, now(), NULL, NULL, '表单类型列表');
INSERT INTO sys_dict_type VALUES (15, '000000', '任务状态', 'wf_task_status', 103, 1, now(), NULL, NULL, '任务状态');
INSERT INTO sys_dict_data VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已撤销');
INSERT INTO sys_dict_data VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, now(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, now(), NULL, NULL, '已完成');
INSERT INTO sys_dict_data VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已作废');
INSERT INTO sys_dict_data VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已退回');
INSERT INTO sys_dict_data VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已终止');
INSERT INTO sys_dict_data VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, now(), NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '动态表单');
INSERT INTO sys_dict_data VALUES (48, '000000', 1, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (49, '000000', 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 103, 1, now(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (50, '000000', 3, '待审核', 'waiting', 'wf_task_status', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data VALUES (51, '000000', 4, '作废', 'invalid', 'wf_task_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '作废');
INSERT INTO sys_dict_data VALUES (52, '000000', 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (53, '000000', 6, '终止', 'termination', 'wf_task_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '终止');
INSERT INTO sys_dict_data VALUES (54, '000000', 7, '转办', 'transfer', 'wf_task_status', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (55, '000000', 8, '委托', 'depute', 'wf_task_status', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '委托');
INSERT INTO sys_dict_data VALUES (56, '000000', 9, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (57, '000000', 10, '加签', 'sign', 'wf_task_status', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (58, '000000', 11, '减签', 'sign_off', 'wf_task_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '减签');
INSERT INTO sys_dict_data VALUES (59, '000000', 11, '超时', 'timeout', 'wf_task_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '超时');

