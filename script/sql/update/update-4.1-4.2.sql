alter table sys_oss_config add column domain varchar(255) null default '' COMMENT '自定义域名';

update sys_oss_config set endpoint = '127.0.0.1:9000' where oss_config_id = 1;
update sys_oss_config set endpoint = 's3-cn-north-1.qiniucs.com', region = '' where oss_config_id = 2;
update sys_oss_config set endpoint = 'oss-cn-beijing.aliyuncs.com' where oss_config_id = 3;
update sys_oss_config set endpoint = 'cos.ap-beijing.myqcloud.com' where oss_config_id = 4;

insert into sys_oss_config values (5, 'image',  'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', 'N', '', '1', '', 'admin', sysdate(), 'admin', sysdate(), NULL, '');

alter table gen_table_column modify column table_id bigint(0) null default null COMMENT '归属表编号';

alter table sys_notice modify column notice_id bigint(0) not null COMMENT '公告ID';

alter table sys_config modify column config_id bigint(0) not null COMMENT '参数主键';
