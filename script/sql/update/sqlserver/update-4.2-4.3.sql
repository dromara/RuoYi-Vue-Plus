INSERT [sys_menu] ([menu_id], [menu_name], [parent_id], [order_num], [path], [component], [query_param], [is_frame], [is_cache], [menu_type], [visible], [status], [perms], [icon], [create_by], [create_time], [update_by], [update_time], [remark]) VALUES (112, N'缓存列表', 2, 6, N'cacheList', N'monitor/cache/list', N'', 1, 0, N'C', N'0', N'0', N'monitor:cache:list', N'redis-list', N'admin', getdate(), N'', NULL, N'缓存列表菜单')
GO

delete from sys_menu WHERE menu_id = 116;
GO

update sys_config set config_key = 'sys.account.captchaEnabled' where config_id = 4
GO

INSERT [sys_menu] ([menu_id], [menu_name], [parent_id], [order_num], [path], [component], [query_param], [is_frame], [is_cache], [menu_type], [visible], [status], [perms], [icon], [create_by], [create_time], [update_by], [update_time], [remark]) VALUES (1050, N'账户解锁', 501, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:logininfor:unlock',  N'#', N'admin', getdate(), N'', null, N'')
GO

INSERT [sys_role_menu] ([role_id], [menu_id]) VALUES (2, 1050)
GO

INSERT [sys_dict_data] ([dict_code], [dict_sort], [dict_label], [dict_value], [dict_type], [css_class], [list_class], [is_default], [status], [create_by], [create_time], [update_by], [update_time], [remark]) VALUES (29, 99, N'其他', N'0', N'sys_oper_type', N'', N'info', N'N', N'0', N'admin', getdate(), N'', NULL, N'其他操作');
GO
