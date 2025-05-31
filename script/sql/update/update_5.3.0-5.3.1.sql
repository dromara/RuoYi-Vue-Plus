ALTER TABLE `flow_node` DROP COLUMN `skip_any_node`;
ALTER TABLE `flow_node`
    ADD COLUMN `ext` text NULL COMMENT '扩展属性' AFTER `update_time`;
ALTER TABLE `flow_user` ADD INDEX `user_associated`(`associated`) USING BTREE

ALTER TABLE `sys_oss`
    ADD COLUMN `ext1` text NULL COMMENT '扩展属性' AFTER `url`;
