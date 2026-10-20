-- ============================================================
-- RuoYi-Vue-Plus 6.0.0 → 6.1.0 工作流升级脚本（MySQL）
-- 对齐 warm-flow 2.1.0：删除废弃的节点版本列、状态字典改为小写英文编码
-- 只给存量库执行；全新建库直接用 script/sql/ry_workflow.sql，无需本脚本
-- 执行完到「系统管理 → 缓存管理 → 刷新缓存」刷一次字典缓存，否则前端仍显示旧文案
-- ============================================================

-- 1. 节点版本已统一由流程定义管理，flow_node.version 在引擎 1.8.9 起就不存在了
--    旧全量脚本里该列是 NOT NULL 且无默认值，引擎写节点会报「doesn't have a default value」
ALTER TABLE `flow_node` DROP COLUMN `version`;

-- 2. 任务状态字典改为引擎 TaskStatus 的 7 个码，外加撤回归档留痕的 cancel
DELETE FROM sys_dict_data WHERE dict_type = 'wf_task_status';
INSERT INTO sys_dict_data VALUES (1761600000000000060, 1, '待审批', 'pending', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '待审批');
INSERT INTO sys_dict_data VALUES (1761600000000000049, 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '通过');
INSERT INTO sys_dict_data VALUES (1761600000000000061, 3, '自动审批', 'auto_pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '自动审批');
INSERT INTO sys_dict_data VALUES (1761600000000000056, 4, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '抄送');
INSERT INTO sys_dict_data VALUES (1761600000000000052, 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '退回');
INSERT INTO sys_dict_data VALUES (1761600000000000062, 6, '拿回', 'take_back', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '拿回');
INSERT INTO sys_dict_data VALUES (1761600000000000051, 7, '失效', 'invalid', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '失效');
INSERT INTO sys_dict_data VALUES (1761600000000000048, 8, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '撤销');
INSERT INTO sys_dict_data VALUES (1761600000000000064, 9, '转办', 'transfer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '转办');
INSERT INTO sys_dict_data VALUES (1761600000000000065, 10, '委派', 'delegate', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '委派');
INSERT INTO sys_dict_data VALUES (1761600000000000066, 11, '加签', 'add_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '加签');
INSERT INTO sys_dict_data VALUES (1761600000000000067, 12, '减签', 'remove_signer', 'wf_task_status', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '减签');

-- 原来的 waiting / termination / transfer / depute / sign / sign_off / timeout 已随上面整组删除：
-- 前两个属实例维度（wf_business_status 有），其余属协作维度 cooperate_type（transfer / delegate /
-- add_signer / remove_signer），timeout 是 flow_event_wait 的状态，都不是任务状态。
-- take_back 目前没有写入方，是留给「拿回」功能的占位，别当冗余删掉。

-- 3. 实例状态字典补 restart（引擎 FlowStatus 有，字典缺）
INSERT INTO sys_dict_data VALUES (1761600000000000063, 8, '重启', 'restart', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '重启');
