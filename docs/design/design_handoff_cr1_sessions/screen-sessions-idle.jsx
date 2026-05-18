// Sessions screen — IDLE state.
// Switch OFF · empty state copy from spec · 4-tab bottom nav with Session active.

function SessionsIdle() {
  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />
      <PageHeader
        eyebrow="Trip Tracker"
        title="Sessions"
        right={<RecordingSwitch on={false} />}
      />

      <StatusHero on={false} />

      {/* Empty state */}
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', padding: '0 36px', textAlign: 'center', gap: 14,
      }}>
        <div style={{
          width: 72, height: 72, borderRadius: 36, background: T.card,
          border: `1.5px solid ${T.hair}`, display: 'grid', placeItems: 'center',
        }}>
          <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke={T.ink3} strokeWidth="1.7" strokeLinecap="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M5.5 12a6.5 6.5 0 0113 0M3 12a9 9 0 0118 0" />
          </svg>
        </div>
        <div style={{
          fontSize: 19, fontWeight: 700, color: T.ink, letterSpacing: -.2,
        }}>No sessions recorded yet</div>
        <div style={{
          fontSize: 14, color: T.ink2, lineHeight: 1.5, maxWidth: 280,
        }}>
          Sessions appear here when always-recording is turned <strong style={{ color: T.ink, fontWeight: 600 }}>ON</strong> from the Sessions header.
        </div>
      </div>

      <BottomNav active="session" />
    </div>
  );
}

window.SessionsIdle = SessionsIdle;
