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
    'MS_Description', N'主键' ,
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
    'MS_Description', N'授权+授权openid' ,
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
    'MS_Description', N'原生openid' ,
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
