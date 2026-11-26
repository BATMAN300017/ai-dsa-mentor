import { useEffect, useState } from "react";

export function useActiveSection(sectionIds: readonly string[]): string {
  const [activeId, setActiveId] = useState<string>(sectionIds[0] ?? "home");

  useEffect(() => {
    const updateActiveFromScroll = () => {
      const scrollMarker = window.scrollY + 140;
      let current = sectionIds[0] ?? "home";

      for (const id of sectionIds) {
        const element = document.getElementById(id);
        if (element && element.offsetTop <= scrollMarker) {
          current = id;
        }
      }
      setActiveId(current);
    };

    updateActiveFromScroll();
    window.addEventListener("scroll", updateActiveFromScroll, { passive: true });
    return () => window.removeEventListener("scroll", updateActiveFromScroll);
  }, [sectionIds]);

  return activeId;
}
