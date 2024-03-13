package org.dromara.job.easyretry;

import com.aizuda.easy.retry.client.job.core.annotation.JobExecutor;
import com.aizuda.easy.retry.client.job.core.dto.JobArgs;
import com.aizuda.easy.retry.client.model.ExecuteResult;
import com.aizuda.easy.retry.common.core.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author www.byteblogs.com
 * @date 2023-09-28 22:54:07
 * @since 2.4.0
 */
@Component
@Slf4j
@JobExecutor(name = "testAnnoJobExecutorSleep5s")
public class TestAnnoJobExecutorSleep5s {

    public ExecuteResult jobExecute(JobArgs jobArgs) {
       log.info("testAnnoJobExecutorSleep5s. jobArgs:{}", JsonUtil.toJsonString(jobArgs));

       try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ExecuteResult.success("测试成功");
    }
}
