import type { ExtensionResponse } from "../messaging/protocol.js";

const backendStateEl = document.getElementById("backend-state");
const modelStateEl = document.getElementById("model-state");
const refreshBtn = document.getElementById("refresh-btn");

function setBackendStatus(label: string, cssClass: "connected" | "connecting" | "unavailable") {
  if (!backendStateEl) return;
  backendStateEl.textContent = label;
  backendStateEl.className = `value ${cssClass}`;
}

function checkHealth() {
  setBackendStatus("Connecting...", "connecting");
  chrome.runtime.sendMessage({ type: "PING_HEALTH" }, (response: ExtensionResponse | undefined) => {
    if (chrome.runtime.lastError || !response) {
      setBackendStatus("Backend unavailable", "unavailable");
      return;
    }
    if (response.ok) {
      setBackendStatus(`Connected (${response.health.version})`, "connected");
      if (modelStateEl) {
        modelStateEl.textContent = response.health.modelLoaded ? "Loaded" : "Not Loaded";
      }
    } else {
      setBackendStatus("Backend unavailable", "unavailable");
    }
  });
}

refreshBtn?.addEventListener("click", checkHealth);
document.addEventListener("DOMContentLoaded", checkHealth);
