import { useEffect, useState } from 'react';
import {
  getDefinitionsByEngine,
  getNamespaceSettings,
  setSetting,
  resetNamespace,
} from '../api/settingsApi';
import './settings.css';

const NAMESPACE_LABELS = {
  appearance: 'Appearance',
  notification: 'Notifications',
  xp: 'XP',
};

function titleCase(value) {
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

function errorMessage(result, fallback) {
  return result?.data?.error?.message || result?.data?.message || fallback;
}

function effectiveValue(def, override) {
  const raw = override?.value ?? def.defaultValue;
  if (def.type === 'BOOLEAN') {
    if (raw == null || raw === '') return Boolean(def.defaultValue);
    return raw === true || raw === 'true';
  }
  if (def.type === 'INTEGER' || def.type === 'DOUBLE') {
    return raw == null || raw === '' ? raw : Number(raw);
  }
  return raw == null ? '' : String(raw);
}

function namespaceTitle(namespace) {
  return NAMESPACE_LABELS[namespace] || titleCase(namespace);
}

function SettingRow({ definition, override, onSave, saving }) {
  const [draft, setDraft] = useState(() => effectiveValue(definition, override));
  const [error, setError] = useState(null);

  const handleToggle = () => {
    const next = !draft;
    setDraft(next);
    onSave(next, setError);
  };

  const handleBlur = () => {
    const next = typeof draft === 'string' ? draft.trim() : draft;
    if (next === effectiveValue(definition, override)) return;
    onSave(next, setError);
  };

  return (
    <div className="settings-row">
      <div className="settings-row-main">
        <p className="settings-row-label">{titleCase(definition.key)}</p>
        {definition.description && (
          <p className="settings-row-description">{definition.description}</p>
        )}
        {error && <p className="settings-row-error">{error}</p>}
      </div>
      <div className="settings-row-control">
        {definition.type === 'BOOLEAN' ? (
          <button
            type="button"
            role="switch"
            aria-checked={draft}
            aria-label={definition.key}
            className={`settings-toggle${draft ? ' is-on' : ''}`}
            disabled={saving}
            onClick={handleToggle}
          >
            <span className="settings-toggle-thumb" />
          </button>
        ) : (
          <input
            type={definition.type === 'INTEGER' || definition.type === 'DOUBLE' ? 'number' : 'text'}
            value={draft ?? ''}
            disabled={saving}
            onChange={(event) => setDraft(event.target.value)}
            onBlur={handleBlur}
          />
        )}
        {saving && <span className="settings-saving">Saving...</span>}
      </div>
    </div>
  );
}

function SettingsSection({ namespace, definitions, overrides, onSave, onReset, savingKey, busy }) {
  const [resetError, setResetError] = useState(null);

  const handleReset = async () => {
    setResetError(null);
    const result = await onReset(namespace);
    if (!result) {
      setResetError('Failed to reset settings.');
    }
  };

  return (
    <section className="settings-section">
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">{namespaceTitle(namespace)}</h2>
          <p className="settings-section-namespace">{namespace}</p>
        </div>
        <button
          type="button"
          className="settings-btn settings-btn-ghost"
          disabled={busy}
          onClick={handleReset}
        >
          Reset to defaults
        </button>
      </div>
      {resetError && <div className="settings-banner-error">{resetError}</div>}
      <div className="settings-rows">
        {definitions.map((definition) => (
          <SettingRow
            key={`${definition.namespace}:${definition.key}:${overrides?.[definition.key]?.updatedAt ?? ''}`}
            definition={definition}
            override={overrides?.[definition.key]}
            saving={savingKey === `${definition.namespace}:${definition.key}`}
            onSave={(value, setError) => onSave(definition, value, setError)}
          />
        ))}
      </div>
    </section>
  );
}

export default function SettingsPage() {
  const [sections, setSections] = useState([]);
  const [overrides, setOverrides] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [savingKey, setSavingKey] = useState(null);
  const [busy, setBusy] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const defResult = await getDefinitionsByEngine('settings');
      if (cancelled) return;

      if (!defResult.data?.success) {
        setLoadError(errorMessage(defResult, 'Failed to load settings.'));
        setSections([]);
        setLoading(false);
        return;
      }

      const definitions = (defResult.data.data || []).filter(
        (definition) => definition.visibility === 'PUBLIC',
      );
      const grouped = definitions.reduce((acc, definition) => {
        const list = acc[definition.namespace] || [];
        list.push(definition);
        acc[definition.namespace] = list;
        return acc;
      }, {});

      setSections(
        Object.entries(grouped).map(([namespace, items]) => ({ namespace, definitions: items })),
      );

      const nextOverrides = {};
      await Promise.all(
        Object.keys(grouped).map(async (namespace) => {
          const result = await getNamespaceSettings(namespace);
          if (cancelled) return;
          if (result.data?.success) {
            nextOverrides[namespace] = result.data.data?.settings || {};
          } else {
            nextOverrides[namespace] = {};
          }
        }),
      );
      if (cancelled) return;
      setOverrides(nextOverrides);
      setLoadError(null);
      setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const reload = () => {
    setLoading(true);
    setLoadError(null);
    setActionError(null);
    setReloadKey((key) => key + 1);
  };

  const handleSave = async (definition, value, setRowError) => {
    setActionError(null);
    setSavingKey(`${definition.namespace}:${definition.key}`);
    const result = await setSetting(definition.namespace, definition.key, value);
    setSavingKey(null);
    if (result.data?.success) {
      setOverrides((prev) => ({
        ...prev,
        [definition.namespace]: {
          ...prev[definition.namespace],
          [definition.key]: result.data.data,
        },
      }));
    } else {
      setRowError(errorMessage(result, 'Failed to save setting.'));
    }
  };

  const handleReset = async (namespace) => {
    setBusy(true);
    setActionError(null);
    const result = await resetNamespace(namespace);
    setBusy(false);
    if (result.data?.success) {
      reload();
      return true;
    }
    setActionError(errorMessage(result, 'Failed to reset settings.'));
    return false;
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Settings</h1>
          <p className="page-subtitle">Your system preferences and notification behavior.</p>
        </div>
      </header>

      {actionError && <div className="settings-banner-error">{actionError}</div>}

      {loading && <div className="settings-state">Loading settings...</div>}

      {!loading && loadError && (
        <div className="settings-state settings-state-error">
          <p>{loadError}</p>
          <button type="button" className="settings-btn settings-btn-ghost" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      {!loading && !loadError && sections.length === 0 && (
        <div className="settings-state">
          <p>No settings are available yet.</p>
        </div>
      )}

      {!loading && !loadError && sections.length > 0 && (
        <div className="settings-sections">
          {sections.map((section) => (
            <SettingsSection
              key={section.namespace}
              namespace={section.namespace}
              definitions={section.definitions}
              overrides={overrides[section.namespace]}
              onSave={handleSave}
              onReset={handleReset}
              savingKey={savingKey}
              busy={busy}
            />
          ))}
        </div>
      )}
    </div>
  );
}
