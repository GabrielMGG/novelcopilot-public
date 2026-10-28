# Library (Biblioteca)

## Propósito
Este diretório abrange todos os subcomponentes encarregados de listar, criar e editar histórias (Books) na página principal da Biblioteca.

## Arquivos Principais
- `Bookshelf.tsx` (Contêiner que enfileira as prateleiras).
- `BookCard.tsx` (Componente singular que exibe o Livro, com suas proporções de capa e título).
- `CreateStoryModal.tsx` & `EditStoryModal.tsx` (Formulários de criação e atualização).

## Onde Editar?
- **Interface e Estilos:** A disposição dos livros está fortemente atrelada ao `App.css` (temporário) e utilitários globais. O HTML interno vive dentro de cada componente `.tsx` aqui especificado.
- **Chamadas de API:** O `LibraryPage.tsx` na raiz das rotas consome o `src/services/storyService.ts`. Mas os modais também fazem chamadas ativas de Update/Create diretamente.

## Fluxo de Dados
A `LibraryPage` capta os dados via API. Os itens processados são descidos para o `<Bookshelf>` que repassa individualmente para múltiplos `<BookCard>`. As edições/atualizações invocam funções no `storyService` que acionam Refetches e Updates de estado local para forçar a re-renderização.

## Ponto de Atenção
- O `coverImagePath` (URL parcial de storage) vem do banco, mas a `<LibraryPage>` e o modal de Editar precisam gerar Signed URLs temporárias antes da renderização final no componente. Tome muito cuidado ao alterar `createStoryCoverSignedUrl`.
