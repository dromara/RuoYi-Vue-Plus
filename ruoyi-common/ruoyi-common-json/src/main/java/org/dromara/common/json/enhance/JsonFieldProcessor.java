package org.dromara.common.json.enhance;

/**
 * 响应字段处理器。
 *
 * <p>生命周期按顺序分为三个阶段，由 {@link JsonValueEnhancer} 统一驱动：
 * <ol>
 *   <li><b>collect</b>（收集阶段）：递归扫描响应对象树时，对每个字段调用一次。
 *       用于采集需要处理的字段 key，存入 {@link JsonEnhancementContext} 供下一阶段批量处理。</li>
 *   <li><b>prepare</b>（预处理阶段）：collect 全部完成后调用一次。
 *       用于执行批量 IO（如批量查询数据库），将结果写入 {@link JsonEnhancementContext}。
 *       此阶段应消除 N+1 查询问题。</li>
 *   <li><b>process</b>（处理阶段）：渲染响应 JSON 树时，对每个字段调用一次。
 *       从 {@link JsonEnhancementContext} 取出 prepare 阶段写入的结果，返回替换后的字段值。
 *       返回原 {@code value} 表示不修改；返回 {@code null} 表示将字段值置为 null。</li>
 * </ol>
 *
 * <p>实现类通过 {@link JsonEnhancementContext#setAttribute} / {@link JsonEnhancementContext#getAttribute}
 * 在三个阶段之间共享数据，建议以实现类全限定名作为 attribute key 前缀以避免冲突。
 */
public interface JsonFieldProcessor {

    /**
     * 收集阶段：扫描字段，将需要处理的 key 存入 context。
     * 每个字段调用一次，整个对象树扫描完成后才会进入 prepare 阶段。
     */
    default void collect(JsonFieldContext fieldContext, JsonEnhancementContext context) {
    }

    /**
     * 预处理阶段：基于 collect 阶段收集到的数据执行批量处理（如批量查询），结果写入 context。
     * 每次响应只调用一次。
     */
    default void prepare(JsonEnhancementContext context) {
    }

    /**
     * 处理阶段：根据 prepare 阶段写入的结果，对字段值进行替换并返回。
     * 返回原 {@code value} 表示不修改该字段；返回 {@code null} 表示将字段值置为 null。
     */
    default Object process(JsonFieldContext fieldContext, Object value, JsonEnhancementContext context) {
        return value;
    }

}
