import { useEffect, useState } from 'react';
import {
  getMemories,
  createMemory,
  updateMemory,
  deleteMemory,
} from '../api/memoryApi';
import './memories.css';

const TYPE_OPTIONS = ['NOTE', 'FACT', 'PREFERENCE', 'INSIGHT', 'SIGNAL'];
const IMPORTANCE_OPTIONS = ['LOW', 'NORMAL', 'HIGH', 'CRITICAL'];
const SOURCE_OPTIONS = ['MANUAL', 'TASK', 'GOAL', 'AI'];

const FILTER_TABS = [
  { key: 'ALL', label: 'All' },
  { key: 'NOTE', label: 'Notes' },
  { key: 'FACT', label: 'Facts' },
  { key: 'PREFERENCE', label: 'Preferences' },
  { key: 'INSIGHT', label: 'Insights' },
  { key: 'SIGNAL', label: 'Signals' },
];

function formatDate(iso) {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function parseTags(value) {
  return value
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function errorMessage(result, fallback) {
  return result?.data?.error?.message || fallback;
}

function MemoryForm({ initial, onCancel, onSubmit, submitting, error }) {
  const [title, setTitle] = useState(initial?.title || '');
  const [content, setContent] = useState(initial?.content || '');
  const [type, setType] = useState(initial?.type || 'NOTE');
  const [importance, setImportance] = useState(initial?.importance || 'NORMAL');
  const [source, setSource] = useState(initial?.source || 'MANUAL');
  const [tags, setTags] = useState((initial?.tags || []).join(', '));

  const handleSubmit = (event) => {
    event.preventDefault();
    const payload = {
      title: title.trim(),
      content: content.trim(),
      type,
      importance,
      source,
      tags: parseTags(tags),
    };
    onSubmit(payload);
  };

  return (
    <form className="memory-form" onSubmit={handleSubmit}>
      <div className="memory-form-grid">
        <div className="memory-form-field memory-form-title">
          <label htmlFor="memory-title">Title *</label>
          <input
            id="memory-title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={255}
            placeholder="What should the system remember?"
            required
          />
        </div>

        <div className="memory-form-field">
          <label htmlFor="memory-type">Type</label>
          <select id="memory-type" value={type} onChange={(event) => setType(event.target.value)}>
            {TYPE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="memory-form-field">
          <label htmlFor="memory-importance">Importance</label>
          <select
            id="memory-importance"
            value={importance}
            onChange={(event) => setImportance(event.target.value)}
          >
            {IMPORTANCE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="memory-form-field">
          <label htmlFor="memory-source">Source</label>
          <select id="memory-source" value={source} onChange={(event) => setSource(event.target.value)}>
            {SOURCE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="memory-form-field memory-form-content">
          <label htmlFor="memory-content">Content *</label>
          <textarea
            id="memory-content"
            value={content}
            onChange={(event) => setContent(event.target.value)}
            maxLength={10000}
            rows={4}
            placeholder="Details worth keeping..."
            required
          />
        </div>

        <div className="memory-form-field">
          <label htmlFor="memory-tags">Tags (comma separated)</label>
          <input
            id="memory-tags"
            type="text"
            value={tags}
            onChange={(event) => setTags(event.target.value)}
            placeholder="fitness, routine, health"
          />
        </div>
      </div>

      {error && <div className="memory-form-error">{error}</div>}

      <div className="memory-form-actions">
        <button type="button" className="memory-btn memory-btn-ghost" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="memory-btn memory-btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : initial ? 'Save changes' : 'Save memory'}
        </button>
      </div>
    </form>
  );
}

function MemoryCard({ memory, onEdit, onDelete, busy }) {
  return (
    <article className={`memory-card memory-card-${memory.type.toLowerCase()}`}>
      <div className="memory-card-main">
        <div className="memory-card-title-row">
          <h3 className="memory-card-title">{memory.title}</h3>
          <span className={`memory-badge memory-badge-type memory-type-${memory.type.toLowerCase()}`}>
            {memory.type}
          </span>
          <span
            className={`memory-badge memory-badge-importance memory-importance-${memory.importance.toLowerCase()}`}
          >
            {memory.importance}
          </span>
          <span className="memory-badge memory-badge-source">{memory.source}</span>
        </div>

        {memory.content && <p className="memory-card-content">{memory.content}</p>}

        <div className="memory-card-meta">
          {memory.createdAt && (
            <span className="memory-meta-item">Added {formatDate(memory.createdAt)}</span>
          )}
          {memory.tags && memory.tags.length > 0 && (
            <span className="memory-meta-item">{memory.tags.join(' · ')}</span>
          )}
        </div>
      </div>

      <div className="memory-card-actions">
        <button
          type="button"
          className="memory-btn memory-btn-ghost"
          disabled={busy}
          onClick={() => onEdit(memory)}
        >
          Edit
        </button>
        <button
          type="button"
          className="memory-btn memory-btn-danger"
          disabled={busy}
          onClick={() => onDelete(memory)}
        >
          Delete
        </button>
      </div>
    </article>
  );
}

export default function MemoriesPage() {
  const [memories, setMemories] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const result = await getMemories({
        ...(filter === 'ALL' ? {} : { type: filter }),
        ...(debouncedSearch ? { search: debouncedSearch } : {}),
      });
      if (cancelled) return;
      if (result.data?.success) {
        setMemories(result.data.data || []);
        setLoadError(null);
      } else {
        setLoadError(errorMessage(result, 'Failed to load memories.'));
        setMemories([]);
      }
      setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [filter, debouncedSearch, reloadKey]);

  const reload = () => {
    setLoading(true);
    setReloadKey((key) => key + 1);
  };

  const selectFilter = (tabKey) => {
    setFilter(tabKey);
    setLoading(true);
    setLoadError(null);
  };

  const handleCreate = async (payload) => {
    setSubmitting(true);
    setFormError(null);
    const result = await createMemory(payload);
    if (result.data?.success) {
      setShowForm(false);
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to save memory.'));
    }
    setSubmitting(false);
  };

  const handleUpdate = async (payload) => {
    setSubmitting(true);
    setFormError(null);
    const result = await updateMemory(editing.id, payload);
    if (result.data?.success) {
      setEditing(null);
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to update memory.'));
    }
    setSubmitting(false);
  };

  const handleDelete = async (memory) => {
    if (!window.confirm(`Delete memory "${memory.title}"?`)) return;
    setBusyId(memory.id);
    setActionError(null);
    const result = await deleteMemory(memory.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to delete memory.'));
    }
    setBusyId(null);
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Memories</h1>
          <p className="page-subtitle">Retain what matters, so the system remembers with you.</p>
        </div>
        <button
          type="button"
          className="memory-btn memory-btn-primary memory-new-btn"
          onClick={() => {
            setEditing(null);
            setFormError(null);
            setShowForm((open) => !open);
          }}
        >
          {showForm && !editing ? 'Close' : 'New Memory'}
        </button>
      </header>

      {showForm && !editing && (
        <MemoryForm
          onCancel={() => setShowForm(false)}
          onSubmit={handleCreate}
          submitting={submitting}
          error={formError}
        />
      )}

      {editing && (
        <MemoryForm
          initial={editing}
          onCancel={() => setEditing(null)}
          onSubmit={handleUpdate}
          submitting={submitting}
          error={formError}
        />
      )}

      {actionError && <div className="memory-banner-error">{actionError}</div>}

      <div className="memory-toolbar">
        <div className="memory-tabs" role="tablist" aria-label="Filter memories by type">
          {FILTER_TABS.map((tab) => (
            <button
              type="button"
              key={tab.key}
              role="tab"
              aria-selected={filter === tab.key}
              className={`memory-tab ${filter === tab.key ? 'memory-tab-active' : ''}`}
              onClick={() => selectFilter(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <input
          type="search"
          className="memory-search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search memories..."
          aria-label="Search memories"
        />
      </div>

      {loading && <div className="memory-state">Loading memories...</div>}

      {!loading && loadError && (
        <div className="memory-state memory-state-error">
          <p>{loadError}</p>
          <button type="button" className="memory-btn memory-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && memories.length === 0 && (
        <div className="memory-state">
          <p>No memories in this view.</p>
        </div>
      )}

      {!loading && !loadError && memories.length > 0 && (
        <div className="memory-list">
          {memories.map((memory) => (
            <MemoryCard
              key={memory.id}
              memory={memory}
              busy={busyId === memory.id}
              onEdit={(selected) => {
                setFormError(null);
                setShowForm(false);
                setEditing(selected);
              }}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
