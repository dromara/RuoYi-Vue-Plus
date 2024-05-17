package org.dromara.job.snailjob;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.core.util.JsonUtil;
import com.aizuda.snailjob.common.log.SnailJobLog;
import org.springframework.stereotype.Component;

/**
 * @author opensnail
 * @date 2024-05-17
 */
@Component
@JobExecutor(name = "testJobExecutor")
public class TestAnnoJobExecutor {

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        SnailJobLog.LOCAL.info("testJobExecutor. jobArgs:{}", JsonUtil.toJsonString(jobArgs));
        SnailJobLog.REMOTE.info("testJobExecutor. jobArgs:{}", JsonUtil.toJsonString(jobArgs));
        return ExecuteResult.success("测试成功");
    }
}
