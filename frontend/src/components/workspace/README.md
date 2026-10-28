# Workspace (Editor e Inteligência Artificial)

## Propósito
Este domínio gerencia o fluxo intenso de Escrita do aplicativo, englobando o editor de texto TipTap, o painel da IA e integrações inline.

## Arquivos Principais
- `ChapterRichEditor.tsx` (Componente de Texto TipTap).
- `AiAssistantPanel.tsx` (Painel Lateral de geração de roteiros/conteúdos da IA).
- `AiSuggestionModal.tsx` (Modal de exibição quando a IA gera uma sugestão autônoma).

## Onde Editar?
- **Interface Visual:** As lógicas visuais estão no `StoryWorkspacePage.tsx` (em `src/pages/`) e espalhadas no componente.
- **Estilos:** `src/styles/workspace.css`, `ChapterRichEditor.css` e `AiAssistantPanel.css`.
- **Chamadas de API:** Consomem principalmente o `src/services/aiService.ts` e `chapterService.ts`.

## Fluxo de Dados
O `StoryWorkspacePage` é a grande âncora (controlador mestre) repassando estados abertos/fechados dos painéis via Props e recebendo retornos via calbacks (`onToggle`, `onSave`). A IA recebe os contextos lendo seleções do TipTap injetadas dinamicamente.

## Ponto de Atenção
- **TipTap Inline AI:** O rastreio automático do texto para sugerir continuações usa uma função que varre a seleção do `editor.state.doc`. Muito cuidado com o `useEffect` para evitar loopings de renderização durante a digitação rápida.
