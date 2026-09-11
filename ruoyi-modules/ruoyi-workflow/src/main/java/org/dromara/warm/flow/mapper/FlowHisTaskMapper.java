/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.warm.flow.entity.FlowHisTask;
import org.dromara.warm.flow.enums.SkipType;

import java.util.Arrays;
import java.util.List;

/**
 * 历史任务记录Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowHisTaskMapper extends WarmMapper<FlowHisTask> {

    /**
     * 根据instanceId获取未退回的历史记录
     */
    default List<FlowHisTask> getNoReject(Long instanceId) {
        LambdaQueryWrapper<FlowHisTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowHisTask::getInstanceId, instanceId)
            .eq(FlowHisTask::getSkipType, SkipType.PASS.getKey())
            .orderByDesc(FlowHisTask::getCreateTime);
        return selectList(queryWrapper);
    }

    /**
     * 根据instanceId和流程编码获取未退回的历史记录
     */
    default List<FlowHisTask> getByInsAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        LambdaQueryWrapper<FlowHisTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowHisTask::getInstanceId, instanceId)
            .in(CollUtil.isNotEmpty(nodeCodes), FlowHisTask::getNodeCode, nodeCodes)
            .orderByDesc(FlowHisTask::getCreateTime);
        return selectList(queryWrapper);
    }

    /**
     * 根据instanceIds删除
     *
     * @param instanceIds 主键
     * @return 结果
     */
    default int deleteByInsIds(List<Long> instanceIds) {
        return delete(new LambdaQueryWrapper<FlowHisTask>().in(FlowHisTask::getInstanceId, instanceIds));
    }

    /**
     * 根据任务id和协作类型查询
     */
    default List<FlowHisTask> listByTaskIdAndCooperateTypes(Long taskId, Integer[] cooperateTypes) {
        LambdaQueryWrapper<FlowHisTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowHisTask::getTaskId, taskId).in(FlowHisTask::getCooperateType, Arrays.asList(cooperateTypes));
        return selectList(queryWrapper);
    }
}
