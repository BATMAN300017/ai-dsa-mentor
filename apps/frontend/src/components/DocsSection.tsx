import { useState } from "react";
import type {
  BackendConnectionState,
  HealthResponse,
  ModelStatusResponse,
  SystemStatusResponse,
} from "../types/api";
import { AppIcon } from "./AppIcon";

interface DocsSectionProps {
  connectionState: BackendConnectionState;
  health: HealthResponse | null;
  systemStatus: SystemStatusResponse | null;
  modelStatus?: ModelStatusResponse | null;
}

export function DocsSection({ connectionState, health, systemStatus, modelStatus }: DocsSectionProps) {
  const [showLiveJson, setShowLiveJson] = useState(false);

  return (
    <section className="w-full py-space-xl bg-surface relative" id="docs">
      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col items-center text-center space-y-space-xs mb-space-xl">
          <span className="px-space-sm py-1 rounded bg-surface-container text-primary font-label-code text-label-code uppercase tracking-wider">
            Technical Reference
          </span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">
            Documentation &amp; Local API Reference
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-xl">
            Run, inspect, and extend the AI DSA Mentor workspace using Maven Wrapper, Spring Boot
            endpoints, and modular Java contracts.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
          {/* Left: Commands & Health API (6 Cols) */}
          <div className="lg:col-span-6 bg-surface-container-low p-space-lg rounded-xl space-y-space-md shadow-md">
            <div className="flex items-center justify-between">
              <span className="font-headline-sm text-headline-sm text-on-surface">
                Workspace Commands &amp; Endpoints
              </span>
              <span className="font-label-code text-label-code text-secondary">v0.1.0</span>
            </div>

            <div className="space-y-space-sm font-label-code text-label-code">
              <div className="p-space-sm bg-surface-container-lowest rounded-lg flex flex-col space-y-1">
                <span className="text-outline"># Start Spring Boot backend (127.0.0.1:8787)</span>
                <span className="text-primary font-bold">
                  ./mvnw -pl apps/backend -am spring-boot:run
                </span>
              </div>
              <div className="p-space-sm bg-surface-container-lowest rounded-lg flex flex-col space-y-1">
                <span className="text-outline"># Query live health check endpoint</span>
                <span className="text-primary font-bold">
                  curl http://127.0.0.1:8787/api/v1/health
                </span>
              </div>
              <div className="p-space-sm bg-surface-container-lowest rounded-lg flex flex-col space-y-1">
                <span className="text-outline"># Launch JavaFX desktop shell</span>
                <span className="text-primary font-bold">./mvnw -pl apps/desktop -am javafx:run</span>
              </div>
              <div className="p-space-sm bg-surface-container-lowest rounded-lg flex flex-col space-y-1">
                <span className="text-outline"># Build and run all JUnit 5 suites</span>
                <span className="text-primary font-bold">./mvnw clean verify</span>
              </div>
            </div>

            <div className="pt-space-sm flex items-center justify-between font-body-sm text-body-sm text-on-surface-variant">
              <span>Inspect live backend telemetry payload?</span>
              <button
                type="button"
                onClick={() => setShowLiveJson((prev) => !prev)}
                className="text-secondary hover:underline flex items-center gap-1 font-label-code text-label-code"
              >
                <span>{showLiveJson ? "Hide JSON" : "View /api/v1/health"}</span>
                <AppIcon name="data_object" className="w-3.5 h-3.5" />
              </button>
            </div>

            {showLiveJson && (
              <pre className="p-space-sm rounded-lg bg-surface-container-lowest font-code-sm text-code-sm text-secondary overflow-x-auto">
                {JSON.stringify(
                  {
                    connectionState,
                    health: health ?? null,
                    model: modelStatus ?? systemStatus?.model ?? null,
                  },
                  null,
                  2
                )}
              </pre>
            )}
          </div>

          {/* Right: Pipeline Schematic & Resource Links (6 Cols) */}
          <div className="lg:col-span-6 bg-surface-container-low p-space-lg rounded-xl space-y-space-md shadow-md">
            <div className="flex items-center justify-between">
              <span className="font-headline-sm text-headline-sm text-on-surface">
                Data Flow Pipeline
              </span>
              <span className="font-label-code text-label-code text-primary">
                Local-First Topology
              </span>
            </div>

            <div className="bg-surface-container-lowest p-space-md rounded-lg flex items-center justify-center">
              <svg
                className="w-full h-auto"
                fill="none"
                viewBox="0 0 420 180"
                xmlns="http://www.w3.org/2000/svg"
              >
                <rect
                  className="fill-surface-container-high stroke-secondary"
                  height="50"
                  rx="6"
                  strokeWidth="1.5"
                  width="110"
                  x="10"
                  y="20"
                />
                <text
                  fill="#7bd0ff"
                  fontFamily="JetBrains Mono"
                  fontSize="10"
                  fontWeight="bold"
                  textAnchor="middle"
                  x="65"
                  y="42"
                >
                  EXT / EDITOR
                </text>
                <text
                  fill="#cbc3d7"
                  fontFamily="Inter"
                  fontSize="9"
                  textAnchor="middle"
                  x="65"
                  y="56"
                >
                  Meaningful Edits
                </text>

                <path d="M120 45 L150 45" stroke="#7bd0ff" strokeWidth="1.5" />

                <rect
                  className="fill-surface-container-high stroke-primary"
                  height="50"
                  rx="6"
                  strokeWidth="1.5"
                  width="110"
                  x="155"
                  y="20"
                />
                <text
                  fill="#d0bcff"
                  fontFamily="JetBrains Mono"
                  fontSize="10"
                  fontWeight="bold"
                  textAnchor="middle"
                  x="210"
                  y="42"
                >
                  TREE-SITTER
                </text>
                <text
                  fill="#cbc3d7"
                  fontFamily="Inter"
                  fontSize="9"
                  textAnchor="middle"
                  x="210"
                  y="56"
                >
                  AST Inspection
                </text>

                <path d="M265 45 L295 45" stroke="#d0bcff" strokeWidth="1.5" />

                <rect
                  className="fill-surface-container-high stroke-tertiary"
                  height="50"
                  rx="6"
                  strokeWidth="1.5"
                  width="110"
                  x="300"
                  y="20"
                />
                <text
                  fill="#c0c1ff"
                  fontFamily="JetBrains Mono"
                  fontSize="10"
                  fontWeight="bold"
                  textAnchor="middle"
                  x="355"
                  y="42"
                >
                  QWEN2.5-3B
                </text>
                <text
                  fill="#cbc3d7"
                  fontFamily="Inter"
                  fontSize="9"
                  textAnchor="middle"
                  x="355"
                  y="56"
                >
                  llama.cpp Local
                </text>

                <path d="M355 70 L355 110 L210 110" stroke="#c0c1ff" strokeWidth="1.5" />

                <rect
                  className="fill-primary/20 stroke-primary"
                  height="48"
                  rx="6"
                  strokeWidth="2"
                  width="220"
                  x="100"
                  y="115"
                />
                <text
                  fill="#d0bcff"
                  fontFamily="JetBrains Mono"
                  fontSize="11"
                  fontWeight="bold"
                  textAnchor="middle"
                  x="210"
                  y="136"
                >
                  SOCRATIC HINT ENGINE
                </text>
                <text
                  fill="#e1e2ec"
                  fontFamily="Inter"
                  fontSize="9"
                  textAnchor="middle"
                  x="210"
                  y="152"
                >
                  Think With Me · No Solution Spoiling
                </text>
              </svg>
            </div>

            <div className="grid grid-cols-2 gap-space-sm pt-space-xs font-label-code text-label-code">
              <a
                className="p-space-sm rounded-lg bg-surface-container hover:bg-surface-container-highest transition-colors flex items-center justify-between text-on-surface"
                href="https://github.com/BATMAN300017/ai-dsa-mentor/blob/main/docs/architecture.md"
                rel="noreferrer"
                target="_blank"
              >
                <span>Architecture Docs</span>
                <AppIcon name="arrow_outward" className="w-4 h-4 text-secondary" />
              </a>
              <a
                className="p-space-sm rounded-lg bg-surface-container hover:bg-surface-container-highest transition-colors flex items-center justify-between text-on-surface"
                href="https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF"
                rel="noreferrer"
                target="_blank"
              >
                <span>Qwen2.5 Model Card</span>
                <AppIcon name="arrow_outward" className="w-4 h-4 text-primary" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
