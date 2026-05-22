package com.wudgaby.stars.service;

import com.wudgaby.stars.domain.vo.StarsImportJobVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * Stars 导入任务服务
 */
public interface IStarsImportService {

    /**
     * 同步当前用户 GitHub Stars（异步执行）
     *
     * @param userId RuoYi 用户 ID
     * @return 导入任务 ID
     */
    Long startSelfSync(Long userId, Integer limit);

    /**
     * 导入指定 GitHub 用户的公开 Stars（异步执行）
     *
     * @param userId RuoYi 用户 ID
     * @param login  GitHub 用户名
     * @param limit  导入条数，null 时使用配置默认值
     * @return 导入任务 ID
     */
    Long startImportUser(Long userId, String login, Integer limit);

    /**
     * 分页查询当前用户的导入任务
     */
    TableDataInfo<StarsImportJobVo> queryPageList(Long userId, PageQuery pageQuery);

    /**
     * 查询导入任务详情（仅本人任务）
     */
    StarsImportJobVo queryById(Long userId, Long jobId);
}
