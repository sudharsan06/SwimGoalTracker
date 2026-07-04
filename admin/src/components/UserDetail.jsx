import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { db, ref, get } from '../firebase';

export default function UserDetail() {
  const { uid } = useParams();
  const [userData, setUserData] = useState(null);
  const [deviceId, setDeviceId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedProfile, setExpandedProfile] = useState(null);
  const [expandedSession, setExpandedSession] = useState(null);

  useEffect(() => {
    loadUserData();
  }, [uid]);

  const loadUserData = async () => {
    setLoading(true);
    try {
      const [userDataSnap, deviceSnap] = await Promise.all([
        get(ref(db, `user_data/${uid}`)),
        get(ref(db, `device_mapping/${uid}`)),
      ]);

      setUserData(userDataSnap.val());
      setDeviceId(deviceSnap.val());
    } catch (err) {
      console.error('Failed to load user:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div>
        <div className="page-header"><h1>User Detail</h1></div>
        <div className="page-body">
          <div className="skeleton" style={{ height: 200 }} />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <div className="page-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Link to="/users" className="btn btn-outline btn-sm">&larr; Back</Link>
            <h1>User Detail</h1>
            <code style={{ fontSize: 13 }}>{uid}</code>
          </div>
        </div>
        <div className="page-body">
          <div className="alert alert-danger">{error}</div>
        </div>
      </div>
    );
  }

  const profiles = userData?.profiles || {};
  const nutrition = userData?.daily_nutrition || {};
  const lastSync = userData?.last_sync || null;
  const syncVersion = userData?.sync_version || null;
  const profileIds = Object.keys(profiles);

  return (
    <div>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Link to="/users" className="btn btn-outline btn-sm">&larr; Back</Link>
          <h1>User Detail</h1>
          <code style={{ fontSize: 13 }}>{uid}</code>
        </div>
      </div>
      <div className="page-body">
        <div className="two-col">
          <div className="card">
            <div className="card-header">Account Info</div>
            <div className="card-body">
              <div className="detail-field">
                <div className="label">Firebase UID</div>
                <div className="value"><code>{uid}</code></div>
              </div>
              <div className="detail-field">
                <div className="label">Device ID</div>
                <div className="value">
                  {deviceId ? <code>{deviceId}</code> : <span className="badge badge-warning">Not mapped</span>}
                </div>
              </div>
              <div className="detail-field">
                <div className="label">Swimmer Profiles</div>
                <div className="value">{profileIds.length}</div>
              </div>
              <div className="detail-field">
                <div className="label">Nutrition Days</div>
                <div className="value">{Object.keys(nutrition).length}</div>
              </div>
              <div className="detail-field">
                <div className="label">Last Sync</div>
                <div className="value">
                  {lastSync ? new Date(lastSync).toLocaleString() : 'Never'}
                </div>
              </div>
              {syncVersion && (
                <div className="detail-field">
                  <div className="label">Sync Version</div>
                  <div className="value">{syncVersion}</div>
                </div>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              Nutrition Data ({Object.keys(nutrition).length} days)
            </div>
            <div className="card-body" style={{ maxHeight: 350, overflowY: 'auto' }}>
              {Object.keys(nutrition).length === 0 ? (
                <div className="empty-state" style={{ padding: 20 }}>
                  <p>No nutrition data synced</p>
                </div>
              ) : (
                <div className="table-container">
                  <table>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Calories</th>
                        <th>Protein</th>
                        <th>Carbs</th>
                        <th>Fats</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(nutrition)
                        .sort(([a], [b]) => b.localeCompare(a))
                        .map(([date, data]) => (
                          <tr key={date}>
                            <td>{date}</td>
                            <td>{data.calories ?? '-'}</td>
                            <td>{data.protein ?? '-'}g</td>
                            <td>{data.carbs ?? '-'}g</td>
                            <td>{data.fats ?? '-'}g</td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>

        {profileIds.length === 0 ? (
          <div className="card" style={{ marginTop: 16 }}>
            <div className="card-body">
              <div className="empty-state">
                <h3>No swimmer profiles</h3>
                <p>This user hasn't synced any swimmer data yet</p>
              </div>
            </div>
          </div>
        ) : (
          profileIds.map((pid) => {
            const profile = profiles[pid] || {};
            const info = profile.info || {};
            const sessions = profile.swim_sessions || {};
            const goals = profile.goals || {};
            const alerts = profile.alerts || {};
            const sessionIds = Object.keys(sessions);
            const isExpanded = expandedProfile === pid;

            return (
              <div className="card" key={pid} style={{ marginTop: 16 }}>
                <div
                  className="card-header"
                  style={{ cursor: 'pointer' }}
                  onClick={() => setExpandedProfile(isExpanded ? null : pid)}
                >
                  <span>
                    Swimmer Profile #{pid}
                    {info.name ? ` - ${info.name}` : ''}
                    <span className="badge badge-info" style={{ marginLeft: 8 }}>
                      {sessionIds.length} sessions
                    </span>
                  </span>
                  <span>{isExpanded ? '▼' : '▶'}</span>
                </div>

                {isExpanded && (
                  <div className="card-body">
                    <div className="two-col">
                      <div>
                        <h4 style={{ fontSize: 14, marginBottom: 12, color: 'var(--gray-700)' }}>
                          Profile Info
                        </h4>
                        <div className="detail-grid">
                          {Object.entries(info).map(([key, val]) => (
                            <div className="detail-field" key={key}>
                              <div className="label">{key}</div>
                              <div className="value">{String(val ?? '')}</div>
                            </div>
                          ))}
                        </div>

                        {Object.keys(goals).length > 0 && (
                          <div style={{ marginTop: 16 }}>
                            <h4 style={{ fontSize: 14, marginBottom: 12, color: 'var(--gray-700)' }}>
                              Goals
                            </h4>
                            <div className="detail-grid">
                              {Object.entries(goals).map(([key, val]) => (
                                <div className="detail-field" key={key}>
                                  <div className="label">{key}</div>
                                  <div className="value">{String(val ?? '')}</div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      <div>
                        <h4 style={{ fontSize: 14, marginBottom: 12, color: 'var(--gray-700)' }}>
                          Swim Sessions ({sessionIds.length})
                        </h4>
                        {sessionIds.length === 0 ? (
                          <p style={{ color: 'var(--gray-500)', fontSize: 13 }}>No sessions recorded</p>
                        ) : (
                          <div style={{ maxHeight: 400, overflowY: 'auto' }}>
                            <table>
                              <thead>
                                <tr>
                                  <th>Date</th>
                                  <th>Distance</th>
                                  <th>Strokes</th>
                                  <th></th>
                                </tr>
                              </thead>
                              <tbody>
                                {sessionIds.map((sid) => {
                                  const session = sessions[sid];
                                  const date = session?.date || sid.substring(0, 10);
                                  const distance = session?.total_distance || 0;
                                  const hasFree = session?.freestyle_ms > 0;
                                  const hasBack = session?.backstroke_ms > 0;
                                  const hasBreast = session?.breaststroke_ms > 0;
                                  const hasFly = session?.butterfly_ms > 0;
                                  const hasIm = session?.im_ms > 0;

                                  const strokes = [];
                                  if (hasFree) strokes.push('Free');
                                  if (hasBack) strokes.push('Back');
                                  if (hasBreast) strokes.push('Breast');
                                  if (hasFly) strokes.push('Fly');
                                  if (hasIm) strokes.push('IM');

                                  return (
                                    <tr key={sid}>
                                      <td>{date}</td>
                                      <td>{distance}m</td>
                                      <td>{strokes.join(', ') || '-'}</td>
                                      <td>
                                        <button
                                          className="btn btn-outline btn-sm"
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setExpandedSession(
                                              expandedSession === sid ? null : sid
                                            );
                                          }}
                                        >
                                          {expandedSession === sid ? 'Hide' : 'Details'}
                                        </button>
                                      </td>
                                    </tr>
                                  );
                                })}
                              </tbody>
                            </table>

                            {expandedSession && sessions[expandedSession] && (
                              <div className="json-viewer" style={{ marginTop: 12 }}>
                                {JSON.stringify(sessions[expandedSession], null, 2)}
                              </div>
                            )}
                          </div>
                        )}

                        {Object.keys(alerts).length > 0 && (
                          <div style={{ marginTop: 16 }}>
                            <h4 style={{ fontSize: 14, marginBottom: 12, color: 'var(--gray-700)' }}>
                              Alert Settings
                            </h4>
                            <div className="detail-grid">
                              {Object.entries(alerts).map(([key, val]) => (
                                <div className="detail-field" key={key}>
                                  <div className="label">{key}</div>
                                  <div className="value">{String(val)}</div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}

        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-header">Raw User Data</div>
          <div className="card-body">
            <div className="json-viewer">
              {JSON.stringify({ deviceId, ...userData }, null, 2)}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
