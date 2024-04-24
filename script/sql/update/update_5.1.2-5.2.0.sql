ALTER TABLE sys_dept ADD dept_category VARCHAR(100) DEFAULT NULL COMMENT '部门类别编码';
ALTER TABLE sys_post ADD dept_id BIGINT(20) NOT NULL COMMENT '部门id', ADD post_category VARCHAR(100) DEFAULT NULL COMMENT '岗位类别编码';
UPDATE sys_post SET dept_id = 100;
UPDATE sys_post SET dept_id = 103 where post_id = 1;
