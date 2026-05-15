package org.dromara.system.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SysSocial;
import org.dromara.system.domain.bo.SysSocialBo;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.mapper.SysSocialMapper;
import org.dromara.system.service.ISysSocialService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 社会化关系Service业务层处理
 *
 * @author thiszhc
 * @date 2023-06-12
 */
@RequiredArgsConstructor
@Service
public class SysSocialServiceImpl implements ISysSocialService {

    private final SysSocialMapper baseMapper;


    /**
     * 查询社会化关系
     *
     * @param id 主键
     * @return 社会化绑定详情
     */
    @Override
    public SysSocialVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 授权列表
     *
     * @param bo 查询条件
     * @return 社会化授权关系列表
     */
    @Override
    public List<SysSocialVo> queryList(SysSocialBo bo) {
        return baseMapper.lambda()
            .eqIfPresent(SysSocial::getUserId, bo.getUserId())
            .eqIfText(SysSocial::getAuthId, bo.getAuthId())
            .eqIfText(SysSocial::getSource, bo.getSource())
            .voList();
    }

    /**
     * 按用户主键查询其绑定的社会化授权列表。
     *
     * @param userId 用户主键
     * @return 用户已绑定的社会化授权列表
     */
    @Override
    public List<SysSocialVo> queryListByUserId(Long userId) {
        return baseMapper.lambda().eq(SysSocial::getUserId, userId).voList();
    }


    /**
     * 新增社会化关系
     *
     * @param bo 业务对象
     * @return 新增成功返回 {@code true}
     */
    @Override
    public Boolean insertByBo(SysSocialBo bo) {
        SysSocial add = MapstructUtils.convert(bo, SysSocial.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            } else {
                return false;
            }
        }
        return flag;
    }

    /**
     * 更新社会化关系
     *
     * @param bo 业务对象
     * @return 更新成功返回 {@code true}
     */
    @Override
    public Boolean updateByBo(SysSocialBo bo) {
        SysSocial update = MapstructUtils.convert(bo, SysSocial.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 待保存的社会化关系实体
     */
    private void validEntityBeforeSave(SysSocial entity) {
        //TODO 做一些数据校验,如唯一约束
    }


    /**
     * 删除社会化关系
     *
     * @param id 主键
     * @return 删除成功返回 {@code true}
     */
    @Override
    public Boolean deleteWithValidById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }


    /**
     * 根据 authId 查询用户信息
     *
     * @param authId 认证id
     * @return 授权信息
     */
    @Override
    public List<SysSocialVo> selectByAuthId(String authId) {
        return baseMapper.lambda().eq(SysSocial::getAuthId, authId).voList();
    }

}
