package com.wudgaby.stars.service;

import com.wudgaby.stars.domain.bo.BatchTagsRequest;
import com.wudgaby.stars.domain.bo.StarsRepoQueryBo;
import com.wudgaby.stars.domain.bo.UpdateStarsRepoBo;
import com.wudgaby.stars.domain.vo.StarsRepoCardVo;
import com.wudgaby.stars.domain.vo.StarsRepoDetailVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 用户仓库查询与编辑服务
 */
public interface IStarsRepoService {

    /**
     * 分页查询用户仓库列表
     */
    TableDataInfo<StarsRepoCardVo> queryPageList(Long userId, StarsRepoQueryBo query, PageQuery pageQuery);

    /**
     * 查询仓库详情
     */
    StarsRepoDetailVo queryById(Long userId, Long userRepoId);

    /**
     * 更新备注、概述、分类与标签
     */
    void update(Long userId, Long userRepoId, UpdateStarsRepoBo request);

    /**
     * 批量为仓库追加标签
     */
    void batchAssignTags(Long userId, BatchTagsRequest request);

}
