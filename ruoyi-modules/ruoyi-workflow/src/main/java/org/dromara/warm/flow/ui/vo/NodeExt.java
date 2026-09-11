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

import java.io.Serializable;
import java.util.List;

/**
 * 节点扩展属性
 *
 * @author warm
 * @since 2025/2/18
 */
@Getter
@Setter
public class NodeExt implements Serializable {
    private static final long serialVersionUID = 1L;
    private String code;
    private String name;
    private String desc;
    private int type;
    private List<ChildNode> childs;

    @Getter
    @Setter
    public static class ChildNode {
        private String code;
        private String desc;
        private String label;
        private int type;
        private boolean must;
        private boolean multiple;
        private int precision;
        private String step;
        private String min;
        private String dateType;
        private String dateFormat;
        private List<DictItem> dict;

    }

    @Getter
    @Setter
    public static class DictItem {
        private String label;
        private String value;
        private boolean selected;

        public DictItem() {
        }

        public DictItem(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public DictItem(String label, String value, boolean selected) {
            this.label = label;
            this.value = value;
            this.selected = selected;
        }
    }

}
