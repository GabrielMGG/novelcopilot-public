# Banco de dados

O NovelCopilot usa MySQL. O Flyway é a única fonte autorizada para criar e
evoluir o schema; o Hibernate apenas mapeia as entidades com
`spring.jpa.hibernate.ddl-auto=none`.

## Regra de migrations

- Migrations ficam em `backend/src/main/resources/db/migration`.
- Crie uma nova migration versionada para cada mudança de schema ou backfill.
- Nunca edite uma migration que já possa ter sido aplicada.
- Nunca use `ddl-auto=update` como mecanismo de evolução.
- A migration deve ser segura para dados existentes e possuir teste específico
  quando executar correção financeira, conversão ou backfill relevante.
- Depois da migration, entidades e repositories devem permanecer compatíveis com
  o schema validado pelos testes.

## Domínios persistidos

- Identidade local e perfil sincronizado com o `sub` do Supabase.
- Histórias, capítulos e memória narrativa dos capítulos.
- Personagens, notas de mundo e referências.
- Gerações e histórico de respostas de IA por história e capítulo.
- Uso de IA, preços dos modelos, créditos, reservas e capturas.
- Perfis de cota e consumo técnico de créditos da IA.
- Benchmark interno, incluindo casos, respostas, avaliações e custos.
- Contadores persistentes de rate limit.

Os nomes e campos exatos devem ser consultados nas migrations e entities, não
copiados deste documento. Isso evita que uma lista manual se torne uma segunda
definição do schema.

## Segurança e operação

- Consultas de domínio devem preservar o vínculo do recurso com o usuário.
- Valores fracionários de créditos usam precisão decimal; não use `double`.
- Backups e restauração serão definidos na preparação do deploy.
- Logs e dumps com dados reais não devem ser versionados.
