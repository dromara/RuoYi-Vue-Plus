ALTER TABLE sys_oss_config ADD COLUMN access_policy char(1) NOT NULL DEFAULT 1 COMMENT '桶权限类型(0=private 1=public 2=custom)' AFTER region;
