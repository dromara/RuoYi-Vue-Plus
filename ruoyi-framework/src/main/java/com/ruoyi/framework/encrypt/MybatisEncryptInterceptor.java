package com.ruoyi.framework.encrypt;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.EncryptField;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.config.properties.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 入参加密拦截器
 *
 * @author 老马
 * @date 2023-01-10 16:42
 */
@Slf4j
@Intercepts({@Signature(
    type = ParameterHandler.class,
    method = "setParameters",
    args = {PreparedStatement.class})
})
public class MybatisEncryptInterceptor implements Interceptor {

    private final EncryptorManager encryptorManager = SpringUtils.getBean(EncryptorManager.class);
    private final EncryptorProperties defaultProperties = SpringUtils.getBean(EncryptorProperties.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof ParameterHandler) {
            // 进行加密操作
            ParameterHandler parameterHandler = (ParameterHandler) target;
            Object parameterObject = parameterHandler.getParameterObject();
            if (ObjectUtil.isNotNull(parameterObject) && !(parameterObject instanceof String)) {
                this.encryptHandler(parameterObject);
            }
        }
        return target;
    }

    /**
     * 加密对象
     *
     * @param sourceObject 待加密对象
     */
    private void encryptHandler(Object sourceObject) {
        if (sourceObject instanceof Map) {
            ((Map<?, Object>) sourceObject).values().forEach(this::encryptHandler);
            return;
        }
        if (sourceObject instanceof List) {
            // 判断第一个元素是否含有注解。如果没有直接返回，提高效率
            Object firstItem = ((List<?>) sourceObject).get(0);
            if (CollectionUtil.isEmpty(EncryptedFieldsCache.get(firstItem.getClass()))) {
                return;
            }
            ((List<?>) sourceObject).forEach(this::encryptHandler);
            return;
        }
        Set<Field> fields = EncryptedFieldsCache.get(sourceObject.getClass());
        try {
            for (Field field : fields) {
                field.set(sourceObject, this.encryptField(String.valueOf(field.get(sourceObject)), field));
            }
        } catch (Exception e) {
            log.error("处理加密字段时出错", e);
        }
    }

    /**
     * 字段值进行加密。通过字段的批注注册新的加密算法
     *
     * @param value 待加密的值
     * @param field 待加密字段
     * @return 加密后结果
     */
    private String encryptField(String value, Field field) {
        EncryptField encryptField = field.getAnnotation(EncryptField.class);
        EncryptorProperties properties = new EncryptorProperties();
        properties.setEnabled(true);
        properties.setAlgorithm(encryptField.algorithm());
        properties.setEncode(encryptField.encode());
        properties.setPassword(StringUtils.isEmpty(encryptField.password()) ? defaultProperties.getPassword() : encryptField.password());
        properties.setPrivateKey(StringUtils.isEmpty(encryptField.privateKey()) ? defaultProperties.getPrivateKey() : encryptField.privateKey());
        properties.setPublicKey(StringUtils.isEmpty(encryptField.publicKey()) ? defaultProperties.getPublicKey() : encryptField.publicKey());
        this.encryptorManager.registAndGetEncryptor(properties);
        return this.encryptorManager.encrypt(value, properties);
    }


    @Override
    public void setProperties(Properties properties) {
    }
}
