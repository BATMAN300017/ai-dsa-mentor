export function BrandLogo({ className = "h-8 w-8" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 36 36"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect
        x="2"
        y="2"
        width="32"
        height="32"
        rx="8"
        fill="#191b23"
        stroke="#494454"
        strokeWidth="1.2"
      />
      <path
        d="M11 23L18 11L25 23"
        stroke="#d0bcff"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M14 19.5H22"
        stroke="#7bd0ff"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <circle cx="18" cy="11" r="2.2" fill="#7bd0ff" />
      <circle cx="11" cy="23" r="2.2" fill="#d0bcff" />
      <circle cx="25" cy="23" r="2.2" fill="#c0c1ff" />
    </svg>
  );
}
