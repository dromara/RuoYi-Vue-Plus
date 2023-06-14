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
     * 查询社会化关系
     */
    @Override
    public SocialUserVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询社会化关系列表
     */
    @Override
    public TableDataInfo<SocialUserVo> queryPageList(SocialUserBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SocialUser> lqw = buildQueryWrapper(bo);
        Page<SocialUserVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询社会化关系列表
     */
    @Override
    public List<SocialUserVo> queryList(SocialUserBo bo) {
        LambdaQueryWrapper<SocialUser> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SocialUser> buildQueryWrapper(SocialUserBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SocialUser> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getAuthId()), SocialUser::getAuthId, bo.getAuthId());
        lqw.eq(StringUtils.isNotBlank(bo.getSource()), SocialUser::getSource, bo.getSource());
        lqw.eq(StringUtils.isNotBlank(bo.getAccessToken()), SocialUser::getAccessToken, bo.getAccessToken());
        lqw.eq(bo.getExpireIn() !=  0, SocialUser::getExpireIn, bo.getExpireIn());
        lqw.eq(StringUtils.isNotBlank(bo.getRefreshToken()), SocialUser::getRefreshToken, bo.getRefreshToken());
        lqw.eq(StringUtils.isNotBlank(bo.getOpenId()), SocialUser::getOpenId, bo.getOpenId());
        lqw.eq(StringUtils.isNotBlank(bo.getScope()), SocialUser::getScope, bo.getScope());
        lqw.eq(StringUtils.isNotBlank(bo.getAccessCode()), SocialUser::getAccessCode, bo.getAccessCode());
        lqw.eq(StringUtils.isNotBlank(bo.getUnionId()), SocialUser::getUnionId, bo.getUnionId());
        lqw.eq(StringUtils.isNotBlank(bo.getScope()), SocialUser::getScope, bo.getScope());
        lqw.eq(StringUtils.isNotBlank(bo.getTokenType()), SocialUser::getTokenType, bo.getTokenType());
        lqw.eq(StringUtils.isNotBlank(bo.getIdToken()), SocialUser::getIdToken, bo.getIdToken());
        lqw.eq(StringUtils.isNotBlank(bo.getMacAlgorithm()), SocialUser::getMacAlgorithm, bo.getMacAlgorithm());
        lqw.eq(StringUtils.isNotBlank(bo.getMacKey()), SocialUser::getMacKey, bo.getMacKey());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), SocialUser::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getOauthToken()), SocialUser::getOauthToken, bo.getOauthToken());
        lqw.eq(StringUtils.isNotBlank(bo.getOauthTokenSecret()), SocialUser::getOauthTokenSecret, bo.getOauthTokenSecret());
        return lqw;
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
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改社会化关系
     */
    @Override
    public Boolean updateByBo(SocialUserBo bo) {
        SocialUser update = MapstructUtils.convert(bo, SocialUser.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SocialUser entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除社会化关系
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
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
