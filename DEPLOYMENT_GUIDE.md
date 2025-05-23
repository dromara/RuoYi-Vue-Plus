# RuoYi-Vue-Plus 系统部署指南

## 一、环境准备
1. **JDK**：需要 JDK 17 或 JDK 21
2. **数据库**：支持 MySQL/Oracle/PostgreSQL/SQLServer
3. **Redis**：5.0+ 版本
4. **Maven**：3.6+ 版本
5. **Node.js**：前端需要（建议 v18+）

## 二、后端配置部署
1. **核心配置文件**：
   - `ruoyi-admin/src/main/resources/application.yml` - 主配置文件
   - `application-dev.yml`/`application-prod.yml` - 环境配置

2. **关键配置项**：
   ```yaml
   # 数据库配置
   spring.datasource:
     url: jdbc:mysql://localhost:3306/ry-vue-plus
     username: root
     password: 123456
     driver-class-name: com.mysql.cj.jdbc.Driver
   
   # Redis配置
   spring.redis:
     host: 127.0.0.1
     port: 6379
     password: ruoyi123
   ```

3. **数据库初始化**：
   - 执行对应数据库的SQL脚本（位于`script/sql/`目录）
   - 例如MySQL：执行`mysql_ry_vue_5.X.sql`

4. **启动方式**：
必须启动基础建设: mysql redis admin
可选启动基础建设: minio(影响文件上传) monitor(影响监控) snailjob(影响定时任务)
MonitorAdminApplication 为 Admin监控服务(非必要 可参考对应文档关闭 搭建Admin监控)
SnailJobServerApplication 为 任务调度中心服务(非必要 可参考对应文档关闭 搭建调度中心)
DromaraApplication 为 主应用服务
需优先启动 MonitorAdminApplication 与 SnailJobServerApplication 具体配置方式参考对应文档
最后启动 主服务 DromaraApplication
工作流相关初始化使用 工作流初始化

## 三、前端部署
克隆仓库
git clone https://gitee.com/xlsea/ruoyi-plus-soybean.git
cd ruoyi-plus-soybean
安装 pnpm (如果未安装)
npm install pnpm -g
设置淘宝镜像

pnpm config set registry https://registry.npmmirror.com
安装依赖
pnpm install
运行开发服务器
pnpm dev
构建生产版本
pnpm build

## 四、Docker部署（可选）
1. **使用docker-compose**：
   ```bash
   cd script/docker
   docker-compose up -d
   ```
   包含的服务：
   - Redis
   - MinIO
   - RuoYi主服务
   - 监控中心

2. **独立容器运行**：
   ```bash
   docker run -d -p 8080:8080 ruoyi/ruoyi-server:5.3.1
   ```

## 五、监控中心
1. **监控服务**：
   - 访问地址：`http://localhost:9090/admin`
   - 默认账号：ruoyi/123456

2. **任务调度中心**：
   - 访问地址：`http://localhost:8800`
   - 配置见`application-dev.yml`中的`snail-job`部分

## 六、注意事项
1. 首次启动会自动初始化系统数据
2. 默认管理员账号：admin/admin123
3. 生产环境建议：
   - 修改默认密码
   - 关闭Swagger（设置`springdoc.swagger-ui.enabled=false`）
   - 配置HTTPS

完整文档参考：[RuoYi-Vue-Plus文档](https://plus-doc.dromara.org)

ruoyi-vue-plus/
├── ruoyi-admin/          # 后台核心模块
├── ruoyi-common/         # 公共模块库
├── ruoyi-modules/        # 业务模块
├── ruoyi-extend/         # 扩展模块
├── ruoyi-plus-soybean/   # 前端Vue3项目
├── script/               # 部署脚本
├── target/               # 编译输出
├── pom.xml               # Maven父工程配置
└── README.md             # 项目说明 