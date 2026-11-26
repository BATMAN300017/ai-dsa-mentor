import { useState } from "react";
import { FAQ_ITEMS } from "../data/landingContent";
import { AppIcon } from "./AppIcon";

export function FaqSection() {
  const [openIds, setOpenIds] = useState<Record<string, boolean>>({});

  const toggleFaq = (id: string) => {
    setOpenIds((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  return (
    <section className="w-full py-space-xl bg-surface-container-lowest relative" id="faq">
      <div className="max-w-4xl mx-auto px-margin-mobile md:px-margin">
        <div className="flex flex-col items-center text-center space-y-space-xs mb-space-xl">
          <span className="px-space-sm py-1 rounded bg-surface-container text-secondary font-label-code text-label-code uppercase tracking-wider">
            Developer FAQ
          </span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">
            Frequently Answered Questions
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-xl">
            Transparent details on local privacy, model configuration, Java desktop architecture,
            and Task 1 scope.
          </p>
        </div>

        <div className="space-y-space-sm">
          {FAQ_ITEMS.map((item) => {
            const isOpen = Boolean(openIds[item.id]);
            return (
              <div
                key={item.id}
                className="bg-surface-container-low rounded-xl overflow-hidden shadow-sm"
              >
                <button
                  type="button"
                  aria-expanded={isOpen}
                  aria-controls={item.id}
                  onClick={() => toggleFaq(item.id)}
                  className="w-full px-space-lg py-space-md flex items-center justify-between text-left hover:bg-surface-container transition-colors gap-space-md"
                >
                  <span className="font-headline-sm text-headline-sm text-on-surface">
                    {item.question}
                  </span>
                  <AppIcon
                    name="keyboard_arrow_down"
                    className={`w-5 h-5 text-primary transition-transform ${
                      isOpen ? "rotate-180" : ""
                    }`}
                  />
                </button>
                {isOpen && (
                  <div
                    id={item.id}
                    className="px-space-lg pb-space-md pt-space-sm text-on-surface-variant font-body-md text-body-md leading-relaxed border-t border-surface-container/50"
                  >
                    {item.answer}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
