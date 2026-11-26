import { useState } from "react";
import { modelService } from "../services/modelService";
import type {
  AiInferResponse,
  BackendConnectionState,
  ModelStatusResponse,
} from "../types/api";
import { AppIcon } from "./AppIcon";

interface ModelSetupPanelProps {
  connectionState: BackendConnectionState;
  modelStatus: ModelStatusResponse | null;
  onRefresh: () => Promise<void>;
}

function formatBytes(bytes: number): string {
  if (!bytes || bytes <= 0) {
    return "0 B";
  }
  const units = ["B", "KB", "MB", "GB"];
  let value = bytes;
  let unitIdx = 0;
  while (value >= 1024 && unitIdx < units.length - 1) {
    value /= 1024;
    unitIdx += 1;
  }
  return `${value.toFixed(unitIdx >= 2 ? 2 : 0)} ${units[unitIdx]}`;
}

export function ModelSetupPanel({
  connectionState,
  modelStatus,
  onRefresh,
}: ModelSetupPanelProps) {
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const [contextSize, setContextSize] = useState(4096);
  const [threads, setThreads] = useState(6);
  const [gpuLayers, setGpuLayers] = useState(0);

  const [systemPrompt, setSystemPrompt] = useState("You are a helpful local assistant.");
  const [userPrompt, setUserPrompt] = useState("Explain binary search time complexity in two sentences.");
  const [maxTokens, setMaxTokens] = useState(192);
  const [temperature, setTemperature] = useState(0.2);

  const [inferLoading, setInferLoading] = useState(false);
  const [inferError, setInferError] = useState<string | null>(null);
  const [inferResponse, setInferResponse] = useState<AiInferResponse | null>(null);

  const backendConnected = connectionState === "connected";
  const downloadStatus = modelStatus?.downloadStatus ?? "NOT_DOWNLOADED";
  const isDownloading = downloadStatus === "DOWNLOADING" || downloadStatus === "VERIFYING";
  const isVerified = downloadStatus === "VERIFIED";
  const runtimeAvailable = modelStatus?.runtimeAvailable ?? false;
  const modelLoaded = modelStatus?.modelLoaded ?? false;
  const modelLoading = modelStatus?.modelLoading ?? false;

  const runAction = async (fn: () => Promise<unknown>) => {
    setActionBusy(true);
    setActionError(null);
    try {
      await fn();
      await onRefresh();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Operation failed");
    } finally {
      setActionBusy(false);
    }
  };

  const handleInfer = async () => {
    if (!modelLoaded || !userPrompt.trim()) {
      return;
    }
    setInferLoading(true);
    setInferError(null);
    try {
      const result = await modelService.infer({
        systemPrompt: systemPrompt.trim() || undefined,
        userPrompt: userPrompt.trim(),
        maxTokens,
        temperature,
      });
      setInferResponse(result);
    } catch (error) {
      setInferError(error instanceof Error ? error.message : "Inference failed");
      setInferResponse(null);
    } finally {
      setInferLoading(false);
    }
  };

  const checksumBadgeClass =
    modelStatus?.checksumVerificationStatus === "VERIFIED_AGAINST_UPSTREAM"
      ? "text-secondary bg-secondary/10"
      : modelStatus?.checksumVerificationStatus === "MISMATCH"
        ? "text-error bg-error/10"
        : "text-on-surface-variant bg-surface-container";

  return (
    <div className="max-w-4xl mx-auto mt-space-lg bg-surface-container-low rounded-xl shadow-xl overflow-hidden">
      <div className="flex flex-wrap items-center justify-between gap-space-sm bg-surface-container px-space-md py-space-sm">
        <div className="flex items-center gap-space-xs">
          <AppIcon name="memory" className="w-4 h-4 text-primary" />
          <span className="font-headline-sm text-headline-sm text-on-surface">
            Local Model &amp; Runtime Console
          </span>
        </div>
        <div className="flex flex-wrap items-center gap-space-xs font-label-code text-label-code">
          <span className="px-2 py-0.5 rounded bg-surface text-primary">
            {modelStatus?.modelName ?? "Qwen2.5-Coder-3B-Instruct"} ·{" "}
            {modelStatus?.expectedQuantization ?? "Q4_K_M"}
          </span>
          <span
            className={`px-2 py-0.5 rounded ${
              modelLoaded
                ? "bg-secondary/15 text-secondary"
                : modelLoading
                  ? "bg-primary/15 text-primary"
                  : "bg-surface text-on-surface-variant"
            }`}
          >
            {modelLoaded ? "READY" : modelLoading ? "LOADING" : modelStatus?.runtimeStatus ?? "OFFLINE"}
          </span>
        </div>
      </div>

      <div className="p-space-lg space-y-space-lg">
        {actionError && (
          <div
            role="alert"
            className="p-space-sm rounded-lg bg-error-container/40 border border-error/40 text-error font-body-sm text-body-sm flex items-start gap-2"
          >
            <AppIcon name="error" className="w-4 h-4 mt-0.5 flex-shrink-0" />
            <span>{actionError}</span>
          </div>
        )}

        {/* Row 1: Model Download + Runtime Lifecycle */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-space-md">
          {/* Model Installation Card */}
          <div className="bg-surface-container-lowest p-space-md rounded-lg space-y-space-sm">
            <div className="flex items-center justify-between gap-2">
              <span className="font-label-code text-label-code text-secondary uppercase">
                1. Model Weights (GGUF)
              </span>
              <span className="px-2 py-0.5 rounded bg-surface-container text-on-surface font-label-code text-label-code">
                {downloadStatus}
              </span>
            </div>

            <div className="space-y-1 font-body-sm text-body-sm text-on-surface-variant">
              <div className="flex items-center justify-between gap-2">
                <span>File:</span>
                <span className="font-label-code text-label-code text-on-surface truncate">
                  {modelStatus?.expectedFilename ?? "qwen2.5-coder-3b-instruct-q4_k_m.gguf"}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span>Pinned Revision:</span>
                <span className="font-label-code text-label-code text-primary">
                  {modelStatus?.revision ? modelStatus.revision.slice(0, 12) : "f74adce6aa16"}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span>Integrity:</span>
                <span className={`px-1.5 py-0.5 rounded font-label-code text-label-code ${checksumBadgeClass}`}>
                  {modelStatus?.checksumVerificationStatus ?? "NOT_CHECKED"}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span>Size:</span>
                <span className="font-label-code text-label-code text-on-surface">
                  {formatBytes(modelStatus?.actualSizeBytes || modelStatus?.bytesDownloaded || 0)} /{" "}
                  {formatBytes(modelStatus?.totalBytes || modelStatus?.expectedSizeBytes || 2104932800)}
                </span>
              </div>
            </div>

            {(isDownloading || modelStatus?.resumablePartPresent) && (
              <div className="space-y-1 pt-1">
                <div className="w-full h-2 rounded-full bg-surface-container overflow-hidden">
                  <div
                    className="h-full bg-primary transition-all duration-300"
                    style={{ width: `${Math.min(100, Math.max(0, modelStatus?.progressPercent ?? 0))}%` }}
                  />
                </div>
                <div className="flex items-center justify-between font-label-code text-label-code text-on-surface-variant">
                  <span>{(modelStatus?.progressPercent ?? 0).toFixed(1)}%</span>
                  <span>
                    {modelStatus?.bytesPerSecond
                      ? `${formatBytes(modelStatus.bytesPerSecond)}/s`
                      : modelStatus?.resumablePartPresent
                        ? "Resumable .part saved"
                        : ""}
                  </span>
                </div>
              </div>
            )}

            {modelStatus?.localCalculatedChecksum && (
              <div className="p-2 rounded bg-surface-container font-code-sm text-code-sm text-on-surface-variant break-all">
                <span className="text-secondary">Local SHA-256: </span>
                {modelStatus.localCalculatedChecksum}
              </div>
            )}

            {modelStatus?.downloadError && (
              <div className="p-2 rounded bg-error-container/30 text-error font-body-sm text-body-sm">
                {modelStatus.downloadError}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-space-xs pt-1">
              <button
                type="button"
                disabled={!backendConnected || actionBusy || isDownloading || isVerified}
                onClick={() => void runAction(() => modelService.startDownload({ forceRedownload: false }))}
                className="px-space-md py-1.5 rounded-lg bg-primary text-on-primary font-label-code text-label-code hover:bg-primary-fixed-dim disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                {isVerified
                  ? "Model Verified"
                  : isDownloading
                    ? "Downloading..."
                    : modelStatus?.resumablePartPresent
                      ? "Resume Download"
                      : "Download Model"}
              </button>

              {isDownloading && (
                <button
                  type="button"
                  disabled={!backendConnected || actionBusy}
                  onClick={() => void runAction(() => modelService.cancelDownload())}
                  className="px-space-sm py-1.5 rounded-lg bg-surface-container-high text-error font-label-code text-label-code hover:bg-surface-container-highest disabled:opacity-40 transition-colors"
                >
                  Cancel Download
                </button>
              )}
            </div>
          </div>

          {/* Runtime Lifecycle Card */}
          <div className="bg-surface-container-lowest p-space-md rounded-lg space-y-space-sm">
            <div className="flex items-center justify-between gap-2">
              <span className="font-label-code text-label-code text-primary uppercase">
                2. llama.cpp Runtime (CPU-Only)
              </span>
              <span
                className={`px-2 py-0.5 rounded font-label-code text-label-code ${
                  runtimeAvailable ? "bg-secondary/15 text-secondary" : "bg-error/10 text-error"
                }`}
              >
                {runtimeAvailable
                  ? `Found (${modelStatus?.runtimeSource ?? "local"})`
                  : "Runtime Missing"}
              </span>
            </div>

            <div className="grid grid-cols-3 gap-space-xs">
              <label className="flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
                <span>Context</span>
                <input
                  type="number"
                  min={512}
                  max={32768}
                  step={512}
                  value={contextSize}
                  onChange={(e) => setContextSize(Number(e.target.value) || 4096)}
                  className="bg-surface-container px-2 py-1 rounded text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
                />
              </label>
              <label className="flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
                <span>CPU Threads</span>
                <input
                  type="number"
                  min={1}
                  max={64}
                  value={threads}
                  onChange={(e) => setThreads(Number(e.target.value) || 6)}
                  className="bg-surface-container px-2 py-1 rounded text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
                />
              </label>
              <label className="flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
                <span>GPU Layers</span>
                <input
                  type="number"
                  min={0}
                  max={128}
                  value={gpuLayers}
                  onChange={(e) => setGpuLayers(Number(e.target.value) || 0)}
                  className="bg-surface-container px-2 py-1 rounded text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
                />
              </label>
            </div>

            <div className="font-body-sm text-body-sm text-on-surface-variant space-y-1">
              <div className="flex items-center justify-between">
                <span>Readiness:</span>
                <span className="font-label-code text-label-code text-on-surface">
                  {modelLoaded
                    ? `Ready on 127.0.0.1:${modelStatus?.runtimePort ?? 0}`
                    : modelLoading
                      ? "Loading GGUF into RAM..."
                      : "Not Loaded"}
                </span>
              </div>
            </div>

            {modelStatus?.runtimeError && (
              <div className="p-2 rounded bg-error-container/30 text-error font-body-sm text-body-sm">
                {modelStatus.runtimeError}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-space-xs pt-1">
              <button
                type="button"
                disabled={
                  !backendConnected ||
                  actionBusy ||
                  !isVerified ||
                  !runtimeAvailable ||
                  modelLoaded ||
                  modelLoading
                }
                onClick={() =>
                  void runAction(() =>
                    modelService.loadModel({ contextSize, threads, gpuLayers })
                  )
                }
                className="px-space-md py-1.5 rounded-lg bg-secondary text-on-secondary font-label-code text-label-code hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                {modelLoading ? "Loading Model..." : modelLoaded ? "Model Loaded" : "Load Model"}
              </button>

              <button
                type="button"
                disabled={!backendConnected || actionBusy || (!modelLoaded && !modelLoading)}
                onClick={() => void runAction(() => modelService.unloadModel())}
                className="px-space-sm py-1.5 rounded-lg bg-surface-container-high text-on-surface font-label-code text-label-code hover:bg-surface-container-highest disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
              >
                Unload Model
              </button>
            </div>
          </div>
        </div>

        {/* Row 2: Basic Local Inference Test Interface */}
        <div className="bg-surface-container-lowest p-space-md rounded-lg space-y-space-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="font-label-code text-label-code text-tertiary uppercase">
              3. Local Inference Test (Raw Model Verification)
            </span>
            <span className="font-label-code text-label-code text-on-surface-variant">
              {modelLoaded ? "Live llama.cpp endpoint ready" : "Requires loaded local model"}
            </span>
          </div>

          {!modelLoaded && (
            <p className="font-body-sm text-body-sm text-on-surface-variant">
              Download and verify the model, ensure <code>llama-server</code> is installed, and click{" "}
              <strong>Load Model</strong> to run real local CPU inference. No simulated output is ever returned.
            </p>
          )}

          <div className="grid grid-cols-1 md:grid-cols-12 gap-space-sm">
            <label className="md:col-span-8 flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
              <span>System Prompt (Optional)</span>
              <input
                type="text"
                value={systemPrompt}
                onChange={(e) => setSystemPrompt(e.target.value)}
                className="bg-surface-container px-space-sm py-1.5 rounded text-body-sm font-body-sm text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
              />
            </label>
            <label className="md:col-span-2 flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
              <span>Max Tokens</span>
              <input
                type="number"
                min={1}
                max={4096}
                value={maxTokens}
                onChange={(e) => setMaxTokens(Number(e.target.value) || 192)}
                className="bg-surface-container px-2 py-1.5 rounded text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
              />
            </label>
            <label className="md:col-span-2 flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
              <span>Temperature</span>
              <input
                type="number"
                min={0}
                max={2}
                step={0.1}
                value={temperature}
                onChange={(e) => setTemperature(Number(e.target.value))}
                className="bg-surface-container px-2 py-1.5 rounded text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
              />
            </label>
          </div>

          <label className="flex flex-col gap-1 font-label-code text-label-code text-on-surface-variant">
            <span>Test Prompt</span>
            <textarea
              rows={2}
              value={userPrompt}
              onChange={(e) => setUserPrompt(e.target.value)}
              className="bg-surface-container px-space-sm py-2 rounded font-body-sm text-body-sm text-on-surface border border-outline-variant/30 focus:outline-none focus:border-primary"
            />
          </label>

          <div className="flex items-center justify-between gap-2">
            <button
              type="button"
              disabled={!backendConnected || !modelLoaded || inferLoading || !userPrompt.trim()}
              onClick={() => void handleInfer()}
              className="px-space-md py-1.5 rounded-lg bg-primary text-on-primary font-label-code text-label-code hover:bg-primary-fixed-dim disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              {inferLoading ? "Running Local Inference..." : "Run Local Inference"}
            </button>
            {inferResponse && (
              <span className="font-label-code text-label-code text-secondary">
                {inferResponse.latencyMillis} ms
                {inferResponse.completionTokens !== null
                  ? ` · ${inferResponse.completionTokens} tokens`
                  : ""}
              </span>
            )}
          </div>

          {inferError && (
            <div className="p-2 rounded bg-error-container/30 text-error font-body-sm text-body-sm">
              {inferError}
            </div>
          )}

          {inferResponse && (
            <pre className="p-space-sm rounded-lg bg-surface-container font-body-sm text-body-sm text-on-surface whitespace-pre-wrap">
              {inferResponse.text}
            </pre>
          )}
        </div>
      </div>
    </div>
  );
}
