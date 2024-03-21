package org.dromara.workflow.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.workflow.domain.bo.WfFormDefinitionBo;
import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.domain.WfFormDefinition;
import org.dromara.workflow.mapper.WfFormDefinitionMapper;
import org.dromara.workflow.service.IWfFormDefinitionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;

/**
 * 表单配置Service业务层处理
 *
 * @author gssong
 * @date 2024-03-18
 */
@RequiredArgsConstructor
@Service
public class WfFormDefinitionServiceImpl implements IWfFormDefinitionService {

    private final WfFormDefinitionMapper baseMapper;

    /**
     * 查询表单配置
     */
    @Override
    public WfFormDefinitionVo getByDefId(String definitionId) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<WfFormDefinition>().eq(WfFormDefinition::getDefinitionId, definitionId));
    }

    /**
     * 查询表单配置列表
     */
    @Override
    public List<WfFormDefinitionVo> queryList(List<String> definitionIds) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<WfFormDefinition>().in(WfFormDefinition::getDefinitionId, definitionIds));
    }

    /**
     * 新增表单配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveOrUpdate(WfFormDefinitionBo bo) {
        WfFormDefinition add = MapstructUtils.convert(bo, WfFormDefinition.class);
        boolean flag = baseMapper.insertOrUpdate(add);
        if (baseMapper.insertOrUpdate(add)) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 批量删除表单配置
     */
    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
