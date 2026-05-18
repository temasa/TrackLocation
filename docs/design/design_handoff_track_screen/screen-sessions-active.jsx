// Sessions screen — RECORDING (Active) state.
// Same layout as Idle but: switch ON inside hero, "Active" title, supporting line,
// and the list of recorded sessions (active one pinned to top).

function SessionsActive() {
  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />
      <PageHeader eyebrow="Trip Tracker" title="Sessions" />

      {/* Hero card contains the M3 switch ON, status text changes to Active */}
      <StatusHero
        on={true}
        supportingOverride="Recording location · 00:12:34 · 184 points"
      />

      {/* Section caption, matching the List screen "Recent trips · Newest first" pattern */}
      <div style={{
        margin: '18px 22px 0', display: 'flex', justifyContent: 'space-between',
        alignItems: 'baseline',
      }}>
        <div style={{ fontSize: 17, fontWeight: 700, color: T.ink, letterSpacing: -.2 }}>
          Recorded sessions
        </div>
        <div style={{ fontSize: 13, color: T.ink2 }}>Newest first</div>
      </div>

      <div style={{ flex: 1, overflow: 'hidden', paddingBottom: 8 }}>
        <SessionRow
          index={7}
          start="Today, 08:19"
          durationMs={(12 * 60 + 34) * 1000}
          distanceKm={1.84}
          points={184}
          active
        />
        <SessionRow
          index={6}
          start="Wed, 07:02"
          durationMs={((1 * 60 + 14) * 60 + 22) * 1000}
          distanceKm={9.42}
          points={1862}
        />
        <SessionRow
          index={5}
          start="Tue, 18:41"
          durationMs={((0 * 60 + 41) * 60 + 8) * 1000}
          distanceKm={3.07}
          points={742}
        />
      </div>

      <BottomNav active="session" />
    </div>
  );
}

window.SessionsActive = SessionsActive;
