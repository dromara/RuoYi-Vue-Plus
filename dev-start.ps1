# 开发环境优化启动脚本 v2.0
# 智能检测变化，快速启动应用
# 使用方法: .\dev-start.ps1

param(
    [string]$Profile = "dev",
    [switch]$Debug = $false,
    [switch]$SkipCompile = $false,
    [switch]$ForceCompile = $false,
    [switch]$CleanStart = $false
)

# 设置编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

Write-Host "🚀 开发环境智能启动工具 v2.0" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green

# 检查环境
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "✅ Java: $javaVersion" -ForegroundColor Cyan

    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
    Write-Host "✅ Maven: $mavenVersion" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ 请确保Java和Maven已正确安装并配置环境变量" -ForegroundColor Red
    exit 1
}

# 智能检测是否需要编译
function Test-NeedCompile {
    if ($ForceCompile) {
        Write-Host "🔄 强制编译模式" -ForegroundColor Yellow
        return $true
    }

    if ($SkipCompile) {
        Write-Host "⏭️ 跳过编译检测" -ForegroundColor Yellow
        return $false
    }

    # 检查是否存在编译产物
    $adminJar = "ruoyi-admin/target/classes"
    if (-not (Test-Path $adminJar)) {
        Write-Host "📦 未找到编译产物，需要编译" -ForegroundColor Yellow
        return $true
    }

    # 检查关键文件的修改时间
    $sourceFiles = @(
        "ruoyi-admin/src/main/java",
        "ruoyi-modules/ruoyi-system/src/main/java",
        "ruoyi-modules/ruoyi-pms/src/main/java",
        "ruoyi-common"
    )

    $lastCompileTime = (Get-Item $adminJar).LastWriteTime

    foreach ($sourceDir in $sourceFiles) {
        if (Test-Path $sourceDir) {
            $latestSource = Get-ChildItem -Path $sourceDir -Recurse -File -Include "*.java" |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1

            if ($latestSource -and $latestSource.LastWriteTime -gt $lastCompileTime) {
                Write-Host "🔄 检测到源码变化: $($latestSource.Name)" -ForegroundColor Yellow
                return $true
            }
        }
    }

    Write-Host "✅ 无需重新编译，使用现有编译产物" -ForegroundColor Green
    return $false
}

# 修复YAML配置文件中的占位符
function Fix-YamlPlaceholders {
    Write-Host "🔧 修复YAML配置占位符..." -ForegroundColor Yellow

    # 定义占位符替换映射
    $placeholders = @{
        '@logging\.level@'    = 'info'
        '@profiles\.active@'  = $Profile
        '@monitor\.username@' = 'ruoyi'
        '@monitor\.password@' = '123456'
    }

    # 需要修复的配置文件列表
    $configFiles = @(
        "ruoyi-admin/src/main/resources/application.yml",
        "ruoyi-admin/src/main/resources/application-dev.yml",
        "ruoyi-admin/src/main/resources/application-prod.yml"
    )

    $fixedCount = 0

    foreach ($configFile in $configFiles) {
        if (Test-Path $configFile) {
            $content = Get-Content $configFile -Raw -Encoding UTF8
            $originalContent = $content

            # 替换所有占位符
            foreach ($placeholder in $placeholders.Keys) {
                $replacement = $placeholders[$placeholder]
                $content = $content -replace $placeholder, $replacement
            }

            # 如果内容有变化，写回文件
            if ($content -ne $originalContent) {
                Set-Content $configFile -Value $content -Encoding UTF8
                $fixedCount++
                Write-Host "  ✅ 已修复: $configFile" -ForegroundColor Green
            }
        }
    }

    # 修复common-mybatis.yml中缺失的mapperPackage配置
    $commonMybatisFile = "ruoyi-common/ruoyi-common-mybatis/src/main/resources/common-mybatis.yml"
    if (Test-Path $commonMybatisFile) {
        $content = Get-Content $commonMybatisFile -Raw -Encoding UTF8

        # 检查是否已经包含mapperPackage配置
        if ($content -notmatch "mapperPackage:") {
            # 在mybatis-plus配置块中添加mapperPackage
            $mapperPackageConfig = "`n  # 多包名使用 例如 org.dromara.**.mapper,org.xxx.**.mapper`n  mapperPackage: org.dromara.**.mapper"
            $content = $content -replace "(mybatis-plus:)", "`$1$mapperPackageConfig"

            Set-Content $commonMybatisFile -Value $content -Encoding UTF8
            $fixedCount++
            Write-Host "  ✅ 已修复: $commonMybatisFile (添加mapperPackage配置)" -ForegroundColor Green
        }
    }

    if ($fixedCount -gt 0) {
        Write-Host "✅ 已修复 $fixedCount 个配置文件中的占位符" -ForegroundColor Green
    }
    else {
        Write-Host "✅ 配置文件占位符已正确" -ForegroundColor Green
    }
}

# 备份原始pom文件
function Backup-PomFiles {
    Write-Host "📋 备份原始pom文件..." -ForegroundColor Yellow

    if (Test-Path "pom.xml.backup") {
        Write-Host "⚠️ 发现已存在的备份文件，跳过备份" -ForegroundColor Yellow
    }
    else {
        Copy-Item "pom.xml" "pom.xml.backup"
        Copy-Item "ruoyi-admin\pom.xml" "ruoyi-admin\pom.xml.backup"
        Copy-Item "ruoyi-modules\pom.xml" "ruoyi-modules\pom.xml.backup"
        Write-Host "✅ 备份完成" -ForegroundColor Green
    }
}

# 切换到开发环境配置
function Switch-ToDevConfig {
    Write-Host "🔄 切换到开发环境配置..." -ForegroundColor Yellow

    Copy-Item "pom-dev.xml" "pom.xml" -Force
    Copy-Item "ruoyi-admin\pom-dev.xml" "ruoyi-admin\pom.xml" -Force
    Copy-Item "ruoyi-modules\pom-dev.xml" "ruoyi-modules\pom.xml" -Force

    Write-Host "✅ 已切换到开发环境配置" -ForegroundColor Green
}

# 恢复原始配置
function Restore-OriginalConfig {
    Write-Host "🔄 恢复原始配置..." -ForegroundColor Yellow

    if (Test-Path "pom.xml.backup") {
        Copy-Item "pom.xml.backup" "pom.xml" -Force
        Copy-Item "ruoyi-admin\pom.xml.backup" "ruoyi-admin\pom.xml" -Force
        Copy-Item "ruoyi-modules\pom.xml.backup" "ruoyi-modules\pom.xml" -Force

        Remove-Item "pom.xml.backup"
        Remove-Item "ruoyi-admin\pom.xml.backup"
        Remove-Item "ruoyi-modules\pom.xml.backup"

        Write-Host "✅ 已恢复原始配置" -ForegroundColor Green
    }
}

# 快速编译项目
function Compile-Project {
    Write-Host "🔨 开始快速编译..." -ForegroundColor Blue
    $startTime = Get-Date

    # 清理缓存（如果需要）
    if ($CleanStart) {
        Write-Host "🧹 清理编译缓存..." -ForegroundColor Yellow
        & mvn clean -q
    }

    # 优化的编译参数
    $mvnParams = @(
        "compile",
        "resources:resources",
        "-T", "2C",                    # 并行编译
        "-DskipTests=true",            # 跳过测试
        "-Dmaven.compile.fork=true",   # 启用编译进程分叉
        "-Dmaven.javadoc.skip=true",   # 跳过javadoc
        "-Dmaven.source.skip=true",    # 跳过源码打包
        "-q"                           # 静默模式
    )

    & mvn $mvnParams

    if ($LASTEXITCODE -eq 0) {
        $endTime = Get-Date
        $duration = $endTime - $startTime
        Write-Host "✅ 编译成功! 耗时: $($duration.TotalSeconds.ToString('F2'))秒" -ForegroundColor Green
    }
    else {
        Write-Host "❌ 编译失败!" -ForegroundColor Red
        throw "编译失败"
    }
}

# 启动应用
function Start-Application {
    Write-Host "🎯 启动范围: 核心模块 + PMS模块" -ForegroundColor Yellow
    Write-Host "📦 跳过模块: monitor、workflow、demo、generator、job" -ForegroundColor Yellow
    Write-Host "🌍 运行环境: $Profile" -ForegroundColor Yellow

    # 优化的启动参数
    $mvnParams = @(
        "spring-boot:run",
        "-pl", "ruoyi-admin",
        "-Dspring.profiles.active=$Profile",
        "-Dspring-boot.run.fork=false",  # 不分叉进程，减少启动时间
        "-Dspring-boot.run.optimizedLaunch=true"  # 优化启动
    )

    if ($Debug) {
        Write-Host "🐛 启用调试模式 (端口: 5005)" -ForegroundColor Yellow
        $mvnParams += "-Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
    }

    # 显示启动命令
    $cmdString = "mvn " + ($mvnParams -join " ")
    Write-Host "🔧 执行命令: $cmdString" -ForegroundColor Cyan

    Write-Host "⏱️ 启动应用..." -ForegroundColor Blue
    Write-Host "🌐 应用将在启动完成后可通过 http://localhost:8080 访问" -ForegroundColor Cyan

    & mvn $mvnParams
}

# 显示使用帮助
function Show-Usage {
    Write-Host "`n📖 使用说明:" -ForegroundColor Cyan
    Write-Host "  .\dev-start.ps1                    # 智能启动（推荐）"
    Write-Host "  .\dev-start.ps1 -SkipCompile       # 跳过编译直接启动"
    Write-Host "  .\dev-start.ps1 -ForceCompile      # 强制重新编译"
    Write-Host "  .\dev-start.ps1 -CleanStart        # 清理后重新编译启动"
    Write-Host "  .\dev-start.ps1 -Debug             # 启用调试模式"
    Write-Host "  .\dev-start.ps1 -Profile prod      # 指定运行环境"
}

# 主逻辑
try {
    # 显示启动模式
    if ($SkipCompile) {
        Write-Host "🚀 快速启动模式 (跳过编译)" -ForegroundColor Magenta
    }
    elseif ($ForceCompile) {
        Write-Host "🔄 强制编译模式" -ForegroundColor Magenta
    }
    elseif ($CleanStart) {
        Write-Host "🧹 清理重启模式" -ForegroundColor Magenta
    }
    else {
        Write-Host "🧠 智能检测模式" -ForegroundColor Magenta
    }

    # 备份并切换配置
    Backup-PomFiles
    Switch-ToDevConfig

    # 修复YAML占位符
    Fix-YamlPlaceholders

    # 智能编译检测
    $needCompile = Test-NeedCompile
    if ($needCompile) {
        Compile-Project
    }

    # 启动应用
    Start-Application
}
catch {
    Write-Host "❌ 启动过程中发生错误: $($_.Exception.Message)" -ForegroundColor Red
    Show-Usage
    exit 1
}
finally {
    # 恢复原始配置
    Restore-OriginalConfig
}

Write-Host "🎉 应用已启动!" -ForegroundColor Green
