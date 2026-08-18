import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { getXpStatistics, getXpStreak, getXpTransactions } from '../api/xpApi';
import { getTaskStatistics } from '../api/taskApi';
import { getGoalStatistics } from '../api/goalApi';
import { getMemories } from '../api/memoryApi';
import { getAiInteractions } from '../api/aiApi';
import './dashboard.css';

function titleCase(value) {
  if (value == null) return '—';
  return String(value).replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

function formatNumber(value) {
  if (value == null) return '—';
  return Number(value).toLocaleString();
}

function formatDate(iso) {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function formatDateTime(iso) {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

function formatMinutes(mins) {
  const value = Number(mins) || 0;
  if (value <= 0) return '0m';
  const hours = Math.floor(value / 60);
  const minutes = value % 60;
  if (hours === 0) return `${minutes}m`;
  return `${hours}h ${minutes}m`;
}

function errorMessage(result, fallback) {
  return result?.data?.error?.message || result?.data?.message || fallback;
}

function Percent({ value }) {
  const clamped = Math.min(100, Math.max(0, Number(value) || 0));
  return (
    <div className="db-progress-track">
      <div className="db-progress-fill" style={{ width: `${clamped}%` }} />
    </div>
  );
}

function LevelWidget({ stats, statsOk, streak, streakOk }) {
  if (!statsOk) {
    return <p className="db-empty">Couldn&apos;t load progress data.</p>;
  }

  const level = stats?.currentLevel ?? 1;
  const progress = Math.min(100, Math.max(0, stats?.levelProgress ?? 0));

  return (
    <div className="db-level-card">
      <div className="db-level-main">
        <p className="db-card-label">Level</p>
        <p className="db-level-number">{level}</p>
        <p className="db-level-progress-text">
          {Math.round(progress * 10) / 10}% to next level
        </p>
        <Percent value={progress} />
        <div className="db-meta">
          <span className="db-meta-item">Lifetime {formatNumber(stats?.lifetimeXp)} XP</span>
          <span className="db-meta-item">Achievements {formatNumber(stats?.achievementsUnlocked)}</span>
        </div>
      </div>
      <div className="db-level-side">
        <p className="db-card-label">XP today</p>
        <p className="db-level-number db-streak-number">{formatNumber(stats?.dailyXp)}</p>
        <div className="db-meta">
          <span className="db-meta-item">Weekly {formatNumber(stats?.weeklyXp)} XP</span>
        </div>
        <p className="db-card-label db-streak-label">Streak</p>
        <p className="db-streak-value">
          {streakOk && streak?.currentStreak != null ? streak.currentStreak : '—'}
          <span className="db-streak-unit"> days</span>
        </p>
        <div className="db-meta">
          {streakOk && streak?.longestStreak != null && (
            <span className="db-meta-item">Longest {streak.longestStreak}</span>
          )}
          {streakOk && streak?.lastActivityDate && (
            <span className="db-meta-item">Last active {formatDate(streak.lastActivityDate)}</span>
          )}
        </div>
      </div>
    </div>
  );
}

function TasksWidget({ stats, ok }) {
  if (!ok) {
    return <p className="db-empty">Couldn&apos;t load task data.</p>;
  }

  const byStatus = stats?.tasksByStatus || {};
  const chips = Object.entries(byStatus)
    .filter(([, count]) => count > 0)
    .sort((a, b) => b[1] - a[1]);

  return (
    <>
      <div className="db-stat-row">
        <div className="db-stat">
          <p className="db-stat-label">Total</p>
          <p className="db-stat-value">{formatNumber(stats?.totalTasks)}</p>
        </div>
        <div className="db-stat">
          <p className="db-stat-label">Completed</p>
          <p className="db-stat-value">{formatNumber(stats?.completedTasks)}</p>
        </div>
        <div className="db-stat">
          <p className="db-stat-label">Overdue</p>
          <p className="db-stat-value">{formatNumber(stats?.overdueTasks)}</p>
        </div>
        <div className="db-stat">
          <p className="db-stat-label">Completion</p>
          <p className="db-stat-value">{Math.round(Number(stats?.completionRate) || 0)}%</p>
        </div>
      </div>
      <div className="db-chips">
        {chips.map(([status, count]) => (
          <span className="db-chip" key={status}>
            {titleCase(status)} {count}
          </span>
        ))}
        {chips.length === 0 && <span className="db-chip db-chip-muted">No tasks</span>}
      </div>
      <p className="db-card-note">
        Focus today {formatMinutes(stats?.focusTimeToday)} · this week {formatMinutes(stats?.focusTimeWeek)}
      </p>
    </>
  );
}

function GoalsWidget({ stats, ok }) {
  if (!ok) {
    return <p className="db-empty">Couldn&apos;t load goal data.</p>;
  }

  return (
    <>
      <div className="db-stat-row">
        <div className="db-stat">
          <p className="db-stat-label">Total</p>
          <p className="db-stat-value">{formatNumber(stats?.totalGoals)}</p>
        </div>
        <div className="db-stat">
          <p className="db-stat-label">Active</p>
          <p className="db-stat-value">{formatNumber(stats?.activeGoals)}</p>
        </div>
        <div className="db-stat">
          <p className="db-stat-label">Completed</p>
          <p className="db-stat-value">{formatNumber(stats?.completedGoals)}</p>
        </div>
      </div>
      <p className="db-card-note">
        Average completion {Math.round(Number(stats?.averageCompletionPercentage) || 0)}%
      </p>
      <Percent value={stats?.averageCompletionPercentage} />
    </>
  );
}

function QuickFacts({ memoryCount, memOk, aiCount, aiOk }) {
  return (
    <>
      <div className="db-stat">
        <p className="db-stat-label">Memories</p>
        <p className="db-stat-value">{memOk ? formatNumber(memoryCount) : '—'}</p>
      </div>
      <div className="db-stat">
        <p className="db-stat-label">AI interactions</p>
        <p className="db-stat-value">{aiOk ? formatNumber(aiCount) : '—'}</p>
      </div>
    </>
  );
}

function Activity({ transactions, ok }) {
  if (!ok) {
    return <p className="db-empty">Couldn&apos;t load recent activity.</p>;
  }

  if (transactions.length === 0) {
    return <p className="db-empty">No activity yet. Complete a task or goal to earn XP.</p>;
  }

  return (
    <ul className="db-activity">
      {transactions.map((tx) => {
        const gain = (tx.amount ?? 0) >= 0;
        return (
          <li className="db-activity-row" key={tx.id}>
            <div className="db-activity-main">
              <p className="db-activity-title">{titleCase(tx.transactionType)}</p>
              <p className="db-activity-meta">
                {tx.reason || tx.sourceType || 'XP transaction'} · {formatDateTime(tx.createdAt)}
              </p>
            </div>
            <span className={`db-activity-amount ${gain ? 'db-gain' : 'db-loss'}`}>
              {gain ? '+' : ''}
              {tx.amount}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const displayName = user?.displayName || user?.username || 'Operator';

  const [stats, setStats] = useState(null);
  const [statsOk, setStatsOk] = useState(false);
  const [streak, setStreak] = useState(null);
  const [streakOk, setStreakOk] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [txOk, setTxOk] = useState(false);
  const [taskStats, setTaskStats] = useState(null);
  const [taskOk, setTaskOk] = useState(false);
  const [goalStats, setGoalStats] = useState(null);
  const [goalOk, setGoalOk] = useState(false);
  const [memoryCount, setMemoryCount] = useState(0);
  const [memOk, setMemOk] = useState(false);
  const [aiCount, setAiCount] = useState(0);
  const [aiOk, setAiOk] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const [statsResult, streakResult, txResult, taskResult, goalResult, memResult, aiResult] =
        await Promise.all([
          getXpStatistics(),
          getXpStreak(),
          getXpTransactions(0, 8),
          getTaskStatistics(),
          getGoalStatistics(),
          getMemories(),
          getAiInteractions(),
        ]);
      if (cancelled) return;

      const sOk = !!statsResult.data?.success;
      const kOk = !!streakResult.data?.success;
      const tOk = !!txResult.data?.success;
      const taskOkFlag = !!taskResult.data?.success;
      const goalOkFlag = !!goalResult.data?.success;
      const mOk = !!memResult.data?.success;
      const aOk = !!aiResult.data?.success;

      setStats(sOk ? statsResult.data.data : null);
      setStatsOk(sOk);
      setStreak(kOk ? streakResult.data.data : null);
      setStreakOk(kOk);
      setTransactions(tOk ? txResult.data.data || [] : []);
      setTxOk(tOk);
      setTaskStats(taskOkFlag ? taskResult.data.data : null);
      setTaskOk(taskOkFlag);
      setGoalStats(goalOkFlag ? goalResult.data.data : null);
      setGoalOk(goalOkFlag);
      setMemoryCount(mOk ? (memResult.data.data || []).length : 0);
      setMemOk(mOk);
      setAiCount(aOk ? (aiResult.data.data || []).length : 0);
      setAiOk(aOk);

      if (!sOk && !taskOkFlag && !goalOkFlag) {
        setLoadError(errorMessage(statsResult, 'Failed to load dashboard data.'));
      } else {
        setLoadError(null);
      }
      setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const reload = () => {
    setLoading(true);
    setLoadError(null);
    setReloadKey((key) => key + 1);
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Welcome back, {displayName}</h1>
          <p className="page-subtitle">Your current state in THE SYSTEM.</p>
        </div>
        <button
          type="button"
          className="db-btn db-btn-primary"
          onClick={reload}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </header>

      {loading && <div className="db-state">Loading your dashboard...</div>}

      {!loading && loadError && (
        <div className="db-state db-state-error">
          <p>{loadError}</p>
          <button type="button" className="db-btn db-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && (
        <>
          <section className="db-section">
            <LevelWidget stats={stats} statsOk={statsOk} streak={streak} streakOk={streakOk} />
          </section>

          <section className="db-section">
            <div className="db-cards">
              <article className="db-card">
                <h2 className="db-section-title">Tasks</h2>
                <TasksWidget stats={taskStats} ok={taskOk} />
              </article>
              <article className="db-card">
                <h2 className="db-section-title">Goals</h2>
                <GoalsWidget stats={goalStats} ok={goalOk} />
              </article>
            </div>
          </section>

          <section className="db-section">
            <div className="db-quickfacts">
              <QuickFacts memoryCount={memoryCount} memOk={memOk} aiCount={aiCount} aiOk={aiOk} />
            </div>
          </section>

          <section className="db-section">
            <h2 className="db-section-title">Recent activity</h2>
            <Activity transactions={transactions} ok={txOk} />
          </section>
        </>
      )}
    </div>
  );
}
