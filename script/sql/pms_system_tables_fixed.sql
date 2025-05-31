-- =============================================
-- PMS系统配置管理模块建表语句 (修复版)
-- 版本: v1.1 (修复MySQL 8.0+兼容性问题)
-- 创建日期: 2024-12-19
-- 说明: 包含租户配置、小程序配置、用户设备管理等表
-- =============================================
-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- =============================================
-- 1. 租户/门店配置表
-- =============================================
DROP TABLE IF EXISTS `pms_tenant_settings`;
CREATE TABLE `pms_tenant_settings` (
    `setting_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设置唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID，关联 sys_tenant.tenant_id',
    `dept_id` bigint DEFAULT NULL COMMENT '部门ID，NULL=租户级配置，非NULL=门店级配置，关联 sys_dept.dept_id',
    `setting_group` varchar(50) NOT NULL COMMENT '设置分组',
    `setting_key` varchar(100) NOT NULL COMMENT '设置键名',
    `setting_value` text COMMENT '设置值',
    `value_type` varchar(20) NOT NULL DEFAULT 'string' COMMENT '值类型：string/integer/boolean/json/decimal',
    `description` varchar(500) DEFAULT NULL COMMENT '设置描述',
    `is_sensitive` tinyint NOT NULL DEFAULT '0' COMMENT '是否敏感设置：0=否，1=是',
    `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统预设：0=否，1=是',
    `sort_order` int DEFAULT '0' COMMENT '排序值',
    `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active=启用，inactive=禁用',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID，关联 sys_dept.dept_id',
    `create_by` bigint DEFAULT NULL COMMENT '创建者，关联 sys_user.user_id',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者，关联 sys_user.user_id',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=删除',
    PRIMARY KEY (`setting_id`),
    UNIQUE KEY `unq_pms_ts_tenant_dept_group_key` (
        `tenant_id`,
        `dept_id`,
        `setting_group`,
        `setting_key`,
        `del_flag`
    ),
    KEY `idx_pms_ts_tenant_dept_group` (`tenant_id`, `dept_id`, `setting_group`),
    KEY `idx_pms_ts_tenant_dept_status` (`tenant_id`, `dept_id`, `status`),
    KEY `idx_pms_ts_group_key` (`setting_group`, `setting_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户/门店配置表';
-- =============================================
-- 2. 小程序配置表
-- =============================================
DROP TABLE IF EXISTS `pms_mp_settings`;
CREATE TABLE `pms_mp_settings` (
    `setting_id` bigint NOT NULL AUTO_INCREMENT COMMENT '小程序设置唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID，关联 sys_tenant.tenant_id',
    `dept_id` bigint DEFAULT NULL COMMENT '部门ID，NULL=租户级配置，非NULL=门店级配置，关联 sys_dept.dept_id',
    `setting_key` varchar(100) NOT NULL COMMENT '设置键名',
    `setting_value` text COMMENT '设置值',
    `value_type` varchar(20) NOT NULL DEFAULT 'string' COMMENT '值类型：string/integer/boolean/json/decimal',
    `description` varchar(500) DEFAULT NULL COMMENT '设置描述',
    `is_sensitive` tinyint NOT NULL DEFAULT '0' COMMENT '是否敏感设置：0=否，1=是',
    `category` varchar(50) DEFAULT NULL COMMENT '配置分类：theme/payment/notification/function/security',
    `sort_order` int DEFAULT '0' COMMENT '排序值',
    `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active=启用，inactive=禁用',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID，关联 sys_dept.dept_id',
    `create_by` bigint DEFAULT NULL COMMENT '创建者，关联 sys_user.user_id',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者，关联 sys_user.user_id',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0=存在，1=删除',
    PRIMARY KEY (`setting_id`),
    UNIQUE KEY `unq_pms_mps_tenant_dept_key` (
        `tenant_id`,
        `dept_id`,
        `setting_key`,
        `del_flag`
    ),
    KEY `idx_pms_mps_tenant_dept_category` (`tenant_id`, `dept_id`, `category`),
    KEY `idx_pms_mps_tenant_dept_status` (`tenant_id`, `dept_id`, `status`),
    KEY `idx_pms_mps_category_key` (`category`, `setting_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '小程序配置表';
-- =============================================
-- 3. 用户设备表
-- =============================================
DROP TABLE IF EXISTS `pms_tenant_user_devices`;
CREATE TABLE `pms_tenant_user_devices` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备记录唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID，关联 sys_tenant.tenant_id',
    `user_id` bigint NOT NULL COMMENT '用户ID，关联 sys_user.user_id',
    `device_type` varchar(20) NOT NULL COMMENT '设备类型：iOS/Android/Web/MiniProgram',
    `device_token` varchar(255) DEFAULT NULL COMMENT '设备令牌，用于推送',
    `device_id` varchar(100) DEFAULT NULL COMMENT '设备唯一标识',
    `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `app_version` varchar(20) DEFAULT NULL COMMENT '应用版本',
    `os_version` varchar(20) DEFAULT NULL COMMENT '操作系统版本',
    `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_active_at` datetime DEFAULT NULL COMMENT '最后活跃时间',
    `ip_address` varchar(50) DEFAULT NULL COMMENT '最后登录IP地址',
    `location` varchar(200) DEFAULT NULL COMMENT '最后登录位置',
    `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active=活跃，inactive=不活跃，expired=已过期，blocked=已阻止',
    `push_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用推送：0=否，1=是',
    `login_count` int NOT NULL DEFAULT '0' COMMENT '登录次数',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unq_pms_tud_tenant_user_device_id` (`tenant_id`, `user_id`, `device_id`),
    KEY `idx_pms_tud_tenant_user_device` (`tenant_id`, `user_id`, `device_type`),
    KEY `idx_pms_tud_tenant_user_status` (`tenant_id`, `user_id`, `status`),
    KEY `idx_pms_tud_device_token` (`device_token`),
    KEY `idx_pms_tud_last_login` (`last_login_at`),
    KEY `idx_pms_tud_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户设备表';
-- =============================================
-- 4. 初始化租户配置数据
-- =============================================
INSERT INTO `pms_tenant_settings` (
        `tenant_id`,
        `dept_id`,
        `setting_group`,
        `setting_key`,
        `setting_value`,
        `value_type`,
        `description`,
        `is_system`,
        `sort_order`,
        `status`,
        `create_time`
    )
VALUES -- 预订规则配置
    (
        '000000',
        NULL,
        'booking_rules',
        'max_advance_booking_days',
        '365',
        'integer',
        '最大提前预订天数',
        1,
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'booking_rules',
        'min_advance_booking_hours',
        '2',
        'integer',
        '最小提前预订小时数',
        1,
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'booking_rules',
        'allow_same_day_booking',
        'true',
        'boolean',
        '允许当日预订',
        1,
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'booking_rules',
        'auto_confirm_booking',
        'false',
        'boolean',
        '自动确认预订',
        1,
        4,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'booking_rules',
        'max_stay_days',
        '30',
        'integer',
        '最大入住天数',
        1,
        5,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'booking_rules',
        'min_stay_days',
        '1',
        'integer',
        '最小入住天数',
        1,
        6,
        'active',
        NOW()
    ),
    -- 财务参数配置
    (
        '000000',
        NULL,
        'finance_params',
        'default_currency',
        'CNY',
        'string',
        '默认货币',
        1,
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'finance_params',
        'tax_rate',
        '0.06',
        'decimal',
        '税率',
        1,
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'finance_params',
        'deposit_percentage',
        '0.30',
        'decimal',
        '押金比例',
        1,
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'finance_params',
        'cancellation_fee_rate',
        '0.10',
        'decimal',
        '取消费率',
        1,
        4,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'finance_params',
        'late_checkout_fee',
        '50.00',
        'decimal',
        '延迟退房费用',
        1,
        5,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'finance_params',
        'early_checkin_fee',
        '30.00',
        'decimal',
        '提前入住费用',
        1,
        6,
        'active',
        NOW()
    ),
    -- 界面外观配置
    (
        '000000',
        NULL,
        'ui_appearance',
        'theme_color',
        '#409EFF',
        'string',
        '主题色',
        1,
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'ui_appearance',
        'logo_url',
        '/images/logo.png',
        'string',
        '品牌标识',
        1,
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'ui_appearance',
        'welcome_message',
        '欢迎入住',
        'string',
        '欢迎语',
        1,
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'ui_appearance',
        'footer_text',
        '云宿居PMS系统',
        'string',
        '页脚文本',
        1,
        4,
        'active',
        NOW()
    ),
    -- 业务流程配置
    (
        '000000',
        NULL,
        'business_process',
        'auto_room_assignment',
        'false',
        'boolean',
        '自动分配房间',
        1,
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'business_process',
        'require_id_verification',
        'true',
        'boolean',
        '要求身份验证',
        1,
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'business_process',
        'auto_send_confirmation',
        'true',
        'boolean',
        '自动发送确认信息',
        1,
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'business_process',
        'enable_guest_review',
        'true',
        'boolean',
        '启用客人评价',
        1,
        4,
        'active',
        NOW()
    );
-- =============================================
-- 5. 初始化小程序配置数据
-- =============================================
INSERT INTO `pms_mp_settings` (
        `tenant_id`,
        `dept_id`,
        `setting_key`,
        `setting_value`,
        `value_type`,
        `description`,
        `category`,
        `sort_order`,
        `status`,
        `create_time`
    )
VALUES -- 主题配置
    (
        '000000',
        NULL,
        'theme_primary_color',
        '#409EFF',
        'string',
        '主题主色调',
        'theme',
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'theme_secondary_color',
        '#67C23A',
        'string',
        '主题辅助色',
        'theme',
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'app_name',
        '云宿居',
        'string',
        '小程序名称',
        'theme',
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'app_logo',
        '/images/mp-logo.png',
        'string',
        '小程序图标',
        'theme',
        4,
        'active',
        NOW()
    ),
    -- 功能开关
    (
        '000000',
        NULL,
        'enable_online_booking',
        'true',
        'boolean',
        '启用在线预订',
        'function',
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'enable_online_payment',
        'true',
        'boolean',
        '启用在线支付',
        'function',
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'enable_guest_services',
        'true',
        'boolean',
        '启用客房服务',
        'function',
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'enable_feedback',
        'true',
        'boolean',
        '启用意见反馈',
        'function',
        4,
        'active',
        NOW()
    ),
    -- 支付配置
    (
        '000000',
        NULL,
        'wechat_pay_enabled',
        'true',
        'boolean',
        '启用微信支付',
        'payment',
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'alipay_enabled',
        'false',
        'boolean',
        '启用支付宝支付',
        'payment',
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'payment_timeout_minutes',
        '30',
        'integer',
        '支付超时时间（分钟）',
        'payment',
        3,
        'active',
        NOW()
    ),
    -- 通知配置
    (
        '000000',
        NULL,
        'enable_booking_notification',
        'true',
        'boolean',
        '启用预订通知',
        'notification',
        1,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'enable_checkin_reminder',
        'true',
        'boolean',
        '启用入住提醒',
        'notification',
        2,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'enable_checkout_reminder',
        'true',
        'boolean',
        '启用退房提醒',
        'notification',
        3,
        'active',
        NOW()
    ),
    (
        '000000',
        NULL,
        'notification_advance_hours',
        '2',
        'integer',
        '通知提前小时数',
        'notification',
        4,
        'active',
        NOW()
    );
-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;