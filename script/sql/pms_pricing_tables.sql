-- ========================================
-- PMS价格管理模块建表语句
-- 包含：价格规则表、价格计算表、特殊日期价格表
-- 版本：v5.7 (修复MySQL 8.0+兼容性问题)
-- ========================================
SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;
-- ========================================
-- 1. 价格规则表
-- ========================================
-- ----------------------------
-- Table structure for pms_room_pricing_rules
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_pricing_rules`;
CREATE TABLE `pms_room_pricing_rules` (
    `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint DEFAULT NULL COMMENT '部门ID (门店)',
    `name` varchar(100) NOT NULL COMMENT '规则名称',
    `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
    `room_type_id` bigint DEFAULT NULL COMMENT '适用房型ID (NULL表示全部房型)',
    `date_range_start` date DEFAULT NULL COMMENT '适用日期范围开始',
    `date_range_end` date DEFAULT NULL COMMENT '适用日期范围结束',
    `days_of_week_json` json DEFAULT NULL COMMENT '适用星期 (JSON数组)',
    `price_adjustment_type` varchar(50) NOT NULL COMMENT '价格调整类型',
    `adjustment_value` decimal(10, 2) NOT NULL COMMENT '调整值',
    `min_length_of_stay` int DEFAULT NULL COMMENT '最小入住天数',
    `max_length_of_stay` int DEFAULT NULL COMMENT '最大入住天数',
    `advance_booking_days_min` int DEFAULT NULL COMMENT '最小提前预订天数',
    `advance_booking_days_max` int DEFAULT NULL COMMENT '最大提前预订天数',
    `channel_restrictions_json` json DEFAULT NULL COMMENT '渠道限制 (JSON数组)',
    `guest_count_min` int DEFAULT NULL COMMENT '最小客人数',
    `guest_count_max` int DEFAULT NULL COMMENT '最大客人数',
    `member_level_restrictions_json` json DEFAULT NULL COMMENT '会员等级限制 (JSON数组)',
    `priority` int NOT NULL DEFAULT '0' COMMENT '优先级 (数字越大优先级越高)',
    `status` varchar(50) NOT NULL DEFAULT 'active' COMMENT '规则状态',
    `effective_start_date` date DEFAULT NULL COMMENT '规则生效开始日期',
    `effective_end_date` date DEFAULT NULL COMMENT '规则生效结束日期',
    `max_discount_amount` decimal(10, 2) DEFAULT NULL COMMENT '最大折扣金额',
    `min_final_price` decimal(10, 2) DEFAULT NULL COMMENT '最低最终价格',
    `is_combinable` tinyint NOT NULL DEFAULT '1' COMMENT '是否可与其他规则组合',
    `usage_limit` int DEFAULT NULL COMMENT '使用次数限制',
    `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用次数',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`rule_id`),
    KEY `idx_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_room_type_id` (`room_type_id`),
    KEY `idx_date_range` (`date_range_start`, `date_range_end`),
    KEY `idx_priority` (`priority`),
    KEY `idx_status` (`status`),
    KEY `idx_effective_dates` (`effective_start_date`, `effective_end_date`),
    CONSTRAINT `fk_pricing_rules_room_type` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '房间价格规则表';
-- ========================================
-- 2. 价格计算历史表
-- ========================================
-- ----------------------------
-- Table structure for pms_pricing_calculations
-- ----------------------------
DROP TABLE IF EXISTS `pms_pricing_calculations`;
CREATE TABLE `pms_pricing_calculations` (
    `calculation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计算记录ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint NOT NULL COMMENT '部门ID (门店)',
    `room_type_id` bigint NOT NULL COMMENT '房型ID',
    `check_in_date` date NOT NULL COMMENT '入住日期',
    `check_out_date` date NOT NULL COMMENT '离店日期',
    `num_adults` int NOT NULL DEFAULT '1' COMMENT '成人数',
    `num_children` int NOT NULL DEFAULT '0' COMMENT '儿童数',
    `channel_code` varchar(50) DEFAULT NULL COMMENT '渠道代码',
    `member_level` varchar(50) DEFAULT NULL COMMENT '会员等级',
    `advance_booking_days` int DEFAULT NULL COMMENT '提前预订天数',
    `base_price` decimal(10, 2) NOT NULL COMMENT '基础价格',
    `final_price` decimal(10, 2) NOT NULL COMMENT '最终价格',
    `total_discount` decimal(10, 2) NOT NULL DEFAULT '0.00' COMMENT '总折扣金额',
    `applied_rules_json` json DEFAULT NULL COMMENT '应用的规则详情 (JSON)',
    `calculation_context_json` json DEFAULT NULL COMMENT '计算上下文 (JSON)',
    `calculation_time` datetime NOT NULL COMMENT '计算时间',
    `calculation_source` varchar(50) DEFAULT 'api' COMMENT '计算来源',
    `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
    `is_final_booking` tinyint NOT NULL DEFAULT '0' COMMENT '是否为最终预订',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`calculation_id`),
    KEY `idx_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_room_type_id` (`room_type_id`),
    KEY `idx_check_dates` (`check_in_date`, `check_out_date`),
    KEY `idx_calculation_time` (`calculation_time`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_channel_code` (`channel_code`),
    CONSTRAINT `fk_pricing_calculations_room_type` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pricing_calculations_order` FOREIGN KEY (`order_id`) REFERENCES `pms_core_orders` (`order_id`) ON DELETE
    SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '价格计算历史表';
-- ========================================
-- 3. 特殊日期价格表
-- ========================================
-- ----------------------------
-- Table structure for pms_special_date_pricing
-- ----------------------------
DROP TABLE IF EXISTS `pms_special_date_pricing`;
CREATE TABLE `pms_special_date_pricing` (
    `special_date_id` bigint NOT NULL AUTO_INCREMENT COMMENT '特殊价格ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint DEFAULT NULL COMMENT '部门ID (门店)',
    `name` varchar(100) NOT NULL COMMENT '特殊日期名称',
    `date_type` varchar(50) NOT NULL COMMENT '日期类型',
    `specific_date` date DEFAULT NULL COMMENT '具体日期',
    `date_range_start` date DEFAULT NULL COMMENT '日期范围开始',
    `date_range_end` date DEFAULT NULL COMMENT '日期范围结束',
    `room_type_id` bigint DEFAULT NULL COMMENT '适用房型ID (NULL表示全部房型)',
    `price_adjustment_type` varchar(50) NOT NULL COMMENT '价格调整类型',
    `adjustment_value` decimal(10, 2) NOT NULL COMMENT '调整值',
    `fixed_price` decimal(10, 2) DEFAULT NULL COMMENT '固定价格',
    `priority` int NOT NULL DEFAULT '100' COMMENT '优先级 (数字越大优先级越高)',
    `status` varchar(50) NOT NULL DEFAULT 'active' COMMENT '状态',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `is_recurring_yearly` tinyint NOT NULL DEFAULT '0' COMMENT '是否每年重复',
    `min_length_of_stay` int DEFAULT NULL COMMENT '最小入住天数',
    `max_length_of_stay` int DEFAULT NULL COMMENT '最大入住天数',
    `channel_restrictions_json` json DEFAULT NULL COMMENT '渠道限制 (JSON数组)',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`special_date_id`),
    KEY `idx_tenant_dept` (`tenant_id`, `dept_id`),
    KEY `idx_room_type_id` (`room_type_id`),
    KEY `idx_specific_date` (`specific_date`),
    KEY `idx_date_range` (`date_range_start`, `date_range_end`),
    KEY `idx_date_type` (`date_type`),
    KEY `idx_priority` (`priority`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_special_pricing_room_type` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '特殊日期价格表';
-- ========================================
-- 4. 初始化基础数据
-- ========================================
-- 插入默认价格规则示例
INSERT INTO `pms_room_pricing_rules` (
        `tenant_id`,
        `dept_id`,
        `name`,
        `description`,
        `room_type_id`,
        `price_adjustment_type`,
        `adjustment_value`,
        `priority`,
        `status`,
        `create_time`,
        `del_flag`
    )
VALUES (
        '000000',
        NULL,
        '周末加价',
        '周六周日房价上调20%',
        NULL,
        'percentage_markup',
        20.00,
        10,
        'active',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        '连住3晚优惠',
        '连续入住3晚及以上享受10%折扣',
        NULL,
        'percentage_discount',
        10.00,
        5,
        'active',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        '提前30天预订优惠',
        '提前30天预订享受15%折扣',
        NULL,
        'percentage_discount',
        15.00,
        8,
        'active',
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        '会员专享折扣',
        '金卡及以上会员享受8%折扣',
        NULL,
        'percentage_discount',
        8.00,
        12,
        'active',
        NOW(),
        '0'
    );
-- 更新周末加价规则的星期限制
UPDATE `pms_room_pricing_rules`
SET `days_of_week_json` = JSON_ARRAY(6, 7)
WHERE `name` = '周末加价'
    AND `tenant_id` = '000000';
-- 更新连住优惠规则的入住天数限制
UPDATE `pms_room_pricing_rules`
SET `min_length_of_stay` = 3
WHERE `name` = '连住3晚优惠'
    AND `tenant_id` = '000000';
-- 更新提前预订规则的预订天数限制
UPDATE `pms_room_pricing_rules`
SET `advance_booking_days_min` = 30
WHERE `name` = '提前30天预订优惠'
    AND `tenant_id` = '000000';
-- 更新会员折扣规则的会员等级限制
UPDATE `pms_room_pricing_rules`
SET `member_level_restrictions_json` = JSON_ARRAY('gold', 'platinum', 'diamond')
WHERE `name` = '会员专享折扣'
    AND `tenant_id` = '000000';
-- 插入特殊日期价格示例
INSERT INTO `pms_special_date_pricing` (
        `tenant_id`,
        `dept_id`,
        `name`,
        `date_type`,
        `specific_date`,
        `price_adjustment_type`,
        `adjustment_value`,
        `priority`,
        `status`,
        `description`,
        `is_recurring_yearly`,
        `create_time`,
        `del_flag`
    )
VALUES (
        '000000',
        NULL,
        '春节假期',
        'holiday',
        NULL,
        'percentage_markup',
        50.00,
        200,
        'active',
        '春节期间房价上调50%',
        1,
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        '国庆假期',
        'holiday',
        NULL,
        'percentage_markup',
        40.00,
        200,
        'active',
        '国庆期间房价上调40%',
        1,
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        '五一假期',
        'holiday',
        NULL,
        'percentage_markup',
        30.00,
        200,
        'active',
        '五一期间房价上调30%',
        1,
        NOW(),
        '0'
    ),
    (
        '000000',
        NULL,
        '元旦假期',
        'holiday',
        '2024-01-01',
        'percentage_markup',
        25.00,
        200,
        'active',
        '元旦当日房价上调25%',
        1,
        NOW(),
        '0'
    );
-- 更新春节假期的日期范围 (2024年为例)
UPDATE `pms_special_date_pricing`
SET `date_range_start` = '2024-02-09',
    `date_range_end` = '2024-02-17'
WHERE `name` = '春节假期'
    AND `tenant_id` = '000000';
-- 更新国庆假期的日期范围
UPDATE `pms_special_date_pricing`
SET `date_range_start` = '2024-10-01',
    `date_range_end` = '2024-10-07'
WHERE `name` = '国庆假期'
    AND `tenant_id` = '000000';
-- 更新五一假期的日期范围
UPDATE `pms_special_date_pricing`
SET `date_range_start` = '2024-05-01',
    `date_range_end` = '2024-05-05'
WHERE `name` = '五一假期'
    AND `tenant_id` = '000000';
SET FOREIGN_KEY_CHECKS = 1;
-- ========================================
-- 说明
-- ========================================
-- 1. 价格规则表支持复杂的规则配置，包括日期范围、星期限制、入住天数、提前预订等
-- 2. 价格计算历史表记录每次价格计算的完整过程和结果
-- 3. 特殊日期价格表支持节假日、活动日等特殊日期的价格设置
-- 4. 所有表都包含完整的多租户支持和审计字段
-- 5. 外键约束确保数据完整性
-- 6. 索引优化查询性能
-- 7. 初始化了基础的价格规则和特殊日期价格示例
-- 8. 支持门店级和租户级的价格管理
-- 9. JSON字段存储复杂的配置信息，如渠道限制、会员等级限制等
-- 10. 优先级机制确保规则的正确应用顺序
-- ========================================