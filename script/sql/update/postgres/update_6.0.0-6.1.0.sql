-- RuoYi-Plus 6.0.0 -> 6.1.0 (PostgreSQL). Run on existing databases before enabling the Warm-Flow timer.
-- New databases use the PostgreSQL full schema. Refresh the dictionary cache after this script.

ALTER TABLE flow_node DROP COLUMN "version";
ALTER TABLE flow_definition ADD COLUMN def_config text;
ALTER TABLE flow_node ADD COLUMN node_config text, ADD COLUMN event_type varchar(40),
    ADD COLUMN event_name varchar(100), ADD COLUMN attached_to_node_code varchar(100),
    ADD COLUMN interrupting int2;
ALTER TABLE flow_his_task ADD COLUMN node_config text;
COMMENT ON COLUMN flow_definition.def_config IS '流程定义配置（自动审批等引擎配置与自定义扩展属性json）';
COMMENT ON COLUMN flow_node.node_config IS '节点配置（服务任务、事件等引擎节点配置json）';
COMMENT ON COLUMN flow_node.event_type IS '事件类型';
COMMENT ON COLUMN flow_node.event_name IS '事件名称（消息事件使用）';
COMMENT ON COLUMN flow_node.attached_to_node_code IS '边界事件附着的任务节点编码';
COMMENT ON COLUMN flow_node.interrupting IS '边界事件是否中断型';
COMMENT ON COLUMN flow_his_task.node_config IS '节点配置（服务任务、事件等引擎节点配置json）';

ALTER TABLE flow_his_task ALTER COLUMN cooperate_type DROP DEFAULT;
ALTER TABLE flow_his_task ALTER COLUMN cooperate_type TYPE varchar(20) USING cooperate_type::varchar(20);
UPDATE flow_his_task SET cooperate_type = CASE cooperate_type
    WHEN '2' THEN 'transfer' WHEN '3' THEN 'delegate' WHEN '6' THEN 'add_signer'
    WHEN '7' THEN 'remove_signer' ELSE 'none' END
WHERE cooperate_type IN ('0','1','2','3','4','5','6','7');
ALTER TABLE flow_his_task ALTER COLUMN cooperate_type SET DEFAULT 'none';
COMMENT ON COLUMN flow_his_task.cooperate_type IS '协作方式（none transfer delegate add_signer remove_signer）';
COMMENT ON COLUMN flow_instance.flow_status IS '实例状态（draft waiting termination invalid cancel finish back restart）';
COMMENT ON COLUMN flow_task.flow_status IS '任务状态（pending pass auto_pass back invalid take_back copy）';
COMMENT ON COLUMN flow_his_task.flow_status IS '任务状态（pending pass auto_pass back invalid take_back copy cancel）';
COMMENT ON COLUMN flow_user.type IS '人员类型（1审批人 2转办人 3委托人 4抄送人）';
COMMENT ON COLUMN flow_user.associated IS '关联id（待办任务id；抄送人记录为 flow_his_task.id）';

UPDATE flow_instance SET flow_status = CASE flow_status
    WHEN '0' THEN 'draft' WHEN '1' THEN 'waiting' WHEN '4' THEN 'termination'
    WHEN '5' THEN 'invalid' WHEN '6' THEN 'cancel' WHEN '7' THEN 'waiting'
    WHEN '8' THEN 'finish' WHEN '9' THEN 'back' WHEN '12' THEN 'restart'
    WHEN '13' THEN 'draft' END
WHERE flow_status IN ('0','1','4','5','6','7','8','9','12','13');
UPDATE flow_task SET flow_status = CASE flow_status
    WHEN '1' THEN 'pending' WHEN '2' THEN 'pass' WHEN '8' THEN 'pass'
    WHEN '9' THEN 'back' WHEN '10' THEN 'invalid' WHEN '11' THEN 'take_back' END
WHERE flow_status IN ('1','2','8','9','10','11');
UPDATE flow_his_task SET flow_status = CASE flow_status
    WHEN '1' THEN 'pending' WHEN '2' THEN 'pass' WHEN '8' THEN 'pass'
    WHEN '9' THEN 'back' WHEN '10' THEN 'invalid' WHEN '11' THEN 'take_back' END
WHERE flow_status IN ('1','2','8','9','10','11');

CREATE TABLE flow_event_wait (
    id int8 NOT NULL PRIMARY KEY, tenant_id varchar(40), del_flag bpchar(1) DEFAULT '0',
    definition_id int8, instance_id int8, node_code varchar(100),
    event_type varchar(40) NOT NULL, event_name varchar(100), attached_task_id int8,
    trigger_type varchar(40), trigger_interval varchar(100), next_trigger_at timestamp,
    timezone varchar(40), repeat_count int4, max_repeat_count int4,
    status varchar(20) NOT NULL, retry_count int4, max_retry_count int4,
    retry_at timestamp, lock_owner varchar(64), lock_until timestamp, version int4,
    payload text, last_error varchar(500), create_time timestamp, update_time timestamp
);
COMMENT ON TABLE flow_event_wait IS '事件等待表';
CREATE INDEX event_wait_status_next ON flow_event_wait (status, next_trigger_at, id);
CREATE INDEX event_wait_status_retry ON flow_event_wait (status, retry_at, id);
CREATE INDEX event_wait_tenant_status_next ON flow_event_wait (tenant_id, status, next_trigger_at, id);
CREATE INDEX event_wait_tenant_status_retry ON flow_event_wait (tenant_id, status, retry_at, id);
CREATE INDEX event_wait_instance_task ON flow_event_wait (instance_id, attached_task_id);
CREATE INDEX event_wait_name_instance_status ON flow_event_wait (event_name, instance_id, status);
CREATE INDEX event_wait_def_node_status ON flow_event_wait (definition_id, node_code, status);

DELETE FROM sys_dict_data WHERE dict_type = 'wf_task_status';
INSERT INTO sys_dict_data VALUES
 (1761600000000000060,1,'待审批','pending','wf_task_status','','primary','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'待审批'),
 (1761600000000000049,2,'通过','pass','wf_task_status','','success','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'通过'),
 (1761600000000000061,3,'自动审批','auto_pass','wf_task_status','','success','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'自动审批'),
 (1761600000000000056,4,'抄送','copy','wf_task_status','','primary','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'抄送'),
 (1761600000000000052,5,'退回','back','wf_task_status','','danger','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'退回'),
 (1761600000000000062,6,'拿回','take_back','wf_task_status','','warning','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'拿回'),
 (1761600000000000051,7,'失效','invalid','wf_task_status','','danger','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'失效'),
 (1761600000000000048,8,'撤销','cancel','wf_task_status','','danger','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'撤销'),
 (1761600000000000064,9,'转办','transfer','wf_task_status','','warning','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'转办'),
 (1761600000000000065,10,'委派','delegate','wf_task_status','','warning','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'委派'),
 (1761600000000000066,11,'加签','add_signer','wf_task_status','','warning','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'加签'),
 (1761600000000000067,12,'减签','remove_signer','wf_task_status','','warning','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'减签'),
 (1761600000000000068,13,'已终止','termination','wf_task_status','','danger','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'终止归档留痕');
INSERT INTO sys_dict_data
SELECT 1761600000000000063,8,'重启','restart','wf_business_status','','info','N',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'重启'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='wf_business_status' AND dict_value='restart');

DELETE FROM sys_menu WHERE menu_id IN (1761400000000011901,1761400000000011902,1761400000000011903);
INSERT INTO sys_menu VALUES (1761400000000011901,'定时任务',1761400000000011630,3,'timer','workflow/timer/index','','N','N','C','0','0','workflow:eventWait:list','time','','',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'定时任务与事件等待监控菜单');
INSERT INTO sys_menu VALUES (1761400000000011902,'定时任务查询',1761400000000011901,1,'#','','','N','Y','F','0','0','workflow:eventWait:query','#','','',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'');
INSERT INTO sys_menu VALUES (1761400000000011903,'定时任务手动补发',1761400000000011901,2,'#','','','N','Y','F','0','0','workflow:eventWait:trigger','#','','',1761000000000000103,1761100000000000001,CURRENT_TIMESTAMP,NULL,NULL,'');

-- Inspect before deciding whether old 2.1.0 waiting tasks should become pending.
SELECT id, instance_id, node_code, flow_status FROM flow_task WHERE flow_status = 'waiting';
