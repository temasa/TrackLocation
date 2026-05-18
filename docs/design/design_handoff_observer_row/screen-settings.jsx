// Settings screen — new layout consistent with Session/List/Track.
// Section pattern: section header + grouped list rows in a white card.

function SettingsRow({ icon, label, supporting, value, last, onClick }) {
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 14, padding: '14px 16px',
      borderBottom: last ? 'none' : `1px solid ${OB.hairSoft}`, cursor: onClick ? 'pointer' : 'default',
    }}>
      <span style={{
        width: 36, height: 36, borderRadius: 10, background: OB.bg, color: OB.ink,
        display: 'grid', placeItems: 'center', flexShrink: 0,
      }}>{icon}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: OB_FONT, fontSize: 15, fontWeight: 600, color: OB.ink, letterSpacing: -.1 }}>
          {label}
        </div>
        {supporting && (
          <div style={{ fontFamily: OB_FONT, fontSize: 12.5, color: OB.ink2, marginTop: 2, lineHeight: 1.35 }}>
            {supporting}
          </div>
        )}
      </div>
      {value && (
        <span style={{ fontFamily: OB_FONT, fontSize: 13, color: OB.ink2, fontWeight: 500 }}>{value}</span>
      )}
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={OB.ink3} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 6l6 6-6 6" />
      </svg>
    </div>
  );
}

function SectionGroup({ title, children }) {
  return (
    <div style={{ margin: '18px 18px 0' }}>
      <div style={{
        fontFamily: OB_FONT, fontSize: 12, fontWeight: 600, color: OB.ink2,
        textTransform: 'uppercase', letterSpacing: 1.4, padding: '0 4px 8px',
      }}>{title}</div>
      <div style={{
        background: '#fff', border: `1.5px solid ${OB.hairSoft}`, borderRadius: 18, overflow: 'hidden',
      }}>{children}</div>
    </div>
  );
}

function ScreenSettings() {
  return (
    <PhoneShell>
      <ObStatusBar />
      <PageTitle title="Settings" />

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 16 }}>
        <SectionGroup title="General">
          <SettingsRow
            icon={
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <path d="M3 12a9 9 0 1018 0 9 9 0 10-18 0" /><path d="M12 7v5l3 2" />
              </svg>
            }
            label="Units"
            supporting="Metric (km, km/h)"
            value=""
          />
          <SettingsRow
            icon={
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="12" cy="12" r="9" /><path d="M12 3a14 14 0 010 18M3 12h18" />
              </svg>
            }
            label="Map style"
            supporting="System default"
          />
          <SettingsRow
            last
            icon={
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s7-7.5 7-13a7 7 0 10-14 0c0 5.5 7 13 7 13z" /><circle cx="12" cy="9" r="2.5" />
              </svg>
            }
            label="Location permission"
            supporting="Granted while in use"
          />
        </SectionGroup>

        <SectionGroup title="Tools">
          <SettingsRow
            icon={
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="12" cy="12" r="3" fill="currentColor" stroke="none" />
                <path d="M5.5 12a6.5 6.5 0 0113 0M3 12a9 9 0 0118 0" />
              </svg>
            }
            label="Observer"
            supporting="Inspect accessibility events captured by the service"
            last
          />
        </SectionGroup>

        <SectionGroup title="About">
          <SettingsRow
            icon={
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="12" cy="12" r="9" /><path d="M12 8v4M12 16h.01" />
              </svg>
            }
            label="About TrackLocation"
            supporting="Version 1.6.2"
            last
          />
        </SectionGroup>
      </div>

      <BottomNav active="settings" />
    </PhoneShell>
  );
}

window.ScreenSettings = ScreenSettings;
