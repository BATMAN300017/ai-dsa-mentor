import { fetchJson, postJson } from "./apiClient";
import type {
  AiInferRequest,
  AiInferResponse,
  ModelDownloadRequest,
  ModelLoadRequest,
  ModelStatusResponse,
} from "../types/api";

export const modelService = {
  getModelStatus(): Promise<ModelStatusResponse> {
    return fetchJson<ModelStatusResponse>("/api/v1/model/status");
  },

  startDownload(request: ModelDownloadRequest = {}): Promise<ModelStatusResponse> {
    return postJson<ModelDownloadRequest, ModelStatusResponse>("/api/v1/model/download", request, 15000);
  },

  cancelDownload(): Promise<ModelStatusResponse> {
    return postJson<undefined, ModelStatusResponse>("/api/v1/model/download/cancel", undefined, 15000);
  },

  loadModel(request: ModelLoadRequest = {}): Promise<ModelStatusResponse> {
    return postJson<ModelLoadRequest, ModelStatusResponse>("/api/v1/model/load", request, 20000);
  },

  unloadModel(): Promise<ModelStatusResponse> {
    return postJson<undefined, ModelStatusResponse>("/api/v1/model/unload", undefined, 15000);
  },

  infer(request: AiInferRequest): Promise<AiInferResponse> {
    return postJson<AiInferRequest, AiInferResponse>("/api/v1/ai/infer", request, 130000);
  },
};
