-- RuoYi-Vue-Plus 6.0.0 -> 6.1.0 (SQL Server). Run on existing databases before enabling the Warm-Flow timer.
-- New databases use the SQL Server full schema. Refresh the dictionary cache after this script.

ALTER TABLE flow_node DROP COLUMN version;
ALTER TABLE flow_definition ADD def_config nvarchar(max) NULL;
ALTER TABLE flow_node ADD node_config nvarchar(max) NULL, event_type nvarchar(40) NULL,
    event_name nvarchar(100) NULL, attached_to_node_code nvarchar(100) NULL,
    interrupting tinyint NULL;
ALTER TABLE flow_his_task ADD node_config nvarchar(max) NULL;
GO

-- Drop the old system-named DEFAULT constraint before changing tinyint to varchar.
DECLARE @constraint_name sysname;
SELECT @constraint_name = dc.name FROM sys.default_constraints dc
JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID(N'dbo.flow_his_task') AND c.name = N'cooperate_type';
IF @constraint_name IS NOT NULL
    EXEC(N'ALTER TABLE dbo.flow_his_task DROP CONSTRAINT ' + QUOTENAME(@constraint_name));
UPDATE flow_his_task SET cooperate_type = 0 WHERE cooperate_type IS NULL;
ALTER TABLE flow_his_task ALTER COLUMN cooperate_type varchar(20) NOT NULL;
ALTER TABLE flow_his_task ADD CONSTRAINT DF_flow_his_task_cooperate_type DEFAULT ('none') FOR cooperate_type;
GO
UPDATE flow_his_task SET cooperate_type = CASE cooperate_type
    WHEN '2' THEN 'transfer' WHEN '3' THEN 'delegate' WHEN '6' THEN 'add_signer'
    WHEN '7' THEN 'remove_signer' ELSE 'none' END
WHERE cooperate_type IN ('0','1','2','3','4','5','6','7');

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
GO

CREATE TABLE flow_event_wait (
    id bigint NOT NULL, tenant_id nvarchar(40) NULL, del_flag nchar(1) DEFAULT('0') NULL,
    definition_id bigint NULL, instance_id bigint NULL, node_code nvarchar(100) NULL,
    event_type nvarchar(40) NOT NULL, event_name nvarchar(100) NULL, attached_task_id bigint NULL,
    trigger_type nvarchar(40) NULL, trigger_interval nvarchar(100) NULL, next_trigger_at datetime2(7) NULL,
    timezone nvarchar(40) NULL, repeat_count int NULL, max_repeat_count int NULL,
    status nvarchar(20) NOT NULL, retry_count int NULL, max_retry_count int NULL,
    retry_at datetime2(7) NULL, lock_owner nvarchar(64) NULL, lock_until datetime2(7) NULL,
    version int NULL, payload nvarchar(max) NULL, last_error nvarchar(500) NULL,
    create_time datetime2(7) NULL, update_time datetime2(7) NULL,
    CONSTRAINT PK_flow_event_wait PRIMARY KEY CLUSTERED (id)
);
GO
CREATE NONCLUSTERED INDEX event_wait_status_next ON flow_event_wait (status, next_trigger_at, id);
CREATE NONCLUSTERED INDEX event_wait_status_retry ON flow_event_wait (status, retry_at, id);
CREATE NONCLUSTERED INDEX event_wait_tenant_status_next ON flow_event_wait (tenant_id, status, next_trigger_at, id);
CREATE NONCLUSTERED INDEX event_wait_tenant_status_retry ON flow_event_wait (tenant_id, status, retry_at, id);
CREATE NONCLUSTERED INDEX event_wait_instance_task ON flow_event_wait (instance_id, attached_task_id);
CREATE NONCLUSTERED INDEX event_wait_name_instance_status ON flow_event_wait (event_name, instance_id, status);
CREATE NONCLUSTERED INDEX event_wait_def_node_status ON flow_event_wait (definition_id, node_code, status);
GO

DELETE FROM sys_dict_data WHERE dict_type = N'wf_task_status';
INSERT INTO sys_dict_data VALUES
 (1761600000000000060,1,N'待审批',N'pending',N'wf_task_status',N'',N'primary',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'待审批'),
 (1761600000000000049,2,N'通过',N'pass',N'wf_task_status',N'',N'success',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'通过'),
 (1761600000000000061,3,N'自动审批',N'auto_pass',N'wf_task_status',N'',N'success',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'自动审批'),
 (1761600000000000056,4,N'抄送',N'copy',N'wf_task_status',N'',N'primary',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'抄送'),
 (1761600000000000052,5,N'退回',N'back',N'wf_task_status',N'',N'danger',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'退回'),
 (1761600000000000062,6,N'拿回',N'take_back',N'wf_task_status',N'',N'warning',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'拿回'),
 (1761600000000000051,7,N'失效',N'invalid',N'wf_task_status',N'',N'danger',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'失效'),
 (1761600000000000048,8,N'撤销',N'cancel',N'wf_task_status',N'',N'danger',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'撤销'),
 (1761600000000000064,9,N'转办',N'transfer',N'wf_task_status',N'',N'warning',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'转办'),
 (1761600000000000065,10,N'委派',N'delegate',N'wf_task_status',N'',N'warning',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'委派'),
 (1761600000000000066,11,N'加签',N'add_signer',N'wf_task_status',N'',N'warning',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'加签'),
 (1761600000000000067,12,N'减签',N'remove_signer',N'wf_task_status',N'',N'warning',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'减签'),
 (1761600000000000068,13,N'已终止',N'termination',N'wf_task_status',N'',N'danger',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'终止归档留痕');
INSERT INTO sys_dict_data
SELECT 1761600000000000063,8,N'重启',N'restart',N'wf_business_status',N'',N'info',N'N',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'重启'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type=N'wf_business_status' AND dict_value=N'restart');

DELETE FROM sys_menu WHERE menu_id IN (1761400000000011901,1761400000000011902,1761400000000011903);
INSERT INTO sys_menu VALUES (1761400000000011901,N'定时任务',1761400000000011630,3,N'timer',N'workflow/timer/index',N'',N'N',N'N',N'C',N'0',N'0',N'workflow:eventWait:list',N'time',N'',N'',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'定时任务与事件等待监控菜单');
INSERT INTO sys_menu VALUES (1761400000000011902,N'定时任务查询',1761400000000011901,1,N'#',N'',N'',N'N',N'Y',N'F',N'0',N'0',N'workflow:eventWait:query',N'#',N'',N'',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'');
INSERT INTO sys_menu VALUES (1761400000000011903,N'定时任务手动补发',1761400000000011901,2,N'#',N'',N'',N'N',N'Y',N'F',N'0',N'0',N'workflow:eventWait:trigger',N'#',N'',N'',1761000000000000103,1761100000000000001,GETDATE(),NULL,NULL,N'');
GO

-- Inspect before deciding whether old 2.1.0 waiting tasks should become pending.
SELECT id, instance_id, node_code, flow_status FROM flow_task WHERE flow_status = N'waiting';
