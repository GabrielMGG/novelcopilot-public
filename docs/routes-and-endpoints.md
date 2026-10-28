# Rotas e endpoints

Este documento registra os grupos de rotas. Os controllers e
`frontend/src/app/routes.tsx` são a referência para parâmetros e DTOs exatos.

## Frontend

| Rota | Finalidade | Proteção |
| --- | --- | --- |
| `/auth` | Login e cadastro | Pública |
| `/` | Biblioteca | Usuário autenticado |
| `/trash` | Lixeira | Usuário autenticado |
| `/stories/:storyId/write` | Escrita | Usuário autenticado |
| `/stories/:storyId/characters` | Personagens | Usuário autenticado |
| `/stories/:storyId/world` | Mundo | Usuário autenticado |
| `/stories/:storyId/notes` | Notas | Usuário autenticado |
| `/stories/:storyId/references` | Referências | Usuário autenticado |
| `/settings/*` | Perfil, créditos e preferências | Usuário autenticado |
| `/internal/benchmark/*` | Benchmark | Usuário administrador |

Rotas desconhecidas exibem a página 404. Um 401 recebido em área protegida
encerra a sessão inválida e redireciona para `/auth`.

## Backend

Salvo exceções configuradas em segurança, as rotas exigem JWT Supabase.

### Domínio literário

- `/stories`: listar, criar, ler, atualizar, enviar à lixeira, restaurar e excluir
  permanentemente histórias.
- `/stories/{storyId}/chapters` e `/chapters/{id}`: CRUD de capítulos.
- `/stories/{storyId}/characters` e `/characters/{id}`: CRUD de personagens.
- `/stories/{storyId}/world-notes` e `/world-notes/{id}`: CRUD de mundo.
- `/stories/{storyId}/references` e `/references/{id}`: CRUD de referências.

Todas as operações por ID validam o dono do recurso. IDs de história e capítulo
fornecidos juntos também precisam pertencer um ao outro.

### Inteligência artificial

- `POST /api/ai/estimate`: estima consumo de créditos sem chamar o provedor.
- `POST /api/ai/generate`: fluxo principal de geração com controle de cota.
- `POST /stories/{storyId}/ai/generate`: fluxo legado/contextual persistido.
- `POST /stories/{storyId}/chapters/{chapterId}/summary`: atualiza a memória.
- `GET /api/stories/{storyId}/chapters/{chapterId}/ai-history`: histórico
  paginado do capítulo.
- `/ai-generations`: leitura e exclusão do histórico legado autorizado.

### Créditos técnicos

- `GET /api/credits/balance`
- `GET /api/credits/transactions`

### Administração e benchmark

- `GET /api/admin/access`: confirma acesso administrativo.
- `/internal/benchmark/*`: configuração, estimativa, execução, histórico,
  avaliação cega, retry e relatório. Exige profile e feature flag do benchmark,
  além de usuário listado em `ADMIN_USER_IDS`.

### Saúde

- `GET /health`: liveness pública; responde JSON estável com `status=UP` e o
  identificador do serviço. Não testa banco nem provedores externos para evitar
  reinicializações em cascata durante indisponibilidades externas.
