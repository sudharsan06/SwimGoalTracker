import { useState, useEffect } from 'react';
import { ref as dbRef, push, set, get, query, orderByChild, limitToLast, serverTimestamp } from 'firebase/database';
import { database } from '../firebase';

const VIDEO_URL = 'https://swimmingo.web.app/video/today.mp4';

export default function DailyFeed() {
  const [videos, setVideos] = useState([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [publishDate, setPublishDate] = useState(new Date().toISOString().split('T')[0]);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    loadVideos();
  }, []);

  const loadVideos = async () => {
    try {
      const snapshot = await get(query(
        dbRef(database, 'daily_feed/videos'),
        orderByChild('created_at'),
        limitToLast(20)
      ));
      const list = [];
      if (snapshot.exists()) {
        snapshot.forEach((child) => {
          list.push({ id: child.key, ...child.val() });
        });
        list.reverse();
      }
      setVideos(list);
    } catch (err) {
      console.error('Failed to load videos', err);
    }
  };

  const handleSave = async () => {
    if (!title.trim()) {
      setMessage('Please provide a title');
      return;
    }

    setSaving(true);
    setMessage('');

    try {
      const videoId = `daily_${Date.now()}`;
      const videoData = {
        video_url: VIDEO_URL,
        thumbnail_url: '',
        title: title.trim(),
        description: description.trim(),
        publish_date: publishDate,
        created_at: serverTimestamp(),
      };

      const videoRef = dbRef(database, `daily_feed/videos/${videoId}`);
      await set(videoRef, videoData);

      setMessage('Video metadata published successfully!');
      setTitle('');
      setDescription('');
      setPublishDate(new Date().toISOString().split('T')[0]);

      loadVideos();
    } catch (err) {
      setMessage('Save failed: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (videoId) => {
    if (!window.confirm('Delete this video entry? This cannot be undone.')) return;
    try {
      await set(dbRef(database, `daily_feed/videos/${videoId}`), null);
      setMessage('Video entry deleted');
      loadVideos();
    } catch (err) {
      setMessage('Delete failed: ' + err.message);
    }
  };

  return (
    <div className="dashboard">
      <h1>Daily Feed Manager</h1>
      <p>
        Publish daily video metadata. Upload the video file manually to your hosting at{' '}
        <code>https://swimmingo.web.app/video/today.mp4</code>.
      </p>

      {/* Publish Form */}
      <div className="card" style={{ marginBottom: 24 }}>
        <h3>Publish Today's Video</h3>

        <div className="form-row">
          <label>Title *</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Today's Freestyle Technique Tip"
            disabled={saving}
          />
        </div>

        <div className="form-row">
          <label>Description</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Brief description of the video content..."
            rows={3}
            disabled={saving}
          />
        </div>

        <div className="form-row">
          <label>Publish Date</label>
          <input
            type="date"
            value={publishDate}
            onChange={(e) => setPublishDate(e.target.value)}
            disabled={saving}
          />
        </div>

        <div className="form-row">
          <label>Video URL</label>
          <input type="text" value={VIDEO_URL} readOnly disabled />
          <small>The video file at this URL is replaced daily. No upload needed here.</small>
        </div>

        <button
          className="btn-primary"
          onClick={handleSave}
          disabled={saving || !title.trim()}
        >
          {saving ? 'Saving...' : 'Publish Metadata'}
        </button>

        {message && (
          <p className={message.includes('fail') ? 'error' : 'success'}>
            {message}
          </p>
        )}
      </div>

      {/* Published Entries */}
      <div className="card">
        <h3>Published Entries ({videos.length})</h3>
        {videos.length === 0 ? (
          <p style={{ color: '#888', marginTop: 12 }}>No entries published yet.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Publish Date</th>
                  <th>Video URL</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {videos.map((v) => (
                  <tr key={v.id}>
                    <td>{v.title || 'Untitled'}</td>
                    <td>{v.publish_date || '-'}</td>
                    <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {v.video_url ? (
                        <a href={v.video_url} target="_blank" rel="noreferrer">
                          {v.video_url.substring(0, 50)}...
                        </a>
                      ) : '-'}
                    </td>
                    <td>
                      <button
                        className="btn-danger"
                        onClick={() => handleDelete(v.id)}
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

      <style>{`
        .form-row { margin-bottom: 12px; }
        .form-row label { display: block; font-weight: 600; margin-bottom: 4px; font-size: 13px; color: #555; }
        .form-row input[type="text"],
        .form-row textarea,
        .form-row input[type="date"],
        .form-row input[type="file"] {
          width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; box-sizing: border-box;
        }
        .form-row textarea { resize: vertical; }
        .form-row small { display: block; margin-top: 4px; color: #888; }
        .form-row input:disabled { background: #f5f5f5; }
        .btn-primary {
          background: #007bff; color: #fff; border: none; padding: 10px 24px; border-radius: 6px;
          font-size: 14px; cursor: pointer; font-weight: 600;
        }
        .btn-primary:disabled { background: #aaa; cursor: not-allowed; }
        .btn-danger {
          background: #dc3545; color: #fff; border: none; padding: 6px 14px; border-radius: 4px;
          font-size: 12px; cursor: pointer;
        }
        .error { color: #dc3545; margin-top: 8px; font-size: 13px; }
        .success { color: #28a745; margin-top: 8px; font-size: 13px; }
        .card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .table-wrapper { overflow-x: auto; margin-top: 12px; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #eee; }
        th { font-weight: 600; color: #555; }
        code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
      `}</style>
    </div>
  );
}
