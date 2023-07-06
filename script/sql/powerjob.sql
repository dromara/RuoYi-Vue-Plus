-- ----------------------------
-- Table structure for pj_app_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_app_info`;
CREATE TABLE `pj_app_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `app_name` varchar(255) NULL DEFAULT NULL,
    `current_server` varchar(255) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `password` varchar(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uidx01_app_info`(`app_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pj_app_info
-- ----------------------------
INSERT INTO `pj_app_info` VALUES (1, 'ruoyi-worker', '127.0.0.1:10010', '2023-06-13 16:32:59.263000', '2023-07-04 17:25:49.798000', '123456');

-- ----------------------------
-- Table structure for pj_container_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_container_info`;
CREATE TABLE `pj_container_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `app_id` bigint(0) NULL DEFAULT NULL,
    `container_name` varchar(255) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `last_deploy_time` datetime(6) NULL DEFAULT NULL,
    `source_info` varchar(255) NULL DEFAULT NULL,
    `source_type` int(0) NULL DEFAULT NULL,
    `status` int(0) NULL DEFAULT NULL,
    `version` varchar(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx01_container_info`(`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_instance_info`;
CREATE TABLE `pj_instance_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `actual_trigger_time` bigint(0) NULL DEFAULT NULL,
    `app_id` bigint(0) NULL DEFAULT NULL,
    `expected_trigger_time` bigint(0) NULL DEFAULT NULL,
    `finished_time` bigint(0) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `instance_id` bigint(0) NULL DEFAULT NULL,
    `instance_params` longtext NULL,
    `job_id` bigint(0) NULL DEFAULT NULL,
    `job_params` longtext NULL,
    `last_report_time` bigint(0) NULL DEFAULT NULL,
    `result` longtext NULL,
    `running_times` bigint(0) NULL DEFAULT NULL,
    `status` int(0) NULL DEFAULT NULL,
    `task_tracker_address` varchar(255) NULL DEFAULT NULL,
    `type` int(0) NULL DEFAULT NULL,
    `wf_instance_id` bigint(0) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx01_instance_info`(`job_id`, `status`) USING BTREE,
    INDEX `idx02_instance_info`(`app_id`, `status`) USING BTREE,
    INDEX `idx03_instance_info`(`instance_id`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_job_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_job_info`;
CREATE TABLE `pj_job_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `alarm_config` varchar(255) NULL DEFAULT NULL,
    `app_id` bigint(0) NULL DEFAULT NULL,
    `concurrency` int(0) NULL DEFAULT NULL,
    `designated_workers` varchar(255) NULL DEFAULT NULL,
    `dispatch_strategy` int(0) NULL DEFAULT NULL,
    `execute_type` int(0) NULL DEFAULT NULL,
    `extra` varchar(255) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `instance_retry_num` int(0) NULL DEFAULT NULL,
    `instance_time_limit` bigint(0) NULL DEFAULT NULL,
    `job_description` varchar(255) NULL DEFAULT NULL,
    `job_name` varchar(255) NULL DEFAULT NULL,
    `job_params` longtext NULL,
    `lifecycle` varchar(255) NULL DEFAULT NULL,
    `log_config` varchar(255) NULL DEFAULT NULL,
    `max_instance_num` int(0) NULL DEFAULT NULL,
    `max_worker_count` int(0) NULL DEFAULT NULL,
    `min_cpu_cores` double NOT NULL,
    `min_disk_space` double NOT NULL,
    `min_memory_space` double NOT NULL,
    `next_trigger_time` bigint(0) NULL DEFAULT NULL,
    `notify_user_ids` varchar(255) NULL DEFAULT NULL,
    `processor_info` varchar(255) NULL DEFAULT NULL,
    `processor_type` int(0) NULL DEFAULT NULL,
    `status` int(0) NULL DEFAULT NULL,
    `tag` varchar(255) NULL DEFAULT NULL,
    `task_retry_num` int(0) NULL DEFAULT NULL,
    `time_expression` varchar(255) NULL DEFAULT NULL,
    `time_expression_type` int(0) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx01_job_info`(`app_id`, `status`, `time_expression_type`, `next_trigger_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pj_job_info
-- ----------------------------
INSERT INTO `pj_job_info` VALUES (1, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 2, 1, NULL, '2023-06-02 15:01:27.717000', '2023-07-04 17:22:12.374000', 1, 0, '', '单机处理器执行测试', NULL, '{}', '{\"type\":1}', 0, 0, 0, 0, 0, NULL, NULL, 'org.dromara.job.processors.StandaloneProcessorDemo', 1, 2, NULL, 1, '30000', 3);
INSERT INTO `pj_job_info` VALUES (2, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 1, 2, NULL, '2023-06-02 15:04:45.342000', '2023-07-04 17:22:12.816000', 0, 0, NULL, '广播处理器测试', NULL, '{}', '{\"type\":1}', 0, 0, 0, 0, 0, NULL, NULL, 'org.dromara.job.processors.BroadcastProcessorDemo', 1, 2, NULL, 1, '30000', 3);
INSERT INTO `pj_job_info` VALUES (3, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 1, 4, NULL, '2023-06-02 15:13:23.519000', '2023-06-02 16:03:22.421000', 0, 0, NULL, 'Map处理器测试', NULL, '{}', '{\"type\":1}', 0, 0, 0, 0, 0, NULL, NULL, 'org.dromara.job.processors.MapProcessorDemo', 1, 2, NULL, 1, '1000', 3);
INSERT INTO `pj_job_info` VALUES (4, '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}', 1, 5, '', 1, 3, NULL, '2023-06-02 15:45:25.896000', '2023-06-02 16:03:23.125000', 0, 0, NULL, 'MapReduce处理器测试', NULL, '{}', '{\"type\":1}', 0, 0, 0, 0, 0, NULL, NULL, 'org.dromara.job.processors.MapReduceProcessorDemo', 1, 2, NULL, 1, '1000', 3);

-- ----------------------------
-- Table structure for pj_oms_lock
-- ----------------------------
DROP TABLE IF EXISTS `pj_oms_lock`;
CREATE TABLE `pj_oms_lock`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `lock_name` varchar(255) NULL DEFAULT NULL,
    `max_lock_time` bigint(0) NULL DEFAULT NULL,
    `ownerip` varchar(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uidx01_oms_lock`(`lock_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_server_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_server_info`;
CREATE TABLE `pj_server_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `ip` varchar(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uidx01_server_info`(`ip`) USING BTREE,
    INDEX `idx01_server_info`(`gmt_modified`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_user_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_user_info`;
CREATE TABLE `pj_user_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `email` varchar(255) NULL DEFAULT NULL,
    `extra` varchar(255) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `password` varchar(255) NULL DEFAULT NULL,
    `phone` varchar(255) NULL DEFAULT NULL,
    `username` varchar(255) NULL DEFAULT NULL,
    `web_hook` varchar(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `uidx01_user_info`(`username`) USING BTREE,
    INDEX `uidx02_user_info`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_workflow_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_workflow_info`;
CREATE TABLE `pj_workflow_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `app_id` bigint(0) NULL DEFAULT NULL,
    `extra` varchar(255) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `lifecycle` varchar(255) NULL DEFAULT NULL,
    `max_wf_instance_num` int(0) NULL DEFAULT NULL,
    `next_trigger_time` bigint(0) NULL DEFAULT NULL,
    `notify_user_ids` varchar(255) NULL DEFAULT NULL,
    `pedag` longtext NULL,
    `status` int(0) NULL DEFAULT NULL,
    `time_expression` varchar(255) NULL DEFAULT NULL,
    `time_expression_type` int(0) NULL DEFAULT NULL,
    `wf_description` varchar(255) NULL DEFAULT NULL,
    `wf_name` varchar(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx01_workflow_info`(`app_id`, `status`, `time_expression_type`, `next_trigger_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_workflow_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_workflow_instance_info`;
CREATE TABLE `pj_workflow_instance_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `actual_trigger_time` bigint(0) NULL DEFAULT NULL,
    `app_id` bigint(0) NULL DEFAULT NULL,
    `dag` longtext NULL,
    `expected_trigger_time` bigint(0) NULL DEFAULT NULL,
    `finished_time` bigint(0) NULL DEFAULT NULL,
    `gmt_create` datetime(6) NULL DEFAULT NULL,
    `gmt_modified` datetime(6) NULL DEFAULT NULL,
    `parent_wf_instance_id` bigint(0) NULL DEFAULT NULL,
    `result` longtext NULL,
    `status` int(0) NULL DEFAULT NULL,
    `wf_context` longtext NULL,
    `wf_init_params` longtext NULL,
    `wf_instance_id` bigint(0) NULL DEFAULT NULL,
    `workflow_id` bigint(0) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uidx01_wf_instance`(`wf_instance_id`) USING BTREE,
    INDEX `idx01_wf_instance`(`workflow_id`, `status`, `app_id`, `expected_trigger_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pj_workflow_node_info
-- ----------------------------
DROP TABLE IF EXISTS `pj_workflow_node_info`;
CREATE TABLE `pj_workflow_node_info`  (
    `id` bigint(0) NOT NULL AUTO_INCREMENT,
    `app_id` bigint(0) NOT NULL,
    `enable` bit(1) NOT NULL,
    `extra` longtext NULL,
    `gmt_create` datetime(6) NULL,
    `gmt_modified` datetime(6) NULL,
    `job_id` bigint(0) NULL DEFAULT NULL,
    `node_name` varchar(255) NULL DEFAULT NULL,
    `node_params` longtext NULL,
    `skip_when_failed` bit(1) NOT NULL,
    `type` int(0) NULL DEFAULT NULL,
    `workflow_id` bigint(0) NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx01_workflow_node_info`(`workflow_id`, `gmt_create`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 ROW_FORMAT = Dynamic;
