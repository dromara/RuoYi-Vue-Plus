package com.wudgaby.stars.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wudgaby.stars.domain.StarsUserRepo;
import com.wudgaby.stars.domain.bo.StarsRepoQueryBo;
import com.wudgaby.stars.domain.vo.StarsRepoCardRow;
import com.wudgaby.stars.domain.vo.StarsUserRepoVo;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 用户-仓库关系 Mapper
 */
public interface StarsUserRepoMapper extends BaseMapperPlus<StarsUserRepo, StarsUserRepoVo> {

    /**
     * 按用户与仓库查询关系
     *
     * @param userId 用户 ID
     * @param repoId 仓库 ID
     * @return 用户-仓库关系，不存在时返回 null
     */
    default StarsUserRepo selectByUserAndRepo(Long userId, Long repoId) {
        return this.selectOne(new LambdaQueryWrapper<StarsUserRepo>()
            .eq(StarsUserRepo::getUserId, userId)
            .eq(StarsUserRepo::getRepoId, repoId));
    }

    /**
     * 分页查询用户仓库卡片
     */
    Page<StarsRepoCardRow> selectRepoCardPage(Page<StarsRepoCardRow> page, @Param("query") StarsRepoQueryBo query);

    /**
     * 查询用户仓库详情行
     */
    StarsRepoCardRow selectRepoDetail(@Param("userId") Long userId, @Param("userRepoId") Long userRepoId);

}
