export const siteUrl =
  process.env.NEXT_PUBLIC_SITE_URL?.replace(/\/$/, "") || "https://smartjirani.app";

export const siteName = "SmartJirani";

export const nav = [
  { href: "/communities", label: "Communities" },
  { href: "/services", label: "Services" },
  { href: "/business", label: "Business" },
  { href: "/ai-tracker", label: "AI Tracker" },
  { href: "/help", label: "Help" },
];
