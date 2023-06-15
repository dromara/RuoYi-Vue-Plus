package org.dromara.system.service;

import org.dromara.system.domain.SocialUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SocialUserVo;
import org.dromara.system.domain.bo.SocialUserBo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.domain.vo.SysUserVo;

import java.util.Collection;
import java.util.List;

/**
 * 社会化关系Service接口
 *
 * @author thiszhc
 * @date 2023-06-12
 */
public interface ISocialUserService {

    /**
     * 新增授权关系
     */
    Boolean insertByBo(SocialUserBo bo);


    /**
     * 删除社会化关系信息
     */
    Boolean deleteWithValidById(Long id);


    /**
     * 根据用户ID查询授权关系
     */
    Boolean isExistByUserIdAndSource(Long userId, String source);


    /**
     * 根据authId查询SocialUser表和SysUser表，返回SocialUserAuthResult映射的对象
     * @param authId 认证ID
     * @return SocialUser
     */
    SocialUserVo selectSocialUserByAuthId(String authId);

}
