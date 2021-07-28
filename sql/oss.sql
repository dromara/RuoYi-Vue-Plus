-- ----------------------------
-- OSS云存储表
-- ----------------------------
drop table if exists sys_oss;
create table sys_oss (
    oss_id          bigint(20)   not null auto_increment    comment '云存储主键',
    file_name       varchar(64)  not null default ''        comment '文件名',
    original_name   varchar(64)  not null default ''        comment '原名',
    file_suffix     varchar(10)  not null default ''        comment '文件后缀名',
    url              varchar(200) not null                   comment 'URL地址',
    create_time     datetime              default null      comment '创建时间',
    create_by       varchar(64)           default ''        comment '上传人',
    update_time     datetime              default null      comment '更新时间',
    update_by       varchar(64)           default ''        comment '更新人',
    service         varchar(10)  not null default 'minio'   comment '服务商',
    primary key (oss_id)
) engine=innodb comment ='OSS云存储表';

insert into sys_config values(10, 'OSS云存储服务商',       'sys.oss.cloudStorageService',      'minio',          'Y', 'admin', sysdate(), '', null, 'OSS云存储服务商(qiniu:七牛云, aliyun:阿里云, qcloud:腾讯云, minio: Minio)');
insert into sys_config values(11, 'OSS预览列表资源开关',   'sys.oss.previewListResource',      'true',           'Y', 'admin', sysdate(), '', null, 'true:开启, false:关闭');

insert into sys_menu values('118',  '文件管理', '1',   '10', 'oss',     'system/oss/index',      1, 0, 'C', '0', '0', 'system:oss:list',      'upload',       'admin', sysdate(), '', null, '文件管理菜单');

insert into sys_menu values('1600', '文件查询', '118', '1', '#', '', 1, 0, 'F', '0', '0', 'system:oss:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1601', '文件上传', '118', '2', '#', '', 1, 0, 'F', '0', '0', 'system:oss:upload',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1602', '文件下载', '118', '3', '#', '', 1, 0, 'F', '0', '0', 'system:oss:download',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1603', '文件删除', '118', '4', '#', '', 1, 0, 'F', '0', '0', 'system:oss:remove',       '#', 'admin', sysdate(), '', null, '');
