ALTER TABLE flow_node ALTER COLUMN node_ratio nvarchar(200) NULL;
GO
IF ((SELECT COUNT(*) FROM ::fn_listextendedproperty('MS_Description',
'SCHEMA', N'dbo',
'TABLE', N'flow_his_task',
'COLUMN', N'collaborator')) > 0)
  EXEC sp_updateextendedproperty
'MS_Description', N'流程签署比例值',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_ratio'
ELSE
  EXEC sp_addextendedproperty
'MS_Description', N'流程签署比例值',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'node_ratio'
GO
ALTER TABLE flow_node DROP COLUMN handler_type;
GO
ALTER TABLE flow_node DROP COLUMN handler_path;
GO
