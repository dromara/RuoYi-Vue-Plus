package org.dromara.datasource.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

/**
 * 字段查询请求
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldQueryBo {

    @NotEmpty(message = "字段不为空")
    private Collection<String> columns;

    private List<FieldCondition> fieldConditions;

    private List<FieldOrder> fieldOrders;

    private List<Integer> offsets;

    private Integer limit;

    private List<String> groupColumns;

    @Data
    public static class FieldCondition {
        @NotEmpty(message = "字段名不能为空")
        private String field;

        private String type;

        @NotEmpty(message = "运算符不能为空")
        private String operator;

        private String linkOperator;

        @NotNull(message = "查询数据不能为空")
        private Object value;

        private Object secondValue;
    }

    @Data
    public static class FieldOrder {
        /**
         * 排序的字段
         */
        @NotEmpty(message = "字段名不能为空")
        private String field;

        /**
         * 排序方式（正序还是反序）
         */
        private String direction;
    }
}
