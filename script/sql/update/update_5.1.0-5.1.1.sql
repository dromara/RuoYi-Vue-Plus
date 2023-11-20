ALTER TABLE sys_logininfor
    ADD COLUMN client_key VARCHAR(32)  NULL DEFAULT NULL COMMENT '客户端' AFTER `user_name`,
    ADD COLUMN device_type VARCHAR(32) NULL DEFAULT NULL COMMENT '设备类型' AFTER `client_key`;
