// Track screen — translucent glass panel + brand-green CTA.
// Map shows through behind the panel so users keep their sense of place.

// ─────────── Faux map background (SVG) ───────────
// Stylized map: roads, blocks, water, park, route line, location pin.
function MapBackground() {
  return (
    <svg
      width="100%" height="100%" viewBox="0 0 380 800" preserveAspectRatio="xMidYMid slice"
      style={{ position: 'absolute', inset: 0, display: 'block' }}
    >
      <defs>
        <pattern id="blocks" x="0" y="0" width="60" height="60" patternUnits="userSpaceOnUse">
          <rect width="60" height="60" fill="#eef0e8" />
          <rect x="2" y="2" width="56" height="56" rx="2" fill="#f6f7f1" />
        </pattern>
      </defs>

      {/* Base */}
      <rect width="380" height="800" fill="#e9ece3" />
      <rect width="380" height="800" fill="url(#blocks)" />

      {/* Park */}
      <path d="M40,40 Q120,20 200,60 T380,80 L380,180 Q260,180 180,150 T40,170 Z" fill="#d6e8c8" opacity=".9" />
      <path d="M260,460 Q330,440 380,460 L380,560 Q320,580 270,560 Z" fill="#d6e8c8" opacity=".85" />

      {/* Water */}
      <path d="M0,540 Q90,520 180,560 T380,540 L380,640 Q260,660 160,640 T0,640 Z" fill="#cfe2ec" opacity=".75" />

      {/* Roads (light gray, layered) */}
      <g stroke="#ffffff" strokeWidth="14" strokeLinecap="round" fill="none" opacity=".85">
        <path d="M-20,260 Q120,240 240,280 T420,260" />
        <path d="M120,-20 Q140,220 100,400 T200,820" />
        <path d="M-20,500 Q160,490 260,510 T420,500" />
        <path d="M280,-20 Q260,300 320,520 T280,820" />
      </g>
      <g stroke="#dfe2d7" strokeWidth="14" strokeLinecap="round" fill="none">
        <path d="M-20,260 Q120,240 240,280 T420,260" />
        <path d="M120,-20 Q140,220 100,400 T200,820" />
        <path d="M-20,500 Q160,490 260,510 T420,500" />
        <path d="M280,-20 Q260,300 320,520 T280,820" />
      </g>

      {/* Minor roads (thin) */}
      <g stroke="#ffffff" strokeWidth="3" fill="none">
        <path d="M0,160 L380,170" />
        <path d="M0,360 L380,370" />
        <path d="M0,440 L380,455" />
        <path d="M60,0 L70,800" />
        <path d="M200,0 L210,800" />
        <path d="M340,0 L348,800" />
      </g>

      {/* Recorded route (the user's previous trip ghost) */}
      <path
        d="M40,420 Q90,400 130,420 T220,400 Q260,390 300,420 T380,440"
        fill="none" stroke="#22c55e" strokeWidth="4" strokeLinecap="round"
        strokeDasharray="0 0" opacity=".9"
      />

      {/* Location pin */}
      <g transform="translate(184, 386)">
        <ellipse cx="0" cy="22" rx="10" ry="3" fill="rgba(0,0,0,.18)" />
        <path
          d="M0,-20 a14,14 0 1,1 -0.001,0 Z M0,-20 C-8,-20 -14,-14 -14,-6 C-14,4 -2,18 0,22 C2,18 14,4 14,-6 C14,-14 8,-20 0,-20 Z"
          fill="#16a34a" stroke="#fff" strokeWidth="2.5"
        />
        <circle cx="0" cy="-6" r="4.5" fill="#fff" />
      </g>

      {/* District labels (subtle, design-system muted) */}
      <g fontFamily="Roboto, system-ui, sans-serif" fill="#7a8073" fontSize="11" fontWeight="500" opacity=".75">
        <text x="60" y="100" letterSpacing="2">CENTRAL PARK</text>
        <text x="250" y="240" letterSpacing="2">HIGHLAND</text>
        <text x="40" y="380">Riverside Ave</text>
        <text x="80" y="600" letterSpacing="2">WATERFRONT</text>
      </g>
    </svg>
  );
}

// ─────────── Map-style FAB cluster (right side, above panel) ───────────
function MapControls() {
  const Btn = ({ children, label }) => (
    <button aria-label={label} style={{
      width: 44, height: 44, borderRadius: 22, background: 'rgba(255,255,255,.92)',
      border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer',
      boxShadow: '0 4px 12px rgba(0,0,0,.12)', color: T.ink,
      backdropFilter: 'blur(10px)', WebkitBackdropFilter: 'blur(10px)',
    }}>{children}</button>
  );
  return (
    <div style={{
      position: 'absolute', right: 16, bottom: 240,
      display: 'flex', flexDirection: 'column', gap: 10, zIndex: 2,
    }}>
      <Btn label="Recenter">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
        </svg>
      </Btn>
      <Btn label="Layers">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round">
          <path d="M12 3l9 5-9 5-9-5 9-5z" />
          <path d="M3 13l9 5 9-5M3 18l9 5 9-5" />
        </svg>
      </Btn>
    </div>
  );
}

// ─────────── Translucent trip panel ───────────
// Glassmorphic: dark surface + backdrop blur + saturation boost.
function TripPanel({ state = 'ready' }) {
  const isReady   = state === 'ready';
  const isLive    = state === 'live';
  const isPaused  = state === 'paused';

  const eyebrow = isReady ? 'Ready for trip' : (isPaused ? 'Trip paused' : 'Trip in progress');
  const elapsed = isReady ? '00:00:00' : (isPaused ? '00:08:42' : '00:23:17');
  const distance = isReady ? '0.00' : (isPaused ? '1.42' : '3.84');
  const speed   = isReady ? '0.00' : (isPaused ? '0.00' : '11.7');

  const cta = isReady ? 'start' : (isPaused ? 'resume' : 'pause');

  return (
    <div style={{
      position: 'absolute', left: 14, right: 14, bottom: 14, zIndex: 3,
      padding: '12px 14px',
      borderRadius: 18,
      // Translucent dark surface with brand-green tint — more glass, less card
      background: 'rgba(20, 48, 38, 0.48)',
      border: `1px solid rgba(255,255,255,.12)`,
      boxShadow: '0 14px 36px rgba(10,20,16,.28)',
      // Heavier blur to keep text legible at lower opacity
      backdropFilter: 'blur(28px) saturate(180%)',
      WebkitBackdropFilter: 'blur(28px) saturate(180%)',
      color: '#fff',
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      {/* Status eyebrow + live dot */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
        <span style={{
          width: 7, height: 7, borderRadius: 4,
          background: isLive ? T.green : (isPaused ? '#f5a524' : 'rgba(255,255,255,.45)'),
          boxShadow: isLive ? `0 0 0 3px rgba(34,197,94,.22)` : 'none',
          animation: isLive ? 'sessPulse 1.6s ease-in-out infinite' : 'none',
        }} />
        <span style={{
          fontFamily: 'JetBrains Mono, Roboto Mono, ui-monospace, monospace',
          fontSize: 10, fontWeight: 600, letterSpacing: 1.2, textTransform: 'uppercase',
          color: 'rgba(255,255,255,.78)',
        }}>{eyebrow}</span>
      </div>

      {/* Elapsed + CTA row */}
      <div style={{
        marginTop: 4, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
      }}>
        <div style={{
          fontFamily: 'JetBrains Mono, Roboto Mono, ui-monospace, monospace',
          fontSize: 30, fontWeight: 600, letterSpacing: -.8, color: '#fff', lineHeight: 1,
          fontVariantNumeric: 'tabular-nums',
        }}>{elapsed}</div>

        <TripCta state={cta} />
      </div>

      {/* Stats row — distance + speed, with hairline above for rhythm */}
      <div style={{
        marginTop: 10, paddingTop: 9,
        borderTop: '1px solid rgba(255,255,255,.12)',
        display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, alignItems: 'baseline',
      }}>
        <PanelMetric icon={
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s7-7.5 7-13a7 7 0 10-14 0c0 5.5 7 13 7 13z" /><circle cx="12" cy="9" r="2.4" />
          </svg>
        } value={distance} unit="km" />
        <PanelMetric icon={
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" />
          </svg>
        } value={speed} unit="km/hr" align="end" />
      </div>
    </div>
  );
}

function PanelMetric({ icon, value, unit, align = 'start' }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: align === 'end' ? 'flex-end' : 'flex-start' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: 'rgba(255,255,255,.78)' }}>
        {icon}
        <span style={{
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
          fontSize: 10, letterSpacing: .4, textTransform: 'uppercase',
        }}>{unit}</span>
      </div>
      <div style={{
        marginTop: 2, fontFamily: 'JetBrains Mono, Roboto Mono, ui-monospace, monospace',
        fontSize: 16, fontWeight: 600, letterSpacing: -.3, lineHeight: 1, color: '#fff',
        fontVariantNumeric: 'tabular-nums',
      }}>{value}</div>
    </div>
  );
}

// CTA — circular brand-green button. Variants: start, pause, resume.
function TripCta({ state = 'start' }) {
  const icon = state === 'start' ? (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="#0a2218">
      <path d="M8 5l12 7-12 7V5z" />
    </svg>
  ) : state === 'pause' ? (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="#0a2218">
      <rect x="6" y="5" width="4.5" height="14" rx="1.2" />
      <rect x="13.5" y="5" width="4.5" height="14" rx="1.2" />
    </svg>
  ) : (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="#0a2218">
      <path d="M8 5l12 7-12 7V5z" />
    </svg>
  );
  return (
    <button aria-label={state === 'start' ? 'Start trip' : state === 'pause' ? 'Pause trip' : 'Resume trip'} style={{
      width: 48, height: 48, borderRadius: 24,
      background: T.green,
      border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer', flexShrink: 0,
      boxShadow: '0 8px 18px -6px rgba(34,197,94,.55), inset 0 -2px 0 rgba(0,0,0,.12)',
    }}>{icon}</button>
  );
}

// ─────────── Screen ───────────
function TrackScreen({ panelState = 'ready' }) {
  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />

      {/* Map (fills body) */}
      <div style={{ position: 'relative', flex: 1, overflow: 'hidden' }}>
        <MapBackground />
        <MapControls />
        <TripPanel state={panelState} />
      </div>

      <BottomNav active="track" />

      <style>{`@keyframes sessPulse { 0%,100% { opacity: 1 } 50% { opacity: .55 } }`}</style>
    </div>
  );
}

window.TrackScreen = TrackScreen;
