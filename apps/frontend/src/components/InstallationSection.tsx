import { useState } from "react";
import { INSTALL_TABS, type InstallTabKey } from "../data/landingContent";
import type { BackendConnectionState, HealthResponse, SystemStatusResponse } from "../types/api";
import { AppIcon } from "./AppIcon";
import { CopyButton } from "./CopyButton";

interface InstallationSectionProps {
  connectionState: BackendConnectionState;
  health: HealthResponse | null;
  systemStatus: SystemStatusResponse | null;
  lastCheckedAt: string | null;
  onRefresh: () => void;
}

export function InstallationSection({
  connectionState,
  health,
  systemStatus,
  lastCheckedAt,
  onRefresh,
}: InstallationSectionProps) {
  const [selectedTab, setSelectedTab] = useState<InstallTabKey>("win");
  const activeOption = INSTALL_TABS.find((tab) => tab.key === selectedTab) ?? INSTALL_TABS[0];

  const backendCardTitle =
    connectionState === "connected"
      ? "Step 1: Backend Connected"
      : connectionState === "connecting"
        ? "Step 1: Connecting..."
        : "Step 1: Backend Unavailable";

  const backendCardSub =
    connectionState === "connected"
      ? `${health?.application ?? "AI DSA Mentor"} v${health?.version ?? "0.1.0"} on 127.0.0.1`
      : connectionState === "connecting"
        ? "Checking http://127.0.0.1:8787/api/v1/health"
        : "Start Spring Boot backend on port 8787";

  const modelCardSub = systemStatus?.model
    ? `${systemStatus.model.expectedQuantization} · ${systemStatus.model.downloadStatus}`
    : "Qwen2.5-Coder-3B Q4_K_M · NOT_DOWNLOADED";

  return (
    <section className="w-full py-space-xl bg-surface-container-lowest relative" id="installation">
      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col items-center text-center space-y-space-xs mb-space-xl">
          <span className="px-space-sm py-1 rounded bg-surface-container text-secondary font-label-code text-label-code uppercase tracking-wider">
            Quickstart &amp; Local Setup
          </span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">
            Reproducible local setup. Built on Java 21.
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-xl">
            Run the Spring Boot API locally, launch the JavaFX desktop companion, or develop the
            frontend directly against 127.0.0.1:8787.
          </p>
        </div>

        {/* OS Terminal Switcher */}
        <div className="max-w-4xl mx-auto bg-surface-container-low rounded-xl shadow-xl overflow-hidden">
          <div className="flex items-center justify-between bg-surface-container px-space-md py-2 overflow-x-auto gap-space-sm">
            <div className="flex items-center gap-space-xs">
              {INSTALL_TABS.map((tab) => {
                const active = tab.key === selectedTab;
                return (
                  <button
                    key={tab.key}
                    type="button"
                    onClick={() => setSelectedTab(tab.key)}
                    className={`px-space-sm py-1.5 rounded-lg font-label-code text-label-code transition-colors whitespace-nowrap ${
                      active
                        ? "bg-surface text-primary"
                        : "text-on-surface-variant hover:text-on-surface"
                    }`}
                  >
                    {tab.label}
                  </button>
                );
              })}
            </div>
            <span className="hidden sm:inline font-label-code text-label-code text-on-surface-variant whitespace-nowrap">
              JDK 21+ &amp; Node 20+
            </span>
          </div>

          <div className="p-space-lg bg-surface-container-lowest font-code-lg text-code-lg text-on-surface space-y-space-md">
            <div className="space-y-space-xs">
              <div className="flex items-center justify-between gap-space-sm bg-surface-container-low px-space-md py-space-sm rounded-lg">
                <span className="text-primary font-label-code text-label-code select-all break-all">
                  {activeOption.command}
                </span>
                <CopyButton text={activeOption.command} />
              </div>
              <p className="font-body-sm text-body-sm text-on-surface-variant px-1">
                {activeOption.note}
              </p>
            </div>

            {/* Live Verification Checklist Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-space-sm pt-space-sm">
              <button
                type="button"
                onClick={onRefresh}
                className="bg-surface-container-low hover:bg-surface-container p-space-sm rounded-lg flex items-center gap-space-xs text-left transition-colors"
                title={lastCheckedAt ? `Last checked at ${lastCheckedAt}. Click to re-check.` : "Click to probe backend"}
              >
                <AppIcon
                  name={
                    connectionState === "connected"
                      ? "check_circle"
                      : connectionState === "connecting"
                        ? "sync"
                        : "error"
                  }
                  className={`w-5 h-5 ${
                    connectionState === "connected"
                      ? "text-secondary"
                      : connectionState === "connecting"
                        ? "text-primary"
                        : "text-error"
                  }`}
                />
                <div className="flex flex-col min-w-0">
                  <span className="font-label-code text-label-code text-on-surface font-bold">
                    {backendCardTitle}
                  </span>
                  <span className="font-body-sm text-body-sm text-on-surface-variant truncate">
                    {backendCardSub}
                  </span>
                </div>
              </button>

              <div className="bg-surface-container-low p-space-sm rounded-lg flex items-center gap-space-xs">
                <AppIcon name="model_training" className="w-5 h-5 text-primary" />
                <div className="flex flex-col min-w-0">
                  <span className="font-label-code text-label-code text-on-surface font-bold">
                    Step 2: Model Metadata
                  </span>
                  <span className="font-body-sm text-body-sm text-on-surface-variant truncate">
                    {modelCardSub}
                  </span>
                </div>
              </div>

              <div className="bg-surface-container-low p-space-sm rounded-lg flex items-center gap-space-xs">
                <AppIcon name="extension" className="w-5 h-5 text-tertiary" />
                <div className="flex flex-col min-w-0">
                  <span className="font-label-code text-label-code text-on-surface font-bold">
                    Step 3: Desktop &amp; Ext
                  </span>
                  <span className="font-body-sm text-body-sm text-on-surface-variant truncate">
                    JavaFX Shell &amp; MV3 Scaffold
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
