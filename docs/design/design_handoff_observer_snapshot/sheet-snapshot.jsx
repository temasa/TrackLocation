// Snapshot viewer — modal bottom sheet for viewing a captured window snapshot.
// Triggered by tapping "View window content" on any event card in the Observer Feed.
//
// Sheet height: ~88% (Material 3 bottom sheet, large).
// Header        : drag handle · title · subtitle (pkg · activity · lastSeenAt) · close (X)
// Meta strip    : 4 inline chips — Event type, First seen, Last seen, Repeat (if >1)
// Mode toggle   : segmented [Formatted | Raw JSON]   (+ Copy on the right)
// Content area  : scrollable. In Formatted mode: indented tree of cls / text / contentDesc.
//                 In Raw JSON mode: pretty-printed JSON with subtle line numbers.
// States covered: ok · parse-error · empty (no readable text)
//                 Missing-snapshot is handled at the card level (link hidden), not here.

// ─────────── Sample snapshot data ───────────
// Modelled on AccessibilityNodeInfo trees the Observer captures.
const SAMPLE_SNAPSHOT = {
  cls: 'android.widget.FrameLayout',
  id:  'com.google.android.apps.maps:id/main_container',
  children: [
    { cls: 'androidx.appcompat.widget.Toolbar', children: [
      { cls: 'android.widget.ImageButton', desc: 'Open navigation drawer' },
      { cls: 'android.widget.TextView',    text: 'Maps' },
      { cls: 'android.widget.ImageView',   desc: 'Search' },
    ]},
    { cls: 'android.widget.LinearLayout', children: [
      { cls: 'android.widget.TextView',  text: '475 Brannan St' },
      { cls: 'android.widget.TextView',  text: 'San Francisco, CA' },
      { cls: 'android.widget.TextView',  text: '12 min drive · 3.4 mi' },
      { cls: 'android.widget.Button',    text: 'Start',   desc: 'Start navigation' },
      { cls: 'android.widget.Button',    text: 'Save',    desc: 'Save to favorites' },
      { cls: 'android.widget.Button',    text: 'Share',   desc: 'Share location' },
    ]},
    { cls: 'androidx.recyclerview.widget.RecyclerView', children: [
      { cls: 'android.widget.TextView', text: 'Nearby' },
      { cls: 'android.widget.TextView', text: 'Blue Bottle Coffee · 0.2 mi' },
      { cls: 'android.widget.TextView', text: 'Sightglass Coffee · 0.4 mi' },
      { cls: 'android.widget.TextView', text: 'Philz Coffee · 0.5 mi' },
    ]},
    { cls: 'android.widget.FrameLayout', desc: 'Bottom sheet handle' },
  ],
};

const SAMPLE_RAW = JSON.stringify(SAMPLE_SNAPSHOT, null, 2);

// ─────────── Header ───────────
function SnapshotHeader({ pkg, activity, lastSeen, onClose }) {
  return (
    <div style={{
      padding: '6px 0 0',
      background: T.card,
      borderTopLeftRadius: 22, borderTopRightRadius: 22,
    }}>
      {/* drag handle */}
      <div style={{ display: 'grid', placeItems: 'center', padding: '4px 0 8px' }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: T.hair }} />
      </div>

      {/* title row */}
      <div style={{
        padding: '0 16px 10px', display: 'flex', alignItems: 'flex-start', gap: 10,
      }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: 18, fontWeight: 700, color: T.ink, letterSpacing: -.3,
            lineHeight: 1.15,
          }}>Window content</div>
          <div style={{
            marginTop: 4,
            fontFamily: OBS.mono, fontSize: 11.5, color: T.ink2,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            letterSpacing: -.1, lineHeight: 1.3,
          }}>
            <span style={{ color: T.ink }}>{pkg}</span>
            {activity && <><span style={{ color: T.ink4 }}>  ·  </span>{activity}</>}
            <span style={{ color: T.ink4 }}>  ·  </span>{lastSeen}
          </div>
        </div>
        <button onClick={onClose} aria-label="Close" style={{
          width: 36, height: 36, borderRadius: 18, background: T.bg,
          border: `1px solid ${T.hair}`,
          display: 'grid', placeItems: 'center', cursor: 'pointer', flexShrink: 0,
        }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={T.ink} strokeWidth="2.2" strokeLinecap="round">
            <path d="M6 6l12 12M18 6L6 18"/>
          </svg>
        </button>
      </div>
    </div>
  );
}

// ─────────── Meta chip ───────────
function MetaChip({ label, value, mono, tone }) {
  const palette = {
    type:   { bg: 'rgba(34,197,94,.10)',  text: T.greenDark },
    repeat: { bg: 'rgba(217,119,6,.10)',  text: '#92400e' },
    plain:  { bg: T.bg,                   text: T.ink },
  }[tone || 'plain'];
  return (
    <div style={{
      padding: '6px 10px', background: palette.bg,
      border: `1px solid ${T.hair}`, borderRadius: 10,
      display: 'flex', flexDirection: 'column', minWidth: 0, flex: '1 1 auto',
    }}>
      <span style={{
        fontSize: 10, fontWeight: 600, color: T.ink3, letterSpacing: .6,
        textTransform: 'uppercase',
      }}>{label}</span>
      <span style={{
        marginTop: 2,
        fontFamily: mono ? OBS.mono : 'inherit',
        fontSize: 12, fontWeight: mono ? 600 : 600, color: palette.text,
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        letterSpacing: -.1, fontVariantNumeric: 'tabular-nums',
      }}>{value}</span>
    </div>
  );
}

function MetaStrip({ type, firstSeen, lastSeen, repeat }) {
  return (
    <div style={{
      padding: '0 16px 12px',
      display: 'grid',
      gridTemplateColumns: repeat ? '1.4fr 1fr 1fr .7fr' : '1.4fr 1fr 1fr',
      gap: 6,
    }}>
      <MetaChip label="Event"      value={type}     mono tone="type" />
      <MetaChip label="First seen" value={firstSeen} mono />
      <MetaChip label="Last seen"  value={lastSeen}  mono />
      {repeat && <MetaChip label="Repeat" value={`×${repeat}`} mono tone="repeat" />}
    </div>
  );
}

// ─────────── Mode toolbar (Formatted / Raw + Copy) ───────────
function ModeToolbar({ mode, onMode, onCopy, copied, disabled }) {
  return (
    <div style={{
      padding: '8px 16px 10px',
      borderTop: `1px solid ${T.hairSoft}`,
      display: 'flex', alignItems: 'center', gap: 8,
      background: T.card,
    }}>
      {/* segmented control */}
      <div style={{
        flex: 1, height: 32, padding: 2, borderRadius: 999,
        background: T.bg, border: `1px solid ${T.hair}`, display: 'flex',
      }}>
        {['Formatted', 'Raw JSON'].map(m => {
          const on = mode === m;
          return (
            <button key={m} onClick={() => onMode(m)} style={{
              flex: 1, border: 'none', padding: 0, borderRadius: 999,
              background: on ? T.ink : 'transparent',
              color: on ? '#fff' : T.ink2,
              fontFamily: 'Roboto, Inter, system-ui, sans-serif',
              fontSize: 12, fontWeight: 600, letterSpacing: .1, cursor: 'pointer',
              transition: 'background .12s, color .12s',
            }}>{m}</button>
          );
        })}
      </div>
      {/* copy button */}
      <button onClick={onCopy} disabled={disabled} style={{
        height: 32, padding: '0 12px 0 10px', borderRadius: 999,
        background: disabled ? T.bg : (copied ? T.green : '#fff'),
        border: `1.5px solid ${disabled ? T.hair : (copied ? T.green : T.ink)}`,
        color: disabled ? T.ink3 : (copied ? '#fff' : T.ink),
        fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        fontSize: 12, fontWeight: 600, cursor: disabled ? 'default' : 'pointer',
        display: 'inline-flex', alignItems: 'center', gap: 6,
        transition: 'all .12s',
      }}>
        {copied ? (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M5 13l4 4L19 7"/></svg>
        ) : (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V5a1 1 0 011-1h10"/>
          </svg>
        )}
        {copied ? 'Copied' : 'Copy'}
      </button>
    </div>
  );
}

// ─────────── Tree node (formatted view) ───────────
// Depth-indented. Each node shows: cls (mono, dim), text (highlighted), desc (italic muted).
function TreeNode({ node, depth = 0, last = false }) {
  const indent = depth * 14;
  const hasText = !!node.text;
  const hasDesc = !!node.desc;
  return (
    <>
      <div style={{
        display: 'flex', alignItems: 'flex-start', gap: 6,
        padding: '3px 0', paddingLeft: indent,
        fontFamily: OBS.mono, fontSize: 12, lineHeight: 1.45,
      }}>
        <span style={{ color: T.ink4, flexShrink: 0, userSelect: 'none' }}>
          {depth === 0 ? '▸' : (last ? '└' : '├')}
        </span>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div style={{ color: T.ink3, letterSpacing: -.1, wordBreak: 'break-all' }}>
            {node.cls.split('.').slice(-1)[0]}
            <span style={{ color: T.ink4 }}>  ({node.cls})</span>
          </div>
          {hasText && (
            <div style={{
              color: T.ink, fontWeight: 600, letterSpacing: -.1,
              paddingLeft: 14, marginTop: 1,
            }}>
              <span style={{ color: T.greenDark, fontWeight: 500 }}>text: </span>
              <span style={{
                background: 'rgba(34,197,94,.10)', padding: '0 4px', borderRadius: 3,
              }}>"{node.text}"</span>
            </div>
          )}
          {hasDesc && (
            <div style={{
              color: T.ink2, fontStyle: 'italic', letterSpacing: -.1,
              paddingLeft: 14, marginTop: 1,
            }}>
              <span style={{ color: T.ink3, fontStyle: 'normal' }}>desc: </span>
              "{node.desc}"
            </div>
          )}
        </div>
      </div>
      {node.children && node.children.map((c, i) => (
        <TreeNode key={i} node={c} depth={depth + 1}
          last={i === node.children.length - 1} />
      ))}
    </>
  );
}

// ─────────── Raw JSON view ───────────
function RawView({ raw }) {
  const lines = raw.split('\n');
  return (
    <div style={{
      fontFamily: OBS.mono, fontSize: 11.5, color: T.ink,
      lineHeight: 1.55, padding: '4px 0',
    }}>
      {lines.map((ln, i) => (
        <div key={i} style={{
          display: 'grid', gridTemplateColumns: '32px 1fr', gap: 8,
          padding: '0 16px',
        }}>
          <span style={{
            color: T.ink4, textAlign: 'right', userSelect: 'none',
            fontVariantNumeric: 'tabular-nums',
          }}>{i + 1}</span>
          <span style={{
            whiteSpace: 'pre', wordBreak: 'break-all', color: T.ink,
          }}>{ln || '\u00a0'}</span>
        </div>
      ))}
    </div>
  );
}

// ─────────── Error / empty-content states ───────────
function SnapshotMessage({ tone = 'error', title, body, hint }) {
  const palette = tone === 'error'
    ? { bg: OBS.errBg,  ring: 'rgba(220,38,38,.22)', accent: T.red,    iconBg: '#fef2f2' }
    : { bg: T.bg,       ring: T.hair,                accent: T.ink2,   iconBg: T.card };
  return (
    <div style={{
      margin: '14px 16px 16px',
      padding: 16, borderRadius: 14,
      background: palette.bg, border: `1px solid ${palette.ring}`,
      display: 'flex', gap: 12, alignItems: 'flex-start',
    }}>
      <div style={{
        width: 32, height: 32, borderRadius: 16, background: palette.iconBg,
        border: `1px solid ${palette.ring}`, display: 'grid', placeItems: 'center',
        flexShrink: 0,
      }}>
        {tone === 'error' ? (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={palette.accent} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="9"/><path d="M12 8v5"/><circle cx="12" cy="16.5" r=".8" fill={palette.accent}/>
          </svg>
        ) : (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={palette.accent} strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M8 14h4"/>
          </svg>
        )}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 14, fontWeight: 700, color: palette.accent, letterSpacing: -.1,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        }}>{title}</div>
        <div style={{
          marginTop: 4, fontSize: 12.5, color: T.ink2, lineHeight: 1.4,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif',
        }}>{body}</div>
        {hint && (
          <div style={{
            marginTop: 8, padding: '6px 8px', background: '#fff',
            border: `1px dashed ${T.hair}`, borderRadius: 8,
            fontFamily: OBS.mono, fontSize: 11, color: T.ink3,
          }}>{hint}</div>
        )}
      </div>
    </div>
  );
}

// ─────────── The sheet ───────────
// state: 'ok' | 'parse-error' | 'no-text'
function SnapshotSheet({
  state = 'ok',
  pkg = 'com.google.android.apps.maps',
  activity = 'MapActivity',
  type = 'WINDOW_CONTENT_CHANGED',
  firstSeen = '13:24:07.214',
  lastSeen  = '13:24:07.981',
  repeat = null,
  mode: initialMode = 'Formatted',
  copied: copiedInitial = false,
}) {
  const [mode, setMode] = React.useState(initialMode);
  const [copied, setCopied] = React.useState(copiedInitial);

  React.useEffect(() => {
    if (!copied) return;
    const t = setTimeout(() => setCopied(false), 1400);
    return () => clearTimeout(t);
  }, [copied]);

  const disabled = state === 'no-text' || (state === 'parse-error' && mode === 'Formatted');

  return (
    <>
      {/* scrim */}
      <div style={{
        position: 'absolute', inset: 0, background: 'rgba(10,12,10,.45)',
      }} />

      {/* sheet */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, top: '12%',
        background: T.card, borderTopLeftRadius: 22, borderTopRightRadius: 22,
        boxShadow: '0 -16px 40px -10px rgba(0,0,0,.18)',
        display: 'flex', flexDirection: 'column', overflow: 'hidden',
        fontFamily: 'Roboto, Inter, system-ui, sans-serif',
      }}>
        <SnapshotHeader
          pkg={pkg}
          activity={activity}
          lastSeen={lastSeen}
          onClose={() => {}}
        />

        <MetaStrip
          type={type}
          firstSeen={firstSeen}
          lastSeen={lastSeen}
          repeat={repeat}
        />

        <ModeToolbar
          mode={mode}
          onMode={setMode}
          onCopy={() => setCopied(true)}
          copied={copied}
          disabled={disabled}
        />

        {/* content */}
        <div style={{
          flex: 1, overflow: 'auto',
          background: state === 'ok' ? '#fbfcfa' : T.card,
          borderTop: `1px solid ${T.hairSoft}`,
        }}>
          {state === 'parse-error' && (
            <SnapshotMessage
              tone="error"
              title="Unable to render snapshot"
              body="The captured node tree could not be parsed. The raw payload is still available — switch to Raw JSON to copy it."
              hint="JsonSyntaxException: Unexpected end of input at line 247, column 14"
            />
          )}

          {state === 'no-text' && (
            <SnapshotMessage
              tone="neutral"
              title="No readable text found"
              body="The captured tree contains nodes but no text or content-description fields. This commonly happens for canvas-rendered surfaces or fully-icon UIs."
            />
          )}

          {state === 'ok' && mode === 'Formatted' && (
            <div style={{ padding: '10px 14px 24px' }}>
              <TreeNode node={SAMPLE_SNAPSHOT} depth={0} last />
            </div>
          )}

          {state === 'ok' && mode === 'Raw JSON' && (
            <div style={{ padding: '8px 0 24px' }}>
              <RawView raw={SAMPLE_RAW} />
            </div>
          )}

          {state === 'parse-error' && mode === 'Raw JSON' && (
            <div style={{ padding: '8px 0 24px' }}>
              <RawView raw={SAMPLE_RAW.slice(0, 280) + '\n  …truncated'} />
            </div>
          )}
        </div>
      </div>
    </>
  );
}

Object.assign(window, { SnapshotSheet, SAMPLE_SNAPSHOT });
