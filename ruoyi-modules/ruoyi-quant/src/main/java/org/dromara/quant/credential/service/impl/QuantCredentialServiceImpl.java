package org.dromara.quant.credential.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.domain.bo.QuantCredentialBo;
import org.dromara.quant.credential.domain.vo.QuantCredentialVo;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.security.CredentialMasker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 凭证管理实现，负责加密写入和脱敏输出。
 */
@Service
@RequiredArgsConstructor
public class QuantCredentialServiceImpl implements org.dromara.quant.credential.service.IQuantCredentialService {
    /**
     * 交易凭证 Mapper。
     */
    private final QuantCredentialMapper credentialMapper;
    /**
     * 交易所统一网关。
     */
    private final ExchangeGateway exchangeGateway;

    static void validateSingleBoundUser(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new ServiceException("交易凭证必须指定唯一所属用户");
        }
    }

    /**
     * 按条件分页查询。
     */
    @Override
    public PageResult<QuantCredentialVo> queryPageList(QuantCredentialBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<QuantCredential> lqw = new LambdaQueryWrapper<QuantCredential>();
        lqw.like(StringUtils.isNotBlank(bo.getAccountName()), QuantCredential::getAccountName, bo.getAccountName());
        lqw.eq(bo.getUserId() != null, QuantCredential::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), QuantCredential::getStatus, bo.getStatus());
        lqw.orderByDesc(QuantCredential::getCreateTime);
        applyUserScope(lqw);
        Page<QuantCredential> page = credentialMapper.selectPage(pageQuery.build(), lqw);
        List<QuantCredentialVo> records = page.getRecords().stream().map(this::toVo).toList();
        return PageResult.build(records, page.getTotal());
    }

    /**
     * 按主键查询。
     */
    @Override
    public QuantCredentialVo queryById(Long id) {
        QuantCredential entity = credentialMapper.selectById(id);
        validateOwner(entity);
        return entity == null ? null : toVo(entity);
    }

    /**
     * 根据业务入参新增记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuantCredentialVo insertByBo(QuantCredentialBo bo) {
        QuantCredential entity = MapstructUtils.convert(bo, QuantCredential.class);
        if (!LoginHelper.isSuperAdmin(LoginHelper.getUserId())) {
            entity.setUserId(LoginHelper.getUserId());
        }
        entity.setSecretKey(bo.getSecretKey());
        validateSingleBoundUser(entity.getUserId());
        applyDefaults(entity);
        validateCredential(entity);
        credentialMapper.insert(entity);
        return toVo(entity);
    }

    /**
     * 根据业务入参更新记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuantCredentialVo updateByBo(QuantCredentialBo bo) {
        QuantCredential current = credentialMapper.selectById(bo.getId());
        if (current == null) {
            throw new ServiceException("凭证不存在");
        }
        validateOwner(current);
        QuantCredential entity = MapstructUtils.convert(bo, QuantCredential.class);
        // Bound principal is immutable on ordinary edit. Ownership transfer
        // requires an explicitly authorized, audited workflow.
        entity.setUserId(bo.getUserId());
        entity.setStatus(current.getStatus());
        if (StringUtils.isBlank(entity.getRunMode())) {
            entity.setRunMode(current.getRunMode());
        }
        if (StringUtils.isBlank(bo.getApiKey())) {
            entity.setApiKey(current.getApiKey());
        }
        if (StringUtils.isBlank(bo.getSecretKey())) {
            entity.setSecretKey(current.getSecretKey());
        } else {
            entity.setSecretKey(bo.getSecretKey());
        }
        if (StringUtils.isBlank(entity.getSignatureType())) {
            entity.setSignatureType(current.getSignatureType());
        }
        validateSingleBoundUser(entity.getUserId());
        boolean modeChanged = !java.util.Objects.equals(current.getRunMode(), entity.getRunMode());
        boolean credentialChanged = StringUtils.isNotBlank(bo.getApiKey()) || StringUtils.isNotBlank(bo.getSecretKey());
        if (modeChanged || credentialChanged || "PAPER".equalsIgnoreCase(entity.getRunMode())) {
            // PAPER 永不访问 private API；从 PAPER 切到 TESTNET/LIVE 时则必须重新验证真实凭证。
            validateCredential(entity);
        } else {
            entity.setConnectionStatus(current.getConnectionStatus());
            entity.setTradePermission(current.getTradePermission());
            entity.setWithdrawalDisabled(current.getWithdrawalDisabled());
            entity.setLastConnectTime(current.getLastConnectTime());
        }
        credentialMapper.updateById(entity);
        return toVo(entity);
    }

    /**
     * 业务校验后批量删除记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        ids.forEach(id -> validateOwner(credentialMapper.selectById(id)));
        return credentialMapper.deleteByIds(ids) > 0;
    }

    /**
     * 修改启停状态。
     */
    @Override
    public void changeStatus(Long id, String status) {
        QuantCredential credential = credentialMapper.selectById(id);
        if (credential == null) {
            throw new ServiceException("凭证不存在");
        }
        validateOwner(credential);
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new ServiceException("凭证状态不合法");
        }
        if ("0".equals(status) && !"NORMAL".equals(credential.getConnectionStatus()) && !"PAPER".equals(credential.getConnectionStatus())) {
            throw new ServiceException("凭证连接状态不是正常，无法启用");
        }
        credential.setStatus(status);
        credentialMapper.updateById(credential);
    }

    /**
     * 验证交易所连通性。
     */
    @Override
    public void testConnectivity(Long id) {
        QuantCredential credential = credentialMapper.selectById(id);
        if (credential == null) {
            throw new ServiceException("凭证不存在");
        }
        validateOwner(credential);
        if ("PAPER".equalsIgnoreCase(credential.getRunMode())) {
            credential.setConnectionStatus("PAPER");
            credential.setTradePermission(true);
            credential.setWithdrawalDisabled(true);
            credential.setLastConnectTime(LocalDateTime.now());
            credentialMapper.updateById(credential);
            return;
        }
        ExchangeGateway.ConnectivityResult result = exchangeGateway.validateCredential(new ExchangeGateway.Credential(credential.getApiKey(), credential.getSecretKey(),
            credential.getSignatureType(), credential.getRunMode()));
        credential.setConnectionStatus(result.success() ? "NORMAL" : "FAILED");
        credential.setTradePermission(result.canTrade());
        credential.setWithdrawalDisabled(result.withdrawalDisabled());
        credential.setLastConnectTime(LocalDateTime.now());
        credentialMapper.updateById(credential);
        if (!result.success()) {
            throw new ServiceException("凭证验证失败：" + result.message());
        }
    }

    private void applyDefaults(QuantCredential entity) {
        if (StringUtils.isBlank(entity.getRunMode())) {
            entity.setRunMode("PAPER");
        }
        if (StringUtils.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        entity.setConnectionStatus("FAILED");
    }

    private void validateCredential(QuantCredential credential) {
        if ("PAPER".equalsIgnoreCase(credential.getRunMode())) {
            credential.setConnectionStatus("PAPER");
            credential.setTradePermission(true);
            credential.setWithdrawalDisabled(true);
            credential.setLastConnectTime(LocalDateTime.now());
            return;
        }
        if (StringUtils.isBlank(credential.getApiKey()) || StringUtils.isBlank(credential.getSecretKey())) {
            throw new ServiceException("TESTNET/LIVE 模式必须配置 API Key 和 Secret Key");
        }
        if (StringUtils.isBlank(credential.getSignatureType())) {
            throw new ServiceException("TESTNET/LIVE 模式必须配置签名方式");
        }
        ExchangeGateway.ConnectivityResult result = exchangeGateway.validateCredential(new ExchangeGateway.Credential(credential.getApiKey(), credential.getSecretKey(),
            credential.getSignatureType(), credential.getRunMode()));
        if (!result.success()) {
            throw new ServiceException("凭证验证失败：" + result.message());
        }
        credential.setConnectionStatus("NORMAL");
        credential.setTradePermission(result.canTrade());
        credential.setWithdrawalDisabled(result.withdrawalDisabled());
        credential.setLastConnectTime(LocalDateTime.now());
    }

    private QuantCredentialVo toVo(QuantCredential entity) {
        QuantCredentialVo vo = MapstructUtils.convert(entity, QuantCredentialVo.class);
        vo.setMaskedApiKey(CredentialMasker.maskApiKey(entity.getApiKey()));
        return vo;
    }

    private void applyUserScope(LambdaQueryWrapper<QuantCredential> wrapper) {
        Long userId = LoginHelper.getUserId();
        if (!LoginHelper.isSuperAdmin(userId)) {
            wrapper.eq(QuantCredential::getUserId, userId);
        }
    }

    private void validateOwner(QuantCredential entity) {
        if (entity == null) {
            return;
        }
        Long userId = LoginHelper.getUserId();
        if (!LoginHelper.isSuperAdmin(userId) && !userId.equals(entity.getUserId())) {
            throw new ServiceException("无权访问该交易账户");
        }
    }
}
