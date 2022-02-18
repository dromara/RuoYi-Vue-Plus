<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>RuoYi-Vue-Plus后台管理框架</h2>
        <p>
          RuoYi-Vue-Plus 是基于 RuoYi-Vue 针对 分布式集群 场景升级(不兼容原框架)
          <br/>
          * 前端开发框架 Vue、Element UI<br/>
          * 后端开发框架 Spring Boot<br/>
          * 容器框架 Undertow 基于 Netty 的高性能容器<br/>
          * 权限认证框架 Spring Security、Jwt 支持多终端认证系统<br/>
          * 关系数据库 MySQL 适配 8.X 最低 5.7<br/>
          * 缓存数据库 Redis 适配 6.X 最低 4.X<br/>
          * 数据库框架 Mybatis-Plus 快速 CRUD 增加开发效率<br/>
          * 数据库框架 p6spy 更强劲的 SQL 分析<br/>
          * 多数据源框架 dynamic-datasource 支持主从与多种类数据库异构<br/>
          * 序列化框架 Jackson 统一使用 jackson 高效可靠<br/>
          * Redis客户端 Redisson 性能强劲、API丰富<br/>
          * 分布式限流 Redisson 全局、请求IP、集群ID 多种限流<br/>
          * 分布式锁 Lock4j 注解锁、工具锁 多种多样<br/>
          * 分布式幂等 Lock4j 基于分布式锁实现<br/>
          * 分布式日志 TLog 支持跟踪链路日志记录、性能分析、链路排查<br/>
          * 分布式任务调度 Xxl-Job 高性能 高可靠 易扩展<br/>
          * 文件存储 Minio 本地存储<br/>
          * 文件存储 七牛、阿里、腾讯 云存储<br/>
          * 监控框架 SpringBoot-Admin 全方位服务监控<br/>
          * 校验框架 Validation 增强接口安全性 严谨性<br/>
          * Excel框架 Alibaba EasyExcel 性能优异 扩展性强<br/>
          * 文档框架 knife4j 美化接口文档<br/>
          * 工具类框架 Hutool、Lombok 减少代码冗余 增加安全性<br/>
          * 代码生成器 适配MP、Knife4j规范化代码 一键生成前后端代码<br/>
          * 部署方式 Docker 容器编排 一键部署业务集群<br/>
          * 国际化 SpringMessage Spring标准国际化方案<br/>
        </p>
        <p>
          <b>当前版本:</b> <span>v{{ version }}</span>
        </p>
        <p>
          <el-tag type="danger">&yen;免费开源</el-tag>
        </p>
        <p>
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://gitee.com/JavaLionLi/RuoYi-Vue-Plus')"
            >访问码云</el-button
          >
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://github.com/JavaLionLi/RuoYi-Vue-Plus')"
          >访问GitHub</el-button
          >
        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>技术选型</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端技术</h4>
            <ul>
              <li>SpringBoot</li>
              <li>Spring Security</li>
              <li>JWT</li>
              <li>MyBatis</li>
              <li>Druid</li>
              <li>Jackson</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>前端技术</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>更新日志</span>
          </div>
          <el-collapse accordion>
            <el-collapse-item title="v4.0.0 - 2022-02-18">
              <ol>
                <li>[重大更新] 重写项目整体结构 数据处理下沉至Mapper符合MVC规范 减少循环依赖</li>
                <li>[重磅更新] 主分支与satoken分支合并 权限统一使用 sa-token</li>
                <li>[重磅更新] 适配升级 SpringBoot 2.6</li>
                <li>[重磅更新] EasyExcel大版本升级3.X</li>
                <li>[重磅更新] 移除链式调用注解 因链式调用不符合java规范 导致很多问题</li>
                <li>[重磅更新] 增加 轻量级 分布式队列 支持</li>
                <li>[重磅更新] 增加 数据脱敏注解 使用序列化控制脱敏 支持多种表达式</li>
                <li>[重磅更新] 重构 使用 Spring 简化 oss 模块代码</li>
                <li>[重磅更新] 重构 调整返回类型为 R 精简 Controller 代码</li>
                <li>update springboot 2.5.8 => 2.6.3</li>
                <li>update mybatis-plus 3.4.3.4 => 3.5.1</li>
                <li>update maven-jar-plugin 3.2.0 => 3.2.2</li>
                <li>update maven-war-plugin 3.2.0 => 3.2.2</li>
                <li>update maven-compiler-plugin 3.1 => 3.9.0</li>
                <li>update hutool 5.7.18 => 5.7.20</li>
                <li>update springboot-admin 2.6.0 => 2.6.2</li>
                <li>update redisson 3.16.7 => 3.16.8</li>
                <li>update qiniu 7.9.0 => 7.9.2</li>
                <li>update aliyun 3.13.1 => 3.14.0</li>
                <li>update qcloud 5.6.58 => 5.6.68</li>
                <li>update minio 8.3.4 => 8.3.5</li>
                <li>update 用户管理部门查询选择节点后分页参数初始</li>
                <li>update 防重复提交标识组合（key + url + header）</li>
                <li>update 接口文档增加 basic 账号密码验证</li>
                <li>update 用户修改减少一次角色列表关联查询</li>
                <li>update 优化部门修改缩放后出现的错位问题</li>
                <li>update 指定 maven 资源过滤为具体文件 防止错误过滤</li>
                <li>update hutool 引入改为 bom 依赖项引入</li>
                <li>update 降低开发环境 redis连接池数量</li>
                <li>update 升级 springboot 2.6.X 解决 springfox 兼容性问题</li>
                <li>update 优化多用户体系处理 更名 LoginUtils 为 LoginHelper 支持 LoginUser 多级缓存</li>
                <li>update 优化加载字典缓存数据</li>
                <li>update 数据库更改 对接多用户体系</li>
                <li>update 移除掉 StringUtils 语义不明确的api方法 使用特定工具替换</li>
                <li>update 优化登录、注册在接口通过`@Validated`注解进行数据基础校验</li>
                <li>update 优化 查询登录用户数据 统一走缓存</li>
                <li>update 优化 redisson 配置 去除掉不常用的配置 使用默认配置</li>
                <li>update 用户访问控制时校验数据权限，防止越权</li>
                <li>update 修改用户注册报未登录警告</li>
                <li>update 调整oss预览开关 使用前端直接调用更改配置参数</li>
                <li>update 使用 satoken 自带的 BCrypt 工具 替换 Security 加密工具 减少依赖</li>
                <li>update 优化 TreeBuildUtils 工具 使用反射自动获取顶级父id</li>
                <li>update 使用 hutool Dict 优化 JsonUtils 防止类型解析异常</li>
                <li>update 优化代码生成 使用新 JsonUtils.parseMap 方法</li>
                <li>update 更新 所有 oss 均支持 https 配置</li>
                <li>add 增加 RedisUtils 工具 hasKey 检查key存在方法</li>
                <li>add 增加 监控中心 自定义事件通知</li>
                <li>add 增加 3.X update 4.0 更新sql</li>
                <li>fix 修复登录失效后多次请求提示多次弹窗问题</li>
                <li>fix 修复 StringUtils 通配符匹配无效</li>
                <li>fix 修复选项卡点击右键刷新丢失参数问题</li>
                <li>fix 修复 数据权限 缓存方法名错误问题</li>
                <li>fix 修复自定义组件`file-upload`无法显示第一个文件，列表显示的文件比实际文件少一个的问题</li>
                <li>fix 修复因升级 sa-token 导致 doLogin 无法获取 token 问题</li>
                <li>fix 修复分页组件请求两次问题</li>
                <li>remove 移除过期代码 分页工具相关</li>
                <li>remove 移除过期代码 多数据源切换</li>
                <li>remove 移除过期代码 数据权限</li>
                <li>3.X 版本进入维护阶段 不进行更新 只修复bug 持续维护到2022年10月</li>
                <li>4.X 版本公测将近一个月 大部分bug已修复 官网主分支更改为 4.X 版本 推荐使用</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v3.5.0 - 2021-12-28">
              <ol>
                <li>[重大更新] 重写数据权限实现</li>
                <li>[重磅更新] 重构分页 简化使用</li>
                <li>[重磅更新] 用户登录 支持校验错误次数锁定登录</li>
                <li>[重磅更新] 增加 jdbc 批处理参数 大幅提升批量操作性能 对原生语句与 MP 均有效</li>
                <li>update springboot 2.5.7 => 2.5.8 升级预防 log4j2 问题</li>
                <li>update springboot-admin 2.5.4 => 2.5.5</li>
                <li>update hutool 5.7.16 => 5.7.18</li>
                <li>update redisson 3.16.4 => 3.16.7</li>
                <li>update dynamic-ds 3.4.1 => 3.5.0</li>
                <li>update qiniu 7.8.0 => 7.9.0</li>
                <li>update minio 8.3.3 => 8.3.4</li>
                <li>update tlog 1.3.4 => 1.3.6 启用 tlog 自动配置</li>
                <li>update clipboard 2.0.6 => 2.0.8</li>
                <li>update 多数据源切换标注过期 3.6.0 移除 推荐使用原生注解</li>
                <li>update 通用权限服务 迁移回 ruoyi-framework 模块</li>
                <li>update 使用 hutool-jwt 替换老旧 jjwt 依赖</li>
                <li>update 调整 OSS 表字段内容长度</li>
                <li>update LoginUser 增加角色缓存 优化角色权限代码</li>
                <li>update 使用 Cglib 重构 BeanCopyUtils 性能优异</li>
                <li>update 禁止所有工具类实例化 优化代码书写规范</li>
                <li>update 优化查询用户的角色组、岗位组代码</li>
                <li>update 更新 RedisUtils 返回客户端实例</li>
                <li>update 修改 健康检查权限 改为用户放行 提高安全性</li>
                <li>update hutool 工具 改为单包引入 减少无用依赖</li>
                <li>update ServicePlusImpl 功能 下沉到 BaseMapperPlus</li>
                <li>update 去除 jdk17 标签 由于很多组件还未适配 导致一些问题</li>
                <li>udpate 代码生成预览支持复制内容</li>
                <li>update 用户导入提示溢出则显示滚动条</li>
                <li>update 路由支持单独配置菜单或角色权限</li>
                <li>update 优化web拦截器 使用原生接口处理 默认非生产环境开启</li>
                <li>update 调整监控依赖 从 common 迁移到 framework</li>
                <li>add 新增 Vue3 分支 与 代码生成模板(由于组件还未完善 仅供学习)</li>
                <li>add 增加 RedisUtils 注册监听器方法</li>
                <li>add 增加 自定义 Xss 校验注解 用户导入增加 Bean 校验</li>
                <li>add oss下载增加 loading 层</li>
                <li>add 新增图片预览组件</li>
                <li>add 集成compression-webpack-plugin插件实现打包Gzip压缩</li>
                <li>add 新增 SqlUtils 检查关键字方法</li>
                <li>fix 修复 集群雪花id重复问题 使用网卡信息绑定生成</li>
                <li>fix 修复 count 语法异常</li>
                <li>fix 修复更改密码问题</li>
                <li>fix 修复sql关键字处理 防止解析器报错</li>
                <li>fix 修复 TreeBuildUtils 顶节点不为 0 问题</li>
                <li>fix 修复 SysOssConfig 主键类型错误</li>
                <li>fix 修复代码生成 导出注解错误</li>
                <li>fix 修复 redisson 集群模式 路径未匹配协议头问题</li>
                <li>fix 修复打包后字体图标偶现的乱码问题</li>
                <li>fix 修复版本差异导致的懒加载报错问题</li>
                <li>fix 修复代码生成字典组重复问题</li>
                <li>remove 删除 jjwt 无用依赖</li>
                <li>remove 移除过期 用户导入</li>
                <li>remove 移除过期工具 DictUtils</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v3.4.0 - 2021-11-29">
              <ol>
                <li>update [重磅更新] 重构 Excel 导入 支持 Validator 校验 支持自定义监听器</li>
                <li>update [重磅更新] Validator 校验框架支持国际化</li>
                <li>update springboot 2.5.6 => 2.5.7</li>
                <li>update hutool 5.7.15 => 5.7.16</li>
                <li>update okhttp 4.9.1 => 4.9.2</li>
                <li>update spring-boot-admin 2.5.2 => 2.5.4</li>
                <li>update redisson 3.16.3 => 3.16.4</li>
                <li>update tlog 1.3.3 => 1.3.4</li>
                <li>update axios 0.21.0 => 0.24.0</li>
                <li>update core-js 3.8.1 => 3.19.1</li>
                <li>update js-cookie 2.2.1 => 3.0.1</li>
                <li>update velocity 1.7 => 2.3</li>
                <li>update 升级 docker 基础镜像</li>
                <li>update 基于 hutool 封装树构建工具 重构部门与菜单树结构返回</li>
                <li>update 减少使用特定数据库函数</li>
                <li>update 配置应用前缀路径 改为配置文件统一配置</li>
                <li>update 升级 swagger 配置 使用 knife4j 增强模式</li>
                <li>update 监控中心 集成监控客户端 实现自监控</li>
                <li>update 调度中心 集成监控客户端 注册到监控中心</li>
                <li>update 优化 tab 对象简化页签操作</li>
                <li>update 解耦 LoginUser 与 SysUser 强关联</li>
                <li>update 更新 RepeatSubmit 注解 aop 处理 针对特殊参数进行过滤</li>
                <li>update DictUtils 字典工具类 标记过期 3.5.0 版本移除 使用 DictService 代替</li>
                <li>update 抽象 DictService 通用 字典服务</li>
                <li>update 抽象 ConfigService 通用 参数配置服务</li>
                <li>update 基于 DictService 重构 Excel 内字典查询功能</li>
                <li>update OSS 模块 整体重命名 消除歧义</li>
                <li>update 更新 redis.conf 存储策略 aof 与 rdb 配置参数</li>
                <li>update 初始化数据转移到 ApplicationRunner 统一处理</li>
                <li>update 优化时间查询语句</li>
                <li>add 增加 框架缓存懒加载 开关</li>
                <li>add 新增 监控中心 Bean 初始化 startup trace 监控插件</li>
                <li>add 增加 ValidatorUtils 校验框架工具 用于在非 Controller 的地方校验对象</li>
                <li>fix 修复 SysOss、SysOssConfig 未继承 BaseEntity 基础实体问题</li>
                <li>fix 修复 xxl-job-admin 部署问题</li>
                <li>fix 修复 回显数据字典键值修正</li>
                <li>fix 修复 Linux 清除临时目录 导致上传找不到目录报错问题</li>
                <li>fix 修复通用实体 传参无法接收问题</li>
                <li>fix 修复 SysLoginController 接口文档书写错误问题</li>
                <li>fix 修复 用户逻辑删除 差异问题</li>
                <li>fix 修复 OSS 七牛云 token 过期未刷新问题</li>
                <li>fix 修复 分页工具 排序字段 null 处理</li>
                <li>fix 修复 用户导入字典使用错误</li>
                <li>fix 修复 关闭 xss 功能导致可重复读 RepeatableFilter 失效</li>
                <li>fix 修复 使用 this.$options.data 报错问题</li>
                <li>fix 修复 代码生成复选框字典遗漏问题</li>
                <li>fix 修复 重复提交不生效问题 由于概念不同 使用 RedisUtils 重构</li>
                <li>fix 修复 OSS 工厂 未实例化服务更新加载问题</li>
                <li>remove 移除 quartz 相关代码与依赖</li>
                <li>remove 移除 feign 相关代码与依赖</li>
                <li>remove 移除 MybatisPlusRedisCache 二级缓存</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v3.3.0 - 2021-10-29">
              <ol>
                <li>add [重磅更新] 增加分布式日志框架 TLog</li>
                <li>add [重磅更新] 增加分布式任务调度系统 Xxl-Job</li>
                <li>add [重大更新] 增加 ruoyi-job 任务调度模块(基于xxl-job)</li>
                <li>update [重大更新]全业务 增加 接口文档注解 格式化代码</li>
                <li>update springboot 2.5.5 => 2.5.6</li>
                <li>update springboot-admin 2.5.1 => 2.5.2</li>
                <li>update element-ui 2.15.5 => 2.15.6</li>
                <li>update hutool 5.7.13 => 5.7.15</li>
                <li>update qcloud.cos 5.6.55 => 5.6.58</li>
                <li>update minio 8.3.0 => 8.3.3</li>
                <li>update 更新 element 2.15.6 表格样式</li>
                <li>update 优化 代码生成常量 关于 BO VO 注释</li>
                <li>update 优化代码生成 导入表 列表返回 主键默认选中</li>
                <li>update MybatisPlusRedisCache 标记过期 推荐使用 spring-cache</li>
                <li>update Quartz 标记过期 推荐迁移至新框架 xxl-job</li>
                <li>update Feign 标记过期</li>
                <li>update 前端增加默认国际化参数</li>
                <li>update 更新 Admin 监控 注释 避免错误使用</li>
                <li>update Admin 监控增加日志文件输出</li>
                <li>update 优化 xxl-job-admin 增加格式化日志输出与 docker 镜像</li>
                <li>update 更新 xxl-job 执行器开关功能</li>
                <li>update 代码生成 改为生成抽象实体</li>
                <li>update 代码生成 搜索框 更新文本域生成 用于模糊查询</li>
                <li>update 通用数据注入改为适配通用实体类</li>
                <li>update 使用路由懒加载提升页面响应速度</li>
                <li>update 迁移所有脚本文件至 script 目录</li>
                <li>update swagger 组顺序配置</li>
                <li>update sql 文件更新 xxljob 控制台菜单</li>
                <li>update 前端增加 任务调度中心页面与环境及 nginx 配置</li>
                <li>update 合并 oss.sql 至主 sql</li>
                <li>update 补全国际化文件(英文)</li>
                <li>update 更新关于全局路径设置与文档链接</li>
                <li>update 删除无用 setUsername 使用自动注入</li>
                <li>update RedisUtils 更新删除 hash 数据方法</li>
                <li>fix 修复 多数据源 aop 语法错误</li>
                <li>fix 修复 子菜单无 query 参数问题</li>
                <li>fix 修复 oss 配置删除时删除缓存 bug</li>
                <li>fix 修复无权限获取请求头 download-filename 导致文件名为空问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v3.2.0 - 2021-9-28">
              <ol>
                <li>update [重大改动]接口文档 支持分组配置</li>
                <li>update [重大改动]security 路径配置抽取到配置文件</li>
                <li>update [重大改动] 将 framework 与 system 模块 解耦 调整依赖结构 解决依赖冲突</li>
                <li>update [重大改动]重写 防重提交实现 使用分布式锁 解决并发问题 压测通过</li>
                <li>update springboot 2.5.4 => 2.5.5 bugfix版本</li>
                <li>update mybatis-plus 3.4.3.3 => 3.4.3.4 bugfix版本</li>
                <li>update redisson 3.16.2 => 3.16.3 bugfix版本</li>
                <li>update easyexcel 2.2.10 => 2.2.11</li>
                <li>update hutool 5.7.11 => 5.7.13</li>
                <li>update file-saver 2.0.4 => 2.0.5</li>
                <li>update dart-sass 1.32.0 => 1.32.13</li>
                <li>update sass-loader 10.1.0 => 10.1.1</li>
                <li>update 优化代码生成 根据MP生成特性 调整导入表结构默认值合理化</li>
                <li>update 将所有 云存储字样 改为 对象存储 避免误解</li>
                <li>update 更新 @Cacheable 错误用法 注意事项</li>
                <li>update 优化 AddressUtils 空校验处理</li>
                <li>update 菜单管理支持配置路由参数</li>
                <li>update 优化aop语法 使用spring自动注入注解</li>
                <li>update 使用 Redisson 限流工具 重写限流实现</li>
                <li>update 使用 vue-data-dict 简化数据字典使用</li>
                <li>update 增加日志注解新增是否保存响应参数开关</li>
                <li>update 用户未登录日志改为 warn 级别</li>
                <li>update OSS模块 关于下载403报错信息优化</li>
                <li>update 更新 Actuator prod 默认暴漏端点 增加暴漏 logfile 日志端点</li>
                <li>update 默认适配jdk11 测试 jdk17 无异常</li>
                <li>update 封装通用下载方法简化下载使用</li>
                <li>add 新增通用方法简化模态/缓存使用</li>
                <li>add 增加 限流演示案例</li>
                <li>add 增加 redis redisson 集群配置</li>
                <li>fix Cron表达式生成器关闭时销毁，避免再次打开时存在上一次修改的数据</li>
                <li>fix 全局限流key会多出一个"-" 将其移动到IP后面 去除多余的空格</li>
                <li>fix 修复多主键代码生成bug</li>
                <li>fix 修复 @Cacheable 与 @DataScope 冲突问题</li>
                <li>fix 修复代码生成页面数据编辑保存之后总是跳转第一页的问题</li>
                <li>remove 移除过期工具 RedisCache</li>
                <li>remove 移除无用配置类 ServerConfig</li>
                <li>remove 移除 SysUser 无用字段 salt</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v3.1.0 - 2021-9-7">
              <ol>
                <li>add [重大改动] 过期 RedisCache 新增 RedisUtils 工具类 新增 发布订阅功能 更灵巧便于使用</li>
                <li>add [重大改动] 新增 saveOrUpdateAll 方法 可完美替代 saveOrUpdateBatch 高性能</li>
                <li>update [重大改动] 重写 InsertAll 方法实现 可完美替代 saveBatch 秒级插入上万数据</li>
                <li>update [重大改动] 更改OSS上传通用路径生成 按照年月日分三级目录</li>
                <li>update [重大改动] MP字段验证策略更改为 NOT_NULL 个别特殊字段使用注解单独处理</li>
                <li>update [重大改动] 所有业务适配 RedisUtils 新工具</li>
                <li>update springboot 2.5.3 => 2.5.4</li>
                <li>update spring-boot-admin 2.5.0 => 2.5.1</li>
                <li>update mybatis-plus 3.4.3 => 3.4.3.3 适配升级 (包含不兼容升级)</li>
                <li>update aliyun.oss 3.13.0 => 3.13.1</li>
                <li>update qcloud.cos 5.6.47 => 5.6.51</li>
                <li>update hutool 5.7.9 => 5.7.11</li>
                <li>update maven-jar-plugin 3.1.1 => 3.2.0</li>
                <li>update feign-okhttp 11.2 => 11.6</li>
                <li>update redisson 3.16.1 => 3.16.2</li>
                <li>add 优化 docker 增加 redis 配置文件</li>
                <li>add 新增暗色菜单风格主题</li>
                <li>add 菜单&部门新增展开/折叠功能</li>
                <li>add 页签右键按钮添加图标 页签新增关闭左侧</li>
                <li>update 优化 OSS 模块与上传组件 异常处理</li>
                <li>update 更新 jackson 配置 支持 LocalDateTime 全局格式化</li>
                <li>update 优化 使用权限工具 获取用户信息</li>
                <li>update 自定义可拖动弹窗宽度指令</li>
                <li>update 重构 将下载excel工具提取到全局</li>
                <li>update 定时任务对检查异常进行事务回滚</li>
                <li>update 优化spy配置文件为 UTF8编码 解决中文注释乱码问题</li>
                <li>update 修改时检查用户数据权限范围</li>
                <li>update 解决 logout 写死 无法扩展路径问题</li>
                <li>update 优化代码生成 导入与同步 批处理效率</li>
                <li>update 修改时检查用户数据权限范围</li>
                <li>update 修改代码生成字典回显样式</li>
                <li>update 修改数据字典回显</li>
                <li>update 优化验证码配置 使用泛型 防止错误输入</li>
                <li>update 优化全局线程池配置 使用泛型 防止错误输入</li>
                <li>update 使用 MP 全局配置分页溢出</li>
                <li>update 代码生成器 导入表时查询 新创建表的优先排序在前面</li>
                <li>update 定时任务支持在线生成cron表达式</li>
                <li>update 自定义弹层溢出滚动样式</li>
                <li>update 优化分页工具排序处理</li>
                <li>update 优化 oss配置 使用发布订阅工具 刷新配置</li>
                <li>update 代码生成 查询数据库列表 按照时间倒序</li>
                <li>update 使用MP自行判断数据库类型</li>
                <li>fix 修复保存配置主题颜色失效问题</li>
                <li>fix 修复 导出雪花id excel失真问题</li>
                <li>fix 修复 druid 监控 集群模式下 无法路由到同一台服务器问题</li>
                <li>fix 解决搜索校验不通过问题</li>
                <li>fix 修复定时器工具编写错误问题</li>
                <li>fix 修复 minio 无 perfix 问题</li>
                <li>fix 修复 富文本图片路径错误问题</li>
                <li>fix 修复 OSS配置清空被过滤问题</li>
                <li>fix 修复 excel 导入与 class 未对应问题</li>
                <li>fix 修复字典组件值为整形不显示问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v3.0.0 - 2021-8-18">
              <ol>
                <li>add [重大更新]重写 OSS 模块相关实现 支持动态配置(页面配置)</li>
                <li>add [重大更新]增加 jackson 超出 JS 最大数值自动转字符串(雪花id序列化)处理</li>
                <li>add [重大更新]重写 防重提交拦截器 支持全局与注解自定义 拦截时间配置配置 优化逻辑</li>
                <li>add [重大更新]新增是否开启用户注册功能</li>
                <li>add [重大更新]增加 easyexcel 工具类</li>
                <li>add [重大更新]集成 性能分析插件 p6spy 更强劲的 SQL 分析</li>
                <li>add [重大更新]增加 完整国际化解决方案</li>
                <li>add [重大更新]支持自定义注解实现接口限流</li>
                <li>update feign-okhttp 11.0 => 11.2</li>
                <li>update okhttp 3.19.4 => 4.9.1</li>
                <li>update minio 8.2.0 => 8.3.0</li>
                <li>update hutool 5.7.6 => 5.7.7</li>
                <li>update element-ui 2.15.2 => 2.15.5</li>
                <li>update springboot admin 2.4.3 => 2.5.0 (新增 Quartz 专属监控页)</li>
                <li>add 增加 admin 监控客户端开关</li>
                <li>add 增加 国际化演示demo</li>
                <li>update 更新软件架构图</li>
                <li>update 优化XSS跨站脚本过滤</li>
                <li>update 优化BLOB下载时清除URL对象引用</li>
                <li>update 更新 防重提交拦截器 demo演示案例</li>
                <li>update 日常字符串校验 统一重构到 StringUtils 便于维护扩展</li>
                <li>update 修改 自动注入器 用户未登录异常拦截抛出警告 返回Null</li>
                <li>update 重构 统一使用 流工具下载</li>
                <li>update 重写 所有业务导出 适配easyexcel工具</li>
                <li>update 移动文件存储业务到 system 模块</li>
                <li>update 代码生成模板 适配新excel导出</li>
                <li>update 将 Actuator 配置 移动到全局配置</li>
                <li>update 统一镜像时区配置 移除主机时间映射</li>
                <li>update 更改多数据源框架更清晰的依赖名</li>
                <li>update 更新 阿里云 maven源 新地址</li>
                <li>update 补全基础实体 文档注解</li>
                <li>update 代码生成文档注解 增加必填判断配置</li>
                <li>update 注入器 insert 增加 update 字段处理</li>
                <li>update 默认首页使用keep-alive缓存</li>
                <li>fix 生产minio回显问题</li>
                <li>fix 修复角色分配用户页面接收参数与传递参数类型不一致导致的错误</li>
                <li>fix 修复代码生成 删除按钮报错 loading 不取消问题</li>
                <li>fix 解决登录后浏览器后台Breadcrumb组件报错</li>
                <li>fix 修复DictUtils方法报错</li>
                <li>fix 头像上传 未走OSS存储问题</li>
                <li>fix oss列表 jpeg 不回显问题</li>
                <li>fix 修复操作日志根据状态查询异常问题</li>
                <li>remove 移除原生excel工具</li>
                <li>remove 移除通用上传下载接口与配置</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.6.0 - 2021-7-28">
              <ol>
                <li>add [重大新增] 增加 OSS 对象存储模块</li>
                <li>remove [重大改动] 删除 自带通用上传 接口 使用OSS模块替换<li>
                <li>update [重大改动] 重写VO转换 支持深拷贝 将VO类抽象到 ServicePlus 泛型处理<li>
                <li>update [重大改动] 多BO合并 使用分组校验 生成BO代码<li>
                <li>update [重大改动] 重构 IServicePlus 功能 增加 BeanCopyUtils 深拷贝工具<li>
                <li>update springboot 2.4.9 => 2.5.3<li>
                <li>update hutool 5.7.4 => 5.7.6<li>
                <li>update minio 8.2.2 => 8.3.0<li>
                <li>update docker plugin 1.2.0 => 1.2.2<li>
                <li>update redisson 3.16.0 => 3.16.1<li>
                <li>update datasource 3.4.0 => 3.4.1<li>
                <li>update element-ui 2.15.2 => 2.15.3<li>
                <li>add 演示Demo增加自定义分页接口案例</li>
                <li>add 角色&菜单新增字段属性提示信息</li>
                <li>update 更新druid配置 独立配置更明显</li>
                <li>update 顶部菜单排除隐藏的默认路由</li>
                <li>update 富文本新增上传文件大小限制</li>
                <li>update 导入用户样式调整</li>
                <li>update 顶部菜单样式调整</li>
                <li>update 密码框新增显示切换密码图标</li>
                <li>update 内链设置meta信息</li>
                <li>update 跳转路由高亮相对应的菜单栏</li>
                <li>fix 修复多数据源druid全局配置缩进错误 引起无效配置问题</li>
                <li>fix 修复定时任务日志执行状态显示</li>
                <li>fix 修复 授权角色空数据问题</li>
                <li>fix 修复 DictData 删除逻辑问题</li>
                <li>fix 修复任意账户越权漏洞</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.5.2 - 2021-7-19">
              <ol>
                <li>update 优化代码生成器注释格式</li>
                <li>fix 回滚代码生成 批处理优化</li>
                <li>fix 代码生成 queryType 重复勾选数据库无默认值问题</li>
                <li>fix 修复接口单参数校验无效问题</li>
                <li>fix 代码生成 queryType >= <= 标识符错误问题</li>
                <li>fix 修复代码生成字典问题</li>
                <li>fix 修复 thread-pool: enabled 配置不生效问题</li>
                <li>remove 删除无用文档与脚本</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.5.1 - 2021-7-13">
              <ol>
                <li>update 验证码开关 转移到表 参数管理 内</li>
                <li>update 使用hutool重构 判断是否url</li>
                <li>fix 修复 docker业务集群部署与文件上传的问题</li>
                <li>fix 修复代码生成同步表结构id冲突问题</li>
                <li>fix 修复代码生成选择字典 无法取消问题</li>
                <li>fix 修复代码生成字典为null问题</li>
                <li>fix 图片上传 多图时无法删除相应图片修复</li>
                <li>remove 删除富文本video事件</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.5.0 - 2021-7-12">
              <ol>
                <li>update springboot 2.4.7 => 2.4.8</li>
                <li>update knife4j 3.0.2 => 3.0.3</li>
                <li>update hutool 5.7.2 => 5.7.4</li>
                <li>update spring-boot-admin 2.4.1 => 2.4.3</li>
                <li>update redisson 3.15.2 => 3.16.0</li>
                <li>add 增加 docker 编排 与 shell 脚本</li>
                <li>add 增加 feign 熔断 自定义结构体解析方法 与 demo 注释</li>
                <li>add 用户管理新增分配角色功能</li>
                <li>add 角色管理新增分配用户功能</li>
                <li>add 增加spring-cache演示案例</li>
                <li>update 独立 springboot-admin 监控到扩展模块项目</li>
                <li>update springboot-admin 监控 增加用户登录权限管理</li>
                <li>update 优化代码生成器 批量导入</li>
                <li>update 优化 增加MP注入异常拦截</li>
                <li>update 关闭默认二级缓存 推荐使用 spring-cache 注解手动缓存</li>
                <li>update FileUpload ImageUpload组件 支持多图片上传</li>
                <li>update 优化中英文语言配置</li>
                <li>update 规范maven写法</li>
                <li>fix redis获取map属性bug修复。</li>
                <li>fix 修复 按钮loading 后端500卡死问题</li>
                <li>fix 相对路径下载问题</li>
                <li>fix 修复 hutool 工具返回结果不一致问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.4.0 - 2021-6-24">
              <ol>
                <li>update springboot 2.3.11 => 2.4.7</li>
                <li>update springboot-admin 2.3.1 => 2.4.1</li>
                <li>update feign 2.2.6 => 3.0.3</li>
                <li>update hutool 5.6.7 => 5.7.2</li>
                <li>update 多数据源替换成dynamic-datasource</li>
                <li>update 适配 jdk11</li>
                <li>update 集成 Lock4j 分布式锁</li>
                <li>update 移除 fastjson 增加 jackson 工具类 重写相关业务</li>
                <li>update 优化 异步工厂重写 使用 spring 异步处理</li>
                <li>update 全局挂载字典标签组件</li>
                <li>update 日志列表支持排序操作</li>
                <li>update 更新 feign demo 更清晰的用法</li>
                <li>update 更新多数据源演示案例</li>
                <li>add 增加 ServicePlusImpl 自动以实现类 重写移除事务注解方法 防止多数据源失效</li>
                <li>add 增加 自定义 批量insert方法</li>
                <li>add 增加 Swagger3 用法示例</li>
                <li>fix 修复地址ip地址特殊回环问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.3.2 - 2021-6-11">
              <ol>
                <li>add redis锁工具类编写</li>
                <li>update spring-cache 整合 redisson</li>
                <li>update MybatisPlus整合Redis二级缓存</li>
                <li>update swagger 升级为 3.0.0 使用 OAS_30 协议</li>
                <li>update 优化 代码生成器 增加表单防重注解</li>
                <li>update 优化 锁切面代码 key到常量类</li>
                <li>fix 修复相对路径上传异常问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.3.1 - 2021-6-4">
              <ol>
                <li>add 增加 redisson 分布式锁 注解与demo案例</li>
                <li>add 增加 Oracle 分支</li>
                <li>update 优化 redis 空密码兼容性</li>
                <li>update 优化前端代码生成按钮增加 loading</li>
                <li>fix 修复 redisson 不能批量删除的bug</li>
                <li>fix 修复表单构建选择下拉选择控制台报错问题</li>
                <li>fix 修复 vo 代码生成 主键列表显示 重复生成bug</li>
                <li>fix 修复上传路径 win 打包编译为 win 路径, linux 报错bug</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.3.0 - 2021-6-1">
              <ol>
                <li>add 升级 luttuce 为 redisson 性能更强 工具更全</li>
                <li>add 增加测试数据sql文件</li>
                <li>add 增加demo模块 单表演示案例(包含数据权限)</li>
                <li>update 完美修复 数据权限功能(支持单表多表过滤)</li>
                <li>update 优化代码生成模板</li>
                <li>update 优化 system 模块 批量操作性能</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.2.1 - 2021-5-29">
              <ol>
                <li>add 增加 security 权限框架 @Async 异步注解配置</li>
                <li>update 优化dataScope参数防止注入</li>
                <li>update 优化参数&字典缓存操作</li>
                <li>update 增加修改包名文档</li>
                <li>update 文档增加演示图例</li>
                <li>fix 修复部门类sql符号错误</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.2.0 - 2021-5-25">
              <ol>
                <li>同步升级 RuoYi-Vue 3.5.0</li>
                <li>add 增加验证码开关</li>
                <li>add 新增IE浏览器版本过低提示页面</li>
                <li>update 升级druid到最新版本v1.2.6</li>
                <li>update 升级fastjson到最新版1.2.76</li>
                <li>update 修改bo加入判断是否设置必填再加载必填注解</li>
                <li>update 生成vue模板导出按钮点击后添加遮罩</li>
                <li>update Redis设置HashKey序列化</li>
                <li>update 优化Redis序列化配置</li>
                <li>fix 修复代码生成器中表字段取消必填无法更新问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.1.2 - 2021-5-21">
              <ol>
                <li>update springboot 升级 2.3.11</li>
                <li>update mybatis-plus 升级 3.4.3 分页Plus对象适配更新</li>
                <li>update 验证码生成更新为无符号整数计算</li>
                <li>update 请求响应对象 与 分页对象 结构修改 适配接口文档配置</li>
                <li>update swagger增加请求前缀</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.1.1 - 2021-5-19">
              <ol>
                <li>update 配置统一提取为 properties 配置类</li>
                <li>update 分页工具 删除过期方法</li>
                <li>update admin 实时监控日志 改为保留一天</li>
                <li>fix 修复swagger开关无法控制关闭问题</li>
                <li>fix maven install 异常</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.1.0 - 2021-5-17">
              <ol>
                <li>update knife4j升级3.0.2</li>
                <li>update 增强分页工具兼容性</li>
                <li>update 通用Service接口 增加自定义vo转换函数</li>
                <li>remove 移除ruoyi自带服务监控(Admin已全部包含)</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v2.0.0 - 2021-5-15">
              <ol>
                <li>springboot 升级 2.3.10 依赖全面升级适配</li>
                <li>add 增加分页工具</li>
                <li>add 增加 增强Mapper 与 增强Service 重写业务适配</li>
                <li>add 代码生成器 增加校验注解</li>
                <li>update 代码生成器修改为MP分页</li>
                <li>update 使用 MP 分页工具 重构业务</li>
                <li>update 重写文档介绍</li>
                <li>remove 移除 pagehelper 分页工具</li>
                <li>fix 修复代码生成 数据权限问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v1.0.2 - 2021-5-13">
              <ol>
                <li>update 更新整合打包文档 重新排版</li>
                <li>fix vue与boot整合打包与admin页面路由冲突</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v1.0.1 - 2021-5-11">
              <ol>
                <li>update 更新banner</li>
                <li>update 配置转移到 yml 文件 统一管理</li>
                <li>update 上传媒体类型添加视频格式</li>
                <li>update 树级结构更新子节点使用replaceFirst</li>
                <li>update 删除操作日志记录日志</li>
                <li>fix 修正导入表权限标识</li>
                <li>fix 文件上传时报错</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v1.0.0 - 2021-5-10">
              <ol>
                <li>RuoYi-Vue-Plus 后台管理系统正式发布</li>
                <li>ORM框架 使用 Mybatis-Plus 3.4.2 简化CRUD</li>
                <li>Bean简化 使用 Lombok 简化 get set toString 等等</li>
                <li>容器改动 Tomcat 改为 并发性能更好的 undertow</li>
                <li>所有原生功能使用 Mybatis-Plus 与 Lombok 重写</li>
                <li>代码生成模板 改为适配 Mybatis-Plus 的代码</li>
                <li>代码生成模板 拆分出Vo,QueryBo,AddBo,EditBo等领域对象</li>
                <li>项目修改为 maven多环境配置</li>
                <li>swagger 修改为 knife4j</li>
                <li>集成 Hutool 5.X 并重写RuoYi部分功能</li>
                <li>集成 Feign 接口化管理 Http 请求(如三方请求 支付,短信,推送等)</li>
                <li>集成 spring-boot-admin 全方位监控</li>
                <li>增加demo模块示例(给不会增加模块的小伙伴做参考)</li>
              </ol>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "4.0.0",
    };
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    },
  },
};
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}
</style>

