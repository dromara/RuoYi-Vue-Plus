/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.config;
import java.io.Serial;

import lombok.Getter;
import lombok.Setter;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.enums.ChartStatus;
import org.dromara.warm.flow.enums.FrameworkType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.List;
/**
 * WarmFlow属性配置文件（warm-flow 前缀绑定）
 *
 * @author warm
 */
@Getter
@Setter
@ConfigurationProperties("warm-flow")
public class WarmFlowProperties implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 开关
     */
    private boolean enabled = true;

    /**
     * 框架类型: springboot、solon
     */
    private FrameworkType framework;

    /**
     * 数据填充处理类路径
     */
    private String dataFillHandlerPath;

    /**
     * 办理人权限处理器类路径
     */
    private String permissionHandlerPath;

    /**
     * 全局监听器类路径
     */
    private String globalListenerPath;

    /**
     * 数据源类型, mybatis模块对orm进一步的封装, 由于各数据库分页语句存在差异,
     * 当配置此参数时, 以此参数结果为基准, 未配置时, 取DataSource中数据源类型,
     * 兜底为mysql数据库
     */
    private String dataSourceType;

    /**
     * ui开关
     */
    private boolean ui = true;

    /**
     * 如果需要工作流共享业务系统权限，默认Authorization，如果有多个token，用逗号分隔
     */
    private String tokenName = "Authorization";

    /**
     * 公共模型流程状态对应的三原色
     */
    private List<String> chartStatusColor;

    /**
     * 经典模式流程状态对应的三原色
     */
    private List<String> chartStatusColorClassics;

    /**
     * 仿钉钉模式流程状态对应的三原色
     */
    private List<String> chartStatusColorMimic;

    /**
     * 是否显示流程图顶部文字
     */
    private boolean topTextShow = true;

    public void init() {

        // 设置数据填充处理类
        FlowEngine.initDataFillHandler(this.getDataFillHandlerPath());

        // 设置办理人权限处理类
        FlowEngine.initPermissionHandler(this.getPermissionHandlerPath());

        // 设置全局监听器
        FlowEngine.initGlobalListener(this.getGlobalListenerPath());

        // 初始化流程状态对应的自定义三原色
        ChartStatus.initCustomColor(this.getChartStatusColor(), this.getChartStatusColorClassics(), this.getChartStatusColorMimic());
    }

}
