# Aetheris RAG 系统启动脚本（Windows PowerShell 版本）
# 用途: 支持命令行参数和交互式菜单的选择性启动
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
# 环境检查函数
# ========================================

function Test-Environment {
    Write-Step "环境检查"

    # 检查 Java
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Error "Java 未安装"
        return $false
    }

    # 检查 Java 版本
    $javaVersionOutput = java -version 2>&1 | Out-String
    if ($javaVersionOutput -match 'version "?(\d+)\.?') {
        $majorVersion = [int]$matches[1]
        if ($majorVersion -ne 21) {
            Write-Warning "Java 版本不匹配: 当前版本 $majorVersion, 需要 Java 21"
            Write-Info "请设置 JAVA_HOME 环境变量指向 Java 21 安装目录"
            Write-Info "或在脚本中手动配置 JAVA_HOME"
            return $false
        }
        Write-Success "Java 版本正确: Java 21"
    }

    # 检查 Maven
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Error "Maven 未安装"
        return $false
    }
    $mavenVersion = mvn -version 2>&1 | Select-Object -First 1
    Write-Success "Maven 版本: $mavenVersion"

    # 检查 Node.js
    if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
        Write-Error "Node.js 未安装"
        return $false
    }
    $nodeVersion = node -v
    Write-Success "Node.js 版本: $nodeVersion"

    # 检查 Docker
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker 未安装"
        return $false
    }
    $dockerVersion = docker --version 2>&1
    Write-Success "Docker 版本: $dockerVersion"

    # 检查 Docker Compose（支持新版 docker compose 和旧版 docker-compose）
    $dockerComposeCmd = $null
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        # 尝试新版命令（docker compose）
        $composeTest = docker compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $dockerComposeCmd = "docker compose"
            $composeVersion = docker compose version 2>&1
            Write-Success "Docker Compose 版本: $composeVersion"
        } else {
            # 尝试旧版命令（docker-compose）
            if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
                $dockerComposeCmd = "docker-compose"
                $composeVersion = docker-compose --version 2>&1
                Write-Success "Docker Compose 版本: $composeVersion"
            } else {
                Write-Error "Docker Compose 未安装"
                return $false
            }
        }
    }

    Write-Host ""
    return $true
}

# ========================================
# .env 文件检查函数
# ========================================

function Initialize-EnvFile {
    $envPath = Join-Path $PROJECT_ROOT ".env"

    if (-not (Test-Path $envPath)) {
        Write-Warning ".env 文件不存在，从 .env.example 创建..."
        $envExamplePath = Join-Path $PROJECT_ROOT ".env.example"
        if (Test-Path $envExamplePath) {
            Copy-Item $envExamplePath $envPath
            Write-Success ".env 文件已创建"
            Write-Warning "请编辑 .env 文件，配置 ZHIPU_API_KEY 等关键参数！"
        } else {
            Write-Error ".env.example 文件不存在，无法创建 .env 文件"
            return $false
        }
    } else {
        Write-Success ".env 文件已存在"
    }

    Write-Host ""
    return $true
}

# ========================================
# 加载环境变量
# ========================================

function Import-EnvVariables {
    param([string]$EnvPath)

    if (-not (Test-Path $EnvPath)) {
        Write-Warning ".env 文件不存在，使用 application.yml 默认配置"
        return
    }

    Write-Info "加载环境变量..."

    Get-Content $EnvPath | ForEach-Object {
        # 跳过注释和空行
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
            return
        }

        # 匹配 KEY=VALUE 格式
        if ($_ -match '^(.+?)=(.+)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()

            # 去除引号
            $value = $value -replace '^"|"$', ''

            # 设置环境变量（仅当前进程）
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }

    Write-Success "环境变量已加载"
}

# ========================================
# 启动函数
# ========================================

function Start-DockerServices {
    Write-Step "启动 Docker 服务"

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

    # 检查是否已运行
    $composePsOutput = Invoke-Expression "$composeCmd ps" 2>&1
    if ($composePsOutput -match "Up") {
        Write-Warning "Docker 服务已在运行"
        return $true
    }

    Write-Info "正在启动 Docker Compose 服务..."
    Invoke-Expression "$composeCmd up -d"

    # 等待服务启动
    Start-Sleep -Seconds 5

    # 检查服务状态
    $composePsOutput = Invoke-Expression "$composeCmd ps" 2>&1
    if ($composePsOutput -match "Up") {
        Write-Info "检查服务健康状态..."

        # 快速检查（最多等待 10 秒）
        for ($i = 1; $i -le 2; $i++) {
            $composePsOutput = Invoke-Expression "$composeCmd ps" 2>&1
            if ($composePsOutput -match "healthy") {
                Write-Success "基础设施启动成功"
                return $true
            }
            if ($i -eq 1) {
                Write-Info "等待服务就绪..."
            }
            Start-Sleep -Seconds 5
        }

        # 如果仍未健康，显示提示但继续
        $composePsOutput = Invoke-Expression "$composeCmd ps" 2>&1
        if ($composePsOutput -notmatch "healthy") {
            Write-Warning "服务启动中，请稍后检查..."
        }
    } else {
        Write-Error "基础设施启动失败"
        Invoke-Expression "$composeCmd ps"
        return $false
    }

    return $true
}

function Start-BackendService {
    Write-Step "启动后端服务"

    # 检查后端是否已运行
    $backendProcesses = Get-Process | Where-Object {
        $_.MainWindowTitle -match "spring-boot:run|AetherisRagApplication" -or
        $_.Path -like "*rag-backend*.jar"
    }

    if ($backendProcesses) {
        Write-Warning "后端已在运行"
        return $true
    }

    $backendDir = Join-Path $PROJECT_ROOT "backend"
    Set-Location $backendDir

    # 检查后端是否已编译
    $targetDir = Join-Path $backendDir "target"
    $classesDir = Join-Path $targetDir "classes"
    if (-not (Test-Path $targetDir) -or -not (Test-Path $classesDir)) {
        Write-Info "后端未编译，开始编译..."
        mvn clean compile
    }

    # 加载 .env 文件中的环境变量
    $envPath = Join-Path $PROJECT_ROOT ".env"
    Import-EnvVariables -EnvPath $envPath

    # 启动后端（后台运行）
    Write-Info "启动 Spring Boot 应用..."

    $logPath = Join-Path $PROJECT_ROOT "logs\backend.log"
    $logDir = Split-Path $logPath -Parent
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }

    # 使用 Start-Job 后台运行 Maven
    $envPath = Join-Path $PROJECT_ROOT ".env"
    $job = Start-Job -ScriptBlock {
        param($ProjectRoot, $LogPath, $EnvPath)
        Set-Location $ProjectRoot\backend

        # 加载环境变量
        if (Test-Path $EnvPath) {
            Get-Content $EnvPath | ForEach-Object {
                if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
                if ($_ -match '^(.+?)=(.+)$') {
                    $key = $matches[1].Trim()
                    $value = $matches[2].Trim() -replace '^"|"$', ''
                    [Environment]::SetEnvironmentVariable($key, $value, "Process")
                }
            }
        }

        mvn spring-boot:run *> $LogPath
    } -ArgumentList $PROJECT_ROOT, $logPath, $envPath

    $BACKEND_PID = $job.Id
    $STARTED_AT = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

    # 更新 PID 文件
    Update-PidsFile -Service "backend" -Pid $BACKEND_PID -Status "running" -StartedAt $STARTED_AT

    Write-Success "后端启动中..."
    Write-Info "📄 查看日志: Get-Content $logPath -Wait"
    Write-Info "   或使用: tail -f $logPath (如果有 Git Bash)"

    # 等待后端启动
    Write-Info "等待后端启动 (10秒)..."
    Start-Sleep -Seconds 10

    # 检测并显示进程信息
    $mvnProcess = Get-Process | Where-Object {
        $_.CommandLine -match "java.*spring-boot:run"
    } -ErrorAction SilentlyContinue

    $appProcess = Get-Process | Where-Object {
        $_.CommandLine -match "java.*AetherisRagApplication"
    } -ErrorAction SilentlyContinue

    if ($mvnProcess -or $appProcess) {
        Write-Info "后端进程信息:"
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
    }

    # 检查后端是否启动成功
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Success "后端启动成功"
        }
    } catch {
        Write-Warning "后端可能还在启动中，请检查日志"
    }

    Set-Location $PROJECT_ROOT
    return $true
}

function Start-FrontendService {
    Write-Step "启动前端服务"

    # 检查前端是否已运行
    $frontendProcesses = Get-Process | Where-Object {
        $_.MainWindowTitle -match "vite|npm.*dev" -or
        $_.CommandLine -match "vite.*frontend|npm.*dev"
    }

    if ($frontendProcesses) {
        Write-Warning "前端已在运行"
        return $true
    }

    $frontendDir = Join-Path $PROJECT_ROOT "frontend"
    Set-Location $frontendDir

    # 检查 node_modules
    if (-not (Test-Path "node_modules")) {
        Write-Info "node_modules 不存在，开始安装依赖..."
        npm install
    }

    # 启动前端（后台运行）
    Write-Info "启动 Vite 开发服务器..."

    $logPath = Join-Path $PROJECT_ROOT "logs\frontend.log"
    $logDir = Split-Path $logPath -Parent
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }

    # 使用 Start-Job 后台运行 npm
    $envPath = Join-Path $PROJECT_ROOT ".env"
    $job = Start-Job -ScriptBlock {
        param($ProjectRoot, $LogPath, $EnvPath)
        Set-Location $ProjectRoot\frontend

        # 加载环境变量（前端可能也需要）
        if (Test-Path $EnvPath) {
            Get-Content $EnvPath | ForEach-Object {
                if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
                if ($_ -match '^(.+?)=(.+)$') {
                    $key = $matches[1].Trim()
                    $value = $matches[2].Trim() -replace '^"|"$', ''
                    [Environment]::SetEnvironmentVariable($key, $value, "Process")
                }
            }
        }

        npm run dev *> $LogPath
    } -ArgumentList $PROJECT_ROOT, $logPath, $envPath

    $FRONTEND_PID = $job.Id
    $STARTED_AT = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

    # 更新 PID 文件
    Update-PidsFile -Service "frontend" -Pid $FRONTEND_PID -Status "running" -StartedAt $STARTED_AT

    Write-Success "前端启动中..."
    Write-Info "📄 查看日志: Get-Content $logPath -Wait"
    Write-Info "   或使用: tail -f $logPath (如果有 Git Bash)"

    # 等待前端启动
    Write-Info "等待前端启动 (5秒)..."
    Start-Sleep -Seconds 5

    # 检测并显示进程信息
    $npmProcess = Get-Process | Where-Object {
        $_.CommandLine -match "npm.*dev"
    } -ErrorAction SilentlyContinue

    $nodeProcess = Get-Process | Where-Object {
        $_.CommandLine -match "node.*vite"
    } -ErrorAction SilentlyContinue

    if ($npmProcess -or $nodeProcess) {
        Write-Info "前端进程信息:"
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
    }

    # 检查前端是否启动成功
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Success "前端启动成功"
        }
    } catch {
        Write-Warning "前端可能还在启动中，请稍后访问"
    }

    Set-Location $PROJECT_ROOT
    return $true
}

# ========================================
# PID 文件管理函数
# ========================================

function Update-PidsFile {
    param(
        [string]$Service,
        [int]$Pid,
        [string]$Status,
        [string]$StartedAt
    )

    $pidsPath = Join-Path $PROJECT_ROOT ".pids.json"

    if (Test-Path $pidsPath) {
        $json = Get-Content $pidsPath | ConvertFrom-Json

        if ($Service -eq "backend") {
            $json.backend.pid = $Pid
            $json.backend.status = $Status
            $json.backend.started_at = $StartedAt
        } elseif ($Service -eq "frontend") {
            $json.frontend.pid = $Pid
            $json.frontend.status = $Status
            $json.frontend.started_at = $StartedAt
        }

        $json | ConvertTo-Json -Depth 10 | Set-Content $pidsPath
    }
}

function Initialize-PidsFile {
    $pidsPath = Join-Path $PROJECT_ROOT ".pids.json"

    if (-not (Test-Path $pidsPath)) {
        $initialJson = @{
            backend = @{
                pid = $null
                status = "stopped"
                started_at = $null
            }
            frontend = @{
                pid = $null
                status = "stopped"
                started_at = $null
            }
        }

        $initialJson | ConvertTo-Json -Depth 10 | Set-Content $pidsPath
    }
}

# ========================================
# 帮助信息
# ========================================

function Show-Help {
    Write-Host @"

用法:
  .\start-win-ps.ps1 [选项]

选项:
  --frontend-only      仅启动前端服务
  --backend-only       仅启动后端服务
  --docker-only        仅启动 Docker 服务（MySQL + Redis）
  --all                启动所有服务（前端+后端+Docker）
  --help, -h           显示此帮助信息

交互模式:
  无参数运行时进入交互模式，可选择要启动的服务

示例:
  .\start-win-ps.ps1                    # 进入交互菜单
  .\start-win-ps.ps1 --frontend-only    # 仅启动前端
  .\start-win-ps.ps1 --backend-only     # 仅启动后端
  .\start-win-ps.ps1 --all              # 启动所有服务

"@ -ForegroundColor Cyan
}

# ========================================
# 交互式菜单
# ========================================

function Show-Menu {
    Write-Host ""
    Write-Info "========================================"
    Write-Info "  Aetheris RAG 系统启动脚本"
    Write-Info "========================================"
    Write-Host ""
    Write-Success "请选择要启动的服务:"
    Write-Host ""
    Write-Host "  1. 启动前端"
    Write-Host "  2. 启动后端"
    Write-Host "  3. 启动所有服务（前端 + 后端）"
    Write-Host "  4. 启动 Docker 服务（MySQL + Redis）"
    Write-Host "  5. 启动所有（包括 Docker）"
    Write-Host ""
    $choice = Read-Host "请输入选项 [1-5]"
    return $choice
}

function Invoke-InteractiveMode {
    $choice = Show-Menu

    switch ($choice) {
        "1" {
            if (Test-Environment) {
                Start-FrontendService
                Write-Host ""
                Write-Success "前端启动完成"
                Write-Info "🌐 访问地址: http://localhost:5173"
            }
        }
        "2" {
            if (Test-Environment) {
                Start-BackendService
                Write-Host ""
                Write-Success "后端启动完成"
                Write-Info "🔧 API 地址: http://localhost:8080"
            }
        }
        "3" {
            if (Test-Environment) {
                Start-BackendService
                Start-FrontendService
                Write-Host ""
                Write-Success "所有服务启动完成"
                Write-Info "🌐 前端: http://localhost:5173"
                Write-Info "🔧 后端: http://localhost:8080"
            }
        }
        "4" {
            if (Test-Environment) {
                Initialize-EnvFile
                Start-DockerServices
                Write-Host ""
                Write-Success "Docker 服务启动完成"
            }
        }
        "5" {
            if (Test-Environment) {
                Initialize-EnvFile
                Start-DockerServices
                Start-BackendService
                Start-FrontendService
                Write-Host ""
                Write-Success "========================================"
                Write-Success "  ✅ 所有服务启动完成！"
                Write-Success "========================================"
                Write-Host ""
                Write-Info "🌐 前端访问地址: http://localhost:5173"
                Write-Info "🔧 后端 API 地址: http://localhost:8080"
                Write-Host ""
                Write-Warning "📝 查看日志:"
                Write-Host "  - 后端: Get-Content $PROJECT_ROOT\logs\backend.log -Wait"
                Write-Host "  - 前端: Get-Content $PROJECT_ROOT\logs\frontend.log -Wait"
                Write-Host "  - Docker: docker compose logs -f"
                Write-Host ""
                Write-Warning "🛑 停止服务:"
                Write-Host "  - 停止所有: .\stop-win-ps.ps1"
                Write-Host "  - 查看进程状态: Get-Content $PROJECT_ROOT\.pids.json | ConvertFrom-Json | ConvertTo-Json"
            }
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
                if (Test-Environment) {
                    Start-FrontendService
                    exit $LASTEXITCODE
                }
                exit 1
            }
            "--backend-only" {
                if (Test-Environment) {
                    Start-BackendService
                    exit $LASTEXITCODE
                }
                exit 1
            }
            "--docker-only" {
                if (Test-Environment) {
                    Initialize-EnvFile
                    Start-DockerServices
                    exit $LASTEXITCODE
                }
                exit 1
            }
            "--all" {
                if (Test-Environment) {
                    Initialize-EnvFile
                    Start-DockerServices
                    Start-BackendService
                    Start-FrontendService
                    Write-Host ""
                    Write-Success "========================================"
                    Write-Success "  ✅ 所有服务启动完成！"
                    Write-Success "========================================"
                    Write-Host ""
                    Write-Info "🌐 前端访问地址: http://localhost:5173"
                    Write-Info "🔧 后端 API 地址: http://localhost:8080"
                    exit 0
                }
                exit 1
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

# 初始化 PID 文件
Initialize-PidsFile

# 解析参数或进入交互模式
if ($args.Count -eq 0) {
    Invoke-InteractiveMode
} else {
    Invoke-ArgumentMode -Arguments $args
}
