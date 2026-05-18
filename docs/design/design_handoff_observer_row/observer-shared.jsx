// Observer Phase 1 — Shared tokens + small primitives.
// Visual language matches existing Sessions/List/Track screens.
// Material 3 with the project's custom green brand. Roboto + Roboto Mono.

const OB = {
  // Surfaces (lifted from Sessions/List screens)
  bg:        '#f1f4f0',
  card:      '#ffffff',
  cardAlt:   '#f8faf7',     // alternating row surface
  hair:      '#e7eae6',
  hairSoft:  '#eef0ec',
  hero:      '#173a2d',
  heroDim:   '#22513f',

  // Ink
  ink:       '#0a0a0a',
  ink2:      '#737373',
  ink3:      '#a3a3a3',
  ink4:      '#cdd1cb',

  // Status colors
  green:     '#22c55e',
  greenDark: '#16a34a',
  greenBg:   'rgba(34,197,94,.12)',

  amber:     '#d97706',
  amberDark: '#92400e',
  amberBg:   '#fef7e6',
  amberHair: '#f5e3b8',

  red:       '#dc2626',
  redDark:   '#991b1b',
  redBg:     '#fef2f2',
  redHair:   '#fecaca',

  blue:      '#2563eb',
  blueBg:    '#eff6ff',

  // Nav
  navPill:   '#0e2a20',
};

const OB_FONT = 'Roboto, "Segoe UI", system-ui, sans-serif';
const OB_MONO = '"Roboto Mono", "JetBrains Mono", ui-monospace, monospace';

// ─────────── Status bar ───────────
function ObStatusBar({ time = '1:25' }) {
  return (
    <div style={{
      height: 30, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 18px', color: OB.ink, fontFamily: OB_FONT, fontSize: 13, fontWeight: 500,
    }}>
      <span>{time}</span>
      <span style={{ display: 'flex', gap: 6, alignItems: 'center', color: OB.ink2, fontSize: 11 }}>
        <svg width="14" height="10" viewBox="0 0 14 10"><path d="M7 9.5L.6 3.1a9 9 0 0112.8 0L7 9.5z" fill={OB.ink}/></svg>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 2 }}>
          <span style={{
            width: 18, height: 9, border: `1.2px solid ${OB.ink}`, borderRadius: 2,
            display: 'inline-block', position: 'relative',
          }}>
            <span style={{ position: 'absolute', inset: 1, width: 6, background: OB.green, borderRadius: 1 }} />
          </span>
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>43</span>
        </span>
      </span>
    </div>
  );
}

// ─────────── Page title pattern (top-level tabs: no back) ───────────
function PageTitle({ eyebrow, title, right }) {
  return (
    <div style={{ padding: '14px 22px 6px' }}>
      {eyebrow && <div style={{
        fontFamily: OB_FONT, fontSize: 14, color: OB.ink2, fontWeight: 400,
      }}>{eyebrow}</div>}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        marginTop: eyebrow ? 2 : 0, gap: 12,
      }}>
        <h1 style={{
          margin: 0, fontFamily: OB_FONT, fontSize: 40, fontWeight: 800,
          letterSpacing: -1.2, color: OB.ink, lineHeight: 1.05,
        }}>{title}</h1>
        {right}
      </div>
    </div>
  );
}

// ─────────── Top app bar (used for Observer — has back arrow) ───────────
function TopAppBar({ title, onBack, trailing }) {
  return (
    <div style={{
      height: 56, display: 'flex', alignItems: 'center', gap: 4,
      padding: '0 6px 0 4px', background: OB.bg,
    }}>
      <button onClick={onBack} aria-label="Back" style={{
        width: 48, height: 48, borderRadius: 24, background: 'transparent', border: 'none',
        display: 'grid', placeItems: 'center', cursor: 'pointer', color: OB.ink,
      }}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
      </button>
      <div style={{
        flex: 1, fontFamily: OB_FONT, fontSize: 22, fontWeight: 700, color: OB.ink,
        letterSpacing: -.2,
      }}>{title}</div>
      {trailing}
    </div>
  );
}

// ─────────── Icon button ───────────
function IconButton({ icon, label, onClick, badge }) {
  return (
    <button onClick={onClick} aria-label={label} style={{
      position: 'relative',
      width: 48, height: 48, borderRadius: 24, background: 'transparent', border: 'none',
      display: 'grid', placeItems: 'center', cursor: 'pointer', color: OB.ink,
    }}>
      {icon}
      {badge && <span style={{
        position: 'absolute', top: 10, right: 10, width: 8, height: 8, borderRadius: 4,
        background: OB.amber, border: `1.5px solid ${OB.bg}`,
      }} />}
    </button>
  );
}

// ─────────── Status chip (used for capture + auto-scroll) ───────────
// Both icon + label — never color-only.
//
// Two visual modes communicate interactivity at a glance:
//   - interactive=true  → solid white CARD + colored border + FILLED icon tile.
//                         Reads as a button. Used for Capture (tap to toggle).
//   - interactive=false → transparent BG, NO border, OUTLINED icon tile, ink2 label.
//                         Reads as a status readout. Used for Auto-scroll (info only;
//                         toggled by tapping the feed list area).
function StatusChip({ tone = 'neutral', icon, label, value, onClick, interactive = true }) {
  const tones = {
    running: { accent: OB.green,     text: OB.greenDark },
    paused:  { accent: OB.amber,     text: OB.amberDark, hair: OB.amberHair, bg: OB.amberBg },
    neutral: { accent: OB.ink3,      text: OB.ink2,      hair: OB.hair },
  };
  const t = tones[tone] || tones.neutral;

  // Interactive: white card + colored border + filled tile (button-like)
  if (interactive) {
    return (
      <button onClick={onClick} style={{
        flex: 1, height: 56, padding: '0 14px', borderRadius: 16,
        background: tone === 'paused' ? OB.amberBg : '#fff',
        border: `1.5px solid ${tone === 'paused' ? OB.amberHair : t.accent}`,
        display: 'flex', alignItems: 'center', gap: 10,
        cursor: onClick ? 'pointer' : 'default',
        fontFamily: OB_FONT, textAlign: 'left',
      }}>
        <span style={{
          width: 28, height: 28, borderRadius: 14, background: t.accent, color: '#fff',
          display: 'grid', placeItems: 'center', flexShrink: 0,
        }}>{icon}</span>
        <span style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          <span style={{ fontSize: 11, color: OB.ink2, lineHeight: 1.1 }}>{label}</span>
          <span style={{
            fontSize: 14, fontWeight: 600, color: t.text, letterSpacing: -.1, lineHeight: 1.2,
            marginTop: 2,
          }}>{value}</span>
        </span>
      </button>
    );
  }

  // Display-only readout: transparent surface, no border, outlined icon tile.
  return (
    <div style={{
      flex: 1, height: 56, padding: '0 14px 0 4px', borderRadius: 16,
      background: 'transparent', border: 'none',
      display: 'flex', alignItems: 'center', gap: 10, cursor: 'default',
      fontFamily: OB_FONT, textAlign: 'left',
    }}>
      <span style={{
        width: 28, height: 28, borderRadius: 14,
        background: 'transparent', border: `1.5px solid ${t.accent}`,
        color: t.accent, display: 'grid', placeItems: 'center', flexShrink: 0,
      }}>{icon}</span>
      <span style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <span style={{
          fontSize: 11, color: OB.ink2, lineHeight: 1.1,
          textTransform: 'uppercase', letterSpacing: 1, fontWeight: 500,
        }}>{label}</span>
        <span style={{
          fontSize: 14, fontWeight: 500, color: t.text, letterSpacing: -.1, lineHeight: 1.2,
          marginTop: 2,
        }}>{value}</span>
      </span>
    </div>
  );
}

// ─────────── Service banner (top of Observer feed) ───────────
function ServiceBanner({ enabled, onOpenSettings }) {
  if (enabled) {
    return (
      <div style={{
        margin: '4px 18px 0', padding: '10px 14px', borderRadius: 14,
        background: '#fff', border: `1.5px solid ${OB.hair}`,
        display: 'flex', alignItems: 'center', gap: 12, fontFamily: OB_FONT,
      }}>
        <span style={{
          width: 26, height: 26, borderRadius: 13, background: OB.greenBg, color: OB.greenDark,
          display: 'grid', placeItems: 'center', flexShrink: 0,
        }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
            <path d="M5 13l4 4L19 7" />
          </svg>
        </span>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <span style={{ fontSize: 11, color: OB.ink2, lineHeight: 1.1 }}>Service</span>
          <span style={{ fontSize: 14, fontWeight: 600, color: OB.greenDark, lineHeight: 1.2, marginTop: 2 }}>Enabled</span>
        </div>
      </div>
    );
  }
  return (
    <div style={{
      margin: '4px 18px 0', padding: '12px 14px', borderRadius: 14,
      background: OB.redBg, border: `1.5px solid ${OB.redHair}`,
      display: 'flex', alignItems: 'center', gap: 12, fontFamily: OB_FONT,
    }}>
      <span style={{
        width: 26, height: 26, borderRadius: 13, background: OB.red, color: '#fff',
        display: 'grid', placeItems: 'center', flexShrink: 0,
      }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round">
          <path d="M12 8v5" /><circle cx="12" cy="16.5" r="1.2" fill="currentColor" stroke="none"/>
        </svg>
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 11, color: OB.redDark, opacity: .85, lineHeight: 1.1 }}>Service</div>
        <div style={{ fontSize: 14, fontWeight: 700, color: OB.redDark, lineHeight: 1.2, marginTop: 2 }}>Disabled</div>
        <div style={{ fontSize: 12, color: OB.redDark, opacity: .8, marginTop: 4, lineHeight: 1.35 }}>
          Enable the accessibility service to start receiving events.
        </div>
      </div>
      <button onClick={onOpenSettings} style={{
        height: 36, padding: '0 12px', borderRadius: 18, background: OB.redDark, color: '#fff',
        border: 'none', fontFamily: OB_FONT, fontSize: 12.5, fontWeight: 600, cursor: 'pointer',
        flexShrink: 0,
      }}>Open settings</button>
    </div>
  );
}

// ─────────── 4-tab bottom navigation (Session/List/Track/Settings) ───────────
function BottomNav({ active = 'settings' }) {
  const Item = ({ id, label, icon }) => {
    const on = id === active;
    return (
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
      }}>
        <div style={{
          padding: on ? '8px 22px' : '8px 14px', borderRadius: 22,
          background: on ? OB.navPill : 'transparent',
          color: on ? OB.green : OB.ink2, display: 'grid', placeItems: 'center',
        }}>{icon}</div>
        <span style={{
          fontFamily: OB_FONT, fontSize: 11, fontWeight: on ? 600 : 500,
          color: on ? OB.ink : OB.ink2,
        }}>{label}</span>
      </div>
    );
  };
  const sw = 1.9;
  return (
    <div style={{
      padding: '10px 6px 16px', background: OB.bg, display: 'flex',
      borderTop: `1px solid ${OB.hair}`,
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

// ─────────── Phone shell (rounded device-style frame) ───────────
function PhoneShell({ children, w = 380, h = 800 }) {
  return (
    <div style={{
      width: w, height: h, background: OB.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: OB_FONT,
    }}>{children}</div>
  );
}

Object.assign(window, {
  OB, OB_FONT, OB_MONO,
  ObStatusBar, PageTitle, TopAppBar, IconButton,
  StatusChip, ServiceBanner, BottomNav, PhoneShell,
});
