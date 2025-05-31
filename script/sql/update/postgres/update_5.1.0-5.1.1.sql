ALTER TABLE sys_logininfor ADD client_key varchar(32) default ''::varchar;
COMMENT ON COLUMN sys_logininfor.client_key IS '客户端';

ALTER TABLE sys_logininfor ADD device_type varchar(32) default ''::varchar;
COMMENT ON COLUMN sys_logininfor.device_type IS '设备类型';
