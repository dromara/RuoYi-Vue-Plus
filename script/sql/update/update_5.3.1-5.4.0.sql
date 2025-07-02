ALTER TABLE `flow_task`
    ADD COLUMN `flow_status` varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）' AFTER `node_type`;

ALTER TABLE `flow_instance`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）' AFTER `variable`;

ALTER TABLE `flow_his_task`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）' AFTER `skip_type`;

ALTER TABLE `sys_social`
    MODIFY COLUMN `access_token` varchar(2000)  NOT NULL COMMENT '用户的授权令牌' AFTER `avatar`;
ALTER TABLE `sys_social`
    MODIFY COLUMN `refresh_token` varchar(2000) DEFAULT NULL COMMENT '刷新令牌，部分平台可能没有' AFTER `expire_in`;

insert into sys_menu values('116', '修改生成配置',  '3',   '2', 'gen-edit/index/:tableId', 'tool/gen/editTable', '', 1, 1, 'C', '1', '0', 'tool:gen:edit',           '#',               103, 1, sysdate(), null, null, '');
insert into sys_menu values('130', '分配用户',     '1',   '2', 'role-auth/user/:roleId', 'system/role/authUser', '', 1, 1, 'C', '1', '0', 'system:role:edit',      '#',               103, 1, sysdate(), null, null, '');
insert into sys_menu values('131', '分配角色',     '1',   '1', 'user-auth/role/:userId', 'system/user/authRole', '', 1, 1, 'C', '1', '0', 'system:user:edit',      '#',               103, 1, sysdate(), null, null, '');
insert into sys_menu values('132', '字典数据',     '1',   '6', 'dict-data/index/:dictId', 'system/dict/data', '', 1, 1, 'C', '1', '0', 'system:dict:list',         '#',               103, 1, sysdate(), null, null, '');
insert into sys_menu values('133', '文件配置管理',  '1',   '10', 'oss-config/index',              'system/oss/config', '', 1, 1, 'C', '1', '0', 'system:ossConfig:list',  '#',                103, 1, sysdate(), null, null, '');
insert into sys_menu values('11700', '流程设计', '11616', '5', 'design/index',   'workflow/processDefinition/design', '', 1, 1, 'C', '1', '0', 'workflow:leave:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('11701', '请假申请', '11616', '6', 'leaveEdit/index', 'workflow/leave/leaveEdit', '', 1, 1, 'C', '1', '0', 'workflow:leave:edit', '#', 103, 1, sysdate(), null, null, '');
