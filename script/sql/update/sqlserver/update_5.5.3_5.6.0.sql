-- 修正已有菜单权限标识
update sys_menu set perms = N'workflow:definition:list' where menu_id = 11620;
GO
update sys_menu set perms = N'workflow:instance:list' where menu_id = 11621;
GO
update sys_menu set perms = N'workflow:instance:currentList' where menu_id = 11629;
GO

-- 流程定义管理相关按钮
INSERT sys_menu VALUES (11644, N'流程定义查询', N'11620', 1, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11645, N'流程定义新增', N'11620', 2, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:add', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11646, N'流程定义修改', N'11620', 3, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11647, N'流程定义删除', N'11620', 4, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11648, N'流程定义导出', N'11620', 5, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:export', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11649, N'流程定义导入', N'11620', 6, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:import', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11650, N'流程定义发布/取消发布', N'11620', 7, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:publish', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11651, N'流程定义复制', N'11620', 8, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:copy', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11652, N'流程定义激活/挂起', N'11620', 9, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:definition:active', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO

-- 流程实例管理相关按钮
INSERT sys_menu VALUES (11653, N'流程实例查询', N'11621', 1, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11654, N'流程变量查询', N'11621', 2, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:variableQuery', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11655, N'流程变量修改', N'11621', 3, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:variable', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11656, N'流程实例激活/挂起', N'11621', 4, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:active', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11657, N'流程实例删除', N'11621', 5, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11658, N'流程实例作废', N'11621', 6, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:invalid', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (11659, N'流程实例撤销', N'11621', 7, N'#', N'', N'', N'N', N'Y', N'F', N'0', N'0', N'workflow:instance:cancel', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
