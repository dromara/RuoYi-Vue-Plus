package org.dromara.demo.service.impl;

import cn.hutool.core.collection.CollUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.dromara.demo.domain.bo.TestLeaveBo;
import org.dromara.demo.domain.vo.TestLeaveVo;
import org.dromara.demo.domain.TestLeave;
import org.dromara.demo.mapper.TestLeaveMapper;
import org.dromara.demo.service.ITestLeaveService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 请假Service业务层处理
 *
 * @author may
 * @date 2023-07-21
 */
@RequiredArgsConstructor
@Service
public class TestLeaveServiceImpl implements ITestLeaveService {

    private final TestLeaveMapper baseMapper;
    private final IActProcessInstanceService iActProcessInstanceService;

    /**
     * 查询请假
     */
    @Override
    public TestLeaveVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询请假列表
     */
    @Override
    public TableDataInfo<TestLeaveVo> queryPageList(TestLeaveBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestLeave> lqw = buildQueryWrapper(bo);
        Page<TestLeaveVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        TableDataInfo<TestLeaveVo> build = TableDataInfo.build(result);
        List<TestLeaveVo> rows = build.getRows();
        if (CollUtil.isNotEmpty(rows)) {
            List<String> ids = StreamUtils.toList(rows, e -> String.valueOf(e.getId()));
            WorkflowUtils.setProcessInstanceListVo(rows, ids, "id");
        }
        return build;
    }

    /**
     * 查询请假列表
     */
    @Override
    public List<TestLeaveVo> queryList(TestLeaveBo bo) {
        LambdaQueryWrapper<TestLeave> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<TestLeave> buildQueryWrapper(TestLeaveBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<TestLeave> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTitle()), TestLeave::getTitle, bo.getTitle());
        lqw.eq(bo.getLeaveDays() != null, TestLeave::getLeaveDays, bo.getLeaveDays());
        return lqw;
    }

    /**
     * 新增请假
     */
    @Override
    public TestLeave insertByBo(TestLeaveBo bo) {
        TestLeave add = MapstructUtils.convert(bo, TestLeave.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return add;
    }

    /**
     * 修改请假
     */
    @Override
    public TestLeave updateByBo(TestLeaveBo bo) {
        TestLeave update = MapstructUtils.convert(bo, TestLeave.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0 ? update : null;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(TestLeave entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除请假
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        List<String> idList = StreamUtils.toList(ids, String::valueOf);
        iActProcessInstanceService.deleteRuntimeProcessAndHisInstByBusinessKeys(idList);
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
