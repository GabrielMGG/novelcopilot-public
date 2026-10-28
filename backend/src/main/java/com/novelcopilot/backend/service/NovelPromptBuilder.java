package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.dto.NovelAiRequest;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class NovelPromptBuilder {

    private static final Pattern REWRITE_WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:[’'\u2010-\u2015-][\\p{L}\\p{N}]+)*");

    public String buildSystemPrompt(NovelAiRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é um assistente de escrita criativa de elite trabalhando dentro do NovelCopilot.\n");
        sb.append("DIRETRIZES FUNDAMENTAIS OBRIGATÓRIAS:\n");
        sb.append("- Preservar a intenção do autor e os fatos estabelecidos; não contradiga o que já foi escrito.\n");
        sb.append("- Respeitar estritamente o contexto fornecido.\n");
        sb.append("- Não inventar fatos objetivos sem base no contexto.\n");
        if (allowsExplicitFactualEdit(request.action())) {
            sb.append("- EXCEÇÃO DE EDIÇÃO: uma alteração factual pedida explicitamente pelo autor pode ser aplicada somente dentro do trecho selecionado; não altere outros fatos por conta própria.\n");
        }
        sb.append("- Respeitar o idioma da obra.\n");
        sb.append("- Não misturar conteúdo de outras histórias.\n");
        sb.append("- NUNCA incluir explicações sobre a própria resposta, saudações ou meta-comentários.\n");

        if (request.language() != null && !request.language().isBlank()) {
            sb.append("\nIDIOMA EXIGIDO: ").append(request.language()).append("\n");
        }
        if (request.tone() != null && !request.tone().isBlank()) {
            sb.append("\nTOM OBRIGATÓRIO: ").append(request.tone()).append("\n");
        }
        if (request.pov() != null && !request.pov().isBlank()) {
            sb.append("\nPOV OBRIGATÓRIO: ").append(request.pov()).append("\n");
        }
        if (request.desiredLength() != null && request.desiredLength() > 0) {
            sb.append("\nTAMANHO DESEJADO: ").append(request.desiredLength()).append(" palavras (aproximadamente).\n");
        }

        if (request.action() == NovelAiAction.PLAN_SCENE ||
            request.action() == NovelAiAction.SUGGEST_CONTINUATION ||
            request.action() == NovelAiAction.GENERATE_CHAPTER_TITLE ||
            request.action() == NovelAiAction.REVIEW_CHAPTER ||
            request.action() == NovelAiAction.SUMMARIZE_SELECTION) {
            sb.append("\nFORMATO EXIGIDO: Retorne somente JSON válido, sem Markdown, blocos de código, introduções ou texto fora do objeto JSON.\n");
        } else {
            sb.append("\nFORMATO EXIGIDO: Retorne somente texto simples, sem Markdown, títulos, listas, blocos de código ou explicações.\n");
        }

        return sb.toString();
    }

    public String buildActionPrompt(NovelAiAction action, NovelAiRequest request, String context) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("### CONTEXTO DA HISTÓRIA\n");
        sb.append(context);
        sb.append("\n==================================\n");

        if (request.instruction() != null && !request.instruction().isBlank()) {
            sb.append("### INSTRUÇÃO ESPECÍFICA DO USUÁRIO\n");
            sb.append(request.instruction()).append("\n\n");
        }

        if (requiresContinuityHierarchy(action)) {
            appendContinuityHierarchy(sb);
        }

        switch (action) {
            case CONTINUE_SCENE -> {
                sb.append("AÇÃO: CONTINUAR CENA\n");
                sb.append("Continue a cena a partir da continuidade imediata.\n");
                sb.append("Continue depois da última frase, sem reapresentar a situação já explicada.\n");
                sb.append("Cada continuação deve provocar pelo menos uma mudança concreta na cena (ação, decisão, descoberta, reação ou consequência concreta).\n");
                sb.append("Não encerre de forma puramente estática.\n");
                sb.append("Não repita informações que acabaram de aparecer.\n");
                sb.append("Preserve o cânone. Não invente números, causas, pistas, facções ou históricos.\n");
                sb.append("Reutilize normalmente fatos e elementos já presentes no contexto.\n");
                sb.append("Evite clichês repetidos.\n");
                sb.append("PERSONALIDADE: Os personagens devem agir, falar e decidir de acordo com as fichas fornecidas. Não uniformize suas vozes. ");
                sb.append("Quando não houver ficha de personagem, use apenas o comportamento demonstrado no texto; não invente uma personalidade completa.\n");
                sb.append("Não exija fatos que o contexto não forneceu.\n");
            }
            case INLINE_CONTINUATION -> {
                sb.append("AÇÃO: CONTINUAÇÃO INLINE\n");
                sb.append("Continue exatamente a partir do ponto atual, encaixando a resposta diretamente no texto.\n");
                sb.append("Se a última frase estiver incompleta, complete-a primeiro.\n");
                sb.append("Gere entre 80 e 180 palavras (texto curto).\n");
                sb.append("Não resuma. Não encerre obrigatoriamente a cena. Não repita o último parágrafo.\n");
                sb.append("Retorne apenas a prosa narrativa que encaixa na continuidade.\n");
            }
            case SUGGEST_CONTINUATION -> {
                sb.append("AÇÃO: SUGERIR CONTINUAÇÃO\n");
                sb.append("Baseado no contexto atual, sugira EXATAMENTE TRÊS opções curtas e diferentes para o que deve acontecer a seguir na cena.\n");
                sb.append("Nenhuma opção deve contradizer o cânone ou explicar o mistério sem base.\n");
                sb.append("O 'summary' deve explicar a direção narrativa. O 'snippet' deve ser curto e utilizável no texto.\n");
                sb.append("Retorne o resultado estritamente no seguinte formato JSON:\n");
                sb.append("{\n" +
                          "  \"options\": [\n" +
                          "    { \"type\": \"safe\", \"title\": \"...\", \"summary\": \"...\", \"snippet\": \"...\" },\n" +
                          "    { \"type\": \"unexpected\", \"title\": \"...\", \"summary\": \"...\", \"snippet\": \"...\" },\n" +
                          "    { \"type\": \"intense\", \"title\": \"...\", \"summary\": \"...\", \"snippet\": \"...\" }\n" +
                          "  ]\n" +
                          "}\n");
            }
            case GENERATE_CHAPTER_TITLE -> {
                sb.append("ACAO: GERAR TITULOS PARA O CAPITULO\n");
                sb.append("Crie EXATAMENTE TRES titulos distintos com base somente no conteudo do capitulo atual.\n");
                sb.append("Cada titulo deve ter entre 2 e 8 palavras e estar no mesmo idioma predominante do capitulo.\n");
                sb.append("Evite spoilers alem do que ja aconteceu, frases genericas, cliches, numeracao e aspas.\n");
                sb.append("Retorne estritamente este formato JSON: {\"options\":[\"...\",\"...\",\"...\"]}\n");
            }
            case REWRITE_SELECTION -> {
                RewriteLimits limits = rewriteLimits(request.selectedText());
                sb.append("AÇÃO: REESCREVER TRECHO\n");
                sb.append("Reescreva SOMENTE o trecho selecionado abaixo.\n");
                sb.append("Melhore clareza, fluidez e naturalidade sem transformar a reescrita em continuação, resumo ou nova cena.\n");
                sb.append("Preserve significado, fatos e ponto de vista, exceto quando a instrução do usuário pedir explicitamente uma alteração dentro do trecho selecionado.\n");
                sb.append("Preserve idioma, variante regional, tempo verbal, pessoa narrativa, registro e grau de formalidade do original.\n");
                sb.append("Não enriqueça o vocabulário por conta própria e não troque palavras comuns por termos mais formais apenas para parecer literário.\n");
                sb.append("Não acrescente fatos, explicações, introduções, encerramentos, comentários ou Markdown.\n");
                sb.append("O original possui aproximadamente ").append(limits.sourceWords()).append(" palavras. ");
                sb.append("A resposta deve ter entre ").append(limits.minimumWords()).append(" e ")
                        .append(limits.maximumWords()).append(" palavras.\n");
                if (limits.singleParagraph()) {
                    sb.append("Mantenha a resposta em um único parágrafo.\n");
                }
                sb.append("Trecho Selecionado:\n").append(request.selectedText()).append("\n");
            }
            case EXPAND_SELECTION -> {
                sb.append("AÇÃO: EXPANDIR TRECHO\n");
                sb.append("Amplie o trecho selecionado adicionando detalhes coerentes.\n");
                sb.append("Não crie fatos novos importantes e preserve a direção.\n");
                sb.append("Não substitua o trecho por outra cena.\n");
                sb.append("Trecho Selecionado:\n").append(request.selectedText()).append("\n");
            }
            case IMPROVE_DIALOGUE -> {
                sb.append("AÇÃO: MELHORAR DIÁLOGO\n");
                sb.append("Torne as falas mais naturais e diferencie as vozes.\n");
                sb.append("Reduza exposição artificial.\n");
                sb.append("Preserve intenção e informação original.\n");
                sb.append("Não mude quem fala e não invente novas relações.\n");
                sb.append("Trecho Selecionado:\n").append(request.selectedText()).append("\n");
            }
            case ADD_TENSION -> {
                sb.append("AÇÃO: ADICIONAR TENSÃO\n");
                sb.append("Aumente a tensão do trecho por meio de ritmo, subtexto, silêncio, reação e consequência.\n");
                sb.append("Não crie inimigos, ameaças, pistas ou eventos novos sem contexto.\n");
                sb.append("Não transforme a cena em ação exagerada.\n");
                sb.append("Trecho Selecionado:\n").append(request.selectedText()).append("\n");
            }
            case SIMPLIFY_SELECTION -> {
                sb.append("AÇÃO: SIMPLIFICAR TRECHO\n");
                sb.append("Reduza o excesso de explicação e remova redundâncias.\n");
                sb.append("Torne as frases mais diretas preservando a informação essencial.\n");
                sb.append("Não resuma até perder o sentido.\n");
                sb.append("Trecho Selecionado:\n").append(request.selectedText()).append("\n");
            }
            case WRITE_CHAPTER_FROM_BEATS -> {
                sb.append("AÇÃO: GERAR PRIMEIRO RASCUNHO DE CAPÍTULO A PARTIR DE BEATS\n");
                sb.append("Escreva um primeiro rascunho de capítulo transformando os tópicos/beats fornecidos na narrativa principal.\n");
                sb.append("MODO FIEL E FLEXÍVEL: O usuário especificou como seguir os beats. Respeite a instrução de estilo fornecida.\n");
                sb.append("Se a instrução pedir modo FIEL: siga todos os beats, mantenha a ordem, não elimine nem adicione grandes eventos; preencha apenas transições, diálogos, descrição e ritmo.\n");
                sb.append("Se a instrução pedir modo FLEXÍVEL: respeite os beats essenciais, permita pequenas transições e reorganização leve, adicione detalhes de ligação; mas não mude o resultado central planejado nem introduza novo arco.\n");
                sb.append("Apresente como prosa pura narrativa.\n");
                sb.append("Não inclua: título automático, explicações, lista de beats, comentários ou Markdown.\n");
            }
            // Ações legadas/pendentes
            case REVIEW_CHAPTER -> {
                sb.append("AÇÃO: REVISAR CAPÍTULO\n");
                sb.append("Avalie o capítulo proporcionalmente ao estágio atual da obra e ao que já foi escrito.\n");
                sb.append("Não trate mistério aberto, arco incompleto, explicação futura ou ausência de resolução como defeito apenas porque a história ainda está no começo ou em desenvolvimento.\n");
                sb.append("Problemas objetivos exigem evidência textual específica. Se não houver evidência suficiente, não faça a acusação.\n");
                sb.append("Separe defeitos objetivos de preferências estilísticas; preferências devem ter prioridade optional.\n");
                sb.append("Não exija que um único capítulo explique todos os personagens, regras, conflitos ou detalhes do mundo.\n");
                sb.append("Avalie coerência, clareza, ritmo, continuidade, personagens e estilo. Preserve nomes e termos fictícios.\n");
                sb.append("A evidência deve citar ou parafrasear brevemente um trecho realmente presente, sem inventar acontecimentos.\n");
                sb.append("Retorne somente JSON neste formato:\n");
                sb.append("{\"chapterStage\":\"opening|development|climax|resolution|unknown\",\"summary\":\"...\",\"findings\":[{\"category\":\"coherence|clarity|pacing|continuity|character|style\",\"severity\":\"critical|important|optional\",\"issue\":\"...\",\"evidence\":\"...\",\"impact\":\"...\",\"suggestion\":\"...\"}],\"strengths\":[\"...\"],\"limitations\":[\"...\"]}\n");
            }
            case PLAN_SCENE -> {
                sb.append("AÇÃO: PLANEJAR CENA\n");
                sb.append("Baseado nos personagens e na instrução fornecida, planeje a cena.\n");
                sb.append("Retorne estritamente em formato JSON válido:\n");
                sb.append("{ \"objetivo\": \"...\", \"conflito\": \"...\", \"progressao\": \"...\", \"virada\": \"...\", \"resultadoEsperado\": \"...\", \"fatosAPreservar\": [\"...\"], \"riscosDeContradicao\": [\"...\"] }\n");
            }
            case CREATE_NEXT_CHAPTER -> {
                sb.append("AÇÃO: GERAR CAPÍTULO\n");
                sb.append("Escreva o próximo capítulo da história, continuando a partir do final do capítulo atual.\n");
                sb.append("Mostre consequências das ações anteriores.\n");
            }
            case SUMMARIZE_SELECTION -> {
                sb.append("AÇÃO: RESUMIR SELEÇÃO\n");
                sb.append("Gere um resumo estruturado da seleção.\n");
                sb.append("Retorne o resultado estritamente em formato JSON válido:\n");
                sb.append("{ \"resumo\": \"...\", \"acontecimentosPrincipais\": [\"...\"], \"personagensPresentes\": [\"...\"], \"fatosNovos\": [\"...\"], \"mudancasRelacionamento\": [\"...\"], \"itensPendentes\": [\"...\"], \"possiveisContradicoes\": [\"...\"] }\n");
            }
        }
        return sb.toString();
    }

    private void appendContinuityHierarchy(StringBuilder sb) {
        sb.append("### HIERARQUIA OBRIGATÓRIA DE CONTINUIDADE\n");
        sb.append("1. ESTADO ATUAL DA CENA: fatos, ações, decisões, relações e atribuições expressos na continuidade imediata são o estado vigente e não podem ser reescritos.\n");
        sb.append("2. CÂNONE RELEVANTE: fichas, notas de mundo e capítulos anteriores complementam a cena, mas não substituem um fato mais recente e explícito.\n");
        sb.append("3. INSTRUÇÃO DO USUÁRIO: define o desenvolvimento desejado, mas nesta ação de continuidade não autoriza alterar silenciosamente fatos já estabelecidos.\n");
        sb.append("4. LIBERDADE CRIATIVA: crie somente nos espaços ainda não definidos pelo contexto.\n");
        sb.append("Antes de escrever, confira internamente quem está presente ou junto, quais decisões e tarefas já foram atribuídas, o que já foi resolvido ou está indisponível e o que cada personagem demonstrou saber. Não exponha essa conferência na resposta.\n");
        sb.append("Continue depois do último acontecimento sem refazer a escolha anterior, separar ou reunir personagens sem transição, trocar suas tarefas ou reabrir uma alternativa já encerrada.\n");
        sb.append("Não invente familiaridade anterior, memória, conhecimento, relação, pista ou motivação para justificar a continuação. Se algo não estiver estabelecido, apresente-o como descoberta nova e verificável na cena, não como passado conhecido.\n\n");
    }

    private boolean requiresContinuityHierarchy(NovelAiAction action) {
        return switch (action) {
            case CONTINUE_SCENE,
                 INLINE_CONTINUATION,
                 SUGGEST_CONTINUATION,
                 PLAN_SCENE,
                 WRITE_CHAPTER_FROM_BEATS,
                 CREATE_NEXT_CHAPTER -> true;
            default -> false;
        };
    }

    private boolean allowsExplicitFactualEdit(NovelAiAction action) {
        return action == NovelAiAction.REWRITE_SELECTION;
    }

    static RewriteLimits rewriteLimits(String selectedText) {
        int sourceWords = countRewriteWords(selectedText);
        if (sourceWords <= 3) {
            return new RewriteLimits(sourceWords, 1, 8, 64, true);
        }
        if (sourceWords <= 30) {
            int maximumWords = Math.max(10, (int) Math.ceil(sourceWords * 1.80));
            return new RewriteLimits(
                    sourceWords,
                    Math.max(2, (int) Math.floor(sourceWords * 0.50)),
                    maximumWords,
                    rewriteOutputTokens(maximumWords),
                    true
            );
        }
        if (sourceWords <= 180) {
            int maximumWords = (int) Math.ceil(sourceWords * 1.50);
            return new RewriteLimits(
                    sourceWords,
                    (int) Math.floor(sourceWords * 0.60),
                    maximumWords,
                    rewriteOutputTokens(maximumWords),
                    false
            );
        }

        int maximumWords = (int) Math.ceil(sourceWords * 1.35);
        return new RewriteLimits(
                sourceWords,
                (int) Math.floor(sourceWords * 0.70),
                maximumWords,
                rewriteOutputTokens(maximumWords),
                false
        );
    }

    static void validateRewrite(String selectedText, String rewrittenText) {
        RewriteLimits limits = rewriteLimits(selectedText);
        String normalized = rewrittenText == null ? "" : rewrittenText.trim();
        int resultWords = countRewriteWords(normalized);

        if (normalized.isEmpty()) {
            throw new IllegalStateException("A reescrita retornou um texto vazio.");
        }
        if (startsWithRewriteMetaComment(normalized)) {
            throw new IllegalStateException("A reescrita retornou uma explicação em vez do texto.");
        }
        if (resultWords < limits.minimumWords() || resultWords > limits.maximumWords()) {
            throw new IllegalStateException("A reescrita não preservou uma extensão proporcional.");
        }
        if (limits.singleParagraph() && normalized.matches("(?s).*\\R\\s*\\R.*")) {
            throw new IllegalStateException("A reescrita curta foi expandida em vários parágrafos.");
        }
    }

    static int countRewriteWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        var matcher = REWRITE_WORD.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int rewriteOutputTokens(int maximumWords) {
        return Math.min(1000, Math.max(64, maximumWords * 2 + 32));
    }

    private static boolean startsWithRewriteMetaComment(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.startsWith("aqui está")
                || lower.startsWith("aqui esta")
                || lower.startsWith("claro,")
                || lower.startsWith("reescrita:")
                || lower.startsWith("here is")
                || lower.startsWith("here's")
                || lower.startsWith("rewritten text:")
                || lower.startsWith("por supuesto")
                || lower.startsWith("aquí está")
                || lower.startsWith("aqui está")
                || lower.startsWith("texto reescrito:");
    }

    record RewriteLimits(
            int sourceWords,
            int minimumWords,
            int maximumWords,
            int maximumOutputTokens,
            boolean singleParagraph
    ) {
    }
}
