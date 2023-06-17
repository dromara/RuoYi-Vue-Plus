ALTER TABLE gen_table ADD data_name varchar(200) default ''::varchar;

COMMENT ON COLUMN gen_table.data_name IS '数据源名称';

UPDATE sys_menu SET path = 'powerjob', component = 'monitor/powerjob/index', perms = 'monitor:powerjob:list', remark = 'powerjob控制台菜单' WHERE menu_id = 120;
