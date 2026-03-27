package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.dto.PushPayloadDTO;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.service.MessageService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.push.helper.PushHelper;
import org.dromara.system.domain.SysMessage;
import org.dromara.system.domain.vo.SysMessageBoxVo;
import org.dromara.system.domain.vo.SysMessageVo;
import org.dromara.system.mapper.SysMessageMapper;
import org.dromara.system.service.ISysMessageService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 消息记录服务实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysMessageServiceImpl implements ISysMessageService, MessageService {

    private static final String GLOBAL_USER_IDS = "0";
    private static final String CATEGORY_SYSTEM = "system";
    private static final String CATEGORY_NOTICE = "notice";
    private static final String CATEGORY_WORKFLOW = "workflow";
    private static final int BOX_LIMIT = 100;
    private static final long BOX_DAYS = 30L;

    private final SysMessageMapper baseMapper;

    @Override
    public SysMessageBoxVo queryMessageBox(Long userId) {
        SysMessageBoxVo box = new SysMessageBoxVo();
        box.setSystemList(selectMessageList(CATEGORY_SYSTEM, userId));
        box.setNoticeList(selectMessageList(CATEGORY_NOTICE, userId));
        box.setWorkflowList(selectMessageList(CATEGORY_WORKFLOW, userId));
        return box;
    }

    @Override
    public void sendMessage(Long userId, String message) {
        PushHelper.sendMessage(userId, buildDefaultMessage(message));
    }

    @Override
    public void sendMessage(String message) {
        PushHelper.sendMessage(buildDefaultMessage(message));
    }

    @Override
    public void sendMessage(Long userId, PushPayloadDTO payload) {
        PushHelper.sendMessage(userId, payload);
    }

    @Override
    public void sendMessage(PushPayloadDTO payload) {
        PushHelper.sendMessage(payload);
    }

    @Override
    public void publishMessage(List<Long> userIds, PushPayloadDTO payload) {
        PushHelper.publishMessage(userIds, storeUsers(userIds, payload));
    }

    @Override
    public void publishAll(String message) {
        publishAll(buildDefaultMessage(message));
    }

    @Override
    public void publishAll(PushPayloadDTO payload) {
        PushHelper.publishAll(storeAll(payload));
    }

    @Override
    public PushPayloadDTO storeAll(PushPayloadDTO payload) {
        return storeMessage(null, payload);
    }

    @Override
    public PushPayloadDTO storeUsers(List<Long> userIds, PushPayloadDTO payload) {
        return storeMessage(userIds, payload);
    }

    private PushPayloadDTO storeMessage(List<Long> userIds, PushPayloadDTO payload) {
        if (!supportsMessageBox(payload)) {
            return payload;
        }
        SysMessage message = buildMessage(userIds, payload);
        baseMapper.insert(message);
        payload.setMessageId(message.getMessageId());
        return payload;
    }

    private List<SysMessageVo> selectMessageList(String category, Long userId) {
        LambdaQueryWrapper<SysMessage> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysMessage::getCategory, category);
        lqw.ge(SysMessage::getCreateTime, new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(BOX_DAYS)));
        lqw.and(wrapper -> wrapper.eq(SysMessage::getSendUserIds, GLOBAL_USER_IDS)
            .or()
            .apply(DataBaseHelper.findInSet(userId, "send_user_ids")));
        lqw.orderByDesc(SysMessage::getCreateTime, SysMessage::getMessageId);
        List<SysMessage> list = baseMapper.selectList(new Page<>(1, BOX_LIMIT, false), lqw);
        return list.stream().map(this::buildVo).toList();
    }

    private SysMessage buildMessage(List<Long> userIds, PushPayloadDTO payload) {
        SysMessage message = new SysMessage();
        message.setMessageId(payload.getMessageId() == null ? IdGeneratorUtil.nextLongId() : payload.getMessageId());
        message.setCategory(resolveCategory(payload));
        message.setType(payload.getType());
        message.setSource(payload.getSource());
        message.setTitle(resolveTitle(payload));
        message.setMessage(payload.getMessage());
        message.setContent(resolveContent(payload));
        message.setDataJson(JsonUtils.toJsonString(payload.getData()));
        message.setPath(payload.getPath());
        message.setSendUserIds(CollUtil.isEmpty(userIds) ? GLOBAL_USER_IDS : StringUtils.joinComma(userIds));
        return message;
    }

    private SysMessageVo buildVo(SysMessage entity) {
        SysMessageVo vo = MapstructUtils.convert(entity, SysMessageVo.class);
        vo.setData(parseData(entity.getDataJson()));
        return vo;
    }

    private boolean supportsMessageBox(PushPayloadDTO payload) {
        if (payload == null) {
            return false;
        }
        if (StringUtils.equalsAny(payload.getType(), PushTypeEnum.MESSAGE.getType(), PushTypeEnum.NOTICE.getType())) {
            return !StringUtils.equalsAny(payload.getType(), PushTypeEnum.LLM.getType())
                && !StringUtils.equalsAny(payload.getSource(), PushSourceEnum.LLM.getSource());
        }
        return false;
    }

    private String resolveCategory(PushPayloadDTO payload) {
        if (StringUtils.equalsAny(payload.getType(), PushTypeEnum.NOTICE.getType())
            || StringUtils.equalsAny(payload.getSource(), PushSourceEnum.NOTICE.getSource())) {
            return CATEGORY_NOTICE;
        }
        if (StringUtils.equalsAny(payload.getSource(), PushSourceEnum.WORKFLOW.getSource())) {
            return CATEGORY_WORKFLOW;
        }
        return CATEGORY_SYSTEM;
    }

    private String resolveTitle(PushPayloadDTO payload) {
        return switch (resolveCategory(payload)) {
            case CATEGORY_NOTICE -> "通知公告消息";
            case CATEGORY_WORKFLOW -> "工作流消息";
            default -> "系统消息";
        };
    }

    private String resolveContent(PushPayloadDTO payload) {
        Object data = payload.getData();
        if (data instanceof Map<?, ?> map) {
            return Convert.toStr(map.get("noticeContent"));
        }
        return null;
    }

    private Object parseData(String dataJson) {
        if (StringUtils.isBlank(dataJson)) {
            return null;
        }
        return JsonUtils.parseObject(dataJson, Object.class);
    }

    private PushPayloadDTO buildDefaultMessage(String message) {
        return PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null);
    }
}
