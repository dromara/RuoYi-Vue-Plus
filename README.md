## 平台简介
[![码云Gitee](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/badge/star.svg?theme=blue)](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus)
[![GitHub](https://img.shields.io/github/stars/JavaLionLi/RuoYi-Vue-Plus.svg?style=social&label=Stars)](https://github.com/JavaLionLi/RuoYi-Vue-Plus)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/blob/master/LICENSE)
[![使用IntelliJ IDEA开发维护](https://img.shields.io/badge/IntelliJ%20IDEA-提供支持-blue.svg)](https://www.jetbrains.com/?from=RuoYi-Vue-Plus)
<br>
[![RuoYi-Vue-Plus](https://img.shields.io/badge/RuoYi_Vue_Plus-2.6.0-success.svg)](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4-blue.svg)]()
[![JDK-8+](https://img.shields.io/badge/JDK-8+-green.svg)]()
[![JDK-11](https://img.shields.io/badge/JDK-11-green.svg)]()

RuoYi-Vue-Plus 是基于 RuoYi-Vue 针对 `分布式集群` 场景升级 定期与 RuoYi-Vue 同步

集成 Lock4j dynamic-datasource OSS存储 等分布式场景解决方案

集成 Mybatis-Plus Lombok Hutool 等便捷开发工具 适配重写相关业务 便于开发 

* 前端开发框架 Vue、Element UI
* 后端开发框架 Spring Boot、Redis
* 容器框架 Undertow 基于 Netty 的高性能容器
* 权限认证框架 Spring Security、Jwt，支持多终端认证系统
* 关系数据库 MySQL 适配 8.X 
* 缓存数据库 Redis 适配 6.X
* 数据库开发框架 Mybatis-Plus 快速 CRUD 增加开发效率 插件化支持各类需求
* 网络框架 Feign、OkHttp3 接口化管理 HTTP 请求
* 工具类框架 Hutool、Lombok 减少代码冗余 增加安全性
* 监控框架 spring-boot-admin 全方位服务监控
* 校验框架 validation 增强接口安全性 严谨性
* 文档框架 knife4j 美化接口文档
* 序列化框架 统一使用 jackson 高效可靠
* 代码生成器 一键生成前后端代码
* 多数据源框架 dynamic-datasource 支持主从与多种类数据库异构
* Redis客户端 采用 Redisson 性能更强
* 分布式锁 Lock4j 注解锁、工具锁 多种多样
* 部署方式 Docker 容器编排 一键部署业务集群
* 文件存储 OSS 对象存储模块 支持(Minio、七牛、阿里、腾讯)

## 参考文档

使用框架前请仔细阅读文档重点注意事项
<br>
>[初始化项目 必看](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/关于初始化项目?sort_id=4164117)
> 
>[部署项目 必看](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/关于应用部署?sort_id=4219382)
> 
>[参考文档 Wiki](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/pages)

## 提问四部曲
### 一、查阅wiki
优先在`wiki->重点事项`，查找是否有相关问题及解决方案，尤其是框架更新后产生的问题，多会在wiki中提及

> [参考文档 Wiki](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/pages)

### 二、借助issues
尝试issues中搜索问题关键字（记得选择已完成），看看是否有其他人提出相同问题
- `如果有`那么依据评论中的解决方案自行尝试解决
- `如果没有`那么提交一个新的issues描述清楚你的问题，需要包含以下内容（优质的issues，能够帮助作者更高效的帮你解决问题）：
    - 出现问题的模块或功能或类，总之你要说清楚在哪出的问题
    - 描述产生问题的相关操作流程，以便复现快速解决
    - 报错的日志截图，一定是截图，不要复制一堆报错的文本
> [issues](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/issues)

### 三、百度
大家都懂，不多描述，将关键的报错信息CC->CV到百度中看看大佬们怎么解决的
> [百度](http://www.baidu.com)

### 四、加群
以上三点已经能解决大家绝大部分问题了，如果还有问题没能通过这几种方式解决，那么加群，大家一起在群里探讨一下

## 贡献代码

欢迎各路英雄豪杰 `PR` 代码 请提交到 `dev` 开发分支 统一测试发版

框架定位为 `通用后台管理系统(分布式集群强化)` 原则上不接受业务 `PR` 

## 修改RuoYi功能
### 依赖改动

* ORM框架 使用 Mybatis-Plus 简化CRUD (不支持主子表)
* Bean简化 使用 Lombok 简化 get set toString 等等
* 容器改动 Tomcat 改为 并发性能更好的 undertow
* 分页移除 pagehelper 改为 Mybatis-Plus 分页
* 升级 swagger 为 knife4j
* 集成 Hutool 5.X 并重写RuoYi部分功能
* 集成 Feign 接口化管理 Http 请求(如三方请求 支付,短信,推送等)
* 移除 自带服务监控 改为 spring-boot-admin 全方位监控
* 增加 demo 模块示例(给不会增加模块的小伙伴做参考)
* 增加 redisson 高性能 Redis 客户端
* 移除 fastjson 统一使用 jackson 序列化
* 集成 dynamic-datasource 多数据源(默认支持MySQL,其他种类需自行适配)
* 集成 Lock4j 实现分布式 注解锁、工具锁 多种多样
* 增加 Docker 容器编排 打包插件与部署脚本
* 移除 本地文件上传 改为 OSS对象存储 支持(Minio、七牛、阿里、腾讯)

### 代码改动

* 所有原生功能使用 Mybatis-Plus 与 Lombok 重写
* 增加 IServicePlus 与 BaseMapperPlus 可自定义通用方法
* 代码生成模板 改为适配 Mybatis-Plus 的代码
* 代码生成模板 根据 Alibaba 代码规约 拆分出 VO、BO 等领域对象
* 代码生成模板 增加 文档注解 与 校验注解 简化通用操作
* 项目修改为 maven多环境配置
* 项目配置修改为 application.yml 统一管理
* 数据权限修改为 适配支持单表、多表
* 使用 redisson 实现 spring-cache 整合
* 增加 mybatis-plus 二级缓存 redis 存储

### 其他

* 同步升级 RuoYi-Vue
* GitHub 地址 [RuoYi-Vue-Plus-github](https://github.com/JavaLionLi/RuoYi-Vue-Plus)
* 单模块 fast 分支 [RuoYi-Vue-Plus-fast](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/tree/fast/)
* Oracle 模块 oracle 分支 [RuoYi-Vue-Plus-oracle](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/tree/oracle/)

## 扫码加群 一起交流
![输入图片说明](https://images.gitee.com/uploads/images/2021/0625/160026_11d949aa_1766278.jpeg "07f7121fab14e57e03e5f6a35eff6ce.jpg")

## 捐献作者
作者为兼职做开源,平时还需要工作,如果帮到了您可以请作者吃个盒饭  
<img src="https://images.gitee.com/uploads/images/2021/0525/101654_451e4523_1766278.jpeg" width="300px" height="450px" />
<img src="https://images.gitee.com/uploads/images/2021/0525/101713_3d18b119_1766278.jpeg" width="300px" height="450px" />

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

## 演示图例

<table border="1" cellpadding="1" cellspacing="1" style="width:500px">
	<tbody>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-972235bcbe3518dedd351ff0e2ee7d1031c.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-5e0097702fa91e2e36391de8127676a7fa1.png" width="1920" /></td>
		</tr>
		<tr>
			<td>
			<p><img src="https://oscimg.oschina.net/oscnet/up-e56e3828f48cd9886d88731766f06d5f3c1.png" width="1920" /></p>
			</td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-0715990ea1a9f254ec2138fcd063c1f556a.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-eaf5417ccf921bb64abb959e3d8e290467f.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-fc285cf33095ebf8318de6999af0f473861.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-60c83fd8bd61c29df6dbf47c88355e9c272.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-7f731948c8b73c7d90f67f9e1c7a534d5c3.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-e4de89b5e2d20c52d3c3a47f9eb88eb8526.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-8791d823a508eb90e67c604f36f57491a67.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-4589afd99982ead331785299b894174feb6.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-8ea177cdacaea20995daf2f596b15232561.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-32d1d04c55c11f74c9129fbbc58399728c4.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-04fa118f7631b7ae6fd72299ca0a1430a63.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-fe7e85b65827802bfaadf3acd42568b58c7.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-eff2b02a54f8188022d8498cfe6af6fcc06.png" width="1920" /></td>
		</tr>
	</tbody>
</table>

## 在线体验

- admin/admin123  
- 陆陆续续收到一些打赏，为了更好的体验已用于演示服务器升级。谢谢各位小伙伴。

演示地址：http://vue.ruoyi.vip  
文档地址：http://doc.ruoyi.vip
