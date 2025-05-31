# Cursor 编译优化配置指南

## 1. Cursor 基础配置优化

### 1.1 工作区设置
在项目根目录创建 `.vscode/settings.json`：

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "D:\\jdk",
      "default": true
    }
  ],
  "java.compile.nullAnalysis.mode": "disabled",
  "java.autobuild.enabled": false,
  "java.maxConcurrentBuilds": 8,
  "java.import.maven.enabled": true,
  "java.import.gradle.enabled": false,
  "java.configuration.maven.userSettings": "C:\\Users\\{username}\\.m2\\settings.xml",
  "maven.executable.path": "D:\\maven3.9\\bin\\mvn.cmd",
  "maven.terminal.useJavaHome": true,
  "maven.terminal.customEnv": [
    {
      "environmentVariable": "MAVEN_OPTS",
      "value": "-Xmx4g -XX:+UseG1GC"
    },
    {
      "environmentVariable": "JAVA_TOOL_OPTIONS", 
      "value": "-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
    }
  ],
  "files.exclude": {
    "**/target": true,
    "**/.idea": true,
    "**/node_modules": true,
    "**/logs": true,
    "**/temp": true
  },
  "search.exclude": {
    "**/target": true,
    "**/node_modules": true,
    "**/logs": true
  },
  "files.watcherExclude": {
    "**/target/**": true,
    "**/node_modules/**": true,
    "**/logs/**": true
  }
}
```

### 1.2 Java扩展配置
在 `.vscode/settings.json` 中添加Java相关配置：

```json
{
  "java.server.launchMode": "Standard",
  "java.sources.organizeImports.starThreshold": 99,
  "java.sources.organizeImports.staticStarThreshold": 99,
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
  "java.format.settings.profile": "GoogleStyle",
  "java.saveActions.organizeImports": true,
  "java.completion.favoriteStaticMembers": [
    "org.junit.Assert.*",
    "org.junit.Assume.*",
    "org.junit.jupiter.api.Assertions.*",
    "org.junit.jupiter.api.Assumptions.*",
    "org.junit.jupiter.api.DynamicContainer.*",
    "org.junit.jupiter.api.DynamicTest.*",
    "org.mockito.Mockito.*",
    "org.mockito.ArgumentMatchers.*",
    "org.mockito.Answers.*"
  ]
}
```

## 2. Maven 集成优化

### 2.1 Maven 任务配置
在 `.vscode/tasks.json` 中配置Maven任务：

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Maven: Clean Compile",
      "type": "shell",
      "command": "mvn",
      "args": ["clean", "compile", "-T", "1C", "-DskipTests=true"],
      "group": {
        "kind": "build",
        "isDefault": true
      },
      "presentation": {
        "echo": true,
        "reveal": "always",
        "focus": false,
        "panel": "shared",
        "showReuseMessage": true,
        "clear": false
      },
      "problemMatcher": ["$maven-compiler-java"]
    },
    {
      "label": "Maven: Compile PMS Module",
      "type": "shell",
      "command": "mvn",
      "args": ["compile", "-pl", "ruoyi-modules/ruoyi-pms", "-am", "-T", "1C", "-DskipTests=true"],
      "group": "build",
      "presentation": {
        "echo": true,
        "reveal": "always",
        "focus": false,
        "panel": "shared"
      },
      "problemMatcher": ["$maven-compiler-java"]
    },
    {
      "label": "Maven: Package",
      "type": "shell",
      "command": "mvn",
      "args": ["clean", "package", "-T", "1C", "-DskipTests=true"],
      "group": "build",
      "presentation": {
        "echo": true,
        "reveal": "always",
        "focus": false,
        "panel": "shared"
      },
      "problemMatcher": ["$maven-compiler-java"]
    },
    {
      "label": "Spring Boot: Run",
      "type": "shell",
      "command": "mvn",
      "args": ["spring-boot:run", "-pl", "ruoyi-admin", "-Dspring.profiles.active=dev"],
      "group": "build",
      "presentation": {
        "echo": true,
        "reveal": "always",
        "focus": false,
        "panel": "dedicated"
      },
      "isBackground": true,
      "problemMatcher": {
        "pattern": {
          "regexp": "^.*$",
          "file": 1,
          "location": 2,
          "message": 3
        },
        "background": {
          "activeOnStart": true,
          "beginsPattern": "^.*Starting.*",
          "endsPattern": "^.*Started.*in.*seconds.*"
        }
      }
    }
  ]
}
```

### 2.2 启动配置
在 `.vscode/launch.json` 中配置调试启动：

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "RuoYi Application",
      "request": "launch",
      "mainClass": "org.dromara.DromaraApplication",
      "projectName": "ruoyi-admin",
      "args": "",
      "vmArgs": "-Xmx2g -Xms1g -XX:+UseG1GC -Dspring.profiles.active=dev -Dfile.encoding=UTF-8",
      "env": {
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
      },
      "console": "integratedTerminal",
      "internalConsoleOptions": "neverOpen"
    },
    {
      "type": "java",
      "name": "RuoYi Debug",
      "request": "launch",
      "mainClass": "org.dromara.DromaraApplication",
      "projectName": "ruoyi-admin",
      "args": "",
      "vmArgs": "-Xmx2g -Xms1g -XX:+UseG1GC -Dspring.profiles.active=dev -Dfile.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
      "env": {
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
      },
      "console": "integratedTerminal",
      "internalConsoleOptions": "neverOpen"
    }
  ]
}
```

## 3. Cursor AI 功能优化

### 3.1 AI 代码补全配置
在用户设置中配置：

```json
{
  "cursor.ai.enabled": true,
  "cursor.ai.model": "gpt-4",
  "cursor.ai.maxTokens": 4000,
  "cursor.ai.temperature": 0.1,
  "cursor.ai.includeContext": true,
  "cursor.ai.contextFiles": [
    "docs/**/*.md",
    "ruoyi-modules/ruoyi-pms/src/main/java/**/*.java",
    "*.md"
  ]
}
```

### 3.2 代码生成提示模板
创建 `.cursor/rules` 文件：

```
# RuoYi-Vue-Plus 开发规则
- 遵循RuoYi框架规范
- 使用MapStruct Plus进行对象转换
- Service方法添加@Transactional注解
- Controller方法添加@Log和@SaCheckPermission注解
- 实体类继承TenantEntity
- BO类使用JSR 303校验注解
- VO类使用@Translation和@Sensitive注解
```

## 4. 性能优化配置

### 4.1 内存和CPU优化
在 `.vscode/settings.json` 中添加：

```json
{
  "java.jdt.ls.vmargs": "-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Dsun.zip.disableMemoryMapping=true -Xmx4g -Xms1g",
  "java.import.maven.offline": false,
  "java.maven.downloadSources": true,
  "java.maven.downloadJavadoc": false,
  "java.referencesCodeLens.enabled": false,
  "java.implementationsCodeLens.enabled": false,
  "java.signatureHelp.enabled": true,
  "java.contentProvider.preferred": "fernflower"
}
```

### 4.2 文件监控优化
```json
{
  "files.watcherExclude": {
    "**/target/**": true,
    "**/node_modules/**": true,
    "**/.git/objects/**": true,
    "**/.git/subtree-cache/**": true,
    "**/logs/**": true,
    "**/temp/**": true
  }
}
```

## 5. 推荐扩展

### 5.1 必装扩展
- **Extension Pack for Java** - Java开发基础包
- **Spring Boot Extension Pack** - Spring Boot支持
- **Maven for Java** - Maven集成
- **Lombok Annotations Support** - Lombok支持
- **SonarLint** - 代码质量检查
- **GitLens** - Git增强
- **Thunder Client** - API测试

### 5.2 可选扩展
- **Bracket Pair Colorizer** - 括号高亮
- **Auto Rename Tag** - 标签自动重命名
- **Path Intellisense** - 路径智能提示
- **Chinese Language Pack** - 中文语言包

## 6. 快捷键配置

### 6.1 自定义快捷键
在 `.vscode/keybindings.json` 中配置：

```json
[
  {
    "key": "ctrl+shift+b",
    "command": "workbench.action.tasks.runTask",
    "args": "Maven: Clean Compile"
  },
  {
    "key": "ctrl+f5",
    "command": "workbench.action.tasks.runTask",
    "args": "Spring Boot: Run"
  },
  {
    "key": "ctrl+shift+f5",
    "command": "workbench.action.debug.start"
  },
  {
    "key": "ctrl+shift+p",
    "command": "workbench.action.tasks.runTask",
    "args": "Maven: Compile PMS Module"
  }
]
```

## 7. 代码片段

### 7.1 创建代码片段文件
在 `.vscode/snippets/java.json` 中添加：

```json
{
  "RuoYi Controller": {
    "prefix": "rycontroller",
    "body": [
      "@Api(tags = \"${1:模块}管理\")",
      "@RestController",
      "@RequestMapping(\"/${2:module}/${3:entity}\")",
      "@RequiredArgsConstructor",
      "public class ${4:Entity}Controller {",
      "",
      "    private final I${4:Entity}Service ${5:entity}Service;",
      "",
      "    @GetMapping(\"/list\")",
      "    @ApiOperation(\"查询${1:模块}列表\")",
      "    @SaCheckPermission(\"${2:module}:${3:entity}:list\")",
      "    public TableDataInfo<${4:Entity}Vo> list(${4:Entity}Bo bo, PageQuery pageQuery) {",
      "        return ${5:entity}Service.queryPageList(bo, pageQuery);",
      "    }",
      "",
      "    $0",
      "}"
    ],
    "description": "创建RuoYi Controller模板"
  },
  "RuoYi Service": {
    "prefix": "ryservice",
    "body": [
      "@Service",
      "@RequiredArgsConstructor",
      "public class ${1:Entity}ServiceImpl implements I${1:Entity}Service {",
      "",
      "    private final ${1:Entity}Mapper baseMapper;",
      "",
      "    @Override",
      "    @Transactional(rollbackFor = Exception.class)",
      "    public Boolean insertByBo(${1:Entity}Bo bo) {",
      "        ${1:Entity} add = MapstructUtils.convert(bo, ${1:Entity}.class);",
      "        boolean flag = baseMapper.insert(add) > 0;",
      "        if (flag) {",
      "            bo.set${2:Id}(add.get${2:Id}());",
      "        }",
      "        return flag;",
      "    }",
      "",
      "    $0",
      "}"
    ],
    "description": "创建RuoYi Service实现类模板"
  }
}
```

## 8. 调试配置

### 8.1 远程调试配置
```json
{
  "type": "java",
  "name": "Remote Debug",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005,
  "projectName": "ruoyi-admin"
}
```

### 8.2 测试配置
```json
{
  "type": "java",
  "name": "JUnit Tests",
  "request": "launch",
  "mainClass": "",
  "projectName": "ruoyi-pms",
  "args": "",
  "vmArgs": "-Dspring.profiles.active=test",
  "env": {},
  "console": "integratedTerminal"
}
```

## 9. 项目结构优化

### 9.1 工作区配置
创建 `ruoyi-vue-plus.code-workspace`：

```json
{
  "folders": [
    {
      "name": "RuoYi-Vue-Plus",
      "path": "."
    },
    {
      "name": "PMS模块",
      "path": "./ruoyi-modules/ruoyi-pms"
    },
    {
      "name": "前端项目",
      "path": "./ruoyi-plus-soybean"
    }
  ],
  "settings": {
    "java.configuration.workspaceCacheLimit": 90
  },
  "extensions": {
    "recommendations": [
      "vscjava.vscode-java-pack",
      "vmware.vscode-spring-boot",
      "vscjava.vscode-maven",
      "gabrielbb.vscode-lombok",
      "sonarsource.sonarlint-vscode"
    ]
  }
}
```

## 10. 故障排除

### 10.1 常见问题
1. **Java Language Server启动失败**
   - 检查Java路径配置
   - 清理工作区缓存：`Ctrl+Shift+P` -> `Java: Reload Projects`

2. **Maven依赖无法解析**
   - 检查Maven配置路径
   - 执行：`Ctrl+Shift+P` -> `Java: Reload Projects`

3. **编译错误**
   - 检查JDK版本是否正确
   - 清理target目录：`mvn clean`

### 10.2 性能优化建议
1. 定期清理工作区缓存
2. 关闭不必要的扩展
3. 调整Java Language Server内存
4. 使用SSD存储项目文件

## 使用建议

1. **首次打开项目**：
   - 等待Java Language Server初始化完成
   - 执行 `Java: Reload Projects` 确保依赖正确加载
   - 运行 `Maven: Clean Compile` 验证编译

2. **日常开发**：
   - 使用快捷键快速编译和运行
   - 利用AI代码补全提升效率
   - 定期清理缓存和临时文件

3. **调试技巧**：
   - 使用断点调试复杂业务逻辑
   - 利用条件断点提升调试效率
   - 使用远程调试连接运行中的应用 