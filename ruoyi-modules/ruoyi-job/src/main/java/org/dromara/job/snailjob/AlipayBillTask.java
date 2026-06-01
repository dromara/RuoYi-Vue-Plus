package org.dromara.job.snailjob;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.job.entity.BillDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * DAG工作流任务-模拟支付宝账单任务
 * <a href="https://juejin.cn/post/7487860254114644019"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "alipayBillTask")
public class AlipayBillTask {

    /**
     * 执行支付宝账单模拟任务，并将结果写入工作流上下文。
     *
     * @param jobArgs 任务执行参数
     * @return 执行结果
     * @throws InterruptedException 任务被中断时抛出
     */
    public ExecuteResult jobExecute(JobArgs jobArgs) throws InterruptedException {
        // 设置清算日期
        String settlementDate = (String) jobArgs.getWfContext().get("settlementDate");
        if (StringUtils.equals(settlementDate, "sysdate")) {
            settlementDate = DateUtils.getDate();
        }
        BillDTO billDTO = new BillDTO(23456789L, "alipay", settlementDate, new BigDecimal("2345.67"));
        // 把billDTO对象放入上下文进行传递
        jobArgs.appendContext("alipay", JsonUtils.toJsonString(billDTO));
        SnailJobLog.REMOTE.info("上下文: {}", jobArgs.getWfContext());
        return ExecuteResult.success(billDTO);
    }

}
