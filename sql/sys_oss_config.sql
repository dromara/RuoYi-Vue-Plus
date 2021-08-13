/*
Navicat MySQL Data Transfer

Source Server         : mysql57
Source Server Version : 50735
Source Host           : localhost:3306
Source Database       : ry-vue

Target Server Type    : MYSQL
Target Server Version : 50735
File Encoding         : 65001

Date: 2021-08-12 09:01:33
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for sys_oss_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss_config`;
CREATE TABLE `sys_oss_config` (
  `oss_config_id` int(5) NOT NULL AUTO_INCREMENT COMMENT '主建',
  `config_key` varchar(255) NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) DEFAULT '' COMMENT 'access_key',
  `secret_key` varchar(255) DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) DEFAULT '' COMMENT '访问站点',
  `is_https` char(1) DEFAULT '0' COMMENT '是否htpps（0否 1是）',
  `region` varchar(255) DEFAULT '' COMMENT '域',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `ext1` varchar(255) DEFAULT '' COMMENT '扩展字段',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='云存储配置表';

-- ----------------------------
-- Records of sys_oss_config
-- ----------------------------
INSERT INTO `sys_oss_config` VALUES ('1', 'minio3', 'ruoyi', 'ruoyi123', 'ruoyi', '', 'http://localhost:9000', '0', '', '0', '', 'admin', '2021-08-11 21:29:37', 'admin', '2021-08-11 22:52:41', null);
