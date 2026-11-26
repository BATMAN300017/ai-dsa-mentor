import { AppIcon } from "./AppIcon";

export function FinalCtaSection() {
  return (
    <section className="w-full py-space-xl bg-surface-container-lowest relative overflow-hidden">
      <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[700px] h-[250px] bg-primary/10 rounded-full blur-[140px] pointer-events-none" />
      <div className="max-w-5xl mx-auto px-margin-mobile md:px-margin relative z-10 text-center space-y-space-md">
        <span className="px-space-sm py-1 rounded bg-surface-container-high text-primary font-label-code text-label-code uppercase tracking-wider">
          Think With Me. Don&apos;t Solve It For Me.
        </span>
        <h2 className="font-display text-display-mobile md:text-display text-on-surface tracking-tight">
          Stop grinding blind. Start mastering algorithms with deep intuition.
        </h2>
        <p className="font-body-lg text-body-lg text-on-surface-variant max-w-2xl mx-auto">
          Built on a privacy-first Java 21 + Spring Boot + JavaFX foundation, engineered for local
          Qwen2.5-Coder-3B reasoning without paid cloud LLM dependencies.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-space-md pt-space-md">
          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-space-xs px-space-xl py-3 rounded-xl bg-primary text-on-primary font-headline-sm text-headline-sm hover:bg-primary-fixed-dim transition-all shadow-xl hover:shadow-primary/30"
            href="#installation"
          >
            <AppIcon name="terminal" className="w-5 h-5" />
            <span>Run Local Foundation</span>
          </a>
          <a
            className="w-full sm:w-auto inline-flex items-center justify-center gap-space-xs px-space-lg py-3 rounded-xl bg-surface-container text-on-surface font-headline-sm text-headline-sm hover:bg-surface-container-high transition-colors"
            href="https://github.com/BATMAN300017/ai-dsa-mentor"
            rel="noreferrer"
            target="_blank"
          >
            <AppIcon name="code" className="w-5 h-5 text-secondary" />
            <span>View on GitHub</span>
          </a>
        </div>
      </div>
    </section>
  );
}
