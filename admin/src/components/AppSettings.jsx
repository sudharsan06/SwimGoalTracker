import { useState, useEffect } from 'react';
import { db, ref, get, set, remove } from '../firebase';

const MAINTENANCE_PATH = 'admin/maintenance';
const ANNOUNCEMENT_PATH = 'admin/announcement';

export default function AppSettings() {
  const [maintenance, setMaintenance] = useState(null);
  const [announcement, setAnnouncement] = useState('');
  const [currentAnnouncement, setCurrentAnnouncement] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const loadSettings = async () => {
    setLoading(true);
    try {
      const [maintSnap, annSnap] = await Promise.all([
        get(ref(db, MAINTENANCE_PATH)),
        get(ref(db, ANNOUNCEMENT_PATH)),
      ]);
      if (maintSnap.exists()) setMaintenance(maintSnap.val());
      else setMaintenance({ enabled: false, message: '' });

      if (annSnap.exists()) {
        setCurrentAnnouncement(annSnap.val().text || annSnap.val());
        setAnnouncement(annSnap.val().text || annSnap.val());
      } else {
        setCurrentAnnouncement('');
        setAnnouncement('');
      }
    } catch (err) {
      console.error(err);
      setMessage('Error loading settings: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const toggleMaintenance = async () => {
    setMessage('');
    const newVal = !maintenance.enabled;
    try {
      await set(ref(db, MAINTENANCE_PATH), {
        enabled: newVal,
        message: maintenance.message || 'App is under maintenance. Please check back later.',
        updatedAt: Date.now(),
      });
      setMaintenance((prev) => ({ ...prev, enabled: newVal }));
      setMessage(`Maintenance mode ${newVal ? 'enabled' : 'disabled'}`);
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  const saveAnnouncement = async () => {
    setMessage('');
    try {
      await set(ref(db, ANNOUNCEMENT_PATH), {
        text: announcement,
        updatedAt: Date.now(),
      });
      setCurrentAnnouncement(announcement);
      setMessage('Announcement saved');
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  const clearAnnouncement = async () => {
    setMessage('');
    try {
      await remove(ref(db, ANNOUNCEMENT_PATH));
      setAnnouncement('');
      setCurrentAnnouncement('');
      setMessage('Announcement cleared');
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>Settings</h1>
      </div>
      <div className="page-body">
        {message && (
          <div
            className={message.startsWith('Error') ? 'alert alert-danger' : 'alert alert-success'}
          >
            {message}
          </div>
        )}

        <div className="grid-2">
          <div className="card">
            <div className="card-header">Maintenance Mode</div>
            <div className="card-body">
              <p style={{ fontSize: 14, color: 'var(--gray-600)', marginBottom: 16 }}>
                When enabled, users will see a maintenance screen when opening the app.
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span
                  className={`badge ${maintenance?.enabled ? 'badge-danger' : 'badge-success'}`}
                >
                  {maintenance?.enabled ? 'Active' : 'Inactive'}
                </span>
                <button
                  className={`btn btn-sm ${maintenance?.enabled ? 'btn-success' : 'btn-warning'}`}
                  onClick={toggleMaintenance}
                >
                  {maintenance?.enabled ? 'Disable' : 'Enable'} Maintenance
                </button>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">In-App Announcement</div>
            <div className="card-body">
              <p style={{ fontSize: 14, color: 'var(--gray-600)', marginBottom: 16 }}>
                Set a message that appears to all users in the app.
              </p>
              {currentAnnouncement && (
                <div className="alert alert-info" style={{ marginBottom: 12 }}>
                  <strong>Current:</strong> {currentAnnouncement}
                </div>
              )}
              <div className="form-group">
                <label>Announcement Text</label>
                <textarea
                  className="form-control"
                  rows={3}
                  value={announcement}
                  onChange={(e) => setAnnouncement(e.target.value)}
                  placeholder="Enter announcement text..."
                />
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-primary" onClick={saveAnnouncement}>
                  Save
                </button>
                <button className="btn btn-outline" onClick={clearAnnouncement}>
                  Clear
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-header">Firebase Project Info</div>
          <div className="card-body">
            <div className="detail-grid">
              <div className="detail-field">
                <div className="label">Project ID</div>
                <div className="value"><code>swimmingo</code></div>
              </div>
              <div className="detail-field">
                <div className="label">Database</div>
                <div className="value"><code>swimmingo-default-rtdb</code></div>
              </div>
              <div className="detail-field">
                <div className="label">Auth Providers</div>
                <div className="value">Email/Password, Google</div>
              </div>
              <div className="detail-field">
                <div className="label">Admin Access</div>
                <div className="value">
                  <span className="badge badge-success">Authenticated</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
