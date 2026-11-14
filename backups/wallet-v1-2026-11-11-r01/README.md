# Wallet V1 Backup

Accepted V1 snapshot saved on 11 November 2026, before V2 planning.

- Commit: `3908f31e3e7c9291023cc0a035826b563a246005`
- Git tag: `wallet-v1-2026-11-11-r01`
- App version: `1.0` with version code `1`

## Contents

- `wallet-v1-source.zip` contains all 54 tracked files, including app source, Gradle wrapper, tests, roadmap, instructions, and wireframes. Its source files were verified against the accepted Git commit.
- `wallet-v1-history.bundle` contains the complete Git history and the V1 checkpoint tag. An isolated clone and Git object verification both passed.
- `wallet-v1-debug.apk` is the directly installable debug build of this snapshot.
- `wallet-v1-release-unsigned.apk` is the R8 optimized release build. It requires signing before installation.
- `MANIFEST.json` records the snapshot and build checks. `SHA256SUMS.txt` checks the saved files for changes.

The backup excludes credentials, local SDK configuration, signing keys, tool caches, and generated build directories. It does not contain wallet records stored on a phone or emulator.

## Restore source

Extract `wallet-v1-source.zip` into an empty folder and open `wallet-v1/repository/` in Android Studio. Configure the Android SDK location locally, then follow `repository/README.md` to build.

## Restore Git history

Run these commands from this backup directory:

```powershell
git clone wallet-v1-history.bundle WalletApp-v1
git -C WalletApp-v1 switch --detach wallet-v1-2026-11-11-r01
```

Open `WalletApp-v1/repository/` for Android development. The checkout is the accepted V1 version, independent of later V2 changes.

## Verification

Fresh debug and release builds, all 6 JVM tests, and debug lint passed before the APKs were copied here. Lint had no errors. The source archive and Git restore were verified separately; Android device tests were not rerun for this archival task.
