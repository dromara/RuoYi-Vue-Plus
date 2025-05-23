ALTER TABLE gen_table ADD data_name nvarchar(200) DEFAULT '' NULL
GO

EXEC sp_addextendedproperty
    'MS_Description', N'数据源名称',
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'data_name'
GO

UPDATE sys_menu SET path = 'powerjob', component = 'monitor/powerjob/index', perms = 'monitor:powerjob:list', remark = 'powerjob控制台菜单' WHERE menu_id = 120
GO

create table sys_social
(
    id                 bigint            NOT NULL,
    user_id            bigint            NOT NULL,
    tenant_id          nvarchar(20)      NULL,
    auth_id            nvarchar(255)     NOT NULL,
    source             nvarchar(255)     NOT NULL,
    open_id            nvarchar(255)     NULL,
    user_name          nvarchar(30)      NOT NULL,
    nick_name          nvarchar(30)      DEFAULT ('')   NULL,
    email              nvarchar(255)     DEFAULT ('')   NULL,
    avatar             nvarchar(500)     DEFAULT ('')   NULL,
    access_token       nvarchar(255)     NOT NULL,
    expire_in          bigint            NULL,
    refresh_token      nvarchar(255)     NULL,
    access_code        nvarchar(255)     NULL,
    union_id           nvarchar(255)     NULL,
    scope              nvarchar(255)     NULL,
    token_type         nvarchar(255)     NULL,
    id_token           nvarchar(255)     NULL,
    mac_algorithm      nvarchar(255)     NULL,
    mac_key            nvarchar(255)     NULL,
    code               nvarchar(255)     NULL,
    oauth_token        nvarchar(255)     NULL,
    oauth_token_secret nvarchar(255)     NULL,
    create_dept        bigint,
    create_by          bigint,
    create_time        datetime2(7),
    update_by          bigint,
    update_time        datetime2(7),
    del_flag           nchar             DEFAULT ('0')   NULL,
    CONSTRAINT PK__sys_social__B21E8F2427725F8A PRIMARY KEY CLUSTERED (id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'user_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'平台+平台唯一id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'auth_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户来源' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'source'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'平台编号唯一id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'open_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'登录账号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'user_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户昵称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'nick_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户邮箱' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'email'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'头像地址' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'avatar'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户的授权令牌' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'access_token'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户的授权令牌的有效期，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'expire_in'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'刷新令牌，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'refresh_token'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'平台的授权信息，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'access_code'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户的 unionid' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'union_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'授予的权限，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'scope'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'个别平台的授权信息，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'token_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'id token，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'id_token'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'小米平台用户的附带属性，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'mac_algorithm'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'小米平台用户的附带属性，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'mac_key'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户的授权code，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'code'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'Twitter平台用户的附带属性，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'oauth_token'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'Twitter平台用户的附带属性，部分平台可能没有' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'oauth_token_secret'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'删除标志（0代表存在 2代表删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_social',
    'COLUMN', N'update_time'
GO


CREATE TABLE sys_client
(
    id                  bigint                              NOT NULL,
    client_id           nvarchar(64)  DEFAULT ''            NULL,
    client_key          nvarchar(32) DEFAULT ''            NULL,
    client_secret       nvarchar(255) DEFAULT ''            NULL,
    grant_type          nvarchar(255) DEFAULT ''            NULL,
    device_type         nvarchar(32) DEFAULT ''            NULL,
    active_timeout      int           DEFAULT ((1800))      NULL,
    timeout             int           DEFAULT ((604800))    NULL,
    status              nchar(1)      DEFAULT ('0')         NULL,
    del_flag            nchar(1)      DEFAULT ('0')         NULL,
    create_dept         bigint                              NULL,
    create_by           bigint                              NULL,
    create_time         datetime2(7)                        NULL,
    update_by           bigint                              NULL,
    update_time         datetime2(7)                        NULL
    CONSTRAINT PK__sys_client___BFBDE87009ED2882 PRIMARY KEY CLUSTERED (id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
'MS_Description', N'主键',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'id'
GO
EXEC sys.sp_addextendedproperty
'MS_Description', N'客户端id' ,
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'client_id'
GO
EXEC sp_addextendedproperty
'MS_Description', N'客户端key',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'client_key'
GO
EXEC sp_addextendedproperty
'MS_Description', N'客户端秘钥',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'client_secret'
GO
EXEC sp_addextendedproperty
'MS_Description', N'授权类型',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'grant_type'
GO
EXEC sp_addextendedproperty
'MS_Description', N'设备类型',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'device_type'
GO
EXEC sp_addextendedproperty
'MS_Description', N'token活跃超时时间',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'active_timeout'
GO
EXEC sp_addextendedproperty
'MS_Description', N'token固定超时',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'timeout'
GO
EXEC sp_addextendedproperty
'MS_Description', N'状态（0正常 1停用）',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'status'
GO
EXEC sp_addextendedproperty
'MS_Description', N'删除标志（0代表存在 2代表删除）',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
'MS_Description', N'创建部门' ,
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'create_dept'
GO
EXEC sp_addextendedproperty
'MS_Description', N'创建者',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'create_by'
GO
EXEC sp_addextendedproperty
'MS_Description', N'创建时间',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'create_time'
GO
EXEC sp_addextendedproperty
'MS_Description', N'更新者',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'update_by'
GO
EXEC sp_addextendedproperty
'MS_Description', N'更新时间',
'SCHEMA', N'dbo',
'TABLE', N'sys_client',
'COLUMN', N'update_time'
GO
EXEC sp_addextendedproperty
'MS_Description', N'系统授权表',
'SCHEMA', N'dbo',
'TABLE', N'sys_client'
GO

INSERT INTO sys_client VALUES (N'1', N'e5cd7e4891bf95d1d19206ce24a7b32e', N'pc', N'pc123', N'password,social', N'pc', 1800, 604800, N'0', N'0', 103, 1, getdate(), 1, getdate())
GO
INSERT INTO sys_client VALUES (N'2', N'428a8310cd442757ae699df5d894f051', N'app', N'app123', N'password,sms,social', N'android', 1800, 604800, N'0', N'0', 103, 1, getdate(), 1, getdate())
GO

INSERT sys_dict_type VALUES (11, N'000000', N'授权类型', N'sys_grant_type', N'0', 103, 1, getdate(), NULL, NULL, N'认证授权类型')
GO
INSERT sys_dict_type VALUES (12, N'000000', N'设备类型', N'sys_device_type', N'0', 103, 1, getdate(), NULL, NULL, N'客户端设备类型')
GO

INSERT sys_dict_data VALUES (30, N'000000', 0, N'密码认证', N'password', N'sys_grant_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'密码认证');
GO
INSERT sys_dict_data VALUES (31, N'000000', 0, N'短信认证', N'sms', N'sys_grant_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'短信认证')
GO
INSERT sys_dict_data VALUES (32, N'000000', 0, N'邮件认证', N'email', N'sys_grant_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'邮件认证')
GO
INSERT sys_dict_data VALUES (33, N'000000', 0, N'小程序认证', N'xcx', N'sys_grant_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'小程序认证')
GO
INSERT sys_dict_data VALUES (34, N'000000', 0, N'三方登录认证', N'`social`', N'sys_grant_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'三方登录认证')
GO
INSERT sys_dict_data VALUES (35, N'000000', 0, N'PC', N'`pc`', N'sys_device_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'PC')
GO
INSERT sys_dict_data VALUES (36, N'000000', 0, N'安卓', N'`android`', N'sys_device_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'安卓')
GO
INSERT sys_dict_data VALUES (37, N'000000', 0, N'iOS', N'`ios`', N'sys_device_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'iOS')
GO
INSERT sys_dict_data VALUES (38, N'000000', 0, N'小程序', N'`xcx`', N'sys_device_type', N'', N'default', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'小程序')
GO

-- 二级菜单
INSERT sys_menu VALUES (123, N'客户端管理', 1, 11, N'client', N'system/client/index', N'', 1, 0, N'C', N'0', N'0', N'system:client:list', N'international', 103, 1, getdate(), NULL, NULL, N'客户端管理菜单')
GO
-- 客户端管理按钮
INSERT sys_menu VALUES (1061, N'客户端管理查询', 123, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:client:query', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1062, N'客户端管理新增', 123, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:client:add', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1063, N'客户端管理修改', 123, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:client:edit', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1064, N'客户端管理删除', 123, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:client:remove', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1065, N'客户端管理导出', 123, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:client:export', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO


-- 角色菜单权限
INSERT sys_role_menu VALUES (2, 1061)
GO
INSERT sys_role_menu VALUES (2, 1062)
GO
INSERT sys_role_menu VALUES (2, 1063)
GO
INSERT sys_role_menu VALUES (2, 1064)
GO
INSERT sys_role_menu VALUES (2, 1065)
GO


UPDATE sys_dept SET leader = null
GO
ALTER TABLE sys_dept ALTER COLUMN leader bigint NULL
GO
