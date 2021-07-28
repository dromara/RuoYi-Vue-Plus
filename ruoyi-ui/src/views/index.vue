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
          * 多数据源框架 dynamic-datasource 支持主从与多种类数据库异构<br/>
          * Redis客户端 采用 Redisson 性能更强<br/>
          * 分布式锁 Lock4j 注解锁、工具锁 多种多样<br/>
          * 部署方式 Docker 容器编排 一键部署业务集群<br/>
          * 文件存储 OSS 对象存储模块 支持(Minio、七牛、阿里、腾讯)<br/>
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

