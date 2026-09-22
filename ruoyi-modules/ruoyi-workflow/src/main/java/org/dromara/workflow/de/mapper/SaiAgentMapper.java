package org.dromara.workflow.de.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.SaiAgent;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 智能体表 sai_agent 的 Mapper（MyBatis-Plus BaseMapperPlus）。
 *
 * <p>本类与 {@link SaiAgent.java} 同源，均在 P0 期内新建（底座无对应 Java 类）。
 * 后续若底座引入实体，本类应替换为底座版的 mapper。
 *
 * <p><b>AU-01 L1 数据权限注解（D-002 已登记）</b>：本 mapper 在查询/更新方法上加
 * {@link DataPermission} 注解（按 {@code create_by} 列）——
 * <b>前提条件</b> = 实体 {@link SaiAgent} <b>已继承 {@code BaseEntity}</b>。
 * 当前 {@code SaiAgent} 仅 {@code implements Serializable}（{@code SaiAgent.java:29}），
 * 未继承 {@code BaseEntity} ⇒ SpEL 模板的 {@code createDept} / {@code createBy} 变量
 * 解析为 {@code "-1"}（默认占位值，{@code PlusDataPermissionHandler.java:115}），
 * SQL 拼接空串 ⇒ **当前 = 形同虚设**。
 *
 * <p>本注解是 <b>声明式占位</b>：一旦 owner 裁 D-00X 让 {@code SaiAgent} 继承 {@code BaseEntity}
 * （= 改底座表 24 列中加 {@code create_by} 等 5 列，不在本 D-002 范围），注解即时生效。
 * 与之配套的「L1 真接 {@code flow_task} / {@code flow_instance} / {@code flow_his_task} Mapper」
 * 同属 D-00X 范畴，本 mapper 不代裁。
 *
 * @author AIP
 */
public interface SaiAgentMapper extends BaseMapperPlus<SaiAgent, SaiAgent> {

    /**
     * 按主键查询并应用数据权限（声明式占位 — 详见类 Javadoc「AU-01」节）。
     *
     * @param id 主键
     * @return 实体
     */
    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    SaiAgent selectById(@Param("id") Serializable id);

    /**
     * 按主键集合查询并应用数据权限（声明式占位 — 详见类 Javadoc「AU-01」节）。
     *
     * @param idList 主键集合
     * @return 实体列表
     */
    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    List<SaiAgent> selectByIds(@Param("coll") Collection<? extends Serializable> idList);

    /**
     * 按主键更新并应用数据权限（声明式占位 — 详见类 Javadoc「AU-01」节）。
     *
     * @param entity 实体
     * @return 更新行数
     */
    @Override
    @DataPermission(value = {
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    }, joinStr = "AND")
    int updateById(@Param("et") SaiAgent entity);
}