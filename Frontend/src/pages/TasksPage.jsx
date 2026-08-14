import { useEffect, useState } from 'react';
import {
  getTasks,
  createTask,
  updateTask,
  deleteTask,
  completeTask,
  getTaskStatistics,
} from '../api/taskApi';
import './tasks.css';

const STATUS_OPTIONS = [
  'DRAFT',
  'PENDING',
  'IN_PROGRESS',
  'BLOCKED',
  'PAUSED',
  'COMPLETED',
  'FAILED',
  'CANCELLED',
  'ARCHIVED',
];

const PRIORITY_OPTIONS = ['LOW', 'NORMAL', 'HIGH', 'CRITICAL'];
const DIFFICULTY_OPTIONS = ['EASY', 'NORMAL', 'HARD', 'EXTREME'];

const FILTER_TABS = [
  { key: 'ALL', label: 'All' },
  { key: 'DRAFT', label: 'Draft' },
  { key: 'PENDING', label: 'Pending' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'COMPLETED', label: 'Completed' },
  { key: 'FAILED', label: 'Failed' },
  { key: 'ARCHIVED', label: 'Archived' },
];

const TERMINAL_STATUSES = ['COMPLETED', 'CANCELLED', 'ARCHIVED'];

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

function isOverdue(task) {
  if (!task.dueDate) return false;
  if (TERMINAL_STATUSES.includes(task.status) || task.status === 'FAILED') return false;
  return new Date(task.dueDate).getTime() < Date.now();
}

function toDatetimeLocal(iso) {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
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

function TaskForm({ initial, onCancel, onSubmit, submitting, error }) {
  const [title, setTitle] = useState(initial?.title || '');
  const [description, setDescription] = useState(initial?.description || '');
  const [category, setCategory] = useState(initial?.category || '');
  const [priority, setPriority] = useState(initial?.priority || 'NORMAL');
  const [difficulty, setDifficulty] = useState(initial?.difficulty || 'NORMAL');
  const [status, setStatus] = useState(initial?.status || 'DRAFT');
  const [dueDate, setDueDate] = useState(toDatetimeLocal(initial?.dueDate));
  const [tags, setTags] = useState((initial?.tags || []).join(', '));

  const handleSubmit = (event) => {
    event.preventDefault();
    const payload = {
      title: title.trim(),
      description: description.trim() || null,
      category: category.trim() || null,
      priority,
      difficulty,
      status,
      dueDate: dueDate ? new Date(dueDate).toISOString() : null,
      tags: parseTags(tags),
    };
    onSubmit(payload);
  };

  return (
    <form className="task-form" onSubmit={handleSubmit}>
      <div className="task-form-grid">
        <div className="task-form-field task-form-title">
          <label htmlFor="task-title">Title *</label>
          <input
            id="task-title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={255}
            placeholder="Task title"
            required
          />
        </div>

        <div className="task-form-field">
          <label htmlFor="task-category">Category</label>
          <input
            id="task-category"
            type="text"
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            maxLength={100}
            placeholder="e.g. Work, Health"
          />
        </div>

        <div className="task-form-field task-form-description">
          <label htmlFor="task-description">Description</label>
          <textarea
            id="task-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={5000}
            rows={3}
            placeholder="What needs to be done?"
          />
        </div>

        <div className="task-form-field">
          <label htmlFor="task-priority">Priority</label>
          <select
            id="task-priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value)}
          >
            {PRIORITY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="task-form-field">
          <label htmlFor="task-difficulty">Difficulty</label>
          <select
            id="task-difficulty"
            value={difficulty}
            onChange={(event) => setDifficulty(event.target.value)}
          >
            {DIFFICULTY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option.charAt(0) + option.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        <div className="task-form-field">
          <label htmlFor="task-status">Status</label>
          <select
            id="task-status"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}
              </option>
            ))}
          </select>
        </div>

        <div className="task-form-field">
          <label htmlFor="task-due">Due date</label>
          <input
            id="task-due"
            type="datetime-local"
            value={dueDate}
            onChange={(event) => setDueDate(event.target.value)}
          />
        </div>

        <div className="task-form-field">
          <label htmlFor="task-tags">Tags (comma separated)</label>
          <input
            id="task-tags"
            type="text"
            value={tags}
            onChange={(event) => setTags(event.target.value)}
            placeholder="work, focus, urgent"
          />
        </div>
      </div>

      {error && <div className="task-form-error">{error}</div>}

      <div className="task-form-actions">
        <button type="button" className="task-btn task-btn-ghost" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="task-btn task-btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : initial ? 'Save changes' : 'Create task'}
        </button>
      </div>
    </form>
  );
}

function TaskCard({ task, onComplete, onEdit, onDelete, busy }) {
  const overdue = isOverdue(task);
  const canComplete = task.status === 'IN_PROGRESS';

  return (
    <article className={`task-card task-card-${task.status.toLowerCase()}`}>
      <div className="task-card-main">
        <div className="task-card-title-row">
          <h3 className="task-card-title">{task.title}</h3>
          <span className={`task-badge task-badge-status task-status-${task.status.toLowerCase()}`}>
            {task.status.replace(/_/g, ' ')}
          </span>
          <span className={`task-badge task-badge-priority task-priority-${task.priority.toLowerCase()}`}>
            {task.priority}
          </span>
          <span className="task-badge task-badge-difficulty">{task.difficulty}</span>
        </div>

        {task.description && <p className="task-card-description">{task.description}</p>}

        <div className="task-card-meta">
          {task.category && <span className="task-meta-item">#{task.category}</span>}
          {task.dueDate && (
            <span className={`task-meta-item ${overdue ? 'task-overdue' : ''}`}>
              Due {formatDate(task.dueDate)}
              {overdue ? ' · overdue' : ''}
            </span>
          )}
          {task.tags && task.tags.length > 0 && (
            <span className="task-meta-item">{task.tags.join(' · ')}</span>
          )}
        </div>
      </div>

      <div className="task-card-actions">
        <button
          type="button"
          className="task-btn task-btn-complete"
          disabled={!canComplete || busy}
          title={
            canComplete
              ? 'Mark as completed'
              : 'Status must be In Progress to complete a task'
          }
          onClick={() => onComplete(task)}
        >
          {busy ? '...' : 'Complete'}
        </button>
        <button
          type="button"
          className="task-btn task-btn-ghost"
          disabled={busy}
          onClick={() => onEdit(task)}
        >
          Edit
        </button>
        <button
          type="button"
          className="task-btn task-btn-danger"
          disabled={busy}
          onClick={() => onDelete(task)}
        >
          Delete
        </button>
      </div>
    </article>
  );
}

function TaskStats({ stats }) {
  const items = [
    { label: 'Total', value: stats?.totalTasks ?? 0 },
    { label: 'Completed', value: stats?.completedTasks ?? 0 },
    { label: 'Failed', value: stats?.failedTasks ?? 0 },
    { label: 'Overdue', value: stats?.overdueTasks ?? 0 },
  ];

  return (
    <div className="task-stats">
      {items.map((item) => (
        <section className="task-stats-item" key={item.label}>
          <p className="task-stats-label">{item.label}</p>
          <p className="task-stats-value">{item.value}</p>
        </section>
      ))}
    </div>
  );
}

export default function TasksPage() {
  const [tasks, setTasks] = useState([]);
  const [stats, setStats] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [busyTaskId, setBusyTaskId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const taskResult = await getTasks(filter === 'ALL' ? {} : { status: filter });
      if (cancelled) return;
      if (taskResult.data?.success) {
        setTasks(taskResult.data.data || []);
        setLoadError(null);
      } else {
        setLoadError(errorMessage(taskResult, 'Failed to load tasks.'));
        setTasks([]);
      }
      const statResult = await getTaskStatistics();
      if (!cancelled && statResult.data?.success) setStats(statResult.data.data);
      if (!cancelled) setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [filter, reloadKey]);

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
    const result = await createTask(payload);
    if (result.data?.success) {
      setShowForm(false);
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to create task.'));
    }
    setSubmitting(false);
  };

  const handleUpdate = async (payload) => {
    setSubmitting(true);
    setFormError(null);
    const result = await updateTask(editing.id, payload);
    if (result.data?.success) {
      setEditing(null);
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to update task.'));
    }
    setSubmitting(false);
  };

  const handleComplete = async (task) => {
    setBusyTaskId(task.id);
    setActionError(null);
    const result = await completeTask(task.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(
        `${errorMessage(result, 'Failed to complete task.')} Change status to In Progress first.`,
      );
    }
    setBusyTaskId(null);
  };

  const handleDelete = async (task) => {
    if (!window.confirm(`Delete task "${task.title}"?`)) return;
    setBusyTaskId(task.id);
    setActionError(null);
    const result = await deleteTask(task.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to delete task.'));
    }
    setBusyTaskId(null);
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Tasks</h1>
          <p className="page-subtitle">Plan, execute, and close out your daily missions.</p>
        </div>
        <button
          type="button"
          className="task-btn task-btn-primary task-new-btn"
          onClick={() => {
            setEditing(null);
            setFormError(null);
            setShowForm((open) => !open);
          }}
        >
          {showForm && !editing ? 'Close' : 'New Task'}
        </button>
      </header>

      <TaskStats stats={stats} />

      {showForm && !editing && (
        <TaskForm
          onCancel={() => setShowForm(false)}
          onSubmit={handleCreate}
          submitting={submitting}
          error={formError}
        />
      )}

      {editing && (
        <TaskForm
          initial={editing}
          onCancel={() => setEditing(null)}
          onSubmit={handleUpdate}
          submitting={submitting}
          error={formError}
        />
      )}

      {actionError && <div className="task-banner-error">{actionError}</div>}

      <div className="task-tabs" role="tablist" aria-label="Filter tasks by status">
        {FILTER_TABS.map((tab) => (
          <button
            type="button"
            key={tab.key}
            role="tab"
            aria-selected={filter === tab.key}
            className={`task-tab ${filter === tab.key ? 'task-tab-active' : ''}`}
            onClick={() => selectFilter(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading && <div className="task-state">Loading tasks...</div>}

      {!loading && loadError && (
        <div className="task-state task-state-error">
          <p>{loadError}</p>
          <button type="button" className="task-btn task-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && tasks.length === 0 && (
        <div className="task-state">
          <p>No tasks in this view.</p>
        </div>
      )}

      {!loading && !loadError && tasks.length > 0 && (
        <div className="task-list">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              busy={busyTaskId === task.id}
              onComplete={handleComplete}
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
