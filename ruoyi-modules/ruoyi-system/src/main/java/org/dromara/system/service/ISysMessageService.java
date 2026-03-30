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
     * 按系统消息、通知公告、工作流消息分类返回
     *
     * @param userId 用户ID
     * @return 消息盒子数据
     */
    SysMessageBoxVo queryMessageBox(Long userId);

    /**
     * 发送指定用户文本消息
     *
     * @param userId  目标用户ID
     * @param message 文本消息内容
     */
    void sendMessage(Long userId, String message);

    /**
     * 全局广播文本消息
     *
     * @param message 文本消息内容
     */
    void sendMessage(String message);

    /**
     * 发送指定用户自定义消息体
     *
     * @param userId  目标用户ID
     * @param payload 消息推送体
     */
    void sendMessage(Long userId, PushPayloadDTO payload);

    /**
     * 全局广播自定义消息体
     *
     * @param payload 消息推送体
     */
    void sendMessage(PushPayloadDTO payload);

    /**
     * 批量发布消息给指定用户列表
     *
     * @param userIds 用户ID集合
     * @param payload 消息推送体
     */
    void publishMessage(List<Long> userIds, PushPayloadDTO payload);

    /**
     * 发布全局广播文本消息
     *
     * @param message 文本消息内容
     */
    void publishAll(String message);

    /**
     * 发布全局广播自定义消息体
     *
     * @param payload 消息推送体
     */
    void publishAll(PushPayloadDTO payload);

    /**
     * 存储全局广播消息到数据库
     *
     * @param payload 消息推送体
     * @return 回填消息ID后的消息体
     */
    PushPayloadDTO storeAll(PushPayloadDTO payload);

    /**
     * 存储指定用户消息到数据库
     *
     * @param userIds 用户ID集合
     * @param payload 消息推送体
     * @return 回填消息ID后的消息体
     */
    PushPayloadDTO storeUsers(List<Long> userIds, PushPayloadDTO payload);

}
