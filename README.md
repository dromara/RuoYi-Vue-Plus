## 重点注意事项

若依文档对事务注解的描述 [关于事务](https://doc.ruoyi.vip/ruoyi/document/htsc.html#%E4%BA%8B%E5%8A%A1%E7%AE%A1%E7%90%86)  以下对多数据源事务做补充:
* 同一个事务下是无法切换数据源的
* 禁止 父方法使用 @Transactional 创建事务 子方法使用 @DataSource 切换数据源
* 正确用法: 子方法单独创建事务 或 父方法使用 @Transactional(propagation = Propagation.REQUIRES_NEW) 为所有子方法创建新事务

关于如何使用Tomcat
* 查看ruoyi-framework模块的pom.xml文件,根据注释更改依赖
* 查看ruoyi-admin模块中的application.yml文件,根据注释更改配置

关于如何创建新模块
* 参考ruoyi-demo模块
* 需要改动: 父pom 与 admin模块pom

## 修改RuoYi功能

* ORM框架 使用 Mybatis-Plus 简化CRUD (目前支持单表生成 不支持树表与主子表)
* Bean简化 使用 Lombok 简化 get set toString 等等
* 容器改动 Tomcat 改为 并发性能更好的 undertow
* 代码生成模板 改为适配 Mybatis-Plus 的代码
* 项目修改为 maven多环境配置
* 升级MybatisPlus 3.4.2
* 增加demo模块示例(给不会增加模块的小伙伴做参考)
* 同步升级 3.3

## 平台简介

若依是一套全部开源的快速开发平台，毫无保留给个人及企业免费使用。

* 前端采用Vue、Element UI。
* 后端采用Spring Boot、Spring Security、Redis & Jwt。
* 权限认证使用Jwt，支持多终端认证系统。
* 支持加载动态权限菜单，多方式轻松权限控制。
* 高效率开发，使用代码生成器可以一键生成前后端代码。
* 提供了单应用版本[RuoYi-Vue-fast](https://github.com/yangzongzhuan/RuoYi-Vue-fast)，Oracle版本[RuoYi-Vue-Oracle](https://github.com/yangzongzhuan/RuoYi-Vue-Oracle)，保持同步更新。
* 不分离版本，请移步[RuoYi](https://gitee.com/y_project/RuoYi)，微服务版本，请移步[RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud)

## 内置功能

1.  用户管理：用户是系统操作者，该功能主要完成系统用户配置。
2.  部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
3.  岗位管理：配置系统用户所属担任职务。
4.  菜单管理：配置系统菜单，操作权限，按钮权限标识等。
5.  角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
6.  字典管理：对系统中经常使用的一些较为固定的数据进行维护。
7.  参数管理：对系统动态配置常用参数。
8.  通知公告：系统通知公告信息发布维护。
9.  操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
10. 登录日志：系统登录日志记录查询包含登录异常。
11. 在线用户：当前系统中活跃用户状态监控。
12. 定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
13. 代码生成：前后端代码的生成（java、html、xml、sql）支持CRUD下载 。
14. 系统接口：根据业务代码自动生成相关的api接口文档。
15. 服务监控：监视当前系统CPU、内存、磁盘、堆栈等相关信息。
16. 缓存监控：对系统的缓存信息查询，命令统计等。
17. 在线构建器：拖动表单元素生成相应的HTML代码。
18. 连接池监视：监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。

## 在线体验

- admin/admin123  
- 陆陆续续收到一些打赏，为了更好的体验已用于演示服务器升级。谢谢各位小伙伴。

演示地址：http://vue.ruoyi.vip  
文档地址：http://doc.ruoyi.vip
