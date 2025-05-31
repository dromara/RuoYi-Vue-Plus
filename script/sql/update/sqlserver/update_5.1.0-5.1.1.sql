ALTER TABLE sys_logininfor ADD client_key nvarchar(32) DEFAULT '' NULL
GO

EXEC sp_addextendedproperty
    'MS_Description', N'客户端',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'client_key'
GO

ALTER TABLE sys_logininfor ADD device_type nvarchar(32) DEFAULT '' NULL
GO

EXEC sp_addextendedproperty
    'MS_Description', N'设备类型',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'device_type'
GO
