# 零编译快速启动脚本
# 跳过编译直接启动应用
# 使用方法: .\quick-start.ps1

param(
    [string]$Profile = "dev"
)

# 设置编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

Write-Host "⚡ 零编译快速启动" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green

# 检查编译产物
$adminClasses = "ruoyi-admin/target/classes"
if (-not (Test-Path $adminClasses)) {
    Write-Host "❌ 未找到编译产物，请先运行编译:" -ForegroundColor Red
    Write-Host "   按 F5 进行智能启动" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ 发现编译产物，准备启动" -ForegroundColor Green

# 修复YAML占位符
$configFile = "ruoyi-admin/src/main/resources/application-$Profile.yml"
if (Test-Path $configFile) {
    $content = Get-Content $configFile -Raw -Encoding UTF8
    $content = $content -replace '@logging\.level@', 'info'
    $content = $content -replace '@profiles\.active@', $Profile
    Set-Content $configFile -Value $content -Encoding UTF8
}

Write-Host "🚀 启动应用..." -ForegroundColor Blue
Write-Host "🌐 应用将在启动完成后可通过 http://localhost:8080 访问" -ForegroundColor Cyan

# 启动应用
& mvn spring-boot:run -pl ruoyi-admin -Dspring.profiles.active=$Profile -q

Write-Host "🎉 应用已启动!" -ForegroundColor Green
