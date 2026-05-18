// Allowlist bottom sheet overlay — modal sheet on top of Observer Feed.
// Compact: ~55% of screen height, feed visible behind under scrim.
//
// Variants:
//   'empty'        — no rules yet, helper text shown
//   'rules-clean'  — rules exist, all applied (no draft indicator)
//   'rules-draft'  — rules exist with unapplied draft edits (indicator + Apply enabled)

function RuleRow({ rule, last }) {
  const { kind, pattern, enabled, draft } = rule;
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px',
      borderBottom: last ? 'none' : `1px solid ${OB.hairSoft}`,
      background: draft ? '#fffdf7' : '#fff', position: 'relative',
    }}>
      {/* Match type segmented toggle */}
      <div style={{
        display: 'flex', background: OB.bg, borderRadius: 8, padding: 2,
        border: `1px solid ${OB.hair}`,
      }}>
        {['Exact', 'Regex'].map((k) => {
          const on = k === kind;
          return (
            <span key={k} style={{
              padding: '4px 8px', borderRadius: 6,
              background: on ? '#fff' : 'transparent',
              boxShadow: on ? '0 1px 2px rgba(0,0,0,.06)' : 'none',
              fontFamily: OB_FONT, fontSize: 11, fontWeight: on ? 600 : 500,
              color: on ? OB.ink : OB.ink2,
            }}>{k}</span>
          );
        })}
      </div>

      {/* Pattern input */}
      <div style={{
        flex: 1, minWidth: 0, height: 32, padding: '0 10px', borderRadius: 8,
        background: '#fff', border: `1px solid ${enabled ? OB.hair : OB.hairSoft}`,
        display: 'flex', alignItems: 'center',
      }}>
        <span style={{
          fontFamily: OB_MONO, fontSize: 12, color: enabled ? OB.ink : OB.ink3,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{pattern}</span>
      </div>

      {/* Enable/disable */}
      <span style={{
        width: 30, height: 18, borderRadius: 9,
        background: enabled ? OB.green : '#d6d8d4', position: 'relative', flexShrink: 0,
      }}>
        <span style={{
          position: 'absolute', top: 2, left: enabled ? 14 : 2, width: 14, height: 14,
          borderRadius: 7, background: '#fff', boxShadow: '0 1px 2px rgba(0,0,0,.25)',
        }} />
      </span>

      {/* Delete */}
      <button style={{
        width: 28, height: 28, borderRadius: 14, background: 'transparent',
        border: 'none', color: OB.ink2, display: 'grid', placeItems: 'center', cursor: 'pointer',
      }} aria-label="Delete rule">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" />
        </svg>
      </button>

      {/* Draft dot indicator on row */}
      {draft && (
        <span style={{
          position: 'absolute', left: 4, top: '50%', transform: 'translateY(-50%)',
          width: 4, height: 22, borderRadius: 2, background: OB.amber,
        }} />
      )}
    </div>
  );
}

function AllowlistSheet({ variant = 'empty' }) {
  const empty   = variant === 'empty';
  const draft   = variant === 'rules-draft';
  const rules = empty ? [] : [
    { kind: 'Exact', pattern: 'com.android.chrome',            enabled: true,  draft: false },
    { kind: 'Exact', pattern: 'com.android.systemui',          enabled: true,  draft: false },
    { kind: 'Regex', pattern: 'com\\.example\\.',              enabled: draft, draft: draft },
    { kind: 'Regex', pattern: 'google\\.android\\.(gm|maps)',  enabled: false, draft: false },
    { kind: 'Exact', pattern: 'com.whatsapp',                  enabled: true,  draft: draft },
  ];

  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0, top: 0,
      display: 'flex', flexDirection: 'column', justifyContent: 'flex-end',
      pointerEvents: 'none',
    }}>
      {/* Scrim */}
      <div style={{
        position: 'absolute', inset: 0, background: 'rgba(10,10,10,.32)', pointerEvents: 'auto',
      }} />

      {/* Sheet */}
      <div style={{
        position: 'relative', pointerEvents: 'auto',
        background: '#fff', borderTopLeftRadius: 22, borderTopRightRadius: 22,
        boxShadow: '0 -10px 30px rgba(0,0,0,.18)', maxHeight: '62%',
        display: 'flex', flexDirection: 'column', fontFamily: OB_FONT,
      }}>
        {/* Grabber */}
        <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 6px' }}>
          <span style={{ width: 36, height: 4, borderRadius: 2, background: OB.hair }} />
        </div>

        {/* Header */}
        <div style={{
          padding: '4px 18px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div>
            <div style={{ fontSize: 17, fontWeight: 700, color: OB.ink, letterSpacing: -.2 }}>Allowlist</div>
            <div style={{ fontSize: 12, color: OB.ink2, marginTop: 2 }}>
              Match by package name. Applies to future capture only.
            </div>
          </div>
          <button aria-label="Add rule" style={{
            width: 36, height: 36, borderRadius: 18, background: OB.ink, color: '#fff',
            border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer',
          }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
              <path d="M12 5v14M5 12h14" />
            </svg>
          </button>
        </div>

        {/* Draft banner */}
        {draft && (
          <div style={{
            margin: '0 18px 10px', padding: '8px 12px', borderRadius: 10,
            background: OB.amberBg, border: `1px solid ${OB.amberHair}`,
            display: 'flex', alignItems: 'center', gap: 8,
            fontSize: 12.5, color: OB.amberDark, fontWeight: 600,
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
              <path d="M12 8v5" /><circle cx="12" cy="16.5" r="1.2" fill="currentColor" stroke="none"/>
            </svg>
            Draft changes not applied
          </div>
        )}

        {/* List */}
        <div style={{
          flex: 1, overflowY: 'auto', margin: '0 18px',
          border: `1px solid ${OB.hairSoft}`, borderRadius: 14, background: '#fff',
        }}>
          {empty ? (
            <div style={{ padding: '24px 18px', textAlign: 'center' }}>
              <div style={{ fontSize: 13.5, color: OB.ink, fontWeight: 600, marginBottom: 4 }}>
                No rules yet
              </div>
              <div style={{ fontSize: 12.5, color: OB.ink2, lineHeight: 1.45 }}>
                Capturing all packages. Add rules to reduce noise.
              </div>
            </div>
          ) : (
            rules.map((r, i) => <RuleRow key={i} rule={r} last={i === rules.length - 1} />)
          )}
        </div>

        {/* Footer */}
        <div style={{
          padding: '12px 18px 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          gap: 12,
        }}>
          <button style={{
            height: 44, padding: '0 18px', borderRadius: 22, background: 'transparent',
            border: `1.5px solid ${OB.hair}`, color: OB.ink, fontWeight: 500, fontSize: 14,
            fontFamily: OB_FONT, cursor: 'pointer',
          }}>Close</button>
          <button disabled={!draft} style={{
            height: 44, padding: '0 22px', borderRadius: 22,
            background: draft ? OB.ink : '#e1e3df',
            color: draft ? '#fff' : OB.ink3,
            border: 'none', fontWeight: 600, fontSize: 14,
            fontFamily: OB_FONT, cursor: draft ? 'pointer' : 'not-allowed',
          }}>Apply</button>
        </div>
      </div>
    </div>
  );
}

// ─────────── Composite: Observer feed + Allowlist sheet on top ───────────
function ScreenObserverWithAllowlist({ sheetVariant = 'rules-draft' }) {
  return (
    <div style={{ position: 'relative', width: 380, height: 800 }}>
      <ScreenObserver variant="running" showAllowlistBadge={sheetVariant === 'rules-draft'} />
      <AllowlistSheet variant={sheetVariant} />
    </div>
  );
}

window.AllowlistSheet = AllowlistSheet;
window.ScreenObserverWithAllowlist = ScreenObserverWithAllowlist;
