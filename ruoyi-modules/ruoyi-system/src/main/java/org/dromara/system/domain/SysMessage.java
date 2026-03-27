package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 消息记录表 sys_message
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private Long messageId;

    /**
     * 消息分组
     */
    private String category;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要消息
     */
    private String message;

    /**
     * 详细内容
     */
    private String content;

    /**
     * 扩展数据 JSON
     */
    private String dataJson;

    /**
     * 前端跳转路径
     */
    private String path;

    /**
     * 目标用户ID串，0 表示全局
     */
    private String sendUserIds;
}
