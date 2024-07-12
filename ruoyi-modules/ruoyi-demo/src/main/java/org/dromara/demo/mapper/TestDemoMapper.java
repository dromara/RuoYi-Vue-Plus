package org.dromara.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.demo.domain.TestDemo;
import org.dromara.demo.domain.vo.TestDemoVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 测试单表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
public interface TestDemoMapper extends BaseMapperPlus<TestDemo, TestDemoVo> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    Page<TestDemoVo> customPageList(@Param("page") Page<TestDemo> page, @Param("ew") Wrapper<TestDemo> wrapper);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    List<TestDemo> selectList(IPage<TestDemo> page, @Param(Constants.WRAPPER) Wrapper<TestDemo> queryWrapper);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    List<TestDemo> selectList(@Param(Constants.WRAPPER) Wrapper<TestDemo> queryWrapper);

    @Override
    @DataPermission(value = {
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    }, joinStr = "AND")
    List<TestDemo> selectBatchIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    int updateById(@Param(Constants.ENTITY) TestDemo entity);

}
