package org.dromara.system.service;

import org.dromara.common.core.domain.dto.PushPayloadDTO;
import org.dromara.system.domain.vo.SysMessageBoxVo;

import java.util.List;

/**
 * 消息记录服务接口
 *
 * @author Lion Li
 */
public interface ISysMessageService {

    /**
     * 查询当前用户消息盒子数据
     *
     * @param userId 用户ID
     * @return 消息盒子数据
     */
    SysMessageBoxVo queryMessageBox(Long userId);

    /**
     * 发送指定用户文本消息
     *
     * @param userId 目标用户
     * @param message 文本消息
     */
    void sendMessage(Long userId, String message);

    /**
     * 广播文本消息
     *
     * @param message 文本消息
     */
    void sendMessage(String message);

    /**
     * 发送指定用户消息
     *
     * @param userId 目标用户
     * @param payload 推送消息体
     */
    void sendMessage(Long userId, PushPayloadDTO payload);

    /**
     * 广播消息
     *
     * @param payload 推送消息体
     */
    void sendMessage(PushPayloadDTO payload);

    /**
     * 发布指定用户消息
     *
     * @param userIds 用户ID列表
     * @param payload 推送消息体
     */
    void publishMessage(List<Long> userIds, PushPayloadDTO payload);

    /**
     * 发布广播文本消息
     *
     * @param message 文本消息
     */
    void publishAll(String message);

    /**
     * 发布广播消息
     *
     * @param payload 推送消息体
     */
    void publishAll(PushPayloadDTO payload);

    /**
     * 记录全局消息
     *
     * @param payload 推送消息体
     * @return 回填消息ID后的推送消息体
     */
    PushPayloadDTO storeAll(PushPayloadDTO payload);

    /**
     * 记录指定用户消息
     *
     * @param userIds 用户ID列表
     * @param payload 推送消息体
     * @return 回填消息ID后的推送消息体
     */
    PushPayloadDTO storeUsers(List<Long> userIds, PushPayloadDTO payload);
}
