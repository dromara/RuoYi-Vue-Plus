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
          * 容器框架 Undertow 基于 XNIO 的高性能容器<br/>
          * 权限认证框架 Sa-Token、Jwt 支持多终端认证系统<br/>
          * 关系数据库 MySQL 适配 8.X 最低 5.7<br/>
          * 关系数据库 Oracle 适配 11g 12c<br/>
          * 关系数据库 PostgreSQL 适配 13 14<br/>
          * 关系数据库 SQLServer 适配 2017 2019<br/>
          * 缓存数据库 Redis 适配 6.X 最低 4.X<br/>
          * 数据库框架 Mybatis-Plus 快速 CRUD 增加开发效率<br/>
          * 数据库框架 p6spy 更强劲的 SQL 分析<br/>
          * 多数据源框架 dynamic-datasource 支持主从与多种类数据库异构<br/>
          * 序列化框架 Jackson 统一使用 jackson 高效可靠<br/>
          * Redis客户端 Redisson 性能强劲、API丰富<br/>
          * 分布式限流 Redisson 全局、请求IP、集群ID 多种限流<br/>
          * 分布式锁 Lock4j 注解锁、工具锁 多种多样<br/>
          * 分布式幂等 Redisson 拦截重复提交<br/>
          * 分布式日志 TLog 支持跟踪链路日志记录、性能分析、链路排查<br/>
          * 分布式任务调度 Xxl-Job 高性能 高可靠 易扩展<br/>
          * 分布式文件存储 Minio 本地存储<br/>
          * 分布式云存储 七牛、阿里、腾讯 云存储<br/>
          * 监控框架 SpringBoot-Admin 全方位服务监控<br/>
          * 校验框架 Validation 增强接口安全性 严谨性<br/>
          * Excel框架 Alibaba EasyExcel 性能优异 扩展性强<br/>
          * 文档框架 SpringDoc、javadoc 无注解零入侵基于java注释<br/>
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
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/pages?sort_id=4106467&doc_id=1469725')"
          >更新日志</el-button
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
              <li>Sa-Token</li>
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
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "4.3.0",
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

