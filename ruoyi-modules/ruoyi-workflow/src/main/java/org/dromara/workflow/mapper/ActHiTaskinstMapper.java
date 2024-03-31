package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.workflow.domain.ActHiTaskinst;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 流程历史任务Mapper接口
 *
 * @author may
 * @date 2024-03-02
 */
@InterceptorIgnore(tenantLine = "true")
public interface ActHiTaskinstMapper extends BaseMapperPlus<ActHiTaskinst, ActHiTaskinst> {

}
