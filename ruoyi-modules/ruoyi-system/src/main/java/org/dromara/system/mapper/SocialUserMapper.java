package org.dromara.system.mapper;

import org.dromara.system.domain.SocialUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SocialUserVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 社会化关系Mapper接口
 *
 * @author thiszhc
 * @date 2023-06-12
 */
public interface SocialUserMapper extends BaseMapperPlus<SocialUser, SocialUserVo> {

    /**
     * 根据authId查询SocialUser表和SysUser表，返回SocialUserAuthResult映射的对象
     * @param authId 认证ID
     * @return SocialUser
     */
    SocialUserVo selectSocialUserByAuthId(String authId);

}
