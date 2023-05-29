ALTER TABLE gen_table ADD data_name nvarchar(200) DEFAULT '' NULL
GO

EXEC sp_addextendedproperty
    'MS_Description', N'数据源名称',
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'data_name'
GO
