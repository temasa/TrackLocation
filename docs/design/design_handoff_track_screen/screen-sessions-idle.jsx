// Sessions screen — IDLE state. Matches the real app screenshot:
//   - Plain page header (eyebrow + title), NO switch
//   - Switch lives INSIDE the dark green hero card on the right
//   - Empty state: white circle w/ broadcast icon + short helper copy

function SessionsIdle() {
  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />
      <PageHeader eyebrow="Trip Tracker" title="Sessions" />

      {/* Hero card contains both the status text AND the M3 switch */}
      <StatusHero on={false} />

      {/* Empty state — vertically centered in remaining space */}
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', padding: '0 36px', textAlign: 'center', gap: 18,
      }}>
        <div style={{
          width: 86, height: 86, borderRadius: 43, background: '#ffffff',
          border: `1px solid ${T.hair}`, display: 'grid', placeItems: 'center',
          boxShadow: '0 1px 3px rgba(0,0,0,.04)',
        }}>
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke={T.ink3} strokeWidth="1.6" strokeLinecap="round">
            <circle cx="12" cy="12" r="2.4" fill={T.ink3} stroke="none" />
            <path d="M6 12a6 6 0 0112 0M3.5 12a8.5 8.5 0 0117 0" />
          </svg>
        </div>
        <div style={{
          fontSize: 22, fontWeight: 700, color: T.ink, letterSpacing: -.3,
        }}>No sessions recorded yet</div>
        <div style={{
          fontSize: 16, color: T.ink2, lineHeight: 1.45, maxWidth: 300,
        }}>
          Sessions appear here when always-recording is turned <strong style={{ color: T.ink, fontWeight: 700 }}>ON</strong>.
        </div>
      </div>

      <BottomNav active="session" />
    </div>
  );
}

window.SessionsIdle = SessionsIdle;
