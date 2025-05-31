# PowerShell中文乱码解决方案

## 问题描述

在Windows系统中启动RuoYi-Vue-Plus项目时，控制台输出的中文日志可能出现乱码问题，表现为：
- Sa-Token等框架组件的中文提示显示为乱码
- 部分应用日志中文字符显示异常
- 控制台输出编码不统一

## 产生原因

1. **JVM默认字符编码问题**：JVM启动时没有指定正确的字符编码
2. **PowerShell控制台编码问题**：PowerShell默认编码与应用输出编码不一致
3. **系统语言设置问题**：系统区域设置与应用期望的编码不匹配

## 解决方案

### 方案一：使用优化的启动脚本（推荐）

1. **使用提供的启动脚本**：
   ```powershell
   .\start-dev-utf8.ps1
   ```

2. **脚本主要优化内容**：
   - 设置PowerShell控制台UTF-8编码
   - 配置JVM字符编码参数
   - 设置系统语言和时区
   - 优化内存和GC参数

### 方案二：手动设置JVM参数

在启动应用时添加以下JVM参数：

```bash
-Dfile.encoding=UTF-8 
-Dconsole.encoding=UTF-8 
-Duser.language=zh 
-Duser.country=CN 
-Duser.timezone=Asia/Shanghai
```

**Maven启动示例**：
```powershell
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Duser.language=zh -Duser.country=CN"
mvn spring-boot:run -pl ruoyi-admin -Dspring-boot.run.profiles=dev
```

**直接Java启动示例**：
```powershell
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Duser.language=zh -Duser.country=CN -jar ruoyi-admin.jar
```

### 方案三：IntelliJ IDEA配置

1. **设置Run Configuration JVM参数**：
   - 打开 `Run/Debug Configurations`
   - 在 `VM options` 中添加：
     ```
     -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Duser.language=zh -Duser.country=CN -Duser.timezone=Asia/Shanghai
     ```

2. **全局IDEA设置**：
   - 打开 `Help` -> `Edit Custom VM Options`
   - 添加以下参数：
     ```
     -Dfile.encoding=UTF-8
     -Dconsole.encoding=UTF-8
     ```

3. **项目编码设置**：
   - `File` -> `Settings` -> `Editor` -> `File Encodings`
   - 设置所有编码为 `UTF-8`

### 方案四：系统级解决方案

1. **Windows系统设置**：
   - 打开 `控制面板` -> `区域` -> `管理` -> `更改系统区域设置`
   - 勾选 `Beta版：使用Unicode UTF-8提供全球语言支持`
   - 重启系统

2. **PowerShell配置文件设置**：
   ```powershell
   # 在PowerShell配置文件中添加以下内容
   [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
   $OutputEncoding = [console]::InputEncoding = [console]::OutputEncoding = New-Object System.Text.UTF8Encoding
   ```

## 验证解决方案

启动应用后，观察以下内容应正确显示中文：

1. **PMS模块初始化信息**：
   ```
   ===============================================
   ========== PMS民宿管理模块初始化完成 ==========
   ========== 包名: org.dromara.pms        ==========
   ========== 版本: 1.0.0              ==========
   ===============================================
   ```

2. **Sa-Token认证提示**：
   ```
   请求地址'/pms/test/database',认证失败'未能读取到有效token',无法访问系统资源
   ```

3. **其他框架组件的中文提示应正常显示**

## 常见问题

### Q1: 脚本执行策略限制
**问题**：PowerShell提示无法执行脚本
**解决**：
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Q2: 部分字符仍然乱码
**问题**：某些特定组件的中文仍有乱码
**解决**：
1. 检查日志框架配置文件编码
2. 确认相关jar包内部编码设置
3. 添加更多JVM编码参数：
   ```
   -Djava.awt.headless=true
   -Dsun.jnu.encoding=UTF-8
   -Djava.io.tmpdir.encoding=UTF-8
   ```

### Q3: IDEA控制台乱码
**问题**：在IDEA中运行仍有乱码
**解决**：
1. `Help` -> `Edit Custom VM Options` 添加编码参数
2. `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Maven` -> `Runner` 设置VM Options
3. 重启IDEA

## 最佳实践建议

1. **统一使用UTF-8编码**：项目、IDE、系统均设置为UTF-8
2. **使用提供的启动脚本**：避免每次手动设置参数
3. **团队开发规范**：建立统一的开发环境配置文档
4. **持续验证**：定期检查新加入的组件是否支持UTF-8

## 参数说明

| 参数                            | 说明             | 作用               |
| ------------------------------- | ---------------- | ------------------ |
| `-Dfile.encoding=UTF-8`         | 设置文件默认编码 | 影响文件读写编码   |
| `-Dconsole.encoding=UTF-8`      | 设置控制台编码   | 影响控制台输出编码 |
| `-Duser.language=zh`            | 设置用户语言     | 影响本地化消息显示 |
| `-Duser.country=CN`             | 设置用户国家     | 影响地区特定格式   |
| `-Duser.timezone=Asia/Shanghai` | 设置时区         | 统一时间显示格式   |

使用这些解决方案后，中文乱码问题应该得到完全解决。 