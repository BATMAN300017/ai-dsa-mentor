import { COMPATIBILITY_MATRIX } from "../data/landingContent";
import { AppIcon } from "./AppIcon";

export function CompatibilitySection() {
  return (
    <section className="w-full py-space-xl bg-surface relative" id="compatibility">
      <div className="max-w-7xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col items-center text-center space-y-space-xs mb-space-xl">
          <span className="px-space-sm py-1 rounded bg-surface-container text-primary font-label-code text-label-code uppercase tracking-wider">
            Ecosystem Matrix
          </span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">
            Built for local execution and platform integration.
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-xl">
            Clear separation between Task 1 operational foundations and upcoming local inference,
            parser, and platform adapters.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-space-md">
          {COMPATIBILITY_MATRIX.map((group) => (
            <div
              key={group.title}
              className="bg-surface-container-low p-space-md rounded-xl space-y-space-sm shadow-md"
            >
              <div
                className={`flex items-center gap-space-xs ${group.eyebrowColorClass} font-label-code text-label-code`}
              >
                <AppIcon name={group.icon} className="w-[18px] h-[18px]" />
                <span>{group.eyebrow}</span>
              </div>
              <h3 className="font-headline-sm text-headline-sm text-on-surface">{group.title}</h3>
              <ul className="space-y-space-xs font-body-sm text-body-sm text-on-surface-variant">
                {group.items.map((item) => (
                  <li key={item.name} className="flex items-center justify-between gap-2">
                    <span>{item.name}</span>
                    <span
                      className={`${group.badgeColorClass} font-label-code text-label-code text-right`}
                    >
                      {item.status}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
