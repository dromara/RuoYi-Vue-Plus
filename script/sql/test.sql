DROP TABLE if EXISTS test_demo;
CREATE TABLE test_demo
(
    id          bigint(0)    NOT NULL COMMENT '主键',
    tenant_id   varchar(20)  NULL DEFAULT '000000' COMMENT '租户编号',
    dept_id     bigint(0)    NULL DEFAULT NULL COMMENT '部门id',
    user_id     bigint(0)    NULL DEFAULT NULL COMMENT '用户id',
    order_num   int(0)       NULL DEFAULT 0 COMMENT '排序号',
    test_key    varchar(255) NULL DEFAULT NULL COMMENT 'key键',
    value       varchar(255) NULL DEFAULT NULL COMMENT '值',
    version     int(0)       NULL DEFAULT 0 COMMENT '版本',
    create_dept bigint(0)    NULL DEFAULT NULL COMMENT '创建部门',
    create_time datetime(0)  NULL DEFAULT NULL COMMENT '创建时间',
    create_by   bigint(0)    NULL DEFAULT NULL COMMENT '创建人',
    update_time datetime(0)  NULL DEFAULT NULL COMMENT '更新时间',
    update_by   bigint(0)    NULL DEFAULT NULL COMMENT '更新人',
    del_flag    int(0)       NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '测试单表';

DROP TABLE if EXISTS test_tree;
CREATE TABLE test_tree
(
    id          bigint(0)    NOT NULL COMMENT '主键',
    tenant_id   varchar(20)  NULL DEFAULT '000000' COMMENT '租户编号',
    parent_id   bigint(0)    NULL DEFAULT 0 COMMENT '父id',
    dept_id     bigint(0)    NULL DEFAULT NULL COMMENT '部门id',
    user_id     bigint(0)    NULL DEFAULT NULL COMMENT '用户id',
    tree_name   varchar(255) NULL DEFAULT NULL COMMENT '值',
    version     int(0)       NULL DEFAULT 0 COMMENT '版本',
    create_dept bigint(0)    NULL DEFAULT NULL COMMENT '创建部门',
    create_time datetime(0)  NULL DEFAULT NULL COMMENT '创建时间',
    create_by   bigint(0)    NULL DEFAULT NULL COMMENT '创建人',
    update_time datetime(0)  NULL DEFAULT NULL COMMENT '更新时间',
    update_by   bigint(0)    NULL DEFAULT NULL COMMENT '更新人',
    del_flag    int(0)       NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB COMMENT = '测试树表';

INSERT INTO sys_user(user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (3, '000000', 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), 3, sysdate(), NULL);
INSERT INTO sys_user(user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (4, '000000', 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), 4, sysdate(), NULL);

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (5, '测试菜单', 0, 5, 'demo', NULL, 1, 0, 'M', '0', '0', NULL, 'star', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1500, '测试单表', 5, 1, 'demo', 'demo/demo/index', 1, 0, 'C', '0', '0', 'demo:demo:list', '#', 103, 1, sysdate(), NULL, NULL, '测试单表菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1501, '测试单表查询', 1500, 1, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1502, '测试单表新增', 1500, 2, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1503, '测试单表修改', 1500, 3, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1504, '测试单表删除', 1500, 4, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1505, '测试单表导出', 1500, 5, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:export', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1506, '测试树表', 5, 1, 'tree', 'demo/tree/index', 1, 0, 'C', '0', '0', 'demo:tree:list', '#', 103, 1, sysdate(), NULL, NULL, '测试树表菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1507, '测试树表查询', 1506, 1, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1508, '测试树表新增', 1506, 2, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1509, '测试树表修改', 1506, 3, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1510, '测试树表删除', 1506, 4, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1511, '测试树表导出', 1506, 5, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:export', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_role(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (3, '000000', '本部门及以下', 'test1', 3, '4', 1, 1, '0', '0', 103, 1, sysdate(), 1, NULL, NULL);
INSERT INTO sys_role(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (4, '000000', '仅本人', 'test2', 4, '5', 1, 1, '0', '0', 103, 1, sysdate(), 1, NULL, NULL);

INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 5);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 100);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 101);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 102);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 103);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 104);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 105);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 106);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 107);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 108);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 500);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 501);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1001);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1002);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1003);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1004);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1005);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1006);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1007);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1008);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1009);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1010);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1011);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1012);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1013);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1014);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1015);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1016);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1017);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1018);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1019);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1020);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1021);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1022);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1023);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1024);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1025);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1026);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1027);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1028);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1029);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1030);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1031);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1032);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1033);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1034);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1035);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1036);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1037);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1038);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1039);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1040);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1041);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1042);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1043);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1044);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1045);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1500);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1501);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1502);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1503);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1504);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1505);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1506);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1507);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1508);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1509);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1510);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (3, 1511);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 5);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1500);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1501);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1502);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1503);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1504);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1505);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1506);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1507);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1508);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1509);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1510);
INSERT INTO sys_role_menu(role_id, menu_id) VALUES (4, 1511);

INSERT INTO sys_user_role(user_id, role_id) VALUES (3, 3);
INSERT INTO sys_user_role(user_id, role_id) VALUES (4, 4);

INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (1, '000000', 102, 4, 1, '测试数据权限', '测试', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (2, '000000', 102, 3, 2, '子节点1', '111', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (3, '000000', 102, 3, 3, '子节点2', '222', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (4, '000000', 108, 4, 4, '测试数据', 'demo', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (5, '000000', 108, 3, 13, '子节点11', '1111', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (6, '000000', 108, 3, 12, '子节点22', '2222', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (7, '000000', 108, 3, 11, '子节点33', '3333', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (8, '000000', 108, 3, 10, '子节点44', '4444', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (9, '000000', 108, 3, 9, '子节点55', '5555', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (10, '000000', 108, 3, 8, '子节点66', '6666', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (11, '000000', 108, 3, 7, '子节点77', '7777', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (12, '000000', 108, 3, 6, '子节点88', '8888', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (13, '000000', 108, 3, 5, '子节点99', '9999', 0, 103, sysdate(), 1, NULL, NULL, 0);

INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (1, '000000', 0, 102, 4, '测试数据权限', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (2, '000000', 1, 102, 3, '子节点1', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (3, '000000', 2, 102, 3, '子节点2', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (4, '000000', 0, 108, 4, '测试树1', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (5, '000000', 4, 108, 3, '子节点11', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (6, '000000', 4, 108, 3, '子节点22', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (7, '000000', 4, 108, 3, '子节点33', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (8, '000000', 5, 108, 3, '子节点44', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (9, '000000', 6, 108, 3, '子节点55', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (10, '000000', 7, 108, 3, '子节点66', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (11, '000000', 7, 108, 3, '子节点77', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (12, '000000', 10, 108, 3, '子节点88', 0, 103, sysdate(), 1, NULL, NULL, 0);
INSERT INTO test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) VALUES (13, '000000', 10, 108, 3, '子节点99', 0, 103, sysdate(), 1, NULL, NULL, 0);
