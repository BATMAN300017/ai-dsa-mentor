import http from "node:http";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import {
  ApiClientError,
  clearLocalSessionToken,
  configureApiBaseUrl,
} from "../src/services/apiClient";
import { healthService } from "../src/services/healthService";
import { modelService } from "../src/services/modelService";

describe("Frontend API integration against local mock HTTP server", () => {
  let server: http.Server;
  let activeToken = "tok-initial";
  let modelLoaded = false;
  let downloadStatus = "NOT_DOWNLOADED";

  beforeAll(async () => {
    server = http.createServer(async (req, res) => {
      const url = req.url ?? "/";
      const method = req.method ?? "GET";

      if (method === "GET" && url === "/api/v1/security/handshake") {
        if (req.headers["x-mentor-client"] !== "ai-dsa-mentor-ui") {
          res.writeHead(403, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ message: "Missing required local UI client header" }));
          return;
        }
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ headerName: "X-Mentor-Local-Token", token: activeToken }));
        return;
      }

      if (method === "GET" && url === "/api/v1/health") {
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(
          JSON.stringify({
            status: "UP",
            application: "AI DSA Mentor",
            version: "0.1.0",
            modelLoaded,
          })
        );
        return;
      }

      if (method === "GET" && url === "/api/v1/model/status") {
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(
          JSON.stringify({
            modelName: "Qwen2.5-Coder-3B-Instruct",
            modelProvider: "Qwen (Alibaba Cloud)",
            huggingFaceRepo: "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
            huggingFaceUrl: "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
            expectedQuantization: "Q4_K_M",
            expectedFileFormat: "GGUF",
            expectedFilename: "qwen2.5-coder-3b-instruct-q4_k_m.gguf",
            expectedSizeBytes: 2104932800,
            revision: "f74adce6aa16316c625447af059dbebe4983757c",
            runtimeTarget: "llama.cpp",
            storageLocationHint: "AppData/AI DSA Mentor/models/qwen2.5-coder-3b",
            downloadStatus,
            checksumVerificationStatus:
              downloadStatus === "VERIFIED" ? "VERIFIED_AGAINST_UPSTREAM" : "NOT_CHECKED",
            upstreamChecksum: "724fb256bec1ff062b2f65e4569e871ad2e95ab2a3989723d1769c54294730b7",
            upstreamChecksumSource: "HF LFS",
            localCalculatedChecksum: null,
            binaryPresentOnDisk: downloadStatus === "VERIFIED",
            resumablePartPresent: false,
            bytesDownloaded: downloadStatus === "VERIFIED" ? 2104932800 : 0,
            totalBytes: 2104932800,
            progressPercent: downloadStatus === "VERIFIED" ? 100 : 0,
            bytesPerSecond: 0,
            actualSizeBytes: downloadStatus === "VERIFIED" ? 2104932800 : 0,
            downloadError: null,
            runtimeAvailable: true,
            runtimeSource: "app-data",
            runtimeStatus: modelLoaded ? "READY" : "MODEL_NOT_LOADED",
            modelLoaded,
            modelLoading: false,
            runtimePort: modelLoaded ? 19001 : 0,
            contextSize: 4096,
            threads: 6,
            gpuLayers: 0,
            runtimeError: null,
          })
        );
        return;
      }

      if (method === "POST") {
        if (req.headers["x-mentor-local-token"] !== activeToken) {
          res.writeHead(401, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ message: "Missing or invalid local session token" }));
          return;
        }

        const chunks: Buffer[] = [];
        for await (const chunk of req) {
          chunks.push(Buffer.from(chunk));
        }
        const rawBody = Buffer.concat(chunks).toString("utf8");
        const parsed = rawBody ? (JSON.parse(rawBody) as Record<string, unknown>) : {};

        if (url === "/api/v1/model/download") {
          downloadStatus = "VERIFIED";
          res.writeHead(200, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ downloadStatus, modelLoaded, runtimeStatus: "MODEL_NOT_LOADED" }));
          return;
        }

        if (url === "/api/v1/model/load") {
          if (downloadStatus !== "VERIFIED") {
            res.writeHead(409, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ message: "Model must be downloaded before loading." }));
            return;
          }
          modelLoaded = true;
          res.writeHead(200, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ downloadStatus, modelLoaded, runtimeStatus: "READY" }));
          return;
        }

        if (url === "/api/v1/model/unload") {
          modelLoaded = false;
          res.writeHead(200, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ downloadStatus, modelLoaded, runtimeStatus: "MODEL_NOT_LOADED" }));
          return;
        }

        if (url === "/api/v1/ai/infer") {
          if (!modelLoaded) {
            res.writeHead(503, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ message: "Local model is not loaded." }));
            return;
          }
          res.writeHead(200, { "Content-Type": "application/json" });
          res.end(
            JSON.stringify({
              text: `Echo from local model: ${String(parsed.userPrompt ?? "")}`,
              modelName: "Qwen2.5-Coder-3B-Instruct",
              latencyMillis: 42,
              promptTokens: 10,
              completionTokens: 8,
              finishReason: "stop",
            })
          );
          return;
        }
      }

      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ message: "Not found" }));
    });

    await new Promise<void>((resolve) => {
      server.listen(0, "127.0.0.1", () => resolve());
    });
    const address = server.address();
    if (address && typeof address === "object") {
      configureApiBaseUrl(`http://127.0.0.1:${address.port}`);
    }
  });

  afterAll(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((err) => (err ? reject(err) : resolve()));
    });
  });

  beforeEach(() => {
    clearLocalSessionToken();
    modelLoaded = false;
    downloadStatus = "NOT_DOWNLOADED";
    activeToken = "tok-initial";
  });

  it("performs security handshake, surfaces backend errors, and executes load/infer/unload", async () => {
    const initialHealth = await healthService.getHealth();
    expect(initialHealth.modelLoaded).toBe(false);

    const initialStatus = await modelService.getModelStatus();
    expect(initialStatus.downloadStatus).toBe("NOT_DOWNLOADED");

    await expect(modelService.loadModel()).rejects.toThrowError(ApiClientError);

    const downloaded = await modelService.startDownload();
    expect(downloaded.downloadStatus).toBe("VERIFIED");

    // Simulate backend restart issuing a new token; apiClient should transparently re-handshake once
    activeToken = "tok-after-restart";
    const loaded = await modelService.loadModel({ contextSize: 4096, threads: 6, gpuLayers: 0 });
    expect(loaded.modelLoaded).toBe(true);

    const inferRes = await modelService.infer({
      userPrompt: "Test O(log n)",
      maxTokens: 64,
      temperature: 0.2,
    });
    expect(inferRes.text).toContain("Test O(log n)");
    expect(inferRes.latencyMillis).toBe(42);

    const unloaded = await modelService.unloadModel();
    expect(unloaded.modelLoaded).toBe(false);
  });
});
