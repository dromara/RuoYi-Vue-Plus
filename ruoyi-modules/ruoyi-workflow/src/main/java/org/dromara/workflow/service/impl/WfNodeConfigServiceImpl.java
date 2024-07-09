package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.workflow.domain.vo.WfFormManageVo;
import org.dromara.workflow.service.IWfFormManageService;
import org.springframework.stereotype.Service;
import org.dromara.workflow.domain.vo.WfNodeConfigVo;
import org.dromara.workflow.domain.WfNodeConfig;
import org.dromara.workflow.mapper.WfNodeConfigMapper;
import org.dromara.workflow.service.IWfNodeConfigService;

import java.util.Collection;
import java.util.List;

/**
 * 节点配置Service业务层处理
 *
 * @author may
 * @date 2024-03-30
 */
@RequiredArgsConstructor
@Service
public class WfNodeConfigServiceImpl implements IWfNodeConfigService {

    private final WfNodeConfigMapper baseMapper;
    private final IWfFormManageService wfFormManageService;

    /**
     * 查询节点配置
     */
    @Override
    public WfNodeConfigVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 保存节点配置
     */
    @Override
    public Boolean saveOrUpdate(List<WfNodeConfig> list) {
        return baseMapper.insertOrUpdateBatch(list);
    }

    /**
     * 批量删除节点配置
     */
    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }



    @Override
    public Boolean deleteByDefIds(Collection<String> ids) {
        return baseMapper.delete(new LambdaQueryWrapper<WfNodeConfig>().in(WfNodeConfig::getDefinitionId, ids)) > 0;
    }

    @Override
    public List<WfNodeConfigVo> selectByDefIds(Collection<String> ids) {
        List<WfNodeConfigVo> wfNodeConfigVos = baseMapper.selectVoList(new LambdaQueryWrapper<WfNodeConfig>().in(WfNodeConfig::getDefinitionId, ids));
        if (CollUtil.isNotEmpty(wfNodeConfigVos)) {
            List<Long> formIds = StreamUtils.toList(wfNodeConfigVos, WfNodeConfigVo::getFormId);
            List<WfFormManageVo> wfFormManageVos = wfFormManageService.queryByIds(formIds);
            for (WfNodeConfigVo wfNodeConfigVo : wfNodeConfigVos) {
                wfFormManageVos.stream().filter(e -> ObjectUtil.equals(e.getId(), wfNodeConfigVo.getFormId())).findFirst().ifPresent(wfNodeConfigVo::setWfFormManageVo);
            }
        }
        return wfNodeConfigVos;
    }
}
