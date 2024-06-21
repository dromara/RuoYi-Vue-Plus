package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import org.dromara.common.core.utils.MapstructUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.workflow.domain.WfDefinitionConfig;
import org.dromara.workflow.domain.bo.WfDefinitionConfigBo;
import org.dromara.workflow.domain.vo.WfDefinitionConfigVo;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.springframework.stereotype.Service;
import org.dromara.workflow.mapper.WfDefinitionConfigMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;

/**
 * 流程定义配置Service业务层处理
 *
 * @author may
 * @date 2024-03-18
 */
@RequiredArgsConstructor
@Service
public class WfDefinitionConfigServiceImpl implements IWfDefinitionConfigService {

    private final WfDefinitionConfigMapper baseMapper;

    /**
     * 查询流程定义配置
     */
    @Override
    public WfDefinitionConfigVo getByDefId(String definitionId) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<WfDefinitionConfig>().eq(WfDefinitionConfig::getDefinitionId, definitionId));
    }

    /**
     * 查询流程定义配置
     *
     * @param tableName 表名
     * @return 结果
     */
    @Override
    public WfDefinitionConfigVo getByTableNameLastVersion(String tableName) {
        List<WfDefinitionConfigVo> wfDefinitionConfigVos = baseMapper.selectVoList(
            new LambdaQueryWrapper<WfDefinitionConfig>().eq(WfDefinitionConfig::getTableName, tableName).orderByDesc(WfDefinitionConfig::getVersion));
        if (CollUtil.isNotEmpty(wfDefinitionConfigVos)) {
            return wfDefinitionConfigVos.get(0);
        }
        return null;
    }

    /**
     * 查询流程定义配置
     *
     * @param definitionId 流程定义id
     * @param tableName    表名
     * @return 结果
     */
    @Override
    public WfDefinitionConfigVo getByDefIdAndTableName(String definitionId, String tableName) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<WfDefinitionConfig>()
            .eq(WfDefinitionConfig::getDefinitionId, definitionId)
            .eq(WfDefinitionConfig::getTableName, tableName));
    }

    /**
     * 查询流程定义配置排除当前查询的流程定义
     *
     * @param tableName    表名
     * @param definitionId 流程定义id
     */
    @Override
    public List<WfDefinitionConfigVo> getByTableNameNotDefId(String tableName, String definitionId) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<WfDefinitionConfig>()
            .eq(WfDefinitionConfig::getTableName, tableName)
            .ne(WfDefinitionConfig::getDefinitionId, definitionId));
    }

    /**
     * 查询流程定义配置列表
     */
    @Override
    public List<WfDefinitionConfigVo> queryList(List<String> definitionIds) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<WfDefinitionConfig>().in(WfDefinitionConfig::getDefinitionId, definitionIds));
    }

    /**
     * 新增流程定义配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveOrUpdate(WfDefinitionConfigBo bo) {
        WfDefinitionConfig add = MapstructUtils.convert(bo, WfDefinitionConfig.class);
        baseMapper.delete(new LambdaQueryWrapper<WfDefinitionConfig>().eq(WfDefinitionConfig::getTableName, bo.getTableName()));
        add.setTableName(add.getTableName().toLowerCase());
        boolean flag = baseMapper.insertOrUpdate(add);
        if (baseMapper.insertOrUpdate(add)) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 批量删除流程定义配置
     */
    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean deleteByDefIds(Collection<String> ids) {
        return baseMapper.delete(new LambdaQueryWrapper<WfDefinitionConfig>().in(WfDefinitionConfig::getDefinitionId, ids)) > 0;
    }
}
