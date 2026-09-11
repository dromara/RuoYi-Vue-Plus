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
package org.dromara.warm.flow.ui.service;

import org.dromara.warm.flow.dto.DefJson;
import org.dromara.warm.flow.dto.PromptContent;
import org.dromara.warm.flow.enums.NodeType;
import org.dromara.warm.flow.utils.MapUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程图提示信息
 *
 * @author warm
 */
public interface ChartExtService {


    /**
     * 设置流程图提示信息
     *
     * @param defJson 流程定义json对象
     */
    void execute(DefJson defJson);

    /**
     * 初始化流程图提示信息
     *
     * @param defJson 流程定义json对象
     */
    default void initPromptContent(DefJson defJson) {
        defJson.setTopText("流程名称: " + defJson.getFlowName());
        defJson.getNodeList().forEach(nodeJson -> {
            // 提示信息主对象
            PromptContent promptContent = new PromptContent();

            if (NodeType.isGateWay(nodeJson.getNodeType())) {
                return;
            }

            // 设置 dialogStyle 样式
            promptContent.setDialogStyle(MapUtil.mergeAll(
                "position", "absolute",
                "backgroundColor", "#fff",
                "border", "1px solid #ccc",
                "borderRadius", "4px",
                "boxShadow", "0 2px 8px rgba(0, 0, 0, 0.15)",
                "padding", "8px 12px",
                "fontSize", "14px",
                "zIndex", 1000,
                "maxWidth", "500px",
                "color", "#333"
            ));

            // 创建 info 列表
            List<PromptContent.InfoItem> infoList = new ArrayList<>();

            // 添加第一个条目: 任务名称
            PromptContent.InfoItem item = new PromptContent.InfoItem()
                .setPrefix("任务名称: ")
                .setContent(nodeJson.getNodeName())
                .setContentStyle(MapUtil.mergeAll("border", "1px solid #d1e9ff",
                    "backgroundColor", "#e8f4ff",
                    "padding", "4px 8px",
                    "borderRadius", "4px"
                ))
                .setRowStyle(MapUtil.mergeAll("fontWeight", "bold",
                    "margin", "0 0 6px 0",
                    "padding", "0 0 8px 0",
                    "borderBottom", "1px solid #ccc"
                ));
            infoList.add(item);
            promptContent.setInfo(infoList);

            nodeJson.setPromptContent(promptContent);
        });
    }
}
