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

import org.dromara.warm.flow.ui.vo.NodeExt;

import java.util.List;

/**
 * 流程设计器-节点扩展属性
 *
 * @author warm
 */
public interface NodeExtService {

    /**
     * 获取节点扩展属性
     *
     * @return 结果
     */
    List<NodeExt> getNodeExt();
}
