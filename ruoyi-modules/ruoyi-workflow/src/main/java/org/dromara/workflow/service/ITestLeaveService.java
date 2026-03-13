package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.TestLeaveBo;
import org.dromara.workflow.domain.vo.TestLeaveVo;

import java.util.List;

/**
 * 请假示例服务接口，定义请假单查询、维护与流程发起能力。
 *
 * @author may
 * @date 2023-07-21
 */
public interface ITestLeaveService {

    /**
     * 根据主键查询请假单。
     *
     * @param id 主键
     * @return 请假详情
     */
    TestLeaveVo queryById(Long id);

    /**
     * 分页查询请假列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 请假分页数据
     */
    TableDataInfo<TestLeaveVo> queryPageList(TestLeaveBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的请假列表。
     *
     * @param bo 查询条件
     * @return 请假列表
     */
    List<TestLeaveVo> queryList(TestLeaveBo bo);

    /**
     * 新增请假单。
     *
     * @param bo 请假信息
     * @return 新增结果
     */
    TestLeaveVo insertByBo(TestLeaveBo bo);

    /**
     * 提交请假单并发起审批流程。
     *
     * @param bo 请假信息
     * @return 提交结果
     */
    TestLeaveVo submitAndFlowStart(TestLeaveBo bo);

    /**
     * 修改请假单。
     *
     * @param bo 请假信息
     * @return 修改结果
     */
    TestLeaveVo updateByBo(TestLeaveBo bo);

    /**
     * 校验并批量删除请假信息。
     *
     * @param ids 主键集合
     * @return 删除结果
     */
    Boolean deleteWithValidByIds(List<Long> ids);
}
