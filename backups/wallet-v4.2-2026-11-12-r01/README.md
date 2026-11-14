# Wallet V4.2 archive

Frozen V4.2 at tag wallet-v4.2-2026-11-12-r01, commit ed1f827008eecd4c1ea6f6ff9a55477d1a04bb4a.

- wallet-v4.2-source.zip: committed source and documentation; open repository/ in Android Studio.
- wallet-v4.2-history.bundle: self-contained history. Clone the bundle and check out the tag above to restore.
- wallet-v4.2-debug.apk: installable debug build.
- wallet-v4.2-release-unsigned.apk: R8-optimized release; requires signing.
- MANIFEST.json and SHA256SUMS.txt: source identity, validation and archive integrity.

Includes visual refinements, search dismissal, account icons, schema-3 migration and backward-compatible backup restore. See V4.2-PLAN.md in the source ZIP for detailed validation. All 23 unit and 38 Android tests passed; three UI tests also passed at 130% text and on the final packaged APK. Debug/release builds and lint passed (zero errors, 54 warnings).

Excludes credentials, signing keys, caches and personal wallet data. Earlier archives remain separate.
