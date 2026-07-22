// Live events loader for the Swimmingo Events page.
// Reads events from the same Firebase Realtime Database the Admin portal writes to,
// so new/edited events appear here (and in the app) automatically.

const EVENTS_DB_URL = 'https://swimmingo-default-rtdb.firebaseio.com/events.json';
const EVENTS_REFRESH_MS = 30000;

document.addEventListener('DOMContentLoaded', () => {
  const container = document.getElementById('eventsTimeline');
  if (!container) return;

  const escapeHtml = (value) => {
    const str = value == null ? '' : String(value);
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const AVATAR_COLORS = [
    '#00796B', '#00897B', '#0277BD', '#1565C0', '#4527A0',
    '#6A1B9A', '#AD1457', '#C2185B', '#D81B60', '#E64A19',
    '#5D4037', '#3949AB', '#00838F', '#2E7D32', '#F4511E',
    '#8E24AA', '#1E88E5', '#0D47A1', '#4A148C', '#00695C',
  ];

  const avatarColor = (title) => {
    let hash = 0;
    for (let i = 0; i < title.length; i++) {
      hash = (hash * 31) + title.charCodeAt(i);
    }
    return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
  };

  // Perceived luminance (YCbCr-style) to pick a readable text color.
  const readableTextColor = (hex) => {
    const c = hex.replace('#', '');
    const r = parseInt(c.substring(0, 2), 16);
    const g = parseInt(c.substring(2, 4), 16);
    const b = parseInt(c.substring(4, 6), 16);
    const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return lum > 0.6 ? '#0F172A' : '#FFFFFF';
  };

  const renderEvents = (events) => {
    if (!events.length) {
      container.innerHTML = '<div class="event-loading">No upcoming events yet. Check back soon!</div>';
      return;
    }
    container.innerHTML = events
      .map((ev) => {
        const title = ev.title || '';
        const trimmed = title.trim();
        let letter;
        if (!trimmed) {
          letter = '?';
        } else if (/^\d/.test(trimmed)) {
          const space = trimmed.indexOf(' ');
          letter = (space > 0 ? trimmed.substring(0, space) : trimmed).toUpperCase();
        } else {
          letter = trimmed.charAt(0).toUpperCase();
        }
        const bg = avatarColor(title.trim());
        const fg = readableTextColor(bg);
        return `
        <div class="event-card fade-up visible">
          <span class="event-date-badge">${escapeHtml(ev.date)}</span>
          <div class="event-body">
            <div class="event-letter-avatar" style="background:${bg};color:${fg};">${escapeHtml(letter)}</div>
            <div class="event-accent-bar" style="background:${bg};"></div>
            <h3>${escapeHtml(ev.title)}</h3>
            <p>${escapeHtml(ev.description)}</p>
            <div class="event-meta">
              <span>&#128205; ${escapeHtml(ev.location)}</span>
              <span>&#9200; ${escapeHtml(ev.time)}</span>
              <span>&#127946; ${escapeHtml(ev.strokes)}</span>
            </div>
            ${
              ev.registrationUrl
                ? `<div class="event-register">
                     <a class="btn btn-primary" href="${escapeHtml(ev.registrationUrl)}" target="_blank" rel="noopener">Register</a>
                   </div>`
                : ''
            }
          </div>
        </div>`;
      })
      .join('');
  };

  const loadEvents = async () => {
    try {
      const res = await fetch(EVENTS_DB_URL, { cache: 'no-store' });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();
      const now = Date.now();
      const events = data
        ? Object.values(data).filter(
            (e) =>
              e &&
              typeof e === 'object' &&
              (e.published === true ||
                (e.publishAt && !isNaN(new Date(e.publishAt)) && new Date(e.publishAt).getTime() <= now))
          )
        : [];
      events.sort((a, b) => (Number(a.id) || 0) - (Number(b.id) || 0));
      renderEvents(events);
    } catch (err) {
      console.error('Failed to load events:', err);
      if (!container.querySelector('.event-card')) {
        container.innerHTML =
          '<div class="event-loading">Could not load events right now. Please refresh.</div>';
      }
    }
  };

  loadEvents();
  setInterval(loadEvents, EVENTS_REFRESH_MS);
});
