# Guia de manutenção e execução local

## Pré-requisitos

- Java 21.
- Node.js 22.
- MySQL acessível pelo backend.
- Maven Wrapper incluído; Maven global é opcional.

O terminal e o IntelliJ precisam usar o mesmo JDK. Confirme com `java -version`
e, dentro de `backend`, `.\mvnw.cmd -version`.

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

Copie `frontend/.env.example` para um arquivo local não versionado:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_SUPABASE_URL=https://seu-projeto.supabase.co
VITE_SUPABASE_ANON_KEY=chave-publica-anon
```

`VITE_API_BASE_URL` é o único nome aceito para a URL do backend.

## Backend

Configure as variáveis no ambiente do processo que inicia o backend. Variáveis
do IntelliJ não aparecem automaticamente no ambiente global do Windows.

### Banco e autenticação

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/novelcopilot
DB_USER=usuario
DB_PASSWORD=senha
SUPABASE_URL=https://seu-projeto.supabase.co
SUPABASE_ANON_KEY=chave-publica-anon
APP_FRONTEND_URL=http://localhost:5173
```

`APP_FRONTEND_URL` aceita origens exatas separadas por vírgula. No deploy, use a
URL pública HTTPS do frontend e não mantenha `localhost` nem domínios antigos.

### IA principal e memória

```env
AI_PROVIDER=cloudflare
AI_PRIMARY_MODEL=@cf/google/gemma-4-26b-a4b-it
CLOUDFLARE_ACCOUNT_ID=conta
CLOUDFLARE_AUTH_TOKEN=token
CLOUDFLARE_SUMMARY_ACCOUNT_ID=conta-de-resumos
CLOUDFLARE_SUMMARY_AUTH_TOKEN=token-de-resumos
AI_SUMMARY_PROVIDER=cloudflare
AI_SUMMARY_MODEL=@cf/google/gemma-4-26b-a4b-it
AI_SUMMARY_MAX_OUTPUT_TOKENS=1400
GEMINI_API_KEY=
GROQ_API_KEY=
```

Credenciais reais nunca devem ser gravadas no repositório. As propriedades de
Gemini e Groq ainda existem, mas suas chaves podem ficar vazias enquanto esses
provedores não forem usados.

O SQL detalhado fica desativado por padrão. Ative
`SPRING_JPA_SHOW_SQL=true` somente em diagnóstico local temporário.

### Desenvolvimento e administração

```env
PORT=8080
ADMIN_USER_IDS=uuid-supabase
AI_DEVELOPMENT_CREDITS_ENABLED=false
AI_DEVELOPMENT_QUOTA_PROFILE=DEMO
AI_RATE_LIMIT_ENABLED=true
SHUTDOWN_TIMEOUT=20s
```

`ADMIN_USER_IDS` aceita UUIDs separados por vírgula. Créditos de desenvolvimento
devem permanecer desabilitados em produção.

### Benchmark

O benchmark permanece bloqueado por padrão. Para uso local autorizado:

```env
SPRING_PROFILES_ACTIVE=benchmark
AI_BENCHMARK_ENABLED=true
ADMIN_USER_IDS=uuid-supabase
AI_BENCHMARK_MAX_RUN_COST_USD=1.00
AI_BENCHMARK_MAX_DAILY_COST_USD=3.00
OPENROUTER_ENABLED=false
OPENROUTER_API_KEY=
```

Ative OpenRouter somente quando houver intenção explícita de consumo.

### Inicialização

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

O backend usa a porta 8080 por padrão. Flyway aplica as migrations e
`ddl-auto=none` impede mudanças automáticas de schema.

## Staging em container

- Use `backend/Dockerfile` como definição reproduzível do backend.
- Configure `GET /health` como health check HTTP.
- A plataforma precisa fornecer a variável `PORT` ou aceitar a porta 8080.
- Use pelo menos 1 GiB de RAM como ponto inicial, sujeito à medição real.
- O filesystem do container deve ser tratado como efêmero; nenhuma obra,
  imagem, backup ou banco pode depender dele.
- Render e Koyeb gratuitas, ambas limitadas a 512 MB, foram descartadas para o
  backend atual por falta de margem segura.
- O primeiro staging usará Oracle Cloud Always Free em uma máquina Ampere A1.
  Não promova a conta para paga neste ciclo.

O container executa com usuário sem privilégios. A JVM recebe por padrão:

```text
-XX:MaxRAMPercentage=30.0
-XX:InitialRAMPercentage=10.0
-XX:+ExitOnOutOfMemoryError
```

Esses limites são provisórios. A plataforma escolhida pode sobrescrevê-los por
`JAVA_TOOL_OPTIONS`, mas somente depois de medir memória, tempo de inicialização
e uma geração real. Com o JAR atual de aproximadamente 176 MiB, ambientes de
512 MB não possuem margem segura.

A compatibilidade ARM64 foi auditada estaticamente: as imagens oficiais Temurin
21 usadas no Dockerfile publicam variantes `linux/arm64/v8`, e o `pom.xml` não
declara dependências diretas presas a `amd64`. O build e a execução reais em uma
máquina ARM64 continuam obrigatórios antes do acesso público.

### Procedimento do primeiro staging gratuito

A topologia inicial será uma máquina Ampere A1 Always Free com 1 OCPU e 3 GB de
RAM, backend em container, MySQL externo e entrada pública somente por HTTPS.
Segredos devem ser fornecidos no ambiente da máquina; banco, backups e obras não
podem depender do disco da VM. `GET /health` será o liveness check.

Antes do primeiro acesso público:

1. Confirmar no painel da Oracle que a forma está marcada como Always Free.
2. Criar regras de rede mínimas para SSH administrativo e HTTP/HTTPS.
3. Instalar Docker e aplicar atualizações de segurança do Linux.
4. Construir a imagem para `linux/arm64` e iniciá-la com limite de RAM.
5. Confirmar usuário sem privilégios, `/health`, logs e reinício automático.
6. Configurar as variáveis sem copiar arquivos `.env` locais.
7. Validar MySQL externo, Supabase e provedores de IA.
8. Medir RAM em repouso, durante login e durante uma geração controlada.
9. Configurar HTTPS antes de atualizar `APP_FRONTEND_URL` e a URL do frontend.
10. Registrar restauração do MySQL antes de aceitar dados reais.

A Oracle pode não ter capacidade gratuita na região e pode recuperar instâncias
Always Free consideradas ociosas. Não será criado tráfego artificial nem recurso
pago para contornar essas limitações. A VM também exige manutenção contínua de
Linux, firewall, TLS, Docker, logs e atualizações.

Enquanto a região retornar `Out of host capacity`, as novas tentativas podem ser
feitas pela stack salva no Resource Manager com o script abaixo. Ele cria um
Plan antes de cada Apply, aceita somente `1 to add, 0 to change, 0 to destroy`,
executa um job por vez e para diante de qualquer erro diferente de capacidade:

```powershell
$env:OCI_NOVELCOPILOT_STACK_ID = 'ocid1.ormstack...'
.\scripts\oracle\retry-a1-capacity.ps1 -DryRun
.\scripts\oracle\retry-a1-capacity.ps1
```

Os padrões são 10 minutos entre tentativas, 144 tentativas e duração máxima de
24 horas. O computador e a conexão precisam permanecer ligados. `Ctrl+C`
interrompe o processo. Os registros locais ficam em
`%LOCALAPPDATA%\NovelCopilot\oracle-capacity-retry`; nenhuma credencial é salva
no repositório. A autenticação da OCI CLI deve ser configurada no perfil do
Windows antes da execução real.

A memória foi reduzida de 4 GB para 3 GB após falhas repetidas de capacidade.
Essa configuração mantém margem sobre a medição local do backend, mas precisa
ser validada em repouso, no login e durante uma geração antes do acesso público.

O iniciador `scripts/oracle/start-a1-capacity.ps1` reinicia o processo quando
ele estiver parado e abre o monitor visual. Respostas `429 TooManyRequests`
entram em espera de cinco minutos até o limite total, sem encerrar a execução.

Fontes oficiais consultadas:

- <https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm>
- <https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm>
- <https://hub.docker.com/_/eclipse-temurin>
- <https://cloud.google.com/run/pricing>
- <https://learn.microsoft.com/en-us/azure/container-apps/billing>

## Staging permanente no Google Cloud Run

O Cloud Run é a alternativa principal enquanto a conta Oracle estiver em
revisão. O serviço usa o `backend/Dockerfile`, mantém o MySQL no Aiven e escala
para zero quando não recebe solicitações. A configuração inicial limita o
backend a 1 vCPU, 1 GiB de RAM, no máximo uma instância e 20 solicitações
simultâneas por instância.

## Staging atual em VM do Azure

O staging ativo foi transferido para uma VM Ubuntu no Azure para permitir estudo
de administração Linux e execução contínua. O GitHub Actions constrói a imagem e
a VM apenas a executa, evitando compilar Java no limite de 1 GiB de RAM. Consulte
o [guia completo de implantação e manutenção](azure-vm-deployment.md).

O Google Cloud exige um projeto associado a uma conta de faturamento. A
franquia gratuita não é uma garantia absoluta de custo zero. Antes da primeira
publicação, crie um limite de gastos do tipo **Spend cap enforcement**, restrito
ao projeto e ao serviço Cloud Run. A aplicação pode ser pausada quando o limite
for atingido e pequenas cobranças ainda podem ocorrer devido ao atraso de
medição.

Prepare as variáveis sem versionar credenciais:

```powershell
Copy-Item backend/cloud-run.env.example.yaml backend/cloud-run.env.yaml
```

Preencha `backend/cloud-run.env.yaml`. O arquivo real é ignorado pelo Git. Para
verificar a operação sem publicar:

```powershell
.\scripts\google\deploy-cloud-run.ps1 `
  -ProjectId SEU_PROJETO `
  -WhatIf
```

Depois de conferir o projeto, a região e o limite de gastos, remova `-WhatIf`.
O comando constrói a imagem remotamente e publica uma revisão:

```powershell
.\scripts\google\deploy-cloud-run.ps1 -ProjectId SEU_PROJETO
```

Após a publicação, valide `GET /health`, login, acesso ao Aiven e uma geração
controlada antes de alterar `VITE_API_BASE_URL` no frontend. Se houver falta de
memória, meça o uso antes de aumentar para 2 GiB. Não configure instância mínima
maior que zero enquanto a meta for permanecer dentro da franquia gratuita.

Fontes oficiais:

- <https://cloud.google.com/run/pricing>
- <https://cloud.google.com/billing/docs/how-to/budgets-spend-caps>

## Validação antes de commit

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm run test:ci
npm run lint
npm run build
```

Os testes automatizados não devem chamar provedores externos nem consumir
créditos. O CI usa Java 21 e Node 22.

## Diagnóstico rápido

- 401: confirme a sessão Supabase; o frontend deve voltar para `/auth`.
- 404 em recurso próprio: compare o `sub` do Supabase com o dono da história.
- CORS: confira `app.frontend-url` usado por `SecurityConfig`.
- IA indisponível: confira provedor, modelo, preço e credenciais no processo.
- Flyway: não altere migration aplicada; crie uma nova migration de correção.
- EPERM: encerre o processo que mantém o arquivo aberto e reinicie a ferramenta.

## Git e dados locais

- Não versione `.env`, logs, renders, dumps ou credenciais.
- Faça um commit após cada cartão aprovado e validado.
- Preserve alterações não relacionadas existentes na árvore de trabalho.
