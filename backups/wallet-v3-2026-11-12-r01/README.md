# Wallet V3 archive

Frozen accepted V3 at tag `wallet-v3-2026-11-12-r01`, commit `a9e1814c681e91518fe22855ed5d7ad25e34ab59`.

- `wallet-v3-source.zip`: committed source and documentation. Open its `repository/` directory in Android Studio.
- `wallet-v3-history.bundle`: self-contained Git history and V1/V2/V3 archive tags. Restore with `git clone wallet-v3-history.bundle restored-wallet-v3`, then check out `wallet-v3-2026-11-12-r01`.
- `wallet-v3-debug.apk`: installable debug build.
- `wallet-v3-release-unsigned.apk`: R8-optimized release; requires signing before installation.
- `MANIFEST.json` and `SHA256SUMS.txt`: source identity, validation and file integrity.

Final checks: 22 unit tests passed; lint passed with 0 errors and 34 warnings; debug and optimized release builds passed. Updated Android test flows were compiled during implementation, but the final calendar/shared-mode UI was not device-tested or visually verified. See V3-PLAN.md in the source snapshot for earlier results and their limits.

Includes monthly all-account totals/calendar, independent detailed range/account analysis, one Net/Income/Expenses chooser, category history, and final compact row styling. Excludes uncommitted V4 drafts, credentials, signing keys, caches and personal wallet data. Earlier archives remain separate.
