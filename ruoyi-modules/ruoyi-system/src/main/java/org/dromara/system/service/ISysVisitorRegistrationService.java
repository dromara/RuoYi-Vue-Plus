package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysVisitorRegistrationBo;
import org.dromara.system.domain.vo.SysVisitorRegistrationVo;

import java.util.List;

/**
 * 访客预约登记 服务层
 *
 * @author System
 */
public interface ISysVisitorRegistrationService {

    /**
     * 分页查询访客预约登记列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 访客预约登记分页列表
     */
    TableDataInfo<SysVisitorRegistrationVo> selectPageVisitorRegistrationList(SysVisitorRegistrationBo bo, PageQuery pageQuery);

    /**
     * 查询访客预约登记列表
     *
     * @param bo 查询条件
     * @return 访客预约登记列表
     */
    List<SysVisitorRegistrationVo> selectVisitorRegistrationList(SysVisitorRegistrationBo bo);

    /**
     * 根据ID查询访客预约登记详情
     *
     * @param id 主键ID
     * @return 访客预约登记详情
     */
    SysVisitorRegistrationVo selectVisitorRegistrationById(Long id);

    /**
     * 新增访客预约登记
     *
     * @param bo 访客预约登记信息
     * @return 结果
     */
    int insertVisitorRegistration(SysVisitorRegistrationBo bo);

    /**
     * 修改访客预约登记
     *
     * @param bo 访客预约登记信息
     * @return 结果
     */
    int updateVisitorRegistration(SysVisitorRegistrationBo bo);

    /**
     * 删除访客预约登记
     *
     * @param ids 需要删除的主键ID集合
     * @return 结果
     */
    int deleteVisitorRegistrationByIds(List<Long> ids);

    /**
     * 访客签到
     *
     * @param id 主键ID
     * @return 结果
     */
    int checkIn(Long id);

    /**
     * 访客签离
     *
     * @param id 主键ID
     * @return 结果
     */
    int checkOut(Long id);

    /**
     * 取消访客预约
     *
     * @param id 主键ID
     * @return 结果
     */
    int cancelAppointment(Long id);
}