ALTER TABLE "SYS_OSS_CONFIG" ADD ("DOMAIN" VARCHAR2(255));

COMMENT ON COLUMN "SYS_OSS_CONFIG"."DOMAIN" IS '自定义域名';

update sys_oss_config set endpoint = '127.0.0.1:9000' where oss_config_id = 1;
update sys_oss_config set endpoint = 's3-cn-north-1.qiniucs.com', region = '' where oss_config_id = 2;
update sys_oss_config set endpoint = 'oss-cn-beijing.aliyuncs.com' where oss_config_id = 3;
update sys_oss_config set endpoint = 'cos.ap-beijing.myqcloud.com' where oss_config_id = 4;

insert into sys_oss_config values (5, 'image', 'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', 'N', '', '1', '', NULL, 'admin', sysdate, 'admin', sysdate, '');

ALTER TABLE "GEN_TABLE_COLUMN" MODIFY ("TABLE_ID" NUMBER(20,0));
