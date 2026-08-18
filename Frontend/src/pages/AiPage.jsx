import { useEffect, useState } from 'react';
import {
  createAiInteraction,
  getAiInteractions,
  deleteAiInteraction,
} from '../api/aiApi';
import './ai.css';

function formatDate(iso) {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

function errorMessage(result, fallback) {
  return result?.data?.error?.message || fallback;
}

function ContextBadges({ interaction }) {
  const meta = [];
  if (interaction.provider) meta.push(interaction.provider);
  if (interaction.model) meta.push(interaction.model);
  if (interaction.totalTokens != null) meta.push(`${interaction.totalTokens} tokens`);
  if (meta.length === 0) return null;
  return (
    <div className="ai-meta">
      {meta.map((item) => (
        <span key={item} className="ai-meta-item">
          {item}
        </span>
      ))}
    </div>
  );
}

function HistoryCard({ interaction, onDelete, busy }) {
  return (
    <article className="ai-history-card">
      <div className="ai-history-qa">
        <p className="ai-history-question">{interaction.message}</p>
        <p className="ai-history-answer">{interaction.response}</p>
        <ContextBadges interaction={interaction} />
        {interaction.createdAt && (
          <span className="ai-meta-item">Ran {formatDate(interaction.createdAt)}</span>
        )}
      </div>
      <div className="ai-history-actions">
        <button
          type="button"
          className="ai-btn ai-btn-danger"
          disabled={busy}
          onClick={() => onDelete(interaction)}
        >
          Delete
        </button>
      </div>
    </article>
  );
}

export default function AiPage() {
  const [message, setMessage] = useState('');
  const [includeMemoryContext, setIncludeMemoryContext] = useState(true);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const result = await getAiInteractions();
      if (cancelled) return;
      if (result.data?.success) {
        setHistory(result.data.data || []);
        setLoadError(null);
      } else {
        setLoadError(errorMessage(result, 'Failed to load AI history.'));
        setHistory([]);
      }
      setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const reload = () => {
    setLoading(true);
    setReloadKey((key) => key + 1);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const trimmed = message.trim();
    if (!trimmed) return;
    setSubmitting(true);
    setFormError(null);
    const result = await createAiInteraction({
      message: trimmed,
      includeMemoryContext,
    });
    if (result.data?.success) {
      setMessage('');
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to run AI interaction.'));
    }
    setSubmitting(false);
  };

  const handleDelete = async (interaction) => {
    if (!window.confirm('Delete this AI interaction?')) return;
    setBusyId(interaction.id);
    setActionError(null);
    const result = await deleteAiInteraction(interaction.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to delete AI interaction.'));
    }
    setBusyId(null);
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">AI</h1>
          <p className="page-subtitle">Ask the system, backed by your memories.</p>
        </div>
      </header>

      <form className="ai-composer" onSubmit={handleSubmit}>
        <label className="ai-composer-label" htmlFor="ai-message">
          Message
        </label>
        <textarea
          id="ai-message"
          className="ai-composer-input"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          maxLength={4000}
          rows={4}
          placeholder="e.g. Summarize what I should focus on today."
          required
        />
        <div className="ai-composer-footer">
          <label className="ai-checkbox">
            <input
              type="checkbox"
              checked={includeMemoryContext}
              onChange={(event) => setIncludeMemoryContext(event.target.checked)}
            />
            Include memory context
          </label>
          <button type="submit" className="ai-btn ai-btn-primary" disabled={submitting || !message.trim()}>
            {submitting ? 'Running...' : 'Run'}
          </button>
        </div>
        {formError && <div className="ai-form-error">{formError}</div>}
      </form>

      {actionError && <div className="ai-banner-error">{actionError}</div>}

      {loading && <div className="ai-state">Loading AI history...</div>}

      {!loading && loadError && (
        <div className="ai-state ai-state-error">
          <p>{loadError}</p>
          <button type="button" className="ai-btn ai-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && history.length === 0 && (
        <div className="ai-state">
          <p>No AI interactions yet.</p>
        </div>
      )}

      {!loading && !loadError && history.length > 0 && (
        <div className="ai-history">
          <h2 className="ai-history-title">History</h2>
          {history.map((interaction) => (
            <HistoryCard
              key={interaction.id}
              interaction={interaction}
              busy={busyId === interaction.id}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
