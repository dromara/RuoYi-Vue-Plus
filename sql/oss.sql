-- ----------------------------
-- OSS对象存储表
-- ----------------------------
drop table if exists sys_oss;
create table sys_oss (
    oss_id          bigint(20)   not null auto_increment    comment '对象存储主键',
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
) engine=innodb comment ='OSS对象存储表';

-- ----------------------------
-- OSS对象存储动态配置表
-- ----------------------------
drop table if exists sys_oss_config;
create table sys_oss_config (
    oss_config_id   bigint(20)   not null auto_increment comment '主建',
    config_key      varchar(255)  not null default ''     comment '配置key',
    access_key      varchar(255)            default ''    comment 'accessKey',
    secret_key      varchar(255)            default ''    comment '秘钥',
    bucket_name     varchar(255)            default ''    comment '桶名称',
    prefix           varchar(255)           default ''     comment '前缀',
    endpoint         varchar(255)           default ''     comment '访问站点',
    is_https         char(1)                default 'N'    comment '是否https（Y=是,N=否）',
    region           varchar(255)           default ''     comment '域',
    status           char(1)                default '1'    comment '状态（0=正常,1=停用）',
    ext1             varchar(255)           default ''      comment '扩展字段',
    create_by       varchar(64)             default ''     comment '创建者',
    create_time     datetime                default null   comment '创建时间',
    update_by       varchar(64)             default ''      comment '更新者',
    update_time     datetime                default null    comment '更新时间',
    remark           varchar(500)           default null    comment '备注',
    primary key (oss_config_id)
) engine=innodb comment='对象存储配置表';

insert into sys_config values(11, 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 'admin', sysdate(), '', null, 'true:开启, false:关闭');

insert into sys_menu values('118',  '文件管理', '1', '10', 'oss', 'system/oss/index', '', 1, 0, 'C', '0', '0', 'system:oss:list', 'upload', 'admin', sysdate(), '', null, '文件管理菜单');

insert into sys_menu values('1600', '文件查询', '118', '1', '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1601', '文件上传', '118', '2', '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:upload',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1602', '文件下载', '118', '3', '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:download',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1603', '文件删除', '118', '4', '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:remove',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1604', '配置添加', '118', '5', '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:add',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1605', '配置编辑', '118', '6', '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_oss_config values (1, 'minio',  'ruoyi',            'ruoyi123',        'ruoyi',             '', 'http://localhost:9000',                'N', '',            '0', '', 'admin', sysdate(), 'admin', sysdate(), NULL);
insert into sys_oss_config values (2, 'qiniu',  'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi',             '', 'http://XXX.XXXX.com',                  'N', 'z0',          '1', '', 'admin', sysdate(), 'admin', sysdate(), NULL);
insert into sys_oss_config values (3, 'aliyun', 'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi',             '', 'http://oss-cn-beijing.aliyuncs.com',   'N', '',            '1', '', 'admin', sysdate(), 'admin', sysdate(), NULL);
insert into sys_oss_config values (4, 'qcloud', 'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi-1250000000',  '', 'http://cos.ap-beijing.myqcloud.com',   'N', 'ap-beijing',  '1', '', 'admin', sysdate(), 'admin', sysdate(), NULL);
