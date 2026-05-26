-- ============================================================
-- Snail AI MySQL 全量建表脚本（仅 CREATE，无 ALTER）
-- 使用：mysql -u user -p database < snail_ai_schema.sql
-- ============================================================

-- ============================================================
-- Enterprise RAG Schema for MySQL
-- ============================================================

-- Knowledge Base
CREATE TABLE snail_ai_rag
(
    id                        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL,
    description               TEXT,
    icon                      VARCHAR(512),
    embedding_model_id        BIGINT(128)  NOT NULL,
    dimension_of_vector_model INT          NOT NULL COMMENT '向量维度',
    rerank_model_id           BIGINT(128),
    search_engine_instance_id BIGINT(128),
    vector_store_instance_id  BIGINT(128),
    search_engine_enable      TINYINT(1)            DEFAULT 0,
    delimiter                 VARCHAR(32)           DEFAULT '\n\n',
    rag_enhancement           TEXT,
    config                    TEXT                  DEFAULT NULL COMMENT 'RAG检索和问答的页面配置参数',
    dedup_strategy            TINYINT(1)   NOT NULL DEFAULT 2            COMMENT '去重策略: 0=NONE 1=BY_NAME 2=BY_CONTENT 3=BY_NAME_OR_CONTENT',
    dedup_action              TINYINT(1)   NOT NULL DEFAULT 0            COMMENT '冲突动作: 0=REJECT 1=SKIP 2=OVERWRITE',
    upload_confirm            TINYINT(1)   NOT NULL DEFAULT 1            COMMENT '上传前二次确认: 0-关 1-开',
    create_dt                 TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    update_dt                 TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- RAG Documents
CREATE TABLE snail_ai_rag_document
(
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rag_id       BIGINT NOT NULL,
    name         VARCHAR(255),
    file_type    VARCHAR(32),
    source_type  VARCHAR(32),
    source_path  VARCHAR(1024),
    storage_path VARCHAR(1024),
    storage_type VARCHAR(32) DEFAULT 'LOCAL',
    file_size    BIGINT      DEFAULT 0,
    content      TEXT,
    status       TINYINT(1)  DEFAULT 0 COMMENT '状态: 0-待处理 1-解析中 2-处理中 3-处理完成 4-处理失败',
    error_msg    TEXT,
    chunk_count  INT         DEFAULT 0,
    content_hash VARCHAR(64) DEFAULT NULL COMMENT '文件内容SHA-256哈希，用于去重',
    resource_id  BIGINT      DEFAULT NULL COMMENT '关联资源库 snail_ai_resource.id',
    create_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_rag_doc_rag ON snail_ai_rag_document (rag_id);
CREATE INDEX idx_rag_content_hash ON snail_ai_rag_document (rag_id, content_hash);
CREATE INDEX idx_rag_name ON snail_ai_rag_document (rag_id, name);
CREATE INDEX idx_rag_doc_resource ON snail_ai_rag_document (resource_id);

-- RAG Chunks
CREATE TABLE snail_ai_rag_chunk
(
    id              BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rag_id          BIGINT NOT NULL,
    document_id     BIGINT NOT NULL,
    paragraph_index INT,
    chunk_index     INT,
    content         TEXT,
    token_count     INT,
    vector_id       VARCHAR(128),
    content_hash    VARCHAR(64) DEFAULT NULL COMMENT 'chunk内容SHA-256，用于向量去重',
    create_dt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_rag_chunk_rag ON snail_ai_rag_chunk (rag_id);
CREATE INDEX idx_rag_chunk_document ON snail_ai_rag_chunk (document_id);
CREATE INDEX idx_chunk_rag_hash ON snail_ai_rag_chunk (rag_id, content_hash);

CREATE TABLE snail_ai_user
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    role           INT,
    totals         INT,
    username       VARCHAR(255),
    email          VARCHAR(64),
    password       VARCHAR(64) NOT NULL,
    create_dt      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

INSERT INTO snail_ai_user (id, role, username, email, password, create_dt, update_dt)
VALUES (1, 2, 'admin', '',  '094c883e17947ff795de8b22279d81c2600dfe85e2dba3fbf562423e883b07ca',
        '2026-02-11 13:56:48.210429', '2026-02-11 13:56:48.210429');

-- ============================================
-- 1. AI 模型提供商表
-- ============================================
CREATE TABLE IF NOT EXISTS snail_ai_model_provider
(
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider_name VARCHAR(255) NOT NULL COMMENT '提供商名称',
    provider_key  VARCHAR(50)  NOT NULL COMMENT '提供商标识符',
    description   TEXT COMMENT '提供商描述',
    icon_url      VARCHAR(500) COMMENT 'LOGO图标URL',
    is_enabled    TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_dt    TIMESTAMP  DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_dt    TIMESTAMP  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_provider_name (provider_name),
    UNIQUE KEY uk_provider_key (provider_key)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'AI模型提供商表';

CREATE INDEX idx_provider_key ON snail_ai_model_provider (provider_key);
CREATE INDEX idx_is_enabled ON snail_ai_model_provider (is_enabled);

-- ============================================
-- 2. AI模型配置表
-- ============================================
CREATE TABLE IF NOT EXISTS snail_ai_model_config
(
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider_id  BIGINT       NOT NULL COMMENT '提供商ID',
    model_name   VARCHAR(255) NOT NULL COMMENT '模型名称',
    model_key    VARCHAR(100) NOT NULL COMMENT '模型标识符',
    model_type   VARCHAR(50)  NOT NULL COMMENT '模型类型(CHAT/EMBEDDING/RERANKER/IMAGE/SPEECH)',
    description  VARCHAR(1000) COMMENT '模型描述',
    api_key      VARCHAR(1000) COMMENT 'API密钥(加密存储)',
    api_endpoint VARCHAR(500) COMMENT 'API端点URL',
    config_json  TEXT COMMENT '模型参数配置(JSON格式)',
    owner_id     BIGINT COMMENT '所有者ID(NULL=全局,具体值=用户ID)',
    scope        VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL' COMMENT '作用域(GLOBAL/PERSONAL)',
    is_default   TINYINT(1)            DEFAULT 0 COMMENT '是否为默认模型',
    is_enabled   TINYINT(1)            DEFAULT 1 COMMENT '是否启用',
    created_dt   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_dt   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY fk_provider_id (provider_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'AI模型配置表';

CREATE INDEX idx_provider_model_type ON snail_ai_model_config (provider_id, model_type);
CREATE INDEX idx_model_type_enabled ON snail_ai_model_config (model_type, is_enabled);
CREATE INDEX idx_owner_id ON snail_ai_model_config (owner_id);
CREATE INDEX idx_is_default ON snail_ai_model_config (is_default);
CREATE INDEX idx_scope ON snail_ai_model_config (scope);
CREATE INDEX idx_model_key ON snail_ai_model_config (model_key);

-- ============================================
-- 3. 模型使用统计表
-- ============================================
CREATE TABLE IF NOT EXISTS snail_ai_model_usage_stat
(
    id                BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    model_id          BIGINT    NOT NULL COMMENT '模型ID',
    user_id           BIGINT    NOT NULL COMMENT '用户ID',
    total_calls       BIGINT         DEFAULT 0 COMMENT '总调用次数',
    success_calls     BIGINT         DEFAULT 0 COMMENT '成功调用次数',
    failed_calls      BIGINT         DEFAULT 0 COMMENT '失败调用次数',
    total_tokens_used BIGINT         DEFAULT 0 COMMENT '总Token使用量',
    total_cost        DECIMAL(18, 8) DEFAULT 0 COMMENT '总费用',
    avg_response_time BIGINT         DEFAULT 0 COMMENT '平均响应时间(毫秒)',
    last_used_dt      TIMESTAMP NULL DEFAULT NULL COMMENT '最后使用时间',
    created_dt        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_dt        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY unique_model_user (model_id, user_id),
    KEY fk_stat_model_id (model_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '模型使用统计表';

CREATE INDEX idx_model_id ON snail_ai_model_usage_stat (model_id);
CREATE INDEX idx_user_id ON snail_ai_model_usage_stat (user_id);
CREATE INDEX idx_last_used_dt ON snail_ai_model_usage_stat (last_used_dt);

-- ============================================
-- 初始化数据 (可选)
-- ============================================
-- 插入常见的AI提供商（重复 provider_key 则忽略）
INSERT IGNORE INTO snail_ai_model_provider (provider_name, provider_key, description, is_enabled)
VALUES ('OpenAI', 'openai', 'OpenAI官方模型 (GPT-4, GPT-3.5等)', 1),
       ('Claude', 'claude', 'Anthropic Claude模型', 1),
       ('Ollama', 'ollama', '本地开源模型 (Llama, Mistral等)', 1),
       ('Google Gemini', 'gemini', 'Google Gemini模型', 1),
       ('阿里云百炼', 'qwen', '阿里云百炼 OpenAI 兼容模型 (Qwen等)', 1),
       ('DeepSeek', 'deepseek', 'DeepSeek OpenAI 兼容模型', 1),
       ('智谱AI', 'zhipu', '智谱AI OpenAI 兼容模型 (GLM等)', 1);

-- ============================================
-- 智能体相关表
-- ============================================

-- 智能体主表
CREATE TABLE IF NOT EXISTS snail_ai_agent
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL COMMENT '智能体名称',
    description             TEXT COMMENT '智能体描述',
    avatar                  VARCHAR(512) COMMENT '头像URL',
    instruction             TEXT COMMENT '系统指令(System Prompt)',
    greeting                TEXT COMMENT '欢迎语',
    preset_questions        TEXT COMMENT '预设问题列表（JSON数组字符串）',
    chat_model_id           BIGINT COMMENT '关联的对话模型ID',
    memory_enabled          TINYINT(1)  DEFAULT 0 COMMENT '是否启用记忆库',
    mcp_enabled             TINYINT(1)  DEFAULT 0 COMMENT '是否启用MCP',
    skill_enabled           TINYINT(1)  DEFAULT 0 COMMENT '是否启用Skill',
    web_search_enabled      TINYINT(1)  DEFAULT 0 COMMENT '是否启用联网搜索',
    rag_enabled             TINYINT(1)  DEFAULT 0 COMMENT '是否启用RAG',
    rag_id                  BIGINT NULL COMMENT '绑定的RAG ID',
    short_term_memory_size  INT         DEFAULT 20 COMMENT '短期记忆滑动窗口保留条数',
    creator_id              BIGINT COMMENT '创建者用户ID',
    is_featured             TINYINT(1)  DEFAULT 0 COMMENT '是否精选',
    view_count              INT         DEFAULT 0 COMMENT '浏览次数',
    status                  TINYINT     DEFAULT 1 COMMENT '状态: 1-活跃 2-非活跃 3-已废弃 4-已禁用',
    config                  TEXT COMMENT '扩展配置(预留)',
    app_id                  VARCHAR(128) NULL COMMENT '关联应用ID(NULL=本地执行)',
    create_dt               TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_dt               TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '智能体表';

CREATE INDEX idx_agent_creator ON snail_ai_agent (creator_id);
CREATE INDEX idx_agent_featured ON snail_ai_agent (is_featured);

-- 智能体对话表
CREATE TABLE IF NOT EXISTS snail_ai_agent_conversation
(
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT      NOT NULL COMMENT '智能体ID',
    user_id         BIGINT      NOT NULL COMMENT '用户ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '对话ID(UUID)',
    title           VARCHAR(255) COMMENT '对话标题',
    create_dt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_dt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conv_id (conversation_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '智能体对话表';

CREATE INDEX idx_agent_conv_agent ON snail_ai_agent_conversation (agent_id);
CREATE INDEX idx_agent_conv_user ON snail_ai_agent_conversation (user_id);

-- 智能体对话消息记录表
CREATE TABLE IF NOT EXISTS snail_ai_agent_conversation_record
(
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT      NOT NULL COMMENT '智能体ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '对话ID',
    user_id         BIGINT      NOT NULL COMMENT '用户ID',
    role            VARCHAR(16) DEFAULT 'user' COMMENT 'user/assistant',
    content         TEXT COMMENT '消息内容',
    thinking        TEXT COMMENT '思考过程（仅assistant）',
    status          INT         DEFAULT 1 COMMENT '1=成功,2=失败,3=进行中',
    token_count     INT         DEFAULT 0 COMMENT 'Token数',
    create_dt       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '智能体对话消息记录';

CREATE INDEX idx_agent_rec_conv ON snail_ai_agent_conversation_record (conversation_id);

-- 智能体使用统计表
CREATE TABLE IF NOT EXISTS snail_ai_agent_usage_stat
(
    id                 BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id           BIGINT NOT NULL COMMENT '智能体ID',
    user_id            BIGINT NOT NULL COMMENT '用户ID',
    user_name          VARCHAR(255) COMMENT '用户名',
    department         VARCHAR(255) DEFAULT '' COMMENT '部门',
    message_count      INT          DEFAULT 0 COMMENT '消息条数',
    conversation_count INT          DEFAULT 0 COMMENT '对话轮次',
    stat_date          DATE   NOT NULL COMMENT '统计日期',
    create_dt          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_user_date (agent_id, user_id, stat_date)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '智能体使用统计';

CREATE INDEX idx_usage_agent ON snail_ai_agent_usage_stat (agent_id);
CREATE INDEX idx_usage_date ON snail_ai_agent_usage_stat (stat_date);

-- ============================================
-- MCP 服务管理
-- ============================================

-- MCP 服务配置表
CREATE TABLE IF NOT EXISTS snail_ai_mcp_server
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(255)  NOT NULL COMMENT 'MCP服务名称',
    description      TEXT          COMMENT 'MCP服务描述',
    transport_type   TINYINT(1)    DEFAULT 1 COMMENT '传输类型: 1-SSE 2-Streamable HTTP 3-Stdio',
    base_uri         VARCHAR(1024) COMMENT '服务基础地址(SSE/Streamable HTTP时使用)',
    endpoint         VARCHAR(1024) COMMENT '端点路径(SSE/Streamable HTTP时可选)',
    command          VARCHAR(1024) COMMENT 'Stdio命令(Stdio时必填)',
    args             TEXT          COMMENT 'Stdio命令参数(JSON数组)',
    env_vars         TEXT          COMMENT 'Stdio环境变量(JSON对象)',
    version          VARCHAR(32)   DEFAULT '1.0.0' COMMENT '版本',
    auth_type        TINYINT(1)    DEFAULT 0 COMMENT '认证方式: 0-无需认证 1-API Key 2-OAuth 3-Basic Auth',
    auth_config      TEXT          COMMENT '认证配置(JSON)',
    status           TINYINT(1)    DEFAULT 0 COMMENT '状态: 0-未连接 1-已连接 2-异常',
    capabilities     TEXT          COMMENT '能力列表(JSON数组)',
    last_connect_dt  TIMESTAMP NULL COMMENT '最后连接时间',
    creator_id       BIGINT        COMMENT '创建者用户ID',
    create_dt        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP服务配置表';

CREATE INDEX idx_mcp_server_creator ON snail_ai_mcp_server (creator_id);
CREATE INDEX idx_mcp_server_status ON snail_ai_mcp_server (status);

-- 智能体与MCP服务关联表(多对多)
CREATE TABLE IF NOT EXISTS snail_ai_agent_mcp_server
(
    id            BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id      BIGINT    NOT NULL COMMENT '智能体ID',
    mcp_server_id BIGINT    NOT NULL COMMENT 'MCP服务ID',
    create_dt     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_mcp (agent_id, mcp_server_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '智能体MCP服务关联表';

CREATE INDEX idx_agent_mcp_agent ON snail_ai_agent_mcp_server (agent_id);
CREATE INDEX idx_agent_mcp_server ON snail_ai_agent_mcp_server (mcp_server_id);

-- 用户订阅的智能体（多对多）
CREATE TABLE IF NOT EXISTS snail_ai_user_agent
(
    id         BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL COMMENT '用户ID',
    agent_id   BIGINT    NOT NULL COMMENT '智能体ID',
    create_dt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_agent (user_id, agent_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '用户订阅的智能体';

CREATE INDEX idx_user_agent_user ON snail_ai_user_agent (user_id);

-- ============================================
-- Skill 技能包管理
-- ============================================

-- Skill 技能包表
CREATE TABLE IF NOT EXISTS snail_ai_skill
(
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL COMMENT 'Skill名称(从SKILL.md解析)',
    description   TEXT          COMMENT 'Skill描述(从SKILL.md解析)',
    file_name     VARCHAR(255)  COMMENT '上传的zip文件名',
    file_path     VARCHAR(1024) COMMENT '解压后存储路径',
    file_size     BIGINT        DEFAULT 0 COMMENT '文件大小(字节)',
    skill_content LONGTEXT          COMMENT 'SKILL.md正文内容(去除frontmatter)',
    storage_path  VARCHAR(500)  DEFAULT NULL COMMENT '对象存储相对路径前缀（如 skills/123/）',
    version       BIGINT        DEFAULT 0 COMMENT '版本号，文件变更时自增，用于缓存一致性校验',
    has_files     TINYINT(1)    DEFAULT 0 COMMENT '是否包含支撑文件（0=仅SKILL.md，1=有scripts/references等）',
    creator_id    BIGINT        COMMENT '创建者用户ID',
    create_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'Skill技能包表';

CREATE INDEX idx_skill_creator ON snail_ai_skill (creator_id);

-- Skill 支撑文件内容表
CREATE TABLE IF NOT EXISTS snail_ai_skill_file
(
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    skill_id   BIGINT       NOT NULL COMMENT 'Skill ID',
    file_path  VARCHAR(255) NOT NULL COMMENT '文件相对路径',
    content    LONGTEXT     NOT NULL COMMENT '文件内容',
    file_size  INT          NOT NULL COMMENT '文件大小(字节)',
    encoding   VARCHAR(50)  DEFAULT 'utf-8' COMMENT '编码方式',
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_skill_path (skill_id, file_path) COMMENT '同一Skill不能有重复的文件路径',
    KEY idx_skill_id (skill_id) COMMENT '技能ID索引'
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'Skill支撑文件内容表';

-- 智能体与Skill关联表(多对多)
CREATE TABLE IF NOT EXISTS snail_ai_agent_skill
(
    id         BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id   BIGINT    NOT NULL COMMENT '智能体ID',
    skill_id   BIGINT    NOT NULL COMMENT 'Skill ID',
    create_dt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_skill (agent_id, skill_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '智能体Skill关联表';

CREATE INDEX idx_agent_skill_agent ON snail_ai_agent_skill (agent_id);
CREATE INDEX idx_agent_skill_skill ON snail_ai_agent_skill (skill_id);

-- ============================================================
CREATE TABLE IF NOT EXISTS snail_ai_store_instance
(
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(128) NOT NULL COMMENT '实例名称',
    category   TINYINT(1)   NOT NULL COMMENT '分类: 1-向量库 2-搜索引擎',
    type       TINYINT(1)   NOT NULL COMMENT '类型: 1-PG_VECTOR 2-MILVUS 3-ELASTICSEARCH 4-PG_FULLTEXT',
    config     TEXT         DEFAULT NULL COMMENT '连接参数 JSON',
    status     TINYINT(1)   DEFAULT 1 COMMENT '状态: 0-停用 1-启用',
    is_default TINYINT(1)   DEFAULT 0 COMMENT '是否为该 category 下默认实例',
    create_dt  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '存储实例';

CREATE INDEX idx_store_instance_category ON snail_ai_store_instance (category);
CREATE INDEX idx_store_instance_type ON snail_ai_store_instance (type);

-- ============================================================
-- 记忆系统（配置 / 主表 / 历史 / 摘要 / 统计 / 提取进度）
-- 依赖：snail_ai_store_instance（conversation_memory 外键）
-- ============================================================

-- 客户端应用
-- ----------------------------
CREATE TABLE IF NOT EXISTS snail_ai_app
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_id         VARCHAR(128) NOT NULL COMMENT '应用唯一标识',
    app_name       VARCHAR(255) NOT NULL COMMENT '应用名称',
    description    VARCHAR(512) COMMENT '应用描述',
    token          VARCHAR(128) NOT NULL COMMENT '通信认证令牌',
    route_strategy VARCHAR(32)  DEFAULT 'LEAST_LOAD' COMMENT '路由策略',
    status         TINYINT(1)   DEFAULT 1 COMMENT '1=启用, 0=停用',
    create_dt      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_id (app_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '客户端应用';

INSERT INTO snail_ai_app VALUES (1, '1', 'demo', '', 'SAI_7c557fc05a304b0482a65ca67afdb0b8', 'LEAST_LOAD', 1, '2026-05-26 12:31:17', '2026-05-26 12:31:17');

-- ----------------------------
-- AI客户端实例节点
-- ----------------------------
CREATE TABLE IF NOT EXISTS snail_ai_client_node
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_id              VARCHAR(128) NOT NULL COMMENT '所属应用ID',
    host_id             VARCHAR(128) NOT NULL COMMENT '客户端实例唯一标识',
    host_ip             VARCHAR(64)  NOT NULL COMMENT '客户端IP',
    grpc_port           INT          NOT NULL COMMENT '客户端gRPC端口',
    max_concurrent      INT          DEFAULT 10 COMMENT '最大并发对话数',
    active_chats        INT          DEFAULT 0 COMMENT '当前活跃对话数',
    supported_providers TEXT COMMENT '支持的模型提供商(JSON数组)',
    labels              TEXT COMMENT '路由标签',
    expire_dt           DATETIME     NOT NULL COMMENT '过期时间(心跳更新)',
    create_dt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_client_node (app_id, host_id),
    INDEX idx_app_expire (app_id, expire_dt)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = 'AI客户端实例节点';

-- OpenAPI 外部用户映射表
CREATE TABLE snail_ai_openapi_user
(
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_id           VARCHAR(128) NOT NULL COMMENT '关联 snail_ai_app.app_id',
    open_id          VARCHAR(64)  NOT NULL COMMENT '平台分配的唯一标识（UUID）',
    platform_user_id BIGINT       NOT NULL COMMENT '关联 snail_ai_user.id，注册时自动创建',
    external_id      VARCHAR(256) DEFAULT NULL COMMENT '外部系统的用户标识（可选，幂等用）',
    nickname         VARCHAR(128) DEFAULT NULL COMMENT '外部用户昵称',
    create_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_dt        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_open (app_id, open_id),
    UNIQUE KEY uk_app_external (app_id, external_id),
    INDEX            idx_open_id (open_id),
    INDEX            idx_platform_user (platform_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='OpenAPI 外部用户映射表';

-- ----------------------------
-- 通用资源存储
-- ----------------------------
CREATE TABLE IF NOT EXISTS snail_ai_resource
(
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    storage_key   VARCHAR(512)  NOT NULL COMMENT '存储键（相对路径或对象Key）',
    original_name VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    file_size     BIGINT        DEFAULT 0 COMMENT '文件大小(bytes)',
    mime_type     VARCHAR(128)  COMMENT 'MIME类型',
    storage_type  VARCHAR(32)   NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型: LOCAL/MINIO',
    access_url    VARCHAR(1024) COMMENT '访问URL',
    biz_type      VARCHAR(64)   NOT NULL DEFAULT 'GENERAL' COMMENT '业务类型: AVATAR/ATTACHMENT/DOCUMENT/GENERAL',
    biz_id        BIGINT        COMMENT '关联业务ID',
    creator_id    BIGINT        COMMENT '上传者ID',
    create_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_dt     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_storage_key (storage_key),
    INDEX idx_biz (biz_type, biz_id),
    INDEX idx_creator (creator_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '通用资源存储';
