ALTER TABLE sys_dept ADD dept_category VARCHAR(100) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description', '部门类别编码', 'SCHEMA', 'dbo', 'TABLE', 'sys_dept', 'COLUMN', 'dept_category';
GO
ALTER TABLE sys_post ADD dept_id BIGINT NOT NULL;
GO
ALTER TABLE sys_post ADD post_category VARCHAR(100) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description', '部门id', 'SCHEMA', 'dbo', 'TABLE', 'sys_post', 'COLUMN', 'dept_id';
EXEC sp_addextendedproperty 'MS_Description', '岗位类别编码', 'SCHEMA', 'dbo', 'TABLE', 'sys_post', 'COLUMN', 'post_category';
GO
UPDATE sys_post SET dept_id = 100;
GO
