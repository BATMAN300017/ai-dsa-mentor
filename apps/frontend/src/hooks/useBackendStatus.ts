import { useCallback, useEffect, useState } from "react";
import { healthService } from "../services/healthService";
import type { BackendConnectionState, HealthResponse, SystemStatusResponse } from "../types/api";

export interface BackendStatusSnapshot {
  connectionState: BackendConnectionState;
  health: HealthResponse | null;
  systemStatus: SystemStatusResponse | null;
  errorMessage: string | null;
  lastCheckedAt: string | null;
  refresh: () => Promise<void>;
}

const POLL_INTERVAL_MS = 10000;

export function useBackendStatus(): BackendStatusSnapshot {
  const [connectionState, setConnectionState] = useState<BackendConnectionState>("connecting");
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [systemStatus, setSystemStatus] = useState<SystemStatusResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [lastCheckedAt, setLastCheckedAt] = useState<string | null>(null);

  const checkBackend = useCallback(async (showConnecting: boolean) => {
    if (showConnecting) {
      setConnectionState("connecting");
    }

    try {
      const healthData = await healthService.getHealth();
      setHealth(healthData);
      setConnectionState("connected");
      setErrorMessage(null);
      setLastCheckedAt(new Date().toLocaleTimeString());

      try {
        const systemData = await healthService.getSystemStatus();
        setSystemStatus(systemData);
      } catch {
        // Keep connected state when /api/v1/health succeeds even if extended status fails
      }
    } catch (error) {
      setHealth(null);
      setSystemStatus(null);
      setConnectionState("unavailable");
      setErrorMessage(error instanceof Error ? error.message : "Backend unavailable");
      setLastCheckedAt(new Date().toLocaleTimeString());
    }
  }, []);

  useEffect(() => {
    void checkBackend(true);
    const interval = window.setInterval(() => {
      void checkBackend(false);
    }, POLL_INTERVAL_MS);

    return () => window.clearInterval(interval);
  }, [checkBackend]);

  const refresh = useCallback(async () => {
    await checkBackend(true);
  }, [checkBackend]);

  return {
    connectionState,
    health,
    systemStatus,
    errorMessage,
    lastCheckedAt,
    refresh,
  };
}
