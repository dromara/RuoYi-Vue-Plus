ALTER TABLE sys_dept ADD dept_category VARCHAR(100) DEFAULT NULL COMMENT '部门类别编码';
ALTER TABLE sys_post ADD dept_id BIGINT(20) NOT NULL COMMENT '部门id', ADD post_category VARCHAR(100) DEFAULT NULL COMMENT '岗位类别编码';
UPDATE sys_post SET dept_id = 100;
UPDATE sys_post SET dept_id = 103 where post_id = 1;
UPDATE sys_menu SET menu_name = 'SnailJob控制台', path = 'snailjob', component = 'monitor/snailjob/index', perms = 'monitor:snailjob:list', remark = 'SnailJob控制台菜单' WHERE menu_id = 120;
