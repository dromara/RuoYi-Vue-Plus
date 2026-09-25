package org.dromara.workflow.de.service;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * DeP1GapCoverServiceImpl：档 B 10 + 档 D 23 共 33 方法全部 owner-pending，
 * 必须抛 ServiceException 而非伪 RESOLVED Map（零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("DeP1GapCoverServiceImpl：33 方法 owner-pending 显式异常")
class DeP1GapCoverServiceImplTest {

    @Test
    @DisplayName("33 方法逐一抛 ServiceException")
    void allMethodsThrow() {
        DeP1GapCoverServiceImpl service = new DeP1GapCoverServiceImpl();
        int checked = 0;
        for (Method m : DeP1GapCoverServiceImpl.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1
                && (m.getName().startsWith("b") || m.getName().startsWith("d"))) {
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
        assertTrue(checked == 33, "应覆盖 33 个方法，实覆盖 " + checked);
    }
}
