---
name: OBD_MANUAL_TEST_GUIDE
description: Manual testing guide for OBD Phase 1 with real ELM327 adapter
metadata:
  type: reference
  version: 1.0
  date: 2026-06-15
---

# OBD Phase 1 Manual Test Guide
## TrackLocation — ELM327 Bluetooth Classic Telemetry

**Status:** Ready for manual device testing with real OBD adapter  
**Hardware Required:** KONNWEI ELM327 (or compatible) Bluetooth adapter  
**Test Vehicle:** 1193cc gasoline, ISO 15765-4 CAN (no MAF/015E PID support)  
**Build Verified:** `16c9f4e` (Stability hardening) + all prior OBD commits  

---

## Pre-Test Checklist

- [ ] App installed on device (SM-G965F or similar, API 29+)
- [ ] ELM327 adapter fully charged / powered on
- [ ] ELM327 adapter bonded via Android Bluetooth Settings
- [ ] Vehicle ignition ON (engine running or ready-to-start)
- [ ] Location permission granted to app
- [ ] "Always-recording" session is OFF (to avoid conflicting with test)

---

## Test Sequence

### Phase 1: OBD Settings Screen & Connection Attempt

**Goal:** Verify UI state transitions and connection flow.

#### Steps:
1. Open TrackLocation app
2. Tap **Settings** (bottom nav, rightmost tab)
3. Scroll down to **TOOLS** section
4. Tap **OBD** row
5. Observe the OBD Settings screen:
   - Status card should show **"Idle — Service not enabled"**
   - Enable toggle should be **OFF**
   - No device selected yet
   - "Pair a new device" row visible

#### Expected Result:
✓ OBD Settings screen loads without crash  
✓ Status text reads "Idle"  
✓ Toggle is in OFF position  

---

### Phase 2: Device Selection & Connection

**Goal:** Verify device picker and connection state transitions.

#### Steps:
1. Tap the **Enable** toggle to turn it ON
2. System requests `BLUETOOTH_CONNECT` permission (API 31+)
   - **Grant permission** when prompted
3. A bonded device picker dialog appears
4. Select your ELM327 adapter from the list (e.g., "KONNWEI" or MAC `47:74:06:14:CD:B3`)
5. Dialog closes; device name + MAC appear in the saved device row
6. Status card updates

#### Expected Results:
✓ Permission dialog appears and can be granted  
✓ Bonded device picker shows available devices  
✓ Device selection saves (visible in "Saved device" row)  
✓ Status card transitions: "Idle" → "Connecting" (within 2–3 seconds)  

**If connection fails:**
- Status card shows **"Waiting — Connection failed: [error message]"**
- Reconnect button appears; tap to retry
- Check Logcat for detailed error:
  ```bash
  adb logcat -s "ObdPollingService:*" | grep -i "connect\|error"
  ```

---

### Phase 3: Live Connection & RPM/Speed Streaming

**Goal:** Verify raw AT I/O, PID polling, and live metric updates.

#### Prerequisites:
- Device is in "Connecting" or "Waiting" state
- Engine running on the vehicle
- If in "Waiting", tap **Reconnect** button

#### Steps:
1. Observe status card as connection attempts
2. When vehicle engine runs and adapter responds, status transitions to **"Connected"**
3. Status card displays:
   - **RPM** (e.g., "845 rpm" at idle, "2400 rpm" at 2000 RPM)
   - **SPEED** (e.g., "0 km/h" while parked, "45 km/h" on road)
   - **FUEL** source label (e.g., "SPEED_DENSITY", "UNAVAILABLE")

#### Expected Results:
✓ Status card shows "Connected" (not "Waiting")  
✓ RPM updates live every ~500 ms (configurable at "Poll rate" preference)  
✓ Speed updates in real time as vehicle moves  
✓ Fuel source correctly identified (SPEED_DENSITY on test vehicle)  
✓ No crash after 1+ minute of streaming  

**Typical Values (at idle, 1.2L gasoline, SPEED_DENSITY):**
- RPM: 800–900
- Fuel rate: ~1.0–1.2 L/h
- Estimated km/L: ~0.8–1.0 (at idle; meaningless since vehicle stationary)

**If metrics are not updating:**
- Check Logcat:
  ```bash
  adb logcat -s "ObdPollingService" | grep "rpm\|speed\|fuel"
  ```
- Verify vehicle exposes the PIDs:
  - `010C` (RPM) — should be present
  - `010D` (Speed) — should be present
  - `015E` or `0110` (Fuel) — may be unavailable (SPEED_DENSITY fallback)

---

### Phase 4: Session Gating & Always-Recording Integration

**Goal:** Verify that OBD samples are gated by session state.

#### Steps:
1. While OBD is "Connected", go to **Session** tab (first bottom-nav tab)
2. Observe the always-recording switch state
3. Toggle **always-recording ON**
4. Check that:
   - Session card appears showing an active session
   - OBD metrics remain streaming in the background
5. Toggle **always-recording OFF**
6. Session ends

#### Expected Results:
✓ Always-recording toggle controls session state  
✓ While session is ON: OBD samples are being written to database  
✓ While session is OFF: OBD samples are NOT written (gating works)  
✓ No interference between session and OBD state  

**Verification:**
- With session ON, tap OBD settings and return; RPM/speed still update
- With session OFF, OBD card may hide or show "Idle" status (depends on service state)

---

### Phase 5: km/L Calculation & Display

**Goal:** Verify instant and average km/L appear on Session/Trip screens.

#### Prerequisites:
- OBD is "Connected"
- Vehicle is moving
- Always-recording session is ON

#### Steps:
1. Start driving (speed > 3 km/h)
2. Go to **Session** screen
3. Observe **OBD Status Card** below always-recording card:
   - Shows "Connected"
   - **Instant km/L** displays (e.g., "5.2 km/L")
   - **Average km/L** shows (e.g., "4.8 km/L" after ~2 min of driving)
   - Fuel source chip visible (e.g., "SPEED_DENSITY")

#### Expected Results:
✓ Instant km/L appears while speed > 3 km/h  
✓ Instant km/L hides when speed drops to < 3 km/h or accuracy > 20 m  
✓ Average km/L updates as session accumulates  
✓ Values are physically realistic for the vehicle (1.2L engine: ~4–7 km/L)  
✓ No crashes after 5+ minutes of streaming  

**On Test Vehicle (1.2L gasoline, no direct fuel rate):**
- Expect **SPEED_DENSITY** fuel source
- km/L should be in the range **3–8** depending on load
- Idle (0 km/h): instant km/L hidden; average shows cumulative

**If km/L displays "—" or "0":**
- Check if fuel rate is "UNAVAILABLE" (both 015E and 0110 unsupported)
- Vehicle may not support any fuel-rate PID (expected for some models)
- Metrics still stream; only km/L is unavailable (not a regression)

---

### Phase 6: Trip Panel Integration (Slice 4)

**Goal:** Verify km/L appears on Track screen during active trip.

#### Prerequisites:
- OBD "Connected"
- Always-recording is ON
- Have a trip active (start a trip via Track screen)

#### Steps:
1. Go to **Track** screen
2. Tap **Start Trip** button
3. While trip is active, observe the **Trip Panel** (lower half):
   - Should show an OBD row with instant/avg km/L metrics
   - Metrics update live
4. Drive for 1–2 minutes
5. Tap **Stop Trip**

#### Expected Results:
✓ Trip panel shows OBD row  
✓ Instant km/L visible and updates (~500 ms cadence)  
✓ Average km/L computed from trip start  
✓ Trip distance and fuel consumed tracked  
✓ No crash after stop  

---

### Phase 7: Stability & Long-Run Test

**Goal:** Verify no memory leaks, crashes, or connection drops under sustained load.

#### Steps:
1. Keep OBD "Connected" for **≥ 5 minutes**
2. Drive on normal roads (mixed speeds)
3. Periodically check:
   - App doesn't freeze or lag
   - Metrics update without stutter
   - No crash banner or white-screen
4. Check memory usage:
   ```bash
   adb shell dumpsys meminfo com.kolee.tracklocation | grep TOTAL
   ```
   - Should remain **≤ 150 MB**

#### Expected Results:
✓ App remains responsive  
✓ No crashes or freezes  
✓ Memory stable (no runaway growth)  
✓ Metrics stream continuously  

---

## Troubleshooting & Log Analysis

### Connection fails immediately ("No response")

**Root Causes:**
- Adapter not bonded or powered off
- Bluetooth socket not opening (API level mismatch)
- Vehicle doesn't support ISO 15765-4 CAN

**Diagnostics:**
```bash
# View connection logs
adb logcat -s "ObdPollingService" | grep -i "connect\|socket\|rfcomm"

# Check Bluetooth adapter
adb shell dumpsys bluetooth_manager | grep -i "enabled\|bonded"
```

**Workaround:**
- Re-pair via Android Bluetooth Settings
- Tap **Reconnect** button in app
- Check vehicle OBD port for damage

---

### RPM/Speed not updating

**Root Causes:**
- Vehicle doesn't support PIDs 010C (RPM) or 010D (Speed)
- ELM327 not initialized properly
- Poll rate too high (> 5 Hz)

**Diagnostics:**
```bash
adb logcat -s "ObdPollingService" | grep -E "010C|010D|parseObdRpm|parseObdSpeed"
```

**Workaround:**
- Lower poll rate in OBD Settings → Preferences (try 2 Hz)
- Restart app and reconnect

---

### km/L shows "—" (unavailable)

**Expected for vehicles without fuel-rate PID:**
- Both `015E` (direct fuel rate) and `0110` (MAF) unsupported
- Test vehicle (1.2L) falls into this category initially
- Solution: Use SPEED_DENSITY fallback (already implemented)

**Check if fallback is active:**
```bash
adb logcat -s "ObdPollingService" | grep -i "speed_density\|fuel.*unavailable"
```

---

## Test Sign-Off

When manual testing is complete:

- [ ] Connection to real adapter successful
- [ ] RPM/speed streaming live
- [ ] km/L calculation working (or correctly shows unavailable)
- [ ] Session gating verified
- [ ] No crashes in 5+ minute run
- [ ] Memory stable (< 150 MB)

**Result:** ✓ OBD Phase 1 ready for production  
**Next Steps:** Observer Phase 2 truncation banner (design handoff pending)

---

## References

- **Commit with latest fixes:** `16c9f4e` (stability hardening)
- **OBD service code:** `feature/obd/service/ObdPollingService.kt`
- **OBD UI screens:** `screens/settings/obd/ObdSettingsScreen.kt`, `screens/sessions/SessionsScreen.kt`
- **Database schema:** `data/roomdb/ObdSampleEntity.kt`, migration 3→4
- **Architecture:** `docs/ARCHITECTURE.md` § OBD Domain Model
- **Prior sessions:** `docs/IMPLEMENTATION-PLAN.md` § Appendix B (full session history)
