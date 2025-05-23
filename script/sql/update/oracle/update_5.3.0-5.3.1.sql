ALTER TABLE flow_node DROP COLUMN skip_any_node;
ALTER TABLE flow_node ADD (ext VARCHAR2(500));
COMMENT ON COLUMN flow_node.ext IS '扩展属性';
create index USER_ASSOCIATED_IDX on FLOW_USER (ASSOCIATED);

ALTER TABLE sys_oss ADD (ext1 VARCHAR2(500));
COMMENT ON COLUMN sys_oss.ext1 IS '扩展属性';
