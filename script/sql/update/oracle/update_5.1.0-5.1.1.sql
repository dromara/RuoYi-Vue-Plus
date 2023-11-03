ALTER TABLE sys_logininfor ADD (client_key VARCHAR(32) DEFAULT '');
COMMENT ON COLUMN sys_logininfor.client_key IS '客户端';

ALTER TABLE sys_logininfor ADD (device_type VARCHAR(32) DEFAULT '');
COMMENT ON COLUMN sys_logininfor.device_type IS '设备类型';
