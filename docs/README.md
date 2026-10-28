# Documentação oficial do NovelCopilot

Esta pasta é a fonte canônica da documentação técnica do projeto. Quando houver
divergência, o código, as migrations do Flyway e os arquivos de configuração têm
precedência; a documentação deve ser corrigida no mesmo cartão que alterar o
comportamento descrito.

## Visão geral

O NovelCopilot é uma aplicação de planejamento e escrita de histórias. O usuário
organiza capítulos, personagens, mundo e referências e pode solicitar apoio de
IA com contexto narrativo e controle de créditos.

## Stack atual

- Frontend: React 19, TypeScript, Vite 8, React Router 7, TipTap e i18next.
- Backend: Java 21, Spring Boot 4, Spring Security e Spring Data JPA.
- Banco: MySQL, com schema versionado exclusivamente pelo Flyway.
- Autenticação: Supabase Auth; o backend valida o token recebido.
- IA: provedores configuráveis, atualmente Cloudflare, Groq, Gemini e
  OpenRouter, conforme disponibilidade e finalidade.

## Estrutura

- `frontend/`: aplicação web.
- `backend/`: API, regras de negócio, segurança, cotas técnicas e integrações.
- `docs/`: documentação técnica oficial.
- `docs/benchmark/`: registros e preparação de avaliações de modelos.

## Índice

- [Arquitetura](architecture.md)
- [Backend](backend.md)
- [Frontend](frontend.md)
- [Banco de dados](database.md)
- [Autenticação e segurança](auth-security.md)
- [Inteligência artificial](ai.md)
- [Rotas e endpoints](routes-and-endpoints.md)
- [Manutenção e execução local](maintenance-guide.md)
- [Staging em VM do Azure](azure-vm-deployment.md)
