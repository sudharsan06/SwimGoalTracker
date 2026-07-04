import { useState, useEffect } from 'react';
import { db, ref, get } from '../firebase';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [userDataSnap, deviceSnap] = await Promise.all([
        get(ref(db, 'user_data')),
        get(ref(db, 'device_mapping')),
      ]);

      const userData = userDataSnap.val() || {};
      const deviceMapping = deviceSnap.val() || {};

      const allUids = new Set([
        ...Object.keys(userData),
        ...Object.keys(deviceMapping),
      ]);

      let totalProfiles = 0;
      let totalSessions = 0;
      let userWithNutrition = 0;
      const userGrowthMap = {};

      for (const uid of Object.keys(userData)) {
        const data = userData[uid];
        const profiles = data?.profiles || {};

        Object.values(profiles).forEach((profile) => {
          totalProfiles++;
          if (profile.swim_sessions) {
            totalSessions += Object.keys(profile.swim_sessions).length;
          }
        });

        if (data?.daily_nutrition) {
          const dates = Object.keys(data.daily_nutrition);
          if (dates.length > 0) userWithNutrition++;
        }
      }

      Object.values(deviceMapping).forEach(() => {});

      setStats({
        totalUsers: allUids.size,
        syncedUsers: Object.keys(userData).length,
        deviceMapped: Object.keys(deviceMapping).length,
        totalProfiles,
        totalSessions,
        userWithNutrition,
      });
    } catch (err) {
      console.error('Dashboard load error:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div>
        <div className="page-header"><h1>Dashboard</h1></div>
        <div className="page-body">
          <div className="stats-grid">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="stat-card">
                <div style={{ flex: 1 }}>
                  <div className="skeleton" style={{ height: 32, width: 80, marginBottom: 8 }} />
                  <div className="skeleton" style={{ height: 16, width: 120 }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <div className="page-header"><h1>Dashboard</h1></div>
        <div className="page-body">
          <div className="alert alert-danger">{error}</div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard</h1>
      </div>
      <div className="page-body">
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon blue"></div>
            <div>
              <div className="stat-value">{stats?.totalUsers || 0}</div>
              <div className="stat-label">Total Users (device + sync)</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon green"></div>
            <div>
              <div className="stat-value">{stats?.syncedUsers || 0}</div>
              <div className="stat-label">Users with Synced Data</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon purple"></div>
            <div>
              <div className="stat-value">{stats?.deviceMapped || 0}</div>
              <div className="stat-label">Device Mappings</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon orange"></div>
            <div>
              <div className="stat-value">{stats?.totalProfiles || 0}</div>
              <div className="stat-label">Swimmer Profiles</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon teal"></div>
            <div>
              <div className="stat-value">{stats?.totalSessions || 0}</div>
              <div className="stat-label">Swim Sessions Logged</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon red"></div>
            <div>
              <div className="stat-value">{stats?.userWithNutrition || 0}</div>
              <div className="stat-label">Users with Nutrition Data</div>
            </div>
          </div>
        </div>

        <div className="grid-2">
          <div className="card">
            <div className="card-header">Database Paths</div>
            <div className="card-body">
              <div style={{ fontFamily: 'monospace', fontSize: 13, lineHeight: 2 }}>
                <div><code>user_data/{'{uid}'}/</code> - Synced user data</div>
                <div style={{ paddingLeft: 20 }}><code>profiles/{'{pid}'}/info/</code> - Swimmer profile</div>
                <div style={{ paddingLeft: 20 }}><code>profiles/{'{pid}'}/swim_sessions/</code> - Swim logs</div>
                <div style={{ paddingLeft: 20 }}><code>profiles/{'{pid}'}/goals/</code> - Goals</div>
                <div style={{ paddingLeft: 20 }}><code>profiles/{'{pid}'}/alerts/</code> - Alerts</div>
                <div style={{ paddingLeft: 20 }}><code>daily_nutrition/{'{date}'}/</code> - Nutrition</div>
                <div style={{ paddingLeft: 20 }}><code>last_sync</code> - Last sync timestamp</div>
                <div><code>device_mapping/{'{uid}'}</code> - Device registration</div>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">Quick Actions</div>
            <div className="card-body">
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <a href="/users" className="btn btn-primary">Manage Users</a>
                <a href="/database" className="btn btn-outline">Browse Raw Data</a>
                <a href="/settings" className="btn btn-outline">App Settings</a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
