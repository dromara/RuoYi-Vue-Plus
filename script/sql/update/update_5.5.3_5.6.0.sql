-- 修正已有菜单权限标识
update sys_menu set perms = 'workflow:definition:list' where menu_id = '11620';
update sys_menu set perms = 'workflow:instance:list' where menu_id = '11621';
update sys_menu set perms = 'workflow:instance:currentList' where menu_id = '11629';

-- 流程定义管理相关按钮
INSERT INTO sys_menu VALUES ('11644', '流程定义查询', '11620', '1', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:query', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11645', '流程定义新增', '11620', '2', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:add', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11646', '流程定义修改', '11620', '3', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:edit', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11647', '流程定义删除', '11620', '4', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:remove', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11648', '流程定义导出', '11620', '5', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:export', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11649', '流程定义导入', '11620', '6', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:import', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11650', '流程定义发布/取消发布', '11620', '7', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:publish', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11651', '流程定义复制', '11620', '8', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:copy', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11652', '流程定义激活/挂起', '11620', '9', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:definition:active', '#', 103, 1, sysdate(), null, null, '');

-- 流程实例管理相关按钮
INSERT INTO sys_menu VALUES ('11653', '流程实例查询', '11621', '1', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:query', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11654', '流程变量查询', '11621', '2', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variableQuery', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11655', '流程变量修改', '11621', '3', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:variable', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11656', '流程实例激活/挂起', '11621', '4', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:active', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11657', '流程实例删除', '11621', '5', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:remove', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11658', '流程实例作废', '11621', '6', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:invalid', '#', 103, 1, sysdate(), null, null, '');
INSERT INTO sys_menu VALUES ('11659', '流程实例撤销', '11621', '7', '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:instance:cancel', '#', 103, 1, sysdate(), null, null, '');
