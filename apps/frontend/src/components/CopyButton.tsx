import { useState } from "react";
import { AppIcon } from "./AppIcon";

interface CopyButtonProps {
  text: string;
  compact?: boolean;
  label?: string;
}

export function CopyButton({ text, compact = false, label = "Copy" }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
    }
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  };

  if (compact) {
    return (
      <button
        type="button"
        onClick={() => void handleCopy()}
        title="Copy command"
        className="flex items-center justify-center pl-space-sm hover:scale-105 transition-transform flex-shrink-0"
      >
        {copied ? (
          <span className="text-secondary font-label-code text-label-code">COPIED</span>
        ) : (
          <AppIcon
            name="content_copy"
            className="w-4 h-4 text-on-surface-variant hover:text-on-surface"
          />
        )}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={() => void handleCopy()}
      className="flex items-center gap-1 text-on-surface-variant hover:text-on-surface font-label-code text-label-code transition-colors flex-shrink-0"
    >
      <AppIcon name={copied ? "check" : "content_copy"} className="w-4 h-4" />
      <span>{copied ? "COPIED!" : label}</span>
    </button>
  );
}
