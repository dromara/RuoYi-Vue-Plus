package org.dromara.workflow.de.sy.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.DeSysConfig;
import org.dromara.workflow.de.mapper.DeSysConfigMapper;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyConfigVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SY-02 系统配置服务（{@link DeSyConfigServiceImpl}）单元测试（JUnit 5，不启 Spring）。
 *
 * <p>由 surefire 自动发现执行（前提：{@code -Dprofiles.active=dev -Dmaven.test.skip=false}）。
 *
 * <h3>本类钉死的真实不变量（非凑覆盖率）</h3>
 * <ul>
 *   <li><b>★ edit 的 null-aware 部分更新语义（H3 修复 · D-022）</b>：PUT 只带部分字段时，
 *       未提供（null）的 {@code configName/configValue/configType/remark} 必须<b>保留库中原值</b>，
 *       不得被 null 覆盖。这是最易回归的一处，故列为本类的核心用例。</li>
 *   <li>add / edit 的键名唯一性应用层判定（{@code selectOne} 命中即拒）。</li>
 *   <li>各方法必填项缺失一律 fail-closed 抛 {@link ServiceException}，不静默降级。</li>
 * </ul>
 *
 * <p>DB 读写用 {@link Proxy} 构造捕获型 stub mapper（记录 insert/update/delete 入参），
 * 与 {@code DeCxServiceImplTest} / {@code BrComplianceServiceTest} 同一手法。
 *
 * @author AIP
 * @since 2026-09-24
 */
@Tag("dev")
@DisplayName("DeSyConfigServiceImpl SY-02 配置 CRUD：唯一性 / null-aware 部分更新 / fail-closed")
public class DeSyConfigServiceImplTest {

    private static final Long CONFIG_ID = 100L;
    private static final Long GENERATED_ID = 500L;

    // ========================================================================
    // ===== getInfo =========================================================
    // ========================================================================

    @Test
    @DisplayName("getInfo：configId 必填，null ⇒ 抛 ServiceException（不返回 null 蒙混）")
    void getInfo_nullIdRejected() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.getInfo(null));
    }

    @Test
    @DisplayName("getInfo：库中不存在 ⇒ null（真查无行，非吞异常）")
    void getInfo_notFoundReturnsNull() {
        Harness h = new Harness();
        assertNull(h.service.getInfo(CONFIG_ID));
    }

    @Test
    @DisplayName("getInfo：命中 ⇒ 字段映射到 VO")
    void getInfo_hitMapsToVo() {
        Harness h = new Harness();
        h.byId = row(CONFIG_ID, "旧名", "k", "旧值", "Y", "旧备注");
        DeSyConfigVo vo = h.service.getInfo(CONFIG_ID);
        assertEquals(CONFIG_ID, vo.getConfigId());
        assertEquals("k", vo.getConfigKey());
        assertEquals("旧名", vo.getConfigName());
    }

    // ========================================================================
    // ===== getConfigByKey ==================================================
    // ========================================================================

    @Test
    @DisplayName("getConfigByKey：null / 空白键 ⇒ 抛 ServiceException")
    void getConfigByKey_blankRejected() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.getConfigByKey(null));
        assertThrows(ServiceException.class, () -> h.service.getConfigByKey("   "));
    }

    @Test
    @DisplayName("getConfigByKey：命中返回键值；未命中返回 null")
    void getConfigByKey_hitAndMiss() {
        Harness h = new Harness();
        h.selectOneResult = row(CONFIG_ID, "n", "k", "V", "N", null);
        assertEquals("V", h.service.getConfigByKey("k"));

        h.selectOneResult = null;
        assertNull(h.service.getConfigByKey("missing"));
    }

    // ========================================================================
    // ===== add =============================================================
    // ========================================================================

    @Test
    @DisplayName("add：configKey 空白 ⇒ 抛 ServiceException（必填）")
    void add_blankKeyRejected() {
        Harness h = new Harness();
        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigKey("  ");
        assertThrows(ServiceException.class, () -> h.service.add(bo));
    }

    @Test
    @DisplayName("add：★键名已存在 ⇒ 抛 ServiceException（唯一性应用层判定，不落库）")
    void add_duplicateKeyRejected() {
        Harness h = new Harness();
        h.selectOneResult = row(1L, "x", "dup-key", "v", "N", null);
        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigKey("dup-key");
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.add(bo));
        assertTrue(ex.getMessage().contains("已存在"), "应命中唯一性拒绝：" + ex.getMessage());
        assertEquals(0, h.inserted.size(), "唯一性拒绝时不得发生 INSERT");
    }

    @Test
    @DisplayName("add：正常 ⇒ configType 缺省补 N、时间戳回填、返回生成主键")
    void add_successDefaultsAndPersists() {
        Harness h = new Harness();
        h.selectOneResult = null; // 无重复
        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigKey("new-key");
        bo.setConfigName("新配置");
        bo.setConfigValue("vv");
        // 不设置 configType ⇒ 期望默认 "N"

        Long id = h.service.add(bo);

        assertEquals(GENERATED_ID, id, "应回传 insert 后生成的主键");
        assertEquals(1, h.inserted.size());
        DeSysConfig saved = h.inserted.get(0);
        assertEquals("N", saved.getConfigType(), "configType 缺省应补 N（系统内置：否）");
        assertEquals("new-key", saved.getConfigKey());
        assertEquals("vv", saved.getConfigValue());
        assertEquals(GENERATED_ID, saved.getConfigId(), "stub 模拟主键生成后应回填到实体");
        assertTrue(saved.getCreateTime() != null && saved.getUpdateTime() != null, "时间戳应回填");
    }

    @Test
    @DisplayName("add：显式 configType 不被缺省逻辑覆盖")
    void add_explicitConfigTypePreserved() {
        Harness h = new Harness();
        h.selectOneResult = null;
        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigKey("k2");
        bo.setConfigType("Y");
        h.service.add(bo);
        assertEquals("Y", h.inserted.get(0).getConfigType());
    }

    // ========================================================================
    // ===== edit：★核心不变量 ===============================================
    // ========================================================================

    @Test
    @DisplayName("edit：configId 必填，null ⇒ 抛 ServiceException")
    void edit_nullIdRejected() {
        Harness h = new Harness();
        DeSyConfigBo bo = new DeSyConfigBo();
        assertThrows(ServiceException.class, () -> h.service.edit(bo));
    }

    @Test
    @DisplayName("edit：目标不存在 ⇒ 抛 ServiceException（参数不存在）")
    void edit_notFoundRejected() {
        Harness h = new Harness();
        h.byId = null;
        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigId(CONFIG_ID);
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.edit(bo));
        assertTrue(ex.getMessage().contains("不存在"), ex.getMessage());
    }

    @Test
    @DisplayName("★edit：部分更新（H3/D-022）—— 只带 configValue，其余 null 字段必须保留原值")
    void edit_partialUpdatePreservesNullFields() {
        Harness h = new Harness();
        h.byId = row(CONFIG_ID, "旧名", "k", "旧值", "Y", "旧备注");

        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigId(CONFIG_ID);
        bo.setConfigValue("新值"); // 唯一提供的业务字段
        // configName / configKey / configType / remark 均留 null ⇒ PUT 部分更新语义

        int rows = h.service.edit(bo);
        assertEquals(1, rows);

        DeSysConfig updated = h.updated.get(0);
        assertEquals("新值", updated.getConfigValue(), "提供的字段应更新");
        assertEquals("旧名", updated.getConfigName(), "★null 字段不得覆盖原值（H3 回归点）");
        assertEquals("Y", updated.getConfigType(), "★null 字段不得覆盖原值");
        assertEquals("旧备注", updated.getRemark(), "★remark 亦须保留（D-022 同模式补齐）");
        assertEquals("k", updated.getConfigKey(), "未尝试改名时键名保持不变");
    }

    @Test
    @DisplayName("edit：改名撞已存在键 ⇒ 抛 ServiceException（唯一性，排除自身）")
    void edit_renameToExistingKeyRejected() {
        Harness h = new Harness();
        h.byId = row(CONFIG_ID, "n", "old-key", "v", "N", null);
        h.selectOneResult = row(999L, "other", "taken", "v", "N", null); // 别的行占了目标键

        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigId(CONFIG_ID);
        bo.setConfigKey("taken"); // 改成已被占用的键
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.edit(bo));
        assertTrue(ex.getMessage().contains("已存在"), ex.getMessage());
        assertEquals(0, h.updated.size(), "唯一性拒绝时不得发生 UPDATE");
    }

    @Test
    @DisplayName("edit：改名到未被占用的新键 ⇒ 成功落库")
    void edit_renameToFreshKeySucceeds() {
        Harness h = new Harness();
        h.byId = row(CONFIG_ID, "n", "old-key", "v", "N", null);
        h.selectOneResult = null; // 无重复

        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigId(CONFIG_ID);
        bo.setConfigKey("fresh-key");
        h.service.edit(bo);
        assertEquals("fresh-key", h.updated.get(0).getConfigKey());
    }

    @Test
    @DisplayName("edit：键名未变（与原值相等）⇒ 跳过唯一性查，不误报重复")
    void edit_sameKeySkipsDuplicateCheck() {
        Harness h = new Harness();
        h.byId = row(CONFIG_ID, "n", "same-key", "v", "N", null);
        h.selectOneResult = row(1L, "x", "same-key", "v", "N", null); // 若误查会命中“重复”

        DeSyConfigBo bo = new DeSyConfigBo();
        bo.setConfigId(CONFIG_ID);
        bo.setConfigKey("same-key"); // 与原键一致
        bo.setConfigValue("v2");
        int rows = h.service.edit(bo);
        assertEquals(1, rows, "键名未变不应触发唯一性拒绝");
        assertEquals("v2", h.updated.get(0).getConfigValue());
    }

    // ========================================================================
    // ===== remove / refreshCache ==========================================
    // ========================================================================

    @Test
    @DisplayName("remove：null / 空数组 ⇒ 抛 ServiceException（拒绝空调用）")
    void remove_emptyRejected() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.remove(null));
        assertThrows(ServiceException.class, () -> h.service.remove(new Long[0]));
    }

    @Test
    @DisplayName("remove：非空 id ⇒ 走 deleteByIds 并回传影响行数")
    void remove_success() {
        Harness h = new Harness();
        int rows = h.service.remove(new Long[]{1L, 2L, 3L});
        assertEquals(3, rows);
        assertEquals(1, h.deletedBatches.size());
        assertEquals(3, h.deletedBatches.get(0).size());
    }

    @Test
    @DisplayName("refreshCache：镜像层恒返回成功标记 1")
    void refreshCache_returnsMarker() {
        Harness h = new Harness();
        assertEquals(1, h.service.refreshCache());
    }

    // ========================================================================
    // ===== 夹具：捕获型 stub mapper（Proxy）================================
    // ========================================================================

    private static DeSysConfig row(Long id, String name, String key, String value, String type, String remark) {
        DeSysConfig r = new DeSysConfig();
        r.setConfigId(id);
        r.setConfigName(name);
        r.setConfigKey(key);
        r.setConfigValue(value);
        r.setConfigType(type);
        r.setRemark(remark);
        return r;
    }

    /** 记录一次测试用到的 mapper 状态与被捕获的写操作。 */
    private static final class Harness {
        final DeSyConfigServiceImpl service;
        final List<DeSysConfig> inserted = new ArrayList<>();
        final List<DeSysConfig> updated = new ArrayList<>();
        final List<Collection<?>> deletedBatches = new ArrayList<>();

        DeSysConfig byId;              // selectById 的固定返回（可编程）
        DeSysConfig selectOneResult;   // selectOne 的固定返回（唯一性检查用）

        Harness() {
            this.service = new DeSyConfigServiceImpl(stub());
        }

        @SuppressWarnings("unchecked")
        private DeSysConfigMapper stub() {
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                switch (name) {
                    case "selectById":
                        return byId;
                    case "selectOne":
                        return selectOneResult;
                    case "insert": {
                        DeSysConfig e = (DeSysConfig) args[0];
                        e.setConfigId(GENERATED_ID); // 模拟主键生成
                        inserted.add(e);
                        return 1;
                    }
                    case "updateById": {
                        DeSysConfig e = (DeSysConfig) args[0];
                        updated.add(e);
                        return 1;
                    }
                    case "deleteByIds": {
                        deletedBatches.add((Collection<?>) args[0]);
                        return ((Collection<?>) args[0]).size();
                    }
                    default:
                        return defaultValue(method.getReturnType());
                }
            };
            ClassLoader cl = DeSysConfigMapper.class.getClassLoader();
            return (DeSysConfigMapper) Proxy.newProxyInstance(cl, new Class<?>[]{DeSysConfigMapper.class}, handler);
        }

        private static Object defaultValue(Class<?> rt) {
            if (rt == boolean.class) {
                return false;
            }
            if (rt == int.class) {
                return 0;
            }
            if (rt == long.class) {
                return 0L;
            }
            if (List.class.isAssignableFrom(rt) || Collection.class.isAssignableFrom(rt)) {
                return new ArrayList<>();
            }
            return null;
        }
    }
}
