CREATE TABLE flow_definition (
    id bigint NOT NULL,
    flow_code nvarchar(40) NOT NULL,
    flow_name nvarchar(100) NOT NULL,
    category nvarchar(100) NULL,
    version nvarchar(20) NOT NULL,
    is_publish tinyint DEFAULT('0') NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    activity_status tinyint DEFAULT('1') NULL,
    listener_type nvarchar(100) NULL,
    listener_path nvarchar(400) NULL,
    ext nvarchar(500) NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_def__3213E83FEE39AE33 PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'flow_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'flow_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程类别',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'category'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程版本',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
'MS_Description', N'是否发布（0未发布 1已发布 9失效）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'is_publish'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程激活状态（0挂起 1激活）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'activity_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'listener_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'listener_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务详情 存业务表对象json字符串',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义表',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition'
GO

CREATE TABLE flow_node (
    id bigint NOT NULL,
    node_type tinyint NOT NULL,
    definition_id bigint NOT NULL,
    node_code nvarchar(100) NOT NULL,
    node_name nvarchar(100) NULL,
    permission_flag nvarchar(200) NULL,
    node_ratio decimal(6,3)  NULL,
    coordinate nvarchar(100) NULL,
    any_node_skip nvarchar(100) NULL,
    listener_type nvarchar(100) NULL,
    listener_path nvarchar(400) NULL,
    handler_type nvarchar(100) NULL,
    handler_path nvarchar(400) NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    version nvarchar(20) NOT NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    ext nvarchar(500) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_nod__3213E83F372470DE PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义id',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'权限标识（权限类型:权限标识，可以多个，用逗号隔开)',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'permission_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程签署比例值',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_ratio'
GO

EXEC sp_addextendedproperty
'MS_Description', N'坐标',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'coordinate'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任意结点跳转',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'any_node_skip'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'listener_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'监听器路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'listener_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'处理器类型',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'handler_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'处理器路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'handler_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'版本',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'version'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'扩展属性',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点表',
'SCHEMA', N'dbo',
'TABLE', N'flow_node'
GO

CREATE TABLE flow_skip (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    now_node_code nvarchar(100) NOT NULL,
    now_node_type tinyint  NULL,
    next_node_code nvarchar(100) NOT NULL,
    next_node_type tinyint  NULL,
    skip_name nvarchar(100) NULL,
    skip_type nvarchar(40) NULL,
    skip_condition nvarchar(200) NULL,
    coordinate nvarchar(100) NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_ski__3213E83F073FEE6E PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义id',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'当前流程节点的编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'now_node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'now_node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'下一个流程节点的编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'next_node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'next_node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'跳转名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'skip_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'跳转类型（PASS审批通过 REJECT退回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'skip_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'跳转条件',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'skip_condition'
GO

EXEC sp_addextendedproperty
'MS_Description', N'坐标',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'coordinate'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点跳转关联表',
'SCHEMA', N'dbo',
'TABLE', N'flow_skip'
GO

CREATE TABLE flow_instance (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    business_id nvarchar(40) NOT NULL,
    node_type tinyint NOT NULL,
    node_code nvarchar(40) NOT NULL,
    node_name nvarchar(100) NULL,
    variable nvarchar(max) NULL,
    flow_status nvarchar(20) NOT NULL,
    activity_status tinyint DEFAULT('1') NULL,
    def_json nvarchar(max) NULL,
    create_by nvarchar(64) NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    ext nvarchar(500) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_ins__3213E83F5190FEE1 PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
TEXTIMAGE_ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_definition表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'business_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务变量',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'variable'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2 审批通过 3自动通过 4终止 5作废 6撤销 7取回  8已完成 9已退回 10失效）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'flow_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程激活状态（0挂起 1激活）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'activity_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程定义json',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'def_json'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建者',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'扩展字段，预留给业务系统使用',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程实例表',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance'
GO

CREATE TABLE flow_task (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    node_code nvarchar(100) NOT NULL,
    node_name nvarchar(100) NULL,
    node_type tinyint NOT NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_tas__3213E83F5AE1F1BA PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_definition表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_instance表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'instance_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'待办任务表',
'SCHEMA', N'dbo',
'TABLE', N'flow_task'
GO

CREATE TABLE flow_his_task (
    id bigint NOT NULL,
    definition_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    task_id bigint NOT NULL,
    node_code nvarchar(200) NULL,
    node_name nvarchar(200) NULL,
    node_type tinyint  NULL,
    target_node_code nvarchar(100) NULL,
    target_node_name nvarchar(100) NULL,
    approver nvarchar(40) NULL,
    cooperate_type tinyint DEFAULT('0') NULL,
    collaborator nvarchar(40) NULL,
    skip_type nvarchar(10) NOT NULL,
    flow_status nvarchar(20) NOT NULL,
    form_custom nchar(1) DEFAULT('N') NULL,
    form_path nvarchar(100) NULL,
    message nvarchar(500) NULL,
    variable nvarchar(max) NULL,
    ext nvarchar(500) NULL,
    create_time datetime2(7)  NULL,
    update_time datetime2(7)  NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_his__3213E83F67951564 PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_definition表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'definition_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_instance表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'instance_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'对应flow_task表的id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'task_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'node_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'目标节点编码',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'target_node_code'
GO

EXEC sp_addextendedproperty
'MS_Description', N'结束节点名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'target_node_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批者',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'approver'
GO

EXEC sp_addextendedproperty
'MS_Description', N'协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'cooperate_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'协作人',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'collaborator'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流转类型（PASS通过 REJECT退回 NONE无动作）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'skip_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程状态（1审批中 2 审批通过 9已退回 10失效）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'flow_status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单是否自定义（Y是 N否）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'form_custom'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批表单路径',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'form_path'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批意见',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'message'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务变量',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'variable'
GO

EXEC sp_addextendedproperty
'MS_Description', N'业务详情 存业务表对象json字符串',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'ext'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务开始时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'审批完成时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'历史任务记录表',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task'
GO

CREATE TABLE flow_user (
    id bigint NOT NULL,
    type nchar(1) NOT NULL,
    processed_by nvarchar(80) NULL,
    associated bigint NOT NULL,
    create_time datetime2(7)  NULL,
    create_by nvarchar(80) NULL,
    update_time datetime2(7)  NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    tenant_id nvarchar(40) NULL,
    CONSTRAINT PK__flow_use__3213E83FFA38CA8B PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX user_processed_type ON flow_user (processed_by ASC, type ASC)
GO
CREATE NONCLUSTERED INDEX user_associated_idx ON flow_user (associated ASC)
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键id',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'权限人',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'processed_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'任务表id',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'associated'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建人',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户id',
'SCHEMA', N'dbo',
'TABLE', N'flow_user',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程用户表',
'SCHEMA', N'dbo',
'TABLE', N'flow_user'
GO

CREATE TABLE flow_category (
    category_id bigint NOT NULL,
    tenant_id nvarchar(20) DEFAULT('000000') NULL,
    parent_id bigint  DEFAULT(0) NULL,
    ancestors nvarchar(500) DEFAULT('') NULL,
    category_name nvarchar(30) NOT NULL,
    order_num int  DEFAULT(0) NULL,
    del_flag nchar(1) DEFAULT('0') NULL,
    create_dept bigint  NULL,
    create_by bigint  NULL,
    create_time datetime2(7)  NULL,
    update_by bigint  NULL,
    update_time datetime2(7)  NULL,
    CONSTRAINT PK__flow_cat__D54EE9B4AE98B9C1 PRIMARY KEY CLUSTERED (category_id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程分类ID',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'category_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户编号',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'父流程分类id',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'parent_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'祖级列表',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'ancestors'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程分类名称',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'category_name'
GO

EXEC sp_addextendedproperty
'MS_Description', N'显示顺序',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'order_num'
GO

EXEC sp_addextendedproperty
'MS_Description', N'删除标志（0代表存在 1代表删除）',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建部门',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'create_dept'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建者',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新者',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'flow_category',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'流程分类',
'SCHEMA', N'dbo',
'TABLE', N'flow_category'
GO

INSERT flow_category VALUES (100, N'000000', 0, N'0', N'OA审批', 0, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (101, N'000000', 100, N'0,100', N'假勤管理', 0, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (102, N'000000', 100, N'0,100', N'人事管理', 1, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (103, N'000000', 101, N'0,100,101', N'请假', 0, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (104, N'000000', 101, N'0,100,101', N'出差', 1, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (105, N'000000', 101, N'0,100,101', N'加班', 2, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (106, N'000000', 101, N'0,100,101', N'换班', 3, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (107, N'000000', 101, N'0,100,101', N'外出', 4, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (108, N'000000', 102, N'0,100,102', N'转正', 1, N'0', 103, 1, getdate(), NULL, NULL);
GO
INSERT flow_category VALUES (109, N'000000', 102, N'0,100,102', N'离职', 2, N'0', 103, 1, getdate(), NULL, NULL);
GO

CREATE TABLE test_leave (
    id bigint NOT NULL,
    tenant_id nvarchar(20) DEFAULT('000000') NULL,
    leave_type nvarchar(255) NOT NULL,
    start_date datetime2(7) NOT NULL,
    end_date datetime2(7) NOT NULL,
    leave_days int NOT NULL,
    remark nvarchar(255) NULL,
    status nvarchar(255) NULL,
    create_dept bigint  NULL,
    create_by bigint  NULL,
    create_time datetime2(7)  NULL,
    update_by bigint  NULL,
    update_time datetime2(7)  NULL,
    CONSTRAINT PK__test_lea__3213E83F348788FA PRIMARY KEY CLUSTERED (id)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'id',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'租户编号',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
'MS_Description', N'请假类型',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'leave_type'
GO

EXEC sp_addextendedproperty
'MS_Description', N'开始时间',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'start_date'
GO

EXEC sp_addextendedproperty
'MS_Description', N'结束时间',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'end_date'
GO

EXEC sp_addextendedproperty
'MS_Description', N'请假天数',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'leave_days'
GO

EXEC sp_addextendedproperty
'MS_Description', N'请假原因',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'remark'
GO

EXEC sp_addextendedproperty
'MS_Description', N'状态',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'status'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建部门',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'create_dept'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建者',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新者',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
'MS_Description', N'请假申请表',
'SCHEMA', N'dbo',
'TABLE', N'test_leave'
GO

INSERT sys_menu VALUES (11616, N'工作流', 0, 6, N'workflow', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'workflow', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11618, N'我的任务', 0, 7, N'task', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'my-task', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11619, N'我的待办', 11618, 2, N'taskWaiting', N'workflow/task/taskWaiting', N'', 1, 1, N'C', N'0', N'0', N'', N'waiting', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11632, N'我的已办', 11618, 3, N'taskFinish', N'workflow/task/taskFinish', N'', 1, 1, N'C', N'0', N'0', N'', N'finish', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11633, N'我的抄送', 11618, 4, N'taskCopyList', N'workflow/task/taskCopyList', N'', 1, 1, N'C', N'0', N'0', N'', N'my-copy', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11620, N'流程定义', 11616, 3, N'processDefinition', N'workflow/processDefinition/index', N'', 1, 1, N'C', N'0', N'0', N'', N'process-definition', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11621, N'流程实例', 11630, 1, N'processInstance', N'workflow/processInstance/index', N'', 1, 1, N'C', N'0', N'0', N'', N'tree-table', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11622, N'流程分类', 11616, 1, N'category', N'workflow/category/index', N'', 1, 0, N'C', N'0', N'0', N'workflow:category:list', N'category', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11629, N'我发起的', 11618, 1, N'myDocument', N'workflow/task/myDocument', N'', 1, 1, N'C', N'0', N'0', N'', N'guide', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11630, N'流程监控', 11616, 4, N'monitor', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'monitor', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11631, N'待办任务', 11630, 2, N'allTaskWaiting', N'workflow/task/allTaskWaiting', N'', 1, 1, N'C', N'0', N'0', N'', N'waiting', 103, 1, GETDATE(), NULL, NULL, N'');
GO

-- 流程分类管理相关按钮
INSERT sys_menu VALUES (11623, N'流程分类查询', 11622, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:category:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11624, N'流程分类新增', 11622, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:category:add', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11625, N'流程分类修改', 11622, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:category:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11626, N'流程分类删除', 11622, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:category:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11627, N'流程分类导出', 11622, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:category:export', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO

-- 请假测试相关按钮
INSERT sys_menu VALUES (11638, N'请假申请', 5, 1, N'leave', N'workflow/leave/index', N'', 1, 0, N'C', N'0', N'0', N'workflow:leave:list', N'#', 103, 1, GETDATE(), NULL, NULL, N'请假申请菜单');
GO
INSERT sys_menu VALUES (11639, N'请假申请查询', 11638, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:leave:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11640, N'请假申请新增', 11638, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:leave:add', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11641, N'请假申请修改', 11638, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:leave:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11642, N'请假申请删除', 11638, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:leave:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11643, N'请假申请导出', 11638, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'workflow:leave:export', N'#', 103, 1, GETDATE(), NULL, NULL, N'');

INSERT sys_dict_type VALUES (13, N'000000', N'业务状态', N'wf_business_status', 103, 1, GETDATE(), NULL, NULL, N'业务状态列表');
GO
INSERT sys_dict_type VALUES (14, N'000000', N'表单类型', N'wf_form_type', 103, 1, GETDATE(), NULL, NULL, N'表单类型列表');
GO
INSERT sys_dict_type VALUES (15, N'000000', N'任务状态', N'wf_task_status', 103, 1, GETDATE(), NULL, NULL, N'任务状态');
GO

INSERT sys_dict_data VALUES (39, N'000000', 1, N'已撤销', N'cancel', N'wf_business_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'已撤销');
GO
INSERT sys_dict_data VALUES (40, N'000000', 2, N'草稿', N'draft', N'wf_business_status', N'', N'info', N'N', 103, 1, GETDATE(), NULL, NULL, N'草稿');
GO
INSERT sys_dict_data VALUES (41, N'000000', 3, N'待审核', N'waiting', N'wf_business_status', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'待审核');
GO
INSERT sys_dict_data VALUES (42, N'000000', 4, N'已完成', N'finish', N'wf_business_status', N'', N'success', N'N', 103, 1, GETDATE(), NULL, NULL, N'已完成');
GO
INSERT sys_dict_data VALUES (43, N'000000', 5, N'已作废', N'invalid', N'wf_business_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'已作废');
GO
INSERT sys_dict_data VALUES (44, N'000000', 6, N'已退回', N'back', N'wf_business_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'已退回');
GO
INSERT sys_dict_data VALUES (45, N'000000', 7, N'已终止', N'termination', N'wf_business_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'已终止');
GO
INSERT sys_dict_data VALUES (46, N'000000', 1, N'自定义表单', N'static', N'wf_form_type', N'', N'success', N'N', 103, 1, GETDATE(), NULL, NULL, N'自定义表单');
GO
INSERT sys_dict_data VALUES (47, N'000000', 2, N'动态表单', N'dynamic', N'wf_form_type', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'动态表单');
GO
INSERT sys_dict_data VALUES (48, N'000000', 1, N'撤销', N'cancel', N'wf_task_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'撤销');
GO
INSERT sys_dict_data VALUES (49, N'000000', 2, N'通过', N'pass', N'wf_task_status', N'', N'success', N'N', 103, 1, GETDATE(), NULL, NULL, N'通过');
GO
INSERT sys_dict_data VALUES (50, N'000000', 3, N'待审核', N'waiting', N'wf_task_status', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'待审核');
GO
INSERT sys_dict_data VALUES (51, N'000000', 4, N'作废', N'invalid', N'wf_task_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'作废');
GO
INSERT sys_dict_data VALUES (52, N'000000', 5, N'退回', N'back', N'wf_task_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'退回');
GO
INSERT sys_dict_data VALUES (53, N'000000', 6, N'终止', N'termination', N'wf_task_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'终止');
GO
INSERT sys_dict_data VALUES (54, N'000000', 7, N'转办', N'transfer', N'wf_task_status', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'转办');
GO
INSERT sys_dict_data VALUES (55, N'000000', 8, N'委托', N'depute', N'wf_task_status', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'委托');
GO
INSERT sys_dict_data VALUES (56, N'000000', 9, N'抄送', N'copy', N'wf_task_status', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'抄送');
GO
INSERT sys_dict_data VALUES (57, N'000000', 10, N'加签', N'sign', N'wf_task_status', N'', N'primary', N'N', 103, 1, GETDATE(), NULL, NULL, N'加签');
GO
INSERT sys_dict_data VALUES (58, N'000000', 11, N'减签', N'sign_off', N'wf_task_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'减签');
GO
INSERT sys_dict_data VALUES (59, N'000000', 11, N'超时', N'timeout', N'wf_task_status', N'', N'danger', N'N', 103, 1, GETDATE(), NULL, NULL, N'超时');
GO
