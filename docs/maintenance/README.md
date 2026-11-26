# AI DSA Mentor — Autonomous Daily Maintenance System

## Schedule
- **Frequency**: Every day (including weekends) at **06:00 AM IST** (`Asia/Kolkata`, UTC+05:30).
- **Cron**: `30 0 * * *` (00:30 UTC) in [daily-maintenance.yml](../../.github/workflows/daily-maintenance.yml).
- **Cloud Execution**: Runs on GitHub Actions (`ubuntu-latest`), so **your computer does not need to be switched on**.

## Architecture & Limitations
- **GitHub Actions** handles the daily unattended schedule, repository checkout, full build/test verification (Java 21/Maven, React/TypeScript frontend, browser extension), dated audit logging, branch creation (`maintenance/daily-audit-YYYY-MM-DD`), and PR creation.
- **Antigravity Desktop Agent**: GitHub Actions cannot directly launch a local Antigravity desktop agent. Deep interactive AI bug-fixing sessions are run on-demand via Antigravity when you are at your machine.
- **CodeRabbit**: Automatically reviews maintenance PRs on GitHub when the CodeRabbit GitHub App is installed on `BATMAN300017/ai-dsa-mentor`.

## Safety & Human Approval
- Never pushes directly to `main`.
- Never merges or approves its own PRs.
- Every PR requires manual review and approval by `@BATMAN300017`.

## Audit Logs
- [2026-11-26](./2026-11-26.md) — Initial daily maintenance system setup.
