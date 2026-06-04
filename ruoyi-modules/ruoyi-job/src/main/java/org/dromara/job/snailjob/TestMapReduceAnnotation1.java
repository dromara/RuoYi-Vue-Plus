package org.dromara.job.snailjob;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aizuda.snailjob.client.job.core.MapHandler;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.annotation.MapExecutor;
import com.aizuda.snailjob.client.job.core.annotation.ReduceExecutor;
import com.aizuda.snailjob.client.job.core.dto.MapArgs;
import com.aizuda.snailjob.client.job.core.dto.ReduceArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.dromara.common.core.utils.StreamUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * MapReduce任务 动态分配 分片后合并结果
 * <a href="https://juejin.cn/post/7448551286506913802"></a>
 *
 * @author 老马
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Component
@JobExecutor(name = "testMapReduceAnnotation1")
public class TestMapReduceAnnotation1 {

    /**
     * 根 Map 任务，将示例数据拆分为多个分片任务。
     *
     * @param mapArgs    Map 任务参数
     * @param mapHandler 分片调度处理器
     * @return 分片调度结果
     */
    @MapExecutor
    public ExecuteResult rootMapExecute(MapArgs mapArgs, MapHandler mapHandler) {
        int partitionSize = 50;
        List<Integer> sourceList = IntStream.rangeClosed(1, 200).boxed().toList();
        List<List<Integer>> partition = StreamUtils.groupByKey(sourceList, i -> (i - 1) / partitionSize)
            .values()
            .stream()
            .toList();
        SnailJobLog.REMOTE.info("端口:{}完成分配任务", SpringUtil.getProperty("server.port"));
        return mapHandler.doMap(partition, "doCalc");
    }

    /**
     * 执行单个分片的累加计算。
     *
     * @param mapArgs 分片任务参数
     * @return 分片计算结果
     */
    @MapExecutor(taskName = "doCalc")
    public ExecuteResult doCalc(MapArgs mapArgs) {
        List<Integer> sourceList = (List<Integer>) mapArgs.getMapResult();
        // 遍历sourceList的每一个元素,计算出一个累加值partitionTotal
        int partitionTotal = sourceList.stream().mapToInt(i -> i).sum();
        // 打印日志到服务器
        ThreadUtil.sleep(3, TimeUnit.SECONDS);
        SnailJobLog.REMOTE.info("端口:{},partitionTotal:{}", SpringUtil.getProperty("server.port"), partitionTotal);
        return ExecuteResult.success(partitionTotal);
    }

    /**
     * 汇总所有 Map 分片的计算结果。
     *
     * @param reduceArgs Reduce 任务参数
     * @return 汇总结果
     */
    @ReduceExecutor
    public ExecuteResult reduceExecute(ReduceArgs reduceArgs) {
        int reduceTotal = reduceArgs.getMapResult().stream().mapToInt(i -> Integer.parseInt((String) i)).sum();
        SnailJobLog.REMOTE.info("端口:{},reduceTotal:{}", SpringUtil.getProperty("server.port"), reduceTotal);
        return ExecuteResult.success(reduceTotal);
    }
}
