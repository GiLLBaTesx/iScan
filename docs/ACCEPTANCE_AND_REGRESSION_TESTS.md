# iScan — Acceptance & Regression Test Plan

This document pairs two things:

1. **Automated regression tests** — JVM/jqwik tests that run on every build (`./gradlew :app:testDebugUnitTest`) and guard the core logic against regressions. No device needed.
2. **Manual acceptance tests** — a human QA checklist for the end‑to‑end feature flows, especially the device‑dependent ones (camera/scanning, biometrics, billing, printing) that cannot be unit‑tested.

Scope reflects the current feature set of the offline‑assessment transformation. The app is **offline‑first**: all core flows must work with the device in airplane mode.

---

## 1. Automated Regression Tests (run on every build)

Run everything:

```bash
./gradlew :app:testDebugUnitTest            # JVM unit + jqwik property tests (no device)
./gradlew :app:compileDebugAndroidTestKotlin # instrumentation tests compile (no device)
# On a connected device/emulator (optional, exercises Room + SQLCipher migration):
./gradlew :app:connectedDebugAndroidTest
```

### JVM unit / property suites (`app/src/test`)

| Suite | Area covered (regression guard) | Design property |
|---|---|---|
| `MasteryCalculatorPropertyTest` | MELC mastery banding + bounded aggregation | Property 1, 2 |
| `AnalyticsEngineItemAnalysisPropertyTest` | Difficulty / discrimination index bounds & classification | Property 3, 4 |
| `QRMetadataRoundTripPropertyTest` | QR encode → decode round‑trip | Property 5 |
| `AnswerSheetRoundTripPropertyTest` | print → fill → scan → process, and parse→print→parse | Property 6, 7 |
| `BubbleParseResolutionPropertyTest` | Bubble fill classification + multi‑mark resolution | Property 8 |
| `RosterRoundTripPropertyTest` | CSV/XLSX roster export → import round‑trip | Property 9 |
| `ValidationEnginePropertyTest` | **Input validation partitioning (valid/invalid)** | **Property 10** |
| `RecycleBinRetentionPurgePropertyTest` | 30‑day recycle‑bin retention purge | Property 11 |
| `TierLimitEnforcementPropertyTest` | Free/Premium tier limit + feature gating | Property 12 |
| `AnswerSheetGradePropertyTest` | **Auto‑grade: points, alternatives, no‑answer/multi‑mark never score** | Req 17.3 |
| `AnswerSheetLayoutTest` | QR answer‑sheet layout: no overlap, dynamic rows, page‑size selection | Req 19/21 |
| `TemplateLayoutTest` | Built‑in/custom template fit‑then‑escalate ladder (columns, grow paper, paginate) | Req 21 |
| `BubbleGridMapperTest` | Printed‑layout → pixel‑grid mapping for scanning | Req 22 |
| `ReportStatisticsTest` | Class statistics (mean/median/high/low) incl. empty cohort | Req 5.4 |
| `PacingEngineTest` | Coverage status + behind‑schedule logic | Req 9 |
| `DashboardIntervalMappingTest` | Dashboard date‑range → interval granularity | Req 12 |

### Instrumentation (`app/src/androidTest`, needs device/emulator)

| Suite | Area |
|---|---|
| `Migration5To6Test` | Room v5 → v6 migration preserves data, adds the four new tables |

**Regression gate (pass criteria):** `./gradlew :app:testDebugUnitTest` is `BUILD SUCCESSFUL` with zero failures before any release. The migration test should also pass on a device before shipping a build that changes the schema.

---

## 2. Manual Acceptance Tests (human QA checklist)

**Legend:** ✅ pass · ❌ fail · ⚠️ pass‑with‑notes. Record device model, Android version, and build under test for each run.

**Preconditions:** fresh install (or Clear All Data) unless a test says otherwise. For offline tests, enable **airplane mode**.

### AT‑01 First‑run onboarding & gates
- [ ] Fresh launch shows the onboarding tutorial (skippable with **Skip**).
- [ ] If the device is rooted, the security warning appears **before** the auth gate.
- [ ] If a device lock (PIN/pattern/biometric) is set, the app requires authentication before any student data is shown; cancelling keeps data hidden with a retry.
- [ ] On a device with **no** lock configured, the app still opens (fail‑open, no lockout).
- [ ] Rotate the screen / switch dark mode mid‑session → it does **not** re‑prompt biometrics (unlock state persists).
- [ ] Background the app and return → it re‑prompts (re‑locks on background).

### AT‑02 Subject / section / exam organization
- [ ] Create a subject folder (name 1–100 chars); empty name is rejected with a message.
- [ ] Rename a subject; contained exams are preserved.
- [ ] Create a section (name ≤ 50 chars, capacity 1–100).
- [ ] Create an exam under a subject (question count 1–200; 0 and 201 are rejected).

### AT‑03 Answer key + MELC mapping
- [ ] Open **Edit Answer Key**, set answers for each question, save, reopen → answers persist.
- [ ] Map a question to a MELC in the Answer Key editor; reopen → mapping persists.
- [ ] Import an answer key from CSV; invalid rows are reported, valid ones imported.

### AT‑04 Built‑in (Quick) & custom templates — **print layout**
- [ ] Settings → **Download Built‑in Templates**: each Quick row (20/30/50/75/100) shows a **preview (eye)** and a **download** icon.
- [ ] Tap the **eye** → PDF opens view‑only (no "downloaded" wording). Tapping eye never triggers a download.
- [ ] Tap **download** → PDF opens / share sheet offered.
- [ ] **Quick 20** → clean 2‑column sheet, bubbles not overlapping.
- [ ] **Quick 60** → fits without overlap (escalates columns and/or paper); still 2‑up.
- [ ] **Quick 100** → stays legible (2‑up, up to 3 columns, grows paper as needed); **no overlapping bubbles** (the original bug).
- [ ] **Create Custom Template** with a high question count and A–G choices → bubbles never overlap; the preview matches the generated PDF.
- [ ] Open a generated PDF in a real viewer and confirm it prints on the stated paper size.

### AT‑05 Scanning & auto‑grading *(device + printed sheet)*
> Requires printing an app‑generated answer sheet and filling bubbles. Scan roughly square‑on (no perspective correction yet).
- [ ] Generate & print an answer sheet for an exam (QR header present).
- [ ] Fill a known pattern of bubbles; scan via the camera.
- [ ] QR is detected and auto‑associates the scan to the correct exam (toast shows the matched exam).
- [ ] With no QR (or unreadable), it falls back to the manually selected exam.
- [ ] Detected answers match what was filled; score/percentage is correct.
- [ ] A fully blank question = "No Answer" (0 points); two shaded = "Invalid – Multiple" (0 points).
- [ ] Camera permission denied → the permission‑required screen appears; granting proceeds.

### AT‑06 Exam Detail tabs (Results / Analytics / Reports)
- [ ] Before any scans: the "Ready to grade!" empty state with **Start Scanning** + **Generate Answer Sheet** (no tabs).
- [ ] After the first scan: **Results | Analytics | Reports** tabs appear (Results default).
- [ ] **Results** tab: class stats + ranked students; **Scan More**, **Export results**, **Clear all results** work.
- [ ] **Analytics** tab: item analysis renders (single app bar — no doubled header).
- [ ] **Reports** tab: Individual (student picker), Class summary, School‑level each generate a PDF and open the share sheet.
- [ ] The overflow (⋮) menu contains only **Edit answer key**, **Rename exam**, **Generate answer sheet**.

### AT‑07 Analytics & reports content
- [ ] Item analysis shows difficulty + discrimination with sane values; low‑quality items are flagged.
- [ ] Individual report includes score, per‑question breakdown, and MELC mastery.
- [ ] Class report includes mean/median/high/low and item analysis; empty cohort doesn't crash.
- [ ] School‑level report: **denied for Free** (upgrade prompt), **allowed for Premium**.

### AT‑08 CSV / Excel import‑export
- [ ] Export exam results (CSV) → file lands in Downloads with a timestamped name; columns correct.
- [ ] Export roster (CSV) and re‑import it → students round‑trip correctly.
- [ ] Import a roster with some invalid rows → valid rows import, invalid rows reported.
- [ ] XLSX export opens in a spreadsheet app.

### AT‑09 Recycle bin & data recovery
- [ ] Delete an exam / subject / section → it moves to the Recycle Bin (not gone).
- [ ] Restore an item → returns to its original location.
- [ ] Permanent delete asks for confirmation.
- [ ] (Retention) items older than 30 days are purged by the background worker (verify logic via `RecycleBinRetentionPurgePropertyTest`; on‑device, confirm the daily worker is scheduled).

### AT‑10 Backup & restore (user‑owned data)
- [ ] Settings → **Manage Backups** → create a backup; a timestamped encrypted file is produced.
- [ ] Move the backup off‑device (user's own cloud/storage) — confirm the app does **not** upload anywhere itself.
- [ ] Restore from a backup → data returns; the app restarts/reloads.
- [ ] Restoring a corrupted/invalid file is rejected with a clear message.
- [ ] Backup/Restore available on **all tiers** and works in **airplane mode**.

### AT‑11 Subscription / tier limits *(billing — leave as‑is per current scope)*
- [ ] Free tier: creating a 4th subject, 6th exam, or 31st scan shows the upgrade prompt.
- [ ] Settings → **Manage Subscription** opens; current plan shown.
- [ ] (If billing enabled) upgrade flow launches Google Play; on success premium features unlock.
- [ ] Opening the subscription screen before billing connects fails gracefully (no crash).

### AT‑12 Localization (EN / FIL)
- [ ] Settings → Language → switch to Filipino → UI updates **without restart**.
- [ ] Relaunch → the chosen language persists.
- [ ] MELC descriptions remain in English (per DepEd standard).
- [ ] On an API 26–32 device, the saved Filipino locale actually applies to the UI (AppCompat back‑port).

### AT‑13 Settings / About / Privacy
- [ ] Privacy Policy is reachable via **Settings → About iScan → Privacy Policy** (no duplicate top‑level entry).
- [ ] About shows version, Terms, Licenses.

### AT‑14 Offline‑first & security
- [ ] In airplane mode: create exam, scan/grade, generate report, backup — all work.
- [ ] No student data is transmitted off‑device by the app itself.
- [ ] Database is encrypted (SQLCipher) — a raw copy of the DB file is not plain‑readable.

### AT‑15 Performance (low‑end target: 2 GB RAM, Android 8)
- [ ] App launches within ~3 s.
- [ ] Exam list with 100+ exams loads within ~1 s (paginated).
- [ ] A single sheet scans/processes within ~5 s.
- [ ] Generating a report for ~100 students completes without an OutOfMemory crash.

---

## 3. Release sign‑off

A build is acceptance‑ready when:

- [ ] `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (all regression suites green).
- [ ] `Migration5To6Test` passes on a device/emulator.
- [ ] All **AT‑01 … AT‑15** manual flows pass (or failures are triaged/accepted) on at least one low‑end and one modern device.
- [ ] Known limitations are acknowledged: scanning assumes a square‑on capture of an app‑printed sheet (no perspective/deskew yet); multi‑page exams are scanned one physical page at a time; billing/login flows are left at current scope.

---

### Known gaps not covered by automated tests (manual only)
Camera capture & real OMR accuracy, biometric prompt, Google Play Billing purchase, PDF rendering/printing fidelity, WorkManager scheduling on‑device, and true WCAG accessibility (needs TalkBack + manual review).
