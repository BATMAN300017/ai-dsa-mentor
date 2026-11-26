export type BackendConnectionState = "connecting" | "connected" | "unavailable";

export interface HealthResponse {
  status: string;
  application: string;
  version: string;
  modelLoaded: boolean;
}

export interface ModelDescriptor {
  modelName: string;
  modelProvider: string;
  huggingFaceRepo: string;
  expectedQuantization: string;
  expectedFileFormat: string;
  expectedFilename: string;
  downloadStatus: string;
  runtimeTarget: string;
  binaryPresentOnDisk: boolean;
}

export interface CapabilityDescriptor {
  id: string;
  name: string;
  state: "FOUNDATION_READY" | "PLANNED" | "IN_DEVELOPMENT" | "NOT_CONFIGURED";
  notes: string;
}

export interface SystemStatusResponse {
  application: string;
  version: string;
  philosophy: string;
  bindScope: string;
  modelLoaded: boolean;
  model: ModelDescriptor;
  capabilities: CapabilityDescriptor[];
}
