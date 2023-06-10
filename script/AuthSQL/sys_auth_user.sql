CREATE TABLE `sys_auth_user` (
                                 `auth_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '授权ID',
                                 `uuid` varchar(500) NOT NULL COMMENT '第三方平台用户唯一ID',
                                 `user_id` bigint(20) unsigned NOT NULL COMMENT '系统用户ID',
                                 `user_name` varchar(30) NOT NULL COMMENT '登录账号',
                                 `nick_name` varchar(30) DEFAULT '' COMMENT '用户昵称',
                                 `avatar` varchar(500) DEFAULT '' COMMENT '头像地址',
                                 `email` varchar(255) DEFAULT '' COMMENT '用户邮箱',
                                 `source` varchar(255) DEFAULT '' COMMENT '用户来源',
                                 ` tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户id',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `tenant_id` varchar(25) NOT NULL DEFAULT '000000',
                                 PRIMARY KEY (`auth_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COMMENT='第三方平台授权用户信息表';
