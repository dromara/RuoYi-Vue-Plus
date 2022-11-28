ALTER TABLE [sys_oss_config] ADD [access_policy] nchar(1) DEFAULT ('1') NOT NULL
GO

EXEC sp_addextendedproperty
'MS_Description', N'桶权限类型(0=private 1=public 2=custom)',
'SCHEMA', N'dbo',
'TABLE', N'sys_oss_config',
'COLUMN', N'access_policy'
GO
