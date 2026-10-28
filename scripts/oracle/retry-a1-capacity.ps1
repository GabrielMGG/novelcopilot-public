[CmdletBinding()]
param(
    [string]$StackId = $env:OCI_NOVELCOPILOT_STACK_ID,
    [string]$Profile = 'DEFAULT',
    [string]$Region = 'sa-saopaulo-1',
    [int]$IntervalMinutes = 10,
    [int]$MaxAttempts = 144,
    [int]$MaxHours = 24,
    [int]$PollSeconds = 20,
    [int]$JobTimeoutMinutes = 30,
    [int]$ThrottleRetrySeconds = 300,
    [int]$ThrottleMaxRetries = 0,
    [string]$OciPath = 'C:\Program Files (x86)\Oracle\oci_cli\oci.exe',
    [string]$LogDirectory = (Join-Path $env:LOCALAPPDATA 'NovelCopilot\oracle-capacity-retry'),
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$env:OCI_CLI_SUPPRESS_FILE_PERMISSIONS_WARNING = 'True'
$env:SUPPRESS_LABEL_WARNING = 'True'

function Write-RunLog {
    param([string]$Message)

    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    if (-not $DryRun) {
        Add-Content -LiteralPath $script:LogPath -Value $line -Encoding UTF8
    }
}

function Invoke-OciJson {
    param([string[]]$Arguments)

    $quotedArguments = @($Arguments | ForEach-Object {
        '"' + ([string]$_).Replace('"', '\"') + '"'
    }) -join ' '
    $throttleCount = 0
    while ($true) {
        $startInfo = New-Object System.Diagnostics.ProcessStartInfo
        $startInfo.FileName = $OciPath
        $startInfo.Arguments = $quotedArguments
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $startInfo
        if (-not $process.Start()) {
            throw 'Nao foi possivel iniciar a OCI CLI.'
        }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $stdout = $stdoutTask.Result
        $stderr = $stderrTask.Result
        if ($process.ExitCode -eq 0) {
            try {
                return ($stdout | ConvertFrom-Json)
            }
            catch {
                throw "A OCI CLI nao retornou JSON valido. Saida: $stdout Avisos: $stderr"
            }
        }

        $isThrottled = $stderr -match '(?i)TooManyRequests|Too many requests|"status"\s*:\s*429'
        if ($isThrottled) {
            $throttleCount++
            if ($ThrottleMaxRetries -gt 0 -and $throttleCount -gt $ThrottleMaxRetries) {
                throw "OCI CLI continuou limitada por 429 apos $ThrottleMaxRetries repeticoes."
            }
            if ($script:EndTime -and (Get-Date).AddSeconds($ThrottleRetrySeconds) -ge $script:EndTime) {
                throw 'Limite total de duracao atingido durante a espera por 429.'
            }
            $limitText = if ($ThrottleMaxRetries -eq 0) { 'ate o limite total' } else { "$throttleCount/$ThrottleMaxRetries" }
            Write-RunLog "Oracle limitou os jobs (429). Nova tentativa da chamada em $ThrottleRetrySeconds segundos ($limitText)."
            Start-Sleep -Seconds $ThrottleRetrySeconds
            continue
        }
        throw "OCI CLI falhou (codigo $($process.ExitCode)): $stderr"
    }
}

function Wait-ResourceManagerJob {
    param([Parameter(Mandatory)][string]$JobId)

    $deadline = (Get-Date).AddMinutes($JobTimeoutMinutes)
    do {
        $job = Invoke-OciJson @(
            'resource-manager', 'job', 'get',
            '--job-id', $JobId,
            '--profile', $Profile,
            '--region', $Region,
            '--output', 'json'
        )
        $state = [string]$job.data.'lifecycle-state'
        Write-RunLog "Job ${JobId}: $state"
        if ($state -in @('SUCCEEDED', 'FAILED', 'CANCELED')) {
            return $job
        }
        if ((Get-Date) -ge $deadline) {
            throw "Tempo limite de $JobTimeoutMinutes minutos excedido no job $JobId."
        }
        Start-Sleep -Seconds $PollSeconds
    } while ($true)
}

function Get-ResourceManagerJobLog {
    param([Parameter(Mandatory)][string]$JobId)

    $result = Invoke-OciJson @(
        'resource-manager', 'job', 'get-job-logs-content',
        '--job-id', $JobId,
        '--profile', $Profile,
        '--region', $Region,
        '--output', 'json'
    )
    if ($result.data -is [string]) {
        return [string]$result.data
    }

    $messages = foreach ($entry in @($result.data)) {
        if ($entry.PSObject.Properties.Name -contains 'message') {
            [string]$entry.message
        }
        else {
            [string]$entry
        }
    }
    return ($messages -join [Environment]::NewLine)
}

function New-ResourceManagerJob {
    param(
        [Parameter(Mandatory)][ValidateSet('Plan', 'Apply')][string]$Type,
        [string]$PlanJobId
    )

    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    if ($Type -eq 'Plan') {
        $arguments = @(
            'resource-manager', 'job', 'create-plan-job',
            '--stack-id', $StackId,
            '--display-name', "novelcopilot-a1-auto-plan-$stamp",
            '--profile', $Profile,
            '--region', $Region,
            '--output', 'json'
        )
    }
    else {
        if ([string]::IsNullOrWhiteSpace($PlanJobId)) {
            throw 'O Apply exige o OCID do Plan aprovado.'
        }
        $arguments = @(
            'resource-manager', 'job', 'create-apply-job',
            '--stack-id', $StackId,
            '--execution-plan-strategy', 'FROM_PLAN_JOB_ID',
            '--execution-plan-job-id', $PlanJobId,
            '--display-name', "novelcopilot-a1-auto-apply-$stamp",
            '--profile', $Profile,
            '--region', $Region,
            '--output', 'json'
        )
    }

    $created = Invoke-OciJson $arguments
    return [string]$created.data.id
}

function Assert-PlanIsSafe {
    param([Parameter(Mandatory)][string]$PlanLog)

    if ($PlanLog -match 'No changes\.') {
        return 'NoChanges'
    }
    if ($PlanLog -notmatch 'Plan:\s+1 to add,\s+0 to change,\s+0 to destroy\.') {
        throw 'Plan recusado: era esperado exatamente 1 recurso a adicionar, 0 a alterar e 0 a destruir.'
    }
    return 'OneAddOnly'
}

if ($IntervalMinutes -lt 1 -or $MaxAttempts -lt 1 -or $MaxHours -lt 1 -or
    $PollSeconds -lt 5 -or $JobTimeoutMinutes -lt 1 -or
    $ThrottleRetrySeconds -lt 1 -or $ThrottleMaxRetries -lt 0) {
    throw 'Os intervalos e limites devem ser numeros positivos; PollSeconds deve ser pelo menos 5.'
}

if (-not (Test-Path -LiteralPath $OciPath -PathType Leaf)) {
    throw "OCI CLI nao encontrada em: $OciPath"
}

if ($DryRun) {
    Write-Host 'SIMULACAO: nenhuma chamada sera feita a Oracle.'
    Write-Host "Stack: $(if ($StackId) { $StackId } else { '<sera lida de OCI_NOVELCOPILOT_STACK_ID>' })"
    Write-Host "Regiao: $Region | Perfil: $Profile"
    Write-Host "Limites: $MaxAttempts tentativas, $IntervalMinutes min entre tentativas, maximo de $MaxHours h."
    $throttleText = if ($ThrottleMaxRetries -eq 0) { 'ate o limite total' } else { "$ThrottleMaxRetries repeticoes" }
    Write-Host "Limite 429: $throttleText, com espera de $ThrottleRetrySeconds segundos."
    Write-Host 'Fluxo: Plan -> exigir 1 add/0 change/0 destroy -> Apply -> parar ao criar ou em erro inesperado.'
    exit 0
}

if ([string]::IsNullOrWhiteSpace($StackId) -or $StackId -notmatch '^ocid1\.ormstack\.') {
    throw 'Defina OCI_NOVELCOPILOT_STACK_ID com o OCID valido da stack antes da execucao real.'
}

New-Item -ItemType Directory -Path $LogDirectory -Force | Out-Null
$script:LogPath = Join-Path $LogDirectory ("retry-{0}.log" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
$script:EndTime = (Get-Date).AddHours($MaxHours)
Write-RunLog "Inicio. Stack=$StackId; regiao=$Region; maximo=$MaxAttempts tentativas/$MaxHours h."

for ($attempt = 1; $attempt -le $MaxAttempts -and (Get-Date) -lt $script:EndTime; $attempt++) {
    Write-RunLog "Tentativa $attempt de ${MaxAttempts}: criando Plan."
    $planJobId = New-ResourceManagerJob -Type Plan
    $planJob = Wait-ResourceManagerJob -JobId $planJobId
    $planLog = Get-ResourceManagerJobLog -JobId $planJobId
    if ([string]$planJob.data.'lifecycle-state' -ne 'SUCCEEDED') {
        throw "Plan falhou. Consulte $planJobId. A automacao foi interrompida."
    }

    $planResult = Assert-PlanIsSafe -PlanLog $planLog
    if ($planResult -eq 'NoChanges') {
        Write-RunLog 'A stack nao tem mudancas: a instancia ja esta provisionada. Encerrando com sucesso.'
        exit 0
    }

    Write-RunLog "Plan seguro ($planJobId). Criando Apply."
    $applyJobId = New-ResourceManagerJob -Type Apply -PlanJobId $planJobId
    $applyJob = Wait-ResourceManagerJob -JobId $applyJobId
    $applyLog = Get-ResourceManagerJobLog -JobId $applyJobId
    if ([string]$applyJob.data.'lifecycle-state' -eq 'SUCCEEDED') {
        Write-RunLog "SUCESSO: instancia criada pelo job $applyJobId."
        exit 0
    }

    if ($applyLog -notmatch '(?i)out of (host )?capacity') {
        throw "Apply falhou por motivo diferente de capacidade. Consulte $applyJobId. Automacao interrompida."
    }

    Write-RunLog "Sem capacidade no job $applyJobId."
    if ($attempt -lt $MaxAttempts -and (Get-Date).AddMinutes($IntervalMinutes) -lt $script:EndTime) {
        Write-RunLog "Nova tentativa em $IntervalMinutes minutos. Use Ctrl+C para parar."
        Start-Sleep -Seconds ($IntervalMinutes * 60)
    }
}

Write-RunLog 'Limite de tentativas ou de duracao atingido sem capacidade disponivel.'
exit 2
