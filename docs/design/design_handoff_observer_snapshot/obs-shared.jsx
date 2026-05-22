// Observer (Phase 1) — shared components, built on the same token palette
// as the existing TrackLocation screens. Material 3, compact operational look.

// Adds a few Observer-specific tokens onto the existing T from cr1-shared.jsx.
const OBS = {
  // status accents — semantic, paired with icon + text (not color-only)
  ok:        T.green,         // running / enabled
  okDark:    T.greenDark,
  warn:      '#d97706',       // paused (advisory)
  warnBg:    'rgba(217,119,6,.10)',
  err:       T.red,           // disabled / not applied
  errBg:     'rgba(220,38,38,.10)',
  // neutrals tuned for dense feed
  mono:      'Roboto Mono, ui-monospace, SFMono-Regular, Menlo, monospace',
  rowDiv:    '#f1f4ef',       // tighter divider between feed rows
  chipBg:    '#ffffff',
};

// ─────────── Top app bar (back · title · trailing) ───────────
function ObsTopBar({ title, onBack, trailing }) {
  return (
    <div style={{
      height: 56, padding: '0 6px 0 4px', display: 'flex', alignItems: 'center',
      gap: 4, background: T.bg, borderBottom: `1px solid ${T.hair}`,
    }}>
      <button onClick={onBack} aria-label="Back" style={{
        width: 44, height: 44, borderRadius: 22, background: 'transparent',
        border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer',
      }}>
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={T.ink} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </button>
      <div style={{
        flex: 1, fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        fontSize: 22, fontWeight: 700, color: T.ink, letterSpacing: -.3,
      }}>{title}</div>
      {trailing}
    </div>
  );
}

// ─────────── State chip — icon + label + value (text, not color-only) ───────────
// tone: 'ok' | 'warn' | 'err' | 'neutral'
function StateChip({ label, value, tone = 'neutral', icon, onClick, action }) {
  const palette = {
    ok:      { dot: OBS.ok,   text: OBS.okDark, ring: OBS.ok,   bg: 'rgba(34,197,94,.10)' },
    warn:    { dot: OBS.warn, text: OBS.warn,   ring: OBS.warn, bg: OBS.warnBg },
    err:     { dot: OBS.err,  text: OBS.err,    ring: OBS.err,  bg: OBS.errBg },
    neutral: { dot: T.ink3,   text: T.ink2,     ring: T.hair,   bg: '#fff' },
  }[tone];
  return (
    <div style={{
      flex: 1, minWidth: 0, padding: '10px 12px',
      background: OBS.chipBg, border: `1.5px solid ${T.hair}`, borderRadius: 14,
      display: 'flex', flexDirection: 'column', gap: 6,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        fontSize: 11, fontWeight: 500, color: T.ink2, letterSpacing: .3,
        textTransform: 'uppercase',
      }}>
        {icon}
        <span>{label}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{
          width: 8, height: 8, borderRadius: 4, background: palette.dot, flexShrink: 0,
          boxShadow: tone === 'ok' ? `0 0 0 3px ${palette.bg}` : 'none',
        }} />
        <span style={{
          fontSize: 14, fontWeight: 700, color: palette.text, letterSpacing: -.1,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>{value}</span>
      </div>
      {action && (
        <button onClick={onClick} style={{
          marginTop: 2, padding: '6px 0', border: 'none', background: 'transparent',
          color: palette.text, fontSize: 12, fontWeight: 600, textAlign: 'left',
          cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        }}>
          {action}
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
            <path d="M9 6l6 6-6 6"/>
          </svg>
        </button>
      )}
    </div>
  );
}

// ─────────── Event row — compact, mono-numeric, dense ───────────
const EVT_ICONS = {
  CLICK:  <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" strokeLinecap="round"/>,
  SCROLL: <><path d="M12 5v14" strokeLinecap="round"/><path d="M7 10l5-5 5 5M7 14l5 5 5-5" strokeLinecap="round" strokeLinejoin="round"/></>,
  TEXT:   <><path d="M4 6h16M4 12h12M4 18h8" strokeLinecap="round"/></>,
  WIN:    <><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 8h18" /></>,
  FOCUS:  <><circle cx="12" cy="12" r="3"/><path d="M5 5l3 3M16 16l3 3M19 5l-3 3M8 16l-3 3" strokeLinecap="round"/></>,
};
const EVT_LABEL = {
  CLICK: 'view clicked', SCROLL: 'view scrolled', TEXT: 'text changed',
  WIN:   'window state', FOCUS: 'view focused',
};

function EventRow({ pkg, activity, type = 'CLICK', time, snippet, dim }) {
  return (
    <div style={{
      padding: '10px 18px 10px 18px',
      display: 'grid', gridTemplateColumns: '28px 1fr auto', gap: 10,
      alignItems: 'flex-start',
      borderBottom: `1px solid ${OBS.rowDiv}`,
      opacity: dim ? .55 : 1,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      {/* leading icon */}
      <div style={{
        width: 28, height: 28, borderRadius: 8, background: T.bg,
        border: `1px solid ${T.hair}`, display: 'grid', placeItems: 'center',
        marginTop: 1,
      }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={T.ink2} strokeWidth="1.8">
          {EVT_ICONS[type] || EVT_ICONS.CLICK}
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
          <span>{EVT_LABEL[type] || type.toLowerCase()}</span>
        </div>
        {snippet && (
          <div style={{
            marginTop: 6, padding: '6px 8px', background: '#fafbfa',
            border: `1px solid ${T.hairSoft}`, borderRadius: 8,
            fontSize: 12, color: T.ink2, lineHeight: 1.35,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>“{snippet}”</div>
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

// ─────────── Transient jump-to-latest FAB ───────────
function JumpFab({ visible }) {
  return (
    <button style={{
      position: 'absolute', right: 16, bottom: 88,
      height: 40, padding: '0 14px 0 12px', borderRadius: 20,
      background: T.ink, color: '#fff', border: 'none',
      display: 'flex', alignItems: 'center', gap: 8,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
      fontSize: 13, fontWeight: 600, letterSpacing: .1, cursor: 'pointer',
      boxShadow: '0 8px 18px -6px rgba(0,0,0,.4)',
      opacity: visible ? 1 : 0, transform: `translateY(${visible ? 0 : 6}px)`,
      transition: 'opacity .18s, transform .18s', pointerEvents: visible ? 'auto' : 'none',
    }}>
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 5v14M5 12l7 7 7-7"/>
      </svg>
      Jump to latest
    </button>
  );
}

// ─────────── Settings row ───────────
function SettingsRow({ icon, title, supporting, trailing, danger }) {
  return (
    <div style={{
      padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 14,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif', cursor: 'pointer',
    }}>
      <div style={{
        width: 36, height: 36, borderRadius: 10, background: T.bg,
        border: `1px solid ${T.hair}`, display: 'grid', placeItems: 'center', flexShrink: 0,
      }}>
        {icon}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 15, fontWeight: 600, color: danger ? T.red : T.ink, letterSpacing: -.1,
        }}>{title}</div>
        {supporting && (
          <div style={{ fontSize: 12.5, color: T.ink2, marginTop: 2 }}>{supporting}</div>
        )}
      </div>
      {trailing || (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={T.ink3} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M9 6l6 6-6 6"/>
        </svg>
      )}
    </div>
  );
}

function SettingsSection({ title, children }) {
  return (
    <div style={{ margin: '18px 22px 0' }}>
      <div style={{
        fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        fontSize: 12, fontWeight: 600, color: T.ink2, letterSpacing: 1.2,
        textTransform: 'uppercase', padding: '0 4px 8px',
      }}>{title}</div>
      <div style={{
        background: T.card, border: `1.5px solid ${T.hair}`, borderRadius: 18,
        overflow: 'hidden',
      }}>
        {React.Children.map(children, (c, i) => (
          <React.Fragment>
            {i > 0 && <div style={{ height: 1, background: T.hairSoft, marginLeft: 66 }} />}
            {c}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}

// ─────────── Filter icon trailing (top app bar) ───────────
function FilterButton({ active, badge }) {
  return (
    <button aria-label="Allowlist" style={{
      width: 44, height: 44, borderRadius: 22, background: 'transparent',
      border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer',
      position: 'relative',
    }}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={T.ink} strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 5h16l-6 8v6l-4-2v-4z"/>
      </svg>
      {badge && (
        <span style={{
          position: 'absolute', top: 8, right: 8, width: 8, height: 8, borderRadius: 4,
          background: OBS.err, border: `1.5px solid ${T.bg}`,
        }} />
      )}
    </button>
  );
}

Object.assign(window, {
  OBS, ObsTopBar, StateChip, EventRow, JumpFab,
  SettingsRow, SettingsSection, FilterButton,
});
