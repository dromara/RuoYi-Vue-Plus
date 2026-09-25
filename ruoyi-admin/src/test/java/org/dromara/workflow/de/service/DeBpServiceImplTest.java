package org.dromara.workflow.de.service;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * DeBpServiceImpl：E0—E9 全部 owner-pending，必须抛 ServiceException 而非伪成功 Map（零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("DeBpServiceImpl：E0—E9 owner-pending 显式异常")
class DeBpServiceImplTest {

    @Test
    @DisplayName("E0—E9 共 10 方法逐一抛 ServiceException")
    void allPhasesThrow() {
        DeBpServiceImpl service = new DeBpServiceImpl();
        int checked = 0;
        for (Method m : DeBpServiceImpl.class.getDeclaredMethods()) {
            if (m.getName().startsWith("e") && m.getParameterCount() == 1) {
                try {
                    m.invoke(service, new Object[]{null});
                    fail("应抛 ServiceException：" + m.getName());
                } catch (java.lang.reflect.InvocationTargetException e) {
                    assertTrue(e.getCause() instanceof ServiceException,
                        m.getName() + " 应为 ServiceException，实得 " + e.getCause());
                    checked++;
                } catch (ReflectiveOperationException e) {
                    fail(m.getName(), e);
                }
            }
        }
        assertTrue(checked == 10, "应覆盖 10 个环节，实覆盖 " + checked);
    }
}
