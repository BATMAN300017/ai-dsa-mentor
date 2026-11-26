import type { LocalHealthPayload } from "./protocol.js";

const DEFAULT_BACKEND_URL = "http://127.0.0.1:8787";

/**
 * Localhost bridge communicating exclusively with the local AI DSA Mentor backend.
 * Future mutating routes will attach the paired X-Mentor-Local-Token header.
 */
export async function fetchLocalHealth(localToken?: string): Promise<LocalHealthPayload> {
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  if (localToken) {
    headers["X-Mentor-Local-Token"] = localToken;
  }

  const response = await fetch(`${DEFAULT_BACKEND_URL}/api/v1/health`, {
    method: "GET",
    headers,
  });

  if (!response.ok) {
    throw new Error(`Local backend returned HTTP ${response.status}`);
  }

  return (await response.json()) as LocalHealthPayload;
}
