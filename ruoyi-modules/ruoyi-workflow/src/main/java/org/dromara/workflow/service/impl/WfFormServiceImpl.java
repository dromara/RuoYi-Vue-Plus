package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.WfForm;
import org.dromara.workflow.domain.WfFormDefinition;
import org.dromara.workflow.domain.bo.WfFormBo;
import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.domain.vo.WfFormVo;
import org.dromara.workflow.mapper.WfFormDefinitionMapper;
import org.dromara.workflow.mapper.WfFormMapper;
import org.dromara.workflow.service.IWfFormService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程表单Service业务层处理
 *
 * @author KonBAI
 */
@RequiredArgsConstructor
@Service
public class WfFormServiceImpl implements IWfFormService {

    private final WfFormMapper baseMapper;

    private final WfFormDefinitionMapper wfFormDefinitionMapper;

    /**
     * 查询流程表单
     *
     * @param formId 流程表单ID
     * @return 流程表单
     */
    @Override
    public WfFormVo queryById(Long formId) {
        return baseMapper.selectVoById(formId);
    }

    /**
     * 查询流程表单列表
     *
     * @param bo 流程表单
     * @return 流程表单
     */
    @Override
    public TableDataInfo<WfFormVo> queryPageList(WfFormBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WfForm> lqw = buildQueryWrapper(bo);
        Page<WfFormVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<WfFormVo> records = result.getRecords();
        if(CollUtil.isNotEmpty(records)){
            List<Long> formIds = StreamUtils.toList(records, WfFormVo::getFormId);
            List<WfFormDefinitionVo> wfFormDefinitionVos = wfFormDefinitionMapper.selectVoList(
                new LambdaQueryWrapper<WfFormDefinition>().in(WfFormDefinition::getFormId, formIds)
            );
            for (WfFormVo record : records) {
                if(CollUtil.isNotEmpty(wfFormDefinitionVos)){
                    wfFormDefinitionVos.stream().filter(e->String.valueOf(e.getFormId()).equals(String.valueOf(record.getFormId()))).findFirst().ifPresent(record::setWfFormDefinitionVo);
                }
            }
        }
        return TableDataInfo.build(result);
    }

    /**
     * 查询流程表单列表
     *
     * @param bo 流程表单
     * @return 流程表单
     */
    @Override
    public List<WfFormVo> queryList(WfFormBo bo) {
        LambdaQueryWrapper<WfForm> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 新增流程表单
     *
     * @param bo 流程表单
     * @return 结果
     */
    @Override
    public int insertForm(WfFormBo bo) {
        WfForm WfForm = MapstructUtils.convert(bo, WfForm.class);
        return baseMapper.insert(WfForm);
    }

    /**
     * 修改流程表单
     *
     * @param bo 流程表单
     * @return 结果
     */
    @Override
    public int updateForm(WfFormBo bo) {
        return baseMapper.update(new WfForm(), new LambdaUpdateWrapper<WfForm>()
            .set(StrUtil.isNotBlank(bo.getFormName()), WfForm::getFormName, bo.getFormName())
            .set(StrUtil.isNotBlank(bo.getContent()), WfForm::getContent, bo.getContent())
            .set(StrUtil.isNotBlank(bo.getRemark()), WfForm::getRemark, bo.getRemark())
            .set(StrUtil.isNotBlank(bo.getStatus()), WfForm::getStatus, bo.getStatus())
            .eq(WfForm::getFormId, bo.getFormId()));
    }

    /**
     * 批量删除流程表单
     *
     * @param ids 需要删除的流程表单ID
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    private LambdaQueryWrapper<WfForm> buildQueryWrapper(WfFormBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WfForm> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFormName()), WfForm::getFormName, bo.getFormName());
        return lqw;
    }
}
