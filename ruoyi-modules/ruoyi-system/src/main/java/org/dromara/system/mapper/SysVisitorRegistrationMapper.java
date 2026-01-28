package org.dromara.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysVisitorRegistration;
import org.dromara.system.domain.bo.SysVisitorRegistrationBo;
import org.dromara.system.domain.vo.SysVisitorRegistrationVo;

import java.util.List;

/**
 * 访客预约登记 Mapper接口
 *
 * @author System
 */
public interface SysVisitorRegistrationMapper extends BaseMapperPlus<SysVisitorRegistration, SysVisitorRegistrationVo> {

    /**
     * 查询访客预约登记列表
     *
     * @param bo 查询条件
     * @return 访客预约登记列表
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    List<SysVisitorRegistrationVo> selectVisitorRegistrationList(@Param("bo") SysVisitorRegistrationBo bo);

    /**
     * 分页查询访客预约登记列表
     *
     * @param page 分页信息
     * @param bo   查询条件
     * @return 访客预约登记分页列表
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    Page<SysVisitorRegistrationVo> selectVisitorRegistrationPage(@Param("page") Page<SysVisitorRegistration> page, @Param("bo") SysVisitorRegistrationBo bo);

}