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
import java.io.Serial;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 字典
 *
 * @author warm
 */
@Getter
@Setter
@NoArgsConstructor
public class Dict implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典标签
     */
    private String label;

    /**
     * 字典值
     */
    private String value;

    private List<Dict> childList;

    public Dict(String label, String value) {
        this.label = label;
        this.value = value;
    }

}
