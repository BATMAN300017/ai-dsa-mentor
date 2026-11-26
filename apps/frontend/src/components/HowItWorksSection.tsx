import { AppIcon } from "./AppIcon";

export function HowItWorksSection() {
  return (
    <section className="w-full py-space-xl bg-surface-container-lowest relative" id="how-it-works">
      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col items-center text-center space-y-space-xs mb-space-xl">
          <span className="px-space-sm py-1 rounded bg-surface-container text-secondary font-label-code text-label-code uppercase tracking-wider">
            Runtime Architecture
          </span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">
            Deterministic analysis meets Socratic intelligence.
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-xl">
            AI DSA Mentor combines planned Tree-sitter AST observation with local Qwen2.5-Coder-3B
            inference, analyzing your reasoning before you hit a wall.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-space-lg">
          {/* Step 1 */}
          <div className="bg-surface-container-low p-space-lg rounded-xl flex flex-col justify-between relative group hover:bg-surface-container transition-all shadow-md">
            <div className="space-y-space-md">
              <div className="flex items-center justify-between">
                <span className="font-label-code text-label-code text-primary bg-primary/10 px-2 py-0.5 rounded">
                  STEP 01 · PLANNED
                </span>
                <AppIcon name="terminal" className="w-6 h-6 text-secondary" />
              </div>
              <h3 className="font-headline-sm text-headline-sm text-on-surface">
                Passive AST &amp; Editor Observation
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                Planned Tree-sitter parsers and browser extension adapters will observe meaningful
                structural edits across LeetCode, coding platforms, or an optional built-in editor.
              </p>
            </div>
            <div className="mt-space-lg pt-space-sm bg-surface-container-lowest p-space-sm rounded-lg font-label-code text-label-code text-secondary flex items-center justify-between">
              <span>&gt; Code Analyzer Port: Defined</span>
              <AppIcon name="schedule" className="w-4 h-4 text-primary" />
            </div>
          </div>

          {/* Step 2 */}
          <div className="bg-surface-container-low p-space-lg rounded-xl flex flex-col justify-between relative group hover:bg-surface-container transition-all shadow-md">
            <div className="space-y-space-md">
              <div className="flex items-center justify-between">
                <span className="font-label-code text-label-code text-secondary bg-secondary/10 px-2 py-0.5 rounded">
                  STEP 02 · PLANNED
                </span>
                <AppIcon name="account_tree" className="w-6 h-6 text-primary" />
              </div>
              <h3 className="font-headline-sm text-headline-sm text-on-surface">
                Mistake &amp; Invariant Detection
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                Detects potential infinite recursion, off-by-one boundary shifts, empty-input gaps,
                and accidental O(N²) loops with out-of-process sandbox test verification.
              </p>
            </div>
            <div className="mt-space-lg pt-space-sm bg-surface-container-lowest p-space-sm rounded-lg font-label-code text-label-code text-primary flex items-center justify-between">
              <span>&gt; Sandbox Isolation Policy: Ready</span>
              <AppIcon name="verified_user" className="w-4 h-4 text-primary" />
            </div>
          </div>

          {/* Step 3 */}
          <div className="bg-surface-container-low p-space-lg rounded-xl flex flex-col justify-between relative group hover:bg-surface-container transition-all shadow-md">
            <div className="space-y-space-md">
              <div className="flex items-center justify-between">
                <span className="font-label-code text-label-code text-tertiary bg-tertiary/10 px-2 py-0.5 rounded">
                  STEP 03 · PLANNED
                </span>
                <AppIcon name="psychology_alt" className="w-6 h-6 text-tertiary" />
              </div>
              <h3 className="font-headline-sm text-headline-sm text-on-surface">
                Tiered Socratic Guidance
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                Powered locally by Qwen2.5-Coder-3B-Instruct (GGUF Q4_K_M) via llama.cpp. Never
                dumps complete solutions—offers progressive questions and conceptual nudges.
              </p>
            </div>
            <div className="mt-space-lg pt-space-sm bg-surface-container-lowest p-space-sm rounded-lg font-label-code text-label-code text-tertiary flex items-center justify-between">
              <span>&gt; 0% Solution Leak Policy</span>
              <AppIcon name="verified" className="w-4 h-4 text-secondary" />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
