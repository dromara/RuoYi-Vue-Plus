package com.ruoyi.quartz.task;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度测试
 * 
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i)
    {
        Console.log(StrUtil.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    public void ryParams(String params)
    {
        Console.log("执行有参方法：" + params);
    }

    public void ryNoParams()
    {
        Console.log("执行无参方法");
    }
}
