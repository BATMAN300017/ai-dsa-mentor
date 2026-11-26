import { fetchLocalHealth } from "../messaging/localBridge.js";
import type { ExtensionMessage, ExtensionResponse } from "../messaging/protocol.js";

chrome.runtime.onInstalled.addListener(() => {
  // Foundation lifecycle hook; platform content injection belongs to Task 2+.
});

chrome.runtime.onMessage.addListener(
  (message: ExtensionMessage, _sender, sendResponse: (res: ExtensionResponse) => void) => {
    if (message?.type === "PING_HEALTH") {
      fetchLocalHealth()
        .then((health) => sendResponse({ ok: true, health }))
        .catch((error: unknown) =>
          sendResponse({
            ok: false,
            error: error instanceof Error ? error.message : "Backend unavailable",
          })
        );
      return true;
    }
    return false;
  }
);
