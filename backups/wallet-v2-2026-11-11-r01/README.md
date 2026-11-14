# Wallet V2 archive

Frozen accepted V2 at tag `wallet-v2-2026-11-11-r01` (commit `8cc58c7`). Includes searchable category selection, blue transfer badges, and transaction-type CSV export.

- `wallet-v2-source.zip`: committed source and documentation; Android project is in `repository/`.
- `wallet-v2-history.bundle`: self-contained Git history through the V2 tag. Restore with `git clone wallet-v2-history.bundle restored-wallet-v2` and check out the V2 tag.
- `wallet-v2-debug.apk`: installable debug build.
- `wallet-v2-release-unsigned.apk`: optimized release; requires signing before installation.
- `MANIFEST.json` and `SHA256SUMS.txt`: archive identity and integrity checks.

Created before the first V3 build. Does not include credentials, signing keys, tool caches, or personal wallet data. The existing V1 archive remains separate.
