insert into sys_menu values('11616', '工作流'  , '0',    '6', 'workflow',          '',                                 '', '1', '0', 'M', '0', '0', '',                       'workflow', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11617', '模型管理', '11616', '2', 'model',             'workflow/model/index',             '', '1', '1', 'C', '0', '0', 'workflow:model:list',    'model', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11618', '我的任务', '0', '7', 'task',              '',                                 '', '1', '0', 'M', '0', '0', '',                       'my-task', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11619', '我的待办', '11618', '2', 'taskWaiting',       'workflow/task/taskWaiting',              '', '1', '1', 'C', '0', '0', '',                       'waiting', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11632', '我的已办', '11618', '3', 'taskFinish',       'workflow/task/taskFinish',              '', '1', '1', 'C', '0', '0', '',                       'finish', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11633', '我的抄送', '11618', '4', 'taskCopyList',       'workflow/task/taskCopyList',              '', '1', '1', 'C', '0', '0', '',                       'my-copy', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '',                       'process-definition', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11621', '流程实例', '11630', '1', 'processInstance',   'workflow/processInstance/index',   '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11622', '流程分类', '11616', '1', 'category',          'workflow/category/index',          '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11629', '我发起的', '11618', '1', 'myDocument',        'workflow/task/myDocument',         '', '1', '1', 'C', '0', '0', '',                       'guide', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11630', '流程监控', '11616', '4', 'monitor',           '',                                 '', '1', '0', 'M', '0', '0', '',                       'monitor', 103, 1, getdate(), NULL, NULL, '');
insert into sys_menu values('11631', '待办任务', '11630', '2', 'allTaskWaiting',    'workflow/task/allTaskWaiting',     '', '1', '1', 'C', '0', '0', '',                       'waiting', 103, 1, getdate(), NULL, NULL, '');


-- 流程分类管理相关按钮
insert into sys_menu values ('11623', '流程分类查询', '11622', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11624', '流程分类新增', '11622', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add',   '#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11625', '流程分类修改', '11622', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit',  '#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11626', '流程分类删除', '11622', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove','#', 103, 1, getdate(), null, null, '');
insert into sys_menu values ('11627', '流程分类导出', '11622', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export','#', 103, 1, getdate(), null, null, '');
-- 请假单信息
create table test_leave
(
    id          bigint        not null
        primary key,
    leave_type  nvarchar(255) not null,
    start_date  datetime2     not null,
    end_date    datetime2     not null,
    leave_days  int           not null,
    remark      nvarchar(255),
    status      nvarchar(255),
    create_dept bigint,
    create_by   bigint,
    create_time datetime2,
    update_by   bigint,
    update_time datetime2,
    tenant_id   nvarchar(20)
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

exec sp_addextendedproperty 'MS_Description', N'状态', 'SCHEMA', 'dbo', 'TABLE', 'test_leave', 'COLUMN', 'status'
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
    tenant_id     nvarchar(20),
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

INSERT INTO wf_category values (1, 'OA', 'OA', 0, 0, '000000', 103, 1, getdate(), 1, getdate());

create table wf_task_back_node
(
    id            bigint not null primary key,
    node_id       nvarchar(255) not null,
    node_name     nvarchar(255) not null,
    order_no      int not null,
    instance_id   nvarchar(255) not null,
    task_type     nvarchar(255) not null,
    assignee      nvarchar(2000) not null,
    tenant_id     nvarchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   datetime2,
    update_by     bigint,
    update_time   datetime2
)

go
exec sp_addextendedproperty 'MS_Description', N'节点审批记录', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node'
go

exec sp_addextendedproperty 'MS_Description', N'主键', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'id'
go

exec sp_addextendedproperty 'MS_Description', N'节点id', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN',
     'node_id'
go

exec sp_addextendedproperty 'MS_Description', N'节点名称', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN',
     'node_name'
go

exec sp_addextendedproperty 'MS_Description', N'排序', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'order_no'
go

exec sp_addextendedproperty 'MS_Description', N'流程实例id', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'instance_id'
go

exec sp_addextendedproperty 'MS_Description', N'节点类型', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'task_type'
go

exec sp_addextendedproperty 'MS_Description', N'审批人', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'assignee'
go

exec sp_addextendedproperty 'MS_Description', N'租户编号', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN',
     'tenant_id'
go

exec sp_addextendedproperty 'MS_Description', N'创建部门', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN',
     'create_dept'
go

exec sp_addextendedproperty 'MS_Description', N'创建者', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'create_by'
go

exec sp_addextendedproperty 'MS_Description', N'创建时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN',
     'create_time'
go

exec sp_addextendedproperty 'MS_Description', N'更新者', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN', 'update_by'
go

exec sp_addextendedproperty 'MS_Description', N'更新时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_task_back_node', 'COLUMN',
     'update_time'
go

create table wf_definition_config
(
    id            bigint not null primary key,
    table_name    nvarchar(255)  not null,
    definition_id nvarchar(255)  not null
        constraint uni_definition_id
        unique,
    process_key   nvarchar(255)  not null,
    version       bigint         not null,
    remark        nvarchar(500) DEFAULT ('') null,
    tenant_id     nvarchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   datetime2,
    update_by     bigint,
    update_time   datetime2
)

go
exec sp_addextendedproperty 'MS_Description', N'流程定义配置', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config'
go

exec sp_addextendedproperty 'MS_Description', N'主键', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN', 'id'
go

exec sp_addextendedproperty 'MS_Description', N'表名', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'table_name'
go

exec sp_addextendedproperty 'MS_Description', N'流程定义ID', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'definition_id'
go

exec sp_addextendedproperty 'MS_Description', N'流程KEY', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'process_key'
go

exec sp_addextendedproperty 'MS_Description', N'流程版本', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'version'
go

exec sp_addextendedproperty 'MS_Description', N'备注', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'remark'
go

exec sp_addextendedproperty 'MS_Description', N'租户编号', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'tenant_id'
go

exec sp_addextendedproperty 'MS_Description', N'创建部门', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'create_dept'
go

exec sp_addextendedproperty 'MS_Description', N'创建者', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN', 'create_by'
go

exec sp_addextendedproperty 'MS_Description', N'创建时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'create_time'
go

exec sp_addextendedproperty 'MS_Description', N'更新者', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN', 'update_by'
go

exec sp_addextendedproperty 'MS_Description', N'更新时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_definition_config', 'COLUMN',
     'update_time'
go

create table wf_form_manage
(
    id            bigint not null primary key,
    form_name     nvarchar(255) not null,
    form_type     nvarchar(255) not null,
    router        nvarchar(255) not null,
    remark        nvarchar(500) null,
    tenant_id     nvarchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   datetime2,
    update_by     bigint,
    update_time   datetime2
)

go
exec sp_addextendedproperty 'MS_Description', N'表单管理', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage'
go

exec sp_addextendedproperty 'MS_Description', N'主键', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN', 'id'
go

exec sp_addextendedproperty 'MS_Description', N'表单名称', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'form_name'
go

exec sp_addextendedproperty 'MS_Description', N'表单类型', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'form_type'
go

exec sp_addextendedproperty 'MS_Description', N'路由地址/表单ID', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'router'
go

exec sp_addextendedproperty 'MS_Description', N'备注', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'remark'
go

exec sp_addextendedproperty 'MS_Description', N'租户编号', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'tenant_id'
go

exec sp_addextendedproperty 'MS_Description', N'创建部门', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'create_dept'
go

exec sp_addextendedproperty 'MS_Description', N'创建者', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN', 'create_by'
go

exec sp_addextendedproperty 'MS_Description', N'创建时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'create_time'
go

exec sp_addextendedproperty 'MS_Description', N'更新者', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN', 'update_by'
go

exec sp_addextendedproperty 'MS_Description', N'更新时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_form_manage', 'COLUMN',
     'update_time'
go

insert into wf_form_manage(id, form_name, form_type, router, remark, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (1, '请假申请', 'static', '/workflow/leaveEdit/index', NULL, '000000', 103, 1, getdate(), 1, getdate());

create table wf_node_config
(
    id               bigint not null primary key,
    form_id          bigint,
    form_type        nvarchar(255) ,
    node_name        nvarchar(255) not null,
    node_id          nvarchar(255) not null,
    definition_id    nvarchar(255) not null,
    apply_user_task  nchar default ('0')  null,
    tenant_id        nvarchar(20),
    create_dept      bigint,
    create_by        bigint,
    create_time      datetime2,
    update_by        bigint,
    update_time      datetime2
)

go
exec sp_addextendedproperty 'MS_Description', N'节点配置', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config'
go

exec sp_addextendedproperty 'MS_Description', N'主键', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN', 'id'
go

exec sp_addextendedproperty 'MS_Description', N'表单id', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'form_id'
go

exec sp_addextendedproperty 'MS_Description', N'表单类型', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'form_type'
go

exec sp_addextendedproperty 'MS_Description', N'节点名称', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'node_name'
go

exec sp_addextendedproperty 'MS_Description', N'节点id', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'node_id'
go

exec sp_addextendedproperty 'MS_Description', N'流程定义id', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'definition_id'
go

exec sp_addextendedproperty 'MS_Description', N'是否为申请人节点 （0是 1否）', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'apply_user_task'
go

exec sp_addextendedproperty 'MS_Description', N'租户编号', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'tenant_id'
go

exec sp_addextendedproperty 'MS_Description', N'创建部门', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'create_dept'
go

exec sp_addextendedproperty 'MS_Description', N'创建者', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN', 'create_by'
go

exec sp_addextendedproperty 'MS_Description', N'创建时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'create_time'
go

exec sp_addextendedproperty 'MS_Description', N'更新者', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN', 'update_by'
go

exec sp_addextendedproperty 'MS_Description', N'更新时间', 'SCHEMA', 'dbo', 'TABLE', 'wf_node_config', 'COLUMN',
     'update_time'
go

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11638, '请假申请', 5, 1, 'leave', 'workflow/leave/index', 1, 0, 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, getdate(), NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11639, '请假申请查询', 11638, 1, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11640, '请假申请新增', 11638, 2, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11641, '请假申请修改', 11638, 3, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11642, '请假申请删除', 11638, 4, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, getdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11643, '请假申请导出', 11638, 5, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, getdate(), NULL, NULL, '');

INSERT INTO sys_dict_type(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, getdate(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, getdate(), NULL, NULL, '表单类型列表');

INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, getdate(), NULL, NULL, '已撤销');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, getdate(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1,getdate(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, getdate(), NULL, NULL, '已完成');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, getdate(), NULL, NULL, '已作废');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, getdate(), NULL, NULL, '已退回');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1,getdate(), NULL, NULL, '已终止');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, getdate(), NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, getdate(), NULL, NULL, '动态表单');

-- 表单管理 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11628, '表单管理', '11616', '5', 'formManage', 'workflow/formManage/index', 1, 0, 'C', '0', '0', 'workflow:formManage:list', 'tree-table', 103, 1, getdate(), null, null, '表单管理菜单');

-- 表单管理按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11644, '表单管理查询', 11628, '1',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:query',        '', 103, 1, getdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11645, '表单管理新增', 11628, '2',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:add',          '', 103, 1, getdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11646, '表单管理修改', 11628, '3',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:edit',         '', 103, 1, getdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11647, '表单管理删除', 11628, '4',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:remove',       '', 103, 1, getdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11648, '表单管理导出', 11628, '5',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:export',       'tree-table', 103, 1, getdate(), null, null, '');
