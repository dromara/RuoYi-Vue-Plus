insert into sys_menu values('112',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '1', '0', 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', now(), '', null, '缓存列表菜单');

delete from sys_menu WHERE menu_id = 116;

update sys_config set config_key = 'sys.account.captchaEnabled' where config_id = 4;

insert into sys_menu values('1050', '账户解锁', '501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', now(), '', null, '');

insert into sys_role_menu values ('2', '1050');

-- 字符串自动转时间 避免框架时间查询报错问题
create or replace function cast_varchar_to_timestamp(varchar) returns timestamptz as $$
select to_timestamp($1, 'yyyy-mm-dd hh24:mi:ss');
$$ language sql strict ;

create cast (varchar as timestamptz) with function cast_varchar_to_timestamp as IMPLICIT;

insert into sys_dict_data values(29, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', now(), '', null, '其他操作');

