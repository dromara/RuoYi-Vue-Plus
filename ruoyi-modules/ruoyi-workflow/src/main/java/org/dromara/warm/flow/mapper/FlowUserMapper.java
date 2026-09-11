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
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.warm.flow.entity.FlowUser;

import java.util.Arrays;
import java.util.List;

/**
 * 流程用户Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowUserMapper extends WarmMapper<FlowUser> {

    /**
     * 根据taskId删除
     *
     * @param taskIdList 待办任务主键集合
     * @return 结果
     */
    default int deleteByTaskIds(List<Long> taskIdList) {
        return delete(new LambdaQueryWrapper<FlowUser>().in(FlowUser::getAssociated, taskIdList));
    }

    /**
     * 根据(待办任务，实例，历史表，节点等)id查询权限人或者处理人
     *
     * @param associatedList (待办任务，实例，历史表，节点等)id集合
     * @param types          用户表类型
     * @return 查询结果
     */
    default List<FlowUser> listByAssociatedAndTypes(List<Long> associatedList, String[] types) {
        LambdaQueryWrapper<FlowUser> queryWrapper = new LambdaQueryWrapper<>();
        if (CollUtil.isNotEmpty(associatedList)) {
            if (associatedList.size() == 1) {
                queryWrapper.eq(FlowUser::getAssociated, associatedList.get(0));
            } else {
                queryWrapper.in(FlowUser::getAssociated, associatedList);
            }
        }
        queryWrapper.in(ArrayUtil.isNotEmpty(types), FlowUser::getType, Arrays.asList(types));
        return selectList(queryWrapper);
    }

    /**
     * 根据办理人查询
     *
     * @param associated   待办任务id
     * @param processedBys 办理人id集合
     * @param types        用户表类型
     * @return 查询结果
     */
    default List<FlowUser> listByProcessedBys(Long associated, List<String> processedBys, String[] types) {
        LambdaQueryWrapper<FlowUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(associated), FlowUser::getAssociated, associated);
        if (CollUtil.isNotEmpty(processedBys)) {
            if (processedBys.size() == 1) {
                queryWrapper.eq(FlowUser::getProcessedBy, processedBys.get(0));
            } else {
                queryWrapper.in(FlowUser::getProcessedBy, processedBys);
            }
        }
        queryWrapper.in(ArrayUtil.isNotEmpty(types), FlowUser::getType, types);
        return selectList(queryWrapper);
    }
}
