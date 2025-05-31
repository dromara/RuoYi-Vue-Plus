ALTER TABLE flow_node DROP COLUMN skip_any_node;
ALTER TABLE flow_node ADD ext nvarchar(500) NULL;

EXEC sp_addextendedproperty
'MS_Description', N'扩展属性',
'SCHEMA', N'dbo',
'TABLE', N'flow_node',
'COLUMN', N'ext'
GO

CREATE NONCLUSTERED INDEX user_associated_idx ON flow_user (associated ASC)
GO

ALTER TABLE sys_oss ADD ext1 nvarchar(500) NULL;

EXEC sp_addextendedproperty
'MS_Description', N'扩展属性',
'SCHEMA', N'dbo',
'TABLE', N'sys_oss',
'COLUMN', N'ext1'
GO
