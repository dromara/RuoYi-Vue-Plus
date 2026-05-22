-- ----------------------------
-- Stars Library - GitHub Stars 知识库
-- 主键由应用侧分配（雪花 ID），不使用 AUTO_INCREMENT
-- ----------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- GitHub 账号绑定
-- ----------------------------
create table stars_github_account
(
    id            bigint(20)   not null                   comment '主键',
    user_id       bigint(20)   not null                   comment 'RuoYi 用户 ID',
    github_login  varchar(100) default null               comment 'GitHub 用户名',
    access_token  varchar(500) not null                   comment 'AES 加密 PAT',
    token_scope   varchar(200) default null               comment 'Token 授权范围',
    bind_time     datetime     default null               comment '绑定时间',
    update_time   datetime     default null               comment '更新时间',
    primary key (id),
    unique key uk_user_id (user_id)
) engine=innodb comment = 'GitHub 账号绑定';

-- ----------------------------
-- 仓库全局缓存（跨用户共享元数据）
-- ----------------------------
create table stars_repo
(
    id                bigint(20)   not null                   comment '主键',
    full_name         varchar(200) not null                   comment 'owner/repo',
    owner             varchar(100) not null                   comment '仓库所有者',
    repo_name         varchar(100) not null                   comment '仓库名',
    description       text         default null               comment '仓库描述',
    language          varchar(50)  default null               comment '主要编程语言',
    stargazers_count  int          default 0                  comment 'Star 数',
    html_url          varchar(500) default null               comment 'GitHub 页面 URL',
    readme_snippet    text         default null               comment 'README 前 3000 字符缓存',
    readme_cached_at  datetime     default null               comment 'README 缓存时间',
    github_updated_at datetime     default null               comment 'GitHub 侧更新时间',
    create_time       datetime     default null               comment '创建时间',
    update_time       datetime     default null               comment '更新时间',
    primary key (id),
    unique key uk_full_name (full_name)
) engine=innodb comment = '仓库全局缓存';

-- ----------------------------
-- 用户-仓库关系（核心表）
-- ----------------------------
create table stars_user_repo
(
    id                    bigint(20)   not null                   comment '主键',
    user_id               bigint(20)   not null                   comment 'RuoYi 用户 ID',
    repo_id               bigint(20)   not null                   comment '仓库 ID',
    import_source         varchar(50)  not null                   comment '导入来源：self | github_username',
    note                  varchar(500) default null               comment '收藏理由/备注',
    category              varchar(50)  default null               comment '主分类',
    classification_source varchar(20)  default null               comment '分类来源：ai|manual',
    summary_one_liner     varchar(100) default null               comment '中文一句话概述',
    summary_text          varchar(500) default null               comment '中文概述',
    summary_status        varchar(20)  default 'pending'          comment '概述状态：pending|processing|done|failed|manual',
    summary_source        varchar(20)  default null               comment '概述来源：ai|manual',
    import_time           datetime     default null               comment '导入时间',
    update_time           datetime     default null               comment '更新时间',
    primary key (id),
    unique key uk_user_repo (user_id, repo_id),
    key idx_user_category (user_id, category),
    key idx_user_status (user_id, summary_status)
) engine=innodb comment = '用户-仓库关系';

-- ----------------------------
-- 用户标签
-- ----------------------------
create table stars_tag
(
    id          bigint(20)  not null                   comment '主键',
    user_id     bigint(20)  not null                   comment 'RuoYi 用户 ID',
    name        varchar(50) not null                   comment '标签名',
    color       varchar(20) default null               comment '标签颜色',
    create_time datetime    default null               comment '创建时间',
    primary key (id),
    unique key uk_user_tag (user_id, name)
) engine=innodb comment = '用户标签';

-- ----------------------------
-- 用户仓库-标签关联
-- ----------------------------
create table stars_user_repo_tag
(
    user_repo_id bigint(20) not null                   comment '用户仓库关系 ID',
    tag_id       bigint(20) not null                   comment '标签 ID',
    primary key (user_repo_id, tag_id)
) engine=innodb comment = '用户仓库标签关联';

-- ----------------------------
-- 导入任务
-- ----------------------------
create table stars_import_job
(
    id              bigint(20)   not null                   comment '主键',
    user_id         bigint(20)   not null                   comment 'RuoYi 用户 ID',
    job_type        varchar(20)  not null                   comment '任务类型：self_sync|import_user',
    source_login    varchar(100) default null               comment '他人导入时的 GitHub username',
    import_limit    int          default 100                comment '计划导入条数上限',
    status          varchar(20)  not null                   comment '状态：pending|running|done|failed|partial',
    total_count     int          default 0                  comment '总条数（通常等于 import_limit）',
    processed_count int          default 0                  comment '已处理条数',
    failed_count    int          default 0                  comment '失败条数',
    error_message   text         default null               comment '错误信息',
    start_time      datetime     default null               comment '开始时间',
    end_time        datetime     default null               comment '结束时间',
    primary key (id),
    key idx_user_job (user_id, status)
) engine=innodb comment = '导入任务';

-- ----------------------------
-- 菜单与权限 seed（stars-web 联调 / 非超管角色授权）
-- 超管 user_id=1 自带 *:*:*，无需下列 role_menu
-- 重复执行：先删 role_menu 再删 menu
-- ----------------------------
delete from sys_role_menu where menu_id between 11900 and 11905;
delete from sys_menu where menu_id between 11900 and 11905;

-- 目录
insert into sys_menu values('11900', 'Stars 知识库', '0', '6', 'stars-library', null, '', 1, 0, 'M', '0', '0', '', 'star', 103, 1, sysdate(), null, null, 'GitHub Stars 知识库');
-- 外链至 stars-web（部署时改 path 为实际地址）
insert into sys_menu values('11901', 'Stars 工作台', '11900', '1', 'http://localhost:5173', null, '', 0, 0, 'C', '0', '0', 'stars:repo:list', 'guide', 103, 1, sysdate(), null, null, 'stars-web 独立前端');
-- 按钮权限（与 Controller @SaCheckPermission 一致）
insert into sys_menu values('11902', 'GitHub 绑定', '11901', '1', '#', '', '', 1, 0, 'F', '0', '0', 'stars:github:bind', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('11903', '仓库编辑', '11901', '2', '#', '', '', 1, 0, 'F', '0', '0', 'stars:repo:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('11904', '仓库导入', '11901', '3', '#', '', '', 1, 0, 'F', '0', '0', 'stars:repo:import', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('11905', '标签管理', '11901', '4', '#', '', '', 1, 0, 'F', '0', '0', 'stars:tag:edit', '#', 103, 1, sysdate(), null, null, '');

-- 赋权给「本部门及以下」测试角色 role_id=3（按需为其他角色在管理端勾选）
insert into sys_role_menu values ('3', '11900');
insert into sys_role_menu values ('3', '11901');
insert into sys_role_menu values ('3', '11902');
insert into sys_role_menu values ('3', '11903');
insert into sys_role_menu values ('3', '11904');
insert into sys_role_menu values ('3', '11905');

-- ----------------------------
-- 已有库升级（若 stars_import_job 已存在且无 import_limit 列）
-- ----------------------------
-- alter table stars_import_job add column import_limit int default 100 comment '计划导入条数上限' after source_login;
