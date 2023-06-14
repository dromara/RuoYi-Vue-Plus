package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 节点图形信息
 *
 * @author may
 */
@Data
public class GraphicInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * x坐标
     */
    private double x;

    /**
     * y坐标
     */
    private double y;

    /**
     * 节点高度
     */
    private double height;

    /**
     * 节点宽度
     */
    private double width;

    /**
     * 节点id
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;
}
