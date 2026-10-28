# Frontend

O frontend é uma SPA em React 19, TypeScript e Vite. O roteamento fica em
`src/app/routes.tsx`; autenticação e expiração de sessão são tratadas pelo
Supabase e pelos componentes de rota protegida.

## Organização principal

- `app`: inicialização e rotas.
- `components`: interface reutilizável por domínio.
- `contexts`: estado global, incluindo autenticação.
- `features`: funcionalidades isoladas, como benchmark interno.
- `i18n`: textos em português e inglês.
- `pages`: páginas associadas a rotas.
- `services`: comunicação com backend e Supabase.
- `styles`: estilos globais e de telas maiores.
- `types`: contratos TypeScript.

## Regras de implementação

1. Use `authenticatedFetch` para endpoints protegidos. Ele envia o token e trata
   sessão expirada de forma centralizada.
2. Use `API_BASE_URL`, derivado de `VITE_API_BASE_URL`; não introduza
   `VITE_API_URL` ou URLs locais espalhadas pelo código.
3. Novas páginas devem ser registradas em `src/app/routes.tsx` e protegidas quando
   exibirem dados privados.
4. Textos visíveis devem usar i18n. O idioma da história é inferido pelo conteúdo;
   o usuário não precisa escolher um idioma para a IA.
5. Alterações no editor TipTap devem preservar formatação, autosave, contagem de
   palavras e ações de IA por seleção.
6. Estados assíncronos precisam distinguir carregamento, vazio e erro. Evite
   spinners que apenas piscam em respostas rápidas.
7. Serviços e componentes novos devem receber testes proporcionais ao risco.

## Áreas principais

- Biblioteca e lixeira de histórias.
- Workspace de escrita, personagens, mundo, notas e referências.
- Configurações de perfil, créditos e consumo de IA.
- Benchmark interno protegido por autenticação e permissão administrativa.

As rotas atuais estão em [Rotas e endpoints](routes-and-endpoints.md). Variáveis e
comandos estão no [Guia de manutenção](maintenance-guide.md).
