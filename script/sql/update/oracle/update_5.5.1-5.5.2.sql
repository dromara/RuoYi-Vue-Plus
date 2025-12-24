ALTER TABLE flow_node MODIFY (node_ratio VARCHAR2(200) DEFAULT NULL NULL);
COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';
ALTER TABLE flow_node DROP COLUMN handler_type;
ALTER TABLE flow_node DROP COLUMN handler_path;
