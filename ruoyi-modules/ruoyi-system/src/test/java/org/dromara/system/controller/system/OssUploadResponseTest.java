package org.dromara.system.controller.system;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssUploadResponseTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsStoredFileAndPassesOptionalExtension(boolean withExtension) {
        var service = mock(ISysOssService.class);
        var file = new MockMultipartFile("file", new byte[]{1});
        var saved = new SysOssVo();
        saved.setUrl("/files/key");
        saved.setOriginalName("report.pdf");
        saved.setOssId(9007199254740993L);
        when(service.upload(eq(file), nullable(SysOssExt.class))).thenReturn(saved);
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            String json = withExtension ? "{\"bizType\":\"report\",\"refId\":\"21\"}" : null;
            var response = new SysOssController(service).upload(file, json).getData();
            assertEquals(new SysOssController.SysOssUploadVo("/files/key", "report.pdf", "9007199254740993"), response);
            var extension = ArgumentCaptor.forClass(SysOssExt.class);
            verify(service).upload(eq(file), extension.capture());
            if (withExtension) {
                assertEquals("report", extension.getValue().getBizType());
                assertEquals("21", extension.getValue().getRefId());
            } else {
                assertNull(extension.getValue());
            }
        }
    }
}
