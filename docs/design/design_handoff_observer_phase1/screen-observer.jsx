// Observer Feed screen — drives all variants from a single component.
// Variants: 'running' | 'service-disabled' | 'capture-paused' | 'autoscroll-paused' | 'empty'
//
// Event card layout (compact, technical, monospace where it counts):
//   ┌─────────────────────────────────────────────────────────────┐
//   │ com.android.chrome              TYPE_VIEW_CLICKED  12:34:56 │
//   │ .browser.ChromeTabbedActivity                               │
//   │ ↳ "Search results"                                          │
//   └─────────────────────────────────────────────────────────────┘

// Event type chip → small tonal pill, color-tagged by category.
function EventTypeChip({ type }) {
  // map type prefix to a tone — purely visual, helps scanning long logs
  const tone = (() => {
    if (/CLICK|TOUCH|GESTURE/.test(type))    return { bg: '#eef6ff', text: '#1d4ed8' };
    if (/SCROLL|FOCUS|HOVER/.test(type))     return { bg: '#f5f3ff', text: '#6d28d9' };
    if (/TEXT|SELECTION|INPUT/.test(type))   return { bg: '#fef7e6', text: '#92400e' };
    if (/WINDOW|CONTENT|STATE/.test(type))   return { bg: '#ecfdf5', text: '#047857' };
    if (/ANNOUNCE|NOTIFICATION/.test(type))  return { bg: '#fdf2f8', text: '#9d174d' };
    return                                          { bg: '#f3f4f6', text: '#374151' };
  })();
  return (
    <span style={{
      padding: '2px 8px', borderRadius: 6, background: tone.bg, color: tone.text,
      fontFamily: OB_MONO, fontSize: 10.5, fontWeight: 500, letterSpacing: .1,
      whiteSpace: 'nowrap', flexShrink: 0,
    }}>{type}</span>
  );
}

function EventRow({ pkg, activity, type, time, snippet, alt }) {
  return (
    <div style={{
      padding: '10px 18px',
      borderBottom: `1px solid ${OB.hairSoft}`,
      background: alt ? OB.cardAlt : '#fff',
      fontFamily: OB_FONT,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{
          fontFamily: OB_MONO, fontSize: 12.5, fontWeight: 600, color: OB.ink,
          letterSpacing: -.1, flex: 1, minWidth: 0,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{pkg}</span>
        <EventTypeChip type={type} />
        <span style={{
          fontFamily: OB_MONO, fontSize: 11, color: OB.ink2,
          fontVariantNumeric: 'tabular-nums', flexShrink: 0,
        }}>{time}</span>
      </div>
      {activity && (
        <div style={{
          marginTop: 3, fontFamily: OB_MONO, fontSize: 11.5, color: OB.ink2,
          letterSpacing: -.1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{activity}</div>
      )}
      {snippet && (
        <div style={{
          marginTop: 4, display: 'flex', alignItems: 'baseline', gap: 6,
        }}>
          <span style={{ color: OB.ink3, fontFamily: OB_MONO, fontSize: 11 }}>↳</span>
          <span style={{
            fontFamily: OB_FONT, fontSize: 12, color: OB.ink, fontStyle: 'italic',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1, minWidth: 0,
          }}>"{snippet}"</span>
        </div>
      )}
    </div>
  );
}

// ─────────── Sample event data ───────────
const SAMPLE_EVENTS = [
  { pkg: 'com.android.chrome',        activity: '.browser.ChromeTabbedActivity', type: 'TYPE_VIEW_CLICKED',          time: '12:34:56', snippet: 'Search results' },
  { pkg: 'com.android.chrome',        activity: '.browser.ChromeTabbedActivity', type: 'TYPE_WINDOW_CONTENT_CHANGED', time: '12:34:55' },
  { pkg: 'com.android.systemui',      activity: '.statusbar.phone.PanelView',    type: 'TYPE_WINDOW_STATE_CHANGED',   time: '12:34:52' },
  { pkg: 'com.example.tracklocation', activity: '.ui.SessionsActivity',          type: 'TYPE_VIEW_SCROLLED',          time: '12:34:48' },
  { pkg: 'com.google.android.gm',     activity: '.ConversationListActivity',     type: 'TYPE_VIEW_FOCUSED',           time: '12:34:42', snippet: 'Inbox · 23 unread' },
  { pkg: 'com.android.chrome',        activity: '.browser.ChromeTabbedActivity', type: 'TYPE_VIEW_TEXT_SELECTION',    time: '12:34:38' },
  { pkg: 'com.example.tracklocation', activity: '.ui.SessionsActivity',          type: 'TYPE_WINDOW_CONTENT_CHANGED', time: '12:34:31' },
  { pkg: 'com.android.settings',      activity: '.SubSettings',                  type: 'TYPE_ANNOUNCEMENT',           time: '12:34:24', snippet: 'Accessibility' },
  { pkg: 'com.whatsapp',              activity: '.HomeActivity',                 type: 'TYPE_VIEW_CLICKED',           time: '12:34:18' },
  { pkg: 'com.android.systemui',      activity: '.qs.QSPanel',                   type: 'TYPE_NOTIFICATION_STATE',     time: '12:34:11' },
];

// ─────────── Jump-to-latest FAB (transient) ───────────
function JumpToLatestFab({ visible }) {
  if (!visible) return null;
  return (
    <button style={{
      position: 'absolute', left: '50%', transform: 'translateX(-50%)', bottom: 92,
      height: 40, padding: '0 16px 0 12px', borderRadius: 20, background: OB.ink,
      color: '#fff', border: 'none', display: 'flex', alignItems: 'center', gap: 6,
      fontFamily: OB_FONT, fontSize: 13, fontWeight: 600, cursor: 'pointer',
      boxShadow: '0 6px 18px rgba(0,0,0,.25)',
    }}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.3" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 5v14M19 12l-7 7-7-7" />
      </svg>
      Jump to latest
    </button>
  );
}

// ─────────── Tap-zone hint (subtle, only when running) ───────────
function TapHint({ message }) {
  return (
    <div style={{
      position: 'absolute', left: '50%', transform: 'translateX(-50%)', bottom: 92,
      padding: '6px 12px', borderRadius: 999, background: 'rgba(10,10,10,.7)', color: '#fff',
      fontFamily: OB_FONT, fontSize: 11, fontWeight: 500, letterSpacing: .2,
      pointerEvents: 'none',
    }}>{message}</div>
  );
}

// ─────────── Screen ───────────
function ScreenObserver({ variant = 'running', showAllowlistBadge = false }) {
  const serviceEnabled    = variant !== 'service-disabled';
  const capturePaused     = variant === 'capture-paused';
  const autoscrollPaused  = variant === 'autoscroll-paused' || capturePaused;
  const showFab           = variant === 'autoscroll-paused';
  const emptyState        = variant === 'empty';

  // For "capture-paused", events shown are the ones recorded BEFORE pause.
  // For "service-disabled", events shown are also historical (capture state separate).
  const events = emptyState ? [] : SAMPLE_EVENTS;

  return (
    <PhoneShell>
      <ObStatusBar />
      <TopAppBar
        title="Observer"
        trailing={
          <IconButton
            label="Allowlist"
            badge={showAllowlistBadge}
            icon={
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 5h16l-6 8v6l-4-2v-4L4 5z" />
              </svg>
            }
          />
        }
      />

      {/* Status block: service banner + capture/auto-scroll chips */}
      <ServiceBanner enabled={serviceEnabled} />
      <div style={{ display: 'flex', gap: 10, padding: '10px 18px 6px' }}>
        <StatusChip
          tone={capturePaused ? 'paused' : 'running'}
          label="Capture"
          value={capturePaused ? 'Paused' : 'Running'}
          icon={capturePaused ? (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="5" width="4" height="14" rx="1" /><rect x="14" y="5" width="4" height="14" rx="1" />
            </svg>
          ) : (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
              <circle cx="12" cy="12" r="4" fill="currentColor" />
            </svg>
          )}
        />
        <StatusChip
          tone={autoscrollPaused ? 'paused' : 'running'}
          label="Auto-scroll"
          value={autoscrollPaused ? 'Paused' : 'Running'}
          interactive={false}
          icon={autoscrollPaused ? (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
              <rect x="6" y="5" width="4" height="14" rx="1" fill="currentColor" stroke="none" />
              <rect x="14" y="5" width="4" height="14" rx="1" fill="currentColor" stroke="none" />
            </svg>
          ) : (
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 5v14M5 12l7 7 7-7" />
            </svg>
          )}
        />
      </div>

      {/* Optional sub-status line — clarifies the paused conditions */}
      {capturePaused && (
        <div style={{
          margin: '4px 18px 0', padding: '8px 12px', borderRadius: 10,
          background: OB.amberBg, border: `1px solid ${OB.amberHair}`,
          fontFamily: OB_FONT, fontSize: 12, color: OB.amberDark, lineHeight: 1.35,
        }}>
          Capture paused — new events are not being received or stored. Previously recorded events remain visible.
        </div>
      )}

      {/* Feed */}
      <div style={{
        flex: 1, marginTop: 8, position: 'relative',
        background: '#fff', borderTop: `1px solid ${OB.hair}`, borderBottom: `1px solid ${OB.hair}`,
        overflow: 'hidden',
      }}>
        {emptyState ? (
          <EmptyFeed />
        ) : (
          <div style={{ height: '100%', overflowY: 'auto' }}>
            {/* Feed header showing count + scope */}
            <div style={{
              padding: '8px 18px', display: 'flex', justifyContent: 'space-between',
              alignItems: 'center', background: OB.bg, borderBottom: `1px solid ${OB.hair}`,
              fontFamily: OB_MONO, fontSize: 10.5, color: OB.ink2, letterSpacing: 1,
              textTransform: 'uppercase',
            }}>
              <span>{events.length} events · all packages</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span style={{
                  width: 6, height: 6, borderRadius: 3,
                  background: capturePaused ? OB.amber : OB.green,
                }} />
                {capturePaused ? 'paused' : 'live'}
              </span>
            </div>
            {events.map((e, i) => (
              <EventRow key={i} {...e} alt={i % 2 === 1} />
            ))}
          </div>
        )}

        {/* Transient jump-to-latest FAB shown only on autoscroll-paused→running transition */}
        <JumpToLatestFab visible={showFab} />
      </div>

      <BottomNav active="settings" />
    </PhoneShell>
  );
}

function EmptyFeed() {
  return (
    <div style={{
      height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', padding: '0 36px', gap: 14, textAlign: 'center',
    }}>
      <div style={{
        width: 64, height: 64, borderRadius: 32, background: OB.bg,
        border: `1.5px solid ${OB.hair}`, display: 'grid', placeItems: 'center',
        color: OB.ink3,
      }}>
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
          <circle cx="12" cy="12" r="3" />
          <path d="M5.5 12a6.5 6.5 0 0113 0M3 12a9 9 0 0118 0" />
        </svg>
      </div>
      <div style={{ fontFamily: OB_FONT, fontSize: 16, fontWeight: 700, color: OB.ink, letterSpacing: -.2 }}>
        Waiting for events
      </div>
      <div style={{ fontFamily: OB_FONT, fontSize: 13, color: OB.ink2, lineHeight: 1.5, maxWidth: 260 }}>
        Captured accessibility events will appear here as soon as they're received.
      </div>
    </div>
  );
}

window.ScreenObserver = ScreenObserver;
