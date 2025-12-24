-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------
CREATE TABLE flow_spel (
    id BIGINT NOT NULL,
    component_name VARCHAR(255),
    method_name VARCHAR(255),
    method_params VARCHAR(255),
    view_spel VARCHAR(255),
    remark VARCHAR(255),
    status CHAR(1) DEFAULT ('0'),
    del_flag CHAR(1) DEFAULT ('0'),
    create_dept BIGINT,
    create_by BIGINT,
    create_time DATETIME,
    update_by BIGINT,
    update_time DATETIME,
    CONSTRAINT PK_flow_spel PRIMARY KEY (id)
);
GO

EXEC sp_addextendedproperty
    'MS_Description', N'流程spel表达式定义表',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'主键id',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'组件名称',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'component_name'
GO

-- method_name 字段注释
    'MS_Description', N'方法名',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'method_name'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'参数',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'method_params'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'预览spel表达式',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'view_spel'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'备注',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'remark'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'状态（0正常 1停用）',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'status'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'删除标志',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'创建部门',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'create_dept'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'创建者',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'创建时间',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'更新者',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'更新时间',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_spel',
    'COLUMN', N'update_time'
GO

INSERT flow_spel VALUES (1, N'spelRuleComponent', N'selectDeptLeaderById', N'initiatorDeptId', N'#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', N'根据部门id获取部门负责人', N'0', N'0', 103, 1, GETDATE(), 1, GETDATE());
GO
INSERT flow_spel VALUES (2, NULL, NULL, N'initiator', N'${initiator}', N'流程发起人', N'0', N'0', 103, 1, GETDATE(), 1, GETDATE());
GO

INSERT sys_menu VALUES (11801, N'流程表达式', N'11616', 2, N'spel', N'workflow/spel/index', N'', 1, 0, N'C', N'0', N'0', N'workflow:spel:list', N'input', 103, 1, GETDATE(), 1, GETDATE(), N'流程达式定义菜单');
GO
INSERT sys_menu VALUES (11802, N'流程spel表达式定义查询', N'11801', 1, N'#', N'', NULL, 1, 0, N'F', N'0', N'0', N'workflow:spel:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11803, N'流程spel表达式定义新增', N'11801', 2, N'#', N'', NULL, 1, 0, N'F', N'0', N'0', N'workflow:spel:add', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11804, N'流程spel表达式定义修改', N'11801', 3, N'#', N'', NULL, 1, 0, N'F', N'0', N'0', N'workflow:spel:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11805, N'流程spel表达式定义删除', N'11801', 4, N'#', N'', NULL, 1, 0, N'F', N'0', N'0', N'workflow:spel:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11806, N'流程spel表达式定义导出', N'11801', 5, N'#', N'', NULL, 1, 0, N'F', N'0', N'0', N'workflow:spel:export', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO

ALTER TABLE flow_definition ADD model_value VARCHAR(40) NOT NULL CONSTRAINT DF_flow_definition_model_value DEFAULT 'CLASSICS';
GO
EXEC sp_addextendedproperty
'MS_Description', N'设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
'SCHEMA', N'dbo',
'TABLE', N'flow_definition',
'COLUMN', N'model_value'
GO
UPDATE flow_skip SET skip_condition = REPLACE(skip_condition, 'notNike', 'notLike');
GO
ALTER TABLE flow_his_task ALTER COLUMN collaborator VARCHAR(500) NULL;
GO
IF ((SELECT COUNT(*) FROM ::fn_listextendedproperty('MS_Description',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'collaborator')) > 0)
  EXEC sp_updateextendedproperty
'MS_Description', N'协作人',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'collaborator'
ELSE
  EXEC sp_addextendedproperty
'MS_Description', N'协作人',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'collaborator'
GO

CREATE TABLE flow_instance_biz_ext (
    id             BIGINT         NOT NULL,
    tenant_id      VARCHAR(20)    DEFAULT ('000000'),
    create_dept    BIGINT,
    create_by      BIGINT,
    create_time    DATETIME,
    update_by      BIGINT,
    update_time    DATETIME,
    business_code  VARCHAR(255),
    business_title VARCHAR(1000),
    del_flag       CHAR(1)        DEFAULT ('0'),
    instance_id    BIGINT,
    business_id    VARCHAR(255),
    CONSTRAINT PK__fi_biz_ext__D54EE9B4AE98B9C1 PRIMARY KEY CLUSTERED (id)
       WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
       ON [PRIMARY]
);

EXEC sp_addextendedproperty
    'MS_Description', N'流程实例业务扩展表',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'主键id',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'租户编号',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'创建部门',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'create_dept'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'创建者',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'创建时间',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'更新者',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'更新时间',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'删除标志',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'业务编码',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'business_code'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'业务标题',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'business_title'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'流程实例Id',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'instance_id'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'业务Id',
    'SCHEMA', N'dbo',
    'TABLE', N'flow_instance_biz_ext',
    'COLUMN', N'business_id'
GO

ALTER TABLE test_leave ADD apply_code nvarchar(50) NOT NULL;
GO
EXEC sp_addextendedproperty
'MS_Description', N'申请编号',
'SCHEMA', N'dbo',
'TABLE', N'test_leave',
'COLUMN', N'apply_code'
GO

update sys_menu set remark = N'/tool/gen' where menu_id = 116;
GO
update sys_menu set remark = N'/system/role' where menu_id = 130;
GO
update sys_menu set remark = N'/system/user' where menu_id = 131;
GO
update sys_menu set remark = N'/system/dict' where menu_id = 132;
GO
update sys_menu set remark = N'/system/oss' where menu_id = 133;
GO
update sys_menu set remark = N'/workflow/processDefinition' where menu_id = 11700;
GO
