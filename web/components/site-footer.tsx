import Link from "next/link";
import { Logo } from "@/components/logo";

const groups = [
  {
    title: "Product",
    links: [
      { href: "/download", label: "Download" },
      { href: "/communities", label: "Communities" },
      { href: "/services", label: "Services" },
    ],
  },
  {
    title: "For you",
    links: [
      { href: "/business", label: "Businesses & fundis" },
      { href: "/help", label: "Help center" },
      { href: "/help#safety", label: "Safety tips" },
    ],
  },
  {
    title: "Legal",
    links: [
      { href: "/privacy", label: "Privacy" },
      { href: "/terms", label: "Terms" },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="border-t border-clay-deep bg-linen">
      <div className="mx-auto grid max-w-6xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
        <div>
          <Logo />
          <p className="mt-4 max-w-xs text-sm leading-6 text-ink-variant">
            One app for Kenya&apos;s neighborhoods — barazas, harambees, trusted
            fundis, urgent help and everyday sharing.
          </p>
        </div>
        {groups.map((group) => (
          <div key={group.title}>
            <h3 className="text-sm font-bold text-ink">{group.title}</h3>
            <ul className="mt-3 space-y-2">
              {group.links.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-ink-variant transition-colors hover:text-emerald-brand"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="border-t border-clay-deep">
        <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-5 text-xs text-outline-brand sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p>© {new Date().getFullYear()} SmartJirani. Made for Kenyan mtaani.</p>
          <p>English &amp; Kiswahili</p>
        </div>
      </div>
    </footer>
  );
}
