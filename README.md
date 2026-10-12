# Satark

An Android MVP in development: check suspicious messages and links on the phone, explain the evidence, and suggest independent verification.

**Phase 4 engineering package is complete (v0.4.0).** [Evidence report](docs/PHASE_4.md), [demo guide](docs/DEMO_GUIDE.md), and a local delivery ZIP with checksums. The competition PDF is deferred at the owner's request. Real WhatsApp/physical-device acceptance is still required.

**Phase 3 adds opt-in notification previews, supported link handling, encrypted summaries and signed offline updates.** Default checks use explicit rules. The optional trained model is off by default because the small synthetic evaluation found false warnings. Analysis performs no network requests. Optional history saves only manual-check summaries; there is no telemetry. See [Phase 3](docs/PHASE_3.md) for setup, limits and evidence.

## Android app

The app supports Android 8.0 (API 26) or newer. Paste a message, share text/plain content, send selected text, or choose Satark when Android offers it for a web link. Review the input, optionally enable experimental AI, then tap **Check privately**. Open **Connections, history and updates** to enable selected WhatsApp previews or encrypted history. Android access must also be granted for previews.

```powershell
.\scripts\android-build.ps1 -Test -Lint
```

The debug APK is copied to `output/ScamCheck-debug.apk`. Building requires Android SDK 35 and a compatible JDK. The SDK/tool downloads used on this machine are isolated under ignored `.tools/`; app users need no Java or SDK.

- Worker-thread checks with cancellation and generation-based stale-result suppression.
- Three result levels, reasons, actual website addresses, safe next steps and synthetic examples.
- Paste/share confirmation; no automatic reading of the clipboard or a chat database.
- Optional POST_NOTIFICATIONS permission on Android 13+; no INTERNET, SMS, contacts or storage permissions, ads or analytics.
- No saved input state or autofill; backups excluded and screen capture protected.
- Local model availability, method used and limitations are visible.
- Model training and reproduction: [model/MODEL_CARD.md](model/MODEL_CARD.md).

## Run on Windows

Requires a JDK with `java` and `javac` on PATH, version 17 or newer. The project has no external Java dependencies and needs no downloads at build/runtime.

```powershell
cd C:\Users\mkb63\Desktop\ScamCheck
.\scripts\build.ps1 -Test -Demo
```

Check your own content without putting it in command arguments:

```powershell
.\scripts\check.ps1
```

Paste the text, then press Enter on an empty line. The console displays destination domains and explanations, not the original message or URL paths/query parameters. Terminal software may retain displayed output in its own scrollback; this is not a private Android UI yet.

## Implemented

- Three outcomes: Strong warning, Needs checking, No warning signs found.
- English-oriented rules for credential requests, urgency, KYC/account actions, pay-to-receive refund patterns, and remote-app requests.
- Basic negation handling for legitimate prevention advice, such as "Never share your OTP".
- Multiple-link extraction, deduplication, input bounds and explicit partial-input warnings.
- URL parsing: actual destination, public suffixes including private hosting, subdomain deception, userinfo, numeric addresses, malformed links, international characters, HTTP and selected shorteners.
- A source-backed starter registry for HDFC Bank, ICICI Bank, SBI, Amazon India and Flipkart.
- Separate message and link evidence. Multiple correlated URL clues are not counted as independent detection votes.
- Plain-language reasons and safe next steps. Known addresses never override suspicious message text.
- Immutable output with no raw message or full submitted URL; optional encrypted history stores limited summaries only.
- A caller can mark input as partial, for later notification integration.

## Core API

```java
ScamChecker checker = new ScamChecker(); // Load bundled lists once.
CheckResult result = checker.check(message);
CheckResult partial = checker.check(notificationPreview, true);
```

Reuse the same checker instance. Result fields include level, findings, links, actions, limitations, engineVersion and method. Each finding has a stable code, kind and explanation. Each link has a host, registrable website address and optional known-brand match. A match indicates a saved address, not a verified sender or safe page.

## Important boundaries

- No fraud probability or detection-accuracy claim is produced.
- The optional local model has only synthetic development evidence and produces false warnings. English paraphrases, negation, sarcasm and multi-message scams remain limited. A few Latin-script Hindi keywords are incidental rules, not validated Hindi/Hinglish support.
- Unknown websites are uncertain, not automatically fraudulent.
- No live page inspection, DNS, TLS certificate checks, redirect resolution or fresh reputation lookups occur.
- Java's baseline IDN parser is not a complete modern browser URL parser. Unsupported/ambiguous inputs should be treated as uncertain. Browser-equivalent URL parsing remains an Android integration task.
- The organisation registry is deliberately small; it is not an exhaustive list of each brand's domains or partner services. Saved entries are not ownership proofs for every page or subdomain.
- There is no public malicious-domain feed bundled yet. Demonstrations use synthetic scam text and unvisited example addresses. A provenance/licence-reviewed feed is a later addition.
- Input is limited to 12,000 UTF-16 code units and 32 distinct links. Limit findings prevent a silent all-clear.
- Notification access checks only body previews from selected WhatsApp sources after consent. Hidden content and full chats are unavailable. No SMS inbox access or universal link interception is provided. Apps opening links internally may bypass Android routing; use share/paste instead.

## Validation

60 core checks, 10 model checks and 17 signed-update checks cover destination confusion, benign OTP notices, uncertain inputs, inference parity, safe model fallback and hostile update inputs. Android tests cover confirmation, consent, encrypted storage, actual system preview delivery and rollback. These are engineering checks, not an independent fraud benchmark. See `docs/VALIDATION.md` and `model/MODEL_CARD.md`.

## Project layout

- `core/src/main/java/in/scamcheck/core`: offline engine and types.
- `core/src/main/java/in/scamcheck/cli`: console entry point.
- `core/src/main/resources`: bundled website registry and Public Suffix List.
- `core/src/test/java`: dependency-free regression checks.
- `scripts`: reproducible compile, check and demo commands.
- `docs/ROADMAP.md`: four phases and acceptance criteria.
- `docs/DATA_SOURCES.md`: source dates, list version, hash and third-party disclosure.

The core targets Java 17. The Android app uses core-library desugaring for records, collections and stream APIs. A checked, simple HTTP(S) link can be handed to a user-selected external app after a separate confirmation. Opening it may use the external app's internet connection and disclose the URL; ScamCheck never fetches it.

## Project name

Satark â€” Think before you tap. The Android app label and screens use Satark. The application ID `in.scamcheck.app`, encryption identifiers and older artifact filenames are retained for compatibility with existing installs. Historical build evidence records the earlier ScamCheck name. New builds also create `output/Satark-debug.apk`.
