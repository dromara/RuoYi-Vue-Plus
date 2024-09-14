insert into sys_menu values('11616', '工作流'  , '0',    '6', 'workflow',          '',                                 '', '1', '0', 'M', '0', '0', '',                       'workflow', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11617', '模型管理', '11616', '2', 'model',             'workflow/model/index',             '', '1', '1', 'C', '0', '0', 'workflow:model:list',    'model', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11618', '我的任务', '0', '7', 'task',              '',                                 '', '1', '0', 'M', '0', '0', '',                       'my-task', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11619', '我的待办', '11618', '2', 'taskWaiting',       'workflow/task/taskWaiting',              '', '1', '1', 'C', '0', '0', '',                       'waiting', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11632', '我的已办', '11618', '3', 'taskFinish',       'workflow/task/taskFinish',              '', '1', '1', 'C', '0', '0', '',                       'finish', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11633', '我的抄送', '11618', '4', 'taskCopyList',       'workflow/task/taskCopyList',              '', '1', '1', 'C', '0', '0', '',                       'my-copy', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '',                       'process-definition', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11621', '流程实例', '11630', '1', 'processInstance',   'workflow/processInstance/index',   '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11622', '流程分类', '11616', '1', 'category',          'workflow/category/index',          '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11629', '我发起的', '11618', '1', 'myDocument',        'workflow/task/myDocument',         '', '1', '1', 'C', '0', '0', '',                       'guide', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11630', '流程监控', '11616', '4', 'monitor',           '',                                 '', '1', '0', 'M', '0', '0', '',                       'monitor', 103, 1, now(), NULL, NULL, '');
insert into sys_menu values('11631', '待办任务', '11630', '2', 'allTaskWaiting',    'workflow/task/allTaskWaiting',     '', '1', '1', 'C', '0', '0', '',                       'waiting', 103, 1, now(), NULL, NULL, '');


-- 流程分类管理相关按钮
insert into sys_menu values ('11623', '流程分类查询', '11622', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1, now(), null, null, '');
insert into sys_menu values ('11624', '流程分类新增', '11622', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values ('11625', '流程分类修改', '11622', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values ('11626', '流程分类删除', '11622', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove','#', 103, 1, now(), null, null, '');
insert into sys_menu values ('11627', '流程分类导出', '11622', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export','#', 103, 1, now(), null, null, '');
-- 请假单信息
create table test_leave
(
    id          bigint not null
        constraint test_leave_pk
            primary key,
    leave_type  varchar(255),
    start_date  timestamp,
    end_date    timestamp,
    leave_days  bigint,
    remark      varchar(255),
    status      varchar(255),
    create_dept bigint,
    create_by   bigint,
    create_time timestamp,
    update_by   bigint,
    update_time timestamp,
    tenant_id   varchar(20)
);

comment on table test_leave is '请假申请表';
comment on column test_leave.id is '主键';
comment on column test_leave.leave_type is '请假类型';
comment on column test_leave.start_date is '开始时间';
comment on column test_leave.end_date is '结束时间';
comment on column test_leave.remark is '请假原因';
comment on column test_leave.status is '状态';
comment on column test_leave.create_dept is '创建部门';
comment on column test_leave.create_by is '创建者';
comment on column test_leave.create_time is '创建时间';
comment on column test_leave.update_by is '更新者';
comment on column test_leave.update_time is '更新时间';
comment on column test_leave.tenant_id is '租户编码';

alter table test_leave
    owner to postgres;

-- 流程分类信息表
create table wf_category
(
    id            bigint not null
        constraint wf_category_pk
            primary key,
    category_name varchar(255),
    category_code varchar(255),
    parent_id     bigint,
    sort_num      bigint,
    tenant_id     varchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_category is '流程分类';
comment on column wf_category.id is '主键';
comment on column wf_category.category_name is '分类名称';
comment on column wf_category.category_code is '分类编码';
comment on column wf_category.parent_id is '父级id';
comment on column wf_category.sort_num is '排序';
comment on column wf_category.tenant_id is '租户id';
comment on column wf_category.create_dept is '创建部门';
comment on column wf_category.create_by is '创建者';
comment on column wf_category.create_time is '创建时间';
comment on column wf_category.update_by is '修改者';
comment on column wf_category.update_time is '修改时间';

alter table wf_category
    owner to postgres;

create unique index uni_category_code
    on wf_category (category_code);

INSERT INTO wf_category values (1, 'OA', 'OA', 0, 0, '000000', 103, 1, now(), 1, now());

create table wf_task_back_node
(
    id            bigint not null
        constraint pk_wf_task_back_node
        primary key,
    node_id       varchar(255) not null,
    node_name     varchar(255) not null,
    order_no      bigint not null,
    instance_id   varchar(255) not null,
    task_type     varchar(255) not null,
    assignee      varchar(2000) not null,
    tenant_id     varchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_task_back_node is '节点审批记录';
comment on column wf_task_back_node.id is '主键';
comment on column wf_task_back_node.node_id is '节点id';
comment on column wf_task_back_node.node_name is '节点名称';
comment on column wf_task_back_node.order_no is '排序';
comment on column wf_task_back_node.instance_id is '流程实例id';
comment on column wf_task_back_node.task_type is '节点类型';
comment on column wf_task_back_node.assignee is '审批人';
comment on column wf_task_back_node.tenant_id is '租户id';
comment on column wf_task_back_node.create_dept is '创建部门';
comment on column wf_task_back_node.create_by is '创建者';
comment on column wf_task_back_node.create_time is '创建时间';
comment on column wf_task_back_node.update_by is '修改者';
comment on column wf_task_back_node.update_time is '修改时间';

alter table wf_task_back_node
    owner to postgres;

create table wf_definition_config
(
    id            bigint not null
        constraint pk_wf_definition_config
        primary key,
    table_name    varchar(255) not null,
    definition_id varchar(255) not null,
    process_key   varchar(255) not null,
    version       bigint       not null,
    tenant_id     varchar(20),
    remark        varchar(500),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_definition_config is '流程定义配置';
comment on column wf_definition_config.id is '主键';
comment on column wf_definition_config.table_name is '表名';
comment on column wf_definition_config.definition_id is '流程定义ID';
comment on column wf_definition_config.process_key is '流程KEY';
comment on column wf_definition_config.version is '流程版本';
comment on column wf_definition_config.tenant_id is '租户id';
comment on column wf_definition_config.remark is '备注';
comment on column wf_definition_config.create_dept is '创建部门';
comment on column wf_definition_config.create_by is '创建者';
comment on column wf_definition_config.create_time is '创建时间';
comment on column wf_definition_config.update_by is '修改者';
comment on column wf_definition_config.update_time is '修改时间';

alter table wf_definition_config
    owner to postgres;
create unique index uni_definition_id
    on wf_definition_config (definition_id);

create table wf_form_manage
(
    id            bigint not null
        constraint pk_wf_form_manage
        primary key,
    form_name     varchar(255) not null,
    form_type     varchar(255) not null,
    router        varchar(255) not null,
    remark        varchar(500),
    tenant_id     varchar(20),
    create_dept   bigint,
    create_by     bigint,
    create_time   timestamp,
    update_by     bigint,
    update_time   timestamp
);

comment on table wf_form_manage is '表单管理';
comment on column wf_form_manage.id is '主键';
comment on column wf_form_manage.form_name is '表单名称';
comment on column wf_form_manage.form_type is '表单类型';
comment on column wf_form_manage.router is '路由地址/表单ID';
comment on column wf_form_manage.remark is '备注';
comment on column wf_form_manage.tenant_id is '租户id';
comment on column wf_form_manage.create_dept is '创建部门';
comment on column wf_form_manage.create_by is '创建者';
comment on column wf_form_manage.create_time is '创建时间';
comment on column wf_form_manage.update_by is '修改者';
comment on column wf_form_manage.update_time is '修改时间';

insert into wf_form_manage(id, form_name, form_type, router, remark, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (1, '请假申请', 'static', '/workflow/leaveEdit/index', NULL, '000000', 103, 1, now(), 1, now());

create table wf_node_config
(
    id               bigint not null
        constraint pk_wf_node_config
            primary key,
    form_id          bigint,
    form_type        varchar(255),
    node_name        varchar(255) not null,
    node_id          varchar(255) not null,
    definition_id    varchar(255) not null,
    apply_user_task  char(1) default '0',
    tenant_id        varchar(20),
    create_dept      bigint,
    create_by        bigint,
    create_time      timestamp,
    update_by        bigint,
    update_time      timestamp
);

comment on table wf_node_config is '节点配置';
comment on column wf_node_config.id is '主键';
comment on column wf_node_config.form_id is '表单id';
comment on column wf_node_config.form_type is '表单类型';
comment on column wf_node_config.node_id is '节点id';
comment on column wf_node_config.node_name is '节点名称';
comment on column wf_node_config.definition_id is '流程定义id';
comment on column wf_node_config.apply_user_task is '是否为申请人节点 （0是 1否）';
comment on column wf_node_config.tenant_id is '租户id';
comment on column wf_node_config.create_dept is '创建部门';
comment on column wf_node_config.create_by is '创建者';
comment on column wf_node_config.create_time is '创建时间';
comment on column wf_node_config.update_by is '修改者';
comment on column wf_node_config.update_time is '修改时间';

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11638, '请假申请', 5, 1, 'leave', 'workflow/leave/index', 1, 0, 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, now(), NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11639, '请假申请查询', 11638, 1, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11640, '请假申请新增', 11638, 2, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11641, '请假申请修改', 11638, 3, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11642, '请假申请删除', 11638, 4, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, now(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11643, '请假申请导出', 11638, 5, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, now(), NULL, NULL, '');

INSERT INTO sys_dict_type(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, now(), NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, now(), NULL, NULL, '表单类型列表');

INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已撤销');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, now(), NULL, NULL, '草稿');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1,now(), NULL, NULL, '待审核');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, now(), NULL, NULL, '已完成');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已作废');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, now(), NULL, NULL, '已退回');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1,now(), NULL, NULL, '已终止');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, now(), NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '动态表单');

-- 表单管理 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11628, '表单管理', '11616', '5', 'formManage', 'workflow/formManage/index', 1, 0, 'C', '0', '0', 'workflow:formManage:list', 'tree-table', 103, 1, now(), null, null, '表单管理菜单');

-- 表单管理按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11644, '表单管理查询', 11628, '1',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:query',        '', 103, 1, now(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11645, '表单管理新增', 11628, '2',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:add',          '', 103, 1, now(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11646, '表单管理修改', 11628, '3',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:edit',         '', 103, 1, now(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11647, '表单管理删除', 11628, '4',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:remove',       '', 103, 1, now(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11648, '表单管理导出', 11628, '5',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:export',       'tree-table', 103, 1, now(), null, null, '');
