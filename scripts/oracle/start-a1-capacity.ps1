[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$StackId,
    [int]$IntervalMinutes = 10,
    [int]$MaxAttempts = 144,
    [int]$MaxHours = 24,
    [string]$RunDirectory = (Join-Path $env:LOCALAPPDATA 'NovelCopilot\oracle-capacity-retry'),
    [switch]$NoMonitor,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$workerScript = Join-Path $PSScriptRoot 'retry-a1-capacity.ps1'
$monitorScript = Join-Path $PSScriptRoot 'show-a1-capacity-status.ps1'
if ($StackId -notmatch '^ocid1\.ormstack\.') { throw 'OCID da stack invalido.' }

if ($DryRun) {
    "SIMULACAO: iniciaria a stack $StackId a cada $IntervalMinutes minutos por ate $MaxHours horas."
    exit 0
}

New-Item -ItemType Directory -Path $RunDirectory -Force | Out-Null
$pidPath = Join-Path $RunDirectory 'active.pid'
$running = $false
if (Test-Path -LiteralPath $pidPath) {
    $rawPid = (Get-Content -LiteralPath $pidPath -Raw).Trim()
    $oldPid = if ($rawPid.StartsWith('{')) { [int](($rawPid | ConvertFrom-Json).pid) } elseif ($rawPid -match '^\d+$') { [int]$rawPid } else { 0 }
    $running = $oldPid -gt 0 -and $null -ne (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)
}

if (-not $running) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $stdoutPath = Join-Path $RunDirectory "background-$stamp.out.log"
    $stderrPath = Join-Path $RunDirectory "background-$stamp.err.log"
    $arguments = @('-NoProfile','-ExecutionPolicy','Bypass','-File',$workerScript,'-StackId',$StackId,'-IntervalMinutes',"$IntervalMinutes",'-MaxAttempts',"$MaxAttempts",'-MaxHours',"$MaxHours",'-ThrottleRetrySeconds','300','-ThrottleMaxRetries','0')
    $worker = Start-Process powershell.exe -ArgumentList $arguments -WindowStyle Hidden -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -PassThru
    @{ pid = $worker.Id; startTime = $worker.StartTime.ToString('o') } | ConvertTo-Json -Compress | Set-Content -LiteralPath $pidPath -Encoding ASCII
}

if (-not $NoMonitor) {
    Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',$monitorScript,'-StackId',$StackId) -WindowStyle Normal | Out-Null
}
