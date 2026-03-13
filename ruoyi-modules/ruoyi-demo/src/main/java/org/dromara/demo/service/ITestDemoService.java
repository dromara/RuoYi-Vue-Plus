package org.dromara.demo.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.demo.domain.TestDemo;
import org.dromara.demo.domain.bo.TestDemoBo;
import org.dromara.demo.domain.vo.TestDemoVo;

import java.util.Collection;
import java.util.List;

/**
 * 测试单表Service接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
public interface ITestDemoService {

    /**
     * 查询单个
     *
     * @param id 主键
     * @return 测试单表视图对象
     */
    TestDemoVo queryById(Long id);

    /**
     * 分页查询测试单表列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<TestDemoVo> queryPageList(TestDemoBo bo, PageQuery pageQuery);

    /**
     * 按自定义 SQL 分页查询测试单表列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<TestDemoVo> customPageList(TestDemoBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的测试单表列表。
     *
     * @param bo 查询条件
     * @return 结果列表
     */
    List<TestDemoVo> queryList(TestDemoBo bo);

    /**
     * 根据新增业务对象插入测试单表
     *
     * @param bo 测试单表新增业务对象
     * @return 是否新增成功
     */
    Boolean insertByBo(TestDemoBo bo);

    /**
     * 根据编辑业务对象修改测试单表
     *
     * @param bo 测试单表编辑业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(TestDemoBo bo);

    /**
     * 校验并删除数据
     *
     * @param ids     主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存测试单表数据。
     *
     * @param list 待保存数据
     * @return 是否保存成功
     */
    Boolean saveBatch(List<TestDemo> list);
}
