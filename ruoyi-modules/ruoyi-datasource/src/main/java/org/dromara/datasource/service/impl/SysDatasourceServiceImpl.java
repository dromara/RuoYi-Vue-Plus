package org.dromara.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.domain.bo.DsFieldQueryBo;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.jdbc.JdbcService;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.dromara.datasource.service.ISysDatasourceService;
import org.dromara.datasource.utils.Assert;
import org.dromara.datasource.utils.SqlUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态数据源Service业务层处理
 *
 * @author ixyxj
 * @date 2024-05-02
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysDatasourceServiceImpl implements ISysDatasourceService {

    private final JdbcService jdbcService;
    private final SysDatasourceMapper baseMapper;

    @PostConstruct
    public void init() {
        List<SysDatasource> datasourceList = baseMapper.selectList();
        jdbcService.addDataSource(datasourceList);
        log.info("添加动态数据源:{}", datasourceList.stream().map(SysDatasource::getDsName).collect(Collectors.joining(",")));
    }

    /**
     * 查询动态数据源
     */
    @Override
    public SysDatasourceVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    @Override
    public SysDatasourceVo queryByName(String name) {
        return baseMapper.selectVoOne(Wrappers.<SysDatasource>lambdaQuery().eq(SysDatasource::getDsName, name));
    }

    /**
     * 查询动态数据源列表
     */
    @Override
    public TableDataInfo<SysDatasourceVo> queryPageList(SysDatasourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        Page<SysDatasourceVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询动态数据源列表
     */
    @Override
    public List<SysDatasourceVo> queryList(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysDatasource> buildQueryWrapper(SysDatasourceBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysDatasource> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDsName()), SysDatasource::getDsName, bo.getDsName());
        lqw.eq(StringUtils.isNotBlank(bo.getDsType()), SysDatasource::getDsType, bo.getDsType());
        lqw.like(StringUtils.isNotBlank(bo.getUsername()), SysDatasource::getUsername, bo.getUsername());
        lqw.eq(StringUtils.isNotBlank(bo.getDriverClassName()), SysDatasource::getDriverClassName, bo.getDriverClassName());
        return lqw;
    }

    /**
     * 新增动态数据源
     */
    @Override
    public Boolean insertByBo(SysDatasourceBo bo) {
        SysDatasource add = MapstructUtils.convert(bo, SysDatasource.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            jdbcService.addDataSource(bo.getDsName(), bo.getConnUrl(), bo.getUsername(), bo.getPassword(), bo.getDriverClassName());
        }
        return flag;
    }

    /**
     * 修改动态数据源
     */
    @Override
    public Boolean updateByBo(SysDatasourceBo bo) {
        SysDatasource update = MapstructUtils.convert(bo, SysDatasource.class);
        validEntityBeforeSave(update);
        jdbcService.updateDataSource(bo.getDsName(), bo.getConnUrl(), bo.getUsername(), bo.getPassword(), bo.getDriverClassName());
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysDatasource entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除动态数据源
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        baseMapper.selectBatchIds(ids, sysDatasource -> jdbcService.removeDataSource(sysDatasource.getResultObject().getDsName()));
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public JdbcService getJdbcService() {
        return jdbcService;
    }

    @Override
    public Boolean testConnByBo(SysDatasourceBo bo) {
        return jdbcService.testConnection(bo.getConnUrl(), bo.getUsername(), bo.getPassword(), bo.getDriverClassName());
    }

    @Override
    public Object queryFieldData(DsFieldQueryBo bo) {
        SysDatasourceVo datasource = queryByName(bo.getDsName());
        Assert.notNull(datasource, "数据库【%s】不存在".formatted(bo.getDsName()));
        if (datasource != null) {
            return jdbcService.query(datasource.getDsName(), datasource.getDsType(), jdbcQuery -> {
                String sql = SqlUtils.buildSqlString(bo, "`" + bo.getSchema() + "`.`" + bo.getTable() + "`");
                return SqlUtils.queryByOffsets(jdbcQuery.getJdbc(), sql, bo.getOffsets());
            });
        }
        return Lists.newArrayList();
    }
}
