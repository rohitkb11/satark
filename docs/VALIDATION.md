# MVP validation

## Verified on this workspace

- Windows / PowerShell.
- Installed Oracle JDK 24.0.2; compiled with `--release 17`.
- Command: `.\scripts\build.ps1 -Test -Demo`.
- 55 dependency-free regression assertions pass.
- Five console scenarios exercise KYC impersonation, a normal OTP notice, a shortened link, a normal Amazon message and a pay-to-receive refund request.

## Covered boundaries

- Brand names appearing in subdomains, URL paths or query strings cannot turn an unrelated destination into a known brand address.
- Destination extraction uses the full bundled Public Suffix List, including `co.uk`, private hosting, wildcard and exception examples.
- Known addresses do not override credential-disclosure requests, and a claimed brand conflicts with a different recognised organisation.
- Normal OTP alerts and explicit don't-share/don't-install/don't-pay advice are not treated as direct requests by the tested rules.
- Unknown, shortened, international, numeric, local, malformed and unsupported links remain uncertain.
- Empty input fails clearly. Oversized/partial inputs and excess links have explicit uncertainty findings.
- Full message text and URL paths/query tokens are omitted from result objects. Destination host/domain values remain present because the user needs to examine them.
- Method output explicitly states rules-only.

## Not yet verified

Physical-device behaviour, real WhatsApp notification visibility, battery use, low-end-phone latency, real-world model accuracy and distribution compliance. Universal link interception is not supported. Phase 3 engineering evidence for history, notifications and updates is recorded below.

These assertions establish specific implementation behaviour. They do not justify accuracy percentages or a claim that messages/sites are safe.

## Phase 2 verified on 2026-10-12

- Debug APK build and signature verification pass. Android application ID: `in.scamcheck.app`, version 0.2.0, minimum API 26, target API 35.
- Final APK: `output/ScamCheck-debug.apk`, 192,421 bytes, SHA-256 `936d986bce0417c1e5fdaf0b1ff1b41a65418576b319823743e2462e1a64831a`.
- Packaged manifest inspected with Android build tools: zero requested permissions, including no INTERNET permission. Model and public suffix resources verified inside the APK.
- 55 core regression checks and 10 model-integration checks pass. The latter include Python/Java inference parity, invalid-model rejection, explicit unavailable-model fallback, and preventing AI-only strong warnings.
- Six Robolectric UI tests pass using Android API 35: shared-input confirmation, result categories, clear/edit invalidation, opt-in model use, selected-text input, and privacy/manifest boundaries.
- Six installed-APK tests pass on an Android 8 / API 26 x86_64 Pixel-profile emulator: shared-input confirmation, benign/unknown categories, opt-in model plus Clear, stale-result cancellation/permission boundaries, and two synthetic UI renders.
- Emulator Wi-Fi and mobile data were disabled for the installed-app tests. This tests offline functionality; it is not a full packet-capture audit of Android or the device keyboard.
- Native UI renders were visually inspected: `output/ScamCheck-home.png` and `output/ScamCheck-result.png`. Synthetic input only. Export helpers exist only in the test APK. Temporary test images were removed from the target app's cache after collection.
- Android lint passes with no errors. Two informational warnings note newer Android test-library versions; these dependencies are test-only. General Gradle deprecation notices remain build-tool warnings.
- Small synthetic model evaluation is documented in `model/MODEL_CARD.md`; it adds review coverage but also flags 6 of 15 benign held-out development examples. It remains off by default.

## Reproduce Android checks

```powershell
.\scripts\android-build.ps1 -Test -Lint
```

With a connected Android emulator/device and the Android SDK available, build the instrumentation APK and run `:app:connectedDebugAndroidTest`. Tests use synthetic fixtures only. A human still needs to test the actual share menu of their installed WhatsApp/SMS app; the automatic tests exercise Android's text/plain sharing contract, not WhatsApp's UI.

## Phase 3 verified on 2026-10-12

- Version 0.3.0 / code 3, min API 26, target API 35. APK signature verification passes with APK Signature Scheme v2.
- Final APK SHA-256: `01A7DF4A8C729906B00BF86DBF99E363B855E3AD455B9D81BDFE8902620B145C`. Delivered as `output/ScamCheck-debug.apk`.
- Packaged manifest requests only POST_NOTIFICATIONS. No INTERNET, SMS, contacts or storage permission. Public update key is packaged; private keys and fixture update packets are absent. No private content logging calls occur in production app source.
- 82 dependency-free checks pass: 55 core, 10 model and 17 signed-update assertions. Update cases include foreign signatures, tampering, replay, expiry/future dates, oversized/truncated data, ZIP traversal/inflation, invalid models and public suffix masquerading as a brand root.
- 12 Robolectric interface tests pass on API 35, including explicit positive consent, cancelled consent, confirmed web input, ambiguous-link rejection and preview details surviving an existing activity's resume.
- 11 installed-app tests pass on the API 26 emulator: six previous smoke tests and five Phase 3 tests. Phase 3 exercises actual Android Keystore encryption, minimal summaries, authenticated tamper rejection, AtomicFile backup recovery, signed import/rollback/replay floor, user-initiated link receiving, off-by-default controls and actual OS notification listener delivery from a synthetic selected sender.
- The notification test grants/rebinds access only within instrumentation on the test emulator. Production uses the user's Android notification settings. The synthetic sender is the separate fixture APK, not WhatsApp; real WhatsApp and manufacturer behaviour are unverified.
- Phase 3 does not claim network-disabled device testing or a packet-capture audit. Offline checks are structurally enforced by absence of INTERNET permission; external app opening and a user's document provider have their own behaviour.
- Lint: zero errors, 11 warnings for synchronous preference persistence, English string literals and newer test-only dependency versions. Gradle/JDK deprecation notices are toolchain warnings.
- Home, synthetic result and default privacy-control renders were inspected. Test-only render code is excluded from the app. Collected PNGs were removed from app cache. Optional history and monitoring were reset off after tests.
- The final rebuild includes a wording correction that links are never opened automatically. Detection and connection logic matches the tested implementation. The updated result render also passes its installed-app test.

For native Phase 3 tests, build/install `:fixture:assembleDebug` along with `:app:assembleDebugAndroidTest` on a disposable emulator. Tests reset their own app-owned history/update data. Do not run them against a personal install containing summaries you want to retain.

## Satark naming and repository publication

The existing MVP was imported into separate feature/documentation commits, then renamed in the Android label, screens and notifications. Legacy package names, cryptographic identifiers and historical screenshots remain compatible. The Satark branding build passes assembleDebug, all 12 Robolectric tests and lint (zero errors); APK signature verification passes. The prior 11 native tests cover the unchanged connection/detection logic. New output `Satark-debug.apk` has SHA-256 `64F315FE8D830012B901D51F4EA1AE2DF0778F659EDAF70679988188ED48C8AA`; the legacy APK alias has the same bytes. The earlier Phase 3 hash above identifies the pre-branding artifact.

## Phase 4

Current v0.4.0 evidence supersedes the earlier artifact hashes: 87 core assertions, 12 interface tests and 12 installed-app offline emulator tests pass. New screenshots show Satark. Latency, memory, synthetic-case confusion counts, final hash and external acceptance limits are documented in `PHASE_4.md` and `evaluation/`. The competition PDF is deferred at the owner's request.
