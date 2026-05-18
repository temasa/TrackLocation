// Shared design tokens + components for the CR#1 Sessions redesign.
// Colors lifted from the current List screen screenshot.

const T = {
  bg:        '#f1f4f0',       // app background
  card:      '#ffffff',
  hair:      '#e7eae6',
  hairSoft:  '#eef0ec',
  hero:      '#173a2d',       // dark green hero card
  heroDim:   '#22513f',       // hero card inner accents
  ink:       '#0a0a0a',
  ink2:      '#737373',
  ink3:      '#a3a3a3',
  green:     '#22c55e',       // active / brand green
  greenDark: '#16a34a',
  blue:      '#2563eb',
  amber:     '#d97706',
  red:       '#dc2626',
  navPill:   '#0e2a20',       // dark green pill behind active nav tab
};

// ─────────── Status bar (Android, matches screenshot chrome) ───────────
function StatusBar({ time = '8:31' }) {
  return (
    <div style={{
      height: 30, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 18px', color: T.ink, fontFamily: 'Roboto, Inter, system-ui, sans-serif',
      fontSize: 13, fontWeight: 500,
    }}>
      <span>{time}</span>
      <span style={{ display: 'flex', gap: 6, alignItems: 'center', color: T.ink2, fontSize: 11 }}>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>4.5G</span>
        <svg width="14" height="10" viewBox="0 0 14 10"><path d="M7 9.5L.6 3.1a9 9 0 0112.8 0L7 9.5z" fill={T.ink}/></svg>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 2 }}>
          <span style={{
            width: 18, height: 9, border: `1.2px solid ${T.ink}`, borderRadius: 2,
            display: 'inline-block', position: 'relative',
          }}>
            <span style={{
              position: 'absolute', inset: 1, width: 4, background: T.green, borderRadius: 1,
            }} />
          </span>
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>26</span>
        </span>
      </span>
    </div>
  );
}

// ─────────── Big page header (eyebrow + title + right slot) ───────────
function PageHeader({ eyebrow = 'Trip Tracker', title, right }) {
  return (
    <div style={{ padding: '14px 22px 6px' }}>
      <div style={{
        fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        fontSize: 14, color: T.ink2, fontWeight: 400,
      }}>{eyebrow}</div>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        marginTop: 2, gap: 12,
      }}>
        <h1 style={{
          margin: 0, fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          fontSize: 40, fontWeight: 800, letterSpacing: -1.2, color: T.ink, lineHeight: 1.05,
        }}>{title}</h1>
        {right}
      </div>
    </div>
  );
}

// ─────────── Outlined pill (matches Export button visual) ───────────
function PillButton({ children, onClick }) {
  return (
    <button onClick={onClick} style={{
      height: 44, padding: '0 22px', borderRadius: 22, background: T.card,
      border: `1.5px solid ${T.hair}`, color: T.ink, fontWeight: 500, fontSize: 15,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif', cursor: 'pointer',
    }}>{children}</button>
  );
}

// ─────────── Always-recording switch — compact, header-sized ───────────
function RecordingSwitch({ on, onChange }) {
  return (
    <button
      onClick={() => onChange && onChange(!on)}
      aria-pressed={on}
      style={{
        height: 44, padding: '0 8px 0 14px', borderRadius: 22,
        background: T.card, border: `1.5px solid ${on ? T.green : T.hair}`,
        display: 'inline-flex', alignItems: 'center', gap: 10, cursor: 'pointer',
        fontFamily: 'Roboto, Inter, system-ui, sans-serif',
      }}
    >
      <span style={{
        fontSize: 13, fontWeight: 600, color: on ? T.greenDark : T.ink2, letterSpacing: .2,
      }}>{on ? 'ON' : 'OFF'}</span>
      <span style={{
        width: 38, height: 22, borderRadius: 11, background: on ? T.green : '#d6d8d4',
        position: 'relative', transition: 'background .2s',
      }}>
        <span style={{
          position: 'absolute', top: 2, left: on ? 18 : 2, width: 18, height: 18, borderRadius: 9,
          background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,.25)', transition: 'left .2s',
        }} />
      </span>
    </button>
  );
}

// ─────────── Dark-green hero status card (parallels List "Ready to record") ───────────
function StatusHero({ on, elapsed = '00:12:34', points = 184 }) {
  return (
    <div style={{
      margin: '14px 22px 0', padding: '20px 22px',
      background: T.hero, borderRadius: 22, color: '#fff',
      display: 'flex', alignItems: 'center', gap: 16, position: 'relative', overflow: 'hidden',
    }}>
      <div style={{
        width: 56, height: 56, borderRadius: 28,
        background: on ? 'rgba(34,197,94,.18)' : T.heroDim,
        display: 'grid', placeItems: 'center', flexShrink: 0, position: 'relative',
      }}>
        <span style={{
          width: 16, height: 16, borderRadius: 8, background: on ? T.green : '#9aa9a1',
          boxShadow: on ? `0 0 0 6px rgba(34,197,94,.22)` : 'none',
          animation: on ? 'sessPulse 1.6s ease-in-out infinite' : 'none',
        }} />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          fontSize: 13, color: 'rgba(255,255,255,.65)', fontWeight: 400,
        }}>Always-recording</div>
        <div style={{
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          fontSize: 22, fontWeight: 700, color: '#fff', letterSpacing: -.4, lineHeight: 1.15,
          marginTop: 2,
        }}>{on ? 'Recording' : 'Idle'}</div>
        {on && (
          <div style={{
            marginTop: 6, fontFamily: 'Roboto, Inter, system-ui, sans-serif', fontSize: 13,
            color: 'rgba(255,255,255,.78)', fontVariantNumeric: 'tabular-nums',
          }}>
            {elapsed} · {points} points
          </div>
        )}
      </div>
      <style>{`@keyframes sessPulse { 0%,100% { opacity: 1 } 50% { opacity: .55 } }`}</style>
    </div>
  );
}

// ─────────── Session row — mirrors the trip row's vertical-line marker ───────────
function SessionRow({ index, start, durationMs, distanceKm, points, active, endTime }) {
  const durationStr = (() => {
    const s = Math.floor(durationMs / 1000);
    const hh = String(Math.floor(s / 3600)).padStart(2, '0');
    const mm = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
    const ss = String(s % 60).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
  })();
  const accent = active ? T.green : T.greenDark;
  return (
    <div style={{
      margin: '12px 22px 0', padding: '18px 18px 18px 20px',
      background: T.card, borderRadius: 20,
      border: `1.5px solid ${active ? T.green : T.hairSoft}`,
      position: 'relative', overflow: 'hidden',
    }}>
      {active && (
        <div style={{
          position: 'absolute', top: 14, right: 16,
          display: 'flex', alignItems: 'center', gap: 6, padding: '4px 10px 4px 8px',
          background: 'rgba(34,197,94,.12)', borderRadius: 999,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif', fontSize: 11, fontWeight: 600,
          color: T.greenDark, letterSpacing: .3, textTransform: 'uppercase',
        }}>
          <span style={{
            width: 7, height: 7, borderRadius: 4, background: T.green,
            animation: 'sessPulse 1.6s ease-in-out infinite',
          }} />
          Active
        </div>
      )}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
        {/* Vertical-line marker like the trip rows */}
        <div style={{ width: 14, paddingTop: 5, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <span style={{ width: 12, height: 12, borderRadius: 6, border: `2.5px solid ${accent}`, background: '#fff' }} />
          <span style={{ width: 2.5, flex: 1, minHeight: 32, background: accent, marginTop: 2, borderRadius: 2 }} />
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 12 }}>
            <div style={{
              fontFamily: 'Roboto, Inter, system-ui, sans-serif',
              fontSize: 19, fontWeight: 700, color: T.ink, letterSpacing: -.2,
            }}>Session #{index}</div>
            <div style={{
              fontFamily: 'Roboto, Inter, system-ui, sans-serif',
              fontSize: 13, color: T.ink2, fontVariantNumeric: 'tabular-nums',
              paddingRight: active ? 70 : 0,
            }}>{start}</div>
          </div>

          <div style={{ height: 1, background: T.hairSoft, margin: '14px 0 14px -34px' }} />

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 4 }}>
            <Metric value={durationStr} unit="duration" />
            <Metric value={`${distanceKm.toFixed(2)} km`} unit="distance" />
            <Metric value={`${points}`} unit="points" />
          </div>
        </div>
      </div>
    </div>
  );
}

function Metric({ value, unit }) {
  return (
    <div>
      <div style={{
        fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        fontSize: 17, fontWeight: 700, color: T.ink, letterSpacing: -.2, lineHeight: 1,
        fontVariantNumeric: 'tabular-nums',
      }}>{value}</div>
      <div style={{
        fontFamily: 'Roboto, Inter, system-ui, sans-serif', fontSize: 12, color: T.ink2,
        marginTop: 6,
      }}>{unit}</div>
    </div>
  );
}

// ─────────── 4-tab bottom nav (Session · List · Track · Settings) ───────────
function BottomNav({ active = 'session' }) {
  const Item = ({ id, label, icon }) => {
    const on = id === active;
    return (
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
      }}>
        <div style={{
          padding: on ? '8px 22px' : '8px 14px', borderRadius: 22,
          background: on ? T.navPill : 'transparent',
          color: on ? T.green : T.ink2, display: 'grid', placeItems: 'center',
          transition: 'all .15s ease',
        }}>{icon}</div>
        <span style={{
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          fontSize: 11, fontWeight: on ? 600 : 500,
          color: on ? T.ink : T.ink2,
        }}>{label}</span>
      </div>
    );
  };
  const sw = 1.9;
  return (
    <div style={{
      padding: '10px 6px 16px', background: T.bg, display: 'flex',
      borderTop: `1px solid ${T.hair}`,
    }}>
      <Item id="session" label="Session" icon={
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={sw} strokeLinecap="round">
          <circle cx="12" cy="12" r="3" fill="currentColor" stroke="none" />
          <path d="M5.5 12a6.5 6.5 0 0113 0M3 12a9 9 0 0118 0" />
        </svg>
      } />
      <Item id="list" label="List" icon={
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={sw} strokeLinecap="round">
          <path d="M4 6h16M4 12h16M4 18h10" />
        </svg>
      } />
      <Item id="track" label="Track" icon={
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={sw}>
          <path d="M12 22s7-7.5 7-13a7 7 0 10-14 0c0 5.5 7 13 7 13z" strokeLinejoin="round"/>
          <circle cx="12" cy="9" r="2.5" />
        </svg>
      } />
      <Item id="settings" label="Settings" icon={
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={sw}>
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1" strokeLinecap="round" />
        </svg>
      } />
    </div>
  );
}

Object.assign(window, {
  T, StatusBar, PageHeader, PillButton, RecordingSwitch, StatusHero,
  SessionRow, BottomNav,
});
