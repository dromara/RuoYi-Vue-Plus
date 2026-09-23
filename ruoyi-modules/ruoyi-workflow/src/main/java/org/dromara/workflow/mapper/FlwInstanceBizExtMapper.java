package org.dromara.workflow.mapper;

import cn.hutool.core.util.ObjectUtil;
import org.apache.ibatis.annotations.Delete;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.domain.FlowInstanceBizExt;

import java.util.Collection;

/**
 * 流程实例业务扩展Mapper接口
 *
 * @author may
 * @date 2025-08-05
 */
public interface FlwInstanceBizExtMapper extends BaseMapperPlus<FlowInstanceBizExt, FlowInstanceBizExt> {

    /**
     * 根据 instanceId 保存或更新流程实例业务扩展
     *
     * @param entity 流程实例业务扩展实体
     * @return 操作是否成功
     */
    default int saveOrUpdateByInstanceId(FlowInstanceBizExt entity) {
        // 查询是否存在
        FlowInstanceBizExt exist = this.lambda()
            .eq(FlowInstanceBizExt::getInstanceId, entity.getInstanceId())
            .one();

        if (ObjectUtil.isNotNull(exist)) {
            // 存在就带上主键更新
            entity.setId(exist.getId());
            return updateById(entity);
        } else {
            // 不存在就插入
            return insert(entity);
        }
    }

    /**
     * 按照流程实例ID删除单个流程实例业务扩展
     * ★ D-009（2026-09-22 · C 组预案风险1(i) 方案）：改**物理删**——原 @TableLogic 逻辑删的行
     *   仍占 uk_inst_biz_code 槽位，解绑（软删）→ 再 createBinding（INSERT）会撞 1062 复活冲突；
     *   本表行随实例存亡（父亡子无意义，无审计留痕需求），物理删即 AU-12「清理」的本义。
     *
     * @param instanceId 流程实例ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM flow_instance_biz_ext WHERE instance_id = #{instanceId}")
    int deleteByInstId(Long instanceId);

    /**
     * 按照流程实例ID批量删除流程实例业务扩展（物理删，同上）
     *
     * @param instanceIds 流程实例ID列表
     * @return 删除的行数
     */
    @Delete("<script>DELETE FROM flow_instance_biz_ext WHERE instance_id IN <foreach collection='instanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteByInstIds(Collection<Long> instanceIds);

}
