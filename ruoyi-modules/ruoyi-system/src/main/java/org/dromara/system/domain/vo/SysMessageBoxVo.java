package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息盒子视图对象
 *
 * @author Lion Li
 */
@Data
public class SysMessageBoxVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 系统消息
     */
    private List<SysMessageVo> systemList = new ArrayList<>();

    /**
     * 通知公告消息
     */
    private List<SysMessageVo> noticeList = new ArrayList<>();

    /**
     * 工作流消息
     */
    private List<SysMessageVo> workflowList = new ArrayList<>();
}
