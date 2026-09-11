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
package org.dromara.warm.flow.ui.controller;

import org.dromara.common.core.domain.R;
import org.dromara.warm.flow.ui.service.WarmFlowService;
import org.dromara.warm.flow.ui.vo.WarmFlowVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设计器Controller 匿名访问
 *
 * @author warm
 */
@RestController
@RequestMapping("/warm-flow-ui")
public class WarmFlowUiController {

    /**
     * 返回流程定义的配置
     *
     * @return R<WarmFlowVo>
     */
    @GetMapping("/config")
    public R<WarmFlowVo> config() {
        return WarmFlowService.config();
    }

}
