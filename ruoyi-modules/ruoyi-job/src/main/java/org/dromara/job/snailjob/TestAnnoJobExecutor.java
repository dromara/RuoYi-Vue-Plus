package org.dromara.job.snailjob;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.core.util.JsonUtil;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.springframework.stereotype.Component;

/**
 * 正常任务
 * <a href="https://juejin.cn/post/7418074037392293914"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "testJobExecutor")
public class TestAnnoJobExecutor {

    /**
     * 执行示例注解任务。
     *
     * @param jobArgs 任务参数
     * @return 执行结果
     */
    public ExecuteResult jobExecute(JobArgs jobArgs) {
        SnailJobLog.LOCAL.info("testJobExecutor. jobArgs:{}", JsonUtil.toJsonString(jobArgs));
        SnailJobLog.REMOTE.info("testJobExecutor. jobArgs:{}", JsonUtil.toJsonString(jobArgs));
        return ExecuteResult.success("测试成功");
    }
}
