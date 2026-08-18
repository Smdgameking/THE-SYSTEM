import { useEffect, useState } from 'react';
import {
  getXpAccount,
  getXpStatistics,
  getXpStreak,
  getXpTransactions,
  getAchievements,
  getMyAchievements,
  checkAchievements,
  getLeaderboard,
} from '../api/xpApi';
import './progress.css';

function titleCase(value) {
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
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

function errorMessage(result, fallback) {
  return result?.data?.error?.message || result?.data?.message || fallback;
}

function formatXp(value) {
  if (value == null) return '—';
  return Number(value).toLocaleString();
}

function ProgressStats({ stats }) {
  const items = [
    { label: 'Daily XP', value: formatXp(stats?.dailyXp) },
    { label: 'Weekly XP', value: formatXp(stats?.weeklyXp) },
    { label: 'Monthly XP', value: formatXp(stats?.monthlyXp) },
    { label: 'Lifetime XP', value: formatXp(stats?.lifetimeXp) },
    { label: 'Tasks', value: stats?.tasksCompleted ?? '—' },
    { label: 'Goals', value: stats?.goalsCompleted ?? '—' },
    { label: 'Achievements', value: stats?.achievementsUnlocked ?? '—' },
  ];

  return (
    <div className="xp-stats">
      {items.map((item) => (
        <section className="xp-stats-item" key={item.label}>
          <p className="xp-stats-label">{item.label}</p>
          <p className="xp-stats-value">{item.value}</p>
        </section>
      ))}
    </div>
  );
}

function LevelCard({ account, streak }) {
  const level = account?.currentLevel ?? 1;
  const progress = Math.min(100, Math.max(0, account?.levelProgress ?? 0));

  return (
    <div className="xp-level-card">
      <div className="xp-level-main">
        <p className="xp-level-label">Level</p>
        <p className="xp-level-number">{level}</p>
        <p className="xp-level-progress-text">{Math.round(progress * 10) / 10}% to next level</p>
        <div className="xp-progress-track">
          <div className="xp-progress-fill" style={{ width: `${progress}%` }} />
        </div>
        <div className="xp-level-meta">
          <span className="xp-meta-item">Current {formatXp(account?.currentXp)} XP</span>
          <span className="xp-meta-item">Earned {formatXp(account?.totalXpEarned)} XP</span>
        </div>
      </div>
      <div className="xp-level-side">
        <p className="xp-level-label">Streak</p>
        <p className="xp-level-number xp-streak-number">
          {streak?.currentStreak != null ? streak.currentStreak : '—'}
          <span className="xp-streak-unit"> days</span>
        </p>
        <div className="xp-streak-meta">
          <span className="xp-meta-item">Longest {streak?.longestStreak != null ? streak.longestStreak : '—'}</span>
          {streak?.lastActivityDate && (
            <span className="xp-meta-item">Last active {formatDate(streak.lastActivityDate)}</span>
          )}
        </div>
      </div>
    </div>
  );
}

function Transactions({ transactions }) {
  if (transactions.length === 0) {
    return <p className="xp-empty">No XP transactions yet. Complete a task or goal to earn XP.</p>;
  }

  return (
    <ul className="xp-transactions">
      {transactions.map((tx) => {
        const gain = (tx.amount ?? 0) >= 0;
        return (
          <li className="xp-transaction" key={tx.id}>
            <div className="xp-transaction-main">
              <p className="xp-transaction-title">{titleCase(tx.transactionType)}</p>
              <p className="xp-transaction-meta">
                {tx.reason || tx.sourceType || 'XP transaction'} · {formatDateTime(tx.createdAt)}
              </p>
            </div>
            <span className={`xp-transaction-amount ${gain ? 'xp-gain' : 'xp-loss'}`}>
              {gain ? '+' : ''}
              {tx.amount}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

function Achievements({ definitions, progressByAchievement }) {
  const visible = (definitions || []).filter(
    (definition) => !definition.isHidden || progressByAchievement[definition.id]?.isUnlocked,
  );

  if (visible.length === 0) {
    return <p className="xp-empty">No achievements available yet.</p>;
  }

  return (
    <div className="xp-achievements">
      {visible.map((definition) => {
        const mine = progressByAchievement[definition.id];
        const unlocked = mine?.isUnlocked;
        const target = mine?.targetProgress;
        const current = mine?.currentProgress ?? 0;
        const pct = target ? Math.min(100, Math.round((current / target) * 100)) : 0;
        return (
          <article
            key={definition.id}
            className={`xp-achievement${unlocked ? ' xp-achievement-unlocked' : ''}`}
          >
            <div className="xp-achievement-head">
              <h4 className="xp-achievement-name">{definition.name}</h4>
              {unlocked ? (
                <span className="xp-badge xp-badge-unlocked">Unlocked</span>
              ) : (
                <span className="xp-badge">{titleCase(definition.category)}</span>
              )}
            </div>
            {definition.description && (
              <p className="xp-achievement-description">{definition.description}</p>
            )}
            {target != null && (
              <div className="xp-achievement-progress">
                <div className="xp-progress-track">
                  <div className="xp-progress-fill" style={{ width: `${pct}%` }} />
                </div>
                <p className="xp-achievement-meta">
                  {current} / {target}
                  {definition.xpReward > 0 ? ` · +${definition.xpReward} XP` : ''}
                </p>
              </div>
            )}
          </article>
        );
      })}
    </div>
  );
}

function Leaderboard({ entries, selfUserId }) {
  if (!entries || entries.length === 0) {
    return <p className="xp-empty">The leaderboard is empty.</p>;
  }

  return (
    <ol className="xp-leaderboard">
      {entries.map((entry) => {
        const self = entry.userId === selfUserId;
        return (
          <li
            key={entry.userId}
            className={`xp-leaderboard-row${self ? ' xp-leaderboard-self' : ''}`}
          >
            <span className="xp-leaderboard-rank">{entry.rank ?? '—'}</span>
            <span className="xp-leaderboard-name">
              {entry.username || 'Unknown'}
              {self && <span className="xp-badge xp-badge-you">You</span>}
            </span>
            <span className="xp-leaderboard-level">Lv {entry.currentLevel ?? 1}</span>
            <span className="xp-leaderboard-xp">{formatXp(entry.currentXp)} XP</span>
          </li>
        );
      })}
    </ol>
  );
}

export default function ProgressPage() {
  const [account, setAccount] = useState(null);
  const [stats, setStats] = useState(null);
  const [streak, setStreak] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [allAchievements, setAllAchievements] = useState([]);
  const [myAchievements, setMyAchievements] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const [accountResult, statsResult, streakResult, txResult, allResult, mineResult, boardResult] =
        await Promise.all([
          getXpAccount(),
          getXpStatistics(),
          getXpStreak(),
          getXpTransactions(0, 15),
          getAchievements(),
          getMyAchievements(),
          getLeaderboard(0, 10),
        ]);
      if (cancelled) return;

      if (accountResult.data?.success) setAccount(accountResult.data.data);
      else setAccount(null);

      if (statsResult.data?.success) setStats(statsResult.data.data);
      else setStats(null);

      if (streakResult.data?.success) setStreak(streakResult.data.data);
      else setStreak(null);

      if (txResult.data?.success) setTransactions(txResult.data.data || []);
      else setTransactions([]);

      if (allResult.data?.success) setAllAchievements(allResult.data.data || []);
      else setAllAchievements([]);

      if (mineResult.data?.success) setMyAchievements(mineResult.data.data || []);
      else setMyAchievements([]);

      if (boardResult.data?.success) setLeaderboard(boardResult.data.data?.entries || []);
      else setLeaderboard([]);

      if (!statsResult.data?.success && !accountResult.data?.success && !boardResult.data?.success) {
        setLoadError(errorMessage(statsResult, 'Failed to load progress data.'));
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

  const runAchievementCheck = async () => {
    setRefreshing(true);
    setActionError(null);
    const result = await checkAchievements();
    if (result.data?.success) {
      reload();
    } else {
      setActionError(errorMessage(result, 'Failed to check achievements.'));
    }
    setRefreshing(false);
  };

  const progressByAchievement = {};
  (myAchievements || []).forEach((mine) => {
    progressByAchievement[mine.achievementId] = mine;
  });

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Progress</h1>
          <p className="page-subtitle">Your XP, achievements, and standing in the system.</p>
        </div>
        <button
          type="button"
          className="xp-btn xp-btn-primary"
          onClick={runAchievementCheck}
          disabled={refreshing}
        >
          {refreshing ? 'Checking...' : 'Refresh achievements'}
        </button>
      </header>

      <ProgressStats stats={stats} />

      <LevelCard account={account} streak={streak} />

      {actionError && <div className="xp-banner-error">{actionError}</div>}

      {loading && <div className="xp-state">Loading progress...</div>}

      {!loading && loadError && (
        <div className="xp-state xp-state-error">
          <p>{loadError}</p>
          <button type="button" className="xp-btn xp-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && (
        <>
          <section className="xp-section">
            <h2 className="xp-section-title">Recent transactions</h2>
            <Transactions transactions={transactions} />
          </section>

          <section className="xp-section">
            <h2 className="xp-section-title">Achievements</h2>
            <Achievements
              definitions={allAchievements}
              progressByAchievement={progressByAchievement}
            />
          </section>

          <section className="xp-section">
            <h2 className="xp-section-title">Leaderboard</h2>
            <Leaderboard entries={leaderboard} selfUserId={account?.userId} />
          </section>
        </>
      )}
    </div>
  );
}
