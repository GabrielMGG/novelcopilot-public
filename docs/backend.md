# Backend

O backend usa Java 21 e Spring Boot 4. Ele expõe a API, valida a identidade do
usuário, aplica autorização por recurso, persiste dados no MySQL, controla
créditos e orquestra provedores de IA.

## Organização principal

- `config`: configuração Spring, propriedades, clientes HTTP, CORS e segurança.
- `controller`: contrato HTTP; deve permanecer fino.
- `dto`: requests e responses da API.
- `entity`: mapeamento JPA do schema criado pelo Flyway.
- `repository`: acesso a dados com Spring Data JPA.
- `security`: autenticação Supabase e contexto do usuário.
- `service`: regras de negócio, acesso, cotas técnicas, contexto e IA.
- `benchmark`: ferramenta administrativa isolada para avaliar modelos.
- `resources/db/migration`: única fonte autorizada para evolução do schema.

## Regras de implementação

1. Controllers não recebem nem confiam em `userId` enviado pelo cliente.
2. Recursos de uma história devem passar pelo método correspondente do
   `StoryAccessService` antes de leitura ou alteração.
3. Regras de negócio ficam em services; repositories não substituem a camada de
   autorização.
4. Mudanças de banco exigem uma nova migration. Nunca altere uma migration já
   aplicada e nunca use `ddl-auto=update`.
5. Chamadas de IA devem respeitar preço cadastrado, reserva/captura/liberação de
   créditos, limites de frequência e histórico.
6. Novos endpoints precisam de testes de sucesso, autenticação e isolamento entre
   usuários quando lidarem com dados privados.

## Componentes de entrada

- Histórias, capítulos, personagens, mundo e referências: controllers de domínio.
- IA: `NovelAiController`, `AiGenerationController` e histórico por capítulo.
- Créditos: reserva, captura e liberação da cota técnica de IA.
- Administração: acesso baseado exclusivamente em `ADMIN_USER_IDS`.
- Saúde: `GET /health` é público.

Consulte [Rotas e endpoints](routes-and-endpoints.md), [Banco de dados](database.md)
e [Guia de manutenção](maintenance-guide.md) para os contratos operacionais.
