package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试对象 chkj_test
 * 
 * @author Lion Li
 * @date 2021-05-14
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("chkj_test")
public class ChkjTest implements Serializable {

private static final long serialVersionUID=1L;


    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** key键 */
    private String testKey;

    /** 值 */
    private String value;

    /** 版本 */
    private Long version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 删除标志 */
    private Long deleted;

    /** 父id */
    private Long parentId;

    /** 排序号 */
    private Long orderNum;

}
