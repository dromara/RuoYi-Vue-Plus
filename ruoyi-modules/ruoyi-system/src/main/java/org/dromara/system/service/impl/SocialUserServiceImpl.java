package org.dromara.system.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.springframework.stereotype.Service;
import org.dromara.system.domain.bo.SocialUserBo;
import org.dromara.system.domain.vo.SocialUserVo;
import org.dromara.system.domain.SocialUser;
import org.dromara.system.mapper.SocialUserMapper;
import org.dromara.system.service.ISocialUserService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

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
     * 新增社会化关系
     */
    @Override
    public Boolean insertByBo(SocialUserBo bo) {
        SocialUser add = MapstructUtils.convert(bo, SocialUser.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
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
     * 根据用户id和来源查询用户是否存在
     *
     * @param userId 用户id
     * @param source 来源
     * @return 是否存在
     */
    @Override
    public Boolean isExistByUserIdAndSource(Long userId, String source) {
        LambdaQueryWrapper<SocialUser> lqw = Wrappers.lambdaQuery();
        lqw.eq(SocialUser::getUserId, userId);
        lqw.eq(SocialUser::getSource, source);
        return baseMapper.selectCount(lqw) > 0;
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
