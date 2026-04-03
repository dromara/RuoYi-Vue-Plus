package org.dromara.workflow.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.workflow.domain.TestLeave;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 请假视图对象 test_leave
 *
 * @author may
 * @date 2023-07-21
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = TestLeave.class)
public class TestLeaveVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 申请编号
     */
    @ExcelProperty(value = "申请编号")
    private String applyCode;

    /**
     * 请假类型
     */
    @ExcelProperty(value = "请假类型")
    private String leaveType;

    /**
     * 开始时间
     */
    @ExcelProperty(value = "开始时间")
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    @ExcelProperty(value = "结束时间")
    private LocalDateTime endDate;

    /**
     * 请假天数
     */
    @ExcelProperty(value = "请假天数")
    private Integer leaveDays;

    /**
     * 备注
     */
    @ExcelProperty(value = "请假原因")
    private String remark;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

}
