package org.dromara.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.workflow.domain.bo.WfBusinessFormBo;
import org.dromara.workflow.domain.vo.WfBusinessFormVo;
import org.dromara.workflow.domain.WfBusinessForm;
import org.dromara.workflow.mapper.WfBusinessFormMapper;
import org.dromara.workflow.service.IWfBusinessFormService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

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

    /**
     * 查询发起流程
     */
    @Override
    public WfBusinessFormVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询发起流程列表
     */
    @Override
    public TableDataInfo<WfBusinessFormVo> queryPageList(WfBusinessFormBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfBusinessForm> lqw = buildQueryWrapper(bo);
        Page<WfBusinessFormVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
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
        validEntityBeforeSave(add);
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
        validEntityBeforeSave(update);
        baseMapper.updateById(update);
        if (StringUtils.isNotBlank(update.getContentValue())) {
            Map<String, Object> variable = JsonUtils.parseObject(update.getContentValue(), new TypeReference<>() {
            });
            update.setVariable(variable);
        }
        return update;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WfBusinessForm entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除发起流程
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
