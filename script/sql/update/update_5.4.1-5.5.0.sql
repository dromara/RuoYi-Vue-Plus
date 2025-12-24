-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------
CREATE TABLE flow_spel (
    id bigint(20) NOT NULL COMMENT '主键id',
    component_name varchar(255) DEFAULT NULL COMMENT '组件名称',
    method_name varchar(255) DEFAULT NULL COMMENT '方法名',
    method_params varchar(255) DEFAULT NULL COMMENT '参数',
    view_spel varchar(255) DEFAULT NULL COMMENT '预览spel表达式',
    remark varchar(255) DEFAULT NULL COMMENT '备注',
    status char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag char(1) DEFAULT '0' COMMENT '删除标志',
    create_dept bigint(20) DEFAULT NULL COMMENT '创建部门',
    create_by bigint(20) DEFAULT NULL COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by bigint(20) DEFAULT NULL COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT='流程spel表达式定义表';

INSERT INTO flow_spel VALUES (1, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 103, 1, sysdate(), 1, sysdate());
INSERT INTO flow_spel VALUES (2, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 103, 1, sysdate(), 1, sysdate());

INSERT INTO sys_menu VALUES ('11801', '流程表达式', '11616', '2', 'spel',    'workflow/spel/index', '', 1, 0, 'C', '0', '0', 'workflow:spel:list', 'input', 103, 1, sysdate(), 1, sysdate(), '流程达式定义菜单');
INSERT INTO sys_menu VALUES ('11802', '流程spel表达式定义查询', '11801', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11803', '流程spel表达式定义新增', '11801', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11804', '流程spel表达式定义修改', '11801', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11805', '流程spel表达式定义删除', '11801', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11806', '流程spel表达式定义导出', '11801', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:export', '#', 103, 1, sysdate(), NULL, NULL, '');

ALTER TABLE `flow_definition`
    ADD COLUMN `model_value` varchar(40) NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模式（CLASSICS经典模式 MIMIC仿钉钉模式）' AFTER `flow_name`;

update flow_skip set skip_condition = REPLACE(skip_condition,'notNike','notLike');

ALTER TABLE `flow_his_task`
    MODIFY COLUMN `collaborator` varchar(500) NULL DEFAULT NULL COMMENT '协作人' AFTER `cooperate_type`;

-- ----------------------------
-- 流程实例业务扩展表
-- ----------------------------

create table flow_instance_biz_ext (
    id             bigint                       not null comment '主键id',
    tenant_id      varchar(20) default '000000' null comment '租户编号',
    create_dept    bigint                       null comment '创建部门',
    create_by      bigint                       null comment '创建者',
    create_time    datetime                     null comment '创建时间',
    update_by      bigint                       null comment '更新者',
    update_time    datetime                     null comment '更新时间',
    business_code  varchar(255)                 null comment '业务编码',
    business_title varchar(1000)                null comment '业务标题',
    del_flag       char        default '0'      null comment '删除标志（0代表存在 1代表删除）',
    instance_id    bigint                       null comment '流程实例Id',
    business_id    varchar(255)                 null comment '业务Id',
    PRIMARY KEY (id)
)  ENGINE = InnoDB COMMENT '流程实例业务扩展表';

ALTER TABLE `test_leave`
    ADD COLUMN `apply_code` varchar(50) NOT NULL COMMENT '申请编号' AFTER `tenant_id`;

update sys_menu set remark = '/tool/gen' where menu_id = 116;
update sys_menu set remark = '/system/role' where menu_id = 130;
update sys_menu set remark = '/system/user' where menu_id = 131;
update sys_menu set remark = '/system/dict' where menu_id = 132;
update sys_menu set remark = '/system/oss' where menu_id = 133;
update sys_menu set remark = '/workflow/processDefinition' where menu_id = 11700;
