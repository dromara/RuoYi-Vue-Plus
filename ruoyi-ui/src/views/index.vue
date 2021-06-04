<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>RuoYi-Vue-Plus后台管理框架</h2>
        <p>
          基于 RuoYi-Vue 集成 Mybatis-Plus Lombok Hutool 等便捷开发工具 适配重写相关业务 便于开发 定期与 RuoYi-Vue 同步
          <br/>
          * 前端开发框架 Vue、Element UI<br/>
          * 后端开发框架 Spring Boot、Redis<br/>
          * 容器框架 Undertow 基于 Netty 的高性能容器<br/>
          * 权限认证框架 Spring Security、Jwt，支持多终端认证系统<br/>
          * 关系数据库 MySQL 适配 8.X<br/>
          * 缓存数据库 Redis 适配 6.X<br/>
          * 数据库开发框架 Mybatis-Plus 快速 CRUD 增加开发效率 插件化支持各类需求<br/>
          * 网络框架 Feign、OkHttp3 接口化管理 HTTP 请求<br/>
          * 工具类框架 Hutool、Lombok 减少代码冗余 增加安全性<br/>
          * 监控框架 spring-boot-admin 全方位服务监控<br/>
          * 校验框架 validation 增强接口安全性 严谨性<br/>
          * 文档框架 knife4j 美化接口文档<br/>
          * 代码生成器 一键生成前后端代码<br/>
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
              <li>Fastjson</li>
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
import config from '../../package.json'

export default {
  name: "index",
  data() {
    return {
      // 版本号
      version: config.version,
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

