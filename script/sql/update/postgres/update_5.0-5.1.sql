ALTER TABLE gen_table ADD data_name varchar(200) default ''::varchar;

COMMENT ON COLUMN gen_table.data_name IS '数据源名称';
