# Phase 3: privacy and practical integration

Satark 0.3.0 adds practical Android connections without uploading messages. The app supports Android 8+ and has no INTERNET permission. Install `output/ScamCheck-debug.apk`; this is a development APK.

## Try the MVP

1. Open **Connections, history and updates**. Select WhatsApp and/or WhatsApp Business.
2. Enable preview checks, read the disclosure and agree. On Android 13+, allow warnings. Enable Satark in Android's notification access screen. You can revoke access there or turn monitoring off in Satark.
3. New selected notification body previews are checked with rules only. Strong evidence produces a generic warning. Tap it to see reasons; details expire after five minutes or when the process stops. Preview text is never stored in history.
4. Share/paste a message whenever a notification hides text or no warning appears. No warning does not prove safety.
5. Choose ScamCheck from Android's offered apps for a web link, or share it. Check it manually. A simple HTTP(S) link can be opened in an external app only after a separate risk/privacy confirmation and app choice. This opening uses that other app and can disclose the URL.
6. Optional encrypted history requires a separate agreement and is off by default. Run a manual check, then review its summary in settings. Delete one summary or all summaries. Turning history off deletes its file and key.
7. Copy `ScamCheck-update-v1.scu` and `ScamCheck-update-v2.scu` to the phone, then choose **Import signed update** in settings. Import v1, then v2. **Restore previous data** restores v1; reimporting v1/v2 is rejected after v2 was accepted. These demonstration packets contain the same prototype data and expire 90 days after publication.

## Notification limits

Android notification access grants broad access. Satark filters the originating package before reading body extras, and UI choices are limited to WhatsApp and WhatsApp Business. It skips group summaries, ignores titles/sender names, caps input, uses a bounded in-memory deduplication cache and rate-limits alerts. It neither reads chat databases nor collects old notifications. No SMS permission or accessibility service is used.

Notification content is partial: hidden previews, missing extras, grouped messages, Android redaction, disabled warning channels and device power controls can prevent useful warnings. All preview results say only a preview was checked. A notification being clicked is not a universal interception point. Android may route verified links to another app, and WhatsApp can open links internally. Satark is not registered as the default browser and makes no App Links ownership claim. Share/paste remains available.

## Storage and updates

- Manual summaries contain time, outcome, method, website host/domain addresses and reason codes. No original message, path/query or complete link is saved. Addresses can still reveal interests, which the consent explains.
- AES-256-GCM authenticates/encrypts summaries with an Android Keystore key and fresh IV. Hardware backing depends on the device. Storage is excluded from backup, limited to 50 entries and bounded in size; tampered data is rejected. Screens/dialogs use FLAG_SECURE. Keyboard and operating-system behaviour are outside the app's control.
- Update envelopes use RSA-2048/SHA-256 signatures verified by a bundled public key. They validate size, strict metadata, creation/expiry dates, monotonic version, allowed archive entries, data structure and model format before activation. No executable code or arbitrary paths are accepted.
- Atomic state changes keep the previous verified bundle. Rollback checks signature and expiry again and retains the highest accepted version. Expired/invalid active data falls back to bundled data. Clock-based expiry assumes the device clock is sensible; reinstall/clearing app storage resets the local replay floor.
- The private development publisher key is ignored under `.tools/publisher/private.pk8` and is never packaged. The public key is `core/src/main/resources/update_public_key.der`. Production needs a separately managed publisher authority and reviewed data; a valid signature establishes source/integrity, not detection accuracy. The demo publisher refuses to silently replace an existing public key if its private key is missing.
- No real malicious-domain feed is bundled. The small brand list, pinned PSL and synthetic local model retain their documented provenance and limitations. AI stays off by default after synthetic false-warning findings.

## Implementation and verification

Source includes `PreviewListener`, `PrivacyActivity`, `LinkGate`, `EncryptedHistory`, `UpdateStore` and `SignedUpdate`. The only requested app permission is POST_NOTIFICATIONS. The exported listener is guarded by Android's BIND_NOTIFICATION_LISTENER_SERVICE permission; settings are not exported. MainActivity accepts supported share/text/web actions and never checks received content without manual confirmation, except the explicitly opted-in preview worker.

The separate `fixture` module is a test-only notification sender with synthetic text. It is not part of the delivered APK. Instrumentation resets its own test data and toggles notification access only inside the emulator test to replace the binding killed by instrumentation startup. Production always uses Android's user consent screen. Android tests cover actual Keystore encryption, ciphertext tampering, signed activation, tamper rejection, rollback/replay floor, routing and real notification delivery. See `VALIDATION.md` for final counts and artifact hashes.

Physical WhatsApp/Business behaviour, Android 13+ permission flow on hardware, manufacturer power management, battery use, real-world scam accuracy and distribution review remain Phase 4/device validation work. This build does not claim Google Play approval or reliable prevention of every scam.

Platform references: [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService), [Android notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission), [Android link handling](https://developer.android.com/training/app-links), [Android Keystore](https://developer.android.com/privacy-and-security/keystore).
