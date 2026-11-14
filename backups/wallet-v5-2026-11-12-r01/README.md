# Wallet V5 backup

Version 5.0, code 11. Created 12 November 2026.

Source/history commit: `f4e4b28d649475568a76397eed5081c650b18df2`.
Git tag: `wallet-v5-2026-11-12-r01`.

Includes the source ZIP, standalone Git history bundle, debug APK, unsigned R8 release APK, release mapping, build log, and SHA-256 checksums. The release APK requires signing before installation; the debug APK is installable.

Extract the source ZIP and open `wallet-v5/repository/` in Android Studio. Credentials, local wallet data, SDK settings, and build caches are excluded. Existing version backups are preserved.

Validation: 25 JVM tests and all 61 Android cases passed across the accepted implementation's regression runs. Extra 320 dp / 200% text checks passed in both themes. The V5 version-stamp build passed JVM tests, debug/release builds, and lint; both APK manifests were verified as version 5.0 / code 11.
