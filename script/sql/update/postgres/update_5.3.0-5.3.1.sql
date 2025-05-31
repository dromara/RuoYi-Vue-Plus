ALTER TABLE flow_node DROP COLUMN skip_any_node;
ALTER TABLE flow_node ADD COLUMN ext varchar(500);
COMMENT ON COLUMN flow_node.ext IS '扩展属性';
CREATE INDEX user_associated_idx ON FLOW_USER USING btree (associated);

ALTER TABLE sys_oss ADD COLUMN ext1 varchar(500));
COMMENT ON COLUMN sys_oss.ext1 IS '扩展属性';
