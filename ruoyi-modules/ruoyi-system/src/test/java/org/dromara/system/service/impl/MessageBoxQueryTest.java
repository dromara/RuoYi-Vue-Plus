package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.common.mybatis.enums.DataBaseType;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.system.domain.SysMessage;
import org.dromara.system.domain.vo.SysMessageVo;
import org.dromara.system.mapper.SysMessageMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageBoxQueryTest {

    @Test
    void storedExtensionDataIsDecodedInTheReturnedMessage() {
        var mapper = mock(SysMessageMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "messages"), SysMessage.class);
        when(mapper.currentModelClass()).thenReturn(SysMessage.class);
        when(mapper.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<SysMessage, SysMessageVo>(mapper));
        var entity = new SysMessage();
        entity.setDataJson("{\"noticeId\":31}");
        when(mapper.selectList(any(IPage.class), any())).thenReturn(List.of(entity));
        var vo = new SysMessageVo();
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var db = mockStatic(DataBaseHelper.class)) {
            db.when(() -> DataBaseHelper.findInSet(42L, "send_user_ids")).thenReturn("find_in_set('42', send_user_ids)");
            beans.when(() -> SpringUtil.getBean(tools.jackson.databind.json.JsonMapper.class))
                .thenReturn(tools.jackson.databind.json.JsonMapper.builder().build());
            mapping.when(() -> MapstructUtils.convert(entity, SysMessageVo.class)).thenReturn(vo);
            var box = new SysMessageServiceImpl(mapper).queryMessageBox(42L);
            assertEquals(java.util.Map.of("noticeId",31), box.getNoticeList().getFirst().getData());
        }
    }
    @Test
    void readsFirstHundredRecentVisibleMessagesForEachCategory() {
        var mapper = mock(SysMessageMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "messages"), SysMessage.class);
        when(mapper.currentModelClass()).thenReturn(SysMessage.class);
        when(mapper.lambda()).thenAnswer(call -> new LambdaCrudChainWrapper<SysMessage, SysMessageVo>(mapper));
        var earliest = LocalDateTime.now().minusDays(30);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var db = mockStatic(DataBaseHelper.class)) {
            db.when(DataBaseHelper::getDataBaseType).thenReturn(DataBaseType.MY_SQL);
            db.when(() -> DataBaseHelper.findInSet(any(), anyString())).thenCallRealMethod();
            when(mapper.selectList(any(IPage.class), any())).thenAnswer(call -> {
                IPage<SysMessage> page = call.getArgument(0);
                LambdaCrudChainWrapper<SysMessage, SysMessageVo> query = call.getArgument(1);
                assertEquals(1, page.getCurrent());
                assertEquals(100, page.getSize());
                String sql = query.getSqlSegment();
                assertTrue(sql.contains("category ="), sql);
                assertTrue(sql.contains("create_time >="), sql);
                assertTrue(sql.contains("AND (send_user_ids ="), sql);
                assertTrue(sql.contains("OR find_in_set('42' , send_user_ids) <> 0)"), sql);
                assertTrue(sql.endsWith("ORDER BY create_time DESC,message_id DESC"), sql);
                var values = query.getParamNameValuePairs().values();
                assertTrue(values.contains("0"));
                var cutoff = values.stream().filter(LocalDateTime.class::isInstance)
                    .map(LocalDateTime.class::cast).findFirst().orElseThrow();
                assertFalse(cutoff.isBefore(earliest));
                assertFalse(cutoff.isAfter(LocalDateTime.now().minusDays(30)));
                String category = values.stream().filter(String.class::isInstance).map(String.class::cast)
                    .filter(Set.of("system", "notice", "workflow")::contains).findFirst().orElseThrow();
                var entity = new SysMessage();
                entity.setCategory(category);
                var view = new SysMessageVo();
                view.setCategory(category);
                mapping.when(() -> MapstructUtils.convert(entity, SysMessageVo.class)).thenReturn(view);
                return List.of(entity);
            });

            var result = new SysMessageServiceImpl(mapper).queryMessageBox(42L);
            assertEquals(List.of("system"), result.getSystemList().stream().map(SysMessageVo::getCategory).toList());
            assertEquals(List.of("notice"), result.getNoticeList().stream().map(SysMessageVo::getCategory).toList());
            assertEquals(List.of("workflow"), result.getWorkflowList().stream().map(SysMessageVo::getCategory).toList());
        }
    }
}
