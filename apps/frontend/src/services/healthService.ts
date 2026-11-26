import { fetchJson } from "./apiClient";
import type { HealthResponse, SystemStatusResponse } from "../types/api";

export const healthService = {
  getHealth(): Promise<HealthResponse> {
    return fetchJson<HealthResponse>("/api/v1/health");
  },

  getSystemStatus(): Promise<SystemStatusResponse> {
    return fetchJson<SystemStatusResponse>("/api/v1/system/status");
  },
};
