package org.dromara.workflow.de.nc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dromara.workflow.de.nc.domain.bo.DeNcNoticeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcNoticeQueryBo;
import org.dromara.workflow.de.nc.domain.vo.DeNcNoticeVo;

import java.util.List;

/**
 * NC-04 通知中心 CRUD 服务接口（2026-09-23）。
 *
 * <p>5 端点对应 5 方法：
 * <ul>
 *   <li>{@link #page} —— 分页查询（GET /de/nc/notice/list）</li>
 *   <li>{@link #list} —— 列表查询（GET /de/nc/notice/listAll）</li>
 *   <li>{@link #getInfo} —— 详情（GET /de/nc/notice/{noticeId}）</li>
 *   <li>{@link #add} —— 新增（POST /de/nc/notice）</li>
 *   <li>{@link #edit} —— 更新（PUT /de/nc/notice）</li>
 *   <li>{@link #remove} —— 批量删除（DELETE /de/nc/notice/{noticeIds}）</li>
 * </ul>
 *
 * <p>写入路径独立 —— 见 {@link DeNcNoticeServiceImpl} 类注。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeNcNoticeService {

    IPage<DeNcNoticeVo> page(DeNcNoticeQueryBo query, Long pageNum, Long pageSize);

    List<DeNcNoticeVo> list(DeNcNoticeQueryBo query);

    DeNcNoticeVo getInfo(Long noticeId);

    Long add(DeNcNoticeBo bo);

    int edit(DeNcNoticeBo bo);

    int remove(Long[] noticeIds);
}