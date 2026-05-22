package com.wudgaby.stars.support;

import com.wudgaby.stars.config.StarsProperties;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 仓库外链 URL 构建
 */
@RequiredArgsConstructor
@Component
public class StarsDeepLinkBuilder {

    private final StarsProperties starsProperties;

    public String githubUrl(String owner, String repoName, String htmlUrl) {
        if (StringUtils.isNotBlank(htmlUrl)) {
            return htmlUrl;
        }
        return "https://github.com/" + owner + "/" + repoName;
    }

    public String zreadUrl(String owner, String repoName) {
        return applyTemplate(starsProperties.deepLink().zreadTemplate(), owner, repoName);
    }

    public String deepwikiUrl(String owner, String repoName) {
        return applyTemplate(starsProperties.deepLink().deepwikiTemplate(), owner, repoName);
    }

    private static String applyTemplate(String template, String owner, String repoName) {
        if (StringUtils.isBlank(template)) {
            return null;
        }
        return template.replace("{owner}", owner).replace("{repo}", repoName);
    }

}
