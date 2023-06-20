package org.dromara.system.service;

import org.dromara.system.domain.bo.SocialUserBo;
import org.dromara.system.domain.vo.SocialUserVo;

import java.util.List;

/**
 * 社会化关系Service接口
 *
 * @author thiszhc
 * @date 2023-06-12
 */
public interface ISocialUserService {


    /**
     * 查询社会化关系
     */
    SocialUserVo queryById(String id);

    /**
     * 查询社会化关系列表
     */
    List<SocialUserVo> queryList();

    /**
     * 新增授权关系
     */
    Boolean insertByBo(SocialUserBo bo);


    /**
     * 删除社会化关系信息
     */
    Boolean deleteWithValidById(Long id);


    /**
     * 根据authId查询SocialUser表和SysUser表，返回SocialUserAuthResult映射的对象
     * @param authId 认证ID
     * @return SocialUser
     */
    SocialUserVo selectSocialUserByAuthId(String authId);

}
