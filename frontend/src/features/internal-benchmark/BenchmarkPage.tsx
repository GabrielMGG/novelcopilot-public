import { useEffect, useEffectEvent, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

interface BenchmarkConfig {
  cases: Array<{
    id: string; internalName: string; category: string; expectedLanguage: string;
    instruction?: string; context?: string;
    chapterContent?: string; selectedExcerpt?: string;
    genre?: string; characters?: string; establishedFacts?: string;
    narrativeObjective?: string; exactTask?: string; mandatoryElements?: string;
    restrictions?: string; tone?: string; pov?: string;
    wordRange?: string; outputFormat?: string; approvalStatus?: string;
  }>;
  participants: Record<string, {
    enabled: boolean;
    provider: string;
    model: string;
    displayName: string;
    referenceModel: boolean;
    inputPricePerMillion: number;
    outputPricePerMillion: number;
  }>;
  totalCases: number;
  maxRunCostUsd: number;
  maxDailyCostUsd: number;
}

interface BenchmarkEstimate {
  caseCount: number;
  totalCalls: number;
  maxProviderCalls: number;
  estimatedInputTokens: number;
  estimatedOutputTokens: number;
  baseEstimatedCost: number;
  maxEstimatedCost: number;
}

interface BenchmarkRunStatus {
  run: { status: string; id: number };
  responses?: { caseId: string; status: string; estimatedCostUsd?: number; participantId?: string; anonymousLabel: string; errorCode?: string }[];
  completedCalls: number;
  failedCalls: number;
  totalCalls: number;
  suiteType?: string;
  expectedParticipantCount?: number;
  expectedResponses?: number;
  completedResponses?: number;
  failedResponses?: number;
  completeCases?: number;
  incompleteCases?: number;
  evaluableResponses?: number;
  evaluatedResponses?: number;
  evaluationCompleted?: boolean;
  reportAvailable?: boolean;
  totalCostUsd?: number;
}

interface BenchmarkRunHistoryItem {
  id: number;
  suiteName: string;
  status: string;
  createdAt: string;
  completedAt: string;
  expectedCalls: number;
  completedCalls: number;
  failedCalls: number;
  evaluatedCases: number;
  totalCostUsd: number;
}

import './BenchmarkPage.css';
import { authenticatedFetch } from '../../services/authenticatedFetch';
import BlindEvaluationView from './BlindEvaluationView';
import FinalReportView from './FinalReportView';

export default function BenchmarkPage() {
  const { runId: runIdParam } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  
  const [config, setConfig] = useState<BenchmarkConfig | null>(null);
  const [history, setHistory] = useState<BenchmarkRunHistoryItem[]>([]);
  
  const [selectedSuite, setSelectedSuite] = useState<string>('');
  const [selectedCases, setSelectedCases] = useState<string[]>([]);
  const [selectedParticipants, setSelectedParticipants] = useState<string[]>([]);
  const [estimate, setEstimate] = useState<BenchmarkEstimate | null>(null);
  
  const [runStatus, setRunStatus] = useState<BenchmarkRunStatus | null>(null);
  const [evaluating, setEvaluating] = useState(false);
  const [reportMode, setReportMode] = useState(false);
  const [recalculatingCosts, setRecalculatingCosts] = useState(false);
  
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const handleRecalculateCosts = async () => {
    if (!runIdParam) return;
    setRecalculatingCosts(true);
    try {
      const res = await authenticatedFetch(`/internal/benchmark/runs/${runIdParam}/recalculate-costs`, { method: 'POST' });
      if (!res.ok) throw new Error('Erro ao recalcular custos');
      const data = await res.json();
      alert(`Custos recalculados com sucesso!\nRespostas corrigidas: ${data.updatedResponses}\nCusto Anterior: US$ ${data.previousCostUsd.toFixed(8)}\nNovo Custo: US$ ${data.newCostUsd.toFixed(8)}\nFonte: ${data.source}`);
      loadRunStatus(Number(runIdParam));
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : 'Erro ao recalcular custos');
    } finally {
      setRecalculatingCosts(false);
    }
  };

  async function loadConfigAndHistory() {
    setLoading(true);
    setError('');

    try {
      const resConfig = await authenticatedFetch('/internal/benchmark/config');
      if (resConfig.status === 401) throw new Error('Sessão expirada ou não autorizada.');
      if (!resConfig.ok) throw new Error(`Erro ao carregar o benchmark: ${resConfig.statusText}`);
      const dataConfig: BenchmarkConfig = await resConfig.json();
      setConfig(dataConfig);

      const resHistory = await authenticatedFetch('/internal/benchmark/runs');
      if (resHistory.ok) {
        const dataHistory: BenchmarkRunHistoryItem[] = await resHistory.json();
        setHistory(dataHistory);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro desconhecido');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void Promise.resolve().then(loadConfigAndHistory);
  }, []);

  const loadRunStatus = async (id: number) => {
    try {
      const res = await authenticatedFetch(`/internal/benchmark/runs/${id}`);
      if (res.status === 401) throw new Error('Sessão expirada ou não autorizada.');
      if (!res.ok) throw new Error(`Erro ${res.status} ao carregar a execução`);
      
      const data = await res.json();
      setRunStatus(data);
      
      if (data.run.status === 'RUNNING') {
        setEvaluating(false);
        setReportMode(false);
        pollStatus(id);
      } else if (data.run.status === 'COMPLETED' || data.run.status === 'COMPLETED_WITH_ERRORS' || data.run.status === 'FAILED') {
        // We handle resuming evaluations via the UI button if it hasn't started, or directly if it's already in progress.
        // But for now, we just show the summary unless they specifically requested to evaluate.
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar a execução');
    }
  };
  const loadRunStatusFromEffect = useEffectEvent(loadRunStatus);

  useEffect(() => {
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) {
        return;
      }

      setRunStatus(null);
      setEvaluating(false);
      setReportMode(false);

      if (runIdParam) {
        void loadRunStatusFromEffect(Number(runIdParam));
      }
    });

    return () => {
      cancelled = true;
    };
  }, [runIdParam]);

  const toggleCase = (id: string) => setSelectedCases(prev => prev.includes(id) ? prev.filter(c => c !== id) : [...prev, id]);
  const toggleParticipant = (id: string) => setSelectedParticipants(prev => prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]);

  const getEstimate = async () => {
    try {
      const res = await authenticatedFetch('/internal/benchmark/estimate', {
        method: 'POST',
        body: JSON.stringify({ caseIds: selectedCases, participantIds: selectedParticipants })
      });
      if (res.status === 401) throw new Error('Sessão expirada ou não autorizada.');
      if (res.status === 429) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Limite do benchmark excedido. Aguarde antes de tentar novamente.');
      }
      if (!res.ok) throw new Error(`Erro ${res.status}`);
      const data = await res.json();
      setEstimate(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error loading estimate');
    }
  };

  const startRun = async () => {
    if (!selectedSuite) {
      alert('Por favor, selecione uma suíte.');
      return;
    }
    
    if (estimate?.maxEstimatedCost && config && estimate.maxEstimatedCost > config.maxRunCostUsd) {
      alert('Custo estimado ultrapassa o limite permitido.');
      return;
    }
    
    if (estimate?.totalCalls !== selectedCases.length * selectedParticipants.length) {
      alert('Quantidade esperada não corresponde à seleção.');
      return;
    }

if (selectedSuite === 'LITERARIO' && config?.cases.some(c => selectedCases.includes(c.id) && c.approvalStatus === 'PRECISA_REVISAO')) {
      alert('Existem casos na suíte literária que precisam de revisão manual. A execução está bloqueada.');
      return;
    }
    
    if (selectedSuite === 'LITERARIO') {
        const missingContext = config?.cases.some(c => selectedCases.includes(c.id) && c.category.toLowerCase().includes('continua') && (!c.chapterContent || c.chapterContent.trim() === ''));
        const missingExcerpt = config?.cases.some(c => selectedCases.includes(c.id) && (c.category.toLowerCase().includes('reescrita') || c.category.toLowerCase().includes('revis')) && (!c.selectedExcerpt || c.selectedExcerpt.trim() === ''));
        if (missingContext || missingExcerpt) {
            alert('Existem casos na suíte literária faltando contexto anterior ou trecho original. A execução está bloqueada.');
            return;
        }
    }

    if (!window.confirm(`Esta execução fará ${estimate?.totalCalls || 0} chamadas reais e poderá consumir aproximadamente US$ ${estimate?.maxEstimatedCost || 0}. Deseja continuar?`)) {
      return;
    }
    try {
      const suiteTitle = selectedSuite === 'NC014_RETEST' ? 'NC014_RETEST_ARKENDU' :
                         selectedSuite === 'COMPATIBILIDADE' ? 'Compatibilidade mínima NovelCopilot' :
                         selectedSuite === 'LITERARIO' ? 'Benchmark literário decisório NovelCopilot' : 
                         selectedSuite === 'FINAL_FACTS_CHARACTERS' ? 'FINAL_FACTS_CHARACTERS' : 'Histórico / legado';
      const res = await authenticatedFetch('/internal/benchmark/run', {
        method: 'POST',
        body: JSON.stringify({ suiteName: suiteTitle, caseIds: selectedCases, participantIds: selectedParticipants })
      });
      if (res.status === 401) throw new Error('Sessão expirada ou não autorizada.');
      if (!res.ok) throw new Error(`Erro ${res.status}`);
      const data = await res.json();
      navigate(`/internal/benchmark/runs/${data.id}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error starting run');
    }
  };

  function pollStatus(id: number) {
    const interval = setInterval(async () => {
      try {
        const res = await authenticatedFetch(`/internal/benchmark/runs/${id}`);
        if (res.status === 401) {
          clearInterval(interval);
          setError('Sessão expirada ou não autorizada.');
          return;
        }
        if (!res.ok) return;
        const data = await res.json();
        setRunStatus(data);
        if (data.run.status === 'COMPLETED' || data.run.status === 'COMPLETED_WITH_ERRORS' || data.run.status === 'FAILED') {
          clearInterval(interval);
          loadConfigAndHistory(); // Refresh history
        }
      } catch {
        clearInterval(interval);
      }
    }, 2000);
  }

  const retryFailed = async () => {
    if (!runIdParam) return;
    try {
      const res = await authenticatedFetch(`/internal/benchmark/runs/${runIdParam}/retry-failed`, { method: 'POST' });
      if (res.status === 401) throw new Error('Sessão expirada');
      if (res.status === 429) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Limite de repetição do benchmark excedido.');
      }
      if (!res.ok) throw new Error('Erro ao retomar falhas');
      pollStatus(Number(runIdParam));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Error starting retry');
    }
  };

  const calculateComparableCases = () => {
    if (!runStatus) return { comparable: 0, incomplete: 0, totalCost: 0, errors: [], evaluableResponses: 0, expectedResponses: 0, completedResponses: 0, evaluatedResponses: 0, isCompat: false, missingData: true };
    
    // Check if mandatory fields are missing
    const missingData = runStatus.expectedResponses === undefined || 
                        runStatus.completedResponses === undefined || 
                        runStatus.completeCases === undefined;
                        
    const totalCost = runStatus.totalCostUsd || 0;
    const errors: { participantId: string; anonymousLabel: string; error: string }[] = [];

    if (runStatus.responses) {
      runStatus.responses.forEach(r => {
        if (r.status === 'FAILED') {
          errors.push({
            participantId: r.participantId || 'Unknown',
            anonymousLabel: r.anonymousLabel,
            error: r.errorCode || 'Unknown error'
          });
        }
      });
    }

    return { 
      comparable: runStatus.completeCases, 
      incomplete: runStatus.incompleteCases, 
      totalCost, 
      errors,
      evaluableResponses: runStatus.evaluableResponses,
      expectedResponses: runStatus.expectedResponses,
      completedResponses: runStatus.completedResponses,
      evaluatedResponses: runStatus.evaluatedResponses,
      isCompat: runStatus.suiteType === 'COMPATIBILITY',
      isLiterary: runStatus.suiteType === 'LITERARY_DECISION',
      missingData
    };
  };

  const hasApprovedCompatRun = useMemo(() => {
    return history.some(h => h.suiteName.includes('Compatibilidade') && (h.status === 'COMPLETED' || h.status === 'COMPLETED_WITH_ERRORS'));
  }, [history]);

  const selectCompatibilidade = () => {
    if (!config) return;
    const compatCases = config.cases.filter(c => c.category.toLowerCase().includes('compatibilidade')).map(c => c.id);
    const compatParticipants = ['openrouter-qwen3-235b', 'openrouter-mistral-small-4', 'openrouter-mistral-small-3-2'].filter(id => config.participants[id]);
    setSelectedCases(compatCases);
    setSelectedParticipants(compatParticipants);
    setEstimate(null);
  };
  
  const selectLiterario = () => {
    if (!config) return;
    const litCases = config.cases.filter(c => c.id.startsWith('lit-dec-')).map(c => c.id);
    setSelectedCases(litCases);
    setSelectedParticipants([]);
    setEstimate(null);
  };

  const selectFinalFactsCharacters = () => {
    if (!config) return;
    const finalCases = config.cases.filter(c => c.id.startsWith('lit-final-')).map(c => c.id);
    const finalParticipants = [
      'groq-llama',
      'openrouter-qwen3-235b',
      'openrouter-mistral-small-4',
    ].filter(id => config.participants[id]);
    setSelectedCases(finalCases);
    setSelectedParticipants(finalParticipants);
    setEstimate(null);
  };

  const selectNc014Retest = () => {
    if (!config) return;
    setSelectedCases(config.cases.filter(c => c.id.startsWith('nc014-retest-')).map(c => c.id));
    setSelectedParticipants([
      'groq-qwen-3-6-27b',
      'cloudflare-gemma',
      'cloudflare-llama4-scout',
    ].filter(id => config.participants[id]));
    setEstimate(null);
  };

  const clearSelection = () => {
    setSelectedCases([]);
    setSelectedParticipants([]);
    setEstimate(null);
  };

  if (loading) {
    return <div className="benchmark-container"><p>Carregando...</p></div>;
  }

  if (error) {
    return (
      <div className="benchmark-container">
        <h1>Internal Benchmark Panel</h1>
        <div className="error"><strong>Erro:</strong> <p>{error}</p></div>
        <button onClick={() => navigate('/internal/benchmark')}>Voltar</button>
      </div>
    );
  }

  if (!config) return <div className="benchmark-container"><p>A configuração do benchmark não foi recebida.</p></div>;

  const validCases = config.cases.filter(c => {
    if (selectedSuite === 'NC014_RETEST') return c.id.startsWith('nc014-retest-');
    if (selectedSuite === 'COMPATIBILIDADE') return c.category.toLowerCase().includes('compatibilidade');
    if (selectedSuite === 'LITERARIO') return c.id.startsWith('lit-dec-');
    if (selectedSuite === 'FINAL_FACTS_CHARACTERS') return c.id.startsWith('lit-final-');
    return true;
  });

  const participantEntries = Object.entries(config.participants).filter(([id]) => {
    if (selectedSuite === 'NC014_RETEST') {
      return ['groq-qwen-3-6-27b', 'cloudflare-gemma', 'cloudflare-llama4-scout'].includes(id);
    }
    if (selectedSuite === 'COMPATIBILIDADE') {
      return ['openrouter-qwen3-235b', 'openrouter-mistral-small-4', 'openrouter-mistral-small-3-2'].includes(id);
    }
    if (selectedSuite === 'FINAL_FACTS_CHARACTERS') {
      return [
        'groq-llama',
        'openrouter-qwen3-235b',
        'openrouter-mistral-small-4',
      ].includes(id);
    }
    return true;
  });
  
  const getParticipantStatusLabel = (id: string, p: { enabled: boolean }) => {
    if (id === 'groq-llama') return 'REFERÊNCIA HISTÓRICA';
    return p.enabled ? 'Ativo' : 'Desabilitado';
  };

  const isParticipantDisabled = (id: string, p: { enabled: boolean }) => {
    if (selectedSuite === 'NC014_RETEST') {
      return !['groq-qwen-3-6-27b', 'cloudflare-gemma', 'cloudflare-llama4-scout'].includes(id) || !p.enabled;
    }
    if (selectedSuite === 'COMPATIBILIDADE') {
      if (id === 'groq-llama' || id === 'groq-gpt-oss' || id === 'gemini') return true;
    }
    if (selectedSuite === 'FINAL_FACTS_CHARACTERS') {
      if (![
        'groq-llama',
        'openrouter-qwen3-235b',
        'openrouter-mistral-small-4',
      ].includes(id)) return true;
    }
    return !p.enabled;
  };

  return (
    <div className="benchmark-container">
      <h1>Internal Benchmark Panel</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <button onClick={() => navigate('/internal/benchmark')} disabled={!runIdParam}>Nova Execução</button>
      </div>

      {!runIdParam && (
        <>
          <div className="history-section" style={{ marginBottom: '40px', padding: '20px', border: '1px solid #ddd', borderRadius: '8px' }}>
            <h2>Histórico de Execuções</h2>
            {history.length === 0 ? (
              <p>Nenhuma execução encontrada.</p>
            ) : (
              <table style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #ccc' }}>
                    <th>ID</th>
                    <th>Nome</th>
                    <th>Data</th>
                    <th>Status</th>
                    <th>Chamadas</th>
                    <th>Custo</th>
                    <th>Casos Avaliados</th>
                    <th>Ação</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map(run => (
                    <tr key={run.id} style={{ borderBottom: '1px solid #eee' }}>
                      <td>{run.id}</td>
                      <td>{run.suiteName}</td>
                      <td>{new Date(run.createdAt).toLocaleString()}</td>
                      <td>
                        {run.status === 'RUNNING' && <span style={{ color: 'blue', fontWeight: 'bold' }}>Em Execução</span>}
                        {run.status === 'COMPLETED' && <span style={{ color: 'green', fontWeight: 'bold' }}>Concluído</span>}
                        {run.status === 'COMPLETED_WITH_ERRORS' && <span style={{ color: 'orange', fontWeight: 'bold' }}>Concluído com Erros</span>}
                        {run.status === 'FAILED' && <span style={{ color: 'red', fontWeight: 'bold' }}>Falhou</span>}
                      </td>
                      <td>{run.completedCalls} de {run.expectedCalls} ({run.failedCalls} erros)</td>
                      <td>US$ {run.totalCostUsd < 0.01 ? run.totalCostUsd.toFixed(8) : run.totalCostUsd.toFixed(6)}</td>
                      <td>{run.evaluatedCases}</td>
                      <td>
                        <button onClick={() => navigate(`/internal/benchmark/runs/${run.id}`)}>Abrir</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <div className="new-run-section">
            <h2>Iniciar Nova Execução</h2>
            
            <div style={{ marginBottom: '20px' }}>
              <label><strong>Selecionar Suíte: </strong></label>
              <select 
                value={selectedSuite} 
                onChange={(e) => {
                  setSelectedSuite(e.target.value);
                  clearSelection();
                }}
                style={{ padding: '8px', fontSize: '16px' }}
              >
                <option value="">-- Selecione --</option>
                <option value="NC014_RETEST">NC-014 — Reteste congelado de Arkendu</option>
                <option value="COMPATIBILIDADE">Compatibilidade mínima NovelCopilot</option>
                <option value="LITERARIO" disabled={!hasApprovedCompatRun}>Benchmark literário {!hasApprovedCompatRun ? '(Requer candidato aprovado na compatibilidade)' : ''}</option>
                <option value="FINAL_FACTS_CHARACTERS">Rodada final — Fatos e personagens</option>
                <option value="HISTORICO">Histórico / legado</option>
              </select>
            </div>

            {selectedSuite && (
              <>
                <div className="actions" style={{ marginBottom: '20px' }}>
                  {selectedSuite === 'COMPATIBILIDADE' && <button onClick={selectCompatibilidade}>Selecionar compatibilidade mínima</button>}
                  {selectedSuite === 'LITERARIO' && <button onClick={selectLiterario}>Selecionar benchmark literário</button>}
                  {selectedSuite === 'FINAL_FACTS_CHARACTERS' && <button onClick={selectFinalFactsCharacters}>Selecionar rodada final</button>}
                  {selectedSuite === 'NC014_RETEST' && <button onClick={selectNc014Retest}>Selecionar reteste NC-014</button>}
                  <button onClick={clearSelection}>Limpar Seleção</button>
                </div>

                {(selectedSuite === 'LITERARIO' || selectedSuite === 'NC014_RETEST') && selectedCases.length > 0 && config && (
                  <div className="preview-cases" style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f9f9f9', borderRadius: '8px' }}>
                    <h3>Pré-visualização dos Casos ({selectedCases.length})</h3>
                    {config.cases.filter(c => selectedCases.includes(c.id)).map(c => (
                      <div key={c.id} style={{ marginBottom: '15px', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', backgroundColor: c.approvalStatus === 'PRECISA_REVISAO' ? '#fff3cd' : '#fff' }}>
                        <h4>{c.internalName} ({c.id})</h4>
                        <p><strong>Categoria:</strong> {c.category} | <strong>Status:</strong> <span style={{ color: c.approvalStatus === 'PRECISA_REVISAO' ? 'red' : 'green', fontWeight: 'bold' }}>{c.approvalStatus || 'N/A'}</span></p>
                        <p><strong>Objetivo:</strong> {c.narrativeObjective}</p>
                        <p><strong>Instrução Exata:</strong> {c.exactTask || c.instruction}</p>
                        <p><strong>Restrições:</strong> {c.restrictions}</p>
                        <p><strong>Tom:</strong> {c.tone} | <strong>POV:</strong> {c.pov} | <strong>Formato:</strong> {c.outputFormat}</p>
                        <p><strong>Personagens:</strong> {c.characters}</p>
                      </div>
                    ))}
                    {config.cases.some(c => selectedCases.includes(c.id) && c.approvalStatus === 'PRECISA_REVISAO') && (
                      <p style={{ color: 'red', fontWeight: 'bold' }}>ATENÇÃO: Existem casos que precisam de revisão! A execução será bloqueada.</p>
                    )}
                  </div>
                )}

                {selectedSuite === 'FINAL_FACTS_CHARACTERS' && (
                  <div className="final-facts-info" style={{ marginTop: '10px', padding: '15px', backgroundColor: '#e8f4f8', borderRadius: '8px', border: '1px solid #bce8f1' }}>
                    <h3 style={{ color: '#31708f', marginTop: 0 }}>Rodada final — Fatos e personagens</h3>
                    <p>Comparação final entre o Groq Llama atual, Qwen3 235B e Mistral Small 4, focada em preservação de fatos, continuidade e personalidade.</p>
                    <ul style={{ margin: '10px 0', paddingLeft: '20px' }}>
                      <li>8 casos;</li>
                      <li>3 participantes;</li>
                      <li>24 chamadas esperadas;</li>
                      <li>avaliação cega A/B/C;</li>
                      <li>4 casos de fatos;</li>
                      <li>4 casos de personagem.</li>
                    </ul>
                  </div>
                )}

                <div className="participants-selection">
                  <h3>Participantes ({participantEntries.length})</h3>
                  {participantEntries.map(([id, p]) => (
                    <div key={id} className="participant-item">
                      <label>
                        <input type="checkbox" checked={selectedParticipants.includes(id)} onChange={() => toggleParticipant(id)} disabled={isParticipantDisabled(id, p)} />
                        <strong>{p.displayName}</strong> ({p.provider} / {p.model}) - {getParticipantStatusLabel(id, p)}
                        {p.referenceModel ? ' [Referência]' : ' [Candidato]'}
                      </label>
                    </div>
                  ))}
                </div>

                <div className="case-list">
                  <h3>Casos ({validCases.length})</h3>
                  {validCases.map((c) => (
                    <div key={c.id} className="case-item">
                      <label>
                        <input type="checkbox" checked={selectedCases.includes(c.id)} onChange={() => toggleCase(c.id)} />
                        {c.internalName} ({c.category}) - {c.expectedLanguage}
                      </label>
                    </div>
                  ))}
                </div>

                <div style={{ marginTop: '20px', padding: '15px', border: '1px solid #aaa', borderRadius: '5px', background: '#f9f9f9' }}>
                  <h4>Resumo da Seleção</h4>
                  <p><strong>Suíte Selecionada:</strong> {selectedSuite === 'NC014_RETEST' ? 'NC-014 — Reteste congelado de Arkendu' : selectedSuite === 'COMPATIBILIDADE' ? 'Compatibilidade mínima NovelCopilot' : selectedSuite === 'LITERARIO' ? 'Benchmark literário' : selectedSuite === 'FINAL_FACTS_CHARACTERS' ? 'Rodada final — Fatos e personagens' : 'Histórico / legado'}</p>
                  <p><strong>Participantes Selecionados:</strong> {selectedParticipants.length}</p>
                  <p><strong>Casos Selecionados:</strong> {selectedCases.length}</p>
                  
                  {selectedCases.length > 0 && selectedParticipants.length > 1 ? (
                    <button onClick={getEstimate} style={{ marginTop: '10px' }}>Gerar Estimativa de Custos</button>
                  ) : (
                    <p style={{ color: 'red' }}>Selecione pelo menos 2 participantes e 1 caso para gerar estimativa.</p>
                  )}
                </div>

                {estimate && (
                  <div className="estimate-box" style={{ marginTop: '20px' }}>
                    <h3>Estimativa de Execução (Máxima Aproximada)</h3>
                    <p><strong>Casos:</strong> {estimate.caseCount}</p>
                    <p><strong>Chamadas Esperadas:</strong> {estimate.totalCalls}</p>
                    <p><strong>Máximo com tentativas:</strong> {estimate.maxProviderCalls}</p>
                    <p><strong>Tokens Max Entrada:</strong> {estimate.estimatedInputTokens}</p>
                    <p><strong>Tokens Max Saída:</strong> {estimate.estimatedOutputTokens}</p>
                    <p><strong>Custo normal estimado:</strong> US$ {estimate.baseEstimatedCost}</p>
                    <p><strong>Custo máximo com tentativas:</strong> US$ {estimate.maxEstimatedCost}</p>
                    {estimate.totalCalls !== selectedCases.length * selectedParticipants.length && (
                       <p style={{ color: 'red', fontWeight: 'bold' }}>ATENÇÃO: Quantidade esperada não corresponde à seleção!</p>
                    )}
                    <button className="danger-btn" onClick={startRun} disabled={estimate.totalCalls !== selectedCases.length * selectedParticipants.length}>Confirmar e Executar Benchmark</button>
                  </div>
                )}
              </>
            )}
          </div>
        </>
      )}

      {runIdParam && !evaluating && runStatus && (
        <div className="run-status">
          <h2>Execução do Benchmark: #{runStatus.run.id}</h2>
          
          {(() => {
            const stats = calculateComparableCases();
            if (stats.missingData) {
              return <div className="error"><p style={{ color: 'red', fontWeight: 'bold' }}>Dados incompletos retornados pelo backend para esta execução.</p></div>;
            }
            return (
              <>
                <p>Status: {runStatus.run.status}</p>
                <p>Progresso: {runStatus.responses?.length || 0} de {stats.expectedResponses}</p>
                <p>Concluídas: {stats.completedResponses}</p>
                <p>Falhas: {(runStatus.failedResponses ?? runStatus.failedCalls ?? 0)}</p>

                {(runStatus.run.status === 'COMPLETED' || runStatus.run.status === 'COMPLETED_WITH_ERRORS' || runStatus.run.status === 'FAILED') && (
                  <div className="run-summary">
                    <h3>Resumo da Execução</h3>
                    
                    <p>{stats.isCompat ? 'Casos completos' : (stats.isLiterary ? 'Casos completos' : 'Casos completamente comparáveis')}: {stats.comparable}</p>
                    <p>Casos incompletos: {stats.incomplete}</p>
                    <p>Respostas avaliadas: {stats.evaluatedResponses} de {stats.evaluableResponses}</p>
                    <p>Custo Real: US$ {stats.totalCost < 0.01 ? stats.totalCost.toFixed(8) : stats.totalCost.toFixed(6)}</p>
                    
                    {stats.errors.length > 0 && (
                      <div className="errors-list">
                        <h4>Erros por participante:</h4>
                        <ul>
                          {stats.errors.map((e, idx) => (
                            <li key={idx}>Participante: {e.participantId} - Erro: {e.error}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {((runStatus.failedResponses ?? 0) > 0 || (runStatus.failedCalls ?? 0) > 0) && (
                      <div>
                        <p className="warning">Execução concluída parcialmente</p>
                        <button onClick={retryFailed}>Reexecutar somente falhas</button>
                      </div>
                    )}
                    
                    <div style={{ marginTop: '20px' }}>
                      <button onClick={() => setEvaluating(true)} disabled={(stats.evaluableResponses ?? 0) === 0 || (runStatus.failedResponses ?? 0) > 0 || (runStatus.failedCalls ?? 0) > 0}>
                        {stats.isCompat ? 'Ir para Avaliação de Compatibilidade' : (stats.isLiterary ? 'Ir para Avaliação Literária Cega' : 'Ir para Avaliação Cega')}
                      </button>
                      <button onClick={() => { setEvaluating(true); setReportMode(true); }} style={{ marginLeft: '10px' }} disabled={!runStatus.reportAvailable}>
                        Visualizar Relatório Final
                      </button>
                      {((stats.completedResponses ?? 0) > 0) && stats.totalCost === 0 && (
                        <button 
                          onClick={handleRecalculateCosts} 
                          style={{ marginLeft: '10px', backgroundColor: '#f0ad4e', color: 'white', border: 'none', padding: '8px 12px', borderRadius: '4px', cursor: 'pointer' }}
                          disabled={recalculatingCosts}
                        >
                          {recalculatingCosts ? 'Recalculando...' : 'Recalcular custos'}
                        </button>
                      )}
                      
                      {!runStatus.reportAvailable && (stats.evaluableResponses ?? 0) > 0 && (
                        <p className="warning">Relatório disponível após avaliar {stats.evaluableResponses} de {stats.evaluableResponses} respostas.</p>
                      )}
                      
                      {stats.expectedResponses !== stats.completedResponses && (
                        <p className="warning">A execução só pode ser considerada totalmente COMPLETED quando todas as chamadas esperadas estiverem concluídas.</p>
                      )}
                    </div>
                  </div>
                )}
              </>
            );
          })()}
        </div>
      )}

      {evaluating && (
        <div className="evaluation-mode">
          {!reportMode ? (
            <BlindEvaluationView 
              runId={Number(runIdParam)} 
              config={config} 
              responses={runStatus?.responses || []} 
              expectedParticipantCount={runStatus?.expectedParticipantCount || 2}
              onFinish={() => setReportMode(true)} 
            />
          ) : (
            <FinalReportView runId={Number(runIdParam)} />
          )}
        </div>
      )}
    </div>
  );
}
