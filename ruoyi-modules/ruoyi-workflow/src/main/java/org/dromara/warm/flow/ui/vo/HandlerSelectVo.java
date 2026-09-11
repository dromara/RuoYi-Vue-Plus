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
package org.dromara.warm.flow.ui.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.warm.flow.dto.FlowPage;
import org.dromara.warm.flow.dto.Tree;

import java.util.List;

/**
 * 流程设计器-办理人选择
 *
 * @author warm
 */
@Getter
@Setter
@Accessors(chain = true)
public class HandlerSelectVo {

    /**
     * 办理人选择，分页列表，有具体办理对象 比如：部门、角色和用户等情况
     */
    private FlowPage<HandlerAuth> handlerAuths;

    /**
     * 左侧树状选择，配合{@link #handlerAuths}使用，比如用户先选择部门，然后选择用户
     */
    private List<Tree> treeSelections;

}
