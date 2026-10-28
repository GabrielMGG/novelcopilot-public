# World (Mundo da História)

## Propósito
Este domínio encapsula a gestão de todo o Lore da história (Locais, Facções, Conceitos Mágicos/Históricos, Itens), chamado de `Bíblia do Mundo`.

## Arquivos Principais
- `WorldWorkspace.tsx` (Componente de Layout principal e Mestre do Estado).
- Formulários fragmentados internamente para edição e criação de itens de mundo.

## Onde Editar?
- **Interface Visual e Lógica:** Integradas em `WorldWorkspace.tsx`.
- **Estilos:** `WorldWorkspace.css` e utilitários de container em `workspace.css`.
- **Chamadas de API:** Usa o `src/services/worldElementService.ts`.

## Fluxo de Dados
Similar ao fluxo de personagens:
Ao montar, lista todos os elementos atrelados ao `storyId` da URL. O usuário interage pelo painel esquerdo, alterando o `selectedElementId` na memória, e o lado direito popula os campos reativos para o elemento.

## Ponto de Atenção
- Da mesma maneira que em Personagens, o design exige reflow estrutural (de lado a lado para pilha vertical) entre o layout Desktop e Mobile (< 768px). Tome cuidado com overflows horizontais indesejados ao introduzir novos Inputs.
