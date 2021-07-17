CREATE TABLE `sys_oss`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT,
    `file_name`   varchar(64)  NOT NULL DEFAULT '' COMMENT '文件名',
    `file_suffix` varchar(10)  NOT NULL DEFAULT '' COMMENT '文件后缀名',
    `url`         varchar(200) NOT NULL COMMENT 'URL地址',
    `create_time` datetime              DEFAULT NULL COMMENT '创建时间',
    `create_by`   varchar(64)  NOT NULL DEFAULT '' COMMENT '上传人',
    `service`     tinyint(2)   NOT NULL DEFAULT '1' COMMENT '服务商',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  DEFAULT CHARSET = utf8 COMMENT ='文件上传';

INSERT INTO `ry`.`sys_menu` (`menu_name`, `parent_id`, `order_num`, `url`, `menu_type`, `visible`, `perms`, `icon`,
                             `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文件管理', '1', '10', '/system/oss', 'C', '0', 'system:oss:view', '#', 'admin', '2018-11-16 13:59:45', '', NULL,
        '');
INSERT INTO `ry`.`sys_menu` (`menu_name`, `parent_id`, `order_num`, `url`, `menu_type`, `visible`, `perms`, `icon`,
                             `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文件上传', '1056', '1', '#', 'F', '0', 'system:oss:add', '#', 'admin', '2018-11-16 13:59:45', '', NULL, '');
INSERT INTO `ry`.`sys_menu` (`menu_name`, `parent_id`, `order_num`, `url`, `menu_type`, `visible`, `perms`, `icon`,
                             `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文件删除', '1056', '2', '#', 'F', '0', 'system:oss:remove', '#', 'admin', '2018-11-16 13:59:45', '', NULL, '');
INSERT INTO `ry`.`sys_menu` (`menu_name`, `parent_id`, `order_num`, `url`, `menu_type`, `visible`, `perms`, `icon`,
                             `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文件配置', '1056', '3', '#', 'F', '0', 'system:oss:config', '#', 'admin', '2018-11-16 13:59:45', '', NULL, '');
INSERT INTO `ry`.`sys_menu` (`menu_name`, `parent_id`, `order_num`, `url`, `menu_type`, `visible`, `perms`, `icon`,
                             `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文件修改', '1056', '4', '#', 'F', '0', 'system:oss:remove', '#', 'admin', '2018-11-16 13:59:45', '', NULL, '');
