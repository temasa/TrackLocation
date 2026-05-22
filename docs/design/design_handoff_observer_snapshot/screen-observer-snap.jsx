// Observer Feed screen — Phase-2 variant with "View window content" link per card.
// Builds on screen-observer-feed.jsx; same chrome, new card affordance.
//
// Card link rules:
//   - hasSnapshot=true   → "View window content" link visible (trailing-bottom)
//   - hasSnapshot=false  → link hidden (preferred; cleaner). Disabled variant
//                          shown in one artboard for layout-consistency comparison.

// ─────────── Sample events (each tagged with snapshot availability) ───────────
const SNAP_EVENTS = [
  { pkg: 'com.android.systemui',           activity: 'NotificationShadeWindowView', type: 'WIN',    time: '13:24:08.412', hasSnapshot: true },
  { pkg: 'com.google.android.apps.maps',   activity: 'MapActivity',                  type: 'CLICK',  time: '13:24:07.981', snippet: 'Start navigation', hasSnapshot: true },
  { pkg: 'com.google.android.apps.maps',   activity: 'MapActivity',                  type: 'SCROLL', time: '13:24:07.214', hasSnapshot: false },
  { pkg: 'com.android.chrome',             activity: 'Browser.MainActivity',         type: 'TEXT',   time: '13:24:05.602', snippet: 'site:developer.android.com a…', hasSnapshot: true },
  { pkg: 'com.android.chrome',             activity: 'Browser.MainActivity',         type: 'FOCUS',  time: '13:24:04.118', hasSnapshot: false },
  { pkg: 'com.example.tracklocation',      activity: 'session.SessionsActivity',     type: 'WIN',    time: '13:24:01.876', hasSnapshot: true },
  { pkg: 'com.android.settings',           activity: 'SubSettings',                  type: 'CLICK',  time: '13:23:58.330', snippet: 'Accessibility',  hasSnapshot: true },
  { pkg: 'com.samsung.android.messaging',  activity: 'ConversationComposer',         type: 'TEXT',   time: '13:23:54.701', snippet: 'on my way',     hasSnapshot: true },
];

const EVT_LABEL2 = {
  CLICK: 'view clicked', SCROLL: 'view scrolled', TEXT: 'text changed',
  WIN:   'window state', FOCUS: 'view focused',
};
const EVT_ICONS2 = {
  CLICK:  <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" strokeLinecap="round"/>,
  SCROLL: <><path d="M12 5v14" strokeLinecap="round"/><path d="M7 10l5-5 5 5M7 14l5 5 5-5" strokeLinecap="round" strokeLinejoin="round"/></>,
  TEXT:   <><path d="M4 6h16M4 12h12M4 18h8" strokeLinecap="round"/></>,
  WIN:    <><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 8h18" /></>,
  FOCUS:  <><circle cx="12" cy="12" r="3"/><path d="M5 5l3 3M16 16l3 3M19 5l-3 3M8 16l-3 3" strokeLinecap="round"/></>,
};

// ─────────── Event row with snapshot link ───────────
// mode: 'auto' → preferred (hide link when missing)
//       'disabled' → show disabled link with "No snapshot captured" (layout-parity demo)
function EventRowSnap({ pkg, activity, type = 'CLICK', time, snippet, hasSnapshot, mode = 'auto', onOpen }) {
  const showDisabled = mode === 'disabled' && !hasSnapshot;
  const showLink     = hasSnapshot || showDisabled;

  return (
    <div style={{
      padding: '10px 18px 12px',
      display: 'grid', gridTemplateColumns: '28px 1fr auto', gap: 10,
      alignItems: 'flex-start',
      borderBottom: `1px solid ${OBS.rowDiv}`,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      {/* leading icon */}
      <div style={{
        width: 28, height: 28, borderRadius: 8, background: T.bg,
        border: `1px solid ${T.hair}`, display: 'grid', placeItems: 'center',
        marginTop: 1,
      }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={T.ink2} strokeWidth="1.8">
          {EVT_ICONS2[type] || EVT_ICONS2.CLICK}
        </svg>
      </div>

      {/* body */}
      <div style={{ minWidth: 0 }}>
        <div style={{
          fontFamily: OBS.mono, fontSize: 13, fontWeight: 600, color: T.ink,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          letterSpacing: -.1,
        }}>{pkg}</div>
        <div style={{
          fontSize: 12, color: T.ink2, marginTop: 2,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>
          <span style={{ fontFamily: OBS.mono }}>{activity || '—'}</span>
          <span style={{ color: T.ink3 }}>  ·  </span>
          <span>{EVT_LABEL2[type] || type.toLowerCase()}</span>
        </div>
        {snippet && (
          <div style={{
            marginTop: 6, padding: '6px 8px', background: '#fafbfa',
            border: `1px solid ${T.hairSoft}`, borderRadius: 8,
            fontSize: 12, color: T.ink2, lineHeight: 1.35,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>“{snippet}”</div>
        )}

        {/* trailing-aligned link (below metadata) */}
        {showLink && (
          <div style={{
            marginTop: 8, display: 'flex', justifyContent: 'flex-end',
          }}>
            <button onClick={onOpen} disabled={showDisabled} style={{
              border: 'none', background: 'transparent',
              padding: '4px 6px', margin: '-4px -6px -4px 0',
              cursor: showDisabled ? 'default' : 'pointer',
              color: showDisabled ? T.ink3 : T.greenDark,
              fontFamily: 'Roboto, Inter, system-ui, sans-serif',
              fontSize: 12.5, fontWeight: 600, letterSpacing: -.05,
              display: 'inline-flex', alignItems: 'center', gap: 4,
            }}>
              {showDisabled ? 'No snapshot captured' : 'View window content'}
              {!showDisabled && (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 6l6 6-6 6"/>
                </svg>
              )}
            </button>
          </div>
        )}
      </div>

      {/* trailing time */}
      <div style={{
        fontFamily: OBS.mono, fontSize: 11, color: T.ink3, fontVariantNumeric: 'tabular-nums',
        marginTop: 4, whiteSpace: 'nowrap',
      }}>{time}</div>
    </div>
  );
}

// ─────────── Screen ───────────
// linkMode: 'auto' (hide when no snapshot, preferred) | 'disabled' (show disabled state)
// sheet:   null | { state, pkg, activity, type, firstSeen, lastSeen, repeat, mode, copied }
function ScreenObserverSnap({ linkMode = 'auto', sheet = null }) {
  const events = SNAP_EVENTS;

  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />
      <ObsTopBar title="Observer" trailing={<FilterButton />} />

      <StateChipsRow
        service={{ value: 'Enabled',  tone: 'ok' }}
        capture={{ value: 'Running',  tone: 'ok' }}
        autoscroll={{ value: 'Running', tone: 'ok' }}
      />
      <AllowlistHelper />

      <div style={{
        margin: '10px 22px 6px', display: 'flex', justifyContent: 'space-between',
        alignItems: 'baseline',
      }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: T.ink, letterSpacing: -.1 }}>
          {events.length} events
        </div>
        <div style={{ fontSize: 11.5, color: T.ink3, fontFamily: OBS.mono }}>
          tap list to pause
        </div>
      </div>

      <div style={{
        flex: 1, overflow: 'hidden', background: T.card,
        borderTop: `1px solid ${T.hair}`, borderBottom: `1px solid ${T.hair}`,
        margin: '0 16px', borderRadius: 14,
      }}>
        {events.map((e, i) => (
          <EventRowSnap key={i} {...e} mode={linkMode} />
        ))}
      </div>

      <BottomNav active="settings" />

      {sheet && <SnapshotSheet {...sheet} />}
    </div>
  );
}

window.ScreenObserverSnap = ScreenObserverSnap;
window.EventRowSnap = EventRowSnap;
