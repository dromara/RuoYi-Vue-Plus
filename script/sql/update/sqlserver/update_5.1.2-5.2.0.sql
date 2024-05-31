ALTER TABLE sys_dept ADD dept_category nvarchar(100) DEFAULT NULL
EXEC sp_addextendedproperty
    'MS_Description', N'部门类别编码',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'dept_category'
GO
ALTER TABLE sys_post ADD dept_id bigint NOT NULL
GO
ALTER TABLE sys_post ADD post_category nvarchar(100) DEFAULT NULL
GO
EXEC sp_addextendedproperty
    'MS_Description', N'部门id',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'dept_id'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'岗位类别编码',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'post_category'
GO
UPDATE sys_post SET dept_id = 100
GO
UPDATE sys_post SET dept_id = 103 where post_id = 1
GO
UPDATE sys_menu SET menu_name = N'SnailJob控制台', path = N'snailjob', component = N'monitor/snailjob/index', perms = N'monitor:snailjob:list', remark = N'SnailJob控制台菜单' WHERE menu_id = 120
GO
