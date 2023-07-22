package org.dromara.demo.service;

import org.dromara.demo.domain.TestLeave;
import org.dromara.demo.domain.vo.TestLeaveVo;
import org.dromara.demo.domain.bo.TestLeaveBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 请假Service接口
 *
 * @author may
 * @date 2023-07-21
 */
public interface ITestLeaveService {

    /**
     * 查询请假
     */
    TestLeaveVo queryById(Long id);

    /**
     * 查询请假列表
     */
    TableDataInfo<TestLeaveVo> queryPageList(TestLeaveBo bo, PageQuery pageQuery);

    /**
     * 查询请假列表
     */
    List<TestLeaveVo> queryList(TestLeaveBo bo);

    /**
     * 新增请假
     */
    TestLeave insertByBo(TestLeaveBo bo);

    /**
     * 修改请假
     */
    TestLeave updateByBo(TestLeaveBo bo);

    /**
     * 校验并批量删除请假信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
