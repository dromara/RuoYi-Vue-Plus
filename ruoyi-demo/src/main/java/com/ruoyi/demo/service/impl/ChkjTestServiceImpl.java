package com.ruoyi.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.demo.bo.ChkjTestAddBo;
import com.ruoyi.demo.bo.ChkjTestEditBo;
import com.ruoyi.demo.bo.ChkjTestQueryBo;
import com.ruoyi.demo.domain.ChkjTest;
import com.ruoyi.demo.mapper.ChkjTestMapper;
import com.ruoyi.demo.service.IChkjTestService;
import com.ruoyi.demo.vo.ChkjTestVo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测试Service业务层处理
 *
 * @author Lion Li
 * @date 2021-05-14
 */
@Service
public class ChkjTestServiceImpl extends ServiceImpl<ChkjTestMapper, ChkjTest> implements IChkjTestService {

    @Override
    public ChkjTestVo queryById(Long id){
        return getVoById(id, obj -> BeanUtil.toBean(obj, ChkjTestVo.class));
    }

    @Override
    public TableDataInfo<ChkjTestVo> queryPageList(ChkjTestQueryBo bo) {
        PagePlus<ChkjTest, ChkjTestVo> result = pageVo(PageUtils.buildPagePlus(), buildQueryWrapper(bo), ChkjTestVo.class);
        return PageUtils.buildDataInfo(result);
    }

    @Override
    public List<ChkjTestVo> queryList(ChkjTestQueryBo bo) {
        return listVo(buildQueryWrapper(bo), ChkjTestVo.class);
    }

    private LambdaQueryWrapper<ChkjTest> buildQueryWrapper(ChkjTestQueryBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChkjTest> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(bo.getTestKey()), ChkjTest::getTestKey, bo.getTestKey());
        lqw.eq(StrUtil.isNotBlank(bo.getValue()), ChkjTest::getValue, bo.getValue());
        lqw.eq(bo.getVersion() != null, ChkjTest::getVersion, bo.getVersion());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            ChkjTest::getCreateTime ,params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.eq(bo.getDeleted() != null, ChkjTest::getDeleted, bo.getDeleted());
        lqw.eq(bo.getParentId() != null, ChkjTest::getParentId, bo.getParentId());
        lqw.eq(bo.getOrderNum() != null, ChkjTest::getOrderNum, bo.getOrderNum());
        return lqw;
    }

    @Override
    public Boolean insertByAddBo(ChkjTestAddBo bo) {
        ChkjTest add = BeanUtil.toBean(bo, ChkjTest.class);
        validEntityBeforeSave(add);
        return save(add);
    }

    @Override
    public Boolean updateByEditBo(ChkjTestEditBo bo) {
        ChkjTest update = BeanUtil.toBean(bo, ChkjTest.class);
        validEntityBeforeSave(update);
        return updateById(update);
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(ChkjTest entity){
        //TODO 做一些数据校验,如唯一约束
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return removeByIds(ids);
    }
}
