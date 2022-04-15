alter table sys_menu change query query_param varchar(255) default null comment '路由参数';

alter table sys_dept modify column ancestors varchar(500) null default '' comment '祖级列表';
