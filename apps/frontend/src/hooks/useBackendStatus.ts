import { useCallback, useEffect, useState } from "react";
import { healthService } from "../services/healthService";
import { modelService } from "../services/modelService";
import type {
  BackendConnectionState,
  HealthResponse,
  ModelStatusResponse,
  SystemStatusResponse,
} from "../types/api";

export interface BackendStatusSnapshot {
  connectionState: BackendConnectionState;
  health: HealthResponse | null;
  systemStatus: SystemStatusResponse | null;
  modelStatus: ModelStatusResponse | null;
  errorMessage: string | null;
  lastCheckedAt: string | null;
  refresh: () => Promise<void>;
}

const IDLE_POLL_INTERVAL_MS = 10000;
const ACTIVE_POLL_INTERVAL_MS = 1000;

export function useBackendStatus(): BackendStatusSnapshot {
  const [connectionState, setConnectionState] = useState<BackendConnectionState>("connecting");
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [systemStatus, setSystemStatus] = useState<SystemStatusResponse | null>(null);
  const [modelStatus, setModelStatus] = useState<ModelStatusResponse | null>(null);
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

      const [systemResult, modelResult] = await Promise.allSettled([
        healthService.getSystemStatus(),
        modelService.getModelStatus(),
      ]);
      if (systemResult.status === "fulfilled") {
        setSystemStatus(systemResult.value);
      }
      if (modelResult.status === "fulfilled") {
        setModelStatus(modelResult.value);
      }
    } catch (error) {
      setHealth(null);
      setSystemStatus(null);
      setModelStatus(null);
      setConnectionState("unavailable");
      setErrorMessage(error instanceof Error ? error.message : "Backend unavailable");
      setLastCheckedAt(new Date().toLocaleTimeString());
    }
  }, []);

  const isActiveTransition =
    modelStatus?.downloadStatus === "DOWNLOADING" ||
    modelStatus?.downloadStatus === "VERIFYING" ||
    modelStatus?.modelLoading === true;

  useEffect(() => {
    void checkBackend(true);
  }, [checkBackend]);

  useEffect(() => {
    const intervalMs = isActiveTransition ? ACTIVE_POLL_INTERVAL_MS : IDLE_POLL_INTERVAL_MS;
    const interval = window.setInterval(() => {
      void checkBackend(false);
    }, intervalMs);

    return () => window.clearInterval(interval);
  }, [checkBackend, isActiveTransition]);

  const refresh = useCallback(async () => {
    await checkBackend(false);
  }, [checkBackend]);

  return {
    connectionState,
    health,
    systemStatus,
    modelStatus,
    errorMessage,
    lastCheckedAt,
    refresh,
  };
}
