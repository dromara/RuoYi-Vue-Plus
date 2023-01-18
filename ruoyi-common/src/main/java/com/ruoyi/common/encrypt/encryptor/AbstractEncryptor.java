package com.ruoyi.common.encrypt.encryptor;

import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.encrypt.IEncryptor;

/**
 * 所有加密执行者的基类
 *
 * @author 老马
 * @date 2023-01-17 16:52
 */
public abstract class AbstractEncryptor implements IEncryptor {
    public AbstractEncryptor(EncryptContext context) {
        //子类必须实现带参数的构造方法
    }
}
