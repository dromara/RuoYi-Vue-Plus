package org.dromara.job.easyretry;

import com.aizuda.easy.retry.client.job.core.dto.JobArgs;
import com.aizuda.easy.retry.client.job.core.executor.AbstractJobExecutor;
import com.aizuda.easy.retry.client.model.ExecuteResult;
import org.springframework.stereotype.Component;

/**
 * @author www.byteblogs.com
 * @date 2023-09-28 22:54:07
 * @since 2.4.0
 */
@Component
public class TestClassJobExecutor extends AbstractJobExecutor {

    @Override
    protected ExecuteResult doJobExecute(JobArgs jobArgs) {
        return ExecuteResult.success("TestJobExecutor测试成功");
    }
}
