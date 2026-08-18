import { useEffect, useState } from 'react';
import {
  getGoals,
  getGoal,
  createGoal,
  updateGoal,
  deleteGoal,
  startGoal,
  pauseGoal,
  resumeGoal,
  completeGoal,
  failGoal,
  archiveGoal,
  updateGoalProgress,
  getGoalStatistics,
  createGoalMilestone,
  completeGoalMilestone,
  deleteGoalMilestone,
} from '../api/goalApi';
import './goals.css';
import { getTasks } from '../api/taskApi';

const PRIORITY_OPTIONS = ['LOW', 'NORMAL', 'HIGH', 'CRITICAL'];
const DIFFICULTY_OPTIONS = ['EASY', 'NORMAL', 'HARD', 'EXTREME'];
const TYPE_OPTIONS = [
  'LONG_TERM',
  'SHORT_TERM',
  'PROJECT',
  'LEARNING',
  'HEALTH',
  'CAREER',
  'HABIT',
  'CUSTOM',
];
const VISIBILITY_OPTIONS = ['PRIVATE', 'FRIENDS', 'PUBLIC'];
const STRATEGY_OPTIONS = [
  'MANUAL',
  'TASK_BASED',
  'XP_BASED',
  'MILESTONE_BASED',
  'PERCENTAGE',
  'CUSTOM',
];

const FILTER_TABS = [
  { key: 'ALL', label: 'All' },
  { key: 'DRAFT', label: 'Draft' },
  { key: 'ACTIVE', label: 'Active' },
  { key: 'PAUSED', label: 'Paused' },
  { key: 'COMPLETED', label: 'Completed' },
  { key: 'FAILED', label: 'Failed' },
  { key: 'ARCHIVED', label: 'Archived' },
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

function titleCase(value) {
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

function GoalForm({ initial, onCancel, onSubmit, submitting, error }) {
  const [title, setTitle] = useState(initial?.title || '');
  const [description, setDescription] = useState(initial?.description || '');
  const [category, setCategory] = useState(initial?.category || '');
  const [priority, setPriority] = useState(initial?.priority || 'NORMAL');
  const [difficulty, setDifficulty] = useState(initial?.difficulty || 'NORMAL');
  const [type, setType] = useState(initial?.type || 'LONG_TERM');
  const [visibility, setVisibility] = useState(initial?.visibility || 'PRIVATE');
  const [estimatedXp, setEstimatedXp] = useState(initial?.estimatedXp ?? '');
  const [targetDate, setTargetDate] = useState(toDatetimeLocal(initial?.targetDate));
  const [completionStrategy, setCompletionStrategy] = useState(
    initial?.completionStrategy || 'MANUAL',
  );
  const [tags, setTags] = useState((initial?.tags || []).join(', '));

  const handleSubmit = (event) => {
    event.preventDefault();
    const xp = parseInt(estimatedXp, 10);
    const payload = {
      title: title.trim(),
      description: description.trim() || null,
      category: category.trim() || null,
      priority,
      difficulty,
      type: initial ? undefined : type,
      visibility,
      estimatedXp: Number.isFinite(xp) && xp >= 0 ? xp : null,
      targetDate: targetDate ? new Date(targetDate).toISOString() : null,
      completionStrategy,
      tags: parseTags(tags),
      customMetadata: null,
    };
    onSubmit(payload);
  };

  return (
    <form className="goal-form" onSubmit={handleSubmit}>
      <div className="goal-form-grid">
        <div className="goal-form-field goal-form-title">
          <label htmlFor="goal-title">Title *</label>
          <input
            id="goal-title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={255}
            placeholder="Goal title"
            required
          />
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-category">Category</label>
          <input
            id="goal-category"
            type="text"
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            maxLength={50}
            placeholder="e.g. Career, Health"
          />
        </div>

        <div className="goal-form-field goal-form-description">
          <label htmlFor="goal-description">Description</label>
          <textarea
            id="goal-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={5000}
            rows={3}
            placeholder="What are you working towards?"
          />
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-priority">Priority</label>
          <select
            id="goal-priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value)}
          >
            {PRIORITY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {titleCase(option)}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-difficulty">Difficulty</label>
          <select
            id="goal-difficulty"
            value={difficulty}
            onChange={(event) => setDifficulty(event.target.value)}
          >
            {DIFFICULTY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {titleCase(option)}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-type">Type</label>
          <select id="goal-type" value={type} onChange={(event) => setType(event.target.value)}>
            {TYPE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {titleCase(option)}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-visibility">Visibility</label>
          <select
            id="goal-visibility"
            value={visibility}
            onChange={(event) => setVisibility(event.target.value)}
          >
            {VISIBILITY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {titleCase(option)}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-strategy">Completion strategy</label>
          <select
            id="goal-strategy"
            value={completionStrategy}
            onChange={(event) => setCompletionStrategy(event.target.value)}
          >
            {STRATEGY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {titleCase(option)}
              </option>
            ))}
          </select>
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-xp">Estimated XP</label>
          <input
            id="goal-xp"
            type="number"
            min="0"
            max="100000"
            value={estimatedXp}
            onChange={(event) => setEstimatedXp(event.target.value)}
            placeholder="e.g. 500"
          />
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-target">Target date</label>
          <input
            id="goal-target"
            type="datetime-local"
            value={targetDate}
            onChange={(event) => setTargetDate(event.target.value)}
          />
        </div>

        <div className="goal-form-field">
          <label htmlFor="goal-tags">Tags (comma separated)</label>
          <input
            id="goal-tags"
            type="text"
            value={tags}
            onChange={(event) => setTags(event.target.value)}
            placeholder="career, milestone"
          />
        </div>
      </div>

      {error && <div className="goal-form-error">{error}</div>}

      <div className="goal-form-actions">
        <button type="button" className="goal-btn goal-btn-ghost" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="goal-btn goal-btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : initial ? 'Save changes' : 'Create goal'}
        </button>
      </div>
    </form>
  );
}

function GoalStats({ stats }) {
  const items = [
    { label: 'Total', value: stats?.totalGoals ?? 0 },
    { label: 'Active', value: stats?.activeGoals ?? 0 },
    { label: 'Completed', value: stats?.completedGoals ?? 0 },
    { label: 'Failed', value: stats?.failedGoals ?? 0 },
    { label: 'Avg progress', value: `${Math.round((stats?.averageCompletionPercentage ?? 0) * 10) / 10}%` },
  ];

  return (
    <div className="goal-stats">
      {items.map((item) => (
        <section className="goal-stats-item" key={item.label}>
          <p className="goal-stats-label">{item.label}</p>
          <p className="goal-stats-value">{item.value}</p>
        </section>
      ))}
    </div>
  );
}

function GoalCard({ goal, linked, busy, detailOpen, onLifecycle, onEdit, onDelete, onToggleDetails }) {
  const progress = Math.round((goal.completionPercentage ?? goal.currentProgress ?? 0) * 10) / 10;
  const isTerminal = goal.status === 'COMPLETED' || goal.status === 'ARCHIVED';
  const isTaskBased = goal.completionStrategy === 'TASK_BASED';

  const lifecycleButtons = [
    { status: 'DRAFT', action: 'start', label: 'Start' },
    { status: 'ACTIVE', action: 'pause', label: 'Pause' },
    { status: 'ACTIVE', action: 'complete', label: 'Complete' },
    { status: 'ACTIVE', action: 'fail', label: 'Fail' },
    { status: 'PAUSED', action: 'resume', label: 'Resume' },
    { status: 'FAILED', action: 'resume', label: 'Retry' },
    { status: 'COMPLETED', action: 'archive', label: 'Archive' },
    { status: 'FAILED', action: 'archive', label: 'Archive' },
    { status: 'DRAFT', action: 'archive', label: 'Archive' },
    { status: 'PAUSED', action: 'archive', label: 'Archive' },
  ].filter((button) => button.status === goal.status && !(button.action === 'complete' && isTaskBased));

  return (
    <article className={`goal-card goal-card-${goal.status.toLowerCase()}`}>
      <div className="goal-card-main">
        <div className="goal-card-title-row">
          <h3 className="goal-card-title">{goal.title}</h3>
          <span className={`goal-badge goal-badge-status goal-status-${goal.status.toLowerCase()}`}>
            {titleCase(goal.status)}
          </span>
          {goal.priority && (
            <span className={`goal-badge goal-badge-priority goal-priority-${goal.priority.toLowerCase()}`}>
              {goal.priority}
            </span>
          )}
          {goal.difficulty && <span className="goal-badge goal-badge-difficulty">{goal.difficulty}</span>}
          {goal.type && <span className="goal-badge">{titleCase(goal.type)}</span>}
        </div>

        {goal.description && <p className="goal-card-description">{goal.description}</p>}

        <div className="goal-card-progress">
          <div className="goal-card-progress-row">
            <span className="goal-card-progress-label">Progress</span>
            <span className="goal-card-progress-value">{progress}%</span>
          </div>
          <div className="goal-progress-track">
            <div className="goal-progress-fill" style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} />
          </div>
        </div>

        <div className="goal-card-meta">
          {goal.category && <span className="goal-meta-item">#{goal.category}</span>}
          {goal.targetDate && (
            <span className="goal-meta-item">Target {formatDate(goal.targetDate)}</span>
          )}
          {goal.completedDate && (
            <span className="goal-meta-item">Completed {formatDate(goal.completedDate)}</span>
          )}
          {goal.estimatedXp > 0 && <span className="goal-meta-item">{goal.estimatedXp} XP</span>}
          {goal.completionStrategy && (
            <span className="goal-meta-item">{titleCase(goal.completionStrategy)}</span>
          )}
          {linked && linked.total > 0 && (
            <span className="goal-meta-item">
              {linked.completed}/{linked.total} tasks done
            </span>
          )}
          {goal.tags && goal.tags.length > 0 && (
            <span className="goal-meta-item">{goal.tags.join(' · ')}</span>
          )}
        </div>
      </div>

      <div className="goal-card-actions">
        {lifecycleButtons.map((button) => (
          <button
            key={`${button.action}-${goal.id}`}
            type="button"
            className="goal-btn goal-btn-secondary"
            disabled={busy}
            onClick={() => onLifecycle(button.action, goal)}
          >
            {button.label}
          </button>
        ))}
        {!isTerminal && (
          <button
            type="button"
            className="goal-btn goal-btn-ghost"
            disabled={busy}
            onClick={() => onEdit(goal)}
          >
            Edit
          </button>
        )}
        <button
          type="button"
          className="goal-btn goal-btn-ghost"
          disabled={busy}
          onClick={onToggleDetails}
        >
          {detailOpen ? 'Close' : 'Details'}
        </button>
        <button
          type="button"
          className="goal-btn goal-btn-danger"
          disabled={busy}
          onClick={() => onDelete(goal)}
        >
          Delete
        </button>
      </div>
    </article>
  );
}

function MilestonePanel({ goal, milestones, busy, onAddMilestone, onCompleteMilestone, onDeleteMilestone, onUpdateProgress, onClose }) {
  const [title, setTitle] = useState('');
  const [progress, setProgress] = useState(goal.currentProgress ?? 0);

  const handleAdd = (event) => {
    event.preventDefault();
    const trimmed = title.trim();
    if (!trimmed) return;
    onAddMilestone({ title: trimmed });
    setTitle('');
  };

  return (
    <section className="goal-detail">
      <div className="goal-detail-header">
        <h2 className="goal-detail-title">{goal.title}</h2>
        <button type="button" className="goal-btn goal-btn-ghost" onClick={onClose}>
          Close
        </button>
      </div>

      <div className="goal-detail-progress">
        <div className="goal-card-progress-row">
          <span className="goal-card-progress-label">Progress</span>
          <span className="goal-card-progress-value">{progress}%</span>
        </div>
        <div className="goal-progress-track">
          <div className="goal-progress-fill" style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} />
        </div>
        {goal.completionStrategy !== 'TASK_BASED' && (
          <>
            <div className="goal-card-progress-row">
              <span className="goal-card-progress-label">Set progress</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              step="1"
              value={progress}
              onChange={(event) => setProgress(Number(event.target.value))}
            />
            <button
              type="button"
              className="goal-btn goal-btn-primary"
              disabled={busy}
              onClick={() => onUpdateProgress(progress)}
            >
              Save progress
            </button>
          </>
        )}
        {goal.completionStrategy === 'TASK_BASED' && (
          <p className="goal-detail-empty">Progress is derived from linked tasks.</p>
        )}
      </div>

      <h3 className="goal-detail-subtitle">Milestones</h3>
      {milestones.length === 0 ? (
        <p className="goal-detail-empty">No milestones yet.</p>
      ) : (
        <ul className="goal-milestones">
          {milestones.map((milestone) => (
            <li
              key={milestone.id}
              className={`goal-milestone${milestone.isCompleted ? ' goal-milestone-completed' : ''}`}
            >
              <div className="goal-milestone-main">
                <p className="goal-milestone-title">{milestone.title}</p>
                {milestone.description && (
                  <p className="goal-milestone-description">{milestone.description}</p>
                )}
              </div>
              <div className="goal-milestone-actions">
                {!milestone.isCompleted && (
                  <button
                    type="button"
                    className="goal-btn goal-btn-secondary"
                    disabled={busy}
                    onClick={() => onCompleteMilestone(milestone)}
                  >
                    Complete
                  </button>
                )}
                <button
                  type="button"
                  className="goal-btn goal-btn-danger"
                  disabled={busy}
                  onClick={() => onDeleteMilestone(milestone)}
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <form className="goal-milestone-form" onSubmit={handleAdd}>
        <input
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={255}
          placeholder="New milestone title"
        />
        <button type="submit" className="goal-btn goal-btn-primary" disabled={!title.trim() || busy}>
          Add milestone
        </button>
      </form>
    </section>
  );
}

export default function GoalsPage() {
  const [goals, setGoals] = useState([]);
  const [stats, setStats] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [busyGoalId, setBusyGoalId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [detailGoalId, setDetailGoalId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [tasksByGoal, setTasksByGoal] = useState({});

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const goalResult = await getGoals(filter === 'ALL' ? {} : { status: filter });
      if (cancelled) return;
      if (goalResult.data?.success) {
        setGoals(goalResult.data.data || []);
        setLoadError(null);
      } else {
        setLoadError(errorMessage(goalResult, 'Failed to load goals.'));
        setGoals([]);
      }
      const statResult = await getGoalStatistics();
      if (!cancelled && statResult.data?.success) setStats(statResult.data.data);
      const taskResult = await getTasks();
      if (!cancelled && taskResult.data?.success) {
        const grouped = {};
        (taskResult.data.data || []).forEach((task) => {
          if (!task.goalId) return;
          if (!grouped[task.goalId]) grouped[task.goalId] = { completed: 0, total: 0 };
          grouped[task.goalId].total += 1;
          if (task.status === 'COMPLETED') grouped[task.goalId].completed += 1;
        });
        setTasksByGoal(grouped);
      }
      if (!cancelled) setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [filter, reloadKey]);

  useEffect(() => {
    if (!detailGoalId) {
      return;
    }
    let cancelled = false;
    (async () => {
      const result = await getGoal(detailGoalId);
      if (!cancelled && result.data?.success) {
        setDetail({ goalId: detailGoalId, data: result.data.data });
      } else if (!cancelled) {
        setActionError(errorMessage(result, 'Failed to load goal details.'));
        setDetailGoalId(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [detailGoalId, reloadKey]);

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
    const result = await createGoal(payload);
    if (result.data?.success) {
      setShowForm(false);
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to create goal.'));
    }
    setSubmitting(false);
  };

  const handleUpdate = async (payload) => {
    setSubmitting(true);
    setFormError(null);
    const result = await updateGoal(editing.id, payload);
    if (result.data?.success) {
      setEditing(null);
      reload();
    } else {
      setFormError(errorMessage(result, 'Failed to update goal.'));
    }
    setSubmitting(false);
  };

  const runLifecycle = async (action, goal) => {
    let reason = null;
    if (action === 'fail') {
      reason = window.prompt('Reason for failing this goal?') ?? '';
    }
    if (action === 'archive' && !window.confirm(`Archive goal "${goal.title}"?`)) return;
    setBusyGoalId(goal.id);
    setActionError(null);
    const call = {
      start: startGoal,
      pause: pauseGoal,
      resume: resumeGoal,
      complete: completeGoal,
      fail: (id) => failGoal(id, reason),
      archive: archiveGoal,
    }[action];
    const result = await call(goal.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, `Failed to ${action} goal.`));
    }
    setBusyGoalId(null);
  };

  const handleDelete = async (goal) => {
    if (!window.confirm(`Delete goal "${goal.title}"?`)) return;
    setBusyGoalId(goal.id);
    setActionError(null);
    const result = await deleteGoal(goal.id);
    if (result.data?.success) {
      if (detailGoalId === goal.id) setDetailGoalId(null);
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to delete goal.'));
    }
    setBusyGoalId(null);
  };

  const toggleDetails = (goal) => {
    setActionError(null);
    setDetailGoalId((current) => (current === goal.id ? null : goal.id));
  };

  const handleAddMilestone = async (payload) => {
    setBusyGoalId(detailGoalId);
    setActionError(null);
    const result = await createGoalMilestone(detailGoalId, payload);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to add milestone.'));
    }
    setBusyGoalId(null);
  };

  const handleCompleteMilestone = async (milestone) => {
    setBusyGoalId(detailGoalId);
    setActionError(null);
    const result = await completeGoalMilestone(detailGoalId, milestone.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to complete milestone.'));
    }
    setBusyGoalId(null);
  };

  const handleDeleteMilestone = async (milestone) => {
    if (!window.confirm(`Delete milestone "${milestone.title}"?`)) return;
    setBusyGoalId(detailGoalId);
    setActionError(null);
    const result = await deleteGoalMilestone(detailGoalId, milestone.id);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to delete milestone.'));
    }
    setBusyGoalId(null);
  };

  const handleUpdateProgress = async (progress) => {
    setBusyGoalId(detailGoalId);
    setActionError(null);
    const result = await updateGoalProgress(detailGoalId, progress);
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to update progress.'));
    }
    setBusyGoalId(null);
  };

  const detailData = detail?.goalId === detailGoalId ? detail.data : null;
  const detailMilestones = detailData?.milestones || [];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Goals</h1>
          <p className="page-subtitle">Define long-term objectives and track milestones.</p>
        </div>
        <button
          type="button"
          className="goal-btn goal-btn-primary goal-new-btn"
          onClick={() => {
            setEditing(null);
            setFormError(null);
            setShowForm((open) => !open);
          }}
        >
          {showForm && !editing ? 'Close' : 'New Goal'}
        </button>
      </header>

      <GoalStats stats={stats} />

      {showForm && !editing && (
        <GoalForm
          onCancel={() => setShowForm(false)}
          onSubmit={handleCreate}
          submitting={submitting}
          error={formError}
        />
      )}

      {editing && (
        <GoalForm
          initial={editing}
          onCancel={() => setEditing(null)}
          onSubmit={handleUpdate}
          submitting={submitting}
          error={formError}
        />
      )}

      {actionError && <div className="goal-banner-error">{actionError}</div>}

      <div className="goal-tabs" role="tablist" aria-label="Filter goals by status">
        {FILTER_TABS.map((tab) => (
          <button
            type="button"
            key={tab.key}
            role="tab"
            aria-selected={filter === tab.key}
            className={`goal-tab ${filter === tab.key ? 'goal-tab-active' : ''}`}
            onClick={() => selectFilter(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading && <div className="goal-state">Loading goals...</div>}

      {!loading && loadError && (
        <div className="goal-state goal-state-error">
          <p>{loadError}</p>
          <button type="button" className="goal-btn goal-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && goals.length === 0 && (
        <div className="goal-state">
          <p>No goals in this view.</p>
        </div>
      )}

      {!loading && !loadError && goals.length > 0 && (
        <div className="goal-list">
          {goals.map((goal) => (
            <div className="goal-card-wrap" key={goal.id}>
              <GoalCard
                goal={goal}
                linked={tasksByGoal[goal.id]}
                busy={busyGoalId === goal.id}
                detailOpen={detailGoalId === goal.id}
                onLifecycle={runLifecycle}
                onEdit={(selected) => {
                  setFormError(null);
                  setShowForm(false);
                  setEditing(selected);
                }}
                onDelete={handleDelete}
                onToggleDetails={() => toggleDetails(goal)}
              />
              {detailGoalId === goal.id && detailData && (
                <MilestonePanel
                  goal={detailData}
                  milestones={detailMilestones}
                  busy={busyGoalId === goal.id}
                  onAddMilestone={handleAddMilestone}
                  onCompleteMilestone={handleCompleteMilestone}
                  onDeleteMilestone={handleDeleteMilestone}
                  onUpdateProgress={handleUpdateProgress}
                  onClose={() => setDetailGoalId(null)}
                />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
