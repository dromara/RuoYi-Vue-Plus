# RuoYi-Vue-Plus 代码升级操作指南

## 📋 升级概述

本指南将帮助您完成从当前版本到上游最新版本（v5.4.0）的升级。

### 🔍 当前状态
- **本地仓库**: 已包含PMS模块和编译优化配置
- **上游仓库**: https://github.com/dromara/RuoYi-Vue-Plus (v5.4.0)
- **个人仓库**: https://github.com/figo990/RuoYi-Vue-Plus
- **升级分支**: upgrade-to-5.4.0

## 🚀 详细操作步骤

### 第一步：处理合并冲突

当前已经开始合并上游代码，但出现了大量冲突。您需要在Cursor中逐个解决这些冲突：

#### 1.1 查看冲突文件列表
```bash
git status
```

#### 1.2 使用Cursor的冲突解决工具
1. 在Cursor中打开有冲突的文件
2. 您会看到类似这样的冲突标记：
```
<<<<<<< HEAD
您的代码
=======
上游代码
>>>>>>> upstream/5.X
```

#### 1.3 冲突解决策略

**核心原则**: 保留您的PMS模块和优化配置，同时接受上游的框架更新

**重要文件处理建议**:

1. **保留您的修改** (选择HEAD版本):
   - `ruoyi-modules/ruoyi-pms/` 下的所有文件
   - `pom-dev.xml`、`ruoyi-admin/pom-dev.xml`、`ruoyi-modules/pom-dev.xml`
   - `.vscode/` 下的配置文件
   - `script/sql/pms/` 下的SQL文件

2. **接受上游更新** (选择upstream版本):
   - `ruoyi-common/` 下的框架核心文件
   - `ruoyi-modules/ruoyi-system/` 下的系统模块文件
   - `ruoyi-modules/ruoyi-demo/` 下的示例文件
   - `ruoyi-modules/ruoyi-generator/` 下的代码生成器文件

3. **手动合并** (需要仔细处理):
   - `pom.xml` - 保留您的MySQL驱动配置，接受其他依赖更新
   - `ruoyi-modules/pom.xml` - 保留PMS模块引用，接受其他更新
   - `.gitignore` - 合并两个版本的忽略规则

### 第二步：使用Cursor快速解决冲突

#### 2.1 批量处理策略
在Cursor中使用以下快捷键：
- `Ctrl+Shift+P` → 搜索 "Git: Resolve Conflicts"
- 或者使用Source Control面板的冲突解决工具

#### 2.2 自动化脚本处理
创建一个PowerShell脚本来批量处理简单冲突：

```powershell
# 自动接受上游版本的文件（框架核心文件）
$acceptUpstream = @(
    "ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/domain/model/RegisterBody.java",
    "ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/enums/DeviceType.java",
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/RedisCacheController.java"
    # ... 更多文件
)

foreach ($file in $acceptUpstream) {
    if (Test-Path $file) {
        git checkout --theirs $file
        git add $file
    }
}

# 自动保留本地版本的文件（您的PMS模块）
$acceptLocal = @(
    "ruoyi-modules/ruoyi-pms/",
    "pom-dev.xml",
    "ruoyi-admin/pom-dev.xml",
    "ruoyi-modules/pom-dev.xml"
    # ... 更多文件
)

foreach ($file in $acceptLocal) {
    if (Test-Path $file) {
        git checkout --ours $file
        git add $file
    }
}
```

### 第三步：验证升级结果

#### 3.1 编译测试
```bash
# 使用您的优化编译脚本
.\pms-compile.ps1

# 或者使用标准编译
mvn clean compile -pl ruoyi-admin
```

#### 3.2 启动测试
```bash
# 使用您的优化启动脚本
.\dev-start.ps1

# 或者使用快速启动
.\quick-start.ps1
```

### 第四步：完成合并

#### 4.1 提交合并结果
```bash
# 添加所有解决的冲突
git add .

# 提交合并
git commit -m "合并上游v5.4.0更新，保留PMS模块和编译优化配置"
```

#### 4.2 推送到您的GitHub仓库
```bash
# 推送升级分支
git push origin upgrade-to-5.4.0

# 切换到主分支并合并
git checkout master
git merge upgrade-to-5.4.0
git push origin master
```

## 🔧 重要注意事项

### 保护您的自定义内容
1. **PMS模块**: 完全保留，这是您的核心业务模块
2. **编译优化配置**: 保留所有pom-dev.xml文件和编译脚本
3. **VSCode配置**: 保留.vscode/下的所有配置文件
4. **数据库脚本**: 保留script/sql/pms/下的所有SQL文件

### 接受框架更新
1. **依赖版本**: 接受上游的依赖版本更新
2. **框架核心**: 接受ruoyi-common下的所有更新
3. **系统模块**: 接受ruoyi-system的功能增强
4. **安全更新**: 接受所有安全相关的更新

### 手动检查项目
1. **配置文件**: 检查application.yml等配置文件
2. **启动类**: 检查RuoYiApplication.java
3. **数据库连接**: 确认MySQL驱动配置正确
4. **菜单权限**: 检查PMS模块的菜单是否正常

## 🚨 应急回滚方案

如果升级过程中出现问题，可以快速回滚：

```bash
# 取消当前合并
git merge --abort

# 回到升级前的状态
git checkout master
git reset --hard HEAD~1

# 删除升级分支
git branch -D upgrade-to-5.4.0
```

## 📞 技术支持

如果在升级过程中遇到问题：
1. 查看冲突文件的具体内容
2. 参考上游的更新日志
3. 测试关键功能是否正常
4. 保持PMS模块的完整性

## 🎯 升级后验证清单

- [ ] 项目能正常编译
- [ ] 应用能正常启动
- [ ] PMS模块功能正常
- [ ] 系统基础功能正常
- [ ] 数据库连接正常
- [ ] 前端页面能正常访问
- [ ] 用户登录功能正常
- [ ] 菜单权限显示正常

升级完成后，您将拥有最新的框架功能，同时保留所有自定义的PMS模块和优化配置。 