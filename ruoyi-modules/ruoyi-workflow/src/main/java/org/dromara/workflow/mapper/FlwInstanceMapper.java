package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.vo.FlowInstanceVo;

/**
 * 实例信息Mapper接口
 *
 * @author may
 * @date 2024-03-02
 */
public interface FlwInstanceMapper extends MPJBaseMapper<FlowInstance> {

    /**
     * 流程实例信息
     *
     * @param page         分页
     * @param queryWrapper 条件
     * @return 结果
     */
    default Page<FlowInstanceVo> selectInstanceList(Page<FlowInstanceVo> page, MPJLambdaWrapper<FlowInstance> queryWrapper) {
        return this.selectJoinPage(page, FlowInstanceVo.class, queryWrapper);
    }

}
