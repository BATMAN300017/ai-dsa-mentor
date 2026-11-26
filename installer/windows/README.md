# Windows Installer Packaging (`jpackage`)

Reserved for Task 2+ desktop installer generation (`.msi` / `.exe`) via JDK `jpackage`.

Notes for future packaging:
- Bundle a custom JRE via `jlink` / `jpackage` targeting Java 21+.
- Installed binaries reside under `Program Files` (read-only); models and runtime state must live in `%LOCALAPPDATA%\AI DSA Mentor`.
