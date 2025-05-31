# PMS模块编译优化配置指南

## 概述

为了提高开发效率，我们优化了PMS模块的编译流程，现在可以通过快捷键快速编译PMS模块，而无需编译整个项目。

## 编译脚本说明

### 1. pms-compile.ps1 - 基础快速编译

**功能特点：**
- 只编译 `ruoyi-pms` 模块及其必要依赖
- 跳过其他业务模块（demo、generator、job、system、workflow）
- 支持并行编译，提高编译速度
- 默认跳过测试，加快编译过程

**使用方法：**
```powershell
# 基础编译
.\pms-compile.ps1

# 清理后编译
.\pms-compile.ps1 -Clean

# 包含测试编译
.\pms-compile.ps1 -SkipTests:$false

# 离线编译
.\pms-compile.ps1 -Offline

# 详细输出
.\pms-compile.ps1 -Quiet:$false
```

### 2. pms-smart-compile.ps1 - 智能增量编译

**功能特点：**
- 支持增量编译，只在文件有变化时才重新编译
- 自动检测源码文件变化（.java、.xml、.properties）
- 维护编译时间戳，避免不必要的重复编译
- 更智能的编译策略

**使用方法：**
```powershell
# 智能编译（推荐）
.\pms-smart-compile.ps1

# 强制重新编译
.\pms-smart-compile.ps1 -Force

# 清理后编译
.\pms-smart-compile.ps1 -Clean

# 详细输出模式
.\pms-smart-compile.ps1 -Verbose
```

## VSCode快捷键配置

### 当前配置的快捷键

| 快捷键         | 功能         | 说明                    |
| -------------- | ------------ | ----------------------- |
| `Ctrl+Alt+B`   | PMS快速编译  | 执行 pms-compile.ps1    |
| `Ctrl+Shift+B` | 完整项目编译 | 编译整个项目            |
| `Ctrl+Shift+P` | PMS模块编译  | Maven原生PMS编译        |
| `Ctrl+F5`      | 启动应用     | 运行Spring Boot应用     |
| `Ctrl+Alt+S`   | 开发环境启动 | 执行 start-dev-utf8.ps1 |

### 推荐的开发流程

1. **日常开发编译：** 使用 `Ctrl+Alt+B` 快速编译PMS模块
2. **首次编译或依赖变更：** 使用 `Ctrl+Shift+B` 完整编译
3. **启动应用：** 使用 `Ctrl+F5` 启动Spring Boot应用

## Maven命令对比

### 传统完整编译
```bash
mvn clean compile -T 1C -DskipTests=true
```
- 编译所有模块
- 耗时较长（通常30-60秒）

### PMS模块编译
```bash
mvn compile -pl ruoyi-modules/ruoyi-pms -am -T 1C -DskipTests=true
```
- 只编译PMS模块及其依赖
- 耗时较短（通常10-20秒）

## 编译优化原理

### 1. 模块选择优化
- 使用 `-pl ruoyi-modules/ruoyi-pms` 指定编译模块
- 使用 `-am` (also make) 自动包含依赖模块
- 跳过不相关的业务模块

### 2. 依赖分析
PMS模块的编译依赖链：
```
ruoyi-pms
├── ruoyi-common-core
├── ruoyi-common-mybatis
├── ruoyi-common-web
├── ruoyi-common-security
└── 其他必要的common模块
```

### 3. 性能优化
- 并行编译：`-T 1C`（使用1个CPU核心）
- 跳过测试：`-DskipTests=true`
- 安静模式：`-q`（减少输出）

## 故障排除

### 常见问题

1. **编译失败：依赖模块未编译**
   ```
   解决方案：使用 -Clean 参数或先执行完整编译
   ```

2. **PowerShell执行策略限制**
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

3. **中文乱码问题**
   ```
   脚本已自动设置UTF-8编码，无需额外配置
   ```

### 性能监控

编译脚本会显示：
- 编译耗时
- 编译范围
- 文件变化检测（智能编译）

## 最佳实践

1. **开发阶段：** 优先使用 `pms-smart-compile.ps1` 进行增量编译
2. **依赖变更：** 使用 `pms-compile.ps1 -Clean` 清理编译
3. **发布前：** 使用完整项目编译确保所有模块正常
4. **CI/CD：** 在持续集成中仍使用完整编译

## 扩展配置

如需为其他模块创建类似的快速编译，可以参考PMS编译脚本，修改模块路径：

```powershell
# 示例：为system模块创建快速编译
$params += "-pl", "ruoyi-modules/ruoyi-system", "-am"
```

## 总结

通过这套编译优化方案，PMS模块的编译时间可以减少50-70%，显著提高开发效率。建议开发团队统一使用这套配置。 