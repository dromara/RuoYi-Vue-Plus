package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsTenantSettingVo;
import org.dromara.pms.domain.bo.PmsTenantSettingBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 租户配置Service接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface IPmsTenantSettingService {

    /**
     * 查询租户配置
     */
    PmsTenantSettingVo queryById(Long settingId);

    /**
     * 查询租户配置列表
     */
    TableDataInfo<PmsTenantSettingVo> queryPageList(PmsTenantSettingBo bo, PageQuery pageQuery);

    /**
     * 查询租户配置列表
     */
    List<PmsTenantSettingVo> queryList(PmsTenantSettingBo bo);

    /**
     * 新增租户配置
     */
    Boolean insertByBo(PmsTenantSettingBo bo);

    /**
     * 修改租户配置
     */
    Boolean updateByBo(PmsTenantSettingBo bo);

    /**
     * 校验并批量删除租户配置信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据配置键获取配置值
     */
    String getSettingValue(String settingKey);

    /**
     * 根据配置键获取配置值（支持门店级继承）
     */
    String getSettingValue(String settingKey, Long deptId);

    /**
     * 根据配置分组获取配置
     */
    Map<String, String> getSettingsByGroup(String settingGroup);

    /**
     * 根据配置分组获取配置（支持门店级继承）
     */
    Map<String, String> getSettingsByGroup(String settingGroup, Long deptId);

    /**
     * 批量更新配置
     */
    Boolean batchUpdateSettings(Map<String, String> settings);

    /**
     * 批量更新配置（支持门店级）
     */
    Boolean batchUpdateSettings(Map<String, String> settings, Long deptId);

    /**
     * 重置门店配置为租户默认配置
     */
    Boolean resetDeptSettings(Long deptId, String settingGroup);

    /**
     * 复制租户配置到门店
     */
    Boolean copyTenantSettingsToDept(Long deptId, String settingGroup);

    /**
     * 获取配置继承关系
     */
    List<PmsTenantSettingVo> getSettingInheritance(String settingKey);

    /**
     * 验证配置值格式
     */
    Boolean validateSettingValue(String settingType, String settingValue);

    /**
     * 加密敏感配置值
     */
    String encryptSensitiveValue(String value);

    /**
     * 解密敏感配置值
     */
    String decryptSensitiveValue(String encryptedValue);
}
