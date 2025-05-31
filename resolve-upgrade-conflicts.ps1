# RuoYi-Vue-Plus 升级冲突自动解决脚本
# 作者: AI Assistant
# 用途: 自动解决升级过程中的Git冲突

Write-Host "🚀 开始自动解决升级冲突..." -ForegroundColor Green

# 检查是否在合并状态
$gitStatus = git status --porcelain
if (-not ($gitStatus -match "^UU|^AA|^DD")) {
    Write-Host "❌ 当前没有检测到合并冲突" -ForegroundColor Red
    exit 1
}

Write-Host "📋 检测到合并冲突，开始自动处理..." -ForegroundColor Yellow

# 1. 自动接受上游版本的文件（框架核心文件）
Write-Host "🔄 处理框架核心文件（接受上游版本）..." -ForegroundColor Cyan

$acceptUpstream = @(
    # 框架核心文件
    "ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/domain/model/RegisterBody.java",
    "ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/enums/DeviceType.java",
    "ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/enums/UserType.java",

    # 系统模块文件
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/MetaVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/RouterVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysClientVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysConfigVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysDeptVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysDictDataVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysDictTypeVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysLogininforVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysOperLogVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysOssConfigVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysPostVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysRoleVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysTenantPackageVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysTenantVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysUserExportVo.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysUserImportVo.java",

    # 系统控制器
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/monitor/CacheController.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysMenuController.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysProfileController.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java",

    # 系统服务
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISysMenuService.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysDeptServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysDictDataServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPostServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysTaskAssigneeServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysTenantServiceImpl.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java",

    # 系统映射器
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysRoleMapper.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysRoleMenuMapper.java",
    "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/SysUserImportListener.java",

    # Demo模块
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/RedisCacheController.java",
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/bo/TestDemoImportVo.java",
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/ExportDemoVo.java",
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestDemoVo.java",
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestTreeVo.java",
    "ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/listener/ExportDemoListener.java",

    # 代码生成器
    "ruoyi-modules/ruoyi-generator/src/main/java/org/dromara/generator/service/GenTableServiceImpl.java",
    "ruoyi-modules/ruoyi-generator/src/main/resources/vm/java/serviceImpl.java.vm",
    "ruoyi-modules/ruoyi-generator/src/main/resources/vm/java/vo.java.vm",
    "ruoyi-modules/ruoyi-generator/src/main/resources/vm/vue/index-tree.vue.vm",
    "ruoyi-modules/ruoyi-generator/src/main/resources/vm/vue/index.vue.vm",

    # 任务调度
    "ruoyi-modules/ruoyi-job/src/main/java/org/dromara/job/snailjob/TestAnnoJobExecutor.java",

    # 工作流模块
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/common/ConditionalOnEnable.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/common/constant/FlowConstant.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/common/enums/ButtonPermissionEnum.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/FlwCategoryController.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/FlwInstanceController.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/bo/BackProcessBo.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/bo/TestLeaveBo.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/vo/FlowCategoryVo.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/vo/FlowTaskVo.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/vo/TestLeaveVo.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/handler/FlowProcessEventHandler.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/handler/WorkflowPermissionHandler.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/IFlwCommonService.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/IFlwInstanceService.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/IFlwTaskAssigneeService.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/IFlwTaskService.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwCommonServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwDefinitionServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwInstanceServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwNodeExtServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskAssigneeServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/TestLeaveServiceImpl.java",
    "ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/WorkflowServiceImpl.java",

    # 扩展模块
    "ruoyi-extend/ruoyi-snailjob-server/src/main/java/com/aizuda/snailjob/server/starter/filter/ActuatorAuthFilter.java",

    # 租户相关
    "ruoyi-common/ruoyi-common-tenant/src/main/java/org/dromara/common/tenant/core/TenantSaTokenDao.java",
    "ruoyi-common/ruoyi-common-tenant/src/main/java/org/dromara/common/tenant/handle/PlusTenantLineHandler.java",

    # Web相关
    "ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/handler/GlobalExceptionHandler.java",

    # 社交相关
    "ruoyi-common/ruoyi-common-social/src/main/java/org/dromara/common/social/utils/SocialUtils.java",

    # 配置文件
    ".run/ruoyi-monitor-admin.run.xml",
    ".run/ruoyi-server.run.xml",
    ".run/ruoyi-snailjob-server.run.xml",

    # Docker配置
    "script/docker/docker-compose.yml",
    "script/docker/nginx/conf/nginx.conf",

    # 工作流配置
    "script/leave/leave1.json",
    "script/leave/leave2.json",
    "script/leave/leave3.json",
    "script/leave/leave4.json",
    "script/leave/leave5.json",

    # SQL脚本
    "script/sql/ry_job.sql",
    "script/sql/ry_vue_5.X.sql",
    "script/sql/ry_workflow.sql"
)

$upstreamCount = 0
foreach ($file in $acceptUpstream) {
    if (Test-Path $file) {
        try {
            git checkout --theirs $file 2>$null
            git add $file 2>$null
            $upstreamCount++
            Write-Host "  ✅ $file" -ForegroundColor Green
        } catch {
            Write-Host "  ⚠️  无法处理: $file" -ForegroundColor Yellow
        }
    }
}

Write-Host "🔄 处理自定义文件（保留本地版本）..." -ForegroundColor Cyan

# 2. 自动保留本地版本的文件（您的自定义内容）
$acceptLocal = @(
    # PMS模块（完全保留）
    "ruoyi-modules/ruoyi-pms/",

    # 开发环境配置
    "pom-dev.xml",
    "ruoyi-admin/pom-dev.xml",
    "ruoyi-modules/pom-dev.xml",

    # VSCode配置
    ".vscode/keybindings.json",
    ".vscode/launch.json",
    ".vscode/tasks.json",
    ".vscode/snippets/",

    # PMS相关SQL
    "script/sql/pms/",
    "script/sql/pms_order_demo_data.sql",
    "script/sql/pms_order_menu.sql",
    "script/sql/pms_order_tables.sql",
    "script/sql/pms_pricing_menu_fixed.sql",
    "script/sql/pms_pricing_tables.sql",
    "script/sql/pms_system_menu_fixed.sql",
    "script/sql/pms_system_tables_fixed.sql"
)

$localCount = 0
foreach ($file in $acceptLocal) {
    if (Test-Path $file) {
        try {
            if (Test-Path $file -PathType Container) {
                # 处理目录
                Get-ChildItem $file -Recurse -File | ForEach-Object {
                    git checkout --ours $_.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/") 2>$null
                    git add $_.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/") 2>$null
                }
            } else {
                # 处理文件
                git checkout --ours $file 2>$null
                git add $file 2>$null
            }
            $localCount++
            Write-Host "  ✅ $file" -ForegroundColor Green
        } catch {
            Write-Host "  ⚠️  无法处理: $file" -ForegroundColor Yellow
        }
    }
}

Write-Host "🔧 处理需要手动合并的重要文件..." -ForegroundColor Magenta

# 3. 需要手动处理的重要文件
$manualFiles = @(
    "pom.xml",
    "ruoyi-modules/pom.xml",
    ".gitignore",
    "README.md"
)

Write-Host "⚠️  以下文件需要手动处理冲突:" -ForegroundColor Yellow
foreach ($file in $manualFiles) {
    if (Test-Path $file) {
        $status = git status --porcelain $file
        if ($status -match "^UU|^AA|^DD") {
            Write-Host "  📝 $file" -ForegroundColor Yellow
        }
    }
}

# 检查剩余冲突
$remainingConflicts = git status --porcelain | Where-Object { $_ -match "^UU|^AA|^DD" }
$conflictCount = ($remainingConflicts | Measure-Object).Count

Write-Host "`n📊 处理结果统计:" -ForegroundColor Cyan
Write-Host "  🔄 自动接受上游版本: $upstreamCount 个文件" -ForegroundColor Green
Write-Host "  💾 自动保留本地版本: $localCount 个文件" -ForegroundColor Green
Write-Host "  ⚠️  剩余需要手动处理: $conflictCount 个文件" -ForegroundColor Yellow

if ($conflictCount -eq 0) {
    Write-Host "`n🎉 所有冲突已自动解决！可以提交合并结果。" -ForegroundColor Green
    Write-Host "执行以下命令完成合并:" -ForegroundColor Cyan
    Write-Host "  git commit -m `"合并上游v5.4.0更新，保留PMS模块和编译优化配置`"" -ForegroundColor White
} else {
    Write-Host "`n📝 请手动解决剩余冲突，然后执行:" -ForegroundColor Yellow
    Write-Host "  git add ." -ForegroundColor White
    Write-Host "  git commit -m `"合并上游v5.4.0更新，保留PMS模块和编译优化配置`"" -ForegroundColor White
}

Write-Host "`n✨ 冲突解决脚本执行完成！" -ForegroundColor Green
