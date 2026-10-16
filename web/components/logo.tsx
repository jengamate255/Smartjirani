export function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <svg
        viewBox="0 0 32 32"
        aria-hidden="true"
        className="h-8 w-8 shrink-0"
      >
        <rect width="32" height="32" rx="9" fill="#135D43" />
        <path
          d="M16 7.5 24 14v9.5a1.5 1.5 0 0 1-1.5 1.5h-4.2v-6.2h-4.6V25H9.5A1.5 1.5 0 0 1 8 23.5V14l8-6.5Z"
          fill="#ABF1CF"
        />
        <circle cx="23.5" cy="8.5" r="3" fill="#F4A261" />
      </svg>
      <span className="text-[1.15rem] font-extrabold tracking-tight text-ink">
        Smart<span className="text-emerald-brand">Jirani</span>
      </span>
    </span>
  );
}
