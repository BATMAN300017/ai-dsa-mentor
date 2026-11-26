import { useState } from "react";
import type { BackendConnectionState, HealthResponse, SystemStatusResponse } from "../types/api";
import { AppIcon } from "./AppIcon";
import { CopyButton } from "./CopyButton";

interface HeroSectionProps {
  connectionState: BackendConnectionState;
  health: HealthResponse | null;
  systemStatus: SystemStatusResponse | null;
  modelStatus?: import("../types/api").ModelStatusResponse | null;
}

const HERO_COMMAND = "./mvnw -pl apps/backend -am spring-boot:run";

export function HeroSection({ connectionState, health, systemStatus, modelStatus }: HeroSectionProps) {
  const [showPolicyNote, setShowPolicyNote] = useState(false);

  const modelStatusLabel =
    connectionState === "unavailable"
      ? "Backend Offline · Preview"
      : health?.modelLoaded || modelStatus?.modelLoaded
        ? "Qwen2.5-Coder-3B Loaded"
        : modelStatus?.modelLoading
          ? "Model: Loading..."
          : "Model: Not Loaded";

  const backendStatusLine =
    connectionState === "connected"
      ? `API 127.0.0.1:8787 UP · ${modelStatus?.expectedQuantization ?? systemStatus?.model.expectedQuantization ?? "Q4_K_M"} · ${modelStatus?.downloadStatus ?? systemStatus?.model.downloadStatus ?? "NOT_DOWNLOADED"}`
      : connectionState === "connecting"
        ? "Probing local Spring Boot backend..."
        : "Local backend not running yet — start with command above";

  return (
    <section className="relative w-full pt-6 pb-space-xl overflow-hidden" id="home">
      {/* Ambient Cyber-Glow Radial Elements */}
      <div className="absolute top-12 left-1/2 -translate-x-1/2 w-[840px] h-[400px] bg-primary/10 rounded-full blur-[140px] pointer-events-none" />
      <div className="absolute top-36 right-10 w-[420px] h-[320px] bg-secondary/10 rounded-full blur-[120px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin relative z-10 flex flex-col items-center">
        {/* Eyebrow Pill */}
        <a
          href="#how-it-works"
          className="inline-flex items-center gap-space-xs px-space-md py-1.5 rounded-full bg-surface-container-high text-primary font-label-code text-label-code shadow-sm hover:bg-surface-container-highest transition-colors cursor-pointer group mb-space-lg"
        >
          <span className="w-2 h-2 rounded-full bg-primary animate-ping" />
          <span className="font-semibold text-primary">[FOUNDATION v0.1.0]</span>
          <span className="text-on-surface-variant group-hover:text-on-surface transition-colors">
            Think with me. Don&apos;t solve it for me. · Qwen2.5-Coder-3B (Planned)
          </span>
          <AppIcon name="arrow_forward" className="w-3.5 h-3.5 text-secondary" />
        </a>

        {/* Main Heading */}
        <h1 className="font-display text-display-mobile md:text-display text-center text-on-surface tracking-tight max-w-4xl">
          Master DSA with an AI mentor that watches your{" "}
          <span className="text-primary">logic</span>, not just your answers.
        </h1>

        {/* Subtitle */}
        <p className="font-body-lg text-body-lg text-on-surface-variant text-center max-w-2xl mt-space-md leading-relaxed">
          Local-first algorithmic tutoring powered by Java 21, Spring Boot, JavaFX, and planned
          Qwen2.5-Coder-3B-Instruct (GGUF Q4_K_M) inference via llama.cpp—guiding your problem
          solving without immediately revealing solutions.
        </p>

        {/* Terminal Quick-Run & Action Controls */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-space-md mt-space-lg w-full max-w-3xl">
          <div className="w-full sm:w-auto flex-1 min-w-0 flex items-center justify-between bg-surface-container-lowest px-space-md py-2.5 rounded-xl shadow-md group">
            <div className="flex items-center gap-space-xs font-label-code text-label-code text-on-surface-variant truncate">
              <span className="text-secondary select-none font-bold">$</span>
              <span className="text-on-surface truncate">{HERO_COMMAND}</span>
            </div>
            <CopyButton text={HERO_COMMAND} compact />
          </div>

          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-space-xs px-space-lg py-2.5 rounded-xl bg-primary text-on-primary font-headline-sm text-headline-sm hover:bg-primary-fixed-dim transition-all shadow-lg hover:shadow-primary/20 hover:-translate-y-0.5 whitespace-nowrap flex-shrink-0"
            href="#installation"
          >
            <AppIcon name="download" className="w-5 h-5" />
            <span>Local Setup Guide</span>
          </a>

          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-space-xs px-space-md py-2.5 rounded-xl bg-surface-container text-on-surface font-body-sm text-body-sm hover:bg-surface-container-high transition-colors whitespace-nowrap flex-shrink-0"
            href="#how-it-works"
          >
            <AppIcon name="play_circle" className="w-[18px] h-[18px] text-secondary" />
            <span>Architecture</span>
          </a>
        </div>

        {/* Live IDE Split-Screen Simulator Hero Visual */}
        <div className="w-full mt-space-xl rounded-xl bg-surface-container-lowest shadow-2xl overflow-hidden">
          {/* Editor Top Chrome Bar */}
          <div className="h-10 bg-surface-container-low px-space-md flex items-center justify-between">
            <div className="flex items-center gap-2 min-w-0">
              <span className="w-3 h-3 rounded-full bg-error opacity-75 inline-block" />
              <span className="w-3 h-3 rounded-full bg-secondary-container opacity-75 inline-block" />
              <span className="w-3 h-3 rounded-full bg-primary-container opacity-75 inline-block" />
              <span className="ml-space-sm font-label-code text-label-code text-on-surface-variant truncate">
                AI DSA Mentor Workspace — UI Concept Preview (#LC-0011)
              </span>
            </div>
            <div className="hidden sm:flex items-center gap-space-md flex-shrink-0">
              <span className="inline-flex items-center gap-1 font-label-code text-label-code text-secondary">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary animate-pulse" />
                {backendStatusLine}
              </span>
            </div>
            <span className="sm:hidden inline-flex items-center gap-1 font-label-code text-label-code text-secondary flex-shrink-0">
              <span className="w-1.5 h-1.5 rounded-full bg-secondary animate-pulse" />
              {connectionState === "connected"
                ? "127.0.0.1 UP"
                : connectionState === "connecting"
                  ? "Connecting"
                  : "Offline"}
            </span>
          </div>

          {/* 2-Pane Core Grid: Left Code, Right Mentor HUD */}
          <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[460px]">
            {/* Code Editor Pane (7 Cols) */}
            <div className="lg:col-span-7 bg-surface-container-lowest p-space-md flex flex-col justify-between overflow-x-auto">
              <div>
                <div className="flex items-center gap-space-xs pb-space-sm mb-space-sm">
                  <span className="inline-flex items-center gap-1.5 px-space-sm py-1 rounded bg-surface-container text-primary font-label-code text-label-code">
                    <AppIcon name="code" className="w-3.5 h-3.5" />
                    solution_two_pointers.py
                  </span>
                  <span className="inline-flex items-center gap-1.5 px-space-sm py-1 rounded text-on-surface-variant hover:text-on-surface font-label-code text-label-code">
                    test_cases.json
                  </span>
                  <span className="ml-auto px-space-xs py-0.5 rounded bg-surface-container-low text-outline font-label-code text-label-code">
                    Editor Preview (Planned)
                  </span>
                </div>

                <div className="font-code-sm text-code-sm leading-relaxed text-on-surface select-none space-y-0.5">
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">1</span>
                    <span>
                      <span className="text-tertiary">class</span>{" "}
                      <span className="text-secondary">Solution</span>:
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">2</span>
                    <span className="pl-4">
                      <span className="text-tertiary">def</span>{" "}
                      <span className="text-primary">maxArea</span>(
                      <span className="text-on-background">self</span>,{" "}
                      <span className="text-secondary">height</span>: List[int]) -&gt; int:
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">3</span>
                    <span className="pl-8 text-on-surface-variant">
                      # Invariant: left and right pointers maximize width
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">4</span>
                    <span className="pl-8">
                      <span className="text-secondary">left</span>,{" "}
                      <span className="text-secondary">right</span> = 0, len(height) - 1
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">5</span>
                    <span className="pl-8">
                      <span className="text-secondary">max_water</span> = 0
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">6</span>
                    <span>&nbsp;</span>
                  </div>
                  <div className="flex items-center gap-space-md bg-surface-container/60 -mx-space-md px-space-md py-0.5">
                    <span className="w-6 text-right text-primary font-bold select-none">7</span>
                    <span className="pl-8">
                      <span className="text-tertiary">while</span>{" "}
                      <span className="text-secondary">left</span> &lt;{" "}
                      <span className="text-secondary">right</span>:
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md bg-surface-container/60 -mx-space-md px-space-md py-0.5">
                    <span className="w-6 text-right text-primary font-bold select-none">8</span>
                    <span className="pl-12">
                      <span className="text-secondary">current_area</span> = min(height[left],
                      height[right]) * (right - left)
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md bg-surface-container/60 -mx-space-md px-space-md py-0.5">
                    <span className="w-6 text-right text-primary font-bold select-none">9</span>
                    <span className="pl-12">
                      <span className="text-secondary">max_water</span> = max(max_water,
                      current_area)
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md bg-surface-container-high -mx-space-md px-space-md py-0.5 relative">
                    <span className="w-6 text-right text-secondary font-bold select-none">10</span>
                    <span className="pl-12 text-on-surface-variant font-medium">
                      # [PROMPT STATE]: You paused here. Which pointer to advance?
                    </span>
                    <span className="inline-block w-2 h-4 bg-primary animate-pulse ml-1" />
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">11</span>
                    <span className="pl-12 text-on-surface-variant opacity-60">
                      if height[left] &lt; height[right]:
                    </span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">12</span>
                    <span className="pl-16 text-on-surface-variant opacity-60">left += 1</span>
                  </div>
                  <div className="flex items-center gap-space-md">
                    <span className="w-6 text-right text-outline select-none">13</span>
                    <span className="pl-8">
                      <span className="text-tertiary">return</span>{" "}
                      <span className="text-secondary">max_water</span>
                    </span>
                  </div>
                </div>
              </div>

              <div className="mt-space-md pt-space-sm flex items-center justify-between text-on-surface-variant font-label-code text-label-code">
                <div className="flex items-center gap-space-md">
                  <span className="text-primary font-semibold">● UTF-8</span>
                  <span>Spaces: 4</span>
                  <span>Tree-sitter AST: Planned</span>
                </div>
                <span className="text-secondary">Preview AST Node: WhileStatement (Depth: 2)</span>
              </div>
            </div>

            {/* AI DSA Mentor HUD Co-pilot Pane (5 Cols) */}
            <div className="lg:col-span-5 bg-surface-container p-space-md flex flex-col justify-between space-y-space-md">
              <div className="space-y-space-sm">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-space-xs">
                    <span
                      className={`w-2 h-2 rounded-full ${
                        health?.modelLoaded ? "bg-secondary animate-ping" : "bg-primary"
                      }`}
                    />
                    <span className="font-headline-sm text-headline-sm text-on-surface">
                      AI DSA Mentor HUD
                    </span>
                  </div>
                  <span className="px-space-xs py-0.5 rounded bg-surface-container-highest text-secondary font-label-code text-label-code">
                    {modelStatusLabel}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-space-xs">
                  <div className="bg-surface-container-low p-space-sm rounded-lg flex flex-col">
                    <span className="font-label-code text-label-code text-on-surface-variant uppercase">
                      Time Complexity (Preview)
                    </span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <span className="font-code-lg text-code-lg text-secondary font-bold">O(N)</span>
                      <span className="text-label-code text-on-surface-variant line-through">
                        O(N²)
                      </span>
                    </div>
                  </div>
                  <div className="bg-surface-container-low p-space-sm rounded-lg flex flex-col">
                    <span className="font-label-code text-label-code text-on-surface-variant uppercase">
                      Aux Space (Preview)
                    </span>
                    <div className="flex items-center gap-1.5 mt-0.5">
                      <span className="font-code-lg text-code-lg text-primary font-bold">O(1)</span>
                      <span className="text-label-code text-secondary">Optimal</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Memory / Pointer ASCII Visualizer Canvas */}
              <div className="bg-surface-container-lowest p-space-sm rounded-lg flex flex-col space-y-space-xs">
                <div className="flex items-center justify-between text-on-surface-variant font-label-code text-label-code">
                  <span>ARRAY POINTER PROJECTION (PREVIEW)</span>
                  <span className="text-secondary">L: idx 0 | R: idx 8</span>
                </div>
                <div className="w-full py-2">
                  <svg
                    className="w-full h-auto"
                    fill="none"
                    viewBox="0 0 340 70"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      className="fill-primary/20 stroke-primary"
                      height="30"
                      rx="4"
                      strokeWidth="1.5"
                      width="30"
                      x="10"
                      y="10"
                    />
                    <text
                      fill="#d0bcff"
                      fontFamily="JetBrains Mono"
                      fontSize="12"
                      fontWeight="bold"
                      textAnchor="middle"
                      x="25"
                      y="30"
                    >
                      1
                    </text>
                    <polygon points="18,47 22,55 14,55" fill="#7bd0ff" />
                    <text
                      fill="#7bd0ff"
                      fontFamily="JetBrains Mono"
                      fontSize="10"
                      textAnchor="middle"
                      x="29"
                      y="55"
                    >
                      L
                    </text>
                    {[
                      { x: 46, tx: 61, v: "8" },
                      { x: 82, tx: 97, v: "6" },
                      { x: 118, tx: 133, v: "2" },
                      { x: 154, tx: 169, v: "5" },
                      { x: 190, tx: 205, v: "4" },
                      { x: 226, tx: 241, v: "8" },
                      { x: 262, tx: 277, v: "3" },
                    ].map((cell) => (
                      <g key={cell.x}>
                        <rect
                          className="fill-surface-container-high stroke-outline"
                          height="30"
                          rx="4"
                          strokeWidth="1"
                          width="30"
                          x={cell.x}
                          y="10"
                        />
                        <text
                          fill="#cbc3d7"
                          fontFamily="JetBrains Mono"
                          fontSize="12"
                          textAnchor="middle"
                          x={cell.tx}
                          y="30"
                        >
                          {cell.v}
                        </text>
                      </g>
                    ))}
                    <rect
                      className="fill-primary/20 stroke-primary"
                      height="30"
                      rx="4"
                      strokeWidth="1.5"
                      width="30"
                      x="298"
                      y="10"
                    />
                    <text
                      fill="#d0bcff"
                      fontFamily="JetBrains Mono"
                      fontSize="12"
                      fontWeight="bold"
                      textAnchor="middle"
                      x="313"
                      y="30"
                    >
                      7
                    </text>
                    <polygon points="306,47 310,55 302,55" fill="#7bd0ff" />
                    <text
                      fill="#7bd0ff"
                      fontFamily="JetBrains Mono"
                      fontSize="10"
                      textAnchor="middle"
                      x="317"
                      y="55"
                    >
                      R
                    </text>
                  </svg>
                </div>
              </div>

              {/* Socratic Hint Card Preview */}
              <div className="bg-surface-container-high p-space-md rounded-lg space-y-space-xs shadow-sm">
                <div className="flex items-center justify-between text-secondary font-label-code text-label-code">
                  <div className="flex items-center gap-space-xs">
                    <AppIcon name="psychology" className="w-4 h-4" />
                    <span>SOCRATIC HINT PREVIEW</span>
                  </div>
                  <span className="text-outline">IN DEVELOPMENT</span>
                </div>
                <p className="font-body-md text-body-md text-on-surface">
                  &ldquo;Notice how both pointers move inward. Since the area is constrained by the
                  shorter wall, what happens if you keep the shorter wall and discard the taller
                  one?&rdquo;
                </p>
                {showPolicyNote && (
                  <p className="font-body-sm text-body-sm text-secondary bg-surface-container-lowest p-space-xs rounded">
                    Socratic DSA tutoring policy belongs to Task 3. Local Qwen2.5-Coder-3B model
                    download, verification, llama.cpp loading, and raw inference testing are ready
                    below in Local Setup.
                  </p>
                )}
                <div className="pt-space-xs flex items-center justify-between">
                  <span className="text-on-surface-variant font-label-code text-label-code">
                    Target: Qwen2.5-Coder-3B Q4_K_M
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowPolicyNote((prev) => !prev)}
                    className="px-space-sm py-1 rounded bg-primary text-on-primary font-label-code text-label-code hover:bg-primary-fixed-dim transition-colors"
                  >
                    {showPolicyNote ? "Hide Status" : "Runtime Status"}
                  </button>
                </div>
              </div>

              {/* Bottom telemetry prompt input */}
              <div className="flex items-center gap-space-xs bg-surface-container-low px-space-sm py-1.5 rounded-lg">
                <span className="text-secondary font-label-code text-label-code">&gt;</span>
                <input
                  className="bg-transparent border-none outline-none font-body-sm text-body-sm text-on-surface-variant placeholder:text-outline w-full"
                  readOnly
                  type="text"
                  value="Local LLM inference (llama.cpp + Qwen2.5-Coder-3B) planned for Task 2+"
                />
                <AppIcon name="schedule" className="w-[18px] h-[18px] text-outline" />
              </div>
            </div>
          </div>
        </div>

        {/* Core Philosophy & Stack Bar */}
        <div className="w-full mt-space-xl pt-space-lg flex flex-col md:flex-row items-center justify-between gap-space-md text-on-surface-variant font-body-sm text-body-sm">
          <div className="flex items-center gap-space-xs">
            <span className="font-headline-sm text-headline-sm text-on-surface font-semibold">
              Local-First Philosophy:
            </span>
            <span>&ldquo;Think with me. Don&apos;t solve it for me.&rdquo;</span>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-space-lg text-outline font-label-code text-label-code">
            <span className="hover:text-on-surface transition-colors">JAVA 21</span>
            <span className="hover:text-on-surface transition-colors">SPRING BOOT</span>
            <span className="hover:text-on-surface transition-colors">JAVAFX</span>
            <span className="hover:text-on-surface transition-colors">LLAMA.CPP</span>
            <span className="hover:text-on-surface transition-colors">TREE-SITTER</span>
            <span className="hover:text-on-surface transition-colors">QWEN2.5-CODER-3B</span>
          </div>
        </div>
      </div>
    </section>
  );
}
