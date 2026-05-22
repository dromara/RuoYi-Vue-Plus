package com.wudgaby.stars.service;

import com.wudgaby.stars.domain.bo.CreateStarsTagBo;
import com.wudgaby.stars.domain.bo.UpdateStarsTagBo;
import com.wudgaby.stars.domain.vo.StarsTagVo;

import java.util.List;

/**
 * 用户标签服务
 */
public interface IStarsTagService {

    /**
     * 查询当前用户全部标签
     */
    List<StarsTagVo> listByUser(Long userId);

    /**
     * 创建标签
     */
    StarsTagVo create(Long userId, CreateStarsTagBo request);

    /**
     * 更新标签
     */
    StarsTagVo update(Long userId, UpdateStarsTagBo request);

    /**
     * 删除标签及其关联
     */
    void delete(Long userId, Long tagId);

}
