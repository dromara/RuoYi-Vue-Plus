package org.dromara.common.oss.constant;

import org.dromara.common.core.constant.GlobalConstants;

import java.util.Arrays;
import java.util.List;

/**
 * 对象存储常量
 *
 * @author Lion Li
 */
public interface OssConstant {

    /**
     * 默认配置KEY
     */
    String DEFAULT_CONFIG_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "sys_oss:default_config";

    /**
     * 预览列表资源开关Key
     */
    String PEREVIEW_LIST_RESOURCE_KEY = "sys.oss.previewListResource";

    /**
     * 系统数据ids
     */
    List<Long> SYSTEM_DATA_IDS = Arrays.asList(
        1761900000000000001L,
        1761900000000000002L,
        1761900000000000003L,
        1761900000000000004L
    );

    /**
     * 云服务商
     */
    String[] CLOUD_SERVICE = new String[] {"aliyun", "qcloud", "qiniu", "obs"};

}
