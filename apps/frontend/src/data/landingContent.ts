export interface NavItem {
  id: string;
  label: string;
  href: string;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { id: "home", label: "Home", href: "#home" },
  { id: "how-it-works", label: "How It Works", href: "#how-it-works" },
  { id: "features", label: "Features", href: "#features" },
  { id: "installation", label: "Installation", href: "#installation" },
  { id: "compatibility", label: "Compatibility", href: "#compatibility" },
  { id: "faq", label: "FAQ", href: "#faq" },
  { id: "docs", label: "Docs", href: "#docs" },
];

export type InstallTabKey = "win" | "mac" | "linux" | "maven";

export interface InstallTabOption {
  key: InstallTabKey;
  label: string;
  command: string;
  note: string;
}

export const INSTALL_TABS: readonly InstallTabOption[] = [
  {
    key: "win",
    label: "Windows (PowerShell)",
    command: ".\\scripts\\setup-dev.ps1 && .\\mvnw.cmd -pl apps/backend -am spring-boot:run",
    note: "Requires JDK 21+, Maven Wrapper, and Node.js 20+",
  },
  {
    key: "mac",
    label: "macOS (Shell)",
    command: "./scripts/setup-dev.sh && ./mvnw -pl apps/backend -am spring-boot:run",
    note: "Supports Apple Silicon (arm64) & Intel (x86_64) via JDK 21+",
  },
  {
    key: "linux",
    label: "Linux (Bash)",
    command: "./scripts/setup-dev.sh && ./mvnw -pl apps/desktop -am javafx:run",
    note: "Runs Spring Boot backend on 127.0.0.1:8787 + JavaFX shell",
  },
  {
    key: "maven",
    label: "Maven / Full Build",
    command: "./mvnw clean verify && ./mvnw -pl apps/desktop -am javafx:run",
    note: "Builds all 9 Maven modules, runs JUnit 5 suites, and starts desktop",
  },
];

export interface CompatibilityGroup {
  icon: string;
  eyebrow: string;
  eyebrowColorClass: string;
  badgeColorClass: string;
  title: string;
  items: { name: string; status: string }[];
}

export const COMPATIBILITY_MATRIX: readonly CompatibilityGroup[] = [
  {
    icon: "public",
    eyebrow: "JUDGES & WEBSITES",
    eyebrowColorClass: "text-primary",
    badgeColorClass: "text-secondary",
    title: "Coding Platforms",
    items: [
      { name: "LeetCode", status: "Planned Adapter" },
      { name: "Codeforces", status: "Planned" },
      { name: "HackerRank", status: "Planned" },
      { name: "NeetCode.io", status: "Planned" },
      { name: "Built-in Editor", status: "Planned" },
    ],
  },
  {
    icon: "integration_instructions",
    eyebrow: "DESKTOP & BROWSER",
    eyebrowColorClass: "text-secondary",
    badgeColorClass: "text-primary",
    title: "Application Shells",
    items: [
      { name: "JavaFX Desktop Shell", status: "Foundation" },
      { name: "Spring Boot Local API", status: "127.0.0.1 Ready" },
      { name: "Chrome MV3 Extension", status: "Scaffolded" },
      { name: "Default Browser Fallback", status: "Supported" },
      { name: "jpackage Installers", status: "Planned" },
    ],
  },
  {
    icon: "terminal",
    eyebrow: "PROGRAMMING LANGUAGES",
    eyebrowColorClass: "text-tertiary",
    badgeColorClass: "text-tertiary",
    title: "Compilers & ASTs",
    items: [
      { name: "Java 21", status: "Tree-sitter Planned" },
      { name: "Python 3.10+", status: "Tree-sitter Planned" },
      { name: "C++ 17 / 20", status: "Tree-sitter Planned" },
      { name: "TypeScript / JS", status: "Tree-sitter Planned" },
      { name: "Isolated Test Sandbox", status: "Out-of-Process" },
    ],
  },
  {
    icon: "memory",
    eyebrow: "LOCAL MODEL SPECS",
    eyebrowColorClass: "text-secondary",
    badgeColorClass: "text-on-surface",
    title: "Qwen2.5-Coder-3B",
    items: [
      { name: "Quantization", status: "GGUF Q4_K_M" },
      { name: "Local Runtime", status: "llama.cpp Server" },
      { name: "Cloud LLM Dependency", status: "None (Offline First)" },
      { name: "Integrity Verification", status: "SHA-256 + Size" },
      { name: "AppData Model Storage", status: "Configured" },
    ],
  },
];

export interface FaqEntry {
  id: string;
  question: string;
  answer: string;
}

export const FAQ_ITEMS: readonly FaqEntry[] = [
  {
    id: "faq-1",
    question: "Does AI DSA Mentor transmit my private source code to paid cloud LLM APIs?",
    answer:
      "No. AI DSA Mentor is designed around a local-first architecture. The Spring Boot backend binds exclusively to 127.0.0.1 loopback, and upcoming AST analysis (Tree-sitter) and local inference (Qwen2.5-Coder-3B-Instruct GGUF Q4_K_M via llama.cpp) execute locally without relying on paid hosted LLM APIs.",
  },
  {
    id: "faq-2",
    question: "What is operational now versus scheduled for later tasks?",
    answer:
      "The foundation and local AI model layer are operational: Java 21 monorepo, Spring Boot local API with localhost origin/token security, resumable SHA-256-verified Qwen2.5-Coder-3B GGUF download manager, llama.cpp local server lifecycle and inference endpoints, JavaFX desktop shell, Chrome MV3 scaffold, and the React + TypeScript UI console. Socratic DSA tutoring policies, Tree-sitter AST parsing, sandboxed code execution, and LeetCode observation belong to upcoming tasks.",
  },
  {
    id: "faq-3",
    question: "How does AI DSA Mentor differ from Copilot or standard chatbots?",
    answer:
      "Guided by the philosophy 'Think with me. Don't solve it for me.', AI DSA Mentor avoids dumping complete solutions. Instead, future tutoring layers combine AST inspection with tiered Socratic prompts—helping you spot off-by-one errors, complexity bottlenecks, and missing invariants yourself.",
  },
  {
    id: "faq-4",
    question: "Where will the 2 GB Qwen2.5-Coder-3B GGUF model be stored?",
    answer:
      "Task 1 prepares the metadata descriptor at resources/models/qwen2.5-coder-3b/model-info.json without downloading the weights yet. In packaged desktop releases, the Model Manager resolves the user's writable OS application-data directory (%LOCALAPPDATA%\\AI DSA Mentor on Windows, ~/Library/Application Support/AI DSA Mentor on macOS, ~/.local/share/ai-dsa-mentor on Linux).",
  },
  {
    id: "faq-5",
    question: "Why Java 21, Spring Boot, and JavaFX instead of Electron?",
    answer:
      "Using Java 21 across the backend, domain modules, and desktop launcher keeps the stack unified, avoids bundling duplicate Chromium runtimes unnecessarily, prepares clean jpackage native distribution, and preserves a local browser fallback whenever JavaFX WebView encounters compatibility limits.",
  },
];
