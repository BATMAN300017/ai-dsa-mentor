import type { BackendConnectionState } from "../types/api";

interface BackendBadgeProps {
  connectionState: BackendConnectionState;
  onRefresh?: () => void;
}

export function BackendBadge({ connectionState, onRefresh }: BackendBadgeProps) {
  const label =
    connectionState === "connected"
      ? "Backend: Connected"
      : connectionState === "connecting"
        ? "Backend: Connecting"
        : "Backend unavailable";

  const dotClass =
    connectionState === "connected"
      ? "bg-secondary"
      : connectionState === "connecting"
        ? "bg-primary animate-pulse"
        : "bg-error";

  const textClass =
    connectionState === "connected"
      ? "text-secondary"
      : connectionState === "connecting"
        ? "text-primary"
        : "text-error";

  return (
    <button
      type="button"
      onClick={onRefresh}
      title="Check local Spring Boot backend status (/api/v1/health)"
      className="inline-flex items-center gap-1.5 px-space-sm py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-container-highest transition-colors font-label-code text-label-code"
    >
      <span className={`w-2 h-2 rounded-full ${dotClass}`} />
      <span className={textClass}>{label}</span>
    </button>
  );
}
