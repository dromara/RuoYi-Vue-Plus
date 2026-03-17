package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysOss;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * OSS对象存储分页查询对象 sys_oss
 *
 * @author Lion Li
 */
@Data
@AutoMapper(target = SysOss.class, reverseConvertGenerate = false)
public class SysOssBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ossId
     */
    private Long ossId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原名
     */
    private String originalName;

    /**
     * 文件后缀名
     */
    private String fileSuffix;

    /**
     * URL地址
     */
    private String url;

    /**
     * 扩展字段
     */
    private String ext1;

    /**
     * 服务商
     */
    private String service;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

}
