import { useState } from 'react';
import { db, ref, get, remove } from '../firebase';

const KNOWN_PATHS = [
  { label: 'Root', path: '' },
  { label: 'User Data (all)', path: 'user_data' },
  { label: 'Device Mapping', path: 'device_mapping' },
];

export default function DatabaseBrowser() {
  const [customPath, setCustomPath] = useState('');
  const [activePath, setActivePath] = useState('user_data');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState({});

  const loadPath = async (path) => {
    setLoading(true);
    setError('');
    setData(null);
    try {
      const refPath = path ? ref(db, path) : ref(db);
      const snap = await get(refPath);
      if (snap.exists()) {
        setData(snap.val());
      } else {
        setData({ __empty: true });
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleNavigate = (path) => {
    setActivePath(path);
    setCustomPath('');
    loadPath(path);
  };

  const handleCustomNav = (e) => {
    e.preventDefault();
    const p = customPath.trim().replace(/^\/+|\/+$/g, '');
    setActivePath(p);
    loadPath(p);
  };

  const handleDelete = async (path) => {
    if (!window.confirm(`Delete entire node "${path}"? This cannot be undone.`)) return;
    try {
      const refPath = path ? ref(db, path) : ref(db);
      await remove(refPath);
      loadPath(activePath);
    } catch (err) {
      setError(err.message);
    }
  };

  const toggleExpand = (key) => {
    setExpanded((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const renderValue = (value, path = '', depth = 0) => {
    if (value === null || value === undefined) {
      return <span className="tree-null">null</span>;
    }
    if (typeof value === 'boolean') {
      return <span className="tree-bool">{value.toString()}</span>;
    }
    if (typeof value === 'number') {
      return <span className="tree-number">{value}</span>;
    }
    if (typeof value === 'string') {
      if (value.length > 100) {
        return <span className="tree-string">&quot;{value.substring(0, 100)}...&quot;</span>;
      }
      return <span className="tree-string">&quot;{value}&quot;</span>;
    }
    if (Array.isArray(value)) {
      if (value.length === 0) return <span className="tree-null">[]</span>;
      const key = path || `array-${depth}`;
      const isExpanded = expanded[key] !== false;
      return (
        <div className="tree-node">
          <span className="tree-toggle" onClick={() => toggleExpand(key)}>
            {isExpanded ? '▼' : '▶'} [{value.length}]
          </span>
          {isExpanded && (
            <div className="tree-children">
              {value.map((item, i) => (
                <div key={i} className="tree-node">
                  <span className="tree-key">{i}: </span>
                  {renderValue(item, `${path}[${i}]`, depth + 1)}
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }
    if (typeof value === 'object') {
      const keys = Object.keys(value);
      if (keys.length === 0) return <span className="tree-null">{'{}'}</span>;
      const key = path || `obj-${depth}`;
      const isExpanded = expanded[key] !== false;
      return (
        <div className="tree-node">
          <span className="tree-toggle" onClick={() => toggleExpand(key)}>
            {isExpanded ? '▼' : '▶'} {'{'} {keys.length} keys {'}'}
          </span>
          {isExpanded && (
            <div className="tree-children">
              {keys.map((k) => (
                <div key={k} className="tree-node">
                  <span className="tree-key">{k}: </span>
                  {renderValue(value[k], `${path}/${k}`, depth + 1)}
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }
    return <span>{String(value)}</span>;
  };

  return (
    <div>
      <div className="page-header">
        <h1>Data Browser</h1>
      </div>
      <div className="page-body">
        <div className="two-col">
          <div>
            <div className="card">
              <div className="card-header">Database Paths</div>
              <div className="card-body" style={{ padding: 8 }}>
                {KNOWN_PATHS.map((item) => (
                  <div
                    key={item.path}
                    onClick={() => handleNavigate(item.path)}
                    style={{
                      padding: '10px 16px',
                      cursor: 'pointer',
                      borderRadius: 6,
                      marginBottom: 2,
                      fontWeight: activePath === item.path ? 600 : 400,
                      background: activePath === item.path ? '#e8f0fe' : 'transparent',
                      color: activePath === item.path ? 'var(--primary)' : 'var(--gray-800)',
                      fontSize: 14,
                    }}
                  >
                    {item.label}
                    <code style={{ marginLeft: 8, fontSize: 11, color: 'var(--gray-500)' }}>
                      /{item.path}
                    </code>
                  </div>
                ))}

                <div style={{
                  marginTop: 16,
                  padding: '10px 16px',
                  fontSize: 12,
                  color: 'var(--gray-500)',
                  borderTop: '1px solid var(--gray-200)',
                }}>
                  <strong>Tip:</strong> Use custom path to drill into specific user:
                  <code style={{ display: 'block', marginTop: 4 }}>
                    user_data/{'{uid}'}/profiles/{'{pid}'}/swim_sessions
                  </code>
                </div>
              </div>
            </div>

            <div className="card" style={{ marginTop: 16 }}>
              <div className="card-header">Custom Path</div>
              <div className="card-body">
                <form onSubmit={handleCustomNav} style={{ display: 'flex', gap: 8 }}>
                  <input
                    className="form-control"
                    placeholder="e.g., user_data/someUid"
                    value={customPath}
                    onChange={(e) => setCustomPath(e.target.value)}
                  />
                  <button className="btn btn-primary" type="submit">
                    Go
                  </button>
                </form>
              </div>
            </div>
          </div>

          <div>
            <div className="card">
              <div className="card-header">
                <span>/{activePath || '(root)'}</span>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button
                    className="btn btn-outline btn-sm"
                    onClick={() => loadPath(activePath)}
                    disabled={loading}
                  >
                    {loading ? 'Loading...' : 'Refresh'}
                  </button>
                  <button
                    className="btn btn-danger btn-sm"
                    onClick={() => handleDelete(activePath)}
                  >
                    Delete
                  </button>
                </div>
              </div>
              <div className="card-body" style={{ padding: 12 }}>
                {loading ? (
                  <div className="skeleton" style={{ height: 300 }} />
                ) : error ? (
                  <div className="alert alert-danger">{error}</div>
                ) : data && data.__empty ? (
                  <div className="empty-state" style={{ padding: 32 }}>
                    <h3>Path is empty</h3>
                    <p>No data found at /{activePath}</p>
                  </div>
                ) : data ? (
                  <div className="tree-view">{renderValue(data, activePath)}</div>
                ) : (
                  <div className="empty-state" style={{ padding: 32 }}>
                    <p>Select a path to browse</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
