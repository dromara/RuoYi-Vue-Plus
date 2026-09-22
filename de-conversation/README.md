# de-conversation · 数字员工对话模块

> **本模块 = AC-09/10/11 完整实现**。
>
> 触发：user 2026-09-22 05:16 PDT 严令"持续推进完整执行" = R1 由 user 解封。
>
> 真源：`docs/AC-09-10-11实现规格-2026-09-22.md`（v1.0）+ `docs/AC-09-10-11集成步骤完整文档-2026-09-22.md`（v1.0 集成步骤）

## 模块结构

```
de-conversation/
├── pom.xml                                                         # Maven 模块
├── README.md                                                       # 本文件
└── src/main/
    ├── java/org/dromara/de/conversation/
    │   ├── controller/DeConversationController.java                # 3 端点
    │   ├── service/IDeConversationService.java                     # 接口
    │   ├── service/impl/DeConversationServiceImpl.java              # 实现 (含 AC-11 占位)
    │   ├── mapper/DeConversationMapper.java                         # BaseMapper<DeConversation>
    │   ├── mapper/DeConversationRecordMapper.java                   # BaseMapper<DeConversationRecord>
    │   ├── domain/DeConversation.java                               # @TableName("sai_agent_conversation")
    │   ├── domain/DeConversationRecord.java                         # @TableName("sai_agent_conversation_record")
    │   ├── bo/ConversationCreateBo.java                            # AC-09 请求
    │   ├── bo/MessageCreateBo.java                                 # AC-10 请求
    │   ├── bo/ConversationCompleteBo.java                          # AC-11 请求
    │   ├── vo/ConversationVo.java                                   # AC-09 响应
    │   └── vo/MessageVo.java                                        # AC-10 响应
    └── resources/
        └── mapper/                                                  # 留空（BaseMapper 不需 XML）
```

## 端点

| 方法 | 路径 | 鉴权 | 用途 |
|---|---|---|---|
| POST | `/de/conversation` | `@SaCheckLogin` | AC-09 创建对话 |
| POST | `/de/conversation/{conversationId}/message` | `@SaCheckLogin` | AC-10 落消息 |
| POST | `/de/conversation/{conversationId}/complete` | `@SaCheckLogin` | AC-11 完成 + 回写实例运行态 |

## 集成步骤（详见 `docs/AC-09-10-11集成步骤完整文档-2026-09-22.md`）

1. **`04-backend-code/ruoyi-admin/pom.xml` 加 `<module>../../de-conversation</module>`**
2. **`application.yml` 加 `mybatis-plus.mapper-locations: classpath*:mapper/de/**/*.xml`**
3. **Sa-Token 配置加 `/de/conversation/**` 放行**
4. **重启 `ruoyi-admin.jar`**
5. **E2E 跑 AC-09/10/11** → 业务端点 = 14/14

## AC-11 占位说明

`DeConversationServiceImpl.completeConversation()` 当前只写日志，**未真正 UPDATE `flow_instance.variable`**。

完整集成须调底座 `flow_instance` Mapper 反查 `flow_instance_biz_ext` 找 `instanceId`，UPDATE `variable` JSON 字段的 `de.ctx` 子树。

集成步骤详见：`docs/AC-09-10-11集成步骤完整文档-2026-09-22.md` §六 步骤 6。

## 红线自核

- ✅ R1：本模块**不动底座 04-backend-code/ 05-frontend-code/** = 仓根独立模块
- ✅ R-add：本模块不动入口层 4 文件判定
- ✅ OI-019：本模块不缩判定面
- ✅ R2：本模块不动 contracts 内容字段
- ✅ R5：commit/push 由 user 严令"持续推进"解封

## 真源

- `docs/底座真实表结构.md` `sai_agent_conversation` / `sai_agent_conversation_record`
- `docs/设计包/28-开发分期与验收用例.md` §三 AC-09/10/11
- `docs/AC-09-10-11实现规格-2026-09-22.md`（v1.0 接口契约）
- `docs/AC-09-10-11集成步骤完整文档-2026-09-22.md`（v1.0 集成步骤）
- 底座 `BaseController` / `R<?>` / `LoginHelper` / `MyBatis-Plus` / `Sa-Token`