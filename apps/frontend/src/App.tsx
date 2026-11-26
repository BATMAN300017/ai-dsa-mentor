import { CompatibilitySection } from "./components/CompatibilitySection";
import { DocsSection } from "./components/DocsSection";
import { FaqSection } from "./components/FaqSection";
import { FeaturesSection } from "./components/FeaturesSection";
import { FinalCtaSection } from "./components/FinalCtaSection";
import { Footer } from "./components/Footer";
import { Header } from "./components/Header";
import { HeroSection } from "./components/HeroSection";
import { HowItWorksSection } from "./components/HowItWorksSection";
import { InstallationSection } from "./components/InstallationSection";
import { NAV_ITEMS } from "./data/landingContent";
import { useActiveSection } from "./hooks/useActiveSection";
import { useBackendStatus } from "./hooks/useBackendStatus";

const SECTION_IDS = NAV_ITEMS.map((item) => item.id);

export default function App() {
  const activeSection = useActiveSection(SECTION_IDS);
  const { connectionState, health, systemStatus, modelStatus, lastCheckedAt, refresh } =
    useBackendStatus();

  return (
    <div className="min-h-screen bg-surface text-on-surface">
      <Header
        activeSection={activeSection}
        connectionState={connectionState}
        health={health}
        onRefreshStatus={() => void refresh()}
      />
      <main className="w-full pt-16 bg-surface min-h-screen">
        <div className="flex flex-col w-full">
          <HeroSection
            connectionState={connectionState}
            health={health}
            systemStatus={systemStatus}
            modelStatus={modelStatus}
          />
          <HowItWorksSection />
          <FeaturesSection />
          <InstallationSection
            connectionState={connectionState}
            health={health}
            systemStatus={systemStatus}
            modelStatus={modelStatus}
            lastCheckedAt={lastCheckedAt}
            onRefresh={refresh}
          />
          <CompatibilitySection />
          <FaqSection />
          <DocsSection
            connectionState={connectionState}
            health={health}
            systemStatus={systemStatus}
            modelStatus={modelStatus}
          />
          <FinalCtaSection />
        </div>
      </main>
      <Footer />
    </div>
  );
}
