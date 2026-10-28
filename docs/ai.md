# Inteligência artificial

O motor de IA ajuda na escrita sem delegar ao provedor decisões de autorização,
contexto ou cobrança. A seleção de modelo é configuração do backend e não uma
escolha técnica exposta ao escritor.

## Provedores

A interface `AiProvider` possui implementações para Cloudflare, Groq, Gemini e
OpenRouter. O provedor principal é resolvido por configuração. Resumos podem usar
credenciais e modelo separados para isolar custo e finalidade.

O benchmark reutiliza provedores, mas é uma ferramenta interna com controles de
acesso e orçamento próprios. Modelos cadastrados nele não ficam automaticamente
habilitados para usuários.

## Contexto narrativo

`NovelContextService` monta um contexto limitado e determinístico conforme a
ação. Ele pode incluir:

- dados da história e do capítulo atual;
- continuidade imediata do texto;
- memórias dos capítulos anteriores;
- personagens e notas de mundo selecionados por relevância;
- instrução e trecho selecionado quando aplicável.

Limites de contexto, quantidade de capítulos, personagens e notas são definidos
em `application.properties`. Não envie toda a história indiscriminadamente.

## Memória de capítulo

A memória registra fatos, comportamentos demonstrados, decisões, relações,
pistas e estado final. Ela deve:

- permanecer no idioma predominante do capítulo, detectado automaticamente;
- diferenciar fato, suspeita e intenção;
- registrar decisões de não agir e reações relevantes;
- evitar inferências, invenções e avaliações literárias;
- ser regenerada quando o hash do conteúdo mudar.

Resumos usam provedor/modelo próprios e geram registro de uso, mas não debitam os
créditos literários do usuário.

## Geração e cobrança

1. Validar usuário, história e capítulo.
2. Validar que existe preço para o provedor/modelo antes da chamada.
3. Estimar e reservar créditos.
4. Aplicar rate limit e idempotência.
5. Chamar o provedor e validar o formato esperado.
6. Capturar o custo em sucesso ou liberar a reserva em falha.
7. Gravar uso e histórico por capítulo.

Ações sobre pequenos trechos podem usar cobrança fracionária configurada. O
backend, e não o frontend, determina o valor final.

## Ações atuais

O contrato principal está no enum `NovelAiAction`. Ele inclui continuação,
reescrita, diálogo, tensão, planejamento, revisão, transformação em capítulo,
resumo interno e geração de títulos. Algumas ações antigas permanecem no contrato
por compatibilidade e não precisam aparecer na interface.

Ao criar ou alterar uma ação:

- defina propósito e formato de saída;
- limite o tamanho proporcionalmente à seleção;
- preserve idioma, fatos e voz do autor;
- adicione tratamento de resposta inválida com liberação de créditos;
- teste contexto, consumo de cotas, histórico e múltiplos idiomas.

A revisão integral de todas as ações está registrada no cartão `FUTURO-AI`, que
só começa depois dos cartões atuais de estabilização.
