package org.dromara.workflow.de.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目自有 RSA 真密钥对 override（修复 A62 揭穿的底座 placeholder DER 编码坏）。
 *
 * <p><strong>问题源头</strong>：底座 {@code application.yml} 第 155-158 行的
 * {@code api-decrypt.public-key} / {@code api-decrypt.private-key} 是 1024-bit RSA
 * placeholder，但<strong>私钥 DER 编码坏</strong>——JDK
 * {@code KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec)} 抛
 * {@code InvalidKeySpecException}（已被 EncryptUtils.validateRsaPrivateKey
 * 捕获并包装为 IllegalArgumentException），导致 ruoyi-common-encrypt 的
 * CryptoFilter bean 创建失败，整个 ruoyi-admin 启动崩溃。</p>
 *
 * <p><strong>修复策略</strong>：本类在项目自有 {@code de/} 子包注入一对<strong>实测 JDK + Hutool
 * 双通过的</strong>真 RSA 1024-bit 密钥对，通过两条独立路径覆盖：
 * <ol>
 *   <li><strong>static 块</strong>：类加载时立即 {@code System.setProperty(...)}，确保即便
 *   Spring Bean 初始化时序异常，最早的 PropertySource 也已是真密钥；</li>
 *   <li><strong>{@link EnvironmentAware}</strong>：{@code @Configuration} 在
 *   {@code ComponentScan} 阶段先于 {@code @AutoConfiguration} 类（如
 *   {@code ApiDecryptAutoConfiguration}）被处理，故 {@code setEnvironment()} 调用时
 *   {@code api-decrypt.public-key} / {@code api-decrypt.private-key} 尚未被绑定到
 *   {@code ApiDecryptProperties}——本类向 {@code ConfigurableEnvironment} 注入一个
 *   {@code MapPropertySource}，并 {@code addFirst}，确保
 *   {@code @ConfigurationProperties(prefix = "api-decrypt")} 读到的是真密钥。</li>
 * </ol></p>
 *
 * <p><strong>G-1 红线（不改底座）</strong>：本类位于项目自有
 * {@code org.dromara.workflow.de.config} 包下，不在底座 vendored 范围（见
 * {@code docs/对底座的偏离登记.md}）。底座 {@code application.yml:155-158} 与
 * {@code ruoyi-common-encrypt/src/main/java/.../ApiDecryptAutoConfiguration.java}
 * 均未改动。</p>
 *
 * <p><strong>类名约定</strong>：名为 {@code DeSaTokenConfig}（按任务规约命名），实际
 * override 的是 {@code api-decrypt} 的公私钥——任务规约的"SaToken"为简称误用，
 * 真源在 {@code ruoyi-common-encrypt} 模块的 {@code ApiDecryptProperties}。</p>
 *
 * @author AIP RSA 密钥对实修
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeSaTokenConfig implements EnvironmentAware {

    /**
     * 真 RSA 1024-bit 公钥（X.509 DER，base64）。
     * <p>生成命令：
     * <pre>
     * openssl genrsa -out private_pkcs1.pem 1024
     * openssl pkcs8 -topk8 -inform PEM -outform PEM -in private_pkcs1.pem -out private_pkcs8.pem -nocrypt
     * openssl rsa -in private_pkcs8.pem -pubout -out public.pem
     * </pre>
     * <p>JDK 校验：{@code PUB_JDK_OK=true}（{@code X509EncodedKeySpec} 解码通过）。</p>
     */
    public static final String RSA_PUBLIC_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLkcEbzbcchS2QhOSIDbelZmaU6nCqmqpikMxQbsj21bJZAWOEBDGmY4WAGkruaGNCofesdVhbbnTf6qxU7iszugBYQ+sqdhOynWnTOPwzTH3wJ263yCqNfg4S2Wq8VnLnvUYSokEyguDJ/+D71jvB2+BMpUWBAFLclbrYPoN0/wIDAQAB";

    /**
     * 真 RSA 1024-bit 私钥（PKCS#8 DER，base64）。
     * <p>JDK 校验：{@code PRIV_JDK_OK=true}（{@code PKCS8EncodedKeySpec} 解码通过）。</p>
     * <p>Hutool 双向加解实测：{@code MATCH_ENCRYPT=true} 且 {@code MATCH_ENCRYPT_REVERSE=true}。</p>
     */
    public static final String RSA_PRIVATE_KEY =
        "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAMuRwRvNtxyFLZCE5IgNt6VmZpTqcKqaqmKQzFBuyPbVslkBY4QEMaZjhYAaSu5oY0Kh96x1WFtudN/qrFTuKzO6AFhD6yp2E7KdadM4/DNMffAnbrfIKo1+DhLZarxWcue9RhKiQTKC4Mn/4PvWO8Hb4EylRYEAUtyVutg+g3T/AgMBAAECgYBlZeCOfNVsu1mK1ucObea/2FXYbz5XfFrKZd2fYs9vQ9pmQy7Sn1Qr0P66ZF5WimCG3C6GFJ1CTa6hqj7Uvx187qE9rFEyu+jkQn21un+tUFuJenIOwtibKoaUoExs2fWOFuG4N9lz2yDW0s3XttWVhiczEVu0/eKRBK8+r+7SgQJBAPJcbynFOIkxu+zIhdatwyD44ER+Y/lXHF9PNXgDJGJKm/fanUPZBg8NwrjPvOnlHSGuC4meRblUO6U0Sin0zC8CQQDXBnf5cIQQPPaBr+MS5rQNNf+fN6Z9HYzjYdz7QonBoooY7LxoFjWjj+JT/G24UactGB1X5Al7knJU3z0/5aAxAkEAuIuX/dd2IEmljjx2ZSy063N22dDf8WnaadyXm0XRO6MSNl3Z6GLl0SqqPi247OUQFKhBks3Yy7EPJdG2M1J68wJAeuHifHW1s0XzpierQGkBqq8aV5ZJGiViF6wcyqa67SMDgrz/6wgOPVwD/vU6iuR3AolOc6UJWkpV9nThnvuOcQJAelt8gKUYvopqsuYl2azsrn7yai13BaXO9dAB6QAASN8e043iwUcHcK+0qsgT5a+/F3ahuge30SZrS6RDovE4Zw==";

    /**
     * PropertySource 名称（用于在日志/调试时识别）。
     */
    private static final String RSA_PROPERTY_SOURCE_NAME = "deRsaOverride";

    static {
        // 第一条防线：类加载时立即写系统属性。优先级低于 command-line / SPRING_APPLICATION_JSON，
        // 但高于 application.yml 文件加载顺序中的默认 profile。
        System.setProperty("api-decrypt.public-key", RSA_PUBLIC_KEY);
        System.setProperty("api-decrypt.private-key", RSA_PRIVATE_KEY);
    }

    @Override
    public void setEnvironment(Environment environment) {
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            return;
        }
        Map<String, Object> overrides = new HashMap<>(4);
        overrides.put("api-decrypt.public-key", RSA_PUBLIC_KEY);
        overrides.put("api-decrypt.private-key", RSA_PRIVATE_KEY);

        // 第二条防线：向 Environment 头部注入 MapPropertySource，addFirst 让
        // @ConfigurationProperties(prefix = "api-decrypt") 解析 ApiDecryptProperties
        // 时第一个命中这里，从而在 EncryptUtils.validateRsaPublicKey / validateRsaPrivateKey
        // 抛 IllegalArgumentException 之前读到真密钥。
        configurableEnvironment.getPropertySources()
            .addFirst(new MapPropertySource(RSA_PROPERTY_SOURCE_NAME, overrides));
    }
}