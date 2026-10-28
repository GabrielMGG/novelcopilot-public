# NovelCopilot

Plataforma web para planejar, organizar e escrever histórias com apoio de
inteligência artificial. O autor mantém capítulos, personagens, mundo e
referências em um único workspace, com memória narrativa e histórico de respostas
de IA por capítulo.

## Estado atual

Projeto independente e aberto para estudo, portfólio e colaboração. O sistema
de créditos representa uma cota técnica para controlar o uso dos recursos de IA.

## Tecnologias

- Frontend: React 19, TypeScript, Vite 8, TipTap e i18next.
- Backend: Java 21, Spring Boot 4 e Spring Security.
- Banco: MySQL com migrations Flyway.
- Autenticação: Supabase Auth.
- IA: provedores configuráveis pelo backend.

## Estrutura do repositório

```text
novelcopilot/
|-- backend/     API, segurança, banco, cotas técnicas e IA
|-- frontend/    interface web e editor
|-- docs/        documentação técnica oficial
`-- .github/     validação automatizada
```

## Pré-requisitos

- Java 21.
- Node.js 22.
- MySQL.
- Projeto Supabase para autenticação.
- Credenciais de provedor de IA somente quando chamadas reais forem testadas.

## Configuração local

### Frontend

Crie um arquivo local a partir de `frontend/.env.example` e informe:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_SUPABASE_URL=https://seu-projeto.supabase.co
VITE_SUPABASE_ANON_KEY=chave-publica-anon
```

### Backend

Configure as variáveis no processo que iniciará o backend, inclusive quando ele
for iniciado pelo IntelliJ:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/novelcopilot
DB_USER=usuario
DB_PASSWORD=senha
SUPABASE_URL=https://seu-projeto.supabase.co
SUPABASE_ANON_KEY=chave-publica-anon
AI_PROVIDER=cloudflare
CLOUDFLARE_ACCOUNT_ID=conta
CLOUDFLARE_AUTH_TOKEN=token
```

As variáveis completas para IA principal, memória, administração e benchmark
estão no [guia de manutenção](docs/maintenance-guide.md). Não grave
credenciais reais em arquivos rastreados pelo Git.

## Executar

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Por padrão, o frontend abre em `http://localhost:5173` e o backend usa a porta
`8080`.

## Validar antes de um commit

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm run test:ci
npm run lint
npm run build
```

Os testes automatizados não devem chamar provedores externos nem consumir
créditos.

## Documentação

- [Índice oficial](docs/README.md)
- [Arquitetura](docs/architecture.md)
- [Execução local e variáveis](docs/maintenance-guide.md)
- [Backend](docs/backend.md)
- [Frontend](docs/frontend.md)
- [Banco de dados](docs/database.md)
- [Autenticação e segurança](docs/auth-security.md)
- [Inteligência artificial](docs/ai.md)
- [Rotas e endpoints](docs/routes-and-endpoints.md)

O código, as migrations e os arquivos de configuração têm precedência quando uma
informação documental estiver desatualizada. Corrija a documentação no mesmo
cartão que alterar o comportamento correspondente.
