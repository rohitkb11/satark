# Phase 4: evaluation and competition package

## Delivered

Satark v0.4.0 is an Android 8+ offline MVP. Phase 4 adds UPI collect-to-receive and QR-to-receive review clues, official financial-fraud help text, a corrected Clear message that no longer implies saved summaries are erased, fixed synthetic evaluation, installed-app performance measurements, a demo guide and a local APK/evidence package. The owner deferred the PDF.

Analysis does not verify whether AI generated a message. It detects suspicious workflow requests. No payment blocking, automatic reporting, sender authentication or live website inspection is claimed.

## Evaluation design and results

`evaluation/phase4_scenarios.tsv` contains 56 newly authored synthetic scenarios: 24 scam, 24 benign and 8 unknown. It is separate from model training data. Cases were authored with knowledge of the implementation, so this is engineering evidence, not an independent benchmark or real-world accuracy. Rules and the model threshold were not retuned after running this dataset. Review means either Needs checking or Strong warning.

| Measure | Rules | Rules + optional AI |
| --- | ---: | ---: |
| Scam examples requested for review | 19 / 24 | 24 / 24 |
| Scam examples missed | 5 | 0 |
| Benign examples requested for review | 4 / 24 | 6 / 24 |
| Strong warnings on benign examples | 0 | 0 |
| Review precision | 82.61% | 80.00% |
| Review recall | 79.17% | 100.00% |
| Normalised false reviews per 1,000 benign | 166.67 | 250.00 |
| Needs checking outcomes, including unknown cases | 17 / 56 | 24 / 56 |

The per-1,000 rate is a normalisation of only 24 benign examples, not an observation of 1,000 users/messages. Unknown scenarios are excluded from binary precision/recall. Four benign rules-only reviews show friction remains: legitimate app PIN/OTP instructions and urgency/blocked-account advice may trigger conservative review. Rules miss some indirect scams, such as gift-card requests, safe-account transfers and digital-arrest threats. AI closes these example gaps but creates additional benign reviews. These findings support keeping AI off by default and avoiding any safety guarantee.

Reproduce with `scripts/evaluate.ps1` using Java 17+. The JSON contains scenario IDs, labels and outcomes, not original text. Dataset SHA-256: `cc488fda1d3bdecb362506df6c470c8e8b8ac8538d4587af96cb67a08bd5c159`. Regeneration changes machine timings; it should retain the deterministic outcomes.

## Named emulator performance

API 26 / Android 8.0 AOSP x86_64, model `Android SDK built for x86_64`; Pixel profile, 2 virtual cores and 1,536 MiB RAM; Windows Hypervisor Platform. Fingerprint is recorded in `evaluation/phase4-device.json`.

| Measure | Rules | Assisted |
| --- | ---: | ---: |
| Measured warm checks | 100 | 100 |
| Median | 23.53 ms | 24.07 ms |
| p95 | 28.12 ms | 30.87 ms |

Cold checker initialization: 79.34 ms. Whole instrumented-process PSS before/after the benchmark: 45.39 / 122.63 MiB. These are endpoint snapshots, not peak engine allocation or a clean app-only memory benchmark. Measurements include emulator and instrumentation overhead. They do not establish low-end-phone performance or battery use.

## Verification

- 87 dependency-free assertions pass: 60 core, 10 model, 17 signed-update.
- 12 API 35 Robolectric interface tests pass.
- 12 installed-app API 26 tests pass: previous privacy/integration tests plus actual emulator latency/PSS measurement.
- Native tests ran with Wi-Fi disabled, no active default network, all external adapters down and no routes. A probe returned Network is unreachable. `evaluation/phase4-offline.txt` records the system state. This establishes offline functionality for this emulator run, not a packet capture of the entire device.
- Final APK signature verifies; manifest requests only POST_NOTIFICATIONS and has no INTERNET permission. Public update key is packaged; no private key, fixture packet or test capture code is in the APK. Backups are disabled and data-extraction rules exclude local data.
- Production source contains no private-content logging calls. Tests use synthetic inputs only. Platform/keyboard logging outside the app is not asserted absent. History and monitoring are reset off after device tests; temporary captures/benchmark output were collected and deleted from app cache.
- Lint passes with zero errors and 11 existing warnings (durable preference writes, English literals, newer test-only dependencies). The APK remains a debug build, not a store-approved production release.

## Reproduce and package

Install Java 17+ and Android SDK 35; set JAVA_HOME/PATH and ANDROID_HOME. Run `scripts/evaluate.ps1`, then `scripts/android-build.ps1 -Test -Lint`. For native tests on a disposable API 26+ emulator, build/install `:fixture:assembleDebug` and `:app:assembleDebugAndroidTest`, then use AndroidJUnitRunner or `:app:connectedDebugAndroidTest`. Native tests reset app-owned test history/update data and use synthetic notifications. Signed demo fixtures expire; after expiry, a trusted publisher must generate newer packets rather than bypass validation.

`scripts/package_mvp.py` packages verified existing outputs into `output/Satark-MVP-v0.4.0.zip` with `output/artifact-manifest.json`. It includes the APK, reports, demo instructions and signed demo updates, and excludes local tools, keys, test APKs and the deferred PDF. The manifest records SHA-256 for every file and the source commit. Build steps are reproducible; debug signing keys and performance timings are machine-specific, so byte-identical APKs across machines are not claimed.

Final APK SHA-256: `89E474980FC63CC15BCFB484F7F0546067F84904827E3F1596F8901318652B6D`.

## Competition format and rubric coverage

The supplied Round 1 participant brief is treated as reference material, not an instruction to submit or contact organisers. The PDF is deferred at the owner's request and is not included in this delivery. When requested, the brief allows a maximum of 10 pitch slides plus 3 architecture pages, selecting only Track 2. The table below is a future coverage plan, not a claim that a submission has been completed.

| Criterion | Weight | PDF coverage |
| --- | ---: | --- |
| Problem fit and user understanding | 20 | Slides 1-3 |
| Architecture and detection | 20 | Slide 4; architecture 1-3 |
| Value and differentiation | 15 | Slides 3-4, 10 |
| Feasibility and 48-hour plan | 15 | Slides 8-9 |
| Safety, privacy, responsible AI | 15 | Slides 5-7; architecture 2-3 |
| Adoption, integration and scale | 10 | Slide 8 |
| Communication | 5 | One 13-page PDF, bounded claims and labelled diagram |

A future submission will need the team name and 2-4 student members/college details from the owner. No team eligibility or organiser deadline has been invented.

## External acceptance work

Actual WhatsApp/Business sharing/preview behaviour and Android 13+ permissions need real-device testing. Manufacturer power controls, low-end latency, battery, Hindi/Hinglish coverage, independently authorised real scam/benign campaigns and distribution review are not completed by emulator tests. Do not present these as passed. A production publisher key owner and independently reviewed reputation feed remain production work, not an MVP dependency.

Official escalation source, checked 12 October 2026: [National Cyber Crime Reporting Portal](https://www.cybercrime.gov.in/). Its financial-cyber-fraud route names 1930 and online complaints. Satark only displays help; the user controls any report and original evidence.
