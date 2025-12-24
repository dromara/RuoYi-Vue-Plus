ALTER TABLE flow_node
    ALTER COLUMN node_ratio TYPE VARCHAR(200),
    ALTER COLUMN node_ratio DROP NOT NULL;
COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';
ALTER TABLE flow_node DROP COLUMN handler_type;
ALTER TABLE flow_node DROP COLUMN handler_path;
