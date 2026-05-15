package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
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
import java.util.function.UnaryOperator;

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

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

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
        fillClientRuleFields(vo);
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
        SysClientVo vo = baseMapper.lambda().eq(SysClient::getClientId, clientId).voOne();
        fillClientRuleFields(vo);
        return vo;
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
        result.getRecords().forEach(this::fillClientRuleFields);
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
        List<SysClientVo> list = baseMapper.selectVoList(lqw);
        list.forEach(this::fillClientRuleFields);
        return list;
    }

    /**
     * 构造客户端列表查询条件。
     *
     * @param bo 客户端筛选条件
     * @return 包含 clientId、clientKey、状态等条件的查询包装器
     */
    private LambdaQueryWrapper<SysClient> buildQueryWrapper(SysClientBo bo) {
        return QueryBuilder.lambda(SysClient.class)
            .eqIfText(SysClient::getClientId, bo.getClientId())
            .eqIfText(SysClient::getClientKey, bo.getClientKey())
            .eqIfText(SysClient::getClientSecret, bo.getClientSecret())
            .eqIfText(SysClient::getStatus, bo.getStatus())
            .orderByAsc(SysClient::getId)
            .build();
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
        add.setAccessPath(resolveRuleValue(bo.getAccessPath(), bo.getAccessPathList(), this::normalizeAccessPath));
        add.setIpWhitelist(resolveRuleValue(bo.getIpWhitelist(), bo.getIpWhitelistList(), UnaryOperator.identity()));
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
        update.setAccessPath(resolveRuleValue(bo.getAccessPath(), bo.getAccessPathList(), this::normalizeAccessPath));
        update.setIpWhitelist(resolveRuleValue(bo.getIpWhitelist(), bo.getIpWhitelistList(), UnaryOperator.identity()));
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
        return baseMapper.lambda()
            .set(SysClient::getStatus, status)
            .eq(SysClient::getClientId, clientId)
            .updateCount();
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
        boolean exist = baseMapper.lambda()
            .eq(SysClient::getClientKey, client.getClientKey())
            .neIfPresent(SysClient::getId, client.getId())
            .exists();
        return !exist;
    }

    /**
     * 回填客户端扩展规则字段，便于前端直接展示和编辑。
     *
     * @param vo 客户端视图对象
     */
    private void fillClientRuleFields(SysClientVo vo) {
        if (ObjectUtil.isNull(vo)) {
            return;
        }
        vo.setGrantTypeList(StringUtils.splitList(vo.getGrantType()));
        vo.setAccessPathList(parseRuleList(vo.getAccessPath(), this::normalizeAccessPath));
        vo.setIpWhitelistList(parseRuleList(vo.getIpWhitelist(), UnaryOperator.identity()));
    }

    /**
     * 统一处理白名单与路径规则的入库格式。
     *
     * @param rawValue 原始字符串
     * @param listValue 列表值
     * @param normalizer 单条规则归一化器
     * @return 逗号拼接后的规则串
     */
    private String resolveRuleValue(String rawValue, List<String> listValue, UnaryOperator<String> normalizer) {
        List<String> rules = CollUtil.isNotEmpty(listValue)
            ? listValue
            : StringUtils.str2List(rawValue, CLIENT_RULE_SEPARATOR_REGEX, true, true);
        if (CollUtil.isEmpty(rules)) {
            return listValue != null || rawValue != null ? "" : null;
        }
        return CollUtil.join(rules.stream()
            .map(normalizer)
            .filter(StringUtils::isNotBlank)
            .toList(), StringUtils.SEPARATOR);
    }

    /**
     * 将规则串转换为列表。
     *
     * @param value 规则串
     * @param normalizer 单条规则归一化器
     * @return 规则列表
     */
    private List<String> parseRuleList(String value, UnaryOperator<String> normalizer) {
        return StringUtils.str2List(value, CLIENT_RULE_SEPARATOR_REGEX, true, true).stream()
            .map(normalizer)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    /**
     * 统一补齐路径前导斜杠，避免配置成 app/** 时无法命中。
     *
     * @param path 路径规则
     * @return 规范化后的路径规则
     */
    private String normalizeAccessPath(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String accessPath = StringUtils.trim(path);
        if (StringUtils.isBlank(accessPath)) {
            return null;
        }
        if (StringUtils.equals(accessPath, "*") || StringUtils.equals(accessPath, "/**")) {
            return "/**";
        }
        return accessPath.startsWith(StringUtils.SLASH) ? accessPath : StringUtils.SLASH + accessPath;
    }

}
