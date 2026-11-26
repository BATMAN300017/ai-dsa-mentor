export interface LocalHealthPayload {
  status: string;
  application: string;
  version: string;
  modelLoaded: boolean;
}

export type ExtensionMessage =
  | { type: "PING_HEALTH" };

export type ExtensionResponse =
  | { ok: true; health: LocalHealthPayload }
  | { ok: false; error: string };
