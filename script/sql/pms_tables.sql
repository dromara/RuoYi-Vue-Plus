-- ----------------------------
-- PMS核心业务表 - 房型表
-- ----------------------------
CREATE TABLE pms_room_types (
    room_type_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '房型唯一ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID(门店), 关联 sys_dept.dept_id',
    name VARCHAR(255) NOT NULL COMMENT '房型名称 (例如: 豪华大床房)',
    default_price DECIMAL(10, 2) NOT NULL COMMENT '房型默认价格 (每晚)',
    capacity INT NOT NULL COMMENT '标准入住人数',
    amenities JSON NULL COMMENT '房型设施标签。JSON数组，例如 ["wifi", "空调"]',
    description TEXT NULL COMMENT '房型描述',
    images_json JSON NULL COMMENT '房型图片。JSON数组',
    sort_order INT DEFAULT 0 COMMENT '显示排序值',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '状态。枚举: active (启用), inactive (禁用)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (room_type_id),
    INDEX idx_pms_rt_dept_name (dept_id, name),
    INDEX idx_pms_rt_dept_status (dept_id, status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 房型表';
-- ----------------------------
-- PMS核心业务表 - 房间表
-- ----------------------------
CREATE TABLE pms_room_rooms (
    room_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '房间唯一ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID(门店), 关联 sys_dept.dept_id',
    room_type_id BIGINT NOT NULL COMMENT '所属房型ID, 关联 pms_room_types.room_type_id',
    room_number VARCHAR(50) NOT NULL COMMENT '房间号 (在门店内应唯一)',
    floor VARCHAR(50) NULL COMMENT '楼层',
    room_status VARCHAR(50) NOT NULL DEFAULT 'available' COMMENT '物理状态。枚举: available (可用), occupied (占用中), maintenance (维护中), out_of_service (停用服务)',
    cleaning_status VARCHAR(50) NOT NULL DEFAULT 'clean' COMMENT '清洁状态。枚举: clean (已清洁), dirty (待清洁), cleaning_in_progress (清洁中), inspected (已查房)',
    description TEXT NULL COMMENT '房间描述或备注',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '记录状态。枚举: active (启用), inactive (禁用)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (room_id),
    UNIQUE KEY unq_pms_r_dept_room_number (dept_id, room_number, del_flag),
    INDEX idx_pms_r_dept_rt (dept_id, room_type_id),
    INDEX idx_pms_r_dept_room_status (dept_id, room_status),
    INDEX idx_pms_r_dept_cleaning_status (dept_id, cleaning_status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 房间表';
-- ----------------------------
-- PMS核心业务表 - 房间锁定记录表
-- ----------------------------
CREATE TABLE pms_room_locks (
    lock_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '锁定记录唯一ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID, 关联 sys_dept.dept_id',
    room_id BIGINT NOT NULL COMMENT '被锁定的房间ID, 关联 pms_room_rooms.room_id',
    start_datetime DATETIME NOT NULL COMMENT '锁定开始时间',
    end_datetime DATETIME NOT NULL COMMENT '锁定结束时间',
    reason TEXT NULL COMMENT '锁定原因',
    lock_type VARCHAR(50) NOT NULL DEFAULT 'manual_lock' COMMENT '锁定类型。枚举: manual_lock (手动锁定), maintenance (维护), auto_block (自动锁定), staff_use (员工自用)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (lock_id),
    INDEX idx_pms_rl_dept_room_time (dept_id, room_id, start_datetime, end_datetime),
    INDEX idx_pms_rl_dept_lock_type (dept_id, lock_type)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 房间锁定记录表';
-- ----------------------------
-- PMS核心业务表 - 核心订单表
-- ----------------------------
CREATE TABLE pms_core_orders (
    order_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '订单唯一ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID, 关联 sys_dept.dept_id',
    contact_id BIGINT NULL COMMENT '主要联系人ID, 关联 cmn_contacts.contact_id',
    primary_contact_name VARCHAR(100) NOT NULL COMMENT '主要联系人姓名 (冗余)',
    primary_contact_phone VARCHAR(50) NOT NULL COMMENT '主要联系人电话 (冗余)',
    pms_room_id BIGINT NULL COMMENT '分配的房间ID, 关联 pms_room_rooms.room_id, 入住时分配',
    room_type_id BIGINT NOT NULL COMMENT '预订的房型ID, 关联 pms_room_types.room_type_id',
    channel_id BIGINT NULL COMMENT '订单来源渠道ID, 关联 pms_core_channels.channel_id',
    check_in_date DATE NOT NULL COMMENT '计划入住日期',
    check_out_date DATE NOT NULL COMMENT '计划离店日期',
    num_adults INT NOT NULL DEFAULT 1 COMMENT '成人数',
    num_children INT DEFAULT 0 COMMENT '儿童数',
    estimated_arrival_time TIME NULL COMMENT '预计抵达时间',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    paid_amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '已付金额',
    -- 由于计算列的支持程度和潜在问题，这里不使用STORED计算列，而是在应用层计算due_amount
    currency VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '货币代码',
    order_status VARCHAR(50) NOT NULL COMMENT '订单状态。枚举: pending_confirmation, confirmed, checked_in, checked_out, cancelled, no_show, extended, waitlist',
    order_source VARCHAR(50) NOT NULL COMMENT '订单来源。枚举: direct_walk_in, direct_phone, direct_website, direct_mall_h5, ota_channel_manager, travel_agency, corporate, other',
    notes TEXT NULL COMMENT '订单备注',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    cancellation_reason TEXT NULL COMMENT '取消原因',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (order_id),
    INDEX idx_pms_o_dept_status_dates (
        dept_id,
        order_status,
        check_in_date,
        check_out_date
    ),
    INDEX idx_pms_o_dept_contact_phone (dept_id, primary_contact_phone),
    INDEX idx_pms_o_dept_source (dept_id, order_source)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 核心订单表';
-- ----------------------------
-- PMS核心业务表 - 核心订单项目表
-- ----------------------------
CREATE TABLE pms_core_order_items (
    order_item_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '订单项目唯一ID',
    order_id BIGINT NOT NULL COMMENT '所属订单ID, 关联 pms_core_orders.order_id',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID, 关联 sys_dept.dept_id (冗余)',
    pms_room_id BIGINT NULL COMMENT '关联房间ID (如房晚项目), 关联 pms_room_rooms.room_id',
    product_id BIGINT NULL COMMENT '关联产品ID (多态), 如 pms_finance_extra_charge_items.item_id',
    product_type VARCHAR(50) NOT NULL COMMENT '产品类型。枚举: room_night, extra_charge_item, package_component, service_fee, discount_adjustment',
    description VARCHAR(255) NOT NULL COMMENT '项目描述 (例如: 豪华大床房住宿, 额外早餐)',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '单价',
    -- 由于计算列的支持程度和潜在问题，这里不使用STORED计算列，而是在应用层计算total_price
    service_date DATE NULL COMMENT '服务发生日期 (例如房晚对应的日期)',
    notes TEXT NULL COMMENT '项目备注',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (order_item_id),
    INDEX idx_pms_oi_order_id (order_id),
    INDEX idx_pms_oi_dept_product_type (dept_id, product_type)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 核心订单项目表';
-- ----------------------------
-- PMS核心业务表 - 订单来源渠道表
-- ----------------------------
CREATE TABLE pms_core_channels (
    channel_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '渠道唯一ID',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, 若渠道归属特定部门',
    name VARCHAR(100) NOT NULL COMMENT '渠道名称 (例如: 携程, 官网直订)',
    channel_type VARCHAR(50) NOT NULL COMMENT '渠道类型。枚举: ota, direct_booking, gds, wholesaler, corporate, internal_use, other',
    description TEXT NULL COMMENT '渠道描述',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '状态。枚举: active (激活), inactive (停用), deprecated (弃用)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (channel_id),
    INDEX idx_pms_ch_dept_name (dept_id, name),
    INDEX idx_pms_ch_type (channel_type),
    INDEX idx_pms_ch_status (status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 订单来源渠道表';
-- ----------------------------
-- PMS核心业务表 - 客户账单(Folio)表
-- ----------------------------
CREATE TABLE pms_finance_folios (
    folio_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '账单唯一ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID, 关联 sys_dept.dept_id',
    order_id BIGINT UNIQUE NOT NULL COMMENT '关联的订单ID, 关联 pms_core_orders.order_id',
    total_charges DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总应收费用',
    total_payments DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总已收付款',
    total_refunds DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总已退款',
    -- 由于计算列的支持程度和潜在问题，这里不使用STORED计算列，而是在应用层计算balance
    folio_status VARCHAR(50) NOT NULL DEFAULT 'open' COMMENT '账单状态。枚举: open, closed, void, pending_settlement',
    notes TEXT NULL COMMENT '账单备注',
    closed_at DATETIME NULL COMMENT '账单关闭时间',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (folio_id),
    INDEX idx_pms_f_dept_order_id (dept_id, order_id),
    INDEX idx_pms_f_dept_status (dept_id, folio_status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 客户账单(Folio)表';
-- ----------------------------
-- PMS核心业务表 - 财务交易流水表
-- ----------------------------
CREATE TABLE pms_finance_transactions (
    transaction_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '交易唯一ID',
    folio_id BIGINT NOT NULL COMMENT '所属账单ID, 关联 pms_finance_folios.folio_id',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID, 关联 sys_dept.dept_id (冗余)',
    transaction_type VARCHAR(50) NOT NULL COMMENT '交易类型。枚举: charge, payment, refund, deposit, deposit_refund, adjustment_positive, adjustment_negative',
    amount DECIMAL(10, 2) NOT NULL COMMENT '交易金额',
    description VARCHAR(255) NOT NULL COMMENT '交易描述',
    payment_method_id BIGINT NULL COMMENT '支付方式ID, 关联 pms_finance_payment_methods.payment_method_id',
    payment_gateway_txn_id VARCHAR(255) NULL COMMENT '支付网关交易号',
    transaction_time DATETIME NOT NULL COMMENT '交易实际发生时间',
    related_order_item_id BIGINT NULL COMMENT '关联的订单项ID, 关联 pms_core_order_items.order_item_id',
    notes TEXT NULL COMMENT '交易备注',
    is_void BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已作废冲销',
    voided_at DATETIME NULL COMMENT '作废时间',
    voided_reason TEXT NULL COMMENT '作废原因',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (transaction_id),
    INDEX idx_pms_ft_folio_id (folio_id),
    INDEX idx_pms_ft_dept_type_time (dept_id, transaction_type, transaction_time),
    INDEX idx_pms_ft_payment_gateway_txn_id (payment_gateway_txn_id)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 财务交易流水表';
-- ----------------------------
-- PMS核心业务表 - 支付方式表
-- ----------------------------
CREATE TABLE pms_finance_payment_methods (
    payment_method_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '支付方式唯一ID',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, 若支付方式归属特定部门',
    name VARCHAR(100) NOT NULL COMMENT '支付方式名称 (例如: 现金, 微信支付)',
    method_type VARCHAR(50) NOT NULL COMMENT '支付方式类型。枚举: cash, credit_card_visa, credit_card_mastercard, credit_card_amex, alipay, wechat_pay, bank_transfer, company_account, voucher, points, other_online_payment, mobile_payment, other',
    details_json JSON NULL COMMENT '支付方式附加配置。例如支付网关参数等',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '状态。枚举: active (激活), inactive (停用)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (payment_method_id),
    INDEX idx_pms_fpm_dept_name (dept_id, name),
    INDEX idx_pms_fpm_dept_type (dept_id, method_type),
    INDEX idx_pms_fpm_dept_status (dept_id, status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 支付方式表';
-- ----------------------------
-- PMS核心业务表 - 附加费用项目表
-- ----------------------------
CREATE TABLE pms_finance_extra_charge_items (
    item_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '附加费用项目唯一ID',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, 若项目归属特定部门',
    name VARCHAR(255) NOT NULL COMMENT '项目名称',
    default_price DECIMAL(10, 2) NOT NULL COMMENT '默认单价',
    category VARCHAR(100) NULL COMMENT '费用类别，如餐饮、服务、商品等',
    is_taxable BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否应税',
    description TEXT NULL COMMENT '项目描述',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '状态。枚举: active (激活), inactive (停用)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (item_id),
    INDEX idx_pms_feci_dept_name (dept_id, name),
    INDEX idx_pms_feci_dept_category (dept_id, category),
    INDEX idx_pms_feci_dept_status (dept_id, status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 附加费用项目表';
-- ----------------------------
-- PMS核心业务表 - 房间价格规则表
-- ----------------------------
CREATE TABLE pms_room_pricing_rules (
    rule_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '价格规则唯一ID',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, 若规则归属特定部门',
    name VARCHAR(255) NOT NULL COMMENT '规则名称 (例如: 周末特价, 连住优惠)',
    room_type_id BIGINT NULL COMMENT '适用的房型ID, 关联 pms_room_types.room_type_id, NULL表示适用于所有房型',
    date_range_start DATE NULL COMMENT '规则适用开始日期',
    date_range_end DATE NULL COMMENT '规则适用结束日期',
    days_of_week_json JSON NULL COMMENT 'JSON数组，数字代表星期几, e.g., [1,2,7] (1-Mon, 7-Sun)',
    min_length_of_stay INT NULL COMMENT '最小入住天数要求',
    max_length_of_stay INT NULL COMMENT '最大入住天数限制',
    price_adjustment_type VARCHAR(50) NOT NULL COMMENT '调整类型。枚举: fixed_amount_override, percentage_discount_from_base, fixed_amount_increase_on_base, fixed_amount_decrease_from_base, set_to_value',
    adjustment_value DECIMAL(10, 2) NOT NULL COMMENT '调整值 (具体金额或百分比，如50.00或0.1代表10%)',
    priority INT NOT NULL DEFAULT 0 COMMENT '规则优先级，数字越大优先级越高',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '状态。枚举: active (激活), inactive (停用), scheduled (计划中)',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (rule_id),
    INDEX idx_pms_rpr_dept_dates (dept_id, date_range_start, date_range_end),
    INDEX idx_pms_rpr_dept_status_priority (dept_id, status, priority)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 房间价格规则表';
-- ----------------------------
-- PMS核心业务表 - 租户级PMS特定设置表
-- ----------------------------
CREATE TABLE pms_tenant_settings (
    setting_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '设置唯一ID',
    tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号, 关联 sys_tenant.tenant_id',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, NULL表示租户全局PMS设置',
    setting_group VARCHAR(100) NOT NULL COMMENT '设置分组，例如: pms_booking_rules, pms_financial_params, pms_ui_appearance',
    setting_key VARCHAR(100) NOT NULL COMMENT '设置项的唯一键 (在tenant_id, dept_id, setting_group下唯一)',
    setting_value TEXT NOT NULL COMMENT '设置项的值 (根据需要可存储JSON字符串)',
    value_type VARCHAR(50) NOT NULL DEFAULT 'string' COMMENT '值类型，如 string, integer, boolean, json，方便解析',
    description TEXT NULL COMMENT '设置项描述',
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为敏感设置',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (setting_id),
    UNIQUE KEY unq_pms_ts_tenant_dept_group_key (
        tenant_id,
        dept_id,
        setting_group,
        setting_key,
        del_flag
    ),
    INDEX idx_pms_ts_tenant_dept_group_key (tenant_id, dept_id, setting_group, setting_key)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 租户级PMS特定设置表';
-- ----------------------------
-- PMS核心业务表 - 民宿管理小程序配置表
-- ----------------------------
CREATE TABLE pms_mp_settings (
    setting_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '小程序设置唯一ID',
    tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号, 关联 sys_tenant.tenant_id',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, NULL表示租户全局小程序设置',
    setting_key VARCHAR(100) NOT NULL COMMENT '小程序配置项的唯一键。例如: mp_theme_color, mp_enable_feature_xyz',
    setting_value TEXT NOT NULL COMMENT '配置项的值 (可存储JSON字符串)',
    value_type VARCHAR(50) NOT NULL DEFAULT 'string' COMMENT '值类型，如 string, integer, boolean, json',
    description TEXT NULL COMMENT '配置项描述',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (setting_id),
    UNIQUE KEY unq_pms_mps_tenant_dept_key (tenant_id, dept_id, setting_key, del_flag),
    INDEX idx_pms_mps_tenant_dept_key (tenant_id, dept_id, setting_key)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 民宿管理小程序配置表';
-- ----------------------------
-- PMS核心业务表 - 租户用户设备表
-- ----------------------------
CREATE TABLE pms_tenant_user_devices (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '设备记录唯一ID',
    user_id BIGINT(20) NOT NULL COMMENT '关联的用户ID, 关联 sys_user.user_id',
    device_type VARCHAR(50) NOT NULL COMMENT '设备类型，例如: ios_app, android_app, wechat_miniprogram, web_push_browser',
    device_token VARCHAR(512) NOT NULL COMMENT '设备推送令牌',
    app_version VARCHAR(50) NULL COMMENT '客户端应用版本',
    last_login_at DATETIME NOT NULL COMMENT '此设备最后登录时间',
    status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '状态。枚举: active (激活), inactive (停用), token_expired (令牌过期)',
    create_time DATETIME NOT NULL COMMENT '记录创建时间',
    update_time DATETIME NOT NULL COMMENT '记录更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY unq_pms_ud_user_device_token_type (user_id, device_token, device_type),
    INDEX idx_pms_ud_user_status (user_id, status)
) ENGINE = InnoDB COMMENT = 'PMS核心业务表 - 租户用户设备表';
-- ----------------------------
-- 公共数据模型 (PMS相关) - 联系人表
-- ----------------------------
CREATE TABLE cmn_contacts (
    contact_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '联系人唯一ID',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, 若联系人主要归属某部门',
    contact_type VARCHAR(50) NOT NULL COMMENT '联系人类型。枚举: guest_individual, guest_group_contact, corporate_contact, travel_agent_contact, company_profile, supplier_contact, employee_profile, other',
    full_name VARCHAR(255) NULL COMMENT '姓名',
    phone_number VARCHAR(50) NULL COMMENT '主要联系电话',
    email VARCHAR(255) NULL COMMENT '电子邮件地址',
    wechat_openid VARCHAR(100) NULL COMMENT '微信OpenID (特定于单个微信公众号或小程序)',
    wechat_unionid VARCHAR(100) NULL COMMENT '微信UnionID (跨多个微信应用的用户唯一标识)',
    related_user_id BIGINT(20) NULL COMMENT '关联的系统用户ID, 关联 sys_user.user_id, 如员工档案关联',
    gender VARCHAR(20) NULL COMMENT '性别。枚举: male, female, non_binary, prefer_not_to_say, unknown',
    date_of_birth DATE NULL COMMENT '出生日期',
    id_type VARCHAR(50) NULL COMMENT '证件类型，如身份证、护照等。可关联sys_dict_data',
    id_number_encrypted VARCHAR(255) NULL COMMENT '证件号码 (加密存储)',
    nationality_country_code VARCHAR(10) NULL COMMENT '国籍代码 (ISO 3166-1 alpha-2)',
    preferred_language VARCHAR(10) NULL COMMENT '偏好语言代码 (e.g., en, zh-CN)',
    address_street VARCHAR(255) NULL COMMENT '街道地址',
    address_city VARCHAR(100) NULL COMMENT '城市',
    address_state_province VARCHAR(100) NULL COMMENT '省/州',
    address_postal_code VARCHAR(50) NULL COMMENT '邮政编码',
    address_country_code VARCHAR(10) NULL COMMENT '国家代码 (ISO 3166-1 alpha-2)',
    contact_status VARCHAR(50) NOT NULL DEFAULT 'active' COMMENT '联系人状态。枚举: active, inactive, prospect, blacklisted, pending_verification, merged_duplicate',
    remarks TEXT NULL COMMENT '备注信息',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (contact_id),
    UNIQUE KEY unq_cmn_c_dept_phone (dept_id, phone_number, del_flag),
    UNIQUE KEY unq_cmn_c_dept_email (dept_id, email, del_flag),
    INDEX idx_cmn_c_dept_fname (dept_id, full_name),
    INDEX idx_cmn_c_dept_status_type (dept_id, contact_status, contact_type),
    INDEX idx_cmn_c_related_user (related_user_id)
) ENGINE = InnoDB COMMENT = '公共数据模型 (PMS相关) - 联系人表';
-- ----------------------------
-- 公共数据模型 (PMS相关) - 联系人标签表
-- ----------------------------
CREATE TABLE cmn_contact_tags (
    tag_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '标签唯一ID',
    dept_id BIGINT(20) NULL COMMENT '部门ID, 关联 sys_dept.dept_id, 若标签归属特定部门或全局',
    name VARCHAR(100) NOT NULL COMMENT '标签名称',
    color VARCHAR(20) NULL COMMENT '标签显示颜色，如十六进制色值 #FF5733',
    category VARCHAR(50) NULL COMMENT '标签分类，如兴趣、客户级别、来源等',
    description TEXT NULL COMMENT '标签描述',
    is_system BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为系统预设标签 (不可删除)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT(20) NULL COMMENT '更新者ID, 关联 sys_user.user_id',
    update_time DATETIME NULL COMMENT '更新时间',
    create_dept_id BIGINT(20) NULL COMMENT '创建部门ID, 关联 sys_dept.dept_id',
    del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (tag_id),
    UNIQUE KEY unq_cmn_ct_dept_name_category (dept_id, name, category, del_flag),
    INDEX idx_cmn_ct_category (category),
    INDEX idx_cmn_ct_is_system (is_system)
) ENGINE = InnoDB COMMENT = '公共数据模型 (PMS相关) - 联系人标签表';
-- ----------------------------
-- 公共数据模型 (PMS相关) - 联系人标签关联表
-- ----------------------------
CREATE TABLE cmn_contact_tag_relations (
    relation_id BIGINT AUTO_INCREMENT NOT NULL COMMENT '关联唯一ID',
    dept_id BIGINT(20) NOT NULL COMMENT '部门ID, 关联 sys_dept.dept_id (冗余, 以联系人所属部门为准)',
    contact_id BIGINT NOT NULL COMMENT '联系人ID, 关联 cmn_contacts.contact_id',
    tag_id BIGINT NOT NULL COMMENT '标签ID, 关联 cmn_contact_tags.tag_id',
    create_by BIGINT(20) NULL COMMENT '创建者ID, 关联 sys_user.user_id',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (relation_id),
    UNIQUE KEY unq_cmn_ctr_dept_contact_tag (dept_id, contact_id, tag_id),
    INDEX idx_cmn_ctr_contact_id (contact_id),
    INDEX idx_cmn_ctr_tag_id (tag_id)
) ENGINE = InnoDB COMMENT = '公共数据模型 (PMS相关) - 联系人标签关联表';
-- ----------------------------
-- 初始化-订单来源渠道表数据
-- ----------------------------
INSERT INTO pms_core_channels (
        channel_id,
        dept_id,
        name,
        channel_type,
        status,
        create_by,
        create_time,
        create_dept_id,
        del_flag
    )
VALUES (
        1,
        NULL,
        '官网直订',
        'direct_booking',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        2,
        NULL,
        '电话预订',
        'direct_booking',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        3,
        NULL,
        '现场步入',
        'direct_walk_in',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        4,
        NULL,
        '携程',
        'ota',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        5,
        NULL,
        '飞猪',
        'ota',
        'active',
        1,
        sysdate(),
        103,
        '0'
    );
-- ----------------------------
-- 初始化-支付方式表数据
-- ----------------------------
INSERT INTO pms_finance_payment_methods (
        payment_method_id,
        dept_id,
        name,
        method_type,
        status,
        create_by,
        create_time,
        create_dept_id,
        del_flag
    )
VALUES (
        1,
        NULL,
        '现金',
        'cash',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        2,
        NULL,
        '微信支付',
        'wechat_pay',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        3,
        NULL,
        '支付宝',
        'alipay',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        4,
        NULL,
        '银行转账',
        'bank_transfer',
        'active',
        1,
        sysdate(),
        103,
        '0'
    ),
    (
        5,
        NULL,
        '公司账户/挂账',
        'company_account',
        'active',
        1,
        sysdate(),
        103,
        '0'
    );