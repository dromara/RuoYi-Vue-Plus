package org.dromara.datasource.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.domain.bo.DsFieldQueryBo;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.jdbc.JdbcService;

import java.util.Collection;
import java.util.List;

/**
 * 动态数据源Service接口
 *
 * @author ixyxj
 * @date 2024-05-02
 */
public interface ISysDatasourceService {

    /**
     * 查询动态数据源
     */
    SysDatasourceVo queryById(Long id);

    /**
     * 通过名称查询
     */
    SysDatasourceVo queryByName(String name);

    /**
     * 查询动态数据源列表
     */
    TableDataInfo<SysDatasourceVo> queryPageList(SysDatasourceBo bo, PageQuery pageQuery);

    /**
     * 查询动态数据源列表
     */
    List<SysDatasourceVo> queryList(SysDatasourceBo bo);

    /**
     * 新增动态数据源
     */
    Boolean insertByBo(SysDatasourceBo bo);

    /**
     * 修改动态数据源
     */
    Boolean updateByBo(SysDatasourceBo bo);

    /**
     * 校验并批量删除动态数据源信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 获取JdbcService
     */
    JdbcService getJdbcService();

    /**
     * 测试连接
     */
    Boolean testConnByBo(SysDatasourceBo bo);

    /**
     * 查询字段数据
     */
    Object queryFieldData(DsFieldQueryBo bo);
}
