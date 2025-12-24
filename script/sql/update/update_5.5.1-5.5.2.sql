ALTER TABLE flow_node MODIFY COLUMN node_ratio varchar(200) NULL COMMENT '流程签署比例值';
ALTER TABLE flow_node DROP COLUMN handler_type;
ALTER TABLE flow_node DROP COLUMN handler_path;
