package org.dromara.demo.domain.vo;

import org.dromara.demo.domain.TestLeave;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


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
     * 标题
     */
    @ExcelProperty(value = "标题")
    private String title;

    /**
     * 请假天数
     */
    @ExcelProperty(value = "请假天数")
    private Integer leaveDays;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 流程实例对象
     */
    private ProcessInstanceVo processInstanceVo;


}
