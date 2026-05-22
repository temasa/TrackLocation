// Observer Feed screens — five variants on one component, switched by `variant`.
// variant: 'live' | 'capture-paused' | 'service-disabled' | 'empty' | 'with-sheet'

// Demo events
const DEMO_EVENTS = [
  { pkg: 'com.android.systemui',           activity: 'NotificationShadeWindowView', type: 'WIN',    time: '13:24:08.412' },
  { pkg: 'com.google.android.apps.maps',   activity: 'MapActivity',                  type: 'CLICK',  time: '13:24:07.981', snippet: 'Start navigation' },
  { pkg: 'com.google.android.apps.maps',   activity: 'MapActivity',                  type: 'SCROLL', time: '13:24:07.214' },
  { pkg: 'com.android.chrome',             activity: 'Browser.MainActivity',         type: 'TEXT',   time: '13:24:05.602', snippet: 'site:developer.android.com a…' },
  { pkg: 'com.android.chrome',             activity: 'Browser.MainActivity',         type: 'FOCUS',  time: '13:24:04.118' },
  { pkg: 'com.example.tracklocation',      activity: 'session.SessionsActivity',     type: 'WIN',    time: '13:24:01.876' },
  { pkg: 'com.android.settings',           activity: 'SubSettings',                  type: 'CLICK',  time: '13:23:58.330', snippet: 'Accessibility' },
  { pkg: 'com.samsung.android.messaging',  activity: 'ConversationComposer',         type: 'TEXT',   time: '13:23:54.701', snippet: 'on my way' },
  { pkg: 'com.android.launcher3',          activity: 'Launcher',                     type: 'SCROLL', time: '13:23:50.044' },
];

function StateChipsRow({ service, capture, autoscroll }) {
  return (
    <div style={{
      padding: '12px 16px 4px', display: 'grid',
      gridTemplateColumns: '1fr 1fr 1fr', gap: 8,
    }}>
      <StateChip
        label="Service"
        value={service.value}
        tone={service.tone}
        icon={
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="4" y="4" width="16" height="16" rx="3"/><path d="M9 12l2 2 4-4"/></svg>
        }
      />
      <StateChip
        label="Capture"
        value={capture.value}
        tone={capture.tone}
        icon={
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>
        }
      />
      <StateChip
        label="Auto-scroll"
        value={autoscroll.value}
        tone={autoscroll.tone}
        icon={
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 5v14M7 14l5 5 5-5"/></svg>
        }
      />
    </div>
  );
}

function ServiceDisabledBanner() {
  return (
    <div style={{
      margin: '12px 16px 4px', padding: '12px 14px',
      background: OBS.errBg, border: `1.5px solid rgba(220,38,38,.25)`, borderRadius: 14,
      display: 'flex', alignItems: 'flex-start', gap: 10,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={OBS.err} strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, marginTop: 1 }}>
        <path d="M12 2L1.5 21h21z"/><path d="M12 9v5M12 17.5h.01"/>
      </svg>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: OBS.err, letterSpacing: -.1 }}>
          Accessibility service is disabled
        </div>
        <div style={{ fontSize: 12.5, color: T.ink2, marginTop: 2, lineHeight: 1.45 }}>
          New events can't be captured until you enable the TrackLocation accessibility service in system settings. Previously recorded events stay visible below.
        </div>
        <button style={{
          marginTop: 10, height: 36, padding: '0 14px', borderRadius: 18,
          background: '#fff', border: `1.5px solid ${OBS.err}`, color: OBS.err,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'inline-flex',
          alignItems: 'center', gap: 6,
        }}>
          Open Accessibility settings
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14 4h6v6M20 4l-9 9M10 5H5v14h14v-5"/>
          </svg>
        </button>
      </div>
    </div>
  );
}

function AllowlistHelper() {
  return (
    <div style={{
      margin: '8px 16px 4px', padding: '8px 12px',
      background: '#fff', border: `1px dashed ${T.hair}`, borderRadius: 10,
      display: 'flex', alignItems: 'center', gap: 8,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={T.ink2} strokeWidth="1.8" strokeLinecap="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v4M12 16h.01"/></svg>
      <div style={{ fontSize: 12, color: T.ink2, lineHeight: 1.35 }}>
        Capturing all packages. <strong style={{ color: T.ink, fontWeight: 600 }}>Add rules</strong> to reduce noise.
      </div>
    </div>
  );
}

function AllowlistSheet() {
  const [rules, setRules] = React.useState([
    { id: 1, type: 'Exact', pattern: 'com.google.android.apps.maps', enabled: true,  applied: true },
    { id: 2, type: 'Regex', pattern: 'com\\.android\\.', enabled: true,  applied: true },
    { id: 3, type: 'Exact', pattern: 'com.samsung.android.messaging', enabled: false, applied: true },
    { id: 4, type: 'Regex', pattern: 'chrome|firefox', enabled: true, applied: false }, // draft
  ]);
  const hasDraft = rules.some(r => !r.applied);

  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0,
      background: T.card, borderTopLeftRadius: 22, borderTopRightRadius: 22,
      boxShadow: '0 -16px 40px -10px rgba(0,0,0,.18)',
      padding: '8px 0 0', maxHeight: '62%', display: 'flex', flexDirection: 'column',
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      {/* drag handle */}
      <div style={{ display: 'grid', placeItems: 'center', padding: '4px 0 6px' }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: T.hair }} />
      </div>

      {/* header */}
      <div style={{ padding: '0 18px 8px', display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 17, fontWeight: 700, color: T.ink, letterSpacing: -.2 }}>Allowlist</div>
          <div style={{ fontSize: 12, color: T.ink2, marginTop: 2 }}>
            {rules.filter(r => r.enabled).length} of {rules.length} rules enabled
          </div>
        </div>
        <button aria-label="Add rule" style={{
          width: 36, height: 36, borderRadius: 18, background: T.bg,
          border: `1px solid ${T.hair}`, display: 'grid', placeItems: 'center', cursor: 'pointer',
        }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.ink} strokeWidth="2.2" strokeLinecap="round">
            <path d="M12 5v14M5 12h14"/>
          </svg>
        </button>
      </div>

      {/* draft indicator */}
      {hasDraft && (
        <div style={{
          margin: '0 18px 8px', padding: '8px 12px',
          background: OBS.warnBg, border: `1px solid rgba(217,119,6,.25)`, borderRadius: 10,
          display: 'flex', alignItems: 'center', gap: 8,
        }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={OBS.warn} strokeWidth="2" strokeLinecap="round">
            <circle cx="12" cy="12" r="9"/><path d="M12 8v5"/><path d="M12 16.5h.01"/>
          </svg>
          <span style={{ fontSize: 12.5, color: OBS.warn, fontWeight: 600 }}>Draft changes not applied</span>
        </div>
      )}

      {/* list */}
      <div style={{ overflow: 'hidden', flex: 1 }}>
        {rules.map((r, i) => (
          <RuleRow key={r.id} rule={r}
            onToggleType={() => setRules(rs => rs.map(x => x.id === r.id ? ({ ...x, type: x.type === 'Exact' ? 'Regex' : 'Exact', applied: false }) : x))}
            onToggleEnable={() => setRules(rs => rs.map(x => x.id === r.id ? ({ ...x, enabled: !x.enabled, applied: false }) : x))}
            onDelete={() => setRules(rs => rs.filter(x => x.id !== r.id))}
            first={i === 0}
          />
        ))}
      </div>

      {/* footer */}
      <div style={{
        padding: '10px 16px 14px', borderTop: `1px solid ${T.hair}`,
        display: 'flex', gap: 10, alignItems: 'center',
      }}>
        <button style={{
          flex: 1, height: 44, borderRadius: 22, background: T.card,
          border: `1.5px solid ${T.hair}`, color: T.ink, fontWeight: 500, fontSize: 14,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif', cursor: 'pointer',
        }}>Close</button>
        <button style={{
          flex: 1, height: 44, borderRadius: 22, background: hasDraft ? T.green : '#cfd2cd',
          border: 'none', color: '#fff', fontWeight: 700, fontSize: 14,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          cursor: hasDraft ? 'pointer' : 'default',
        }}>Apply</button>
      </div>
    </div>
  );
}

function RuleRow({ rule, onToggleType, onToggleEnable, onDelete, first }) {
  return (
    <div style={{
      padding: '10px 18px',
      borderTop: first ? 'none' : `1px solid ${T.hairSoft}`,
      display: 'grid', gridTemplateColumns: '74px 1fr auto auto', gap: 10, alignItems: 'center',
      opacity: rule.enabled ? 1 : .55,
    }}>
      {/* match-type segmented toggle */}
      <button onClick={onToggleType} style={{
        height: 30, padding: 2, border: `1px solid ${T.hair}`, borderRadius: 999,
        background: T.bg, display: 'flex', cursor: 'pointer',
      }}>
        <span style={{
          flex: 1, fontSize: 11, fontWeight: 600, letterSpacing: .3,
          display: 'grid', placeItems: 'center', borderRadius: 999,
          background: rule.type === 'Exact' ? T.ink : 'transparent',
          color: rule.type === 'Exact' ? '#fff' : T.ink2,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        }}>EXACT</span>
        <span style={{
          flex: 1, fontSize: 11, fontWeight: 600, letterSpacing: .3,
          display: 'grid', placeItems: 'center', borderRadius: 999,
          background: rule.type === 'Regex' ? T.ink : 'transparent',
          color: rule.type === 'Regex' ? '#fff' : T.ink2,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        }}>REGEX</span>
      </button>

      {/* pattern */}
      <div style={{
        fontFamily: OBS.mono, fontSize: 12.5, color: T.ink, fontWeight: 500,
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        padding: '6px 10px', background: T.bg, border: `1px solid ${T.hair}`, borderRadius: 8,
        letterSpacing: -.1,
      }}>{rule.pattern}</div>

      {/* enable switch (compact) */}
      <button onClick={onToggleEnable} aria-pressed={rule.enabled} style={{
        width: 34, height: 20, borderRadius: 10,
        background: rule.enabled ? T.green : '#d6d8d4',
        border: 'none', position: 'relative', cursor: 'pointer', padding: 0,
        transition: 'background .15s',
      }}>
        <span style={{
          position: 'absolute', top: 2, left: rule.enabled ? 16 : 2,
          width: 16, height: 16, borderRadius: 8, background: '#fff',
          boxShadow: '0 1px 2px rgba(0,0,0,.2)', transition: 'left .15s',
        }} />
      </button>

      {/* delete */}
      <button onClick={onDelete} aria-label="Delete rule" style={{
        width: 30, height: 30, borderRadius: 15, background: 'transparent',
        border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer',
      }}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.ink2} strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"/>
        </svg>
      </button>
    </div>
  );
}

function EmptyFeed() {
  return (
    <div style={{
      flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', padding: '0 36px', textAlign: 'center', gap: 14,
    }}>
      <div style={{
        width: 64, height: 64, borderRadius: 32, background: T.card,
        border: `1.5px solid ${T.hair}`, display: 'grid', placeItems: 'center',
      }}>
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke={T.ink3} strokeWidth="1.7" strokeLinecap="round">
          <circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3" fill={T.ink3} stroke="none"/>
        </svg>
      </div>
      <div style={{ fontSize: 17, fontWeight: 700, color: T.ink, letterSpacing: -.2 }}>
        Waiting for events
      </div>
      <div style={{ fontSize: 13, color: T.ink2, lineHeight: 1.5, maxWidth: 260 }}>
        Captured events will stream in here. Open another app to generate accessibility events.
      </div>
    </div>
  );
}

function ScreenObserverFeed({ variant = 'live' }) {
  const service    = variant === 'service-disabled'
    ? { value: 'Disabled', tone: 'err' }
    : { value: 'Enabled',  tone: 'ok' };
  const capture    = variant === 'capture-paused'
    ? { value: 'Paused',   tone: 'warn' }
    : { value: 'Running',  tone: 'ok' };
  const autoscroll = (variant === 'capture-paused' || variant === 'service-disabled' || variant === 'with-sheet')
    ? { value: 'Paused',   tone: 'warn' }
    : { value: 'Running',  tone: 'ok' };

  const showEmpty       = variant === 'empty';
  const showSheet       = variant === 'with-sheet';
  const showJumpFab     = variant === 'capture-paused' || variant === 'with-sheet'; // recently transitioned
  const events          = showEmpty ? [] : DEMO_EVENTS;

  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />
      <ObsTopBar
        title="Observer"
        trailing={<FilterButton badge={showSheet} />}
      />

      {variant === 'service-disabled' && <ServiceDisabledBanner />}
      <StateChipsRow service={service} capture={capture} autoscroll={autoscroll} />
      <AllowlistHelper />

      {/* meta row */}
      <div style={{
        margin: '10px 22px 6px', display: 'flex', justifyContent: 'space-between',
        alignItems: 'baseline',
      }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: T.ink, letterSpacing: -.1 }}>
          {showEmpty ? 'No events yet' : `${events.length} events`}
        </div>
        <div style={{ fontSize: 11.5, color: T.ink3, fontFamily: OBS.mono }}>
          {variant === 'capture-paused' ? 'paused · drag to scroll' :
           variant === 'service-disabled' ? 'history shown · capture off' :
           autoscroll.value === 'Paused' ? 'tap list to resume' : 'tap list to pause'}
        </div>
      </div>

      {/* feed */}
      <div style={{
        flex: 1, overflow: 'hidden', background: T.card,
        borderTop: `1px solid ${T.hair}`, borderBottom: `1px solid ${T.hair}`,
        margin: '0 16px', borderRadius: 14,
      }}>
        {showEmpty ? <EmptyFeed /> : events.map((e, i) => (
          <EventRow key={i} {...e}
            dim={variant === 'service-disabled' && i < 3 /* recent dim while disabled */ ? false : false} />
        ))}
      </div>

      <JumpFab visible={showJumpFab} />

      {/* Bottom nav: settings is active because we navigated from Settings -> Tools -> Observer */}
      <BottomNav active="settings" />

      {showSheet && <AllowlistSheet />}
    </div>
  );
}

window.ScreenObserverFeed = ScreenObserverFeed;
