package org.dromara.common.mybatis.utils;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;

/**
 * ID 生成工具类
 *
 * @author AprilWind
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdGeneratorUtil {

    private static final IdentifierGenerator GENERATOR = SpringUtils.getBean(IdentifierGenerator.class);

    /**
     * 生成字符串类型主键 ID
     * <p>
     * 调用 {@link IdentifierGenerator#nextId(Object)}，返回 String 格式 ID。
     * </p>
     *
     * @return 字符串格式主键 ID
     */
    public static String nextId() {
        return GENERATOR.nextId(null).toString();
    }

    /**
     * 生成 Long 类型主键 ID
     * <p>
     * 自动将生成的数字型主键转换为 Long 类型
     * </p>
     *
     * @return Long 类型主键 ID
     */
    public static Long nextLongId() {
        return GENERATOR.nextId(null).longValue();
    }

    /**
     * 生成 Number 类型主键 ID
     * <p>
     * 推荐在需要保留原始 Number 类型时使用
     * </p>
     *
     * @return Number 类型主键 ID
     */
    public static Number nextNumberId() {
        return GENERATOR.nextId(null);
    }

    /**
     * 根据实体生成数字型主键 ID
     * <p>
     * 若自定义的 {@link IdentifierGenerator} 根据实体内容生成 ID，则可以使用本方法
     * </p>
     *
     * @param entity 实体对象
     * @return Number 类型主键 ID
     */
    public static Number nextId(Object entity) {
        return GENERATOR.nextId(entity);
    }

    /**
     * 根据实体生成字符串主键 ID
     * <p>
     * 与 {@link #nextId(Object)} 类似，但返回 String 类型
     * </p>
     *
     * @param entity 实体对象
     * @return 字符串格式主键 ID
     */
    public static String nextStringId(Object entity) {
        return GENERATOR.nextId(entity).toString();
    }

    /**
     * 生成 32 位 UUID
     * <p>
     * 底层使用 {@link IdWorker#get32UUID()}
     * </p>
     *
     * @return 32 位 UUID 字符串
     */
    public static String nextUUID() {
        return IdWorker.get32UUID();
    }

    /**
     * 根据实体生成 32 位 UUID
     * <p>
     * 默认 {@link IdentifierGenerator#nextUUID(Object)} 实现忽略实体，但保留该方法便于扩展。
     * </p>
     *
     * @param entity 实体对象
     * @return 32 位 UUID 字符串
     */
    public static String nextUUID(Object entity) {
        return GENERATOR.nextUUID(entity);
    }

    /**
     * 生成带指定前缀的字符串主键 ID
     * <p>
     * 示例：prefix = "ORD"，生成结果形如：{@code ORD20251211000123}
     * </p>
     *
     * @param prefix 自定义前缀
     * @return 带前缀的字符串主键 ID
     */
    public static String nextIdWithPrefix(String prefix) {
        return prefix + nextId();
    }

    /**
     * 生成带前缀的 UUID
     *
     * @param prefix 前缀
     * @return prefix + UUID
     */
    public static String nextUUIDWithPrefix(String prefix) {
        return prefix + nextUUID();
    }

}
