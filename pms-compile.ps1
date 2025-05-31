# PMS模块快速编译脚本
# 专门用于编译 ruoyi-pms 模块，跳过其他公共模块的编译
# 使用方法: .\pms-compile.ps1

param(
    [switch]$Clean = $false,
    [switch]$SkipTests = $true,
    [switch]$Offline = $false,
    [switch]$Quiet = $true
)

# 设置编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

Write-Host "🚀 PMS模块快速编译工具" -ForegroundColor Green
Write-Host "========================" -ForegroundColor Green

# 检查Java和Maven环境
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

# 构建Maven命令
$mvnCmd = "mvn"
$params = @()

# 添加编译参数
if ($Clean) {
    $params += "clean"
}

$params += "compile"

# 指定只编译PMS模块及其依赖
$params += "-pl"
$params += "ruoyi-modules/ruoyi-pms"
$params += "-am"  # also make - 同时编译依赖模块

# 并行编译
$params += "-T"
$params += "1C"

# 跳过测试
if ($SkipTests) {
    $params += "-DskipTests=true"
}

# 离线模式
if ($Offline) {
    $params += "-o"
}

# 安静模式
if ($Quiet) {
    $params += "-q"
}

Write-Host "🎯 编译模块: ruoyi-pms (及其依赖)" -ForegroundColor Yellow
Write-Host "📦 编译范围: 仅PMS模块，跳过其他业务模块" -ForegroundColor Yellow

# 显示将要执行的命令
$cmdString = "$mvnCmd " + ($params -join " ")
Write-Host "🔧 执行命令: $cmdString" -ForegroundColor Cyan

# 执行编译
Write-Host "⏱️ 开始编译..." -ForegroundColor Blue
$startTime = Get-Date

try {
    & $mvnCmd $params

    if ($LASTEXITCODE -eq 0) {
        $endTime = Get-Date
        $duration = $endTime - $startTime
        Write-Host "✅ PMS模块编译成功! 耗时: $($duration.TotalSeconds.ToString('F2'))秒" -ForegroundColor Green
        Write-Host "📋 编译范围: ruoyi-pms + 必要依赖模块" -ForegroundColor Green
    }
    else {
        Write-Host "❌ 编译失败!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}
catch {
    Write-Host "❌ 编译过程中发生错误: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "🎉 PMS模块编译完成!" -ForegroundColor Green
