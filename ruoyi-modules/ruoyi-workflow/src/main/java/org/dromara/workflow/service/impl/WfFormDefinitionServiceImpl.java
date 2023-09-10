package org.dromara.workflow.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.workflow.domain.bo.WfFormDefinitionBo;
import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.domain.WfFormDefinition;
import org.dromara.workflow.mapper.WfFormDefinitionMapper;
import org.dromara.workflow.service.IWfFormDefinitionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 动态单与流程定义关联信息Service业务层处理
 *
 * @author may
 * @date 2023-08-31
 */
@RequiredArgsConstructor
@Service
public class WfFormDefinitionServiceImpl implements IWfFormDefinitionService {

    private final WfFormDefinitionMapper baseMapper;

    /**
     * 查询动态单与流程定义关联信息
     */
    @Override
    public WfFormDefinitionVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询动态单与流程定义关联信息列表
     */
    @Override
    public TableDataInfo<WfFormDefinitionVo> queryPageList(WfFormDefinitionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfFormDefinition> lqw = buildQueryWrapper(bo);
        Page<WfFormDefinitionVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询动态单与流程定义关联信息列表
     */
    @Override
    public List<WfFormDefinitionVo> queryList(WfFormDefinitionBo bo) {
        LambdaQueryWrapper<WfFormDefinition> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WfFormDefinition> buildQueryWrapper(WfFormDefinitionBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfFormDefinition> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getFormId() != null, WfFormDefinition::getFormId, bo.getFormId());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessDefinitionKey()), WfFormDefinition::getProcessDefinitionKey, bo.getProcessDefinitionKey());
        lqw.like(StringUtils.isNotBlank(bo.getProcessDefinitionName()), WfFormDefinition::getProcessDefinitionName, bo.getProcessDefinitionName());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessDefinitionId()), WfFormDefinition::getProcessDefinitionId, bo.getProcessDefinitionId());
        lqw.eq(bo.getProcessDefinitionVersion() != null, WfFormDefinition::getProcessDefinitionVersion, bo.getProcessDefinitionVersion());
        return lqw;
    }

    /**
     * 新增动态单与流程定义关联信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(WfFormDefinitionBo bo) {
        WfFormDefinition add = MapstructUtils.convert(bo, WfFormDefinition.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改动态单与流程定义关联信息
     */
    @Override
    public Boolean updateByBo(WfFormDefinitionBo bo) {
        WfFormDefinition update = MapstructUtils.convert(bo, WfFormDefinition.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WfFormDefinition entity){
        LambdaQueryWrapper<WfFormDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WfFormDefinition::getFormId,entity.getFormId());
        baseMapper.delete(wrapper);
    }

    /**
     * 批量删除动态单与流程定义关联信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
