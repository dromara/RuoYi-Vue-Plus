## 修改RuoYi功能

* ORM框架 使用 Mybatis-Plus 简化CRUD
* Bean简化 使用 Lombok 简化 get set toString 等等
* 容器改动 Tomcat 改为 并发性能更好的 undertow
* 代码生成模板 改为适配 Mybatis-Plus 的代码

## 平台简介

* 前端采用Vue、Element UI。
* 后端采用Spring Boot、Spring Security、Redis & Jwt。
* 权限认证使用Jwt，支持多终端认证系统。
* 支持加载动态权限菜单，多方式轻松权限控制。
* 高效率开发，使用代码生成器可以一键生成前后端代码。
* 感谢[Vue-Element-Admin](https://github.com/PanJiaChen/vue-element-admin)，[eladmin-web](https://gitee.com/elunez/eladmin-web?_from=gitee_search)。
* 不分离版本，请移步[RuoYi](https://gitee.com/y_project/RuoYi)，微服务版本，请移步[RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud)
* 阿里云优惠券：[点我进入](https://www.aliyun.com/minisite/goods?userCode=brki8iof&share_source=copy_link)，腾讯云优惠券：[点我领取](https://cloud.tencent.com/redirect.php?redirect=1025&cps_key=198c8df2ed259157187173bc7f4f32fd&from=console)&nbsp;&nbsp;

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
16. 在线构建器：拖动表单元素生成相应的HTML代码。
17. 连接池监视：监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。

## 在线体验

- admin/admin123  
- 陆陆续续收到一些打赏，为了更好的体验已用于演示服务器升级。谢谢各位小伙伴。

演示地址：http://vue.ruoyi.vip  
文档地址：http://doc.ruoyi.vip

## 演示图

<table>
    <tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102616_d94ada2f_1766278.jpeg"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_016f5bcf_1766278.jpeg"/></td>
    </tr>
    <tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102616_f479788d_1766278.jpeg"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102616_6ec5b078_1766278.jpeg"/></td>
    </tr>
    <tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102616_57a03817_1766278.jpeg"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102616_1077f0e1_1766278.jpeg"/></td>
    </tr>
	<tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_117a21e0_1766278.jpeg"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_4086e6f3_1766278.jpeg"/></td>
    </tr>	 
    <tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_b3c6d70e_1766278.jpeg"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_bdcfa03d_1766278.jpeg"/></td>
    </tr>
	<tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_ad83b775_1766278.jpeg"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102618_ed9e3ba4_1766278.jpeg"/></td>
    </tr>
	<tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_3033dd06_1766278.png"/></td>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_7597a8de_1766278.png"/></td>
    </tr>
    <tr>
        <td><img src="https://images.gitee.com/uploads/images/2020/0523/102617_58ad38a4_1766278.jpeg"/></td>
        <td><img src="https://oscimg.oschina.net/oscnet/up-6d73c2140ce694e3de4c05035fdc1868d4c.png"/></td>
    </tr>
</table>


## 若依前后端分离交流群

QQ群： [![加入QQ群](https://img.shields.io/badge/已满-937441-blue.svg)](https://jq.qq.com/?_wv=1027&k=5bVB1og) [![加入QQ群](https://img.shields.io/badge/已满-887144332-blue.svg)](https://jq.qq.com/?_wv=1027&k=5eiA4DH) [![加入QQ群](https://img.shields.io/badge/已满-180251782-blue.svg)](https://jq.qq.com/?_wv=1027&k=5AxMKlC) [![加入QQ群](https://img.shields.io/badge/104180207-blue.svg)](https://jq.qq.com/?_wv=1027&k=51G72yr) 点击按钮入群。
