export type BackendConnectionState = "connecting" | "connected" | "unavailable";

export type ModelDownloadStatus =
  | "NOT_DOWNLOADED"
  | "DOWNLOADING"
  | "VERIFYING"
  | "VERIFIED"
  | "CANCELLED"
  | "FAILED"
  | "MISSING_OR_CORRUPT";

export type ChecksumVerificationStatus =
  | "NOT_CHECKED"
  | "VERIFIED_AGAINST_UPSTREAM"
  | "UNVERIFIED_NO_UPSTREAM_CHECKSUM"
  | "MISMATCH";

export type InferenceRuntimeStatus =
  | "UNINITIALIZED"
  | "RUNTIME_MISSING"
  | "MODEL_NOT_LOADED"
  | "STARTING"
  | "READY"
  | "UNLOADING"
  | "ERROR";

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

export interface ModelStatusResponse {
  modelName: string;
  modelProvider: string;
  huggingFaceRepo: string;
  huggingFaceUrl: string;
  expectedQuantization: string;
  expectedFileFormat: string;
  expectedFilename: string;
  expectedSizeBytes: number;
  revision: string;
  runtimeTarget: string;
  storageLocationHint: string;
  downloadStatus: ModelDownloadStatus;
  checksumVerificationStatus: ChecksumVerificationStatus;
  upstreamChecksum: string | null;
  upstreamChecksumSource: string | null;
  localCalculatedChecksum: string | null;
  binaryPresentOnDisk: boolean;
  resumablePartPresent: boolean;
  bytesDownloaded: number;
  totalBytes: number;
  progressPercent: number;
  bytesPerSecond: number;
  actualSizeBytes: number;
  downloadError: string | null;
  runtimeAvailable: boolean;
  runtimeSource: string | null;
  runtimeStatus: InferenceRuntimeStatus;
  modelLoaded: boolean;
  modelLoading: boolean;
  runtimePort: number;
  contextSize: number;
  threads: number;
  gpuLayers: number;
  runtimeError: string | null;
}

export interface ModelDownloadRequest {
  forceRedownload?: boolean;
}

export interface ModelLoadRequest {
  contextSize?: number;
  threads?: number;
  gpuLayers?: number;
}

export interface AiInferRequest {
  systemPrompt?: string;
  userPrompt: string;
  maxTokens?: number;
  temperature?: number;
  topP?: number;
}

export interface AiInferResponse {
  text: string;
  modelName: string;
  latencyMillis: number;
  promptTokens: number | null;
  completionTokens: number | null;
  finishReason: string | null;
}

export interface SecurityHandshakeResponse {
  headerName: string;
  token: string;
}
