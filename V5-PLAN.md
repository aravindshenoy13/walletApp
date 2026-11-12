# Wallet V5 release

Accepted on 12 November 2026. V5 names the state, UI, and logical bug-fix work already accepted in this chat; it adds no new product features.

## Included work

- Explicit backup/restore and search state handling; work and confirmation survive Activity recreation.
- Restored export account/type selections, safe missing-selection handling, consistent backup/restore size limits, explicit timestamp-precision validation, and retry for failed reads.
- Analysis default-date refresh, stable filtered-record gestures, current-category indication, device clock formatting, chart accessibility, wheel scrolling, and responsive large-text/amount layouts.
- Per-finding resolutions and verification boundaries remain in [UI-BUGS.md](UI-BUGS.md) and [LOGICAL-BUGS.md](LOGICAL-BUGS.md).

## Version and compatibility

- Android version name: `5.0`; version code: `11`.
- Application ID remains `com.aravind.wallet`; Room schema remains 3.
- Full-backup format remains 2, with format-1 restore compatibility. The 20 MiB limit applies to both creation and restore. Fractional-second backup timestamps are explicitly rejected rather than silently truncated.
- Release builds retain R8 optimization and resource shrinking. The release APK is unsigned and requires signing for installation.

## Validation

The accepted implementation passed 25 JVM tests and all 61 Android cases across the regression run and isolated reruns. Additional 320 dp / 200% text checks passed in both themes, with screenshots inspected. Lint reported zero errors and 54 warnings. Detailed boundaries, including simulated restored export parameters and programmatic accessibility checks, are recorded in the bug reports.

The V5 version-stamp build passed `testDebugUnitTest assembleDebug assembleRelease lint --offline`. Both APK manifests were verified as version 5.0 / code 11. Source and artifact checksums are included with the standalone backup.

## Backup

- Git tag: `wallet-v5-2026-11-12-r01`.
- Standalone backup: `backups/wallet-v5-2026-11-12-r01/`.
- Include source ZIP, standalone Git history bundle, V5 debug APK, unsigned optimized release APK, and SHA-256 checksums.
- Source/history archives exclude ignored credentials, local device data, build caches, and generated APKs. APKs are provided separately in the backup folder.
- Earlier backups are preserved. This release follows the user's explicit request to use the existing top-level `backups/` folder. App source and working build output remain in `repository/`.
