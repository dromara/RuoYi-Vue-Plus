-- LIVE 外发订单人工核查按钮：仅在派发待核查菜单存在时添加独立操作权限。
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query_param,
  is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu,
  ext, create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       'LIVE 外发订单核查',
       (SELECT menu_id FROM sys_menu WHERE perms = 'quant:dispatch:view' LIMIT 1),
       90, '', NULL, '', 'N', 'Y', 'F', '1', '0', 'quant:dispatch:recover', '#', '', '',
       NULL, NULL, NOW(), NULL, NULL, '仅核实 Binance 保护单事实并按原流程恢复，不提供强制解锁'
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'quant:dispatch:view')
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'quant:dispatch:recover');
