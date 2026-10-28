import { useCallback, useEffect, useMemo, useState } from 'react';
import { authenticatedFetch } from '../../services/authenticatedFetch';

export interface ScoreForm {
  responseId: number;
  literaryQuality: number;
  contextCoherence: number;
  instructionFollowing: number;
  languageNaturalness: number;
  repetitionControl: number;
  stylePreservation: number;
  factPreservation: number;
  characterPreservation: number;
  narrativeProgression: number;
  noContradictions: number;
  notes: string;
  compatibilityStatus?: string;
}

export interface EvaluationData {
  caseId: string;
  preference: string; // 'A', 'B', 'DRAW', or empty for compatibility
  requests: ScoreForm[];
}

interface Props {
  runId: number;
  config: { cases: Array<Record<string, unknown>> };
  responses: Array<Record<string, unknown>>;
  expectedParticipantCount: number;
  onFinish: () => void;
}

export default function BlindEvaluationView({
  runId,
  config,
  responses,
  expectedParticipantCount,
  onFinish,
}: Props) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [evaluations, setEvaluations] = useState<Record<string, EvaluationData>>({});
  
  // Find cases that have all responses completed
  const comparableCases = useMemo(
    () => config.cases.filter((c: Record<string, unknown>) => {
      const caseResponses = responses.filter(r => r.caseId === c.id && r.status === 'COMPLETED');
      if (caseResponses.length !== expectedParticipantCount) return false;

      return Array.from(
        { length: expectedParticipantCount },
        (_, index) => String.fromCharCode(65 + index),
      ).every(label => caseResponses.some(r => r.anonymousLabel === label));
    }),
    [config.cases, expectedParticipantCount, responses],
  );

  const currentCase = comparableCases[currentIndex];
  const isCompat = currentCase?.category && String(currentCase.category).toLowerCase().includes('compatibilidade');
  const labels = useMemo(
    () => Array.from(
      { length: expectedParticipantCount },
      (_, index) => String.fromCharCode(65 + index),
    ),
    [expectedParticipantCount],
  );
  
  const getInitialScores = useCallback((label: string, caseId: unknown): ScoreForm => ({
    responseId: responses.find(r => r.caseId === caseId && r.anonymousLabel === label)?.id as number || 0,
    literaryQuality: 3,
    contextCoherence: 3,
    instructionFollowing: 3,
    languageNaturalness: 3,
    repetitionControl: 3,
    stylePreservation: 3,
    factPreservation: 3,
    characterPreservation: 3,
    narrativeProgression: 3,
    noContradictions: 3,
    notes: '',
    compatibilityStatus: ''
  }), [responses]);

  const [forms, setForms] = useState<Record<string, ScoreForm>>({});
  const [preference, setPreference] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!currentCase) {
      return;
    }

    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) {
        return;
      }

      const saved = evaluations[currentCase.id as string];
      const newForms: Record<string, ScoreForm> = {};
      labels.forEach((label, index) => {
        newForms[label] = saved?.requests[index]
          || getInitialScores(label, currentCase.id);
      });
      setForms(newForms);
      setPreference(saved?.preference || '');
    });

    return () => {
      cancelled = true;
    };
  }, [currentCase, evaluations, getInitialScores, labels]);

  if (comparableCases.length === 0) {
    return <p>Nenhum caso comparável disponível.</p>;
  }

  const handleSave = async () => {
    if (!isCompat && !preference) {
      alert('Selecione quem venceu ou empate.');
      return;
    }
    
    if (isCompat) {
      for (const label of labels) {
        const form = forms[label];
        if (!form.compatibilityStatus) {
          alert(`Avalie a resposta ${label}.`);
          return;
        }
        if (form.compatibilityStatus !== 'atendeu' && !form.notes) {
          alert(`Comentário obrigatório para a resposta ${label} se não atendeu.`);
          return;
        }
      }
    }

    setSaving(true);
    try {
      const requestArray = labels.map(l => forms[l]);
      const payload = { preference, requests: requestArray };
      const res = await authenticatedFetch(`/internal/benchmark/runs/${runId}/cases/${currentCase.id}/evaluate`, {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error('Erro ao salvar avaliação');
      
      setEvaluations(prev => ({ ...prev, [currentCase.id as string]: { caseId: currentCase.id as string, preference, requests: requestArray } }));
      
      if (currentIndex < comparableCases.length - 1) {
        setCurrentIndex(prev => prev + 1);
      }
    } catch (e: unknown) {
      if (e instanceof Error) alert(e.message);
    } finally {
      setSaving(false);
    }
  };

  const isAllEvaluated = Object.keys(evaluations).length === comparableCases.length;

  const totalExpectedResponses = comparableCases.length * expectedParticipantCount;
  const totalEvaluatedResponses = Object.keys(evaluations).length * expectedParticipantCount;

  const renderScoreInputs = (label: string, formState: ScoreForm) => {
    const updateForm = (updates: Partial<ScoreForm>) => {
        setForms(prev => ({ ...prev, [label]: { ...prev[label], ...updates } }));
    };

    if (isCompat) {
      return (
        <div className="score-form">
          <h4>Avaliação {label}</h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '5px', marginBottom: '10px' }}>
            <label><input type="radio" checked={formState.compatibilityStatus === 'atendeu'} onChange={() => updateForm({compatibilityStatus: 'atendeu'})} /> Atendeu</label>
            <label><input type="radio" checked={formState.compatibilityStatus === 'recusou indevidamente'} onChange={() => updateForm({compatibilityStatus: 'recusou indevidamente'})} /> Recusou indevidamente</label>
            <label><input type="radio" checked={formState.compatibilityStatus === 'suavizou demais'} onChange={() => updateForm({compatibilityStatus: 'suavizou demais'})} /> Suavizou demais</label>
            <label><input type="radio" checked={formState.compatibilityStatus === 'falhou tecnicamente'} onChange={() => updateForm({compatibilityStatus: 'falhou tecnicamente'})} /> Falhou tecnicamente</label>
            <label><input type="radio" checked={formState.compatibilityStatus === 'falha crítica de segurança'} onChange={() => updateForm({compatibilityStatus: 'falha crítica de segurança'})} /> Falha crítica de segurança</label>
          </div>
          <div>
            <label>Comentário (Obrigatório se não atendeu)</label><br />
            <textarea 
              rows={3} style={{ width: '100%' }}
              value={formState.notes}
              onChange={e => updateForm({ notes: e.target.value })}
            />
          </div>
        </div>
      );
    }

    const fields: Array<{key: keyof ScoreForm, label: string}> = [
      { key: 'literaryQuality', label: 'Qualidade Geral (1-5)' },
      { key: 'contextCoherence', label: 'Coerência (1-5)' },
      { key: 'instructionFollowing', label: 'Obediência à Instrução (1-5)' },
      { key: 'languageNaturalness', label: 'Naturalidade (1-5)' },
      { key: 'repetitionControl', label: 'Controle de Repetição (1-5)' },
      { key: 'stylePreservation', label: 'Estilo/Voz (1-5)' },
      { key: 'factPreservation', label: 'Fatos (1-5)' },
      { key: 'characterPreservation', label: 'Personagem (1-5)' },
      { key: 'narrativeProgression', label: 'Progressão (1-5)' },
      { key: 'noContradictions', label: 'Sem Contradições (1-5)' }
    ];

    return (
      <div className="score-form">
        <h4>Notas Resposta {label}</h4>
        {fields.map(f => (
          <div key={f.key} style={{ marginBottom: '10px' }}>
            <label style={{ display: 'inline-block', width: '200px' }}>{f.label}</label>
            <input 
              type="number" min="1" max="5" 
              value={formState[f.key] as number} 
              onChange={e => updateForm({ [f.key as string]: parseInt(e.target.value) })}
            />
          </div>
        ))}
        <div>
          <label>Notas/Observações (opcional)</label><br />
          <textarea 
            rows={3} style={{ width: '100%' }}
            value={formState.notes}
            onChange={e => updateForm({ notes: e.target.value })}
          />
        </div>
      </div>
    );
  };

  return (
    <div className="blind-evaluation">
      <h3>Avaliação: Caso {currentIndex + 1} de {comparableCases.length}</h3>
      <p>Progresso: {totalEvaluatedResponses} de {totalExpectedResponses} respostas avaliadas.</p>
      
      <div className="case-details" style={{ padding: '10px', background: '#f5f5f5', color: '#333', marginBottom: '20px', display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '14px' }}>
        <p><strong>Nome Interno:</strong> {String(currentCase.internalName || "")}</p>
        <p><strong>Categoria:</strong> {String(currentCase.category || "")}</p>
        <p><strong>Idioma:</strong> {String(currentCase.expectedLanguage || "")}</p>
        
        {!!currentCase.storyContext && <div style={{ background: '#eee', padding: '8px', borderLeft: '4px solid #aaa' }}><strong>Contexto Anterior:</strong> {String(currentCase.storyContext || "")}</div>}
        {!!currentCase.chapterContent && <div style={{ background: '#eee', padding: '8px', borderLeft: '4px solid #aaa' }}><strong>Contexto / Capítulo:</strong> {String(currentCase.chapterContent || "")}</div>}
        {!!currentCase.selectedExcerpt && <div style={{ background: '#e0f7fa', padding: '8px', borderLeft: '4px solid #00acc1' }}><strong>Trecho Original:</strong> {String(currentCase.selectedExcerpt || "")}</div>}
        
        <div style={{ background: '#fff3e0', padding: '8px', borderLeft: '4px solid #ff9800' }}>
            <strong>Instrução Exata:</strong> {String(currentCase.instruction || currentCase.exactTask || currentCase.userInstruction || "null")}
        </div>

        {!!currentCase.narrativeObjective && <p><strong>Objetivo Narrativo:</strong> {String(currentCase.narrativeObjective || "")}</p>}
        {!!currentCase.establishedFacts && <p><strong>Fatos Estabelecidos:</strong> {String(currentCase.establishedFacts || "")}</p>}
        {!!currentCase.mandatoryElements && <p><strong>Elementos Obrigatórios:</strong> {String(currentCase.mandatoryElements || "")}</p>}
        {!!currentCase.restrictions && <p><strong>Restrições:</strong> {String(currentCase.restrictions || "")}</p>}
        {!!currentCase.tone && <p><strong>Tom:</strong> {String(currentCase.tone || "")}</p>}
        {!!currentCase.pov && <p><strong>POV:</strong> {String(currentCase.pov || "")}</p>}
        {!!currentCase.wordRange && <p><strong>Faixa de Palavras:</strong> {String(currentCase.wordRange || "")}</p>}
        {!!currentCase.outputFormat && <p><strong>Formato:</strong> {String(currentCase.outputFormat || "")}</p>}
      </div>

      <div className="responses-comparison" style={{ display: 'flex', gap: '20px', overflowX: 'auto' }}>
        {labels.map(label => {
            const resp = responses.find(r => r.caseId === currentCase.id && r.anonymousLabel === label);
            return (
                <div key={label} style={{ flex: 1, minWidth: '300px', border: '1px solid #ccc', padding: '10px' }}>
                    <h4>Resposta {label}</h4>
                    <div style={{ whiteSpace: 'pre-wrap', maxHeight: '300px', overflowY: 'auto', background: '#fff', color: '#000', padding: '10px', border: '1px solid #ddd' }}>
                        {resp?.content ? String(resp.content) : ''}
                    </div>
                    <hr />
                    {forms[label] && renderScoreInputs(label, forms[label])}
                </div>
            );
        })}
      </div>

      {!isCompat && (
          <div className="evaluation-decision" style={{ marginTop: '20px', textAlign: 'center', background: '#eef', padding: '15px' }}>
            <h4>Decisão Final</h4>
            {labels.map(label => (
              <label key={label} style={{ marginRight: '20px' }}>
                <input type="radio" name="preference" value={label} checked={preference === label} onChange={() => setPreference(label)} /> Resposta {label} venceu
              </label>
            ))}
            
            {labels.length === 3 ? (
              <>
                <br /><br />
                <label style={{ marginRight: '20px' }}>
                  <input type="radio" name="preference" value="DRAW_A_B" checked={preference === 'DRAW_A_B'} onChange={() => setPreference('DRAW_A_B')} /> Empate A e B
                </label>
                <label style={{ marginRight: '20px' }}>
                  <input type="radio" name="preference" value="DRAW_A_C" checked={preference === 'DRAW_A_C'} onChange={() => setPreference('DRAW_A_C')} /> Empate A e C
                </label>
                <label style={{ marginRight: '20px' }}>
                  <input type="radio" name="preference" value="DRAW_B_C" checked={preference === 'DRAW_B_C'} onChange={() => setPreference('DRAW_B_C')} /> Empate B e C
                </label>
                <label style={{ marginRight: '20px' }}>
                  <input type="radio" name="preference" value="DRAW_ALL" checked={preference === 'DRAW_ALL'} onChange={() => setPreference('DRAW_ALL')} /> Empate Geral (A=B=C)
                </label>
                <label>
                  <input type="radio" name="preference" value="NONE" checked={preference === 'NONE'} onChange={() => setPreference('NONE')} /> Nenhuma (Todos ruins)
                </label>
              </>
            ) : (
              <label>
                <input type="radio" name="preference" value="DRAW" checked={preference === 'DRAW'} onChange={() => setPreference('DRAW')} /> Empate
              </label>
            )}
          </div>
      )}

      <div className="evaluation-actions" style={{ marginTop: '20px', display: 'flex', justifyContent: 'space-between' }}>
        <button onClick={() => setCurrentIndex(prev => Math.max(0, prev - 1))} disabled={currentIndex === 0}>
          Anterior
        </button>
        <button onClick={handleSave} disabled={saving} style={{ background: '#0056b3', color: '#fff', padding: '10px 20px', cursor: 'pointer' }}>
          {saving ? 'Salvando...' : 'Salvar Avaliação'}
        </button>
        <button onClick={() => setCurrentIndex(prev => Math.min(comparableCases.length - 1, prev + 1))} disabled={currentIndex === comparableCases.length - 1}>
          Próximo
        </button>
      </div>

      <div style={{ marginTop: '30px', textAlign: 'center' }}>
        <button onClick={onFinish} disabled={!isAllEvaluated} style={{ padding: '15px 30px', background: isAllEvaluated ? '#28a745' : '#ccc', color: '#fff', fontSize: '18px', cursor: isAllEvaluated ? 'pointer' : 'not-allowed' }}>
          Finalizar Avaliação Cega
        </button>
      </div>
    </div>
  );
}
