import { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { auth, onAuthStateChanged } from './firebase';
import Login from './components/Login';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import Users from './components/Users';
import UserDetail from './components/UserDetail';
import Swimmers from './components/Swimmers';
import Events from './components/Events';
import DailyFeed from './components/DailyFeed';
import DatabaseBrowser from './components/DatabaseBrowser';
import AppSettings from './components/AppSettings';
import './App.css';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(u);
      setLoading(false);
    });
    return () => unsub();
  }, []);

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="spinner" />
        <p>Loading SwimminGO Admin...</p>
      </div>
    );
  }

  if (!user) {
    return <Login />;
  }

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="users" element={<Users />} />
        <Route path="users/:uid" element={<UserDetail />} />
        <Route path="swimmers" element={<Swimmers />} />
        <Route path="events" element={<Events />} />
        <Route path="dailyfeed" element={<DailyFeed />} />
        <Route path="database" element={<DatabaseBrowser />} />
        <Route path="settings" element={<AppSettings />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}

export default App;
