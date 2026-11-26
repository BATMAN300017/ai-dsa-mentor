interface AppIconProps {
  name: string;
  className?: string;
}

export function AppIcon({ name, className = "w-4 h-4" }: AppIconProps) {
  const common = {
    className: `inline-block flex-shrink-0 ${className}`,
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.8,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    viewBox: "0 0 24 24",
    "aria-hidden": true,
  };

  switch (name) {
    case "person":
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="4" />
          <path d="M4 20c1.5-4 5-6 8-6s6.5 2 8 6" />
        </svg>
      );
    case "menu":
      return (
        <svg {...common}>
          <path d="M4 7h16M4 12h16M4 17h16" />
        </svg>
      );
    case "close":
      return (
        <svg {...common}>
          <path d="M6 6l12 12M18 6L6 18" />
        </svg>
      );
    case "arrow_forward":
      return (
        <svg {...common}>
          <path d="M5 12h14M13 6l6 6-6 6" />
        </svg>
      );
    case "arrow_outward":
      return (
        <svg {...common}>
          <path d="M7 17L17 7M8 7h9v9" />
        </svg>
      );
    case "content_copy":
      return (
        <svg {...common}>
          <rect x="9" y="9" width="11" height="11" rx="2" />
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
        </svg>
      );
    case "check":
      return (
        <svg {...common}>
          <path d="M5 12l5 5L20 7" />
        </svg>
      );
    case "check_circle":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M8.5 12.5l2.5 2.5 4.5-5" />
        </svg>
      );
    case "download":
      return (
        <svg {...common}>
          <path d="M12 4v11M7 11l5 5 5-5M5 20h14" />
        </svg>
      );
    case "play_circle":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <polygon points="10,8 16,12 10,16" fill="currentColor" stroke="none" />
        </svg>
      );
    case "code":
      return (
        <svg {...common}>
          <path d="M8 9l-4 3 4 3M16 9l4 3-4 3M13 6l-2 12" />
        </svg>
      );
    case "psychology":
    case "psychology_alt":
      return (
        <svg {...common}>
          <path d="M9.5 4a4.5 4.5 0 0 0-4.5 4.5c0 1.4.6 2.6 1.5 3.4V15a2 2 0 0 0 2 2h1v3h4v-3h1a4 4 0 0 0 4-4v-1.5A5.5 5.5 0 0 0 13.5 4h-4z" />
          <circle cx="12" cy="10" r="1.5" fill="currentColor" stroke="none" />
        </svg>
      );
    case "schedule":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 2" />
        </svg>
      );
    case "terminal":
      return (
        <svg {...common}>
          <rect x="3" y="4" width="18" height="16" rx="2" />
          <path d="M7 9l3 3-3 3M12 15h5" />
        </svg>
      );
    case "account_tree":
      return (
        <svg {...common}>
          <rect x="3" y="4" width="5" height="5" rx="1" />
          <rect x="16" y="4" width="5" height="5" rx="1" />
          <rect x="16" y="15" width="5" height="5" rx="1" />
          <path d="M8 6.5h8M12 6.5v11h4" />
        </svg>
      );
    case "verified_user":
    case "verified":
      return (
        <svg {...common}>
          <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      );
    case "format_list_numbered":
      return (
        <svg {...common}>
          <path d="M10 6h10M10 12h10M10 18h10M4 6h2v4M4 10h2M4 15h2v1l-2 2h2" />
        </svg>
      );
    case "bug_report":
      return (
        <svg {...common}>
          <rect x="8" y="8" width="8" height="10" rx="4" />
          <path d="M9 8V6a3 3 0 0 1 6 0v2M4 11h4M16 11h4M5 16l3-1M19 16l-3-1" />
        </svg>
      );
    case "warning":
      return (
        <svg {...common}>
          <path d="M12 3L2 20h20L12 3z" />
          <path d="M12 10v4M12 17h.01" />
        </svg>
      );
    case "speed":
      return (
        <svg {...common}>
          <path d="M5 17a8 8 0 1 1 14 0" />
          <path d="M12 13l4-4" />
        </svg>
      );
    case "mic":
      return (
        <svg {...common}>
          <rect x="9" y="3" width="6" height="10" rx="3" />
          <path d="M6 11a6 6 0 0 0 12 0M12 17v4M9 21h6" />
        </svg>
      );
    case "hub":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="2.5" />
          <circle cx="6" cy="6" r="2" />
          <circle cx="18" cy="6" r="2" />
          <circle cx="18" cy="18" r="2" />
          <circle cx="6" cy="18" r="2" />
          <path d="M10.2 10.2L7.5 7.5M13.8 10.2l2.7-2.7M13.8 13.8l2.7 2.7M10.2 13.8l-2.7 2.7" />
        </svg>
      );
    case "sync":
      return (
        <svg {...common}>
          <path d="M4 12a8 8 0 0 1 13.5-5.7M20 4v4h-4M20 12a8 8 0 0 1-13.5 5.7M4 20v-4h4" />
        </svg>
      );
    case "error":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v5M12 16h.01" />
        </svg>
      );
    case "model_training":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="3" />
          <path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1" />
        </svg>
      );
    case "extension":
      return (
        <svg {...common}>
          <path d="M14 4a2 2 0 0 1 2 2v2h3a1 1 0 0 1 1 1v4h-2a2 2 0 0 0 0 4h2v3a1 1 0 0 1-1 1h-4v-2a2 2 0 0 0-4 0v2H5a1 1 0 0 1-1-1v-4h2a2 2 0 0 0 0-4H4V9a1 1 0 0 1 1-1h3V6a2 2 0 0 1 2-2z" />
        </svg>
      );
    case "public":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18" />
        </svg>
      );
    case "integration_instructions":
      return (
        <svg {...common}>
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M9 10l-2 2 2 2M15 10l2 2-2 2M13 9l-2 6" />
        </svg>
      );
    case "memory":
      return (
        <svg {...common}>
          <rect x="7" y="7" width="10" height="10" rx="1.5" />
          <path d="M9 3v4M15 3v4M9 17v4M15 17v4M3 9h4M3 15h4M17 9h4M17 15h4" />
        </svg>
      );
    case "keyboard_arrow_down":
      return (
        <svg {...common}>
          <path d="M6 9l6 6 6-6" />
        </svg>
      );
    case "data_object":
      return (
        <svg {...common}>
          <path d="M9 4H7a2 2 0 0 0-2 2v4a2 2 0 0 1-2 2 2 2 0 0 1 2 2v4a2 2 0 0 0 2 2h2M15 4h2a2 2 0 0 1 2 2v4a2 2 0 0 0 2 2 2 2 0 0 0-2 2v4a2 2 0 0 1-2 2h-2" />
        </svg>
      );
    default:
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="6" />
        </svg>
      );
  }
}
