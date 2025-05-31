-- 房型管理、房间管理、房间锁定管理数据库表结构
-- 基于PMS数据模型v5.4
SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
-- Table structure for pms_room_types
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_types`;
CREATE TABLE `pms_room_types` (
    `room_type_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '房型唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID (门店)',
    `type_name` varchar(100) NOT NULL COMMENT '房型名称',
    `type_code` varchar(50) NOT NULL COMMENT '房型代码',
    `description` varchar(500) DEFAULT NULL COMMENT '房型描述',
    `standard_occupancy` int(11) NOT NULL DEFAULT '2' COMMENT '标准入住人数',
    `max_occupancy` int(11) NOT NULL DEFAULT '2' COMMENT '最大入住人数',
    `room_area` decimal(8, 2) DEFAULT NULL COMMENT '房间面积(平方米)',
    `bed_configuration` varchar(200) DEFAULT NULL COMMENT '床型配置',
    `amenities` varchar(1000) DEFAULT NULL COMMENT '房间设施',
    `default_price` decimal(10, 2) NOT NULL COMMENT '默认价格',
    `status` varchar(50) NOT NULL DEFAULT 'active' COMMENT '房型状态',
    `sort_order` int(11) DEFAULT '0' COMMENT '排序值',
    `images` varchar(2000) DEFAULT NULL COMMENT '房型图片',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`room_type_id`),
    UNIQUE KEY `uk_room_type_code_dept` (`type_code`, `dept_id`, `del_flag`),
    KEY `idx_room_type_dept` (`dept_id`),
    KEY `idx_room_type_status` (`status`),
    KEY `idx_room_type_tenant` (`tenant_id`),
    KEY `idx_room_type_dept_status` (`dept_id`, `status`, `del_flag`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '房型管理表';
-- ----------------------------
-- Table structure for pms_room_rooms
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_rooms`;
CREATE TABLE `pms_room_rooms` (
    `room_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '房间唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_type_id` bigint(20) NOT NULL COMMENT '房型ID',
    `room_number` varchar(50) NOT NULL COMMENT '房间号',
    `floor` varchar(20) DEFAULT NULL COMMENT '楼层',
    `room_status` varchar(50) NOT NULL DEFAULT 'available' COMMENT '房间物理状态',
    `cleaning_status` varchar(50) NOT NULL DEFAULT 'clean' COMMENT '清洁状态',
    `description` varchar(500) DEFAULT NULL COMMENT '房间描述',
    `special_amenities` varchar(1000) DEFAULT NULL COMMENT '房间特殊设施',
    `last_cleaning_time` datetime DEFAULT NULL COMMENT '最后清洁时间',
    `last_maintenance_time` datetime DEFAULT NULL COMMENT '最后维护时间',
    `status_remarks` varchar(500) DEFAULT NULL COMMENT '房间状态备注',
    `sort_order` int(11) DEFAULT '0' COMMENT '排序值',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`room_id`),
    UNIQUE KEY `uk_room_number_dept` (`room_number`, `dept_id`, `del_flag`),
    KEY `idx_room_type` (`room_type_id`),
    KEY `idx_room_dept` (`dept_id`),
    KEY `idx_room_status` (`room_status`),
    KEY `idx_room_cleaning` (`cleaning_status`),
    KEY `idx_room_tenant` (`tenant_id`),
    CONSTRAINT `fk_room_room_type` FOREIGN KEY (`room_type_id`) REFERENCES `pms_room_types` (`room_type_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '房间管理表';
-- ----------------------------
-- Table structure for pms_room_locks
-- ----------------------------
DROP TABLE IF EXISTS `pms_room_locks`;
CREATE TABLE `pms_room_locks` (
    `lock_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '锁定记录唯一ID',
    `tenant_id` varchar(20) NOT NULL COMMENT '租户ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID (门店)',
    `room_id` bigint(20) NOT NULL COMMENT '房间ID',
    `lock_type` varchar(50) NOT NULL COMMENT '锁定类型',
    `lock_start_time` datetime NOT NULL COMMENT '锁定开始时间',
    `lock_end_time` datetime DEFAULT NULL COMMENT '锁定结束时间',
    `lock_reason` varchar(500) NOT NULL COMMENT '锁定原因',
    `lock_status` varchar(50) NOT NULL DEFAULT 'active' COMMENT '锁定状态',
    `unlock_time` datetime DEFAULT NULL COMMENT '解锁时间',
    `unlock_by` bigint(20) DEFAULT NULL COMMENT '解锁操作人',
    `unlock_reason` varchar(500) DEFAULT NULL COMMENT '解锁原因',
    `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`lock_id`),
    KEY `idx_room_lock_tenant` (`tenant_id`),
    KEY `idx_room_lock_dept` (`dept_id`),
    KEY `idx_room_lock_room` (`room_id`),
    KEY `idx_room_lock_status` (`lock_status`),
    KEY `idx_room_lock_time` (`lock_start_time`, `lock_end_time`),
    CONSTRAINT `fk_room_lock_room` FOREIGN KEY (`room_id`) REFERENCES `pms_room_rooms` (`room_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '房间锁定管理表';
-- ----------------------------
-- 插入基础字典数据
-- ----------------------------
-- 房型状态字典
INSERT INTO sys_dict_type (
        dict_id,
        tenant_id,
        dict_name,
        dict_type,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086030,
        '000000',
        '房型状态',
        'pms_room_type_status',
        103,
        1,
        sysdate(),
        '房型状态列表'
    );
INSERT INTO sys_dict_data (
        dict_code,
        tenant_id,
        dict_sort,
        dict_label,
        dict_value,
        dict_type,
        css_class,
        list_class,
        is_default,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086031,
        '000000',
        1,
        '启用',
        'active',
        'pms_room_type_status',
        '',
        'success',
        'Y',
        103,
        1,
        sysdate(),
        '房型启用状态'
    ),
    (
        1926240964416086032,
        '000000',
        2,
        '禁用',
        'inactive',
        'pms_room_type_status',
        '',
        'danger',
        'N',
        103,
        1,
        sysdate(),
        '房型禁用状态'
    ),
    (
        1926240964416086033,
        '000000',
        3,
        '维护中',
        'maintenance',
        'pms_room_type_status',
        '',
        'warning',
        'N',
        103,
        1,
        sysdate(),
        '房型维护状态'
    );
-- 房间物理状态字典
INSERT INTO sys_dict_type (
        dict_id,
        tenant_id,
        dict_name,
        dict_type,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086034,
        '000000',
        '房间物理状态',
        'pms_room_status',
        103,
        1,
        sysdate(),
        '房间物理状态列表'
    );
INSERT INTO sys_dict_data (
        dict_code,
        tenant_id,
        dict_sort,
        dict_label,
        dict_value,
        dict_type,
        css_class,
        list_class,
        is_default,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086035,
        '000000',
        1,
        '可用',
        'available',
        'pms_room_status',
        '',
        'success',
        'Y',
        103,
        1,
        sysdate(),
        '房间可用状态'
    ),
    (
        1926240964416086036,
        '000000',
        2,
        '占用',
        'occupied',
        'pms_room_status',
        '',
        'primary',
        'N',
        103,
        1,
        sysdate(),
        '房间占用状态'
    ),
    (
        1926240964416086037,
        '000000',
        3,
        '维护中',
        'maintenance',
        'pms_room_status',
        '',
        'warning',
        'N',
        103,
        1,
        sysdate(),
        '房间维护状态'
    ),
    (
        1926240964416086038,
        '000000',
        4,
        '停用',
        'out_of_order',
        'pms_room_status',
        '',
        'danger',
        'N',
        103,
        1,
        sysdate(),
        '房间停用状态'
    );
-- 清洁状态字典
INSERT INTO sys_dict_type (
        dict_id,
        tenant_id,
        dict_name,
        dict_type,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086039,
        '000000',
        '清洁状态',
        'pms_cleaning_status',
        103,
        1,
        sysdate(),
        '房间清洁状态列表'
    );
INSERT INTO sys_dict_data (
        dict_code,
        tenant_id,
        dict_sort,
        dict_label,
        dict_value,
        dict_type,
        css_class,
        list_class,
        is_default,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086040,
        '000000',
        1,
        '已清洁',
        'clean',
        'pms_cleaning_status',
        '',
        'success',
        'Y',
        103,
        1,
        sysdate(),
        '房间已清洁'
    ),
    (
        1926240964416086041,
        '000000',
        2,
        '待清洁',
        'dirty',
        'pms_cleaning_status',
        '',
        'warning',
        'N',
        103,
        1,
        sysdate(),
        '房间待清洁'
    ),
    (
        1926240964416086042,
        '000000',
        3,
        '清洁中',
        'cleaning',
        'pms_cleaning_status',
        '',
        'primary',
        'N',
        103,
        1,
        sysdate(),
        '房间清洁中'
    ),
    (
        1926240964416086043,
        '000000',
        4,
        '检查中',
        'inspecting',
        'pms_cleaning_status',
        '',
        'info',
        'N',
        103,
        1,
        sysdate(),
        '房间检查中'
    );
-- 锁定类型字典
INSERT INTO sys_dict_type (
        dict_id,
        tenant_id,
        dict_name,
        dict_type,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086044,
        '000000',
        '房间锁定类型',
        'pms_lock_type',
        103,
        1,
        sysdate(),
        '房间锁定类型列表'
    );
INSERT INTO sys_dict_data (
        dict_code,
        tenant_id,
        dict_sort,
        dict_label,
        dict_value,
        dict_type,
        css_class,
        list_class,
        is_default,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086045,
        '000000',
        1,
        '维护锁定',
        'maintenance',
        'pms_lock_type',
        '',
        'warning',
        'N',
        103,
        1,
        sysdate(),
        '房间维护锁定'
    ),
    (
        1926240964416086046,
        '000000',
        2,
        '清洁锁定',
        'cleaning',
        'pms_lock_type',
        '',
        'primary',
        'N',
        103,
        1,
        sysdate(),
        '房间清洁锁定'
    ),
    (
        1926240964416086047,
        '000000',
        3,
        '管理锁定',
        'management',
        'pms_lock_type',
        '',
        'info',
        'N',
        103,
        1,
        sysdate(),
        '房间管理锁定'
    ),
    (
        1926240964416086048,
        '000000',
        4,
        '故障锁定',
        'malfunction',
        'pms_lock_type',
        '',
        'danger',
        'N',
        103,
        1,
        sysdate(),
        '房间故障锁定'
    );
-- 锁定状态字典
INSERT INTO sys_dict_type (
        dict_id,
        tenant_id,
        dict_name,
        dict_type,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086049,
        '000000',
        '锁定状态',
        'pms_lock_status',
        103,
        1,
        sysdate(),
        '房间锁定状态列表'
    );
INSERT INTO sys_dict_data (
        dict_code,
        tenant_id,
        dict_sort,
        dict_label,
        dict_value,
        dict_type,
        css_class,
        list_class,
        is_default,
        create_dept,
        create_by,
        create_time,
        remark
    )
VALUES (
        1926240964416086050,
        '000000',
        1,
        '锁定中',
        'active',
        'pms_lock_status',
        '',
        'warning',
        'Y',
        103,
        1,
        sysdate(),
        '房间锁定中'
    ),
    (
        1926240964416086051,
        '000000',
        2,
        '已解锁',
        'unlocked',
        'pms_lock_status',
        '',
        'success',
        'N',
        103,
        1,
        sysdate(),
        '房间已解锁'
    ),
    (
        1926240964416086052,
        '000000',
        3,
        '已过期',
        'expired',
        'pms_lock_status',
        '',
        'info',
        'N',
        103,
        1,
        sysdate(),
        '锁定已过期'
    );
-- ----------------------------
-- 插入测试数据
-- ----------------------------
-- 插入房型测试数据
INSERT INTO pms_room_types (
        room_type_id,
        tenant_id,
        dept_id,
        type_name,
        type_code,
        description,
        standard_occupancy,
        max_occupancy,
        room_area,
        bed_configuration,
        amenities,
        default_price,
        status,
        sort_order,
        create_dept,
        create_by,
        create_time
    )
VALUES (
        1,
        '000000',
        103,
        '标准单人间',
        'STD_SINGLE',
        '舒适的标准单人间，适合商务出行',
        1,
        1,
        20.00,
        '单人床 1.2m',
        'WiFi,空调,电视,独立卫浴',
        188.00,
        'active',
        1,
        103,
        1,
        sysdate()
    ),
    (
        2,
        '000000',
        103,
        '标准双人间',
        'STD_DOUBLE',
        '温馨的标准双人间，适合情侣或夫妻',
        2,
        2,
        25.00,
        '双人床 1.8m',
        'WiFi,空调,电视,独立卫浴,迷你吧',
        268.00,
        'active',
        2,
        103,
        1,
        sysdate()
    ),
    (
        3,
        '000000',
        103,
        '豪华套房',
        'DELUXE_SUITE',
        '豪华套房，享受尊贵体验',
        2,
        4,
        50.00,
        '大床 2.0m + 沙发床',
        'WiFi,空调,电视,独立卫浴,迷你吧,客厅,阳台',
        588.00,
        'active',
        3,
        103,
        1,
        sysdate()
    );
-- 插入房间测试数据
INSERT INTO pms_room_rooms (
        room_id,
        tenant_id,
        dept_id,
        room_type_id,
        room_number,
        floor,
        room_status,
        cleaning_status,
        description,
        last_cleaning_time,
        sort_order,
        create_dept,
        create_by,
        create_time
    )
VALUES (
        1,
        '000000',
        103,
        1,
        '101',
        '1F',
        'available',
        'clean',
        '一楼标准单人间',
        sysdate(),
        101,
        103,
        1,
        sysdate()
    ),
    (
        2,
        '000000',
        103,
        1,
        '102',
        '1F',
        'available',
        'clean',
        '一楼标准单人间',
        sysdate(),
        102,
        103,
        1,
        sysdate()
    ),
    (
        3,
        '000000',
        103,
        2,
        '201',
        '2F',
        'available',
        'clean',
        '二楼标准双人间',
        sysdate(),
        201,
        103,
        1,
        sysdate()
    ),
    (
        4,
        '000000',
        103,
        2,
        '202',
        '2F',
        'occupied',
        'dirty',
        '二楼标准双人间',
        DATE_SUB(sysdate(), INTERVAL 1 DAY),
        202,
        103,
        1,
        sysdate()
    ),
    (
        5,
        '000000',
        103,
        3,
        '301',
        '3F',
        'available',
        'clean',
        '三楼豪华套房',
        sysdate(),
        301,
        103,
        1,
        sysdate()
    );
SET FOREIGN_KEY_CHECKS = 1;
SELECT '房型管理、房间管理、房间锁定管理表创建完成' as message;