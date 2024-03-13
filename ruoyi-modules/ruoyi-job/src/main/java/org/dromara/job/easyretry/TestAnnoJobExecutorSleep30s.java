package org.dromara.job.easyretry;

import com.aizuda.easy.retry.client.job.core.annotation.JobExecutor;
import com.aizuda.easy.retry.client.job.core.dto.JobArgs;
import com.aizuda.easy.retry.client.model.ExecuteResult;
import com.aizuda.easy.retry.common.core.util.JsonUtil;
import org.springframework.stereotype.Component;

/**
 * @author www.byteblogs.com
 * @date 2023-09-28 22:54:07
 * @since 2.4.0
 */
@Component
@JobExecutor(name = "testAnnoJobExecutorSleep30s")
public class TestAnnoJobExecutorSleep30s {

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        System.out.println(JsonUtil.toJsonString(jobArgs));
        try {
            Thread.sleep(30 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ExecuteResult.success("测试成功");
    }
}
