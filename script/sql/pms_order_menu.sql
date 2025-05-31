-- ========================================
-- PMS订单管理功能菜单创建语句
-- 包含：订单管理主菜单、子菜单、权限点定义
-- 按照RuoYi框架的菜单结构标准创建
-- ========================================
-- ========================================
-- 1. 订单管理主菜单
-- ========================================
-- 插入订单管理主菜单
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单管理',
        (
            SELECT menu_id
            FROM sys_menu
            WHERE menu_name = 'PMS系统'
                AND parent_id = 0
            LIMIT 1
        ), 2, 'order', NULL,
        NULL,
        1,
        0,
        'M',
        '0',
        '0',
        NULL,
        'order',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '订单管理主菜单'
    );
-- 获取订单管理主菜单ID (用于后续子菜单)
SET @order_menu_id = LAST_INSERT_ID();
-- ========================================
-- 2. 订单管理子菜单
-- ========================================
-- 2.1 订单列表
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单列表',
        @order_menu_id,
        1,
        'orders',
        'pms/order/index',
        NULL,
        1,
        0,
        'C',
        '0',
        '0',
        'pms:order:list',
        'list',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '订单列表页面'
    );
-- 2.2 订单详情
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单详情',
        @order_menu_id,
        2,
        'order-detail',
        'pms/order/detail',
        NULL,
        1,
        0,
        'C',
        '1',
        '0',
        'pms:order:detail',
        'detail',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '订单详情页面'
    );
-- 2.3 订单创建
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '创建订单',
        @order_menu_id,
        3,
        'order-create',
        'pms/order/create',
        NULL,
        1,
        0,
        'C',
        '1',
        '0',
        'pms:order:create',
        'plus',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '创建订单页面'
    );
-- 2.4 订单状态历史
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '状态历史',
        @order_menu_id,
        4,
        'order-history',
        'pms/order/history',
        NULL,
        1,
        0,
        'C',
        '0',
        '0',
        'pms:orderHistory:list',
        'time-range',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '订单状态历史'
    );
-- 2.5 入住客人管理
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '入住客人',
        @order_menu_id,
        5,
        'order-guests',
        'pms/order/guests',
        NULL,
        1,
        0,
        'C',
        '0',
        '0',
        'pms:orderGuest:list',
        'user',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '入住客人管理'
    );
-- 2.6 渠道管理
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '渠道管理',
        @order_menu_id,
        6,
        'channels',
        'pms/order/channels',
        NULL,
        1,
        0,
        'C',
        '0',
        '0',
        'pms:channel:list',
        'link',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '订单来源渠道管理'
    );
-- 2.7 库存快照
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '库存快照',
        @order_menu_id,
        7,
        'inventory',
        'pms/order/inventory',
        NULL,
        1,
        0,
        'C',
        '0',
        '0',
        'pms:inventory:list',
        'monitor',
        103,
        1,
        NOW(),
        1,
        NOW(),
        '房间库存快照管理'
    );
-- ========================================
-- 3. 订单管理权限按钮
-- ========================================
-- 获取订单列表菜单ID
SET @order_list_menu_id = (
        SELECT menu_id
        FROM sys_menu
        WHERE menu_name = '订单列表'
            AND parent_id = @order_menu_id
    );
-- 3.1 订单列表权限按钮
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单查询',
        @order_list_menu_id,
        1,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:query',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单新增',
        @order_list_menu_id,
        2,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:add',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单修改',
        @order_list_menu_id,
        3,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:edit',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单删除',
        @order_list_menu_id,
        4,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:remove',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '订单导出',
        @order_list_menu_id,
        5,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:export',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '状态变更',
        @order_list_menu_id,
        6,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:changeStatus',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '房间分配',
        @order_list_menu_id,
        7,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:assignRoom',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '办理入住',
        @order_list_menu_id,
        8,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:checkIn',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '办理退房',
        @order_list_menu_id,
        9,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:order:checkOut',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
-- 3.2 入住客人管理权限按钮
SET @order_guests_menu_id = (
        SELECT menu_id
        FROM sys_menu
        WHERE menu_name = '入住客人'
            AND parent_id = @order_menu_id
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '客人查询',
        @order_guests_menu_id,
        1,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:orderGuest:query',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '客人新增',
        @order_guests_menu_id,
        2,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:orderGuest:add',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '客人修改',
        @order_guests_menu_id,
        3,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:orderGuest:edit',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '客人删除',
        @order_guests_menu_id,
        4,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:orderGuest:remove',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '关联档案',
        @order_guests_menu_id,
        5,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:orderGuest:linkContact',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
-- 3.3 渠道管理权限按钮
SET @channels_menu_id = (
        SELECT menu_id
        FROM sys_menu
        WHERE menu_name = '渠道管理'
            AND parent_id = @order_menu_id
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '渠道查询',
        @channels_menu_id,
        1,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:channel:query',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '渠道新增',
        @channels_menu_id,
        2,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:channel:add',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '渠道修改',
        @channels_menu_id,
        3,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:channel:edit',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '渠道删除',
        @channels_menu_id,
        4,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:channel:remove',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '渠道导出',
        @channels_menu_id,
        5,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:channel:export',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
-- 3.4 库存快照权限按钮
SET @inventory_menu_id = (
        SELECT menu_id
        FROM sys_menu
        WHERE menu_name = '库存快照'
            AND parent_id = @order_menu_id
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '快照查询',
        @inventory_menu_id,
        1,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:inventory:query',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '生成快照',
        @inventory_menu_id,
        2,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:inventory:generate',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
INSERT INTO sys_menu (
        menu_name,
        parent_id,
        order_num,
        path,
        component,
        query_param,
        is_frame,
        is_cache,
        menu_type,
        visible,
        status,
        perms,
        icon,
        create_dept,
        create_by,
        create_time,
        update_by,
        update_time,
        remark
    )
VALUES (
        '快照导出',
        @inventory_menu_id,
        3,
        '',
        '',
        NULL,
        1,
        0,
        'F',
        '0',
        '0',
        'pms:inventory:export',
        '#',
        103,
        1,
        NOW(),
        1,
        NOW(),
        ''
    );
-- ========================================
-- 4. 角色权限分配 (示例)
-- ========================================
-- 为PMS管理员角色分配订单管理权限 (假设角色ID为2)
-- 注意：实际使用时需要根据具体的角色ID进行调整
-- 获取所有订单管理相关的菜单ID
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2,
    menu_id
FROM sys_menu
WHERE menu_name IN ('订单管理')
    OR parent_id = @order_menu_id
    OR parent_id IN (
        SELECT menu_id
        FROM sys_menu
        WHERE parent_id = @order_menu_id
    );
-- ========================================
-- 说明
-- ========================================
-- 1. 创建了完整的订单管理菜单结构
-- 2. 包含主菜单、子菜单和权限按钮
-- 3. 权限点命名遵循 pms:模块:操作 的规范
-- 4. 菜单类型：M-目录，C-菜单，F-按钮
-- 5. visible: 0-显示，1-隐藏
-- 6. status: 0-正常，1-停用
-- 7. is_frame: 1-是，0-否 (是否为外链)
-- 8. is_cache: 1-缓存，0-不缓存
-- 9. 示例角色权限分配，实际使用时需要调整角色ID
-- 10. 菜单路径和组件路径需要与前端路由配置保持一致
-- ========================================