import { useState, useEffect } from 'react';
import { db, ref, get } from '../firebase';

export default function Swimmers() {
  const [swimmers, setSwimmers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [selectedSwimmer, setSelectedSwimmer] = useState(null);
  const [users, setUsers] = useState({});

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [swimSnap, usersSnap] = await Promise.all([
        get(ref(db, 'swimmers')),
        get(ref(db, 'users')),
      ]);

      const usersData = usersSnap.val() || {};
      setUsers(usersData);

      const swimData = swimSnap.val() || {};
      const list = Object.entries(swimData).map(([id, s]) => ({
        id,
        ...(typeof s === 'object' ? s : {}),
      }));
      setSwimmers(list);
    } catch (err) {
      console.error('Failed to load swimmers:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const filtered = swimmers.filter((s) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      (s.name && s.name.toLowerCase().includes(q)) ||
      s.id.toLowerCase().includes(q)
    );
  });

  const getUserEmail = (userId) => {
    if (!userId) return '-';
    const u = users[userId];
    return u ? (u.email || userId.substring(0, 10) + '...') : userId.substring(0, 10) + '...';
  };

  return (
    <div>
      <div className="page-header">
        <h1>Swimmers</h1>
      </div>
      <div className="page-body">
        <div className="two-col">
          <div>
            <div className="card">
              <div className="card-header">
                <span>All Swimmer Profiles ({filtered.length})</span>
                <button className="btn btn-outline btn-sm" onClick={loadData}>
                  Refresh
                </button>
              </div>
              <div className="card-body">
                <div className="search-bar">
                  <input
                    type="text"
                    placeholder="Search swimmers..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </div>

                {loading ? (
                  <div>
                    {[...Array(5)].map((_, i) => (
                      <div key={i} className="skeleton skeleton-row" />
                    ))}
                  </div>
                ) : error ? (
                  <div className="alert alert-danger">{error}</div>
                ) : filtered.length === 0 ? (
                  <div className="empty-state" style={{ padding: 32 }}>
                    <h3>No swimmers found</h3>
                  </div>
                ) : (
                  <div className="table-container">
                    <table>
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Gender</th>
                          <th>Age Group</th>
                          <th>Logs</th>
                          <th>Owner</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {filtered.map((s) => {
                          const logCount = s.swimLogs ? Object.keys(s.swimLogs).length : 0;
                          return (
                            <tr key={s.id}>
                              <td style={{ fontWeight: 600 }}>
                                {s.name || s.swimmerName || 'Unnamed'}
                              </td>
                              <td>{s.gender || '-'}</td>
                              <td>{s.ageGroup || s.age || '-'}</td>
                              <td>{logCount}</td>
                              <td>{getUserEmail(s.userId || s.parentId)}</td>
                              <td>
                                <button
                                  className="btn btn-outline btn-sm"
                                  onClick={() => setSelectedSwimmer(s)}
                                >
                                  View
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </div>

          <div>
            {selectedSwimmer ? (
              <div className="card">
                <div className="card-header">
                  <span>Swimmer Details</span>
                  <button
                    className="btn btn-outline btn-sm"
                    onClick={() => setSelectedSwimmer(null)}
                  >
                    Close
                  </button>
                </div>
                <div className="card-body">
                  <div className="detail-field">
                    <div className="label">ID</div>
                    <div className="value"><code>{selectedSwimmer.id}</code></div>
                  </div>
                  <div className="detail-field">
                    <div className="label">Name</div>
                    <div className="value">{selectedSwimmer.name || selectedSwimmer.swimmerName || '-'}</div>
                  </div>
                  <div className="detail-field">
                    <div className="label">Gender</div>
                    <div className="value">{selectedSwimmer.gender || '-'}</div>
                  </div>
                  <div className="detail-field">
                    <div className="label">Age / Age Group</div>
                    <div className="value">{selectedSwimmer.age || selectedSwimmer.ageGroup || '-'}</div>
                  </div>
                  <div className="detail-field">
                    <div className="label">Parent / Owner</div>
                    <div className="value">{getUserEmail(selectedSwimmer.userId || selectedSwimmer.parentId)}</div>
                  </div>

                  {selectedSwimmer.swimLogs && (
                    <div style={{ marginTop: 16 }}>
                      <div className="label" style={{ marginBottom: 8 }}>Swim Logs ({Object.keys(selectedSwimmer.swimLogs).length})</div>
                      <div className="json-viewer" style={{ maxHeight: 300 }}>
                        {JSON.stringify(selectedSwimmer.swimLogs, null, 2)}
                      </div>
                    </div>
                  )}

                  {selectedSwimmer.bestTimes && (
                    <div style={{ marginTop: 16 }}>
                      <div className="label" style={{ marginBottom: 8 }}>Best Times</div>
                      <div className="json-viewer" style={{ maxHeight: 200 }}>
                        {JSON.stringify(selectedSwimmer.bestTimes, null, 2)}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="card">
                <div className="card-body">
                  <div className="empty-state">
                    <h3>Select a swimmer</h3>
                    <p>Click "View" on any swimmer to see details</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
