[CmdletBinding()]
param(
    [string]$RunDirectory = (Join-Path $env:LOCALAPPDATA 'NovelCopilot\oracle-capacity-retry'),
    [string]$StackId = $env:OCI_NOVELCOPILOT_STACK_ID,
    [switch]$TextOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-AutomationStatus {
    $pidPath = Join-Path $RunDirectory 'active.pid'
    $processId = 0
    $expectedStart = $null
    if (Test-Path -LiteralPath $pidPath) {
        $rawPid = (Get-Content -LiteralPath $pidPath -Raw).Trim()
        if ($rawPid.StartsWith('{')) {
            $pidData = $rawPid | ConvertFrom-Json
            $processId = [int]$pidData.pid
            $expectedStart = [datetime]$pidData.startTime
        }
        elseif ($rawPid -match '^\d+$') {
            $processId = [int]$rawPid
        }
    }

    $process = if ($processId -gt 0) { Get-Process -Id $processId -ErrorAction SilentlyContinue } else { $null }
    $running = $null -ne $process -and $process.ProcessName -in @('powershell', 'pwsh')
    if ($running -and $expectedStart) {
        $running = [math]::Abs(($process.StartTime - $expectedStart).TotalSeconds) -lt 2
    }

    $latestLog = Get-ChildItem -LiteralPath $RunDirectory -Filter 'retry-*.log' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $lines = if ($latestLog) { @(Get-Content -LiteralPath $latestLog.FullName -Tail 150) } else { @('Nenhum log encontrado.') }
    $lastLine = if ($lines.Count -gt 0) { [string]$lines[-1] } else { 'Sem eventos.' }

    [pscustomobject]@{
        Running = $running
        ProcessId = $processId
        ExpectedStart = $expectedStart
        LastLine = $lastLine
        LogPath = if ($latestLog) { $latestLog.FullName } else { '' }
        Text = $lines -join [Environment]::NewLine
    }
}

if ($TextOnly) {
    $status = Get-AutomationStatus
    "RUNNING=$($status.Running)"
    "PID=$($status.ProcessId)"
    "LAST=$($status.LastLine)"
    "LOG=$($status.LogPath)"
    exit 0
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$form = New-Object System.Windows.Forms.Form
$form.Text = 'Oracle A1 - Tentativas automaticas'
$form.Size = New-Object System.Drawing.Size(900, 600)
$form.StartPosition = 'CenterScreen'

$statusLabel = New-Object System.Windows.Forms.Label
$statusLabel.Location = New-Object System.Drawing.Point(16, 16)
$statusLabel.Size = New-Object System.Drawing.Size(850, 28)
$statusLabel.Font = New-Object System.Drawing.Font('Segoe UI', 12, [System.Drawing.FontStyle]::Bold)
$form.Controls.Add($statusLabel)

$lastLabel = New-Object System.Windows.Forms.Label
$lastLabel.Location = New-Object System.Drawing.Point(16, 50)
$lastLabel.Size = New-Object System.Drawing.Size(850, 44)
$form.Controls.Add($lastLabel)

$logBox = New-Object System.Windows.Forms.TextBox
$logBox.Location = New-Object System.Drawing.Point(16, 100)
$logBox.Size = New-Object System.Drawing.Size(850, 400)
$logBox.Multiline = $true
$logBox.ReadOnly = $true
$logBox.ScrollBars = 'Vertical'
$logBox.Font = New-Object System.Drawing.Font('Consolas', 9)
$form.Controls.Add($logBox)

$openButton = New-Object System.Windows.Forms.Button
$openButton.Text = 'Abrir pasta de logs'
$openButton.Location = New-Object System.Drawing.Point(16, 515)
$openButton.Size = New-Object System.Drawing.Size(160, 32)
$openButton.Add_Click({ Start-Process explorer.exe -ArgumentList $RunDirectory })
$form.Controls.Add($openButton)

$startButton = New-Object System.Windows.Forms.Button
$startButton.Text = 'Iniciar novamente'
$startButton.Location = New-Object System.Drawing.Point(190, 515)
$startButton.Size = New-Object System.Drawing.Size(160, 32)
$startButton.Add_Click({
    $current = Get-AutomationStatus
    if ($current.Running) {
        [System.Windows.Forms.MessageBox]::Show('A automacao ja esta ativa.', 'Oracle A1') | Out-Null
        return
    }
    if ([string]::IsNullOrWhiteSpace($StackId)) {
        [System.Windows.Forms.MessageBox]::Show('O OCID da stack nao foi informado ao monitor.', 'Oracle A1') | Out-Null
        return
    }
    $launcher = Join-Path $PSScriptRoot 'start-a1-capacity.ps1'
    Start-Process powershell.exe -WindowStyle Hidden -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',$launcher,'-StackId',$StackId,'-NoMonitor') | Out-Null
})
$form.Controls.Add($startButton)

$stopButton = New-Object System.Windows.Forms.Button
$stopButton.Text = 'Interromper automacao'
$stopButton.Location = New-Object System.Drawing.Point(365, 515)
$stopButton.Size = New-Object System.Drawing.Size(180, 32)
$stopButton.Add_Click({
    $current = Get-AutomationStatus
    if (-not $current.Running) {
        [System.Windows.Forms.MessageBox]::Show('A automacao nao esta ativa.', 'Oracle A1') | Out-Null
        return
    }
    $answer = [System.Windows.Forms.MessageBox]::Show(
        "Interromper o processo $($current.ProcessId)?",
        'Confirmar interrupcao',
        [System.Windows.Forms.MessageBoxButtons]::YesNo,
        [System.Windows.Forms.MessageBoxIcon]::Warning
    )
    if ($answer -eq [System.Windows.Forms.DialogResult]::Yes) {
        Stop-Process -Id $current.ProcessId
    }
})
$form.Controls.Add($stopButton)

function Update-Window {
    $current = Get-AutomationStatus
    if ($current.Running) {
        $statusLabel.Text = "ATIVA - processo $($current.ProcessId)"
        $statusLabel.ForeColor = [System.Drawing.Color]::DarkGreen
    }
    else {
        $statusLabel.Text = 'PARADA'
        $statusLabel.ForeColor = [System.Drawing.Color]::DarkRed
    }
    $lastLabel.Text = "Ultimo evento: $($current.LastLine)"
    $logBox.Text = $current.Text
    $logBox.SelectionStart = $logBox.TextLength
    $logBox.ScrollToCaret()
}

$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 5000
$timer.Add_Tick({ Update-Window })
$timer.Start()
Update-Window
[void]$form.ShowDialog()
