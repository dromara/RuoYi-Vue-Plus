package com.ruoyi.framework.encrypt;

import cn.hutool.core.collection.CollectionUtil;
import com.ruoyi.common.annotation.EncryptField;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.config.properties.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 出参解密拦截器
 *
 * @author 老马
 * @date 2023-01-12 13:47
 */
@Slf4j
@Intercepts({@Signature(
    type = ResultSetHandler.class,
    method = "handleResultSets",
    args = {Statement.class})
})
public class MybatisDecryptInterceptor implements Interceptor {

    private final EncryptorManager encryptorManager = SpringUtils.getBean(EncryptorManager.class);
    private final EncryptorProperties defaultProperties = SpringUtils.getBean(EncryptorProperties.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取执行mysql执行结果
        Object result = invocation.proceed();
        if (result == null) {
            return null;
        }
        decryptHandler(result);
        return result;
    }

    /**
     * 解密对象
     *
     * @param sourceObject 待加密对象
     */
    private void decryptHandler(Object sourceObject) {
        if (sourceObject instanceof Map) {
            ((Map<?, Object>) sourceObject).values().forEach(this::decryptHandler);
            return;
        }
        if (sourceObject instanceof List) {
            // 判断第一个元素是否含有注解。如果没有直接返回，提高效率
            Object firstItem = ((List<?>) sourceObject).get(0);
            if (CollectionUtil.isEmpty(EncryptedFieldsCache.get(firstItem.getClass()))) {
                return;
            }
            ((List<?>) sourceObject).forEach(this::decryptHandler);
            return;
        }
        Set<Field> fields = EncryptedFieldsCache.get(sourceObject.getClass());
        try {
            for (Field field : fields) {
                field.set(sourceObject, this.decryptField(String.valueOf(field.get(sourceObject)), field));
            }
        } catch (Exception e) {
            log.error("处理解密字段时出错", e);
        }
    }

    /**
     * 字段值进行加密。通过字段的批注注册新的加密算法
     *
     * @param value 待加密的值
     * @param field 待加密字段
     * @return 加密后结果
     */
    private String decryptField(String value, Field field) {
        EncryptField encryptField = field.getAnnotation(EncryptField.class);
        EncryptorProperties properties = new EncryptorProperties();
        properties.setEnabled(true);
        properties.setAlgorithm(encryptField.algorithm());
        properties.setEncode(encryptField.encode());
        properties.setPassword(StringUtils.isEmpty(encryptField.password()) ? defaultProperties.getPassword() : encryptField.password());
        properties.setPrivateKey(StringUtils.isEmpty(encryptField.privateKey()) ? defaultProperties.getPrivateKey() : encryptField.privateKey());
        properties.setPublicKey(StringUtils.isEmpty(encryptField.publicKey()) ? defaultProperties.getPublicKey() : encryptField.publicKey());
        this.encryptorManager.registAndGetEncryptor(properties);
        return this.encryptorManager.decrypt(value, properties);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
