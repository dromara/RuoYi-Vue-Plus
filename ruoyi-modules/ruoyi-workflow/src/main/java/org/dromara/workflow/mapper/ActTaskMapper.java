package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.domain.vo.TaskVo;


/**
 * 任务信息Mapper接口
 *
 * @author may
 * @date 2024-03-02
 */
@InterceptorIgnore(tenantLine = "true")
public interface ActTaskMapper extends BaseMapperPlus<TaskVo, TaskVo> {
    /**
     * 获取待办信息
     *
     * @param page         分页
     * @param queryWrapper 条件
     * @return 结果
     */
    Page<TaskVo> getTaskWaitByPage(@Param("page") Page<TaskVo> page, @Param(Constants.WRAPPER) Wrapper<TaskVo> queryWrapper);

    /**
     * 获取已办
     *
     * @param page         分页
     * @param queryWrapper 条件
     * @return 结果
     */
    Page<TaskVo> getTaskFinishByPage(@Param("page") Page<TaskVo> page, @Param(Constants.WRAPPER) Wrapper<TaskVo> queryWrapper);

    /**
     * 查询当前用户的抄送
     *
     * @param page         分页
     * @param queryWrapper 条件
     * @return 结果
     */
    Page<TaskVo> getTaskCopyByPage(@Param("page") Page<TaskVo> page, @Param(Constants.WRAPPER) QueryWrapper<TaskVo> queryWrapper);
}
