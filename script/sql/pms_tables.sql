-- This script creates the core PMS tables based on PMS数据模型.md v5.4
-- and inserts necessary basic data like default channels and payment methods.
SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
-- Table structure for pms_customer_contacts
-- ----------------------------
DROP TABLE IF EXISTS `pms_customer_contacts`;
CREATE TABLE `pms_customer_contacts` (
    `contact_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '联系人唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `contact_type` varchar(50) DEFAULT NULL COMMENT '联系人类型',
    `full_name` varchar(255) DEFAULT NULL COMMENT '联系人姓名',
    `phone_number` varchar(50) DEFAULT NULL COMMENT '主要联系电话',
    `email` varchar(255) DEFAULT NULL COMMENT '电子邮件地址',
    `wechat_openid` varchar(128) DEFAULT NULL COMMENT '微信OpenID',
    `wechat_unionid` varchar(128) DEFAULT NULL COMMENT '微信UnionID',
    `gender` varchar(50) DEFAULT NULL COMMENT '性别',
    `date_of_birth` date DEFAULT NULL COMMENT '出生日期',
    `id_type` varchar(50) DEFAULT NULL COMMENT '证件类型',
    `id_number_encrypted` varchar(255) DEFAULT NULL COMMENT '证件号码 (加密存储)',
    `nationality_country_code` varchar(10) DEFAULT NULL COMMENT '国籍代码',
    `address_province` varchar(100) DEFAULT NULL COMMENT '地址-省份',
    `address_city` varchar(100) DEFAULT NULL COMMENT '地址-城市',
    `address_district` varchar(100) DEFAULT NULL COMMENT '地址-区县',
    `address_detail` varchar(500) DEFAULT NULL COMMENT '地址-详细地址',
    `postal_code` varchar(20) DEFAULT NULL COMMENT '邮政编码',
    `contact_status` varchar(50) DEFAULT NULL COMMENT '联系人状态',
    `member_level` varchar(50) DEFAULT NULL COMMENT '会员等级',
    `total_stays` int(11) DEFAULT '0' COMMENT '总入住次数',
    `total_amount` decimal(10, 2) DEFAULT '0.00' COMMENT '总消费金额',
    `last_stay_date` date DEFAULT NULL COMMENT '最后入住日期',
    `remarks` text COMMENT '备注信息',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建记录的操作员所属部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`contact_id`),
    KEY `idx_pms_cc_tenant` (`tenant_id`),
    KEY `idx_pms_cc_tenant_name` (`tenant_id`, `full_name`),
    KEY `idx_pms_cc_tenant_phone` (`tenant_id`, `phone_number`),
    KEY `idx_pms_cc_tenant_status_type` (`tenant_id`, `contact_status`, `contact_type`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '客户联系人表';
-- ----------------------------
-- Table structure for pms_contact_tags
-- ----------------------------
DROP TABLE IF EXISTS `pms_contact_tags`;
CREATE TABLE `pms_contact_tags` (
    `tag_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '标签唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店, 可空)',
    `name` varchar(100) DEFAULT NULL COMMENT '标签名称',
    `color` varchar(20) DEFAULT NULL COMMENT '标签显示颜色',
    `category` varchar(50) DEFAULT NULL COMMENT '标签分类',
    `description` varchar(500) DEFAULT NULL COMMENT '标签描述',
    `is_system` tinyint(1) DEFAULT '0' COMMENT '是否为系统预设标签',
    `sort_order` int(11) DEFAULT '0' COMMENT '排序值',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`tag_id`),
    KEY `idx_pms_ct_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_pms_ct_tenant_dept_name_category` (`tenant_id`, `dept_id`, `name`, `category`),
    KEY `idx_pms_ct_category` (`category`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '联系人标签表';
-- ----------------------------
-- Table structure for pms_contact_tag_relations
-- ----------------------------
DROP TABLE IF EXISTS `pms_contact_tag_relations`;
CREATE TABLE `pms_contact_tag_relations` (
    `relation_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `contact_id` bigint(20) NOT NULL COMMENT '联系人ID',
    `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `unq_pms_ctr_tenant_contact_tag` (`tenant_id`, `contact_id`, `tag_id`, `del_flag`),
    KEY `idx_pms_ctr_tenant_contact` (`tenant_id`, `contact_id`),
    KEY `idx_pms_ctr_tenant_tag` (`tenant_id`, `tag_id`),
    KEY `idx_pms_ctr_contact_id` (`contact_id`),
    KEY `idx_pms_ctr_tag_id` (`tag_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '联系人标签关联表';
-- ----------------------------
-- Table structure for pms_room_types
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_types`;
CREATE TABLE `pms_room_types` (
    `room_type_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '房型唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `name` varchar(255) NOT NULL COMMENT '房型名称',
    `default_price` decimal(10, 2) DEFAULT NULL COMMENT '房型默认价格',
    `capacity` int(11) DEFAULT NULL COMMENT '标准入住人数',
    `amenities` json DEFAULT NULL COMMENT '房型设施标签',
    `status` varchar(50) DEFAULT NULL COMMENT '状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`room_type_id`),
    KEY `idx_pms_rt_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_pms_rt_tenant_dept_name` (`tenant_id`, `dept_id`, `name`),
    KEY `idx_pms_rt_tenant_dept_status` (`tenant_id`, `dept_id`, `status`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '房型表';
-- ----------------------------
-- Table structure for pms_room_rooms
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_rooms`;
CREATE TABLE `pms_room_rooms` (
    `room_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '房间唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_type_id` bigint(20) DEFAULT NULL COMMENT '所属房型ID',
    `room_number` varchar(50) NOT NULL COMMENT '房间号',
    `floor` varchar(50) DEFAULT NULL COMMENT '楼层',
    `room_status` varchar(50) DEFAULT NULL COMMENT '物理状态',
    `cleaning_status` varchar(50) DEFAULT NULL COMMENT '清洁状态',
    `status` varchar(50) DEFAULT NULL COMMENT '记录状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`room_id`),
    UNIQUE KEY `unq_pms_r_tenant_dept_room_number` (
        `tenant_id`,
        `dept_id`,
        `room_number`,
        `del_flag`
    ),
    KEY `idx_pms_r_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_pms_r_tenant_dept_rt` (`tenant_id`, `dept_id`, `room_type_id`),
    KEY `idx_pms_r_tenant_dept_status` (
        `tenant_id`,
        `dept_id`,
        `room_status`,
        `cleaning_status`
    ),
    KEY `idx_pms_r_tenant_dept_status_date` (
        `tenant_id`,
        `dept_id`,
        `room_status`,
        `cleaning_status`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '房间表';
-- ----------------------------
-- Table structure for pms_room_locks
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_locks`;
CREATE TABLE `pms_room_locks` (
    `lock_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '锁定唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_id` bigint(20) DEFAULT NULL COMMENT '被锁定的房间ID',
    `start_datetime` datetime DEFAULT NULL COMMENT '锁定开始时间',
    `end_datetime` datetime DEFAULT NULL COMMENT '锁定结束时间',
    `lock_type` varchar(50) DEFAULT NULL COMMENT '锁定类型',
    `reason` text COMMENT '锁定原因',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`lock_id`),
    KEY `idx_pms_rl_tenant_dept_room` (`tenant_id`, `dept_id`, `room_id`),
    KEY `idx_pms_rl_tenant_dept_time` (
        `tenant_id`,
        `dept_id`,
        `start_datetime`,
        `end_datetime`
    ),
    KEY `idx_pms_rl_tenant_dept_type` (`tenant_id`, `dept_id`, `lock_type`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '房间锁定表';
-- ----------------------------
-- Table structure for pms_room_pricing_rules
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_pricing_rules`;
CREATE TABLE `pms_room_pricing_rules` (
    `rule_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '价格规则唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，可空)',
    `name` varchar(100) DEFAULT NULL COMMENT '规则名称',
    `room_type_id` bigint(20) DEFAULT NULL COMMENT '房型ID (可空)',
    `date_range_start` date DEFAULT NULL COMMENT '适用开始日期',
    `date_range_end` date DEFAULT NULL COMMENT '适用结束日期',
    `days_of_week_json` json DEFAULT NULL COMMENT '适用星期 (JSON, e.g., [1,2,7])',
    `min_length_of_stay` int(11) DEFAULT NULL COMMENT '最小入住天数',
    `max_length_of_stay` int(11) DEFAULT NULL COMMENT '最大入住天数',
    `price_adjustment_type` varchar(50) DEFAULT NULL COMMENT '调整类型',
    `adjustment_value` decimal(10, 2) DEFAULT NULL COMMENT '调整值',
    `priority` int(11) DEFAULT '0' COMMENT '规则优先级',
    `status` varchar(50) DEFAULT NULL COMMENT '状态',
    `channel_restrictions_json` json DEFAULT NULL COMMENT '渠道限制(JSON数组)',
    `guest_count_min` int(11) DEFAULT NULL COMMENT '最小客人数',
    `guest_count_max` int(11) DEFAULT NULL COMMENT '最大客人数',
    `advance_booking_days_min` int(11) DEFAULT NULL COMMENT '最小提前预订天数',
    `advance_booking_days_max` int(11) DEFAULT NULL COMMENT '最大提前预订天数',
    `member_level_restrictions_json` json DEFAULT NULL COMMENT '会员等级限制(JSON数组)',
    `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
    `is_combinable` tinyint(1) DEFAULT '1' COMMENT '是否可与其他规则叠加',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`rule_id`),
    KEY `idx_pms_rpr_tenant_dept_status_priority` (`tenant_id`, `dept_id`, `status`, `priority`),
    KEY `idx_pms_rpr_tenant_dept_rt_dates` (
        `tenant_id`,
        `dept_id`,
        `room_type_id`,
        `date_range_start`,
        `date_range_end`
    ),
    KEY `idx_pms_rpr_tenant_dept_combinable` (
        `tenant_id`,
        `dept_id`,
        `is_combinable`,
        `status`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '房间价格规则表';
-- ----------------------------
-- Table structure for pms_core_orders
-- ----------------------------
DROP TABLE IF EXISTS `pms_core_orders`;
CREATE TABLE `pms_core_orders` (
    `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `contact_id` bigint(20) DEFAULT NULL COMMENT '联系人ID',
    `pms_room_id` bigint(20) DEFAULT NULL COMMENT '分配的房间ID',
    `room_type_id` bigint(20) DEFAULT NULL COMMENT '预订的房型ID',
    `channel_id` bigint(20) DEFAULT NULL COMMENT '订单来源渠道ID',
    `check_in_date` date DEFAULT NULL COMMENT '计划入住日期',
    `check_out_date` date DEFAULT NULL COMMENT '计划离店日期',
    `num_adults` int(11) DEFAULT NULL COMMENT '成人数',
    `num_children` int(11) DEFAULT NULL COMMENT '儿童数',
    `total_amount` decimal(10, 2) DEFAULT NULL COMMENT '订单总金额',
    `paid_amount` decimal(10, 2) DEFAULT '0.00' COMMENT '已付金额',
    `order_status` varchar(50) DEFAULT NULL COMMENT '订单状态',
    `order_source` varchar(50) DEFAULT NULL COMMENT '订单来源',
    `notes` text COMMENT '订单备注',
    `actual_check_in_time` datetime DEFAULT NULL COMMENT '实际入住时间',
    `actual_check_out_time` datetime DEFAULT NULL COMMENT '实际退房时间',
    `confirmed_time` datetime DEFAULT NULL COMMENT '订单确认时间',
    `cancelled_time` datetime DEFAULT NULL COMMENT '订单取消时间',
    `expected_arrival_time` time DEFAULT NULL COMMENT '预计到达时间',
    `special_requests` text COMMENT '特殊要求',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`order_id`),
    KEY `idx_pms_o_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_pms_o_tenant_dept_status` (`tenant_id`, `dept_id`, `order_status`),
    KEY `idx_pms_o_tenant_dept_dates` (
        `tenant_id`,
        `dept_id`,
        `check_in_date`,
        `check_out_date`
    ),
    KEY `idx_pms_o_tenant_dept_contact` (`tenant_id`, `dept_id`, `contact_id`),
    KEY `idx_pms_o_tenant_dept_room_dates` (
        `tenant_id`,
        `dept_id`,
        `pms_room_id`,
        `check_in_date`,
        `check_out_date`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '核心订单表';
-- ----------------------------
-- Table structure for pms_core_order_items
-- ----------------------------
DROP TABLE IF EXISTS `pms_core_order_items`;
CREATE TABLE `pms_core_order_items` (
    `order_item_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单项唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `order_id` bigint(20) NOT NULL COMMENT '所属订单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `pms_room_id` bigint(20) DEFAULT NULL COMMENT '关联房间ID',
    `product_id` bigint(20) DEFAULT NULL COMMENT '产品ID',
    `product_type` varchar(50) DEFAULT NULL COMMENT '产品类型',
    `description` varchar(500) DEFAULT NULL COMMENT '项目描述',
    `quantity` int(11) DEFAULT NULL COMMENT '数量',
    `unit_price` decimal(10, 2) DEFAULT NULL COMMENT '单价',
    `service_date` date DEFAULT NULL COMMENT '服务日期',
    `notes` text COMMENT '项目备注',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`order_item_id`),
    KEY `idx_pms_oi_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_pms_oi_tenant_dept_product` (
        `tenant_id`,
        `dept_id`,
        `product_type`,
        `product_id`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '订单项目表';
-- ----------------------------
-- Table structure for pms_core_channels
-- ----------------------------
DROP TABLE IF EXISTS `pms_core_channels`;
CREATE TABLE `pms_core_channels` (
    `channel_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '渠道唯一ID',
    `tenant_id` varchar(20) DEFAULT NULL COMMENT '租户ID (可空)',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，可空)',
    `name` varchar(100) DEFAULT NULL COMMENT '渠道名称',
    `channel_type` varchar(50) DEFAULT NULL COMMENT '渠道类型',
    `description` varchar(500) DEFAULT NULL COMMENT '渠道描述',
    `status` varchar(50) DEFAULT NULL COMMENT '状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`channel_id`),
    KEY `idx_pms_ch_tenant_dept_status` (`tenant_id`, `dept_id`, `status`),
    KEY `idx_pms_ch_tenant_dept_type` (`tenant_id`, `dept_id`, `channel_type`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '订单渠道表';
-- ----------------------------
-- Records for pms_core_channels (Basic Data based on Enum)
-- These are examples, adjust tenant_id/dept_id as needed or handle in service layer
-- ----------------------------
INSERT INTO `pms_core_channels`
VALUES (
        1,
        NULL,
        NULL,
        '现场步入',
        'direct_booking',
        '客人直接到店预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        2,
        NULL,
        NULL,
        '电话预订',
        'direct_booking',
        '通过电话联系民宿预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        3,
        NULL,
        NULL,
        '官网预订',
        'direct_booking',
        '通过民宿官方网站预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        4,
        NULL,
        NULL,
        'H5商城',
        'direct_booking',
        '通过民宿H5商城预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        5,
        NULL,
        NULL,
        'OTA渠道管理',
        'ota',
        '通过第三方OTA平台（如携程、飞猪）渠道管理器同步的订单',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        6,
        NULL,
        NULL,
        '旅行社',
        'travel_agency',
        '通过合作旅行社预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        7,
        NULL,
        NULL,
        '企业协议',
        'corporate',
        '通过企业协议预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        8,
        NULL,
        NULL,
        '内部预订',
        'internal_use',
        '员工或内部人员预订',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_core_channels`
VALUES (
        9,
        NULL,
        NULL,
        '其他渠道',
        'other',
        '其他未列明的渠道',
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
-- ----------------------------
-- Table structure for pms_finance_folios
-- ----------------------------
DROP TABLE IF EXISTS `pms_finance_folios`;
CREATE TABLE `pms_finance_folios` (
    `folio_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '账单唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID',
    `total_charges` decimal(10, 2) DEFAULT '0.00' COMMENT '总应收费用',
    `total_payments` decimal(10, 2) DEFAULT '0.00' COMMENT '总已收付款',
    `total_refunds` decimal(10, 2) DEFAULT '0.00' COMMENT '总已退款',
    `folio_status` varchar(50) DEFAULT NULL COMMENT '账单状态',
    `notes` text COMMENT '账单备注',
    `closed_at` datetime DEFAULT NULL COMMENT '账单关闭时间',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`folio_id`),
    UNIQUE KEY `unq_pms_ff_order` (`order_id`) COMMENT '订单与账单一对一',
    KEY `idx_pms_ff_tenant_dept_order` (`tenant_id`, `dept_id`, `order_id`),
    KEY `idx_pms_ff_tenant_dept_status` (`tenant_id`, `dept_id`, `folio_status`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '客户账单表';
-- ----------------------------
-- Table structure for pms_finance_transactions
-- ----------------------------
DROP TABLE IF EXISTS `pms_finance_transactions`;
CREATE TABLE `pms_finance_transactions` (
    `transaction_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '交易唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `folio_id` bigint(20) NOT NULL COMMENT '所属账单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `transaction_type` varchar(50) DEFAULT NULL COMMENT '交易类型',
    `amount` decimal(10, 2) DEFAULT NULL COMMENT '交易金额',
    `description` varchar(500) DEFAULT NULL COMMENT '交易描述',
    `payment_method_id` bigint(20) DEFAULT NULL COMMENT '支付方式ID',
    `payment_gateway_txn_id` varchar(128) DEFAULT NULL COMMENT '支付网关交易号',
    `transaction_time` datetime DEFAULT NULL COMMENT '交易时间',
    `related_order_item_id` bigint(20) DEFAULT NULL COMMENT '关联订单项ID',
    `notes` text COMMENT '交易备注',
    `is_void` tinyint(1) DEFAULT '0' COMMENT '是否已作废',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`transaction_id`),
    KEY `idx_pms_ft_tenant_folio` (`tenant_id`, `folio_id`),
    KEY `idx_pms_ft_tenant_dept_type_time` (
        `tenant_id`,
        `dept_id`,
        `transaction_type`,
        `transaction_time`
    )
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '财务交易流水表';
-- ----------------------------
-- Table structure for pms_finance_payment_methods
-- ----------------------------
DROP TABLE IF EXISTS `pms_finance_payment_methods`;
CREATE TABLE `pms_finance_payment_methods` (
    `payment_method_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '支付方式唯一ID',
    `tenant_id` varchar(20) DEFAULT NULL COMMENT '租户ID (可空)',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，可空)',
    `name` varchar(100) DEFAULT NULL COMMENT '支付方式名称',
    `method_type` varchar(50) DEFAULT NULL COMMENT '支付方式类型',
    `details_json` json DEFAULT NULL COMMENT '附加配置',
    `status` varchar(50) DEFAULT NULL COMMENT '状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`payment_method_id`),
    KEY `idx_pms_fpm_tenant_dept_status` (`tenant_id`, `dept_id`, `status`),
    KEY `idx_pms_fpm_tenant_dept_type` (`tenant_id`, `dept_id`, `method_type`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COMMENT = '支付方式表';
-- ----------------------------
-- Records for pms_finance_payment_methods (Basic Data based on Enum)
-- These are examples, adjust tenant_id/dept_id as needed or handle in service layer
-- ----------------------------
INSERT INTO `pms_finance_payment_methods`
VALUES (
        1,
        NULL,
        NULL,
        '现金',
        'cash',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        2,
        NULL,
        NULL,
        '支付宝',
        'alipay',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        3,
        NULL,
        NULL,
        '微信支付',
        'wechat_pay',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        4,
        NULL,
        NULL,
        '银行卡',
        'credit_card_unionpay',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        5,
        NULL,
        NULL,
        '银行转账',
        'bank_transfer',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        6,
        NULL,
        NULL,
        '公司挂账',
        'company_account',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        7,
        NULL,
        NULL,
        '代金券',
        'voucher',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        8,
        NULL,
        NULL,
        '积分兑换',
        'points_redemption',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
INSERT INTO `pms_finance_payment_methods`
VALUES (
        9,
        NULL,
        NULL,
        '其他支付',
        'other',
        NULL,
        'active',
        NULL,
        NULL,
        NOW(),
        NULL,
        NULL,
        '0'
    );
-- ----------------------------
-- Table structure for pms_finance_extra_charge_items
-- ----------------------------
DROP TABLE IF EXISTS `pms_finance_extra_charge_items`;
CREATE TABLE `pms_finance_extra_charge_items` (
    `item_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '附加费用项唯一ID',
    `tenant_id` varchar(20) DEFAULT NULL COMMENT '租户ID (可空)',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，可空)',
    `name` varchar(100) DEFAULT NULL COMMENT '项目名称',
    `default_price` decimal(10, 2) DEFAULT NULL COMMENT '默认单价',
    `category` varchar(50) DEFAULT NULL COMMENT '费用类别',
    `is_taxable` tinyint(1) DEFAULT '0' COMMENT '是否应税',
    `description` varchar(500) DEFAULT NULL COMMENT '项目描述',
    `status` varchar(50) DEFAULT NULL COMMENT '状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`item_id`),
    KEY `idx_pms_feci_tenant_dept_status` (`tenant_id`, `dept_id`, `status`),
    KEY `idx_pms_feci_tenant_dept_category` (`tenant_id`, `dept_id`, `category`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '附加费用项目表';
-- ----------------------------
-- Table structure for pms_suppliers
-- ----------------------------
DROP TABLE IF EXISTS `pms_suppliers`;
CREATE TABLE `pms_suppliers` (
    `supplier_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '供应商唯一ID',
    `tenant_id` varchar(20) DEFAULT NULL COMMENT '租户ID (可空，NULL表示全局供应商)',
    `supplier_type` varchar(50) DEFAULT NULL COMMENT '供应商类型',
    `company_name` varchar(255) DEFAULT NULL COMMENT '公司名称',
    `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人姓名',
    `phone_number` varchar(50) DEFAULT NULL COMMENT '联系电话',
    `email` varchar(255) DEFAULT NULL COMMENT '邮箱地址',
    `business_scope` text COMMENT '业务范围描述',
    `service_area` varchar(255) DEFAULT NULL COMMENT '服务区域',
    `contract_info` text COMMENT '合同信息',
    `status` varchar(50) DEFAULT NULL COMMENT '供应商状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`supplier_id`),
    KEY `idx_pms_s_tenant_type` (`tenant_id`, `supplier_type`),
    KEY `idx_pms_s_tenant_name` (`tenant_id`, `company_name`),
    KEY `idx_pms_s_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '供应商表';
-- ----------------------------
-- Table structure for pms_partners
-- ----------------------------
DROP TABLE IF EXISTS `pms_partners`;
CREATE TABLE `pms_partners` (
    `partner_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '合作伙伴唯一ID',
    `tenant_id` varchar(20) DEFAULT NULL COMMENT '租户ID (可空，NULL表示全局合作伙伴)',
    `partner_type` varchar(50) DEFAULT NULL COMMENT '合作伙伴类型',
    `company_name` varchar(255) DEFAULT NULL COMMENT '公司名称',
    `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人姓名',
    `phone_number` varchar(50) DEFAULT NULL COMMENT '联系电话',
    `email` varchar(255) DEFAULT NULL COMMENT '邮箱地址',
    `website_url` varchar(255) DEFAULT NULL COMMENT '网站地址',
    `commission_rate` decimal(5, 2) DEFAULT NULL COMMENT '佣金比例',
    `payment_terms` varchar(255) DEFAULT NULL COMMENT '付款条件',
    `cooperation_level` varchar(50) DEFAULT NULL COMMENT '合作等级',
    `contract_start_date` date DEFAULT NULL COMMENT '合同开始日期',
    `contract_end_date` date DEFAULT NULL COMMENT '合同结束日期',
    `status` varchar(50) DEFAULT NULL COMMENT '合作伙伴状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`partner_id`),
    KEY `idx_pms_p_tenant_type` (`tenant_id`, `partner_type`),
    KEY `idx_pms_p_tenant_name` (`tenant_id`, `company_name`),
    KEY `idx_pms_p_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '合作伙伴表';
-- ----------------------------
-- Table structure for pms_tenant_settings
-- ----------------------------
DROP TABLE IF EXISTS `pms_tenant_settings`;
CREATE TABLE `pms_tenant_settings` (
    `setting_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '设置唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，NULL=租户级)',
    `setting_group` varchar(100) DEFAULT NULL COMMENT '设置分组',
    `setting_key` varchar(100) DEFAULT NULL COMMENT '设置键名',
    `setting_value` text COMMENT '设置值',
    `value_type` varchar(50) DEFAULT NULL COMMENT '值类型',
    `description` varchar(500) DEFAULT NULL COMMENT '设置描述',
    `is_sensitive` tinyint(1) DEFAULT '0' COMMENT '是否敏感设置',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`setting_id`),
    UNIQUE KEY `unq_pms_ts_tenant_dept_group_key` (
        `tenant_id`,
        `dept_id`,
        `setting_group`,
        `setting_key`,
        `del_flag`
    ),
    KEY `idx_pms_ts_tenant_dept_group` (`tenant_id`, `dept_id`, `setting_group`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '租户/门店配置表';
-- ----------------------------
-- Table structure for pms_mp_settings
-- ----------------------------
DROP TABLE IF EXISTS `pms_mp_settings`;
CREATE TABLE `pms_mp_settings` (
    `setting_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '小程序设置唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，NULL=租户级)',
    `setting_key` varchar(100) DEFAULT NULL COMMENT '设置键名',
    `setting_value` text COMMENT '设置值',
    `value_type` varchar(50) DEFAULT NULL COMMENT '值类型',
    `description` varchar(500) DEFAULT NULL COMMENT '设置描述',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`setting_id`),
    UNIQUE KEY `unq_pms_mps_tenant_dept_key` (
        `tenant_id`,
        `dept_id`,
        `setting_key`,
        `del_flag`
    ),
    KEY `idx_pms_mps_tenant_dept` (`tenant_id`, `dept_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '小程序配置表';
-- ----------------------------
-- Table structure for pms_tenant_user_devices
-- ----------------------------
DROP TABLE IF EXISTS `pms_tenant_user_devices`;
CREATE TABLE `pms_tenant_user_devices` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '设备记录唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
    `device_token` varchar(255) DEFAULT NULL COMMENT '设备令牌',
    `app_version` varchar(50) DEFAULT NULL COMMENT '应用版本',
    `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
    `status` varchar(50) DEFAULT NULL COMMENT '状态',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_pms_tud_tenant_user_device` (`tenant_id`, `user_id`, `device_type`),
    KEY `idx_pms_tud_tenant_user_status` (`tenant_id`, `user_id`, `status`),
    KEY `idx_pms_tud_device_token` (`device_token`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '用户设备表';
-- ----------------------------
-- Table structure for pms_order_status_history
-- ----------------------------
DROP TABLE IF EXISTS `pms_order_status_history`;
CREATE TABLE `pms_order_status_history` (
    `history_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '历史记录唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `order_id` bigint(20) NOT NULL COMMENT '订单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `from_status` varchar(50) DEFAULT NULL COMMENT '原状态',
    `to_status` varchar(50) NOT NULL COMMENT '新状态',
    `change_reason` varchar(500) DEFAULT NULL COMMENT '状态变更原因',
    `change_time` datetime NOT NULL COMMENT '变更时间',
    `change_by` bigint(20) DEFAULT NULL COMMENT '操作人',
    `notes` text COMMENT '备注',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`history_id`),
    KEY `idx_pms_osh_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_pms_osh_tenant_dept_time` (`tenant_id`, `dept_id`, `change_time`),
    KEY `idx_pms_osh_order_time` (`order_id`, `change_time`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '订单状态变更历史表';
-- ----------------------------
-- Table structure for pms_order_guests
-- ----------------------------
DROP TABLE IF EXISTS `pms_order_guests`;
CREATE TABLE `pms_order_guests` (
    `guest_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '客人记录唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `order_id` bigint(20) NOT NULL COMMENT '订单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `contact_id` bigint(20) DEFAULT NULL COMMENT '关联客户联系人ID(可空)',
    `guest_type` varchar(50) NOT NULL COMMENT '客人类型(primary/additional)',
    `is_primary_contact` tinyint(1) DEFAULT '0' COMMENT '是否为订单主联系人',
    `full_name` varchar(255) NOT NULL COMMENT '客人姓名',
    `id_type` varchar(50) DEFAULT NULL COMMENT '证件类型',
    `id_number_encrypted` varchar(255) DEFAULT NULL COMMENT '证件号码(加密)',
    `phone_number` varchar(50) DEFAULT NULL COMMENT '联系电话',
    `age_group` varchar(50) DEFAULT NULL COMMENT '年龄组(adult/child/infant)',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`guest_id`),
    KEY `idx_pms_og_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_pms_og_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_pms_og_order_guest_type` (`order_id`, `guest_type`),
    KEY `idx_pms_og_tenant_contact` (`tenant_id`, `contact_id`),
    KEY `idx_pms_og_order_primary` (`order_id`, `is_primary_contact`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '订单入住客人表';
-- ----------------------------
-- Table structure for pms_pricing_calculations
-- ----------------------------
DROP TABLE IF EXISTS `pms_pricing_calculations`;
CREATE TABLE `pms_pricing_calculations` (
    `calculation_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '计算记录唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_type_id` bigint(20) NOT NULL COMMENT '房型ID',
    `check_in_date` date NOT NULL COMMENT '入住日期',
    `check_out_date` date NOT NULL COMMENT '离店日期',
    `base_price` decimal(10, 2) NOT NULL COMMENT '基础价格',
    `final_price` decimal(10, 2) NOT NULL COMMENT '最终价格',
    `applied_rules_json` json DEFAULT NULL COMMENT '应用的规则详情',
    `calculation_time` datetime NOT NULL COMMENT '计算时间',
    `channel_id` bigint(20) DEFAULT NULL COMMENT '渠道ID',
    `guest_count` int(11) DEFAULT NULL COMMENT '客人数量',
    `advance_booking_days` int(11) DEFAULT NULL COMMENT '提前预订天数',
    `calculation_context` json DEFAULT NULL COMMENT '计算上下文',
    PRIMARY KEY (`calculation_id`),
    KEY `idx_pms_pc_tenant_dept_rt_dates` (
        `tenant_id`,
        `dept_id`,
        `room_type_id`,
        `check_in_date`,
        `check_out_date`
    ),
    KEY `idx_pms_pc_calculation_time` (`calculation_time`),
    KEY `idx_pms_pc_tenant_dept_channel` (`tenant_id`, `dept_id`, `channel_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '价格计算历史表';
-- ----------------------------
-- Table structure for pms_special_date_pricing
-- ----------------------------
DROP TABLE IF EXISTS `pms_special_date_pricing`;
CREATE TABLE `pms_special_date_pricing` (
    `special_date_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '特殊日期唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店，可空)',
    `room_type_id` bigint(20) DEFAULT NULL COMMENT '房型ID (可空表示全部房型)',
    `special_date` date NOT NULL COMMENT '特殊日期',
    `date_type` varchar(50) NOT NULL COMMENT '日期类型',
    `date_name` varchar(100) NOT NULL COMMENT '日期名称',
    `price_adjustment_type` varchar(50) NOT NULL COMMENT '调整类型',
    `adjustment_value` decimal(10, 2) NOT NULL COMMENT '调整值',
    `priority` int(11) DEFAULT '100' COMMENT '优先级',
    `status` varchar(50) DEFAULT 'active' COMMENT '状态',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`special_date_id`),
    KEY `idx_pms_sdp_tenant_dept_date` (`tenant_id`, `dept_id`, `special_date`),
    KEY `idx_pms_sdp_tenant_dept_rt` (`tenant_id`, `dept_id`, `room_type_id`),
    KEY `idx_pms_sdp_tenant_dept_type_status` (`tenant_id`, `dept_id`, `date_type`, `status`),
    KEY `idx_pms_sdp_date_range` (`special_date`, `status`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '特殊日期价格表';
-- ----------------------------
-- Table structure for pms_room_inventory_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_inventory_snapshot`;
CREATE TABLE `pms_room_inventory_snapshot` (
    `snapshot_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '快照唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_type_id` bigint(20) NOT NULL COMMENT '房型ID',
    `snapshot_date` date NOT NULL COMMENT '快照日期',
    `total_rooms` int(11) NOT NULL COMMENT '总房间数',
    `available_rooms` int(11) NOT NULL COMMENT '可用房间数',
    `occupied_rooms` int(11) NOT NULL COMMENT '已占用房间数',
    `maintenance_rooms` int(11) NOT NULL COMMENT '维护中房间数',
    `locked_rooms` int(11) NOT NULL COMMENT '锁定房间数',
    `dirty_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '脏房数量',
    `out_of_order_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '停用房间数',
    `reserved_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '预留房间数',
    `last_updated` datetime NOT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`snapshot_id`),
    UNIQUE KEY `unq_pms_ris_tenant_dept_rt_date` (
        `tenant_id`,
        `dept_id`,
        `room_type_id`,
        `snapshot_date`
    ),
    KEY `idx_pms_ris_tenant_dept_date` (`tenant_id`, `dept_id`, `snapshot_date`),
    KEY `idx_pms_ris_last_updated` (`last_updated`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = '房型库存快照表';
-- ----------------------------
-- Add Foreign Key Constraints
-- Note: Foreign key constraints are added after all tables are created
-- to avoid dependency issues. Ensure referenced tables exist.
-- ----------------------------
-- ALTER TABLE `pms_room_rooms` ADD CONSTRAINT `fk_pms_room_rooms_room_type_id` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_room_locks` ADD CONSTRAINT `fk_pms_room_locks_room_id` FOREIGN KEY (`room_id`) REFERENCES `pms_room_rooms` (`room_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_core_orders` ADD CONSTRAINT `fk_pms_core_orders_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `pms_customer_contacts` (`contact_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_core_orders` ADD CONSTRAINT `fk_pms_core_orders_room_type_id` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_core_orders` ADD CONSTRAINT `fk_pms_core_orders_pms_room_id` FOREIGN KEY (`pms_room_id`) REFERENCES `pms_room_rooms` (`room_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_core_orders` ADD CONSTRAINT `fk_pms_core_orders_channel_id` FOREIGN KEY (`channel_id`) REFERENCES `pms_core_channels` (`channel_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_core_order_items` ADD CONSTRAINT `fk_pms_order_items_order_id` FOREIGN KEY (`order_id`) REFERENCES `pms_core_orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_finance_folios` ADD CONSTRAINT `fk_pms_finance_folios_order_id` FOREIGN KEY (`order_id`) REFERENCES `pms_core_orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_finance_transactions` ADD CONSTRAINT `fk_pms_finance_transactions_folio_id` FOREIGN KEY (`folio_id`) REFERENCES `pms_finance_folios` (`folio_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_finance_transactions` ADD CONSTRAINT `fk_pms_finance_transactions_payment_method_id` FOREIGN KEY (`payment_method_id`) REFERENCES `pms_finance_payment_methods` (`payment_method_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_finance_transactions` ADD CONSTRAINT `fk_pms_finance_transactions_order_item_id` FOREIGN KEY (`related_order_item_id`) REFERENCES `pms_core_order_items` (`order_item_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_contact_tag_relations` ADD CONSTRAINT `fk_pms_ctr_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `pms_customer_contacts` (`contact_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_contact_tag_relations` ADD CONSTRAINT `fk_pms_ctr_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `pms_contact_tags` (`tag_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_tenant_user_devices` ADD CONSTRAINT `fk_pms_tud_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
-- ALTER TABLE `pms_tenant_settings` ADD CONSTRAINT `fk_pms_ts_dept_id` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`dept_id`) ON DELETE SET NULL ON UPDATE CASCADE;
-- ALTER TABLE `pms_mp_settings` ADD CONSTRAINT `fk_pms_mps_dept_id` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`dept_id`) ON DELETE SET NULL ON UPDATE CASCADE;
SET FOREIGN_KEY_CHECKS = 1;