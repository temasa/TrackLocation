# TrackLocation Fuel Consumption Diagnostic Guide

## To Identify the Unreasonably High Fuel Consumption Issue

### Step 1: Open the App and Navigate to Track Screen
1. **Open TrackLocation** on your phone
2. Go to the **Track** bottom nav tab
3. Find a session/trip that shows **unreasonably high fuel consumption**

### Step 2: Screenshot and Note These Values
Please capture:
- **Session/Trip Name** or timestamp
- **Reported km/L** (what looks "unreasonably high"?)
- **Distance displayed** (meters or km)
- **Fuel consumed** (liters)
- **OBD Status** (DIRECT_FUEL_RATE, MAF_DERIVED, SPEED_DENSITY, UNAVAILABLE)

### Step 3: Export Data via ADB
Run these commands on your computer:

```powershell
$adb = "C:\Users\rinal\AppData\Local\Android\sdk\platform-tools\adb.exe"

# Take a backup of the entire app database
& $adb backup -f track_database.ab com.kolee.tracklocation

# This creates track_database.ab - you may be prompted on the phone to confirm backup
```

**On your phone:** A dialog will appear asking "Back up my data?" — tap **"Back up my data"**

### Step 4: Detailed Questions to Answer

**A. Which metric looks wrong?**
- [ ] Session average km/L? (show value)
- [ ] Trip average km/L? (show value)
- [ ] Instant km/L on OBD screen? (show value)

**B. What condition causes it?**
- [ ] After app restart/crash?
- [ ] After Bluetooth adapter disconnect?
- [ ] When recording but not tracking?
- [ ] Always, regardless of conditions?
- [ ] Specific time of day?

**C. Data collection details:**
- How long was the session/trip?
- Was OBD connected the whole time?
- Did you see any "UNAVAILABLE" fuel rate messages?
- What's your engine displacement in cc? (from settings)
- Fuel source used (DIRECT, MAF, or SPEED_DENSITY)?

**D. Example numbers:**
```
Session ABC:
- Distance: X km
- Fuel consumed: Y L
- Reported avg: Z km/L
- Expected avg: W km/L (what it should be)
```

## What We'll Analyze

Once you provide the above, I'll check for:

1. **Missing OBD samples** in the database
2. **Fuel rate spikes** (huge values like 50+ L/h)
3. **Time gap anomalies** (missing samples due to adapter drops)
4. **Null fuel rates** (UNAVAILABLE periods)
5. **Session distance vs GPS distance mismatch**
6. **Database corruption or incomplete writes**
7. **Timestamp desynchronization** between samples

## Screenshots to Capture

1. **Main Track screen** showing the problematic session/trip
2. **OBD Polling status screen** (show fuel source and real-time km/L)
3. **Session/Trip details** (if available - distance, fuel, duration)
4. **Logcat from the device** while recording (see below)

---

## Alternative: Share Raw Logs via Logcat

If the app is running and you can start fresh recording:

```powershell
$adb = "C:\Users\rinal\AppData\Local\Android\sdk\platform-tools\adb.exe"

# Clear and start recording logs
& $adb logcat --clear
# <Do your recording/test on the phone for 2-3 minutes>
& $adb logcat -d > logcat_dump.txt

# Share logcat_dump.txt
```

Then search in the file for:
- `"poll:"` — shows each polling cycle's fuel rate and speed
- `"runPollingLoop"` — shows fuel calculation logic
- Any errors or exceptions

---

**Once you've gathered this data, reply with:**
1. Screenshot(s) of the issue
2. The numbers from Step 4 above
3. Answers to the conditions in **Step 4B**

This will help pinpoint whether it's a **missing data**, **fuel rate spike**, **adapter glitch**, or **distance/fuel accumulator mismatch** issue.
