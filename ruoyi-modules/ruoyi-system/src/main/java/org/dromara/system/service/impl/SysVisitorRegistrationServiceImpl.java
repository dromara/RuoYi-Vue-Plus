package org.dromara.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysVisitorRegistration;
import org.dromara.system.domain.bo.SysVisitorRegistrationBo;
import org.dromara.system.domain.vo.SysVisitorRegistrationVo;
import org.dromara.system.mapper.SysVisitorRegistrationMapper;
import org.dromara.system.service.ISysVisitorRegistrationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 访客预约登记 服务实现
 *
 * @author System
 */
@RequiredArgsConstructor
@Service
public class SysVisitorRegistrationServiceImpl implements ISysVisitorRegistrationService {

    private final SysVisitorRegistrationMapper baseMapper;

    /**
     * 分页查询访客预约登记列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 访客预约登记分页列表
     */
    @Override
    public TableDataInfo<SysVisitorRegistrationVo> selectPageVisitorRegistrationList(SysVisitorRegistrationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysVisitorRegistration> wrapper = buildQueryWrapper(bo);
        Page<SysVisitorRegistrationVo> page = baseMapper.selectVisitorRegistrationPage(pageQuery.build(), bo);
        return TableDataInfo.build(page);
    }

    /**
     * 查询访客预约登记列表
     *
     * @param bo 查询条件
     * @return 访客预约登记列表
     */
    @Override
    public List<SysVisitorRegistrationVo> selectVisitorRegistrationList(SysVisitorRegistrationBo bo) {
        return baseMapper.selectVisitorRegistrationList(bo);
    }

    /**
     * 根据ID查询访客预约登记详情
     *
     * @param id 主键ID
     * @return 访客预约登记详情
     */
    @Override
    public SysVisitorRegistrationVo selectVisitorRegistrationById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 新增访客预约登记
     *
     * @param bo 访客预约登记信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertVisitorRegistration(SysVisitorRegistrationBo bo) {
        SysVisitorRegistration entity = BeanUtil.toBean(bo, SysVisitorRegistration.class);
        entity.setStatus(SysVisitorRegistration.STATUS_APPOINTMENT);
        validEntityBeforeSave(entity);
        return baseMapper.insert(entity);
    }

    /**
     * 修改访客预约登记
     *
     * @param bo 访客预约登记信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVisitorRegistration(SysVisitorRegistrationBo bo) {
        SysVisitorRegistration entity = BeanUtil.toBean(bo, SysVisitorRegistration.class);
        validEntityBeforeSave(entity);
        return baseMapper.updateById(entity);
    }

    /**
     * 删除访客预约登记
     *
     * @param ids 需要删除的主键ID集合
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteVisitorRegistrationByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids);
    }

    /**
     * 访客签到
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    @Transactional
    public int checkIn(Long id) {
        SysVisitorRegistration registration = baseMapper.selectById(id);
        if (registration == null) {
            throw new ServiceException("访客预约登记不存在");
        }
        if (!SysVisitorRegistration.STATUS_APPOINTMENT.equals(registration.getStatus())) {
            throw new ServiceException("只有预约中的访客才能进行签到操作");
        }
        
        registration.setStatus(SysVisitorRegistration.STATUS_CHECKED_IN);
        registration.setCheckInTime(new Date());
        return baseMapper.updateById(registration);
    }

    /**
     * 访客签离
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    @Transactional
    public int checkOut(Long id) {
        SysVisitorRegistration registration = baseMapper.selectById(id);
        if (registration == null) {
            throw new ServiceException("访客预约登记不存在");
        }
        if (!SysVisitorRegistration.STATUS_CHECKED_IN.equals(registration.getStatus())) {
            throw new ServiceException("只有已签到的访客才能进行签离操作");
        }
        
        registration.setStatus(SysVisitorRegistration.STATUS_CHECKED_OUT);
        registration.setCheckOutTime(new Date());
        return baseMapper.updateById(registration);
    }

    /**
     * 取消访客预约
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    @Transactional
    public int cancelAppointment(Long id) {
        SysVisitorRegistration registration = baseMapper.selectById(id);
        if (registration == null) {
            throw new ServiceException("访客预约登记不存在");
        }
        if (!SysVisitorRegistration.STATUS_APPOINTMENT.equals(registration.getStatus())) {
            throw new ServiceException("只有预约中的访客才能取消预约");
        }
        
        registration.setStatus(SysVisitorRegistration.STATUS_CANCELLED);
        return baseMapper.updateById(registration);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SysVisitorRegistration> buildQueryWrapper(SysVisitorRegistrationBo bo) {
        LambdaQueryWrapper<SysVisitorRegistration> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getVisitorName()), SysVisitorRegistration::getVisitorName, bo.getVisitorName());
        wrapper.eq(StringUtils.isNotBlank(bo.getVisitorPhone()), SysVisitorRegistration::getVisitorPhone, bo.getVisitorPhone());
        wrapper.like(StringUtils.isNotBlank(bo.getVisitPurpose()), SysVisitorRegistration::getVisitPurpose, bo.getVisitPurpose());
        wrapper.eq(bo.getDeptId() != null, SysVisitorRegistration::getDeptId, bo.getDeptId());
        wrapper.eq(StringUtils.isNotBlank(bo.getStatus()), SysVisitorRegistration::getStatus, bo.getStatus());
        
        // 预约时间范围查询
        if (bo.getAppointmentTimeStart() != null) {
            wrapper.ge(SysVisitorRegistration::getAppointmentTime, bo.getAppointmentTimeStart());
        }
        if (bo.getAppointmentTimeEnd() != null) {
            wrapper.le(SysVisitorRegistration::getAppointmentTime, bo.getAppointmentTimeEnd());
        }
        
        // 状态列表查询
        if (bo.getStatusList() != null && bo.getStatusList().length > 0) {
            wrapper.in(SysVisitorRegistration::getStatus, bo.getStatusList());
        }
        
        wrapper.orderByDesc(SysVisitorRegistration::getCreateTime);
        return wrapper;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysVisitorRegistration entity) {
        // 校验预约时间不能是过去时间
        if (entity.getAppointmentTime() != null) {
            Date now = new Date();
            if (entity.getAppointmentTime().before(now)) {
                throw new ServiceException("预约到访时间不能早于当前时间");
            }
        }
        
        // 校验联系电话格式
        if (StringUtils.isNotBlank(entity.getVisitorPhone())) {
            String phone = entity.getVisitorPhone();
            if (!phone.matches("^1[3-9]\\d{9}$|^0\\d{2,3}-?\\d{7,8}$")) {
                throw new ServiceException("联系电话格式不正确");
            }
        }
    }
}