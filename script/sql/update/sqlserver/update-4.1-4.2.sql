ALTER TABLE [sys_oss_config] ADD [domain] nvarchar(255) DEFAULT '' NULL
GO

EXEC sp_addextendedproperty
'MS_Description', N'自定义域名',
'SCHEMA', N'dbo',
'TABLE', N'sys_oss_config',
'COLUMN', N'domain'
GO

UPDATE [sys_oss_config] SET [access_key] = N'ruoyi', [secret_key] = N'ruoyi123', [endpoint] = N'127.0.0.1:9000' WHERE [oss_config_id] = 1
GO

UPDATE [sys_oss_config] SET [endpoint] = N's3-cn-north-1.qiniucs.com' WHERE [oss_config_id] = 2
GO

UPDATE [sys_oss_config] SET [endpoint] = N'oss-cn-beijing.aliyuncs.com' WHERE [oss_config_id] = 3
GO

UPDATE [sys_oss_config] SET [endpoint] = N'cos.ap-beijing.myqcloud.com' WHERE [oss_config_id] = 4
GO

INSERT INTO [sys_oss_config] ([oss_config_id], [config_key], [access_key], [secret_key], [bucket_name], [prefix], [endpoint], [domain], [is_https], [region], [status], [ext1], [create_by], [create_time], [update_by], [update_time], [remark]) VALUES (N'5', N'image',  N'ruoyi', N'ruoyi123', N'ruoyi', N'image', N'127.0.0.1:9000', N'',N'N', N'', N'1', N'', N'admin', getdate(), N'admin', getdate(), NULL)
GO

ALTER TABLE [gen_table_column] ALTER COLUMN [table_id] bigint NULL
GO
