import { useState, useEffect } from 'react';
import { db, ref, get, set, remove } from '../firebase';

const EMPTY_FORM = {
  title: '',
  date: '',
  location: '',
  time: '',
  strokes: '',
  description: '',
  registrationUrl: '',
  published: true,
  publishAt: '',
};

const FIELDS = [
  { name: 'title', label: 'Event Title', type: 'text', required: true, placeholder: 'Summer Regional Championships' },
  { name: 'date', label: 'Date', type: 'text', required: true, placeholder: 'July 15, 2026' },
  { name: 'location', label: 'Location', type: 'text', placeholder: 'City Aquatics Center' },
  { name: 'time', label: 'Time', type: 'text', placeholder: '8:00 AM - 5:00 PM' },
  { name: 'strokes', label: 'Strokes', type: 'text', placeholder: 'All Strokes' },
  { name: 'registrationUrl', label: 'Registration URL', type: 'url', placeholder: 'https://example.com/register' },
  { name: 'publishAt', label: 'Schedule Auto-Publish Date (optional)', type: 'date', placeholder: '' },
];

const isScheduledFuture = (ev) => {
  if (!ev.publishAt) return false;
  const t = new Date(ev.publishAt).getTime();
  return !isNaN(t) && t > Date.now();
};

export default function Events() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const loadEvents = async () => {
    setLoading(true);
    try {
      const snap = await get(ref(db, 'events'));
      const data = snap.val() || {};
      const list = Object.values(data).sort((a, b) => (Number(a.id) || 0) - (Number(b.id) || 0));
      setEvents(list);
    } catch (err) {
      console.error('Failed to load events:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const openAdd = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
    setMessage('');
  };

  const openEdit = (ev) => {
    setEditing(ev);
    setForm({ ...EMPTY_FORM, ...ev });
    setShowForm(true);
    setMessage('');
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
    setForm(EMPTY_FORM);
    setMessage('');
  };

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const nextId = () => {
    const ids = events.map((e) => Number(e.id) || 0);
    return ids.length ? Math.max(...ids) + 1 : 1;
  };

  const handleSave = async () => {
    setMessage('');
    if (!form.title || !form.date) {
      setMessage('Title and Date are required.');
      return;
    }
    setSaving(true);
    try {
      const id = editing ? Number(editing.id) : nextId();
      const payload = {
        id,
        title: form.title.trim(),
        date: form.date.trim(),
        location: form.location.trim(),
        time: form.time.trim(),
        strokes: form.strokes.trim(),
        description: form.description.trim(),
        registrationUrl: form.registrationUrl.trim(),
        published: form.published,
        publishAt: form.publishAt || '',
        updatedAt: Date.now(),
      };
      await set(ref(db, `events/${id}`), payload);
      const visibility = form.published
        ? 'Now live on the website & app.'
        : form.publishAt
        ? `Saved as scheduled — it will go live on ${form.publishAt}.`
        : 'Saved as draft (unpublished). Use "Publish" or check "Published" to make it live.';
      setMessage((editing ? 'Event updated. ' : 'Event created. ') + visibility);
      closeForm();
      await loadEvents();
    } catch (err) {
      setMessage('Error: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const togglePublished = async (ev) => {
    try {
      await set(ref(db, `events/${ev.id}/published`), !ev.published);
      await loadEvents();
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  const handleDelete = async (ev) => {
    if (!window.confirm(`Delete "${ev.title}"? This cannot be undone.`)) return;
    setMessage('');
    try {
      await remove(ref(db, `events/${ev.id}`));
      setMessage('Event deleted.');
      await loadEvents();
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>Events</h1>
      </div>
      <div className="page-body">
        {message && (
          <div
            className={message.startsWith('Error') ? 'alert alert-danger' : 'alert alert-success'}
          >
            {message}
          </div>
        )}

        <div className="two-col">
          <div>
            <div className="card">
              <div className="card-header">
                <span>All Events ({events.length})</span>
                <button className="btn btn-primary btn-sm" onClick={openAdd}>
                  + Add Event
                </button>
              </div>
              <div className="card-body">
                {loading ? (
                  <div>
                    {[...Array(4)].map((_, i) => (
                      <div key={i} className="skeleton skeleton-row" />
                    ))}
                  </div>
                ) : error ? (
                  <div className="alert alert-danger">{error}</div>
                ) : events.length === 0 ? (
                  <div className="empty-state" style={{ padding: 32 }}>
                    <h3>No events yet</h3>
                    <p>Click "Add Event" to create your first event.</p>
                  </div>
                ) : (
                  <div className="table-container">
                    <table>
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Title</th>
                          <th>Status</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {events.map((ev) => (
                          <tr key={ev.id}>
                            <td style={{ whiteSpace: 'nowrap' }}>{ev.date || '-'}</td>
                            <td style={{ fontWeight: 600 }}>{ev.title || 'Untitled'}</td>
                            <td>
                              <span
                                className={`badge ${
                                  isScheduledFuture(ev)
                                    ? 'badge-warning'
                                    : ev.published === false
                                    ? 'badge-danger'
                                    : 'badge-success'
                                }`}
                              >
                                {isScheduledFuture(ev)
                                  ? 'Scheduled'
                                  : ev.published === false
                                  ? 'Unpublished'
                                  : 'Published'}
                              </span>
                            </td>
                            <td style={{ whiteSpace: 'nowrap' }}>
                              <button
                                className="btn btn-outline btn-sm"
                                onClick={() => togglePublished(ev)}
                                style={{ marginRight: 6 }}
                              >
                                {ev.published === false ? 'Publish' : 'Unpublish'}
                              </button>
                              <button
                                className="btn btn-outline btn-sm"
                                onClick={() => openEdit(ev)}
                                style={{ marginRight: 6 }}
                              >
                                Edit
                              </button>
                              <button
                                className="btn btn-danger btn-sm"
                                onClick={() => handleDelete(ev)}
                              >
                                Delete
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </div>

          <div>
            {showForm && (
              <div className="card">
                <div className="card-header">
                  <span>{editing ? 'Edit Event' : 'New Event'}</span>
                  <button className="btn btn-outline btn-sm" onClick={closeForm}>
                    Cancel
                  </button>
                </div>
                <div className="card-body">
                  {FIELDS.map((f) => (
                    <div className="form-group" key={f.name}>
                      <label>
                        {f.label}
                        {f.required && <span style={{ color: 'red' }}> *</span>}
                      </label>
                      <input
                        className="form-control"
                        type={f.type}
                        name={f.name}
                        value={form[f.name]}
                        placeholder={f.placeholder}
                        onChange={handleChange}
                      />
                    </div>
                  ))}

                  <div className="form-group">
                    <label>Description</label>
                    <textarea
                      className="form-control"
                      rows={4}
                      name="description"
                      value={form.description}
                      placeholder="Describe the event..."
                      onChange={handleChange}
                    />
                  </div>

                  <div className="form-group">
                    <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <input
                        type="checkbox"
                        name="published"
                        checked={form.published}
                        onChange={(e) =>
                          setForm((prev) => ({ ...prev, published: e.target.checked }))
                        }
                      />
                      Published (visible on website & app)
                    </label>
                    <small style={{ color: 'var(--gray-600)', marginTop: 4 }}>
                      Uncheck to save as a draft. To publish later, either check this box
                      or set a "Schedule Auto-Publish Date". After saving, use the
                      <strong> Publish</strong> button in the list to make a draft live
                      instantly. Changes appear on the website within ~30s and in the app
                      within ~60s (or pull-to-refresh).
                    </small>
                  </div>

                  <button
                    className="btn btn-primary"
                    onClick={handleSave}
                    disabled={saving}
                  >
                    {saving ? 'Saving...' : editing ? 'Update Event' : 'Create Event'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
