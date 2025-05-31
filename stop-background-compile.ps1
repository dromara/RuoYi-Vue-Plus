# 停止后台编译进程脚本
# 用于停止VSCode Java Language Server和Maven的后台编译
# 使用方法: .\stop-background-compile.ps1

param(
    [switch]$Force = $false,
    [switch]$CleanCache = $false
)

# 设置编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "🛑 停止后台编译进程工具" -ForegroundColor Red
Write-Host "=========================" -ForegroundColor Red

# 停止Java Language Server进程
function Stop-JavaLanguageServer {
    Write-Host "🔍 查找Java Language Server进程..." -ForegroundColor Yellow

    $javaProcesses = Get-Process | Where-Object {
        $_.ProcessName -like "*java*" -and
        ($_.CommandLine -like "*jdt.ls*" -or
         $_.CommandLine -like "*eclipse.jdt*" -or
         $_.CommandLine -like "*language-server*")
    } -ErrorAction SilentlyContinue

    if ($javaProcesses) {
        foreach ($process in $javaProcesses) {
            try {
                Write-Host "  🛑 停止进程: $($process.ProcessName) (PID: $($process.Id))" -ForegroundColor Red
                if ($Force) {
                    $process | Stop-Process -Force
                } else {
                    $process | Stop-Process
                }
                Write-Host "  ✅ 已停止" -ForegroundColor Green
            }
            catch {
                Write-Host "  ⚠️ 无法停止进程 $($process.Id): $($_.Exception.Message)" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "  ✅ 未发现Java Language Server进程" -ForegroundColor Green
    }
}

# 停止Maven进程
function Stop-MavenProcesses {
    Write-Host "🔍 查找Maven进程..." -ForegroundColor Yellow

    $mavenProcesses = Get-Process | Where-Object {
        $_.ProcessName -like "*mvn*" -or
        $_.ProcessName -like "*maven*"
    } -ErrorAction SilentlyContinue

    if ($mavenProcesses) {
        foreach ($process in $mavenProcesses) {
            try {
                Write-Host "  🛑 停止进程: $($process.ProcessName) (PID: $($process.Id))" -ForegroundColor Red
                if ($Force) {
                    $process | Stop-Process -Force
                } else {
                    $process | Stop-Process
                }
                Write-Host "  ✅ 已停止" -ForegroundColor Green
            }
            catch {
                Write-Host "  ⚠️ 无法停止进程 $($process.Id): $($_.Exception.Message)" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "  ✅ 未发现Maven进程" -ForegroundColor Green
    }
}

# 清理编译缓存
function Clear-CompileCache {
    if (-not $CleanCache) {
        return
    }

    Write-Host "🧹 清理编译缓存..." -ForegroundColor Yellow

    # 清理target目录
    $targetDirs = Get-ChildItem -Path "." -Recurse -Directory -Name "target" -ErrorAction SilentlyContinue
    foreach ($targetDir in $targetDirs) {
        try {
            Write-Host "  🗑️ 清理: $targetDir" -ForegroundColor Cyan
            Remove-Item $targetDir -Recurse -Force -ErrorAction SilentlyContinue
        }
        catch {
            Write-Host "  ⚠️ 无法清理 $targetDir" -ForegroundColor Yellow
        }
    }

    # 清理VSCode Java工作区缓存
    $vscodeJavaCache = "$env:USERPROFILE\.vscode\extensions\redhat.java-*\jre\*\bin"
    if (Test-Path $vscodeJavaCache) {
        try {
            Write-Host "  🗑️ 清理VSCode Java缓存" -ForegroundColor Cyan
            Remove-Item $vscodeJavaCache -Recurse -Force -ErrorAction SilentlyContinue
        }
        catch {
            Write-Host "  ⚠️ 无法清理VSCode Java缓存" -ForegroundColor Yellow
        }
    }

    # 清理.metadata目录
    $metadataDirs = Get-ChildItem -Path "." -Recurse -Directory -Name ".metadata" -ErrorAction SilentlyContinue
    foreach ($metadataDir in $metadataDirs) {
        try {
            Write-Host "  🗑️ 清理: $metadataDir" -ForegroundColor Cyan
            Remove-Item $metadataDir -Recurse -Force -ErrorAction SilentlyContinue
        }
        catch {
            Write-Host "  ⚠️ 无法清理 $metadataDir" -ForegroundColor Yellow
        }
    }

    Write-Host "✅ 缓存清理完成" -ForegroundColor Green
}

# 检查端口占用
function Check-PortUsage {
    Write-Host "🔍 检查端口占用..." -ForegroundColor Yellow

    $ports = @(8080, 5005, 35729)  # 常用的开发端口

    foreach ($port in $ports) {
        $connections = netstat -ano | findstr ":$port"
        if ($connections) {
            Write-Host "  ⚠️ 端口 $port 被占用:" -ForegroundColor Yellow
            $connections | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
        } else {
            Write-Host "  ✅ 端口 $port 空闲" -ForegroundColor Green
        }
    }
}

# 显示使用帮助
function Show-Usage {
    Write-Host "`n📖 使用说明:" -ForegroundColor Cyan
    Write-Host "  .\stop-background-compile.ps1                # 正常停止进程"
    Write-Host "  .\stop-background-compile.ps1 -Force         # 强制停止进程"
    Write-Host "  .\stop-background-compile.ps1 -CleanCache    # 停止进程并清理缓存"
    Write-Host "  .\stop-background-compile.ps1 -Force -CleanCache  # 强制停止并清理"
}

# 主逻辑
try {
    # 显示当前模式
    if ($Force) {
        Write-Host "⚡ 强制停止模式" -ForegroundColor Magenta
    } else {
        Write-Host "🛑 正常停止模式" -ForegroundColor Magenta
    }

    if ($CleanCache) {
        Write-Host "🧹 包含缓存清理" -ForegroundColor Magenta
    }

    Write-Host ""

    # 停止Java Language Server
    Stop-JavaLanguageServer

    # 停止Maven进程
    Stop-MavenProcesses

    # 清理缓存
    Clear-CompileCache

    # 检查端口占用
    Check-PortUsage

    Write-Host "`n🎉 后台编译进程已停止!" -ForegroundColor Green
    Write-Host "💡 建议重启VSCode以确保完全生效" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ 停止过程中发生错误: $($_.Exception.Message)" -ForegroundColor Red
    Show-Usage
    exit 1
}

Show-Usage
