export function Footer() {
  return (
    <footer className="w-full bg-surface-container-lowest py-space-xl">
      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col md:flex-row items-center justify-between gap-space-lg pb-space-lg">
          <div className="flex flex-col items-center md:items-start gap-space-xs">
            <div className="flex items-center gap-space-xs">
              <span className="font-headline-sm text-headline-sm text-on-surface">
                AI DSA Mentor
              </span>
              <span className="font-label-code text-label-code text-secondary px-space-xs py-0.5 rounded bg-surface-container">
                Local-First Foundation
              </span>
            </div>
            <p className="font-body-sm text-body-sm text-on-surface-variant max-w-sm text-center md:text-left">
              Local-first DSA tutoring application guided by &ldquo;Think with me. Don&apos;t solve
              it for me.&rdquo;
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-space-md font-body-sm text-body-sm text-on-surface-variant">
            <a className="hover:text-on-surface transition-colors" href="#home">
              Home
            </a>
            <a className="hover:text-on-surface transition-colors" href="#how-it-works">
              How It Works
            </a>
            <a className="hover:text-on-surface transition-colors" href="#installation">
              Installation
            </a>
            <a className="hover:text-on-surface transition-colors" href="#docs">
              Documentation
            </a>
            <a
              className="hover:text-on-surface transition-colors"
              href="https://github.com/BATMAN300017/ai-dsa-mentor"
              rel="noreferrer"
              target="_blank"
            >
              GitHub Repository
            </a>
          </div>
        </div>

        <div className="pt-space-md flex flex-col sm:flex-row items-center justify-between gap-space-sm font-body-sm text-body-sm text-on-surface-variant">
          <p>© 2026 AI DSA Mentor. MIT License.</p>
          <p className="font-label-code text-label-code">
            v0.1.0 · Java 21 / Spring Boot / Qwen2.5-Coder-3B (Q4_K_M planned)
          </p>
        </div>
      </div>
    </footer>
  );
}
