import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { siteName, siteUrl } from "@/lib/site";
import "./globals.css";

const jakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-jakarta",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "SmartJirani — Your mtaa, connected",
    template: "%s — SmartJirani",
  },
  description:
    "SmartJirani is the Kenyan neighborhood app for barazas, harambees, trusted fundis, urgent help and everyday sharing. Free for residents. English & Kiswahili.",
  keywords: [
    "neighborhood app Kenya",
    "community app",
    "baraza",
    "harambee",
    "fundi",
    "SmartJirani",
  ],
  openGraph: {
    type: "website",
    siteName,
    url: siteUrl,
    title: "SmartJirani — Your mtaa, connected",
    description:
      "Barazas, harambees, trusted fundis, urgent help and everyday sharing — one app for Kenya's neighborhoods.",
    locale: "en_KE",
  },
  twitter: {
    card: "summary_large_image",
    title: "SmartJirani — Your mtaa, connected",
    description:
      "Barazas, harambees, trusted fundis, urgent help and everyday sharing — one app for Kenya's neighborhoods.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={jakarta.variable}>
      <body className="flex min-h-screen flex-col">
        <SiteHeader />
        <main className="flex-1">{children}</main>
        <SiteFooter />
      </body>
    </html>
  );
}
