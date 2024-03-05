insert into sys_menu values('11616', '工作流'  , '0',    '6', 'workflow',          '',                                 '', '1', '0', 'M', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11617', '模型管理', '11616', '2', 'model',             'workflow/model/index',             '', '1', '1', 'C', '0', '0', 'workflow:model:list',    'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11618', '我的任务', '0', '7', 'task',              '',                                 '', '1', '0', 'M', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11619', '我的待办', '11618', '2', 'taskWaiting',       'workflow/task/taskWaiting',              '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11632', '我的已办', '11618', '3', 'taskFinish',       'workflow/task/taskFinish',              '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11633', '我的抄送', '11618', '4', 'taskCopyList',       'workflow/task/taskCopyList',              '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11621', '流程实例', '11630', '1', 'processInstance',   'workflow/processInstance/index',   '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11622', '流程分类', '11616', '1', 'category',          'workflow/category/index',          '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11629', '我发起的', '11618', '1', 'myDocument',        'workflow/task/myDocument',         '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11630', '流程监控', '11616', '4', 'monitor',           '',                                 '', '1', '0', 'M', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11631', '待办任务', '11630', '2', 'allTaskWaiting',    'workflow/task/allTaskWaiting',     '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');


-- 流程分类管理相关按钮
insert into sys_menu values ('11623', '流程分类查询', '11622', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11624', '流程分类新增', '11622', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add',   '#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11625', '流程分类修改', '11622', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit',  '#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11626', '流程分类删除', '11622', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove','#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11627', '流程分类导出', '11622', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export','#', 103, 1, getdate(), null, null, '');
-- 请假单信息
DROP TABLE if EXISTS test_leave;
create table test_leave
(
    id          bigint        not null
        primary key,
    leave_type  nvarchar(255) not null,
    start_date  datetime2     not null,
    end_date    datetime2     not null,
    leave_days  int           not null,
    remark      nvarchar(255),
    create_dept bigint,
    create_by   bigint,
    create_time datetime2,
    update_by   bigint,
    update_time datetime2,
    tenant_id   nvarchar(20) default '000000'
)
go

exec sp_addextendedproperty 'MS_Description', N'请假申请表', 'SCHEMA', 'dbo', 'TABLE', 'test_leave'
go

exec sp_addextendedproperty 'MS_Description', N'主键', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'id'
go

exec sp_addextendedproperty 'MS_Description', N'请假类型', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN',
     'leave_type'
go

exec sp_addextendedproperty 'MS_Description', N'开始时间', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN',
     'start_date'
go

exec sp_addextendedproperty 'MS_Description', N'结束时间', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'end_date'
go

exec sp_addextendedproperty 'MS_Description', N'请假天数', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN',
     'leave_days'
go

exec sp_addextendedproperty 'MS_Description', N'请假原因', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'remark'
go

exec sp_addextendedproperty 'MS_Description', N'创建部门', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN',
     'create_dept'
go

exec sp_addextendedproperty 'MS_Description', N'创建者', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'create_by'
go

exec sp_addextendedproperty 'MS_Description', N'创建时间', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN',
     'create_time'
go

exec sp_addextendedproperty 'MS_Description', N'更新者', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'update_by'
go

exec sp_addextendedproperty 'MS_Description', N'更新时间', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN',
     'update_time'
go

exec sp_addextendedproperty 'MS_Description', N'租户编号', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'tenant_id'
go

-- 流程分类信息表
DROP TABLE if EXISTS wf_category;
create table wf_category
(
    id            bigint not null
        primary key,
    category_name nvarchar(255),
    category_code nvarchar(255)
        constraint uni_category_code
        unique,
    parent_id     bigint,
    sort_num      int,
    tenant_id     nvarchar(20) default '000000',
    create_dept   bigint,
    create_by     bigint,
    create_time   datetime2,
    update_by     bigint,
    update_time   datetime2
)
go

exec sp_addextendedproperty 'MS_Description', N'流程分类', 'SCHEMA', 'dbo', 'TABLE', 'wf_category'
go

exec sp_addextendedproperty 'MS_Description', N'主键', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN', 'id'
go

exec sp_addextendedproperty 'MS_Description', N'分类名称', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN',
     'category_name'
go

exec sp_addextendedproperty 'MS_Description', N'分类编码', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN',
     'category_code'
go

exec sp_addextendedproperty 'MS_Description', N'父级id', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN', 'parent_id'
go

exec sp_addextendedproperty 'MS_Description', N'排序', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN', 'sort_num'
go

exec sp_addextendedproperty 'MS_Description', N'租户编号', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN',
     'tenant_id'
go

exec sp_addextendedproperty 'MS_Description', N'创建部门', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN',
     'create_dept'
go

exec sp_addextendedproperty 'MS_Description', N'创建者', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN', 'create_by'
go

exec sp_addextendedproperty 'MS_Description', N'创建时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN',
     'create_time'
go

exec sp_addextendedproperty 'MS_Description', N'更新者', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN', 'update_by'
go

exec sp_addextendedproperty 'MS_Description', N'更新时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_category', 'COLUMN',
     'update_time'
go


INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11638, '请假申请', 5, 1, 'leave', 'workflow/leave/index', 1, 0, 'C', '0', '0', 'demo:leave:list', '#', 103, 1, getdate(), NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11639, '请假申请查询', 11638, 1, '#', '', 1, 0, 'F', '0', '0', 'demo:leave:query', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11640, '请假申请新增', 11638, 2, '#', '', 1, 0, 'F', '0', '0', 'demo:leave:add', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11641, '请假申请修改', 11638, 3, '#', '', 1, 0, 'F', '0', '0', 'demo:leave:edit', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11642, '请假申请删除', 11638, 4, '#', '', 1, 0, 'F', '0', '0', 'demo:leave:remove', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11643, '请假申请导出', 11638, 5, '#', '', 1, 0, 'F', '0', '0', 'demo:leave:export', '#', 103, 1, getdate(), NULL, NULL, '');
