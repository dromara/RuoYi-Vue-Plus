ALTER TABLE flow_task ADD flow_status nvarchar(20) NULL;

EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_task',
'COLUMN', N'flow_status'
GO

IF ((SELECT COUNT(*) FROM ::fn_listextendedproperty('MS_Description',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'flow_status')) > 0)
  EXEC sp_updateextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'flow_status'
ELSE
  EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_instance',
'COLUMN', N'flow_status'
GO

IF ((SELECT COUNT(*) FROM ::fn_listextendedproperty('MS_Description',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'flow_status')) > 0)
  EXEC sp_updateextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'flow_status'
ELSE
  EXEC sp_addextendedproperty
'MS_Description', N'流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'flow_status'
GO

-- sys_social 表修改列
ALTER TABLE sys_social ALTER COLUMN access_token VARCHAR(2000) NOT NULL
GO
ALTER TABLE sys_social ALTER COLUMN refresh_token VARCHAR(2000) NULL
GO

INSERT sys_menu VALUES (116, N'修改生成配置',  3,   2, N'gen-edit/index/:tableId', N'tool/gen/editTable', N'', 1, 1, N'C', N'1', N'0', N'tool:gen:edit',           N'#',               103, 1, getdate(), null, null, N'');
GO
INSERT sys_menu VALUES (130, N'分配用户',     1,   2, N'role-auth/user/:roleId', N'system/role/authUser', N'', 1, 1, N'C', N'1', N'0', N'system:role:edit',      N'#',               103, 1, getdate(), null, null, N'');
GO
INSERT sys_menu VALUES (131, N'分配角色',     1,   1, N'user-auth/role/:userId', N'system/user/authRole', N'', 1, 1, N'C', N'1', N'0', N'system:user:edit',      N'#',               103, 1, getdate(), null, null, N'');
GO
INSERT sys_menu VALUES (132, N'字典数据',     1,   6, N'dict-data/index/:dictId', N'system/dict/data', N'', 1, 1, N'C', N'1', N'0', N'system:dict:list',         N'#',               103, 1, getdate(), null, null, N'');
GO
INSERT sys_menu VALUES (133, N'文件配置管理',  1,   10, N'oss-config/index',              N'system/oss/config', N'', 1, 1, N'C', N'1', N'0', N'system:ossConfig:list',  N'#',                103, 1, getdate(), null, null, N'');
GO
INSERT sys_menu VALUES (11700, N'流程设计', 11616, 5, N'design/index',   N'workflow/processDefinition/design', N'', 1, 1, N'C', N'1', N'0', N'workflow:leave:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11701, N'请假申请', 11616, 6, N'leaveEdit/index', N'workflow/leave/leaveEdit', N'', 1, 1, N'C', N'1', N'0', N'workflow:leave:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
