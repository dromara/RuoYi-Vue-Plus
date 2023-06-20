package org.dromara.system.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SocialUser;
import org.dromara.system.domain.bo.SocialUserBo;
import org.dromara.system.domain.vo.SocialUserVo;
import org.dromara.system.mapper.SocialUserMapper;
import org.dromara.system.service.ISocialUserService;
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
public class SocialUserServiceImpl implements ISocialUserService {

    private final SocialUserMapper baseMapper;


    /**
     * 查询社会化关系
     */
    @Override
    public SocialUserVo queryById(String id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 授权列表
     */
    @Override
    public List<SocialUserVo> queryList() {
        return baseMapper.selectVoList();
    }



    /**
     * 新增社会化关系
     */
    @Override
    public Boolean insertByBo(SocialUserBo bo) {
        SocialUser add = MapstructUtils.convert(bo, SocialUser.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            }else {
                return false;
            }
        }
        return flag;
    }


    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SocialUser entity) {
        //TODO 做一些数据校验,如唯一约束
    }


    /**
     * 删除社会化关系
     */
    @Override
    public Boolean deleteWithValidById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }


    /**
     * 根据authId查询用户信息
     *
     * @param authId 用户id
     * @return 用户信息
     */
    @Override
    public SocialUserVo selectSocialUserByAuthId(String authId) {
        return baseMapper.selectSocialUserByAuthId(authId);
    }

}
