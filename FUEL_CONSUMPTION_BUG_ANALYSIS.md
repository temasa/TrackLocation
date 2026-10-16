# High Fuel Consumption Bug Analysis
**Root Cause Analysis from Code Review**

---

## 🔴 **Critical Issues Found (No Input Validation)**

### **Issue 1: MAF-Derived Fuel Rate Has No Upper Bound**

**File:** `ObdPollingService.kt`, Line 606-615

```kotlin
if (response.contains("41 10")) {  // PID 0110 - MAF
    val a = parts[idx + 1].toInt(16)
    val b = parts[idx + 2].toInt(16)
    val maf = (a * 256 + b) / 100.0              // ← Range: 0 to 655.35 g/s
    val fuelRate = maf * 3600.0 / (14.7 * 750.0) // ← NO BOUNDS CHECK!
    return Pair(fuelRate, "MAF_DERIVED")
}
```

**Problem:**
- Max possible MAF: `(255 × 256 + 255) / 100 = 655.35 g/s`
- Resulting fuel rate: `655.35 × 3600 / (14.7 × 750) = **213 L/h**` ❌

**Scenario That Causes Issue:**
1. OBD adapter returns corrupted CAN frame: `41 10 FF FF`
2. Parser reads: a=255, b=255
3. Calculated fuelRate = 213 L/h (instead of realistic 5-15 L/h)
4. One polling cycle accumulates: `213 × 0.5s / 3600 = 0.0296 L`
5. If this corrupted sample appears 10 times → **0.3L from garbage data**
6. Over a session, corrupted samples inflate total fuel by 20-30%

**Impact:**
- Distance: 50 km
- Expected fuel: 4 L (12.5 km/L)
- Actual accumulated: 5.2 L (corrupted sample spike)
- **Reported: 50 / 5.2 = 9.6 km/L** (looks good, but inflated!)

---

### **Issue 2: Speed-Density Calculation Vulnerable to Temperature Sensor Glitches**

**File:** `ObdPollingService.kt`, Line 513-535

```kotlin
if (sdCycle % 8 == 0) {
    extractDataBytes(writeATCommand(socket, "010F"), 0x41, 0x0F)?.getOrNull(0)?.let { a ->
        cachedIatK = (a - 40) + 273.15  // ← IAT in Kelvin
    }
    // ... lambda also cached ...
}

val mafGs = (rpm * mapKpa * ve * displacementL * molarMassAir) / (120.0 * r * cachedIatK)
                                                                              ↑ NO VALIDATION!
```

**Problem:**
- Normal IAT: 25°C = 298K
- **If sensor glitch:** reads 0 → cachedIatK = (0-40)+273.15 = 233K (very cold)
- **Or critical failure:** reads 255 → cachedIatK = (255-40)+273.15 = 488K (extremely hot, unrealistic)

**Scenario:**
1. IAT sensor returns 0 (sensor error/disconnected)
2. cachedIatK = 233K (instead of 298K)
3. mafGs multiplied by factor: 298/233 = **1.28x higher**
4. Fuel rate also **1.28x higher**
5. Over 8-cycle refresh window (~4 seconds at 2Hz):
   - Expected fuel: 0.02L
   - Actual with cold temp: 0.026L (28% inflated!)

**Extreme case:**
- Temp sensor value = 240 (almost max)
- cachedIatK = 473K (unrealistic!)
- mafGs multiplied by 298/473 = **0.63x** (fuel too LOW)
- Then sensor recovers to 25 → cachedIatK = 298K
- Next cycle: fuel suddenly **1.6x higher** relative to previous
- Accumulated mismatches add up

**No bounds check:** Code accepts any temp from 0-255°C without validation

---

### **Issue 3: Session Distance Sync Issue - Fuel Persisted, Distance Stale**

**File:** `ObdPollingService.kt`, Line 440-451

```kotlin
val sessionDao = (applicationContext as com.kolee.tracklocation.TrackApp).sessionDao
val activeSession = sessionDao.getActiveSession()  // ← Read from DB
val sid = activeSession?.id
if (sid != null && fuelIncrementL > 0.0) {
    sessionDao.addObdAccumulator(sid, fuelIncrementL)  // ← WRITE fuel
}
// But then read distance:
val persistedDistKm = (activeSession?.distanceMeters ?: 0.0) / 1000.0
                      ↑ OLD reading, predates the fuel write!
```

**Problem:**
- OBD reads active session and writes fuel increment
- But `distanceMeters` is updated by TrackingService, not OBD
- If TrackingService is slow/stalled, distance lags behind fuel accumulation

**Scenario: Recording Without Tracking**
1. User starts always-recording (isActive = true)
2. Drives 5 minutes WITHOUT starting a trip (isTracking = false)
3. Distance not accumulated during this phase (only trips count)
4. Fuel IS accumulated: 1.5 L
5. Trip starts after 5 minutes
6. Distance accumulates now: 45 km
7. **Session calculates:** 45 km / (1.5L + 2L trip) = 13 km/L (misleading! Should be 15 km/L for trip alone)

**More severe scenario:**
1. Session.distanceMeters = 0 km (no trip yet)
2. But session.obdFuelConsumedL = 2.5 L (accumulated)
3. avgKmL check (line 450):
   ```kotlin
   if (persistedFuelL > 0.01 && persistedDistKm > 0.01) {
       avgKmL = persistedDistKm / persistedFuelL
   }
   ```
4. If persistedDistKm < 0.01 (< 10 meters), NO avgKmL calculated
5. When distance finally updates to 15 km:
   - avgKmL = 15 / 2.5 = **6 km/L** (unreasonably high consumption!)

---

### **Issue 4: Fuel Rate Can Become NaN or Infinity**

**File:** `ObdPollingService.kt`, Line 530 (speed-density)

```kotlin
val mafGs = (rpm * mapKpa * ve * displacementL * molarMassAir) / (120.0 * r * cachedIatK)
if (mafGs <= 0.0) return null
val afr = 14.7 * cachedLambda
val fuelGs = mafGs / afr                    // ← Division by afr
val fuelLph = fuelGs * 3600.0 / 745.0      // ← Could overflow or be NaN
```

**Edge Cases:**
- If cachedLambda = 0: `afr = 0` → `fuelGs = Infinity`
- If cachedIatK = 0: `mafGs = Infinity` → `fuelGs = Infinity`
- If RPM = 0 but idling: mafGs = small number, but lambda = 1.0, results in valid fuel
- **But:** No check that calculated fuelLph is reasonable

**Scenario:**
1. Lambda sensor returns 0 (sensor failure)
2. cachedLambda = 0 (cached every 8 cycles)
3. afr = 14.7 × 0 = 0
4. `fuelGs = mafGs / 0` = **Infinity**
5. Persisted to database as Infinity
6. avgKmL = 45 / Infinity = **0** (shows NO fuel consumption, unrealistic!)

---

## **Root Cause Summary**

| Issue | Cause | Symptom | Likelihood |
|-------|-------|---------|-----------|
| **MAF spike** | Corrupted bytes → 255,255 | 213 L/h instead of 8 L/h | **High** (CAN bus noise) |
| **Cold temp spike** | IAT sensor glitch → reads 0 or 255 | Fuel 1.28x-1.6x inflated | **Medium** (sensor failure) |
| **Distance-fuel sync** | Session distance not updated | High fuel / low distance | **High** (async write) |
| **NaN/Infinity** | Lambda or IAT = 0 | Database stores Infinity | **Low** (full sensor failure) |

---

## **Recommended Fixes**

### **Fix 1: Add Fuel Rate Bounds Checking**

```kotlin
private fun clampFuelRate(fuelLph: Double?): Double? {
    if (fuelLph == null) return null
    return fuelLph.coerceIn(0.1, 100.0)  // Gasoline cars: 0.1 to ~100 L/h
}

// In parseObdFuel():
val fuelRate = maf * 3600.0 / (14.7 * 750.0)
return Pair(clampFuelRate(fuelRate), "MAF_DERIVED")

// In speed-density:
if (mafGs <= 0.0 || !mafGs.isFinite()) return null
val fuelLph = fuelGs * 3600.0 / 745.0
if (!fuelLph.isFinite() || fuelLph > 100.0) return null  // Bounds check
return Pair(fuelLph, mafGs)
```

### **Fix 2: Validate Temperature and Lambda Sensor Values**

```kotlin
if (sdCycle % 8 == 0) {
    extractDataBytes(writeATCommand(socket, "010F"), 0x41, 0x0F)?.getOrNull(0)?.let { a ->
        val iatC = a - 40  // Raw to Celsius
        if (iatC in -40..125) {  // Reasonable engine operating range
            cachedIatK = iatC + 273.15
        }
    }
    extractDataBytes(writeATCommand(socket, "0144"), 0x41, 0x44)?.let { b ->
        if (b.size >= 2) {
            val lambda = (2.0 / 65536.0) * (b[0] * 256 + b[1])
            if (lambda in 0.5..2.0) {  // Valid AFR correction range
                cachedLambda = lambda
            }
        }
    }
}
```

### **Fix 3: Sync Distance and Fuel Writes**

```kotlin
// Instead of reading old session state:
val activeSession = sessionDao.getActiveSession()

// Calculate what we'll write:
val persistedFuelL = (activeSession?.obdFuelConsumedL ?: 0.0) + fuelIncrementL

// Write fuel ONLY if we have recent distance
val persistedDistKm = (activeSession?.distanceMeters ?: 0.0) / 1000.0
if (persistedDistKm > 0.01) {  // Only if distance is meaningful
    if (fuelIncrementL > 0.0) {
        sessionDao.addObdAccumulator(sid, fuelIncrementL)
    }
    // Re-read to get updated values
    val updatedSession = sessionDao.getActiveSession()
    val finalFuelL = (updatedSession?.obdFuelConsumedL ?: 0.0)
    val finalDistKm = (updatedSession?.distanceMeters ?: 0.0) / 1000.0
    if (finalFuelL > 0.01 && finalDistKm > 0.01) {
        avgKmL = finalDistKm / finalFuelL
    }
}
```

---

## **Verification**

Once fixed, add logging:

```kotlin
if (fuelRateLph != null && fuelIncrementL > 0.0) {
    Log.d(TAG, "fuel_sample: rate=${String.format("%.2f", fuelRateLph)} L/h, " +
        "dt=${String.format("%.2f", dtSeconds)}s, increment=${String.format("%.4f", fuelIncrementL)}L, " +
        "source=$fuelSource, bounds_ok=${fuelRateLph in 0.1..100.0}")
}
```

This will help detect anomalies in production.
