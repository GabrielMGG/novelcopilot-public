[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$ProjectId,

    [ValidateNotNullOrEmpty()]
    [string]$Region = "southamerica-east1",

    [ValidateNotNullOrEmpty()]
    [string]$ServiceName = "novelcopilot-staging",

    [ValidateNotNullOrEmpty()]
    [string]$EnvironmentFile = "backend/cloud-run.env.yaml"
)

$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$environmentPath = if ([IO.Path]::IsPathRooted($EnvironmentFile)) {
    $EnvironmentFile
} else {
    Join-Path $repositoryRoot $EnvironmentFile
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "Google Cloud CLI (gcloud) nao foi encontrado. Instale-o e autentique antes de executar este script."
}

if (-not (Test-Path -LiteralPath $environmentPath -PathType Leaf)) {
    throw "Arquivo de variaveis nao encontrado: $environmentPath. Copie backend/cloud-run.env.example.yaml para backend/cloud-run.env.yaml e preencha os valores."
}

$environmentText = Get-Content -LiteralPath $environmentPath -Raw
$requiredVariables = @(
    "SPRING_DATASOURCE_URL",
    "DB_USER",
    "DB_PASSWORD",
    "SUPABASE_URL",
    "SUPABASE_ANON_KEY",
    "APP_FRONTEND_URL",
    "CLOUDFLARE_ACCOUNT_ID",
    "CLOUDFLARE_AUTH_TOKEN"
)

foreach ($variable in $requiredVariables) {
    if ($environmentText -notmatch "(?m)^$([regex]::Escape($variable))\s*:\s*.+$") {
        throw "A variavel obrigatoria $variable esta ausente ou vazia em $environmentPath."
    }
}

if ($environmentText -match "(?i)substitua|seu-projeto|host:porta|\.example") {
    throw "O arquivo de variaveis ainda contem valores de exemplo. Substitua-os antes da publicacao."
}

$sourcePath = Join-Path $repositoryRoot "backend"
$arguments = @(
    "run", "deploy", $ServiceName,
    "--project=$ProjectId",
    "--region=$Region",
    "--source=$sourcePath",
    "--env-vars-file=$environmentPath",
    "--execution-environment=gen2",
    "--cpu=1",
    "--memory=1Gi",
    "--min=0",
    "--max=1",
    "--concurrency=20",
    "--timeout=300",
    "--cpu-throttling",
    "--allow-unauthenticated",
    "--quiet"
)

if (-not $PSCmdlet.ShouldProcess(
        "$ServiceName no projeto $ProjectId ($Region)",
        "Construir e publicar o backend no Google Cloud Run"
    )) {
    return
}

Push-Location $repositoryRoot
try {
    & gcloud @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "A publicacao no Cloud Run falhou com codigo $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
