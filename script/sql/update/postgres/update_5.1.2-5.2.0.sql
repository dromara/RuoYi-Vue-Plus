ALTER TABLE sys_dept
    ADD COLUMN dept_category VARCHAR(100) DEFAULT NULL COMMENT '部门类别编码';
ALTER TABLE sys_post
    ADD COLUMN dept_id BIGINT NOT NULL COMMENT '部门id',
    ADD COLUMN post_category VARCHAR(100) DEFAULT NULL COMMENT '岗位类别编码';
UPDATE sys_post SET dept_id = 100;
