ALTER TABLE sys_dept ADD COLUMN dept_category varchar(100) default null::varchar;
COMMENT ON COLUMN sys_dept.dept_category IS '客户端';
ALTER TABLE sys_post ADD COLUMN dept_id int8 NOT NULL;
COMMENT ON COLUMN sys_post.dept_id IS '部门id';
ALTER TABLE sys_post ADD COLUMN post_category varchar(100) default null::varchar;
COMMENT ON COLUMN sys_post.post_category IS '岗位类别编码';
UPDATE sys_post SET dept_id = 100;
UPDATE sys_post SET dept_id = 103 where post_id = 1;
UPDATE sys_menu SET menu_name = 'SnailJob控制台', path = 'snailjob', component = 'monitor/snailjob/index', perms = 'monitor:snailjob:list', remark = 'SnailJob控制台菜单' WHERE menu_id = 120;
