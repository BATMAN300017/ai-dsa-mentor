# Installation Guide

## Prerequisites

- **Java**: JDK 21 (LTS) or newer (e.g., Eclipse Temurin 21, with `jpackage` included)
- **Maven**: Apache Maven 3.9+ (or use the included `./mvnw` / `mvnw.cmd` Maven Wrapper)
- **Node.js**: Node.js 20+ and npm 10+

## Quick Development Setup

### Windows (PowerShell)

```powershell
.\scripts\setup-dev.ps1
```

### macOS / Linux

```bash
chmod +x scripts/*.sh
./scripts/setup-dev.sh
```

## Running Each Component (Task 1)

1. **Start the Spring Boot Backend** (`http://127.0.0.1:8787`):
   ```bash
   ./mvnw -pl apps/backend spring-boot:run
   ```
2. **Start the React Frontend** (`http://127.0.0.1:5173`):
   ```bash
   cd apps/frontend
   npm run dev
   ```
3. **Run the JavaFX Desktop Application**:
   ```bash
   ./mvnw -pl apps/desktop -am javafx:run
   ```
4. **Load the Chrome Extension Scaffold**:
   - Build extension: `cd apps/browser-extension && npm install && npm run build`
   - Open `chrome://extensions`, enable **Developer mode**, and click **Load unpacked** pointing to `apps/browser-extension/dist` (or `apps/browser-extension` after building).

## Desktop Installers (Planned)

Full OS installers (`.msi`, `.dmg`, `.deb`) via `jpackage` are reserved for a later task under `installer/`.
