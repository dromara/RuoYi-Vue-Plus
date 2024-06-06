insert into sys_menu values('11616', '工作流'  , '0',    '6', 'workflow',          '',                                 '', '1', '0', 'M', '0', '0', '',                       'workflow', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11617', '模型管理', '11616', '2', 'model',             'workflow/model/index',             '', '1', '1', 'C', '0', '0', 'workflow:model:list',    'model', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11618', '我的任务', '0', '7', 'task',              '',                                 '', '1', '0', 'M', '0', '0', '',                       'my-task', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11619', '我的待办', '11618', '2', 'taskWaiting',       'workflow/task/taskWaiting',              '', '1', '1', 'C', '0', '0', '',                       'waiting', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11632', '我的已办', '11618', '3', 'taskFinish',       'workflow/task/taskFinish',              '', '1', '1', 'C', '0', '0', '',                       'finish', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11633', '我的抄送', '11618', '4', 'taskCopyList',       'workflow/task/taskCopyList',              '', '1', '1', 'C', '0', '0', '',                       'my-copy', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11620', '流程定义', '11616', '3', 'processDefinition', 'workflow/processDefinition/index', '', '1', '1', 'C', '0', '0', '',                       'process-definition', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11621', '流程实例', '11630', '1', 'processInstance',   'workflow/processInstance/index',   '', '1', '1', 'C', '0', '0', '',                       'tree-table', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11622', '流程分类', '11616', '1', 'category',          'workflow/category/index',          '', '1', '0', 'C', '0', '0', 'workflow:category:list', 'category', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11629', '我发起的', '11618', '1', 'myDocument',        'workflow/task/myDocument',         '', '1', '1', 'C', '0', '0', '',                       'guide', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11630', '流程监控', '11616', '4', 'monitor',           '',                                 '', '1', '0', 'M', '0', '0', '',                       'monitor', 103, 1, sysdate, NULL, NULL, '');
insert into sys_menu values('11631', '待办任务', '11630', '2', 'allTaskWaiting',    'workflow/task/allTaskWaiting',     '', '1', '1', 'C', '0', '0', '',                       'waiting', 103, 1, sysdate, NULL, NULL, '');


-- 流程分类管理相关按钮
insert into sys_menu values ('11623', '流程分类查询', '11622', '1', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values ('11624', '流程分类新增', '11622', '2', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add',   '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values ('11625', '流程分类修改', '11622', '3', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit',  '#', 103, 1, sysdate, null, null, '');
insert into sys_menu values ('11626', '流程分类删除', '11622', '4', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove','#', 103, 1, sysdate, null, null, '');
insert into sys_menu values ('11627', '流程分类导出', '11622', '5', '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export','#', 103, 1, sysdate, null, null, '');

-- 请假单信息
create table TEST_LEAVE
(
    ID          NUMBER(20) not null
        constraint PK_TEST_LEAVE
        primary key,
    LEAVE_TYPE  VARCHAR2(255),
    START_DATE  DATE,
    END_DATE    DATE,
    LEAVE_DAYS  NUMBER(10),
    REMARK      VARCHAR2(255),
    STATUS      VARCHAR2(255),
    CREATE_DEPT NUMBER(20),
    CREATE_BY   NUMBER(20),
    CREATE_TIME DATE,
    UPDATE_BY   NUMBER(20),
    UPDATE_TIME DATE,
    TENANT_ID   VARCHAR2(20)
);

comment on table TEST_LEAVE is '请假申请表';
comment on column TEST_LEAVE.ID is '主键';
comment on column TEST_LEAVE.LEAVE_TYPE is '请假类型';
comment on column TEST_LEAVE.START_DATE is '开始时间';
comment on column TEST_LEAVE.END_DATE is '结束时间';
comment on column TEST_LEAVE.LEAVE_DAYS is '请假天数';
comment on column TEST_LEAVE.REMARK is '请假原因';
comment on column TEST_LEAVE.STATUS is '状态';
comment on column TEST_LEAVE.CREATE_DEPT is '创建部门';
comment on column TEST_LEAVE.CREATE_BY is '创建者';
comment on column TEST_LEAVE.CREATE_TIME is '创建时间';
comment on column TEST_LEAVE.UPDATE_BY is '更新者';
comment on column TEST_LEAVE.UPDATE_TIME is '更新时间';
comment on column TEST_LEAVE.TENANT_ID is '租户编号';

-- 流程分类信息表
create table WF_CATEGORY
(
    ID            NUMBER(20) not null
        constraint PK_WF_CATEGORY
        primary key,
    CATEGORY_NAME VARCHAR2(255),
    CATEGORY_CODE VARCHAR2(255)
        constraint UNI_CATEGORY_CODE
        unique,
    PARENT_ID     NUMBER(20),
    SORT_NUM      NUMBER(10),
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE
);

comment on table WF_CATEGORY is '流程分类';
comment on column WF_CATEGORY.ID is '主键';
comment on column WF_CATEGORY.CATEGORY_NAME is '分类名称';
comment on column WF_CATEGORY.CATEGORY_CODE is '分类编码';
comment on column WF_CATEGORY.PARENT_ID is '父级id';
comment on column WF_CATEGORY.SORT_NUM is '排序';
comment on column WF_CATEGORY.TENANT_ID is '租户编号';
comment on column WF_CATEGORY.CREATE_DEPT is '创建部门';
comment on column WF_CATEGORY.CREATE_BY is '创建者';
comment on column WF_CATEGORY.CREATE_TIME is '创建时间';
comment on column WF_CATEGORY.UPDATE_BY is '更新者';
comment on column WF_CATEGORY.UPDATE_TIME is '更新时间';
INSERT INTO wf_category values (1, 'OA', 'OA', 0, 0, '000000', 103, 1, sysdate, 1, sysdate);

create table WF_TASK_BACK_NODE
(
    ID            NUMBER(20) not null
        constraint PK_WF_TASK_BACK_NODE
        primary key,
    NODE_ID       VARCHAR2(255) not null,
    NODE_NAME     VARCHAR2(255) not null,
    ORDER_NO      NUMBER(20) not null,
    INSTANCE_ID   VARCHAR2(255) not null,
    TASK_TYPE     VARCHAR2(255) not null,
    ASSIGNEE      VARCHAR2(2000) not null,
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE
);
comment on table WF_TASK_BACK_NODE is '节点审批记录';
comment on column WF_TASK_BACK_NODE.ID is '主键';
comment on column WF_TASK_BACK_NODE.NODE_ID is '节点id';
comment on column WF_TASK_BACK_NODE.NODE_NAME is '节点名称';
comment on column WF_TASK_BACK_NODE.ORDER_NO is '排序';
comment on column WF_TASK_BACK_NODE.INSTANCE_ID is '流程实例id';
comment on column WF_TASK_BACK_NODE.TASK_TYPE is '节点类型';
comment on column WF_TASK_BACK_NODE.ASSIGNEE is '审批人';
comment on column WF_TASK_BACK_NODE.TENANT_ID is '租户编号';
comment on column WF_TASK_BACK_NODE.CREATE_DEPT is '创建部门';
comment on column WF_TASK_BACK_NODE.CREATE_BY is '创建者';
comment on column WF_TASK_BACK_NODE.CREATE_TIME is '创建时间';
comment on column WF_TASK_BACK_NODE.UPDATE_BY is '更新者';
comment on column WF_TASK_BACK_NODE.UPDATE_TIME is '更新时间';

create table WF_DEFINITION_CONFIG
(
    ID            NUMBER(20) NOT NULL
        CONSTRAINT PK_WF_DEFINITION_CONFIG
        PRIMARY KEY,
    TABLE_NAME    VARCHAR2(255) NOT NULL,
    DEFINITION_ID VARCHAR2(255) NOT NULL,
    PROCESS_KEY   VARCHAR2(255) NOT NULL,
    VERSION       NUMBER(10)    NOT NULL,
    REMARK        VARCHAR2(500),
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE,
    constraint uni_definition_id
        unique (definition_id)
);
comment on table WF_DEFINITION_CONFIG is '流程定义配置';
comment on column WF_DEFINITION_CONFIG.ID is '主键';
comment on column WF_DEFINITION_CONFIG.TABLE_NAME is '表名';
comment on column WF_DEFINITION_CONFIG.DEFINITION_ID is '流程定义ID';
comment on column WF_DEFINITION_CONFIG.PROCESS_KEY is '流程KEY';
comment on column WF_DEFINITION_CONFIG.VERSION is '流程版本';
comment on column WF_DEFINITION_CONFIG.TENANT_ID is '租户编号';
comment on column WF_DEFINITION_CONFIG.REMARK is '备注';
comment on column WF_DEFINITION_CONFIG.CREATE_DEPT is '创建部门';
comment on column WF_DEFINITION_CONFIG.CREATE_BY is '创建者';
comment on column WF_DEFINITION_CONFIG.CREATE_TIME is '创建时间';
comment on column WF_DEFINITION_CONFIG.UPDATE_BY is '更新者';
comment on column WF_DEFINITION_CONFIG.UPDATE_TIME is '更新时间';

create table WF_FORM_MANAGE
(
    ID            NUMBER(20) NOT NULL
        CONSTRAINT PK_WF_FORM_MANAGE
        PRIMARY KEY,
    FORM_NAME     VARCHAR2(255) NOT NULL,
    FORM_TYPE     VARCHAR2(255) NOT NULL,
    ROUTER        VARCHAR2(255) NOT NULL,
    REMARK        VARCHAR2(500),
    TENANT_ID     VARCHAR2(20),
    CREATE_DEPT   NUMBER(20),
    CREATE_BY     NUMBER(20),
    CREATE_TIME   DATE,
    UPDATE_BY     NUMBER(20),
    UPDATE_TIME   DATE
);

comment on table WF_FORM_MANAGE is '表单管理';
comment on column WF_FORM_MANAGE.ID is '主键';
comment on column WF_FORM_MANAGE.FORM_NAME is '表单名称';
comment on column WF_FORM_MANAGE.FORM_TYPE is '表单类型';
comment on column WF_FORM_MANAGE.ROUTER is '路由地址/表单ID';
comment on column WF_FORM_MANAGE.REMARK is '备注';
comment on column WF_FORM_MANAGE.TENANT_ID is '租户编号';
comment on column WF_FORM_MANAGE.CREATE_DEPT is '创建部门';
comment on column WF_FORM_MANAGE.CREATE_BY is '创建者';
comment on column WF_FORM_MANAGE.CREATE_TIME is '创建时间';
comment on column WF_FORM_MANAGE.UPDATE_BY is '更新者';
comment on column WF_FORM_MANAGE.UPDATE_TIME is '更新时间';

insert into wf_form_manage(id, form_name, form_type, router, remark, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (1, '请假申请', 'static', '/workflow/leaveEdit/index', NULL, '000000', 103, 1, sysdate, 1, sysdate);

create table WF_NODE_CONFIG
(
    ID               NUMBER(20) NOT NULL
        CONSTRAINT PK_WF_NODE_CONFIG
        PRIMARY KEY,
    FORM_ID          NUMBER(20),
    FORM_TYPE        VARCHAR2(255),
    NODE_NAME        VARCHAR2(255) NOT NULL,
    NODE_ID          VARCHAR2(255) NOT NULL,
    DEFINITION_ID    VARCHAR2(255) NOT NULL,
    APPLY_USER_TASK  CHAR(1) DEFAULT '0',
    TENANT_ID        VARCHAR2(20),
    CREATE_DEPT      NUMBER(20),
    CREATE_BY        NUMBER(20),
    CREATE_TIME      DATE,
    UPDATE_BY        NUMBER(20),
    UPDATE_TIME      DATE
);

comment on table WF_NODE_CONFIG is '节点配置';
comment on column WF_NODE_CONFIG.ID is '主键';
comment on column WF_NODE_CONFIG.FORM_ID is '表单id';
comment on column WF_NODE_CONFIG.FORM_TYPE is '表单类型';
comment on column WF_NODE_CONFIG.NODE_ID is '节点id';
comment on column WF_NODE_CONFIG.NODE_NAME is '节点名称';
comment on column WF_NODE_CONFIG.DEFINITION_ID is '流程定义id';
comment on column WF_NODE_CONFIG.APPLY_USER_TASK is '是否为申请人节点 （0是 1否）';
comment on column WF_NODE_CONFIG.TENANT_ID is '租户编号';
comment on column WF_NODE_CONFIG.CREATE_DEPT is '创建部门';
comment on column WF_NODE_CONFIG.CREATE_BY is '创建者';
comment on column WF_NODE_CONFIG.CREATE_TIME is '创建时间';
comment on column WF_NODE_CONFIG.UPDATE_BY is '更新者';
comment on column WF_NODE_CONFIG.UPDATE_TIME is '更新时间';

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11638, '请假申请', 5, 1, 'leave', 'workflow/leave/index', 1, 0, 'C', '0', '0', 'workflow:leave:list', '#', 103, 1, sysdate, NULL, NULL, '请假申请菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11639, '请假申请查询', 11638, 1, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:query', '#', 103, 1, sysdate, NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11640, '请假申请新增', 11638, 2, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:add', '#', 103, 1, sysdate, NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11641, '请假申请修改', 11638, 3, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:edit', '#', 103, 1, sysdate, NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11642, '请假申请删除', 11638, 4, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:remove', '#', 103, 1, sysdate, NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (11643, '请假申请导出', 11638, 5, '#', '', 1, 0, 'F', '0', '0', 'workflow:leave:export', '#', 103, 1, sysdate, NULL, NULL, '');

INSERT INTO sys_dict_type(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (13, '000000', '业务状态', 'wf_business_status', 103, 1, sysdate, NULL, NULL, '业务状态列表');
INSERT INTO sys_dict_type(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (14, '000000', '表单类型', 'wf_form_type', 103, 1, sysdate, NULL, NULL, '表单类型列表');

INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (39, '000000', 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate, NULL, NULL, '已撤销');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (40, '000000', 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 103, 1, sysdate, NULL, NULL, '草稿');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (41, '000000', 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 103, 1,sysdate, NULL, NULL, '待审核');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (42, '000000', 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 103, 1, sysdate, NULL, NULL, '已完成');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (43, '000000', 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate, NULL, NULL, '已作废');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (44, '000000', 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 103, 1, sysdate, NULL, NULL, '已退回');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (45, '000000', 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 103, 1,sysdate, NULL, NULL, '已终止');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (46, '000000', 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 103, 1, sysdate, NULL, NULL, '自定义表单');
INSERT INTO sys_dict_data(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (47, '000000', 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 103, 1, sysdate, NULL, NULL, '动态表单');

-- 表单管理 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11628, '表单管理', '11616', '5', 'formManage', 'workflow/formManage/index', 1, 0, 'C', '0', '0', 'workflow:formManage:list', 'tree-table', 103, 1, sysdate, null, null, '表单管理菜单');

-- 表单管理按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11644, '表单管理查询', 11628, '1',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:query',        '', 103, 1, sysdate, null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11645, '表单管理新增', 11628, '2',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:add',          '', 103, 1, sysdate, null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11646, '表单管理修改', 11628, '3',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:edit',         '', 103, 1, sysdate, null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11647, '表单管理删除', 11628, '4',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:remove',       '', 103, 1, sysdate, null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(11648, '表单管理导出', 11628, '5',  '#', '', 1, 0, 'F', '0', '0', 'workflow:formManage:export',       'tree-table', 103, 1, sysdate, null, null, '');
