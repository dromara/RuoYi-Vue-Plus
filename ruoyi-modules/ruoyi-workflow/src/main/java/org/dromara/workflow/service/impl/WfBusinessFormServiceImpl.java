package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.workflow.domain.ActHiProcinst;
import org.dromara.workflow.domain.WfFormDefinition;
import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.mapper.WfFormDefinitionMapper;
import org.dromara.workflow.service.IActHiProcinstService;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.dromara.workflow.domain.bo.WfBusinessFormBo;
import org.dromara.workflow.domain.vo.WfBusinessFormVo;
import org.dromara.workflow.domain.WfBusinessForm;
import org.dromara.workflow.mapper.WfBusinessFormMapper;
import org.dromara.workflow.service.IWfBusinessFormService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 发起流程Service业务层处理
 *
 * @author may
 * @date 2023-09-16
 */
@RequiredArgsConstructor
@Service
public class WfBusinessFormServiceImpl implements IWfBusinessFormService {

    private final WfBusinessFormMapper baseMapper;
    private final WfFormDefinitionMapper wfFormDefinitionMapper;
    private final IActProcessInstanceService iActProcessInstanceService;
    private final IActHiProcinstService iActHiProcinstService;

    /**
     * 查询发起流程
     */
    @Override
    public WfBusinessFormVo queryById(Long id) {
        WfBusinessFormVo wfBusinessFormVo = baseMapper.selectVoById(id);
        if (wfBusinessFormVo != null) {
            WfFormDefinitionVo wfFormDefinitionVo = wfFormDefinitionMapper.selectVoOne(
                new LambdaQueryWrapper<WfFormDefinition>().eq(WfFormDefinition::getFormId, wfBusinessFormVo.getFormId())
            );
            wfBusinessFormVo.setWfFormDefinitionVo(wfFormDefinitionVo);
        }
        return wfBusinessFormVo;
    }

    /**
     * 查询发起流程列表
     */
    @Override
    public TableDataInfo<WfBusinessFormVo> queryPageList(WfBusinessFormBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfBusinessForm> lqw = buildQueryWrapper(bo);
        Page<WfBusinessFormVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<WfBusinessFormVo> records = result.getRecords();
        WorkflowUtils.setProcessInstanceListVo(records, StreamUtils.toList(records, e -> String.valueOf(e.getId())), "id");
        return TableDataInfo.build(result);
    }

    /**
     * 查询发起流程列表
     */
    @Override
    public List<WfBusinessFormVo> queryList(WfBusinessFormBo bo) {
        LambdaQueryWrapper<WfBusinessForm> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WfBusinessForm> buildQueryWrapper(WfBusinessFormBo bo) {
        LambdaQueryWrapper<WfBusinessForm> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getApplyCode()), WfBusinessForm::getApplyCode, bo.getApplyCode());
        lqw.like(StringUtils.isNotBlank(bo.getFormName()), WfBusinessForm::getFormName, bo.getFormName());
        return lqw;
    }

    /**
     * 新增发起流程
     */
    @Override
    public WfBusinessForm insertByBo(WfBusinessFormBo bo) {
        WfBusinessForm add = MapstructUtils.convert(bo, WfBusinessForm.class);
        //规则自行修改
        add.setApplyCode(DateUtils.dateTimeNow());
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        if (StringUtils.isNotBlank(add.getContentValue())) {
            Map<String, Object> variable = JsonUtils.parseObject(add.getContentValue(), new TypeReference<>() {
            });
            add.setVariable(variable);
        }
        return add;
    }

    /**
     * 修改发起流程
     */
    @Override
    public WfBusinessForm updateByBo(WfBusinessFormBo bo) {
        WfBusinessForm update = MapstructUtils.convert(bo, WfBusinessForm.class);
        baseMapper.updateById(update);
        if (StringUtils.isNotBlank(update.getContentValue())) {
            Map<String, Object> variable = JsonUtils.parseObject(update.getContentValue(), new TypeReference<>() {
            });
            update.setVariable(variable);
        }
        return update;
    }

    /**
     * 批量删除发起流程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        List<ActHiProcinst> actHiProcinsts = iActHiProcinstService.selectByBusinessKeyIn(StreamUtils.toList(ids, String::valueOf));
        if (CollUtil.isNotEmpty(actHiProcinsts)) {
            iActProcessInstanceService.deleteRuntimeProcessAndHisInst(actHiProcinsts.stream().map(ActHiProcinst::getId).collect(Collectors.toList()));
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
