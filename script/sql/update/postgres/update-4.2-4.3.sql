insert into sys_menu values('112',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '1', '0', 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', now(), '', null, '缓存列表菜单');

delete from sys_menu WHERE menu_id = 116;

update sys_config set config_key = 'sys.account.captchaEnabled' where config_id = 4

insert into sys_menu values('1050', '账户解锁', '501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', now(), '', null, '');

insert into sys_role_menu values ('2', '1050');
