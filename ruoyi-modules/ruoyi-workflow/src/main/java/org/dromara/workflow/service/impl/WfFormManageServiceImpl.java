package org.dromara.workflow.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.workflow.common.enums.FormTypeEnum;
import org.springframework.stereotype.Service;
import org.dromara.workflow.domain.bo.WfFormManageBo;
import org.dromara.workflow.domain.vo.WfFormManageVo;
import org.dromara.workflow.domain.WfFormManage;
import org.dromara.workflow.mapper.WfFormManageMapper;
import org.dromara.workflow.service.IWfFormManageService;

import java.util.List;
import java.util.Collection;

/**
 * 表单管理Service业务层处理
 *
 * @author may
 * @date 2024-03-29
 */
@RequiredArgsConstructor
@Service
public class WfFormManageServiceImpl implements IWfFormManageService {

    private final WfFormManageMapper baseMapper;

    /**
     * 查询表单管理
     */
    @Override
    public WfFormManageVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<WfFormManageVo> queryByIds(List<Long> ids) {
        return baseMapper.selectVoBatchIds(ids);
    }

    /**
     * 查询表单管理列表
     */
    @Override
    public TableDataInfo<WfFormManageVo> queryPageList(WfFormManageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfFormManage> lqw = buildQueryWrapper(bo);
        Page<WfFormManageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<WfFormManageVo> selectList() {
        List<WfFormManageVo> wfFormManageVos = baseMapper.selectVoList(new LambdaQueryWrapper<WfFormManage>().orderByDesc(WfFormManage::getUpdateTime));
        for (WfFormManageVo wfFormManageVo : wfFormManageVos) {
            wfFormManageVo.setFormTypeName(FormTypeEnum.findByType(wfFormManageVo.getFormType()));
        }
        return wfFormManageVos;
    }

    /**
     * 查询表单管理列表
     */
    @Override
    public List<WfFormManageVo> queryList(WfFormManageBo bo) {
        LambdaQueryWrapper<WfFormManage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WfFormManage> buildQueryWrapper(WfFormManageBo bo) {
        LambdaQueryWrapper<WfFormManage> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFormName()), WfFormManage::getFormName, bo.getFormName());
        lqw.eq(StringUtils.isNotBlank(bo.getFormType()), WfFormManage::getFormType, bo.getFormType());
        return lqw;
    }

    /**
     * 新增表单管理
     */
    @Override
    public Boolean insertByBo(WfFormManageBo bo) {
        WfFormManage add = MapstructUtils.convert(bo, WfFormManage.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改表单管理
     */
    @Override
    public Boolean updateByBo(WfFormManageBo bo) {
        WfFormManage update = MapstructUtils.convert(bo, WfFormManage.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量删除表单管理
     */
    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
