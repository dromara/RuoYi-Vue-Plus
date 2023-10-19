package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.domain.vo.TaskVo;

import java.util.List;

/**
 * 运行时任务Mapper接口
 *
 * @author may
 * @date 2023-10-19
 */
public interface ActTaskMapper extends BaseMapperPlus<TaskVo, TaskVo> {

    /**
     * 获取待办信息
     *
     * @param page         分页
     * @param queryWrapper 条件
     * @param userId       用户id
     * @param groupIds     用户角色id
     * @return 结果
     */
    Page<TaskVo> getTaskWaitByPage(@Param("page") Page<TaskVo> page, @Param(Constants.WRAPPER) Wrapper<TaskVo> queryWrapper, @Param("userId") String userId, @Param("groupIds") List<String> groupIds);
}
