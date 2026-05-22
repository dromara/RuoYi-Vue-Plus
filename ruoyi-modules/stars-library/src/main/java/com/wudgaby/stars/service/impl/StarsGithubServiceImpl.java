package com.wudgaby.stars.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wudgaby.stars.domain.StarsGithubAccount;
import com.wudgaby.stars.domain.vo.GithubStatusVo;
import com.wudgaby.stars.github.GitHubApiClient;
import com.wudgaby.stars.github.GitHubUser;
import com.wudgaby.stars.mapper.StarsGithubAccountMapper;
import com.wudgaby.stars.service.IStarsGithubService;
import com.wudgaby.stars.config.StarsProperties;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.encrypt.properties.EncryptorProperties;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;

/**
 * GitHub 账号绑定服务实现
 */
@RequiredArgsConstructor
@Service
public class StarsGithubServiceImpl implements IStarsGithubService {

    private final StarsGithubAccountMapper accountMapper;
    private final GitHubApiClient gitHubApiClient;
    private final StarsProperties starsProperties;
    private final EncryptorProperties encryptorProperties;

    @Override
    public void bind(Long userId, String token) {
        GitHubUser githubUser = validateToken(token);
        String encryptedToken = encryptToken(token);
        LocalDateTime now = LocalDateTime.now();

        StarsGithubAccount account = findByUserId(userId);
        if (account == null) {
            account = new StarsGithubAccount();
            account.setId(IdGeneratorUtil.nextLongId());
            account.setUserId(userId);
            account.setBindTime(now);
            account.setGithubLogin(githubUser.login());
            account.setAccessToken(encryptedToken);
            account.setTokenScope(githubUser.scope());
            account.setUpdateTime(now);
            accountMapper.insert(account);
            return;
        }

        account.setGithubLogin(githubUser.login());
        account.setAccessToken(encryptedToken);
        account.setTokenScope(githubUser.scope());
        account.setUpdateTime(now);
        accountMapper.updateById(account);
    }

    @Override
    public void unbind(Long userId) {
        accountMapper.delete(new LambdaQueryWrapper<StarsGithubAccount>()
            .eq(StarsGithubAccount::getUserId, userId));
    }

    @Override
    public GithubStatusVo getStatus(Long userId) {
        StarsGithubAccount account = findByUserId(userId);
        if (account == null) {
            return new GithubStatusVo(false, null);
        }
        return new GithubStatusVo(true, account.getGithubLogin());
    }

    @Override
    public String decryptToken(Long userId) {
        StarsGithubAccount account = findByUserId(userId);
        if (account == null) {
            throw new ServiceException("未绑定 GitHub 账号");
        }
        return decryptTokenValue(account.getAccessToken());
    }

    private GitHubUser validateToken(String token) {
        try {
            GitHubUser user = gitHubApiClient.fetchCurrentUser(token);
            if (user == null || StringUtils.isBlank(user.login())) {
                throw new ServiceException("GitHub Token 无效或已过期");
            }
            return user;
        } catch (WebClientResponseException.Unauthorized ex) {
            throw new ServiceException("GitHub Token 无效或已过期");
        } catch (WebClientResponseException ex) {
            throw new ServiceException("GitHub API 调用失败: " + ex.getStatusCode().value());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("GitHub Token 验证失败");
        }
    }

    private StarsGithubAccount findByUserId(Long userId) {
        return accountMapper.selectOne(new LambdaQueryWrapper<StarsGithubAccount>()
            .eq(StarsGithubAccount::getUserId, userId));
    }

    private String encryptToken(String token) {
        return EncryptUtils.encryptByAes(token, requireEncryptKey());
    }

    private String decryptTokenValue(String encryptedToken) {
        return EncryptUtils.decryptByAes(encryptedToken, requireEncryptKey());
    }

    private String requireEncryptKey() {
        String password = starsProperties.github().tokenEncryptKey();
        if (StringUtils.isBlank(password)) {
            password = encryptorProperties.getPassword();
        }
        if (StringUtils.isBlank(password)) {
            throw new ServiceException("GitHub Token 加密密钥未配置，请设置 stars.github.token-encrypt-key 或 mybatis-encryptor.password");
        }
        return password;
    }
}
