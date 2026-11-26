import type { SecurityHandshakeResponse } from "../types/api";

function resolveDefaultBaseUrl(): string {
  const metaEnv = (import.meta as { env?: Record<string, string | undefined> }).env;
  return (metaEnv?.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");
}

let apiBaseUrl = resolveDefaultBaseUrl();
let inMemoryToken: string | null = null;
let inMemoryTokenHeader = "X-Mentor-Local-Token";

export function configureApiBaseUrl(baseUrl: string): void {
  apiBaseUrl = baseUrl.replace(/\/+$/, "");
}

export function clearLocalSessionToken(): void {
  inMemoryToken = null;
}

export class ApiClientError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
  }
}

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const data = (await response.json()) as { message?: string };
    if (data && typeof data.message === "string" && data.message.trim().length > 0) {
      return data.message;
    }
  } catch {
    // Fall back to HTTP status string when body is empty or non-JSON
  }
  return `Backend responded with HTTP ${response.status}`;
}

export async function getLocalSessionToken(): Promise<{ headerName: string; token: string }> {
  if (inMemoryToken) {
    return { headerName: inMemoryTokenHeader, token: inMemoryToken };
  }

  const controller = new AbortController();
  const timer = globalThis.setTimeout(() => controller.abort(), 4000);
  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/security/handshake`, {
      method: "GET",
      headers: {
        Accept: "application/json",
        "X-Mentor-Client": "ai-dsa-mentor-ui",
      },
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new ApiClientError(await parseErrorMessage(response), response.status);
    }

    const payload = (await response.json()) as SecurityHandshakeResponse;
    inMemoryToken = payload.token;
    inMemoryTokenHeader = payload.headerName || "X-Mentor-Local-Token";
    return { headerName: inMemoryTokenHeader, token: inMemoryToken };
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiClientError("Security handshake timed out");
    }
    throw new ApiClientError(error instanceof Error ? error.message : "Network error");
  } finally {
    globalThis.clearTimeout(timer);
  }
}

export async function fetchJson<T>(path: string, timeoutMs = 4000): Promise<T> {
  const controller = new AbortController();
  const timer = globalThis.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new ApiClientError(await parseErrorMessage(response), response.status);
    }

    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiClientError("Backend request timed out");
    }
    throw new ApiClientError(error instanceof Error ? error.message : "Network error");
  } finally {
    globalThis.clearTimeout(timer);
  }
}

export async function postJson<TReq, TRes>(
  path: string,
  body?: TReq,
  timeoutMs = 15000,
  retryOnUnauthorized = true
): Promise<TRes> {
  const { headerName, token } = await getLocalSessionToken();
  const controller = new AbortController();
  const timer = globalThis.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const headers: Record<string, string> = {
      Accept: "application/json",
      [headerName]: token,
    };
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${apiBaseUrl}${path}`, {
      method: "POST",
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    if (response.status === 401 && retryOnUnauthorized) {
      clearLocalSessionToken();
      return postJson<TReq, TRes>(path, body, timeoutMs, false);
    }

    if (!response.ok) {
      throw new ApiClientError(await parseErrorMessage(response), response.status);
    }

    return (await response.json()) as TRes;
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiClientError("Backend request timed out");
    }
    throw new ApiClientError(error instanceof Error ? error.message : "Network error");
  } finally {
    globalThis.clearTimeout(timer);
  }
}
