import { useState } from "react";
import { NAV_ITEMS } from "../data/landingContent";
import type { BackendConnectionState, HealthResponse } from "../types/api";
import { AppIcon } from "./AppIcon";
import { BackendBadge } from "./BackendBadge";
import { BrandLogo } from "./BrandLogo";

interface HeaderProps {
  activeSection: string;
  connectionState: BackendConnectionState;
  health: HealthResponse | null;
  onRefreshStatus: () => void;
}

export function Header({
  activeSection,
  connectionState,
  health,
  onRefreshStatus,
}: HeaderProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-surface-container-lowest/85 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.4)]">
      <div className="h-16 max-w-7xl mx-auto px-margin-mobile md:px-margin flex items-center justify-between gap-space-md">
        <div className="flex items-center gap-space-sm">
          <BrandLogo className="h-8 w-8 flex-shrink-0" />
          <a className="flex items-center gap-space-xs group" href="#home">
            <span className="font-headline-sm text-headline-sm text-on-surface tracking-tight group-hover:text-primary transition-colors whitespace-nowrap">
              AI DSA Mentor
            </span>
          </a>
          <span className="hidden sm:inline-flex items-center px-space-xs py-0.5 rounded-full bg-surface-container-high text-secondary font-label-code text-label-code whitespace-nowrap">
            v{health?.version ?? "0.1.0"} Foundation
          </span>
        </div>

        <nav className="hidden xl:flex items-center gap-space-md" aria-label="Primary Navigation">
          {NAV_ITEMS.map((item) => {
            const isActive = activeSection === item.id;
            return (
              <a
                key={item.id}
                aria-current={isActive ? "page" : undefined}
                href={item.href}
                className={
                  isActive
                    ? "transition-colors text-primary font-semibold font-body-sm text-body-sm"
                    : "text-on-surface-variant hover:text-on-surface transition-colors font-body-sm text-body-sm"
                }
              >
                {item.label}
              </a>
            );
          })}
        </nav>

        <div className="flex items-center gap-space-sm">
          <div className="hidden sm:block">
            <BackendBadge connectionState={connectionState} onRefresh={onRefreshStatus} />
          </div>
          <a
            className="hidden md:inline-flex items-center gap-space-xs px-space-sm py-1.5 rounded-lg bg-surface-container-high text-on-surface-variant hover:text-on-surface hover:bg-surface-container-highest transition-colors font-label-code text-label-code"
            href="https://github.com/BATMAN300017/ai-dsa-mentor"
            rel="noreferrer"
            target="_blank"
          >
            <span className="text-secondary">★</span>
            <span>GitHub</span>
          </a>
          <a
            className="inline-flex items-center justify-center px-space-md py-1.5 rounded-lg bg-primary text-on-primary font-body-sm text-body-sm font-semibold hover:bg-primary-fixed-dim transition-all shadow-[0_0_16px_-3px_rgba(208,188,255,0.4)] whitespace-nowrap"
            href="#installation"
          >
            Local Setup
          </a>
          <button
            type="button"
            onClick={() => setMobileMenuOpen((prev) => !prev)}
            aria-expanded={mobileMenuOpen}
            aria-label="Toggle navigation menu"
            className="xl:hidden w-8 h-8 rounded-full bg-surface-container-high text-on-surface flex items-center justify-center hover:bg-surface-container-highest transition-colors"
          >
            <AppIcon name={mobileMenuOpen ? "close" : "menu"} className="w-[18px] h-[18px]" />
          </button>
          <div
            className="hidden xl:flex w-8 h-8 rounded-full bg-primary items-center justify-center flex-shrink-0"
            title="Local-first mode (no cloud account required)"
          >
            <AppIcon name="person" className="w-[18px] h-[18px] text-on-primary" />
          </div>
        </div>
      </div>

      {mobileMenuOpen && (
        <div className="xl:hidden bg-surface-container-low border-t border-outline-variant/20 px-margin-mobile py-space-md space-y-space-sm">
          <div className="sm:hidden pb-space-xs">
            <BackendBadge connectionState={connectionState} onRefresh={onRefreshStatus} />
          </div>
          <nav className="grid grid-cols-2 gap-space-xs" aria-label="Mobile Navigation">
            {NAV_ITEMS.map((item) => {
              const isActive = activeSection === item.id;
              return (
                <a
                  key={item.id}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`px-space-sm py-1.5 rounded-lg font-body-sm text-body-sm transition-colors ${
                    isActive
                      ? "bg-surface-container-high text-primary font-semibold"
                      : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container"
                  }`}
                >
                  {item.label}
                </a>
              );
            })}
          </nav>
        </div>
      )}
    </header>
  );
}
