-- ========================================
-- PMS订单管理模块建表语句
-- 包含：订单核心表、订单增强表、库存管理表
-- 版本：v5.6 (对应PMS数据模型.md v5.6)
-- ========================================
SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;
-- ========================================
-- 1. 订单核心表
-- ========================================
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
    `num_adults` int(11) DEFAULT '1' COMMENT '成人数',
    `num_children` int(11) DEFAULT '0' COMMENT '儿童数',
    `total_amount` decimal(15, 2) DEFAULT '0.00' COMMENT '订单总金额',
    `paid_amount` decimal(15, 2) DEFAULT '0.00' COMMENT '已付金额',
    `order_status` varchar(50) NOT NULL DEFAULT 'pending_confirmation' COMMENT '订单状态',
    `order_source` varchar(50) DEFAULT NULL COMMENT '订单来源',
    `confirmation_time` datetime DEFAULT NULL COMMENT '确认时间',
    `actual_check_in_time` datetime DEFAULT NULL COMMENT '实际入住时间',
    `actual_check_out_time` datetime DEFAULT NULL COMMENT '实际退房时间',
    `cancellation_time` datetime DEFAULT NULL COMMENT '取消时间',
    `expected_arrival_time` time DEFAULT NULL COMMENT '预计到达时间',
    `special_requests` text COMMENT '特殊要求',
    `internal_notes` text COMMENT '内部备注',
    `cancellation_reason` varchar(500) DEFAULT NULL COMMENT '取消原因',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`order_id`),
    KEY `idx_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_contact_id` (`contact_id`),
    KEY `idx_room_type_id` (`room_type_id`),
    KEY `idx_pms_room_id` (`pms_room_id`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_check_in_date` (`check_in_date`),
    KEY `idx_check_out_date` (`check_out_date`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '核心订单表';
-- ----------------------------
-- Table structure for pms_core_order_items
-- ----------------------------
DROP TABLE IF EXISTS `pms_core_order_items`;
CREATE TABLE `pms_core_order_items` (
    `order_item_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `order_id` bigint(20) NOT NULL COMMENT '订单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `product_type` varchar(50) NOT NULL COMMENT '产品类型',
    `description` varchar(255) NOT NULL COMMENT '项目描述',
    `quantity` int(11) NOT NULL DEFAULT '1' COMMENT '数量',
    `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
    `total_price` decimal(12, 2) NOT NULL COMMENT '总价',
    `service_date` date DEFAULT NULL COMMENT '服务日期',
    `item_status` varchar(50) DEFAULT 'active' COMMENT '项目状态',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`order_item_id`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_product_type` (`product_type`),
    KEY `idx_service_date` (`service_date`),
    CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `pms_core_orders` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单项目表';
-- ----------------------------
-- Table structure for pms_core_channels
-- ----------------------------
DROP TABLE IF EXISTS `pms_core_channels`;
CREATE TABLE `pms_core_channels` (
    `channel_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '渠道ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店级渠道)',
    `channel_code` varchar(50) NOT NULL COMMENT '渠道代码',
    `channel_name` varchar(100) NOT NULL COMMENT '渠道名称',
    `channel_type` varchar(50) NOT NULL COMMENT '渠道类型',
    `commission_rate` decimal(5, 2) DEFAULT '0.00' COMMENT '佣金比例',
    `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人',
    `contact_phone` varchar(50) DEFAULT NULL COMMENT '联系电话',
    `contact_email` varchar(255) DEFAULT NULL COMMENT '联系邮箱',
    `api_endpoint` varchar(500) DEFAULT NULL COMMENT 'API接口地址',
    `api_credentials_json` text COMMENT 'API认证信息(JSON)',
    `channel_status` varchar(50) NOT NULL DEFAULT 'active' COMMENT '渠道状态',
    `sort_order` int(11) DEFAULT '0' COMMENT '排序',
    `description` varchar(500) DEFAULT NULL COMMENT '渠道描述',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`channel_id`),
    UNIQUE KEY `uk_tenant_channel_code` (`tenant_id`, `channel_code`),
    KEY `idx_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_channel_type` (`channel_type`),
    KEY `idx_channel_status` (`channel_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单来源渠道表';
-- ========================================
-- 2. 订单增强表
-- ========================================
-- ----------------------------
-- Table structure for pms_order_status_history
-- ----------------------------
DROP TABLE IF EXISTS `pms_order_status_history`;
CREATE TABLE `pms_order_status_history` (
    `history_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `order_id` bigint(20) NOT NULL COMMENT '订单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `from_status` varchar(50) DEFAULT NULL COMMENT '原状态',
    `to_status` varchar(50) NOT NULL COMMENT '新状态',
    `change_reason` varchar(50) DEFAULT NULL COMMENT '变更原因',
    `change_notes` varchar(500) DEFAULT NULL COMMENT '变更备注',
    `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
    `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
    `change_time` datetime NOT NULL COMMENT '变更时间',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`history_id`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_to_status` (`to_status`),
    KEY `idx_change_time` (`change_time`),
    KEY `idx_operator_id` (`operator_id`),
    CONSTRAINT `fk_order_history_order` FOREIGN KEY (`order_id`) REFERENCES `pms_core_orders` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单状态历史表';
-- ----------------------------
-- Table structure for pms_order_guests
-- ----------------------------
DROP TABLE IF EXISTS `pms_order_guests`;
CREATE TABLE `pms_order_guests` (
    `guest_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '客人ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `order_id` bigint(20) NOT NULL COMMENT '订单ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `contact_id` bigint(20) DEFAULT NULL COMMENT '关联联系人ID',
    `is_primary_contact` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为订单主联系人',
    `guest_type` varchar(50) NOT NULL DEFAULT 'adult' COMMENT '客人类型',
    `full_name` varchar(255) NOT NULL COMMENT '客人姓名',
    `gender` varchar(20) DEFAULT NULL COMMENT '性别',
    `age_group` varchar(50) DEFAULT NULL COMMENT '年龄组',
    `date_of_birth` date DEFAULT NULL COMMENT '出生日期',
    `id_type` varchar(50) DEFAULT NULL COMMENT '证件类型',
    `id_number_encrypted` varchar(500) DEFAULT NULL COMMENT '证件号码(加密)',
    `phone_number` varchar(50) DEFAULT NULL COMMENT '联系电话',
    `email` varchar(255) DEFAULT NULL COMMENT '邮箱地址',
    `nationality_country_code` varchar(10) DEFAULT NULL COMMENT '国籍代码',
    `special_needs` varchar(500) DEFAULT NULL COMMENT '特殊需求',
    `guest_notes` varchar(500) DEFAULT NULL COMMENT '客人备注',
    `check_in_time` datetime DEFAULT NULL COMMENT '入住时间',
    `check_out_time` datetime DEFAULT NULL COMMENT '退房时间',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`guest_id`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_contact_id` (`contact_id`),
    KEY `idx_guest_type` (`guest_type`),
    KEY `idx_is_primary_contact` (`is_primary_contact`),
    KEY `idx_full_name` (`full_name`),
    CONSTRAINT `fk_order_guests_order` FOREIGN KEY (`order_id`) REFERENCES `pms_core_orders` (`order_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_order_guests_contact` FOREIGN KEY (`contact_id`) REFERENCES `pms_customer_contacts` (`contact_id`) ON DELETE
    SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单客人信息表';
-- ========================================
-- 3. 库存管理表
-- ========================================
-- ----------------------------
-- Table structure for pms_room_inventory_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_inventory_snapshot`;
CREATE TABLE `pms_room_inventory_snapshot` (
    `snapshot_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_type_id` bigint(20) NOT NULL COMMENT '房型ID',
    `snapshot_date` date NOT NULL COMMENT '快照日期',
    `total_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '总房间数',
    `available_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '可用房间数',
    `occupied_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '已占用房间数',
    `dirty_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '脏房数量',
    `out_of_order_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '停用房间数',
    `reserved_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '预留房间数',
    `maintenance_rooms` int(11) NOT NULL DEFAULT '0' COMMENT '维护房间数',
    `confirmed_bookings` int(11) NOT NULL DEFAULT '0' COMMENT '已确认预订数',
    `pending_bookings` int(11) NOT NULL DEFAULT '0' COMMENT '待确认预订数',
    `occupancy_rate` decimal(5, 2) DEFAULT '0.00' COMMENT '入住率',
    `snapshot_time` datetime NOT NULL COMMENT '快照生成时间',
    `is_manual` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否手动生成',
    `notes` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`snapshot_id`),
    UNIQUE KEY `uk_tenant_dept_roomtype_date` (
        `tenant_id`,
        `dept_id`,
        `room_type_id`,
        `snapshot_date`
    ),
    KEY `idx_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_room_type_id` (`room_type_id`),
    KEY `idx_snapshot_date` (`snapshot_date`),
    KEY `idx_snapshot_time` (`snapshot_time`),
    CONSTRAINT `fk_inventory_snapshot_room_type` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '房间库存快照表';
-- ========================================
-- 4. 初始化基础数据
-- ========================================
-- 插入默认渠道数据
INSERT INTO `pms_core_channels` (
        `tenant_id`,
        `dept_id`,
        `channel_code`,
        `channel_name`,
        `channel_type`,
        `commission_rate`,
        `channel_status`,
        `sort_order`,
        `description`,
        `create_time`,
        `del_flag`
    )
VALUES (
        '000000',
        NULL,
        'DIRECT_WALK_IN',
        '现场步入',
        'direct_booking',
        0.00,
        'active',
        1,
        '客人直接到店预订',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'DIRECT_PHONE',
        '电话预订',
        'direct_booking',
        0.00,
        'active',
        2,
        '客人电话预订',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'DIRECT_WEBSITE',
        '官网预订',
        'direct_booking',
        0.00,
        'active',
        3,
        '官方网站预订',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'DIRECT_MALL_H5',
        'H5商城',
        'direct_booking',
        0.00,
        'active',
        4,
        'H5商城预订',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'OTA_CTRIP',
        '携程',
        'ota',
        15.00,
        'active',
        10,
        '携程OTA平台',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'OTA_FLIGGY',
        '飞猪',
        'ota',
        12.00,
        'active',
        11,
        '飞猪OTA平台',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'OTA_MEITUAN',
        '美团',
        'ota',
        18.00,
        'active',
        12,
        '美团OTA平台',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'TRAVEL_AGENCY',
        '旅行社',
        'wholesaler',
        10.00,
        'active',
        20,
        '旅行社渠道',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'CORPORATE',
        '企业协议',
        'corporate',
        5.00,
        'active',
        30,
        '企业协议客户',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        'INTERNAL',
        '内部预订',
        'internal_use',
        0.00,
        'active',
        99,
        '内部使用',
        NOW(),
        '0'
    );
SET FOREIGN_KEY_CHECKS = 1;
-- ========================================
-- 说明
-- ========================================
-- 1. 所有表都包含完整的多租户支持和审计字段
-- 2. 外键约束确保数据完整性
-- 3. 索引优化查询性能
-- 4. 初始化了基础的渠道数据
-- 5. 支持门店级数据隔离
-- 6. 客人信息表支持与联系人档案关联
-- 7. 库存快照表支持房态实时监控
-- ========================================