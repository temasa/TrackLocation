// List screen — unchanged content, but with the new 4-tab bottom nav.
// Export pill stays (per your correction). Built from the current screenshot.

function TripRow({ index, date, km, duration, kmh }) {
  return (
    <div style={{
      margin: '12px 22px 0', padding: '18px 18px 18px 20px',
      background: T.card, borderRadius: 20, border: `1.5px solid ${T.hairSoft}`,
    }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
        <div style={{ width: 14, paddingTop: 5, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <span style={{ width: 12, height: 12, borderRadius: 6, border: `2.5px solid ${T.green}`, background: '#fff' }} />
          <span style={{ width: 2.5, flex: 1, minHeight: 32, background: T.green, marginTop: 2, borderRadius: 2 }} />
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <div style={{ fontSize: 19, fontWeight: 700, color: T.ink, letterSpacing: -.2 }}>
              Track #{index}
            </div>
            <div style={{ fontSize: 13, color: T.ink2, fontVariantNumeric: 'tabular-nums' }}>
              {date}
            </div>
          </div>

          <div style={{ height: 1, background: T.hairSoft, margin: '14px 0 14px -34px' }} />

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 4 }}>
            <Metric value={`${km} km`} unit="km" />
            <Metric value={duration} unit="duration" />
            <Metric value={`${kmh} km/h`} unit="avg speed" />
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ value, unit, color }) {
  return (
    <div style={{
      flex: 1, padding: '18px 16px', background: T.card,
      border: `1.5px solid ${T.hairSoft}`, borderRadius: 18,
    }}>
      <div style={{
        fontSize: 30, fontWeight: 800, letterSpacing: -.6, lineHeight: 1, color,
        fontVariantNumeric: 'tabular-nums',
      }}>{value}</div>
      <div style={{ fontSize: 14, color: T.ink2, marginTop: 8 }}>{unit}</div>
    </div>
  );
}

function ListUpdatedNav() {
  return (
    <div style={{
      width: 380, height: 800, background: T.bg, position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', borderRadius: 26,
      fontFamily: 'Roboto, Inter, system-ui, sans-serif',
    }}>
      <StatusBar />
      <PageHeader
        eyebrow="Trip Tracker"
        title="Trips"
        right={<PillButton>Export</PillButton>}
      />

      {/* Current-trip hero card (kept from existing design) */}
      <div style={{
        margin: '14px 22px 0', padding: '18px 20px', background: T.hero,
        borderRadius: 22, color: '#fff', display: 'flex', alignItems: 'center', gap: 14,
      }}>
        <div style={{
          width: 52, height: 52, borderRadius: 26, background: T.heroDim,
          display: 'grid', placeItems: 'center', flexShrink: 0,
        }}>
          <span style={{ width: 14, height: 14, borderRadius: 7, background: T.green }} />
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 13, color: 'rgba(255,255,255,.65)' }}>Current trip</div>
          <div style={{ fontSize: 19, fontWeight: 700, color: '#fff', marginTop: 2, letterSpacing: -.2 }}>
            Ready to record
          </div>
        </div>
        <button style={{
          height: 44, padding: '0 22px', borderRadius: 22, background: T.green,
          border: 'none', color: '#fff', fontWeight: 600, fontSize: 15,
          fontFamily: 'Roboto, Inter, system-ui, sans-serif', cursor: 'pointer',
        }}>Start</button>
      </div>

      {/* Stats row */}
      <div style={{ display: 'flex', gap: 10, padding: '14px 22px 0' }}>
        <StatCard value="5"    unit="Trips"    color={T.greenDark} />
        <StatCard value="52.4" unit="Distance" color={T.blue} />
        <StatCard value="3.3"  unit="Hours"    color={T.amber} />
      </div>

      {/* Recent trips */}
      <div style={{
        margin: '18px 22px 0', display: 'flex', justifyContent: 'space-between',
        alignItems: 'baseline',
      }}>
        <div style={{ fontSize: 17, fontWeight: 700, color: T.ink, letterSpacing: -.2 }}>Recent trips</div>
        <div style={{ fontSize: 13, color: T.ink2 }}>Newest first</div>
      </div>

      <div style={{ flex: 1, overflow: 'hidden', paddingBottom: 8 }}>
        <TripRow index={4} date="Wed, 07:15" km="5.47" duration="00:49:21" kmh="6.7" />
        <TripRow index={5} date="Wed, 07:29" km="0.72" duration="00:01:10" kmh="36.8" />
      </div>

      <BottomNav active="list" />
    </div>
  );
}

window.ListUpdatedNav = ListUpdatedNav;
