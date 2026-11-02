-- 订单管理-同步：只在订单管理菜单存在且权限尚未创建时插入。
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query_param,
  is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu,
  ext, create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '订单管理-同步',
       (SELECT menu_id FROM sys_menu WHERE perms = 'quant:order:list' LIMIT 1),
       90, '', NULL, '', 'N', 'Y', 'F', '1', '0', 'quant:order:sync', '#', '', '',
       NULL, NULL, NOW(), NULL, NULL, 'AiQuant 量化模块权限按钮'
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'quant:order:list')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'quant:order:sync');
