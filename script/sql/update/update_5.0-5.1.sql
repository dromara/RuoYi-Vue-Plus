ALTER TABLE gen_table ADD COLUMN data_name varchar(200) NULL DEFAULT '' COMMENT '数据源名称' AFTER table_id;

UPDATE sys_menu SET path = 'powerjob', component = 'monitor/powerjob/index', perms = 'monitor:powerjob:list', remark = 'powerjob控制台菜单' WHERE menu_id = 120
