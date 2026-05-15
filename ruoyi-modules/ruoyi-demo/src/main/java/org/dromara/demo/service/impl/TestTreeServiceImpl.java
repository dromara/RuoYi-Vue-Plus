package org.dromara.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.demo.domain.TestTree;
import org.dromara.demo.domain.bo.TestTreeBo;
import org.dromara.demo.domain.vo.TestTreeVo;
import org.dromara.demo.mapper.TestTreeMapper;
import org.dromara.demo.service.ITestTreeService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 测试树表Service业务层处理
 *
 * @author Lion Li
 * @date 2021-07-26
 */
// @DS("slave") // 切换从库查询
@RequiredArgsConstructor
@Service
public class TestTreeServiceImpl implements ITestTreeService {

    private final TestTreeMapper treeMapper;

    /**
     * 根据主键查询测试树表详情。
     *
     * @param id 主键
     * @return 测试树表视图对象
     */
    @Override
    public TestTreeVo queryById(Long id) {
        return treeMapper.selectVoById(id);
    }

    // @DS("slave") // 切换从库查询
    /**
     * 查询符合条件的测试树表列表。
     *
     * @param bo 查询条件
     * @return 结果列表
     */
    @Override
    public List<TestTreeVo> queryList(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = buildQueryWrapper(bo);
        return treeMapper.selectVoList(lqw);
    }

    /**
     * 构建测试树表动态查询条件。
     *
     * @param bo 查询条件
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TestTree> buildQueryWrapper(TestTreeBo bo) {
        LambdaQueryWrapper<TestTree> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDeptId() != null, TestTree::getDeptId, bo.getDeptId());
        lqw.eq(bo.getUserId() != null, TestTree::getUserId, bo.getUserId());
        lqw.like(StringUtils.isNotBlank(bo.getTreeName()), TestTree::getTreeName, bo.getTreeName());
        lqw.orderByAsc(TestTree::getId);
        return lqw;
    }

    /**
     * 新增测试树表数据。
     *
     * @param bo 新增业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(TestTreeBo bo) {
        TestTree add = MapstructUtils.convert(bo, TestTree.class);
        validEntityBeforeSave(add);
        boolean flag = treeMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 更新测试树表数据。
     *
     * @param bo 编辑业务对象
     * @return 是否更新成功
     */
    @Override
    public Boolean updateByBo(TestTreeBo bo) {
        TestTree update = MapstructUtils.convert(bo, TestTree.class);
        validEntityBeforeSave(update);
        return treeMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(TestTree entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 按主键集合删除测试树表数据，并按需执行删除前校验。
     *
     * @param ids 主键集合
     * @param isValid 是否执行删除校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return treeMapper.deleteByIds(ids) > 0;
    }
}
