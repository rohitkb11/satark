# Four-phase MVP build

## Phase 1 - Offline detection foundation (implemented)

- Create an isolated project and reusable local engine.
- Define stable result levels, finding codes, actions and explicit uncertainty.
- Implement bounded text and link input, English message rules, URL parsing and combined evidence.
- Bundle source-backed organisation addresses and a pinned Public Suffix List.
- Build a terminal demo and meaningful regression checks.
- Document limitations and prevent raw messages/URL secrets from being retained in results.

Exit: the core builds and tests on this workspace without Android tools, a server or network requests. It can demonstrate scam, benign and unknown cases. It is clearly labelled rules-only.

## Phase 2 - Android app and local AI (implemented)

- Set up the Android SDK/toolchain and native app project.
- Build input confirmation, paste/share receiving, checking state, result details and safe next steps.
- Process on a worker thread; support cancellation and prevent outdated results appearing after input changes.
- Bundle assets and omit INTERNET permission.
- Integrate a compact, licence-reviewed local message model. Evaluate it against rules-only before enabling it; expose method/model status and do not substitute fake confidence if loading fails.
- Prepare a documented synthetic/public development set with campaign/template-grouped held-out evaluation.
- Keep English as the supported language until Hindi/Hinglish are separately evaluated.
- Include an Android compatibility check for Java records/collections/streams and browser-equivalent URL parsing.

Exit: an installable APK handles paste and supported text/link shares, performs local checks, explains results, and exposes a real model status. Verify on a physical device or emulator.

Delivered: `output/ScamCheck-debug.apk`. Six UI tests pass on Robolectric API 35, and six installed-app tests pass on an Android 8 / API 26 emulator with Wi-Fi and mobile data disabled. The local model is bundled but remains opt-in after its measured false warnings. User-facing notification monitoring and automatic browser routing have not been claimed as completed.

## Phase 3 - Privacy and practical integration (implemented)

- Add explicit consent and selectable notification sources only after checking platform/distribution requirements.
- Mark truncated, hidden or partial notification content; never imply a full chat was examined.
- Test user-initiated supported link handling. Document routing gaps and retain share/paste fallback. No promise of intercepting every WhatsApp tap.
- Add optional local history, off by default, with protected storage, deletion and backup exclusions.
- Add signed, versioned offline update import with integrity verification, failure recovery and rollback.
- Add a provenance/licence-reviewed harmful-address feed if available; unknown is still not malicious.
- Audit logs, dependencies, permissions, exported components and local data handling.

Exit: privacy controls work; integration coverage is demonstrated and honestly documented. Do not bypass Android restrictions to read private chat/SMS data.

Delivered: explicit opt-in WhatsApp/Business preview filtering, private generic warnings, supported HTTP(S) receiving with manual check and confirmed external handoff, encrypted optional summary history, signed and validated offline updates with rollback and retained replay floor. Tests exercise the actual Android notification service using a synthetic sender, not installed WhatsApp. No suitable licensed real threat feed has been bundled. See `PHASE_3.md` for setup and boundaries.

## Phase 4 - Evaluation and competition demonstration

- Test unseen scam campaigns, benign banking messages, shortened links, misleading domains and app/model failures.
- Measure recall, precision, false warnings per 1,000 benign messages, uncertain outcomes, latency and memory on named devices.
- Compare rules-only and rules-plus-model. Do not present synthetic scenario success as real-world performance.
- Demonstrate operation with networking disabled and inspect network/log/backup behaviour.
- Produce a reproducible APK, demo script, evidence report and known limitations.
- Prepare the self-contained pitch PDF: up to 10 slides plus up to 3 architecture pages/slides and source/AI disclosures.

Exit: all working features have demonstrated acceptance evidence. Unfinished automatic integration is identified as future work.
