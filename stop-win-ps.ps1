# Aetheris RAG 系统停止脚本（Windows PowerShell 版本）
# 用途: 支持命令行参数和交互式菜单的选择性停止
# 要求: PowerShell 5.1+ 或 PowerShell Core 7+

# 设置错误处理
$ErrorActionPreference = "Stop"

# ========================================
# 获取项目根目录（脚本所在目录）
# ========================================
$PROJECT_ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $PROJECT_ROOT

# ========================================
# 辅助函数
# ========================================

function Write-ColorOutput {
    <#
    .SYNOPSIS
    带颜色的输出函数
    .PARAMETER Color
    颜色：Green, Yellow, Red, Blue, Cyan
    #>
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Write-Step {
    param([string]$Message)
    Write-ColorOutput "[$Message]" "Yellow"
}

function Write-Success {
    param([string]$Message)
    Write-ColorOutput "✅ $Message" "Green"
}

function Write-Warning {
    param([string]$Message)
    Write-ColorOutput "⚠️  $Message" "Yellow"
}

function Write-Error {
    param([string]$Message)
    Write-ColorOutput "❌ $Message" "Red"
}

function Write-Info {
    param([string]$Message)
    Write-ColorOutput $Message "Blue"
}

# ========================================
# 停止函数
# ========================================

function Stop-BackendService {
    Write-Step "停止后端"

    # 查找后端进程
    # 分别查找 Maven 进程和应用进程
    $mvnProcess = Get-Process | Where-Object {
        $_.CommandLine -match "java.*spring-boot:run"
    } -ErrorAction SilentlyContinue

    $appProcess = Get-Process | Where-Object {
        $_.CommandLine -match "java.*AetherisRagApplication"
    } -ErrorAction SilentlyContinue

    # 如果没找到，尝试查找 PowerShell Job（用于我们启动的后台进程）
    $backendJobs = Get-Job | Where-Object {
        $_.Command -match "mvn spring-boot:run|spring-boot:run"
    } -ErrorAction SilentlyContinue

    if ($mvnProcess -or $appProcess -or $backendJobs) {
        if ($mvnProcess -or $appProcess) {
            Write-Info "找到后端进程:"
            if ($mvnProcess) {
                $mvnProcess | ForEach-Object {
                    Write-Host "  - Maven 进程: $($_.Id)" -ForegroundColor Cyan
                }
            }
            if ($appProcess) {
                $appProcess | ForEach-Object {
                    Write-Host "  - 应用进程: $($_.Id)" -ForegroundColor Cyan
                }
            }

            # 优雅关闭
            Write-Info "正在优雅关闭..."
            if ($mvnProcess) { $mvnProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
            if ($appProcess) { $appProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
            Start-Sleep -Seconds 3

            # 检查并强制关闭
            $remainingMvnProcess = Get-Process | Where-Object {
                $_.CommandLine -match "java.*spring-boot:run"
            } -ErrorAction SilentlyContinue

            $remainingAppProcess = Get-Process | Where-Object {
                $_.CommandLine -match "java.*AetherisRagApplication"
            } -ErrorAction SilentlyContinue

            if ($remainingMvnProcess -or $remainingAppProcess) {
                Write-Warning "进程仍在运行，强制关闭..."
                if ($remainingMvnProcess) { $remainingMvnProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
                if ($remainingAppProcess) { $remainingAppProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
                Start-Sleep -Seconds 1
            }
        }

        if ($backendJobs) {
            Write-Info "找到后端 Job:"
            $backendJobs | ForEach-Object {
                Write-Host "  - Job ID: $($_.Id)" -ForegroundColor Cyan
            }

            # 停止 Job
            $backendJobs | Stop-Job -PassThru | Remove-Job -Force
        }

        Write-Success "后端已停止"

        # 更新 PID 文件
        Update-PidsFile -Service "backend"

        return $true
    } else {
        Write-Warning "未找到运行中的后端进程"
        return $false
    }
}

function Stop-FrontendService {
    Write-Step "停止前端"

    # 查找前端进程
    # 分别查找 npm 和 node 进程
    $npmProcess = Get-Process | Where-Object {
        $_.CommandLine -match "npm.*dev"
    } -ErrorAction SilentlyContinue

    $nodeProcess = Get-Process | Where-Object {
        $_.CommandLine -match "node.*vite"
    } -ErrorAction SilentlyContinue

    # 如果没找到，尝试查找 PowerShell Job
    $frontendJobs = Get-Job | Where-Object {
        $_.Command -match "npm run dev"
    } -ErrorAction SilentlyContinue

    if ($npmProcess -or $nodeProcess -or $frontendJobs) {
        if ($npmProcess -or $nodeProcess) {
            Write-Info "找到前端进程:"
            if ($npmProcess) {
                $npmProcess | ForEach-Object {
                    Write-Host "  - npm 进程: $($_.Id)" -ForegroundColor Cyan
                }
            }
            if ($nodeProcess) {
                $nodeProcess | ForEach-Object {
                    Write-Host "  - node 进程 (Vite): $($_.Id)" -ForegroundColor Cyan
                }
            }

            # 优雅关闭
            Write-Info "正在优雅关闭..."
            if ($npmProcess) { $npmProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
            if ($nodeProcess) { $nodeProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
            Start-Sleep -Seconds 2

            # 检查并强制关闭
            $remainingNpmProcess = Get-Process | Where-Object {
                $_.CommandLine -match "npm.*dev"
            } -ErrorAction SilentlyContinue

            $remainingNodeProcess = Get-Process | Where-Object {
                $_.CommandLine -match "node.*vite"
            } -ErrorAction SilentlyContinue

            if ($remainingNpmProcess -or $remainingNodeProcess) {
                Write-Warning "进程仍在运行，强制关闭..."
                if ($remainingNpmProcess) { $remainingNpmProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
                if ($remainingNodeProcess) { $remainingNodeProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
                Start-Sleep -Seconds 1
            }
        }

        if ($frontendJobs) {
            Write-Info "找到前端 Job:"
            $frontendJobs | ForEach-Object {
                Write-Host "  - Job ID: $($_.Id)" -ForegroundColor Cyan
            }

            # 停止 Job
            $frontendJobs | Stop-Job -PassThru | Remove-Job -Force
        }

        Write-Success "前端已停止"

        # 更新 PID 文件
        Update-PidsFile -Service "frontend"

        return $true
    } else {
        Write-Warning "未找到运行中的前端进程"
        return $false
    }
}

function Stop-DockerServices {
    Write-Step "停止 Docker 服务"

    # 确定使用的命令
    $composeCmd = $null
    $composeTest = docker compose version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $composeCmd = "docker compose"
    } elseif (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        $composeCmd = "docker-compose"
    } else {
        Write-Error "无法找到 Docker Compose 命令"
        return $false
    }

    # 检查 Docker 服务是否在运行
    $composePsOutput = Invoke-Expression "$composeCmd ps" 2>&1
    if ($composePsOutput -match "Up") {
        Invoke-Expression "$composeCmd down"
        Write-Success "Docker 服务已停止"
        return $true
    } else {
        Write-Warning "Docker 服务未运行"
        return $false
    }
}

# ========================================
# PID 文件管理函数
# ========================================

function Update-PidsFile {
    param([string]$Service)

    $pidsPath = Join-Path $PROJECT_ROOT ".pids.json"

    if (Test-Path $pidsPath) {
        $json = Get-Content $pidsPath | ConvertFrom-Json

        if ($Service -eq "backend") {
            $json.backend.pid = $null
            $json.backend.status = "stopped"
            $json.backend.started_at = $null
        } elseif ($Service -eq "frontend") {
            $json.frontend.pid = $null
            $json.frontend.status = "stopped"
            $json.frontend.started_at = $null
        }

        $json | ConvertTo-Json -Depth 10 | Set-Content $pidsPath
    }
}

# ========================================
# 帮助信息
# ========================================

function Show-Help {
    Write-Host @"

用法:
  .\stop-win-ps.ps1 [选项]

选项:
  --frontend-only      仅停止前端服务
  --backend-only       仅停止后端服务
  --all                停止所有服务（前端+后端）
  --docker-only        仅停止 Docker 服务（MySQL + Redis）
  --help, -h           显示此帮助信息

交互模式:
  无参数运行时进入交互模式，选择后自动退出

示例:
  .\stop-win-ps.ps1                    # 进入交互菜单
  .\stop-win-ps.ps1 --frontend-only    # 仅停止前端
  .\stop-win-ps.ps1 --backend-only     # 仅停止后端
  .\stop-win-ps.ps1 --all              # 停止前端和后端

"@ -ForegroundColor Cyan
}

# ========================================
# 交互式菜单
# ========================================

function Show-Menu {
    Write-Host ""
    Write-Info "========================================"
    Write-Info "  Aetheris RAG 系统停止脚本"
    Write-Info "========================================"
    Write-Host ""
    Write-Success "请选择要停止的服务:"
    Write-Host ""
    Write-Host "  1. 停止前端"
    Write-Host "  2. 停止后端"
    Write-Host "  3. 停止所有服务（前端 + 后端）"
    Write-Host "  4. 停止 Docker 服务（MySQL + Redis）"
    Write-Host "  5. 停止所有（包括 Docker）"
    Write-Host ""
    $choice = Read-Host "请输入选项 [1-5]"
    return $choice
}

function Invoke-InteractiveMode {
    $choice = Show-Menu

    switch ($choice) {
        "1" {
            Stop-FrontendService
            Write-Success "前端已停止"
        }
        "2" {
            Stop-BackendService
            Write-Success "后端已停止"
        }
        "3" {
            Write-Info "正在停止所有服务（前端 + 后端）..."
            Stop-BackendService
            Stop-FrontendService
            Write-Success "所有服务已停止"
        }
        "4" {
            Stop-DockerServices
            Write-Success "Docker 服务已停止"
        }
        "5" {
            Write-Info "正在停止所有服务（包括 Docker）..."
            Stop-BackendService
            Stop-FrontendService
            Stop-DockerServices
            Write-Success "所有服务已停止"
        }
        default {
            Write-Error "无效选项，请输入 1-5"
            exit 1
        }
    }
}

# ========================================
# 参数解析
# ========================================

function Invoke-ArgumentMode {
    param([string[]]$Arguments)

    while ($Arguments.Count -gt 0) {
        switch ($Arguments[0]) {
            "--frontend-only" {
                Stop-FrontendService
                exit $LASTEXITCODE
            }
            "--backend-only" {
                Stop-BackendService
                exit $LASTEXITCODE
            }
            "--all" {
                Stop-BackendService
                Stop-FrontendService
                exit 0
            }
            "--docker-only" {
                Stop-DockerServices
                exit $LASTEXITCODE
            }
            {$_ -eq "--help" -or $_ -eq "-h"} {
                Show-Help
                exit 0
            }
            default {
                Write-Error "未知参数: $($Arguments[0])"
                Write-Host ""
                Show-Help
                exit 1
            }
        }
        $Arguments = $Arguments[1..($Arguments.Count - 1)]
    }
}

# ========================================
# 主流程
# ========================================

# 解析参数或进入交互模式
if ($args.Count -eq 0) {
    Invoke-InteractiveMode
} else {
    Invoke-ArgumentMode -Arguments $args
}
