package org.dromara.job.easyretry;

import com.aizuda.easy.retry.client.job.core.annotation.JobExecutor;
import com.aizuda.easy.retry.client.job.core.dto.JobArgs;
import com.aizuda.easy.retry.client.model.ExecuteResult;
import com.aizuda.easy.retry.common.core.util.JsonUtil;
import com.aizuda.easy.retry.common.log.EasyRetryLog;
import org.springframework.stereotype.Component;

/**
 * @author www.byteblogs.com
 * @date 2023-09-28 22:54:07
 */
@Component
@JobExecutor(name = "testJobExecutor")
public class TestAnnoJobExecutor {

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        EasyRetryLog.LOCAL.info("testJobExecutor. jobArgs:{}", JsonUtil.toJsonString(jobArgs));
        EasyRetryLog.REMOTE.info("testJobExecutor. jobArgs:{}", JsonUtil.toJsonString(jobArgs));
        return ExecuteResult.success("测试成功");
    }
}
