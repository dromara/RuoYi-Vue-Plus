package org.dromara.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysMessage;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息记录视图对象 sys_message
 *
 * @author Lion Li
 */
@Data
@AutoMapper(target = SysMessage.class)
public class SysMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
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
     * 扩展数据
     */
    private Object data;

    /**
     * 前端跳转路径
     */
    private String path;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
