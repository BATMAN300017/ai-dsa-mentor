import { AppIcon } from "./AppIcon";

export function FeaturesSection() {
  return (
    <section className="w-full py-space-xl bg-surface relative" id="features">
      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col items-center text-center space-y-space-xs mb-space-xl">
          <span className="px-space-sm py-1 rounded bg-surface-container text-primary font-label-code text-label-code uppercase tracking-wider">
            Engine Capabilities (Planned Roadmap)
          </span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">
            Engineered for algorithmic instincts, not rote memorization.
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-2xl">
            Five modular Java subsystems designed to transform how developers build problem-solving
            intuition across upcoming implementation tasks.
          </p>
        </div>

        {/* Bento Grid (5 Core Capabilities) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg">
          {/* Feature 1: Progressive Hints (8 Cols) */}
          <div className="lg:col-span-8 bg-surface-container-low p-space-lg rounded-xl shadow-md flex flex-col justify-between">
            <div className="space-y-space-sm">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-space-xs font-label-code text-label-code text-primary">
                  <AppIcon name="format_list_numbered" className="w-[18px] h-[18px]" />
                  <span>CAPABILITY 01</span>
                </div>
                <span className="px-space-xs py-0.5 rounded bg-surface-container text-secondary font-label-code text-label-code">
                  Planned
                </span>
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface">
                Tiered Socratic Hint Escalation
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant max-w-xl">
                Calibrated progression ensures you do the cognitive heavy lifting while avoiding hours
                spinning wheels on a single blind spot.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-space-sm mt-space-md">
              <div className="bg-surface-container p-space-sm rounded-lg space-y-space-xs">
                <div className="flex items-center justify-between text-secondary font-label-code text-label-code">
                  <span>LEVEL 01</span>
                  <span>Subtle</span>
                </div>
                <h4 className="font-headline-sm text-headline-sm text-on-surface">
                  Directional Whisper
                </h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  &ldquo;Consider whether sorting or maintaining an invariant changes the lookup
                  cost.&rdquo;
                </p>
              </div>
              <div className="bg-surface-container p-space-sm rounded-lg space-y-space-xs">
                <div className="flex items-center justify-between text-primary font-label-code text-label-code">
                  <span>LEVEL 02</span>
                  <span>Targeted</span>
                </div>
                <h4 className="font-headline-sm text-headline-sm text-on-surface">
                  Edge-Case Reveal
                </h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  &ldquo;What happens if elements repeat or become negative? Does your stack pop
                  too early?&rdquo;
                </p>
              </div>
              <div className="bg-surface-container p-space-sm rounded-lg space-y-space-xs">
                <div className="flex items-center justify-between text-tertiary font-label-code text-label-code">
                  <span>LEVEL 03</span>
                  <span>Rigorous</span>
                </div>
                <h4 className="font-headline-sm text-headline-sm text-on-surface">
                  Invariant Proof
                </h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  &ldquo;Formulate the loop invariant before committing transitions for your DP or
                  pointer state.&rdquo;
                </p>
              </div>
            </div>
          </div>

          {/* Feature 2: Real-time Mistake Detection (4 Cols) */}
          <div className="lg:col-span-4 bg-surface-container-low p-space-lg rounded-xl shadow-md flex flex-col justify-between">
            <div className="space-y-space-sm">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-space-xs font-label-code text-label-code text-error">
                  <AppIcon name="bug_report" className="w-[18px] h-[18px]" />
                  <span>CAPABILITY 02</span>
                </div>
                <span className="px-space-xs py-0.5 rounded bg-surface-container text-outline font-label-code text-label-code">
                  Planned
                </span>
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface">
                Static Mistake Interceptor
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Planned Tree-sitter checks for silent failure patterns: integer overflow, null next
                pointers, recursion base-case gaps, and off-by-one slices.
              </p>
            </div>
            <div className="mt-space-md p-space-sm rounded-lg bg-surface-container-lowest space-y-space-xs font-label-code text-label-code">
              <div className="flex items-center gap-2 text-error">
                <AppIcon name="warning" className="w-3.5 h-3.5" />
                <span>Sample Pattern: Dangling Next Pointer</span>
              </div>
              <p className="text-on-surface-variant font-code-sm text-code-sm">
                Node: <span className="text-on-surface">fast = fast.next.next</span>
                <br />
                Check: <span className="text-error">Null guard missing when fast.next is null</span>
              </p>
            </div>
          </div>

          {/* Feature 3: Instant Complexity Profiler (4 Cols) */}
          <div className="lg:col-span-4 bg-surface-container-low p-space-lg rounded-xl shadow-md flex flex-col justify-between">
            <div className="space-y-space-sm">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-space-xs font-label-code text-label-code text-secondary">
                  <AppIcon name="speed" className="w-[18px] h-[18px]" />
                  <span>CAPABILITY 03</span>
                </div>
                <span className="px-space-xs py-0.5 rounded bg-surface-container text-outline font-label-code text-label-code">
                  Planned
                </span>
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface">
                Instant Big-O Profiling
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Static AST traversal estimates Big-O Time and Auxiliary Space complexity to catch
                quadratic regressions early.
              </p>
            </div>
            <div className="mt-space-md p-space-sm bg-surface-container-lowest rounded-lg">
              <div className="flex items-center justify-between text-on-surface-variant font-label-code text-label-code mb-1">
                <span>N vs TIME CURVE (PREVIEW)</span>
                <span className="text-secondary font-bold">O(N log N) Target</span>
              </div>
              <svg
                className="w-full h-14"
                fill="none"
                viewBox="0 0 240 60"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path d="M0 55 C 60 54, 120 45, 240 10" fill="none" stroke="#7bd0ff" strokeWidth="2" />
                <path
                  d="M0 55 C 80 50, 140 25, 240 2"
                  fill="none"
                  stroke="#ffb4ab"
                  strokeDasharray="3 3"
                  strokeWidth="1.5"
                />
                <circle className="fill-primary" cx="160" cy="35" r="4" />
              </svg>
              <div className="flex items-center justify-between text-label-code text-on-surface-variant mt-1">
                <span className="text-error">Brute Force: O(N²)</span>
                <span className="text-secondary">Optimal Path: O(N log N)</span>
              </div>
            </div>
          </div>

          {/* Feature 4: Voice Mentoring (4 Cols) */}
          <div className="lg:col-span-4 bg-surface-container-low p-space-lg rounded-xl shadow-md flex flex-col justify-between">
            <div className="space-y-space-sm">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-space-xs font-label-code text-label-code text-primary">
                  <AppIcon name="mic" className="w-[18px] h-[18px]" />
                  <span>CAPABILITY 04</span>
                </div>
                <span className="px-space-xs py-0.5 rounded bg-surface-container text-primary font-label-code text-label-code">
                  Coming soon
                </span>
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface">
                Local Voice Mentoring
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Optional hands-free audio reasoning and spoken nudges designed to keep your focus on
                the problem statement without switching tabs.
              </p>
            </div>
            <div className="mt-space-md p-space-sm bg-surface-container-lowest rounded-lg flex items-center justify-between">
              <div className="flex items-center gap-1.5 h-8">
                <span className="w-1 h-3 bg-secondary rounded-full animate-bounce" />
                <span
                  className="w-1 h-6 bg-primary rounded-full animate-bounce"
                  style={{ animationDelay: "0.1s" }}
                />
                <span
                  className="w-1 h-8 bg-tertiary rounded-full animate-bounce"
                  style={{ animationDelay: "0.2s" }}
                />
                <span
                  className="w-1 h-4 bg-primary rounded-full animate-bounce"
                  style={{ animationDelay: "0.15s" }}
                />
                <span
                  className="w-1 h-7 bg-secondary rounded-full animate-bounce"
                  style={{ animationDelay: "0.05s" }}
                />
                <span
                  className="w-1 h-2 bg-on-surface-variant rounded-full animate-bounce"
                  style={{ animationDelay: "0.25s" }}
                />
              </div>
              <span className="font-label-code text-label-code text-on-surface-variant">
                Voice Engine: Coming soon
              </span>
            </div>
          </div>

          {/* Feature 5: Skill Tree & Retention Analytics (4 Cols) */}
          <div className="lg:col-span-4 bg-surface-container-low p-space-lg rounded-xl shadow-md flex flex-col justify-between">
            <div className="space-y-space-sm">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-space-xs font-label-code text-label-code text-tertiary">
                  <AppIcon name="hub" className="w-[18px] h-[18px]" />
                  <span>CAPABILITY 05</span>
                </div>
                <span className="px-space-xs py-0.5 rounded bg-surface-container text-outline font-label-code text-label-code">
                  Planned
                </span>
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface">
                Learning Progress Radar
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Future local progress tracking across canonical DSA topics: Dynamic Programming,
                Graphs, DSU, Trees, and Bitwise Logic.
              </p>
            </div>
            <div className="mt-space-md p-space-sm bg-surface-container-lowest rounded-lg flex items-center justify-center">
              <svg
                className="w-40 h-auto"
                fill="none"
                viewBox="0 0 160 120"
                xmlns="http://www.w3.org/2000/svg"
              >
                <polygon
                  className="stroke-outline/40"
                  points="80,10 145,45 125,110 35,110 15,45"
                  strokeWidth="1"
                />
                <polygon
                  className="stroke-outline/20"
                  points="80,25 125,50 110,95 50,95 35,50"
                  strokeWidth="1"
                />
                <polygon
                  className="fill-primary/20 stroke-primary"
                  points="80,20 135,48 115,100 45,98 25,48"
                  strokeWidth="1.5"
                />
                <text fill="#d0bcff" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle" x="80" y="8">
                  DP
                </text>
                <text fill="#7bd0ff" fontFamily="JetBrains Mono" fontSize="8" x="142" y="48">
                  GRAPHS
                </text>
                <text fill="#cbc3d7" fontFamily="JetBrains Mono" fontSize="8" x="125" y="118">
                  DSU
                </text>
                <text fill="#cbc3d7" fontFamily="JetBrains Mono" fontSize="8" x="25" y="118">
                  TREES
                </text>
                <text fill="#7bd0ff" fontFamily="JetBrains Mono" fontSize="8" x="5" y="48">
                  BITS
                </text>
              </svg>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
