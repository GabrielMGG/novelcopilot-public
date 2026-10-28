# Autenticação e segurança

O Supabase gerencia credenciais e sessão. O NovelCopilot não recebe nem armazena
senhas em seu banco MySQL.

## Fluxo

1. O frontend autentica diretamente no Supabase.
2. `authenticatedFetch` obtém a sessão válida e envia
   `Authorization: Bearer <token>` ao backend.
3. `SupabaseAuthenticationFilter` valida o token e preenche o contexto de
   segurança com o usuário autenticado.
4. `CurrentUserService` fornece a identidade às regras de negócio.
5. `StoryAccessService` valida que histórias e recursos filhos pertencem ao
   usuário antes de qualquer acesso.

Token ausente, inválido ou expirado retorna 401. Recurso inexistente ou de outro
usuário retorna 404 para não revelar a existência de IDs alheios.

## Regras obrigatórias

- Nunca aceite `userId` do body, query string ou cabeçalho como identidade.
- Nunca registre tokens, segredos ou payloads sensíveis em logs.
- Nunca leia ou altere um recurso privado antes da validação de acesso.
- Ao receber história e capítulo, valide posse e vínculo entre ambos.
- Permissão administrativa vem somente de `ADMIN_USER_IDS` no backend.
- Benchmark exige administrador, profile `benchmark` e feature flag ativa.
- Credenciais reais ficam em variáveis de ambiente, nunca no repositório.

## Frontend

Rotas privadas usam `ProtectedRoute`; rotas administrativas também usam
`AdminRoute`. Quando a API rejeita a sessão com 401, o tratamento central evita
que a aplicação permaneça mostrando páginas privadas com dados inválidos.

## Checklist para novos endpoints

1. Definir se a rota é pública, autenticada ou administrativa.
2. Obter a identidade somente do contexto autenticado.
3. Aplicar autorização ao recurso e aos pais relacionados.
4. Testar acesso do dono e tentativa por outro usuário.
5. Garantir resposta sanitizada, sem stack trace ou detalhes internos.
