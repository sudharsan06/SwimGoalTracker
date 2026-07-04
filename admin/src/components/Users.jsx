import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { db, ref, get } from '../firebase';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const [deviceSnap, userDataSnap] = await Promise.all([
        get(ref(db, 'device_mapping')),
        get(ref(db, 'user_data')),
      ]);

      const deviceMapping = deviceSnap.val() || {};
      const userData = userDataSnap.val() || {};

      const allUids = new Set([
        ...Object.keys(deviceMapping),
        ...Object.keys(userData),
      ]);

      const list = [];
      for (const uid of allUids) {
        const data = userData[uid] || null;
        const deviceId = deviceMapping[uid] || null;
        const profiles = data?.profiles || {};
        const profileIds = Object.keys(profiles);
        let swimCount = 0;
        profileIds.forEach((pid) => {
          const sessions = profiles[pid]?.swim_sessions;
          if (sessions) swimCount += Object.keys(sessions).length;
        });

        const lastSync = data?.last_sync || null;

        list.push({
          uid,
          deviceId: typeof deviceId === 'string' ? deviceId : null,
          profileCount: profileIds.length,
          swimCount,
          lastSync,
          hasNutrition: data?.daily_nutrition ? Object.keys(data.daily_nutrition).length > 0 : false,
        });
      }

      setUsers(list);
    } catch (err) {
      console.error('Failed to load users:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const filtered = users.filter((u) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return u.uid.toLowerCase().includes(q) || u.deviceId?.toLowerCase().includes(q);
  });

  return (
    <div>
      <div className="page-header">
        <h1>Users</h1>
      </div>
      <div className="page-body">
        <div className="card">
          <div className="card-header">
            <span>Registered Users ({filtered.length})</span>
            <button className="btn btn-outline btn-sm" onClick={loadUsers}>
              Refresh
            </button>
          </div>
          <div className="card-body">
            <div className="search-bar">
              <input
                type="text"
                placeholder="Search by UID or device ID..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            {loading ? (
              <div>
                {[...Array(8)].map((_, i) => (
                  <div key={i} className="skeleton skeleton-row" />
                ))}
              </div>
            ) : error ? (
              <div className="alert alert-danger">{error}</div>
            ) : filtered.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon"></div>
                <h3>{search ? 'No users match your search' : 'No users found'}</h3>
                <p>{search ? 'Try a different search term' : 'No users have logged into the app yet'}</p>
              </div>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>User UID</th>
                      <th>Device ID</th>
                      <th>Profiles</th>
                      <th>Swim Logs</th>
                      <th>Nutrition</th>
                      <th>Last Sync</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((u) => (
                      <tr key={u.uid}>
                        <td><code>{u.uid.substring(0, 12)}...</code></td>
                        <td>
                          {u.deviceId ? (
                            <code>{u.deviceId.substring(0, 10)}...</code>
                          ) : (
                            <span className="badge badge-warning">No device</span>
                          )}
                        </td>
                        <td>{u.profileCount}</td>
                        <td>{u.swimCount}</td>
                        <td>
                          {u.hasNutrition ? (
                            <span className="badge badge-success">Yes</span>
                          ) : (
                            <span className="badge badge-warning">No</span>
                          )}
                        </td>
                        <td>
                          {u.lastSync
                            ? new Date(u.lastSync).toLocaleDateString()
                            : '-'}
                        </td>
                        <td>
                          <Link
                            to={`/users/${u.uid}`}
                            className="btn btn-outline btn-sm"
                          >
                            View
                          </Link>
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
    </div>
  );
}
