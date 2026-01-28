-- ----------------------------
-- 访客预约登记表
-- ----------------------------
create table sys_visitor_registration
(
    id                bigint(20)      not null                   comment '主键ID',
    tenant_id         varchar(20)     default '000000'           comment '租户编号',
    visitor_name      varchar(50)     not null                   comment '访客姓名',
    visitor_phone     varchar(20)     not null                   comment '访客联系电话',
    visit_purpose     varchar(500)    not null                   comment '访问事由',
    dept_id           bigint(20)      not null                   comment '预约访问部门ID',
    appointment_time  datetime        not null                   comment '预约到访时间',
    status            char(1)         default '0'                comment '状态（0预约中 1已签到 2已签离 3已取消）',
    check_in_time     datetime                                   comment '实际签到时间',
    check_out_time    datetime                                   comment '实际签离时间',
    remark            varchar(500)    default null               comment '备注',
    del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (id)
) engine=innodb comment = '访客预约登记表';

-- 添加索引
alter table sys_visitor_registration add index idx_visitor_name (visitor_name);
alter table sys_visitor_registration add index idx_dept_id (dept_id);
alter table sys_visitor_registration add index idx_appointment_time (appointment_time);
alter table sys_visitor_registration add index idx_status (status);
alter table sys_visitor_registration add index idx_create_time (create_time);