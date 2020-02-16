/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.0.222
 Source Server Type    : MySQL
 Source Server Version : 80019
 Source Host           : 192.168.0.222:3306
 Source Schema         : ry-vue

 Target Server Type    : MySQL
 Target Server Version : 80019
 File Encoding         : 65001

 Date: 14/02/2020 13:29:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chkj_test
-- ----------------------------
DROP TABLE IF EXISTS `chkj_test`;
CREATE TABLE `chkj_test`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `test_key` varchar(255) NOT NULL DEFAULT '' COMMENT 'key键',
  `value` varchar(255) NOT NULL DEFAULT '' COMMENT '值',
  `version` int(0) NULL DEFAULT 0 COMMENT '版本',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint(0) NULL DEFAULT 0 COMMENT '删除状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 COMMENT = '测试表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------
INSERT INTO `gen_table` VALUES (1, 'chkj_test', '测试表', 'CsTest', 'crud', 'com.ruoyi.project.cstest', 'cstest', 'cstest', '测试', 'Lion Li', '{}', 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27', '测试代码生成器');

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------
INSERT INTO `gen_table_column` VALUES (1, '1', 'id', '主键', 'int', 'Integer', 'id', '1', '1', NULL, '1', NULL, NULL, NULL, 'EQ', 'input', '', 1, 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27');
INSERT INTO `gen_table_column` VALUES (2, '1', 'test_key', 'key键', 'varchar(255)', 'String', 'testKey', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 2, 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27');
INSERT INTO `gen_table_column` VALUES (3, '1', 'value', '值', 'varchar(255)', 'String', 'value', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27');
INSERT INTO `gen_table_column` VALUES (4, '1', 'version', '版本', 'int', 'Integer', 'version', '0', '0', '1', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 4, 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27');
INSERT INTO `gen_table_column` VALUES (5, '1', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', '1', NULL, NULL, '1', '1', 'EQ', 'datetime', '', 5, 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27');
INSERT INTO `gen_table_column` VALUES (6, '1', 'deleted', '删除状态', 'tinyint', 'Integer', 'deleted', '0', '0', '1', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 6, 'admin', '2020-02-12 03:39:31', '', '2020-02-14 04:55:27');

INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2000, '测试用例', 0, 5, '', NULL, 1, 'M', '0', NULL, 'bug', 'admin', '2020-02-12 03:57:28', '', NULL, '');
INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2007, '测试', 2000, 1, 'cstest', 'cstest/cstest/index', 1, 'C', '0', 'cstest:cstest:list', '#', 'admin', '2018-03-01 00:00:00', 'ry', '2018-03-01 00:00:00', '测试菜单');
INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2008, '测试查询', 2007, 1, '#', '', 1, 'F', '0', 'cstest:cstest:query', '#', 'admin', '2018-03-01 00:00:00', 'ry', '2018-03-01 00:00:00', '');
INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2009, '测试新增', 2007, 2, '#', '', 1, 'F', '0', 'cstest:cstest:add', '#', 'admin', '2018-03-01 00:00:00', 'ry', '2018-03-01 00:00:00', '');
INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2010, '测试修改', 2007, 3, '#', '', 1, 'F', '0', 'cstest:cstest:edit', '#', 'admin', '2018-03-01 00:00:00', 'ry', '2018-03-01 00:00:00', '');
INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2011, '测试删除', 2007, 4, '#', '', 1, 'F', '0', 'cstest:cstest:remove', '#', 'admin', '2018-03-01 00:00:00', 'ry', '2018-03-01 00:00:00', '');
INSERT INTO `sys_menu`(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2012, '测试导出', 2007, 5, '#', '', 1, 'F', '0', 'cstest:cstest:export', '#', 'admin', '2018-03-01 00:00:00', 'ry', '2018-03-01 00:00:00', '');

SET FOREIGN_KEY_CHECKS = 1;