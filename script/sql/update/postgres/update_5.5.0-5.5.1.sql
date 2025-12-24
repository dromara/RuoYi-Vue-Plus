ALTER TABLE flow_definition ADD create_by VARCHAR(64) DEFAULT '' NOT NULL;
ALTER TABLE flow_definition ADD update_by VARCHAR(64) DEFAULT '' NOT NULL;
COMMENT ON COLUMN flow_definition.create_by IS '创建人';
COMMENT ON COLUMN flow_definition.update_by IS '更新人';

ALTER TABLE flow_node ADD create_by VARCHAR(64) DEFAULT '' NOT NULL;
ALTER TABLE flow_node ADD update_by VARCHAR(64) DEFAULT '' NOT NULL;
COMMENT ON COLUMN flow_node.create_by IS '创建人';
COMMENT ON COLUMN flow_node.update_by IS '更新人';

ALTER TABLE flow_skip ADD create_by VARCHAR(64) DEFAULT '' NOT NULL;
ALTER TABLE flow_skip ADD update_by VARCHAR(64) DEFAULT '' NOT NULL;
COMMENT ON COLUMN flow_skip.create_by IS '创建人';
COMMENT ON COLUMN flow_skip.update_by IS '更新人';

ALTER TABLE flow_instance ADD update_by VARCHAR(64) DEFAULT '' NOT NULL;
COMMENT ON COLUMN flow_instance.update_by IS '更新人';

ALTER TABLE flow_task ADD create_by VARCHAR(64) DEFAULT '' NOT NULL;
ALTER TABLE flow_task ADD update_by VARCHAR(64) DEFAULT '' NOT NULL;
COMMENT ON COLUMN flow_task.create_by IS '创建人';
COMMENT ON COLUMN flow_task.update_by IS '更新人';

ALTER TABLE flow_user ADD update_by VARCHAR(64) DEFAULT '' NOT NULL;
COMMENT ON COLUMN flow_user.update_by IS '更新人';
