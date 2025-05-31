package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsRoomLock;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;

/**
 * 房间锁定管理视图对象 pms_room_locks
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsRoomLock.class)
public class PmsRoomLockVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 锁定记录唯一ID
     */
    @ExcelProperty(value = "锁定记录唯一ID")
    private Long lockId;

    /**
     * 部门ID (门店)
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty(value = "门店名称")
    private String deptName;

    /**
     * 房间ID
     */
    @ExcelProperty(value = "房间ID")
    private Long roomId;

    /**
     * 房间号
     */
    @ExcelProperty(value = "房间号")
    private String roomNumber;

    /**
     * 锁定类型
     */
    @ExcelProperty(value = "锁定类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_lock_type")
    private String lockType;

    /**
     * 锁定开始时间
     */
    @ExcelProperty(value = "锁定开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockStartTime;

    /**
     * 锁定结束时间
     */
    @ExcelProperty(value = "锁定结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockEndTime;

    /**
     * 锁定原因
     */
    @ExcelProperty(value = "锁定原因")
    private String lockReason;

    /**
     * 锁定状态
     */
    @ExcelProperty(value = "锁定状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_lock_status")
    private String lockStatus;

    /**
     * 解锁时间
     */
    @ExcelProperty(value = "解锁时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date unlockTime;

    /**
     * 解锁操作人
     */
    @ExcelProperty(value = "解锁操作人")
    private Long unlockBy;

    /**
     * 解锁操作人姓名
     */
    @ExcelProperty(value = "解锁操作人姓名")
    private String unlockByName;

    /**
     * 解锁原因
     */
    @ExcelProperty(value = "解锁原因")
    private String unlockReason;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
