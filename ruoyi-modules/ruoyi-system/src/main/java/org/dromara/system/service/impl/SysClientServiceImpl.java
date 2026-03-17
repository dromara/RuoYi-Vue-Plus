package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.system.domain.SysClient;
import org.dromara.system.domain.bo.SysClientBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.mapper.SysClientMapper;
import org.dromara.system.service.ISysClientService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 客户端管理Service业务层处理
 *
 * @author Michelle.Chung
 * @date 2023-06-18
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysClientServiceImpl implements ISysClientService {

    private final SysClientMapper baseMapper;

    /**
     * 查询客户端管理
     *
     * @param id 主键
     * @return 客户端详情
     */
    @Override
    public SysClientVo queryById(Long id) {
        SysClientVo vo = baseMapper.selectVoById(id);
        vo.setGrantTypeList(StringUtils.splitList(vo.getGrantType()));
        return vo;
    }

    /**
     * 查询客户端管理
     *
     * @param clientId 客户端标识
     * @return 客户端详情
     */
    @Cacheable(cacheNames = CacheNames.SYS_CLIENT, key = "#clientId")
    @Override
    public SysClientVo queryByClientId(String clientId) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysClient>().eq(SysClient::getClientId, clientId));
    }

    /**
     * 查询客户端管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 客户端分页列表
     */
    @Override
    public PageResult<SysClientVo> queryPageList(SysClientBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysClient> lqw = buildQueryWrapper(bo);
        Page<SysClientVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        result.getRecords().forEach(r -> r.setGrantTypeList(StringUtils.splitList(r.getGrantType())));
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询客户端管理列表
     *
     * @param bo 查询条件
     * @return 客户端列表
     */
    @Override
    public List<SysClientVo> queryList(SysClientBo bo) {
        LambdaQueryWrapper<SysClient> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 构造客户端列表查询条件。
     *
     * @param bo 客户端筛选条件
     * @return 包含 clientId、clientKey、状态等条件的查询包装器
     */
    private LambdaQueryWrapper<SysClient> buildQueryWrapper(SysClientBo bo) {
        LambdaQueryWrapper<SysClient> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getClientId()), SysClient::getClientId, bo.getClientId());
        lqw.eq(StringUtils.isNotBlank(bo.getClientKey()), SysClient::getClientKey, bo.getClientKey());
        lqw.eq(StringUtils.isNotBlank(bo.getClientSecret()), SysClient::getClientSecret, bo.getClientSecret());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysClient::getStatus, bo.getStatus());
        lqw.orderByAsc(SysClient::getId);
        return lqw;
    }

    /**
     * 新增客户端管理
     *
     * @param bo 客户端业务对象
     * @return 新增成功返回 {@code true}
     */
    @Override
    public Boolean insertByBo(SysClientBo bo) {
        SysClient add = MapstructUtils.convert(bo, SysClient.class);
        add.setGrantType(CollUtil.join(bo.getGrantTypeList(), StringUtils.SEPARATOR));
        // 生成clientid
        String clientKey = bo.getClientKey();
        String clientSecret = bo.getClientSecret();
        add.setClientId(SecureUtil.md5(clientKey + clientSecret));
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改客户端管理
     *
     * @param bo 客户端业务对象
     * @return 修改成功返回 {@code true}
     */
    @CacheEvict(cacheNames = CacheNames.SYS_CLIENT, key = "#bo.clientId")
    @Override
    public Boolean updateByBo(SysClientBo bo) {
        SysClient update = MapstructUtils.convert(bo, SysClient.class);
        update.setGrantType(StringUtils.joinComma(bo.getGrantTypeList()));
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 修改状态
     *
     * @param clientId 客户端标识
     * @param status   状态值
     * @return 更新条数
     */
    @CacheEvict(cacheNames = CacheNames.SYS_CLIENT, key = "#clientId")
    @Override
    public int updateClientStatus(String clientId, String status) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysClient>()
                .set(SysClient::getStatus, status)
                .eq(SysClient::getClientId, clientId));
    }

    /**
     * 批量删除客户端管理
     *
     * @param ids     主键集合
     * @param isValid 是否执行业务校验
     * @return 删除成功返回 {@code true}
     */
    @CacheEvict(cacheNames = CacheNames.SYS_CLIENT, allEntries = true)
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 校验客户端key是否唯一
     *
     * @param client 客户端信息
     * @return 结果
     */
    @Override
    public boolean checkClickKeyUnique(SysClientBo client) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysClient>()
            .eq(SysClient::getClientKey, client.getClientKey())
            .ne(ObjectUtil.isNotNull(client.getId()), SysClient::getId, client.getId()));
        return !exist;
    }

}
