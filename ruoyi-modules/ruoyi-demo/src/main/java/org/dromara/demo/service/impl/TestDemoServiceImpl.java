package org.dromara.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.translation.collection.TranslationCollectionWrapper;
import org.dromara.demo.domain.TestDemo;
import org.dromara.demo.domain.bo.TestDemoBo;
import org.dromara.demo.domain.vo.TestDemoVo;
import org.dromara.demo.mapper.TestDemoMapper;
import org.dromara.demo.service.ITestDemoService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 测试单表Service业务层处理
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@RequiredArgsConstructor
@Service
public class TestDemoServiceImpl implements ITestDemoService {

    private final TestDemoMapper baseMapper;

    /**
     * 根据主键查询测试单表详情。
     *
     * @param id 主键
     * @return 测试单表视图对象
     */
    @Override
    public TestDemoVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询测试单表列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<TestDemoVo> queryPageList(TestDemoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestDemo> lqw = buildQueryWrapper(bo);
        Page<TestDemoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(TranslationCollectionWrapper.form(result.getRecords()), result.getTotal());
    }

    /**
     * 通过自定义 SQL 分页查询测试单表列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<TestDemoVo> customPageList(TestDemoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestDemo> lqw = buildQueryWrapper(bo);
        Page<TestDemoVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的测试单表列表。
     *
     * @param bo 查询条件
     * @return 结果列表
     */
    @Override
    public List<TestDemoVo> queryList(TestDemoBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构建测试单表动态查询条件。
     *
     * @param bo 查询条件
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TestDemo> buildQueryWrapper(TestDemoBo bo) {
        LambdaQueryWrapper<TestDemo> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, TestDemo::getDeptId, bo.getDeptId());
        lqw.eq(bo.getUserId() != null, TestDemo::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getTestKey()), TestDemo::getTestKey, bo.getTestKey());
        lqw.eq(StringUtils.isNotBlank(bo.getValue()), TestDemo::getValue, bo.getValue());
        lqw.orderByAsc(TestDemo::getId);
        return lqw;
    }

    /**
     * 新增测试单表数据。
     *
     * @param bo 新增业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(TestDemoBo bo) {
        TestDemo add = MapstructUtils.convert(bo, TestDemo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 更新测试单表数据。
     *
     * @param bo 编辑业务对象
     * @return 是否更新成功
     */
    @Override
    public Boolean updateByBo(TestDemoBo bo) {
        TestDemo update = MapstructUtils.convert(bo, TestDemo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(TestDemo entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 按主键集合删除测试单表数据，并按需执行删除前校验。
     *
     * @param ids 主键集合
     * @param isValid 是否执行删除校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
            List<TestDemo> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 批量保存测试单表数据。
     *
     * @param list 待保存实体列表
     * @return 是否保存成功
     */
    @Override
    public Boolean saveBatch(List<TestDemo> list) {
        return baseMapper.insertBatch(list);
    }
}
