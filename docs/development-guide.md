# Development Guide

## Build & Verification Commands

### Build entire Java workspace & run JUnit 5 tests

```bash
./mvnw clean verify
```

### Build only the backend or desktop module

```bash
./mvnw -pl apps/backend -am test
./mvnw -pl apps/desktop -am compile
```

### Frontend commands (`apps/frontend`)

```bash
npm install
npm run typecheck
npm run build
npm run dev
```

### Browser extension commands (`apps/browser-extension`)

```bash
npm install
npm run validate
npm run build
```

## Coding Rules

1. Keep controllers thin — place all business logic in `@Service` classes or domain modules under `modules/`.
2. Never duplicate service classes across `apps/backend` and `modules/`.
3. Never commit model binaries (`*.gguf`, `*.bin`) or large native runtimes (`llama.cpp` binaries) to Git.
4. Keep the backend strictly bound to `127.0.0.1` with explicit local origin allowlists.
