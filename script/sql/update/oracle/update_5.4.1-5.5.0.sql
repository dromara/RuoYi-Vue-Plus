-- ----------------------------
-- 流程spel表达式定义表
-- ----------------------------
CREATE TABLE flow_spel (
    id NUMBER(20) NOT NULL,
    component_name VARCHAR2(255),
    method_name VARCHAR2(255),
    method_params VARCHAR2(255),
    view_spel VARCHAR2(255),
    remark VARCHAR2(255),
    status CHAR(1) DEFAULT '0',
    del_flag CHAR(1) DEFAULT '0',
    create_dept NUMBER(20),
    create_by NUMBER(20),
    create_time DATE,
    update_by NUMBER(20),
    update_time DATE
);

alter table flow_spel add constraint pk_flow_spel primary key (id);

COMMENT ON TABLE flow_spel IS '流程spel表达式定义表';
COMMENT ON COLUMN flow_spel.id IS '主键id';
COMMENT ON COLUMN flow_spel.component_name IS '组件名称';
COMMENT ON COLUMN flow_spel.method_name IS '方法名';
COMMENT ON COLUMN flow_spel.method_params IS '参数';
COMMENT ON COLUMN flow_spel.view_spel IS '预览spel表达式';
COMMENT ON COLUMN flow_spel.remark IS '备注';
COMMENT ON COLUMN flow_spel.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN flow_spel.del_flag IS '删除标志';
COMMENT ON COLUMN flow_spel.create_dept IS '创建部门';
COMMENT ON COLUMN flow_spel.create_by IS '创建者';
COMMENT ON COLUMN flow_spel.create_time IS '创建时间';
COMMENT ON COLUMN flow_spel.update_by IS '更新者';
COMMENT ON COLUMN flow_spel.update_time IS '更新时间';

INSERT INTO flow_spel VALUES (1, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 103, 1, SYSDATE, 1, SYSDATE);
INSERT INTO flow_spel VALUES (2, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 103, 1, SYSDATE, 1, SYSDATE);

INSERT INTO sys_menu VALUES ('11801', '流程表达式', '11616', 2, 'spel', 'workflow/spel/index', '', 1, 0, 'C', '0', '0', 'workflow:spel:list', 'input', 103, 1, SYSDATE, 1, SYSDATE, '流程达式定义菜单');
INSERT INTO sys_menu VALUES ('11802', '流程spel表达式定义查询', '11801', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:query', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11803', '流程spel表达式定义新增', '11801', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:add', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11804', '流程spel表达式定义修改', '11801', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:edit', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11805', '流程spel表达式定义删除', '11801', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:remove', '#', 103, 1, SYSDATE, NULL, NULL, '');
INSERT INTO sys_menu VALUES ('11806', '流程spel表达式定义导出', '11801', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:export', '#', 103, 1, SYSDATE, NULL, NULL, '');

ALTER TABLE flow_definition ADD model_value VARCHAR2(40) DEFAULT 'CLASSICS' NOT NULL;
COMMENT ON COLUMN flow_definition.model_value IS '设计器模式（CLASSICS经典模式 MIMIC仿钉钉模式）';

UPDATE flow_skip SET skip_condition = REPLACE(skip_condition, 'notNike', 'notLike');

ALTER TABLE flow_his_task MODIFY (collaborator VARCHAR2(500) DEFAULT NULL NULL);
COMMENT ON COLUMN flow_his_task.collaborator IS '协作人';

-- ----------------------------
-- 流程实例业务扩展表
-- ----------------------------
CREATE TABLE flow_instance_biz_ext (
    id             NUMBER(20),
    tenant_id      VARCHAR2(20)  DEFAULT '000000',
    create_dept    NUMBER(20),
    create_by      NUMBER(20),
    create_time    TIMESTAMP,
    update_by      NUMBER(20),
    update_time    TIMESTAMP,
    business_code  VARCHAR2(255),
    business_title VARCHAR2(1000),
    del_flag       CHAR(1)       DEFAULT '0',
    instance_id    NUMBER(20),
    business_id    VARCHAR2(255)
);

alter table flow_instance_biz_ext add constraint pk_fi_biz_ext primary key (id);

COMMENT ON TABLE flow_instance_biz_ext IS '流程实例业务扩展表';
COMMENT ON COLUMN flow_instance_biz_ext.id  IS '主键id';
COMMENT ON COLUMN flow_instance_biz_ext.tenant_id  IS '租户编号';
COMMENT ON COLUMN flow_instance_biz_ext.create_dept  IS '创建部门';
COMMENT ON COLUMN flow_instance_biz_ext.create_by  IS '创建者';
COMMENT ON COLUMN flow_instance_biz_ext.create_time  IS '创建时间';
COMMENT ON COLUMN flow_instance_biz_ext.update_by  IS '更新者';
COMMENT ON COLUMN flow_instance_biz_ext.update_time  IS '更新时间';
COMMENT ON COLUMN flow_instance_biz_ext.business_code  IS '业务编码';
COMMENT ON COLUMN flow_instance_biz_ext.business_title  IS '业务标题';
COMMENT ON COLUMN flow_instance_biz_ext.del_flag  IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN flow_instance_biz_ext.instance_id  IS '流程实例Id';
COMMENT ON COLUMN flow_instance_biz_ext.business_id  IS '业务Id';

ALTER TABLE test_leave ADD COLUMN apply_code VARCHAR2(50) NOT NULL;
COMMENT ON COLUMN test_leave.apply_code IS '申请编号';

update sys_menu set remark = '/tool/gen' where menu_id = 116;
update sys_menu set remark = '/system/role' where menu_id = 130;
update sys_menu set remark = '/system/user' where menu_id = 131;
update sys_menu set remark = '/system/dict' where menu_id = 132;
update sys_menu set remark = '/system/oss' where menu_id = 133;
update sys_menu set remark = '/workflow/processDefinition' where menu_id = 11700;
