-- pj_app_info definition

-- Drop table

-- DROP TABLE pj_app_info;

CREATE TABLE pj_app_info (
    id int8 NOT NULL,
    app_name varchar(255) NULL,
    current_server varchar(255) NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    "password" varchar(255) NULL,
    CONSTRAINT pj_app_info_pkey PRIMARY KEY (id),
    CONSTRAINT uidx01_app_info UNIQUE (app_name)
);

INSERT INTO pj_app_info VALUES(1, 'ruoyi-worker', '127.0.0.1:10010', '2023-06-13 16:32:59.263', '2023-07-04 17:25:49.798', '123456');



-- pj_container_info definition

-- Drop table

-- DROP TABLE pj_container_info;

CREATE TABLE pj_container_info (
    id int8 NOT NULL,
    app_id int8 NULL,
    container_name varchar(255) NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    last_deploy_time timestamp NULL,
    source_info varchar(255) NULL,
    source_type int4 NULL,
    status int4 NULL,
    "version" varchar(255) NULL,
    CONSTRAINT pj_container_info_pkey PRIMARY KEY (id)
);
CREATE INDEX idx01_container_info ON pj_container_info USING btree (app_id);


-- pj_instance_info definition

-- Drop table

-- DROP TABLE pj_instance_info;

CREATE TABLE pj_instance_info (
    id int8 NOT NULL,
    actual_trigger_time int8 NULL,
    app_id int8 NULL,
    expected_trigger_time int8 NULL,
    finished_time int8 NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    instance_id int8 NULL,
    instance_params oid NULL,
    job_id int8 NULL,
    job_params oid NULL,
    last_report_time int8 NULL,
    "result" oid NULL,
    running_times int8 NULL,
    status int4 NULL,
    task_tracker_address varchar(255) NULL,
    "type" int4 NULL,
    wf_instance_id int8 NULL,
    CONSTRAINT pj_instance_info_pkey PRIMARY KEY (id)
);
CREATE INDEX idx01_instance_info ON pj_instance_info USING btree (job_id, status);
CREATE INDEX idx02_instance_info ON pj_instance_info USING btree (app_id, status);
CREATE INDEX idx03_instance_info ON pj_instance_info USING btree (instance_id, status);


-- pj_job_info definition

-- Drop table

-- DROP TABLE pj_job_info;

CREATE TABLE pj_job_info (
    id int8 NOT NULL,
    alarm_config varchar(255) NULL,
    app_id int8 NULL,
    concurrency int4 NULL,
    designated_workers varchar(255) NULL,
    dispatch_strategy int4 NULL,
    execute_type int4 NULL,
    extra varchar(255) NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    instance_retry_num int4 NULL,
    instance_time_limit int8 NULL,
    job_description varchar(255) NULL,
    job_name varchar(255) NULL,
    job_params oid NULL,
    lifecycle varchar(255) NULL,
    log_config varchar(255) NULL,
    max_instance_num int4 NULL,
    max_worker_count int4 NULL,
    min_cpu_cores float8 NOT NULL,
    min_disk_space float8 NOT NULL,
    min_memory_space float8 NOT NULL,
    next_trigger_time int8 NULL,
    notify_user_ids varchar(255) NULL,
    processor_info varchar(255) NULL,
    processor_type int4 NULL,
    status int4 NULL,
    tag varchar(255) NULL,
    task_retry_num int4 NULL,
    time_expression varchar(255) NULL,
    time_expression_type int4 NULL,
    CONSTRAINT pj_job_info_pkey PRIMARY KEY (id)
);
CREATE INDEX idx01_job_info ON pj_job_info USING btree (app_id, status, time_expression_type, next_trigger_time);


INSERT INTO pj_job_info VALUES(1, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 2, 1, NULL, '2023-06-02 15:01:27.717', '2023-07-04 17:22:12.374', 1, 0, '', '单机处理器执行测试', NULL, '{}', '{\"type\":1}', 0, 0, 0.0, 0.0, 0.0, NULL, NULL, 'org.dromara.job.processors.StandaloneProcessorDemo', 1, 2, NULL, 1, '30000', 3);
INSERT INTO pj_job_info VALUES(2, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 1, 2, NULL, '2023-06-02 15:04:45.342', '2023-07-04 17:22:12.816', 0, 0, NULL, '广播处理器测试', NULL, '{}', '{\"type\":1}', 0, 0, 0.0, 0.0, 0.0, NULL, NULL, 'org.dromara.job.processors.BroadcastProcessorDemo', 1, 2, NULL, 1, '30000', 3);
INSERT INTO pj_job_info VALUES(3, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 1, 4, NULL, '2023-06-02 15:13:23.519', '2023-06-02 16:03:22.421', 0, 0, NULL, 'Map处理器测试', NULL, '{}', '{\"type\":1}', 0, 0, 0.0, 0.0, 0.0, NULL, NULL, 'org.dromara.job.processors.MapProcessorDemo', 1, 2, NULL, 1, '1000', 3);
INSERT INTO pj_job_info VALUES(4, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 1, 3, NULL, '2023-06-02 15:45:25.896', '2023-06-02 16:03:23.125', 0, 0, NULL, 'MapReduce处理器测试', NULL, '{}', '{\"type\":1}', 0, 0, 0.0, 0.0, 0.0, NULL, NULL, 'org.dromara.job.processors.MapReduceProcessorDemo', 1, 2, NULL, 1, '1000', 3);


-- pj_oms_lock definition

-- Drop table

-- DROP TABLE pj_oms_lock;

CREATE TABLE pj_oms_lock (
    id int8 NOT NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    lock_name varchar(255) NULL,
    max_lock_time int8 NULL,
    ownerip varchar(255) NULL,
    CONSTRAINT pj_oms_lock_pkey PRIMARY KEY (id),
    CONSTRAINT uidx01_oms_lock UNIQUE (lock_name)
);


-- pj_server_info definition

-- Drop table

-- DROP TABLE pj_server_info;

CREATE TABLE pj_server_info (
    id int8 NOT NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    ip varchar(255) NULL,
    CONSTRAINT pj_server_info_pkey PRIMARY KEY (id),
    CONSTRAINT uidx01_server_info UNIQUE (ip)
);
CREATE INDEX idx01_server_info ON pj_server_info USING btree (gmt_modified);


-- pj_user_info definition

-- Drop table

-- DROP TABLE pj_user_info;

CREATE TABLE pj_user_info (
    id int8 NOT NULL,
    email varchar(255) NULL,
    extra varchar(255) NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    "password" varchar(255) NULL,
    phone varchar(255) NULL,
    username varchar(255) NULL,
    web_hook varchar(255) NULL,
    CONSTRAINT pj_user_info_pkey PRIMARY KEY (id)
);
CREATE INDEX uidx01_user_info ON pj_user_info USING btree (username);
CREATE INDEX uidx02_user_info ON pj_user_info USING btree (email);


-- pj_workflow_info definition

-- Drop table

-- DROP TABLE pj_workflow_info;

CREATE TABLE pj_workflow_info (
    id int8 NOT NULL,
    app_id int8 NULL,
    extra varchar(255) NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    lifecycle varchar(255) NULL,
    max_wf_instance_num int4 NULL,
    next_trigger_time int8 NULL,
    notify_user_ids varchar(255) NULL,
    pedag oid NULL,
    status int4 NULL,
    time_expression varchar(255) NULL,
    time_expression_type int4 NULL,
    wf_description varchar(255) NULL,
    wf_name varchar(255) NULL,
    CONSTRAINT pj_workflow_info_pkey PRIMARY KEY (id)
);
CREATE INDEX idx01_workflow_info ON pj_workflow_info USING btree (app_id, status, time_expression_type, next_trigger_time);


-- pj_workflow_instance_info definition

-- Drop table

-- DROP TABLE pj_workflow_instance_info;

CREATE TABLE pj_workflow_instance_info (
    id int8 NOT NULL,
    actual_trigger_time int8 NULL,
    app_id int8 NULL,
    dag oid NULL,
    expected_trigger_time int8 NULL,
    finished_time int8 NULL,
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    parent_wf_instance_id int8 NULL,
    "result" oid NULL,
    status int4 NULL,
    wf_context oid NULL,
    wf_init_params oid NULL,
    wf_instance_id int8 NULL,
    workflow_id int8 NULL,
    CONSTRAINT pj_workflow_instance_info_pkey PRIMARY KEY (id),
    CONSTRAINT uidx01_wf_instance UNIQUE (wf_instance_id)
);
CREATE INDEX idx01_wf_instance ON pj_workflow_instance_info USING btree (workflow_id, status, app_id, expected_trigger_time);


-- pj_workflow_node_info definition

-- Drop table

-- DROP TABLE pj_workflow_node_info;

CREATE TABLE pj_workflow_node_info (
    id int8 NOT NULL,
    app_id int8 NOT NULL,
    "enable" bool NOT NULL,
    extra oid NULL,
    gmt_create timestamp NOT NULL,
    gmt_modified timestamp NOT NULL,
    job_id int8 NULL,
    node_name varchar(255) NULL,
    node_params oid NULL,
    skip_when_failed bool NOT NULL,
    "type" int4 NULL,
    workflow_id int8 NULL,
    CONSTRAINT pj_workflow_node_info_pkey PRIMARY KEY (id)
);
CREATE INDEX idx01_workflow_node_info ON pj_workflow_node_info USING btree (workflow_id, gmt_create);
