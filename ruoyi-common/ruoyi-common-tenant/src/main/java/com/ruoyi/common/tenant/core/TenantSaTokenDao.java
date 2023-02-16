package com.ruoyi.common.tenant.core;

import com.ruoyi.common.core.constant.GlobalConstants;
import com.ruoyi.common.satoken.core.dao.PlusSaTokenDao;

import java.util.List;

/**
 * SaToken 认证数据持久层 适配多租户
 *
 * @author Lion Li
 */
public class TenantSaTokenDao extends PlusSaTokenDao {

    @Override
    public String get(String key) {
        return super.get(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public void set(String key, String value, long timeout) {
        super.set(GlobalConstants.GLOBAL_REDIS_KEY + key, value, timeout);
    }

    /**
     * 修修改指定key-value键值对 (过期时间不变)
     */
    @Override
    public void update(String key, String value) {
        super.update(GlobalConstants.GLOBAL_REDIS_KEY + key, value);
    }

    /**
     * 删除Value
     */
    @Override
    public void delete(String key) {
        super.delete(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    /**
     * 获取Value的剩余存活时间 (单位: 秒)
     */
    @Override
    public long getTimeout(String key) {
        return super.getTimeout(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    /**
     * 修改Value的剩余存活时间 (单位: 秒)
     */
    @Override
    public void updateTimeout(String key, long timeout) {
        super.updateTimeout(GlobalConstants.GLOBAL_REDIS_KEY + key, timeout);
    }


    /**
     * 获取Object，如无返空
     */
    @Override
    public Object getObject(String key) {
        return super.getObject(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    /**
     * 写入Object，并设定存活时间 (单位: 秒)
     */
    @Override
    public void setObject(String key, Object object, long timeout) {
        super.setObject(GlobalConstants.GLOBAL_REDIS_KEY + key, object, timeout);
    }

    /**
     * 更新Object (过期时间不变)
     */
    @Override
    public void updateObject(String key, Object object) {
        super.updateObject(GlobalConstants.GLOBAL_REDIS_KEY + key, object);
    }

    /**
     * 删除Object
     */
    @Override
    public void deleteObject(String key) {
        super.deleteObject(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    /**
     * 获取Object的剩余存活时间 (单位: 秒)
     */
    @Override
    public long getObjectTimeout(String key) {
        return super.getObjectTimeout(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    /**
     * 修改Object的剩余存活时间 (单位: 秒)
     */
    @Override
    public void updateObjectTimeout(String key, long timeout) {
        super.updateObjectTimeout(GlobalConstants.GLOBAL_REDIS_KEY + key, timeout);
    }


    /**
     * 搜索数据
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        return super.searchData(GlobalConstants.GLOBAL_REDIS_KEY + prefix, keyword, start, size, sortType);
    }
}
