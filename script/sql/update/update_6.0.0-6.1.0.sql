-- ============================================================
-- RuoYi-Vue-Plus 6.0.0 → 6.1.0 工作流升级脚本（MySQL）
-- 对齐 warm-flow 2.1.0：新增节点/定义配置与事件列、迁移状态编码及事件等待表
-- 只给存量库执行；全新建库直接用 script/sql/ry_workflow.sql，无需本脚本
-- 请在启动已开启 warm-flow.timer-enabled 的 2.1.0 应用之前，按顺序执行本脚本
-- 执行完到「系统管理 → 缓存管理 → 刷新缓存」刷一次字典缓存，否则前端仍显示旧文案
-- ============================================================

-- 1. 节点版本已统一由流程定义管理，flow_node.version 在引擎 1.8.9 起就不存在了
--    旧全量脚本里该列是 NOT NULL 且无默认值，引擎写节点会报「doesn't have a default value」
ALTER TABLE `flow_node` DROP COLUMN `version`;

-- 2. 补齐 2.1.0 的定义、服务任务、抄送与事件节点配置列
ALTER TABLE `flow_definition`
    ADD COLUMN `def_config` text NULL COMMENT '流程定义配置（自动审批等引擎配置与自定义扩展属性json）' AFTER `ext`;

ALTER TABLE `flow_node`
    ADD COLUMN `node_config` text NULL COMMENT '节点配置（服务任务、事件等引擎节点配置json）' AFTER `ext`,
    ADD COLUMN `event_type` varchar(40) NULL COMMENT '事件类型' AFTER `node_config`,
    ADD COLUMN `event_name` varchar(100) NULL COMMENT '事件名称（消息事件使用）' AFTER `event_type`,
    ADD COLUMN `attached_to_node_code` varchar(100) NULL COMMENT '边界事件附着的任务节点编码' AFTER `event_name`,
    ADD COLUMN `interrupting` tinyint(1) NULL COMMENT '边界事件是否中断型' AFTER `attached_to_node_code`;

ALTER TABLE `flow_his_task`
    ADD COLUMN `node_config` text NULL COMMENT '节点配置（服务任务、事件等引擎节点配置json）' AFTER `ext`;

-- 3. 实例、任务和协作状态改用独立的字符串编码
ALTER TABLE `flow_instance`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '实例状态（draft waiting termination invalid cancel finish back restart）';
ALTER TABLE `flow_task`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '任务状态（pending pass auto_pass back invalid take_back copy）';
ALTER TABLE `flow_his_task`
    MODIFY COLUMN `flow_status` varchar(20) NOT NULL COMMENT '任务状态（pending pass auto_pass back invalid take_back copy；撤回归档为 cancel）',
    MODIFY COLUMN `cooperate_type` varchar(20) NOT NULL DEFAULT 'none' COMMENT '协作方式（none transfer delegate add_signer remove_signer）';
ALTER TABLE `flow_user`
    MODIFY COLUMN `type` char(1) NOT NULL COMMENT '人员类型（1审批人 2转办人 3委托人 4抄送人）',
    MODIFY COLUMN `associated` bigint NOT NULL COMMENT '关联id（待办任务id；抄送人记录为 flow_his_task.id）';

-- 旧数字码只在命中时转换，已经使用新编码的数据不会改变
UPDATE flow_instance SET flow_status = CASE flow_status
    WHEN '0' THEN 'draft' WHEN '1' THEN 'waiting' WHEN '4' THEN 'termination'
    WHEN '5' THEN 'invalid' WHEN '6' THEN 'cancel' WHEN '7' THEN 'waiting'
    WHEN '8' THEN 'finish' WHEN '9' THEN 'back' WHEN '12' THEN 'restart'
    WHEN '13' THEN 'draft' END
WHERE flow_status IN ('0', '1', '4', '5', '6', '7', '8', '9', '12', '13');

UPDATE flow_task SET flow_status = CASE flow_status
    WHEN '1' THEN 'pending' WHEN '2' THEN 'pass' WHEN '8' THEN 'pass'
    WHEN '9' THEN 'back' WHEN '10' THEN 'invalid' WHEN '11' THEN 'take_back' END
WHERE flow_status IN ('1', '2', '8', '9', '10', '11');

UPDATE flow_his_task SET flow_status = CASE flow_status
    WHEN '1' THEN 'pending' WHEN '2' THEN 'pass' WHEN '8' THEN 'pass'
    WHEN '9' THEN 'back' WHEN '10' THEN 'invalid' WHEN '11' THEN 'take_back' END
WHERE flow_status IN ('1', '2', '8', '9', '10', '11');

UPDATE flow_his_task SET cooperate_type = CASE cooperate_type
    WHEN '0' THEN 'none' WHEN '1' THEN 'none' WHEN '2' THEN 'transfer'
    WHEN '3' THEN 'delegate' WHEN '4' THEN 'none' WHEN '5' THEN 'none'
    WHEN '6' THEN 'add_signer' WHEN '7' THEN 'remove_signer' END
WHERE cooperate_type IN ('0', '1', '2', '3', '4', '5', '6', '7');

-- 4. 任务状态字典改为引擎 TaskStatus 的 7 个码，外加撤回归档留痕的 cancel
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

-- 5. 实例状态字典补 restart（引擎 FlowStatus 有，字典缺）
INSERT INTO sys_dict_data VALUES (1761600000000000063, 8, '重启', 'restart', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '重启');

-- 6. 事件等待表：定时 / 消息等需要「等待」的事件统一落在此表（与全量脚本一致）。
--    6.0.0 存量库没有这张表，不建表则定时器与定时任务页面全部不可用
--    注意：建表后还需在 application.yml 打开 warm-flow.timer-enabled: true，引擎才会扫描本表
CREATE TABLE IF NOT EXISTS `flow_event_wait`
(
    `id`               bigint       NOT NULL COMMENT '主键id',
    `tenant_id`        varchar(40)  DEFAULT NULL COMMENT '租户id',
    `del_flag`         char(1)      DEFAULT '0' COMMENT '删除标志',
    `definition_id`    bigint       DEFAULT NULL COMMENT '流程定义id（定时启动事件必须有）',
    `instance_id`      bigint       DEFAULT NULL COMMENT '流程实例id（实例等待事件必须有）',
    `node_code`        varchar(100) DEFAULT NULL COMMENT '事件节点编码（定位沿用 definition_id + node_code）',
    `event_type`       varchar(40)  NOT NULL COMMENT '事件类型（MESSAGE_START TIMER_START TIMER_CATCH MESSAGE_CATCH MESSAGE_THROW TIMER_BOUNDARY MESSAGE_BOUNDARY ERROR_CATCH ERROR_BOUNDARY TERMINATE_END MESSAGE_END）',
    `event_name`       varchar(100) DEFAULT NULL COMMENT '事件名称（消息事件使用）',
    `attached_task_id` bigint       DEFAULT NULL COMMENT '边界事件关联的待办任务id',
    `trigger_type`     varchar(40)  DEFAULT NULL COMMENT '触发策略类型（AFTER FIXED POINT_IN_TIME）',
    `trigger_interval` varchar(100) DEFAULT NULL COMMENT '触发策略原始配置',
    `next_trigger_at`  datetime     DEFAULT NULL COMMENT '下次触发时间',
    `timezone`         varchar(40)  DEFAULT NULL COMMENT '定时触发使用的时区',
    `repeat_count`     int          DEFAULT NULL COMMENT '已触发次数',
    `max_repeat_count` int          DEFAULT NULL COMMENT '最大触发次数',
    `status`           varchar(20)  NOT NULL COMMENT '等待状态（WAITING RUNNING CANCELLED RETRY DEAD）',
    `retry_count`      int          DEFAULT NULL COMMENT '已重试次数',
    `max_retry_count`  int          DEFAULT NULL COMMENT '最大重试次数',
    `retry_at`         datetime     DEFAULT NULL COMMENT '下次重试时间',
    `lock_owner`       varchar(64)  DEFAULT NULL COMMENT '抢占持有者标识',
    `lock_until`       datetime     DEFAULT NULL COMMENT '抢占租约到期时间',
    `version`          int          DEFAULT NULL COMMENT '乐观锁版本号',
    `payload`          text         COMMENT '消息事件接收的业务载荷',
    `last_error`       varchar(500) DEFAULT NULL COMMENT '最近一次失败原因摘要',
    `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `event_wait_status_next` (`status`, `next_trigger_at`, `id`) USING BTREE,
    KEY `event_wait_status_retry` (`status`, `retry_at`, `id`) USING BTREE,
    KEY `event_wait_tenant_status_next` (`tenant_id`, `status`, `next_trigger_at`, `id`) USING BTREE,
    KEY `event_wait_tenant_status_retry` (`tenant_id`, `status`, `retry_at`, `id`) USING BTREE,
    KEY `event_wait_instance_task` (`instance_id`, `attached_task_id`) USING BTREE,
    KEY `event_wait_name_instance_status` (`event_name`, `instance_id`, `status`) USING BTREE,
    KEY `event_wait_def_node_status` (`definition_id`, `node_code`, `status`) USING BTREE
) ENGINE = InnoDB COMMENT ='事件等待表';

-- 7. 「流程监控」下补「定时任务」菜单 + 查询/手动补发按钮（与 script/sql/ry_workflow.sql 全量脚本一致）
--    先删后插保证可重复执行；重新执行后记得给角色重新勾选按钮权限
DELETE FROM sys_menu WHERE menu_id IN (1761400000000011901, 1761400000000011902, 1761400000000011903);
insert into sys_menu values (1761400000000011901, '定时任务', 1761400000000011630, 3, 'timer', 'workflow/timer/index', '', 'N', 'N', 'C', '0', '0', 'workflow:eventWait:list', 'time', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '定时任务与事件等待监控菜单');
insert into sys_menu values (1761400000000011902, '定时任务查询', 1761400000000011901, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:eventWait:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values (1761400000000011903, '定时任务手动补发', 1761400000000011901, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'workflow:eventWait:trigger', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
