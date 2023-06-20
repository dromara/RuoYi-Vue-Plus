package org.dromara.system.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysSocial;
import org.dromara.system.domain.vo.SysSocialVo;

/**
 * 社会化关系Mapper接口
 *
 * @author thiszhc
 */
public interface SysSocialMapper extends BaseMapperPlus<SysSocial, SysSocialVo> {

    /**
     * 根据 authId 查询 SysSocial 表和 SysUser 表，返回 SysSocialAuthResult 映射的对象
     *
     * @param authId 认证ID
     * @return SysSocial
     */
    SysSocialVo selectByAuthId(String authId);

}
