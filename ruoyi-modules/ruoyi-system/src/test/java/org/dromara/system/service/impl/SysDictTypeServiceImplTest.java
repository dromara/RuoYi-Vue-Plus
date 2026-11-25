package org.dromara.system.service.impl;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.domain.vo.SysDictDataVo;
import org.dromara.system.mapper.SysDictDataMapper;
import org.dromara.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SysDictTypeServiceImplTest {

    @ParameterizedTest(name = "按字面分隔符 {0} 双向转换，保留输入顺序")
    @ValueSource(strings = {"|", ".", "[+]", ","})
    void shouldTranslateMultipleEntriesUsingLiteralSeparator(String separator) {
        var service = new SysDictTypeServiceImpl(mock(SysDictTypeMapper.class), mock(SysDictDataMapper.class));
        var cachedService = mock(SysDictTypeServiceImpl.class);
        var pending = new SysDictDataVo();
        pending.setDictValue("pending");
        pending.setDictLabel("待处理");
        var done = new SysDictDataVo();
        done.setDictValue("done");
        done.setDictLabel("已完成");
        when(cachedService.selectDictDataByType("status")).thenReturn(List.of(pending, done));

        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(cachedService);
            String values = "done" + separator + "pending";
            String labels = "已完成" + separator + "待处理";
            assertAll(
                () -> assertEquals(labels, service.getDictLabel("status", values, separator)),
                () -> assertEquals(values, service.getDictValue("status", labels, separator))
            );
        }
    }
}
