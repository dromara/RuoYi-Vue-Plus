ALTER TABLE gen_table ADD (data_name VARCHAR2(200) DEFAULT '');

COMMENT ON COLUMN gen_table.data_name IS '数据源名称';
