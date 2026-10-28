# Characters (Personagens)

## Propósito
Este domínio gerencia toda a listagem, visualização, edição estruturada (abrangendo abas Psicológicas e Físicas) e exclusão dos Personagens da história.

## Arquivos Principais
- `CharacterWorkspace.tsx` (Componente de Layout principal e Mestre do Estado).
- `CharacterList.tsx` (Sidebar listando a navegação interna dos personagens da história).
- Formulários fragmentados internamente no workspace para tratar as propriedades.

## Onde Editar?
- **Interface Visual e Lógica:** Quase inteiramente contida no gigantesco `CharacterWorkspace.tsx`.
- **Estilos:** `CharacterWorkspace.css` e heranças do `workspace.css`.
- **Chamadas de API:** `src/services/characterService.ts` via hooks ou chamadas diretas.

## Fluxo de Dados
A página faz o fetch da lista base. Ao clicar em um personagem, ele atualiza um estado `selectedId` e invoca o carregamento dos detalhes inteiros dele para preenchimento dos Inputs. Ao clicar em Salvar, injeta o payload no banco, atualiza o item no State da lista e renderiza visualmente as correções.

## Ponto de Atenção
- Existe uma particularidade onde o design separa fortemente as visões Mobile e Desktop (especialmente nos layouts de `display: grid` alterando para `display: flex` em coluna). Não altere metragens sem verificar o `.character-detail-layout` no CSS.
