# Phase 2 handover

## Use the MVP

1. Transfer `output/ScamCheck-debug.apk` to an Android 8.0 or newer phone and install it as a development/test APK.
2. Open ScamCheck and paste text using its Paste button, or share plain text/a link into it from another app where supported.
3. Review the input. Nothing is checked until you tap Check privately.
4. Leave experimental AI off for the default rule checks. Enable it only to explore the development model and its limits.
5. Read the result, reasons, actual destination address and independent-verification advice.
6. Tap Clear to clear the input and result. No history or account exists.

The app does not read WhatsApp chats, an SMS inbox, incoming notifications or the clipboard in the background. It does not open submitted links or become a default browser. Those integration tasks belong to Phase 3.

## Files

- `output/ScamCheck-debug.apk`: signed debug/test build.
- `output/ScamCheck-home.png`, `output/ScamCheck-result.png`: synthetic native UI previews.
- `app/src/main`: native app code, manifest and resources.
- `core/src/main/java/in/scamcheck/core/AssistedChecker.java`: optional model integration with safe fallback.
- `model/MODEL_CARD.md`: model implementation, data provenance, measured failures and intended use.
- `docs/VALIDATION.md`: build, UI, emulator and permission evidence.

## Build requirements

Gradle 8.14.3 (checksum-pinned wrapper), Android Gradle Plugin 8.9.2, Android SDK platform/build-tools 35 and JDK 17+ compatible with this Gradle version. This workspace was verified using JDK 24.0.2. Android Studio can open the project root. Build dependencies require internet access on the development computer on first setup; the installed app does not.

The downloaded toolchain is under ignored `.tools/`. No global SDK path, PATH or Java installation was changed. The PowerShell build script uses the local toolchain where available, otherwise the checked-in Gradle wrapper and ANDROID_HOME/conventional SDK location.

## Remaining limits

The model is a small synthetic English classifier, not a production fraud detector. New phrasing, unknown organisations, mixed languages and multi-message scams need further data and testing. Offline URL checks cannot establish live website safety. Strict/uncertain URL parsing is used because this version does not launch a browser or claim browser-equivalent interpretation of every unusual URL.
