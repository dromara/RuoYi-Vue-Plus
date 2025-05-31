ALTER TABLE sys_logininfor ADD (client_key varchar2(32) DEFAULT '');
COMMENT ON COLUMN sys_logininfor.client_key IS '客户端';

ALTER TABLE sys_logininfor ADD (device_type varchar2(32) DEFAULT '');
COMMENT ON COLUMN sys_logininfor.device_type IS '设备类型';
