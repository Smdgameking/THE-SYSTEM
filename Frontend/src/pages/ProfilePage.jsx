import { useState } from 'react';
import { useAuth } from '../auth/useAuth';
import './profile.css';

const LIMITS = {
  displayName: 100,
  bio: 500,
  avatarUrl: 500,
  timezone: 50,
  locale: 10,
  country: 2,
};

const FIELD_LABELS = {
  displayName: 'Display name',
  bio: 'Bio',
  avatarUrl: 'Avatar URL',
  timezone: 'Timezone',
  locale: 'Locale',
  country: 'Country',
};

const TEXT_FIELDS = [
  { name: 'displayName', placeholder: 'Your display name' },
  { name: 'avatarUrl', placeholder: 'https://example.com/avatar.png' },
  { name: 'timezone', placeholder: 'UTC' },
  { name: 'locale', placeholder: 'en-US' },
  { name: 'country', placeholder: 'US' },
];

function toTrimmedOrNull(value) {
  const trimmed = typeof value === 'string' ? value.trim() : '';
  return trimmed || null;
}

function formatDateTime(iso) {
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
  return result?.data?.error?.message || result?.data?.message || fallback;
}

function ProfileForm({ initial, onSave, saving, error, success }) {
  const [values, setValues] = useState({
    displayName: initial.displayName || '',
    bio: initial.bio || '',
    avatarUrl: initial.avatarUrl || '',
    timezone: initial.timezone || '',
    locale: initial.locale || '',
    country: initial.country || '',
  });
  const [validationError, setValidationError] = useState(null);

  const setField = (field) => (event) => {
    setValues((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const invalid = TEXT_FIELDS.find(
      (field) => (values[field.name] || '').trim().length > LIMITS[field.name],
    );
    if (invalid) {
      setValidationError(
        `${FIELD_LABELS[invalid.name]} must not exceed ${LIMITS[invalid.name]} characters.`,
      );
      return;
    }
    if ((values.bio || '').trim().length > LIMITS.bio) {
      setValidationError(`Bio must not exceed ${LIMITS.bio} characters.`);
      return;
    }
    setValidationError(null);
    const country = values.country.trim();
    const payload = {
      displayName: toTrimmedOrNull(values.displayName),
      bio: toTrimmedOrNull(values.bio),
      avatarUrl: toTrimmedOrNull(values.avatarUrl),
      timezone: toTrimmedOrNull(values.timezone),
      locale: toTrimmedOrNull(values.locale),
      country: country ? country.toUpperCase() : null,
    };
    onSave(payload);
  };

  return (
    <form className="profile-form" onSubmit={handleSubmit}>
      <h2 className="profile-form-title">Edit profile</h2>

      <div className="profile-form-note">
        Username <strong>@{initial.username || 'operator'}</strong> is your login identifier and
        cannot be changed.
      </div>

      <div className="profile-form-grid">
        {TEXT_FIELDS.map((field) => (
          <div className="profile-form-field" key={field.name}>
            <label htmlFor={`profile-${field.name}`}>{FIELD_LABELS[field.name]}</label>
            <input
              id={`profile-${field.name}`}
              type="text"
              value={values[field.name]}
              onChange={setField(field.name)}
              maxLength={LIMITS[field.name]}
              placeholder={field.placeholder}
            />
          </div>
        ))}

        <div className="profile-form-field profile-form-bio">
          <label htmlFor="profile-bio">Bio</label>
          <textarea
            id="profile-bio"
            value={values.bio}
            onChange={setField('bio')}
            maxLength={LIMITS.bio}
            rows={3}
            placeholder="A short line about you"
          />
          <span className="profile-form-count">
            {values.bio.length}/{LIMITS.bio}
          </span>
        </div>
      </div>

      {(validationError || error) && (
        <div className="profile-banner-error">{validationError || error}</div>
      )}

      {success && (
        <div className="profile-banner-success">Profile updated successfully.</div>
      )}

      <div className="profile-form-actions">
        <button
          type="button"
          className="task-btn task-btn-ghost"
          onClick={() => setValues({
            displayName: initial.displayName || '',
            bio: initial.bio || '',
            avatarUrl: initial.avatarUrl || '',
            timezone: initial.timezone || '',
            locale: initial.locale || '',
            country: initial.country || '',
          })}
        >
          Reset
        </button>
        <button type="submit" className="task-btn task-btn-primary" disabled={saving}>
          {saving ? 'Saving...' : 'Save changes'}
        </button>
      </div>
    </form>
  );
}

export default function ProfilePage() {
  const { user, loading, updateProfile } = useAuth();
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);
  const [success, setSuccess] = useState(false);

  if (loading) {
    return <div className="profile-state">Loading profile...</div>;
  }

  const displayName = user?.displayName || user?.username || 'Operator';

  const handleSave = async (payload) => {
    setSaving(true);
    setFormError(null);
    setSuccess(false);
    const result = await updateProfile(payload);
    if (result.success) {
      setSuccess(true);
    } else {
      setFormError(errorMessage(result, 'Failed to update profile.'));
    }
    setSaving(false);
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Profile</h1>
          <p className="page-subtitle">Your operator identity and preferences.</p>
        </div>
      </header>

      <section className="profile-card">
        <div className="profile-avatar">{displayName.charAt(0)}</div>
        <div className="profile-identity">
          <div className="profile-name-row">
            <h2 className="profile-name">{displayName}</h2>
            {user?.accountStatus && (
              <span className="profile-badge">{user.accountStatus}</span>
            )}
          </div>
          {user?.username && <p className="profile-username">@{user.username}</p>}
          {user?.bio && <p className="profile-bio">{user.bio}</p>}
        </div>
      </section>

      <section className="profile-details">
        <div className="profile-detail">
          <p className="profile-detail-label">Timezone</p>
          <p className="profile-detail-value">{user?.timezone || '—'}</p>
        </div>
        <div className="profile-detail">
          <p className="profile-detail-label">Locale</p>
          <p className="profile-detail-value">{user?.locale || '—'}</p>
        </div>
        <div className="profile-detail">
          <p className="profile-detail-label">Country</p>
          <p className="profile-detail-value">{user?.country || '—'}</p>
        </div>
        <div className="profile-detail">
          <p className="profile-detail-label">Member since</p>
          <p className="profile-detail-value">{formatDateTime(user?.createdAt) || '—'}</p>
        </div>
        <div className="profile-detail">
          <p className="profile-detail-label">Last active</p>
          <p className="profile-detail-value">{formatDateTime(user?.lastActiveAt) || '—'}</p>
        </div>
        <div className="profile-detail">
          <p className="profile-detail-label">Account status</p>
          <p className="profile-detail-value">{user?.accountStatus || '—'}</p>
        </div>
      </section>

      <ProfileForm
        key={user?.updatedAt}
        initial={user || {}}
        onSave={handleSave}
        saving={saving}
        error={formError}
        success={success}
      />
    </div>
  );
}
