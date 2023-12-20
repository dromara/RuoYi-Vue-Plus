-- ----------------------------
-- Table structure for pj_app_info
-- ----------------------------

CREATE TABLE [pj_app_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [app_name] varchar(255)   NULL,
  [current_server] varchar(255)   NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [password] varchar(255)   NULL
)
GO

ALTER TABLE [pj_app_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Records of [pj_app_info]
-- ----------------------------
SET IDENTITY_INSERT [pj_app_info] ON
GO

INSERT INTO [pj_app_info] ([id], [app_name], [current_server], [gmt_create], [gmt_modified], [password]) VALUES (N'1', N'ruoyi-worker', N'192.168.31.100:10010', N'2023-06-13 16:32:59.2630000', N'2023-07-04 17:25:49.7980000', N'123456')
GO

SET IDENTITY_INSERT [pj_app_info] OFF
GO


-- ----------------------------
-- Table structure for pj_container_info
-- ----------------------------

CREATE TABLE [pj_container_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [app_id] bigint  NULL,
  [container_name] varchar(255)   NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [last_deploy_time] datetime2(7)  NULL,
  [source_info] varchar(255)   NULL,
  [source_type] int  NULL,
  [status] int  NULL,
  [version] varchar(255)   NULL
)
GO

ALTER TABLE [pj_container_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_instance_info
-- ----------------------------

CREATE TABLE [pj_instance_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [actual_trigger_time] bigint  NULL,
  [app_id] bigint  NULL,
  [expected_trigger_time] bigint  NULL,
  [finished_time] bigint  NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [instance_id] bigint  NULL,
  [instance_params] varchar(max)   NULL,
  [job_id] bigint  NULL,
  [job_params] varchar(max)   NULL,
  [last_report_time] bigint  NULL,
  [result] varchar(max)   NULL,
  [running_times] bigint  NULL,
  [status] int  NULL,
  [task_tracker_address] varchar(255)   NULL,
  [type] int  NULL,
  [wf_instance_id] bigint  NULL
)
GO

ALTER TABLE [pj_instance_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_job_info
-- ----------------------------

CREATE TABLE [pj_job_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [alarm_config] varchar(255)   NULL,
  [app_id] bigint  NULL,
  [concurrency] int  NULL,
  [designated_workers] varchar(255)   NULL,
  [dispatch_strategy] int  NULL,
  [execute_type] int  NULL,
  [extra] varchar(255)   NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [instance_retry_num] int  NULL,
  [instance_time_limit] bigint  NULL,
  [job_description] varchar(255)   NULL,
  [job_name] varchar(255)   NULL,
  [job_params] varchar(max)   NULL,
  [lifecycle] varchar(255)   NULL,
  [log_config] varchar(255)   NULL,
  [max_instance_num] int  NULL,
  [max_worker_count] int  NULL,
  [min_cpu_cores] float(53)  NOT NULL,
  [min_disk_space] float(53)  NOT NULL,
  [min_memory_space] float(53)  NOT NULL,
  [next_trigger_time] bigint  NULL,
  [notify_user_ids] varchar(255)   NULL,
  [processor_info] varchar(255)   NULL,
  [processor_type] int  NULL,
  [status] int  NULL,
  [tag] varchar(255)   NULL,
  [task_retry_num] int  NULL,
  [time_expression] varchar(255)   NULL,
  [time_expression_type] int  NULL
)
GO

ALTER TABLE [pj_job_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Records of [pj_job_info]
-- ----------------------------
SET IDENTITY_INSERT [pj_job_info] ON
GO

INSERT INTO [pj_job_info] ([id], [alarm_config], [app_id], [concurrency], [designated_workers], [dispatch_strategy], [execute_type], [extra], [gmt_create], [gmt_modified], [instance_retry_num], [instance_time_limit], [job_description], [job_name], [job_params], [lifecycle], [log_config], [max_instance_num], [max_worker_count], [min_cpu_cores], [min_disk_space], [min_memory_space], [next_trigger_time], [notify_user_ids], [processor_info], [processor_type], [status], [tag], [task_retry_num], [time_expression], [time_expression_type]) VALUES (N'1', N'{"alertThreshold":0,"silenceWindowLen":0,"statisticWindowLen":0}', N'1', N'5', N'', N'2', N'1', NULL, N'2023-06-02 15:01:27.7170000', N'2023-07-04 17:22:12.3740000', N'1', N'0', N'', N'?????????', NULL, N'{}', N'{"type":1}', N'0', N'0', N'0.000000000000000', N'0.000000000000000', N'0.000000000000000', NULL, NULL, N'org.dromara.job.processors.StandaloneProcessorDemo', N'1', N'2', NULL, N'1', N'30000', N'3')
GO

INSERT INTO [pj_job_info] ([id], [alarm_config], [app_id], [concurrency], [designated_workers], [dispatch_strategy], [execute_type], [extra], [gmt_create], [gmt_modified], [instance_retry_num], [instance_time_limit], [job_description], [job_name], [job_params], [lifecycle], [log_config], [max_instance_num], [max_worker_count], [min_cpu_cores], [min_disk_space], [min_memory_space], [next_trigger_time], [notify_user_ids], [processor_info], [processor_type], [status], [tag], [task_retry_num], [time_expression], [time_expression_type]) VALUES (N'2', N'{"alertThreshold":0,"silenceWindowLen":0,"statisticWindowLen":0}', N'1', N'5', N'', N'1', N'2', NULL, N'2023-06-02 15:04:45.3420000', N'2023-07-04 17:22:12.8160000', N'0', N'0', NULL, N'???????', NULL, N'{}', N'{"type":1}', N'0', N'0', N'0.000000000000000', N'0.000000000000000', N'0.000000000000000', NULL, NULL, N'org.dromara.job.processors.BroadcastProcessorDemo', N'1', N'2', NULL, N'1', N'30000', N'3')
GO

INSERT INTO [pj_job_info] ([id], [alarm_config], [app_id], [concurrency], [designated_workers], [dispatch_strategy], [execute_type], [extra], [gmt_create], [gmt_modified], [instance_retry_num], [instance_time_limit], [job_description], [job_name], [job_params], [lifecycle], [log_config], [max_instance_num], [max_worker_count], [min_cpu_cores], [min_disk_space], [min_memory_space], [next_trigger_time], [notify_user_ids], [processor_info], [processor_type], [status], [tag], [task_retry_num], [time_expression], [time_expression_type]) VALUES (N'3', N'{"alertThreshold":0,"silenceWindowLen":0,"statisticWindowLen":0}', N'1', N'5', N'', N'1', N'4', NULL, N'2023-06-02 15:13:23.5190000', N'2023-06-02 16:03:22.4210000', N'0', N'0', NULL, N'Map?????', NULL, N'{}', N'{"type":1}', N'0', N'0', N'0.000000000000000', N'0.000000000000000', N'0.000000000000000', NULL, NULL, N'org.dromara.job.processors.MapProcessorDemo', N'1', N'2', NULL, N'1', N'1000', N'3')
GO

INSERT INTO [pj_job_info] ([id], [alarm_config], [app_id], [concurrency], [designated_workers], [dispatch_strategy], [execute_type], [extra], [gmt_create], [gmt_modified], [instance_retry_num], [instance_time_limit], [job_description], [job_name], [job_params], [lifecycle], [log_config], [max_instance_num], [max_worker_count], [min_cpu_cores], [min_disk_space], [min_memory_space], [next_trigger_time], [notify_user_ids], [processor_info], [processor_type], [status], [tag], [task_retry_num], [time_expression], [time_expression_type]) VALUES (N'4', N'{"alertThreshold":0,"silenceWindowLen":0,"statisticWindowLen":0}', N'1', N'5', N'', N'1', N'3', NULL, N'2023-06-02 15:45:25.8960000', N'2023-06-02 16:03:23.1250000', N'0', N'0', NULL, N'MapReduce?????', NULL, N'{}', N'{"type":1}', N'0', N'0', N'0.000000000000000', N'0.000000000000000', N'0.000000000000000', NULL, NULL, N'org.dromara.job.processors.MapReduceProcessorDemo', N'1', N'2', NULL, N'1', N'1000', N'3')
GO

SET IDENTITY_INSERT [pj_job_info] OFF
GO


-- ----------------------------
-- Table structure for pj_oms_lock
-- ----------------------------

CREATE TABLE [pj_oms_lock] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [lock_name] varchar(255)   NULL,
  [max_lock_time] bigint  NULL,
  [ownerip] varchar(255)   NULL
)
GO

ALTER TABLE [pj_oms_lock] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_server_info
-- ----------------------------

CREATE TABLE [pj_server_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [ip] varchar(255)   NULL
)
GO

ALTER TABLE [pj_server_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_user_info
-- ----------------------------

CREATE TABLE [pj_user_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [email] varchar(255)   NULL,
  [extra] varchar(255)   NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [password] varchar(255)   NULL,
  [phone] varchar(255)   NULL,
  [username] varchar(255)   NULL,
  [web_hook] varchar(255)   NULL
)
GO

ALTER TABLE [pj_user_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_workflow_info
-- ----------------------------

CREATE TABLE [pj_workflow_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [app_id] bigint  NULL,
  [extra] varchar(255)   NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [lifecycle] varchar(255)   NULL,
  [max_wf_instance_num] int  NULL,
  [next_trigger_time] bigint  NULL,
  [notify_user_ids] varchar(255)   NULL,
  [pedag] varchar(max)   NULL,
  [status] int  NULL,
  [time_expression] varchar(255)   NULL,
  [time_expression_type] int  NULL,
  [wf_description] varchar(255)   NULL,
  [wf_name] varchar(255)   NULL
)
GO

ALTER TABLE [pj_workflow_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_workflow_instance_info
-- ----------------------------

CREATE TABLE [pj_workflow_instance_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [actual_trigger_time] bigint  NULL,
  [app_id] bigint  NULL,
  [dag] varchar(max)   NULL,
  [expected_trigger_time] bigint  NULL,
  [finished_time] bigint  NULL,
  [gmt_create] datetime2(7)  NULL,
  [gmt_modified] datetime2(7)  NULL,
  [parent_wf_instance_id] bigint  NULL,
  [result] varchar(max)   NULL,
  [status] int  NULL,
  [wf_context] varchar(max)   NULL,
  [wf_init_params] varchar(max)   NULL,
  [wf_instance_id] bigint  NULL,
  [workflow_id] bigint  NULL
)
GO

ALTER TABLE [pj_workflow_instance_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Table structure for pj_workflow_node_info
-- ----------------------------

CREATE TABLE [pj_workflow_node_info] (
  [id] bigint  IDENTITY(1,1) NOT NULL,
  [app_id] bigint  NOT NULL,
  [enable] bit  NOT NULL,
  [extra] varchar(max)   NULL,
  [gmt_create] datetime2(7)  NOT NULL,
  [gmt_modified] datetime2(7)  NOT NULL,
  [job_id] bigint  NULL,
  [node_name] varchar(255)   NULL,
  [node_params] varchar(max)   NULL,
  [skip_when_failed] bit  NOT NULL,
  [type] int  NULL,
  [workflow_id] bigint  NULL
)
GO

ALTER TABLE [pj_workflow_node_info] SET (LOCK_ESCALATION = TABLE)
GO


-- ----------------------------
-- Uniques structure for table pj_app_info
-- ----------------------------
ALTER TABLE [pj_app_info] ADD CONSTRAINT [uidx01_app_info] UNIQUE NONCLUSTERED ([app_name] ASC)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Primary Key structure for table pj_app_info
-- ----------------------------
ALTER TABLE [pj_app_info] ADD CONSTRAINT [PK__pj_app_i__3213E83FDD7E2005] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_container_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_container_info]
ON [pj_container_info] (
  [app_id] ASC
)
GO


-- ----------------------------
-- Primary Key structure for table pj_container_info
-- ----------------------------
ALTER TABLE [pj_container_info] ADD CONSTRAINT [PK__pj_conta__3213E83FE1AAA8BE] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_instance_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_instance_info]
ON [pj_instance_info] (
  [job_id] ASC,
  [status] ASC
)
GO

CREATE NONCLUSTERED INDEX [idx02_instance_info]
ON [pj_instance_info] (
  [app_id] ASC,
  [status] ASC
)
GO

CREATE NONCLUSTERED INDEX [idx03_instance_info]
ON [pj_instance_info] (
  [instance_id] ASC,
  [status] ASC
)
GO


-- ----------------------------
-- Primary Key structure for table pj_instance_info
-- ----------------------------
ALTER TABLE [pj_instance_info] ADD CONSTRAINT [PK__pj_insta__3213E83F6F188642] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_job_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_job_info]
ON [pj_job_info] (
  [app_id] ASC,
  [status] ASC,
  [time_expression_type] ASC,
  [next_trigger_time] ASC
)
GO


-- ----------------------------
-- Primary Key structure for table pj_job_info
-- ----------------------------
ALTER TABLE [pj_job_info] ADD CONSTRAINT [PK__pj_job_i__3213E83FBFBCD483] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Uniques structure for table pj_oms_lock
-- ----------------------------
ALTER TABLE [pj_oms_lock] ADD CONSTRAINT [uidx01_oms_lock] UNIQUE NONCLUSTERED ([lock_name] ASC)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Primary Key structure for table pj_oms_lock
-- ----------------------------
ALTER TABLE [pj_oms_lock] ADD CONSTRAINT [PK__pj_oms_l__3213E83F31F31A08] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_server_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_server_info]
ON [pj_server_info] (
  [gmt_modified] ASC
)
GO


-- ----------------------------
-- Uniques structure for table pj_server_info
-- ----------------------------
ALTER TABLE [pj_server_info] ADD CONSTRAINT [uidx01_server_info] UNIQUE NONCLUSTERED ([ip] ASC)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Primary Key structure for table pj_server_info
-- ----------------------------
ALTER TABLE [pj_server_info] ADD CONSTRAINT [PK__pj_serve__3213E83F75246E89] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_user_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [uidx01_user_info]
ON [pj_user_info] (
  [username] ASC
)
GO

CREATE NONCLUSTERED INDEX [uidx02_user_info]
ON [pj_user_info] (
  [email] ASC
)
GO


-- ----------------------------
-- Primary Key structure for table pj_user_info
-- ----------------------------
ALTER TABLE [pj_user_info] ADD CONSTRAINT [PK__pj_user___3213E83FB78DE8FD] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_workflow_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_workflow_info]
ON [pj_workflow_info] (
  [app_id] ASC,
  [status] ASC,
  [time_expression_type] ASC,
  [next_trigger_time] ASC
)
GO


-- ----------------------------
-- Primary Key structure for table pj_workflow_info
-- ----------------------------
ALTER TABLE [pj_workflow_info] ADD CONSTRAINT [PK__pj_workf__3213E83F790DC98A] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_workflow_instance_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_wf_instance]
ON [pj_workflow_instance_info] (
  [workflow_id] ASC,
  [status] ASC,
  [app_id] ASC,
  [expected_trigger_time] ASC
)
GO


-- ----------------------------
-- Uniques structure for table pj_workflow_instance_info
-- ----------------------------
ALTER TABLE [pj_workflow_instance_info] ADD CONSTRAINT [uidx01_wf_instance] UNIQUE NONCLUSTERED ([wf_instance_id] ASC)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Primary Key structure for table pj_workflow_instance_info
-- ----------------------------
ALTER TABLE [pj_workflow_instance_info] ADD CONSTRAINT [PK__pj_workf__3213E83F5AF8A72D] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO


-- ----------------------------
-- Indexes structure for table pj_workflow_node_info
-- ----------------------------
CREATE NONCLUSTERED INDEX [idx01_workflow_node_info]
ON [pj_workflow_node_info] (
  [workflow_id] ASC,
  [gmt_create] ASC
)
GO


-- ----------------------------
-- Primary Key structure for table pj_workflow_node_info
-- ----------------------------
ALTER TABLE [pj_workflow_node_info] ADD CONSTRAINT [PK__pj_workf__3213E83FD000EE6D] PRIMARY KEY CLUSTERED ([id])
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
ON [PRIMARY]
GO

