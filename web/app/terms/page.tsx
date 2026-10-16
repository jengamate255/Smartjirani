import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Terms of service",
  description: "The rules for using SmartJirani.",
  alternates: { canonical: "/terms" },
};

export default function TermsPage() {
  return (
    <article className="mx-auto max-w-3xl px-4 py-16 sm:px-6 md:py-20">
      <h1 className="text-4xl font-extrabold tracking-tight text-ink">Terms of service</h1>
      <p className="mt-2 text-sm text-outline-brand">Last updated: October 2026</p>
      <div className="mt-8 space-y-8 text-sm leading-6 text-ink-variant">
        <section>
          <h2 className="text-lg font-bold text-ink">Using SmartJirani</h2>
          <p className="mt-2">
            SmartJirani connects residents of the same neighborhood. To keep it
            trustworthy: use your real identity, be a good neighbor, and follow
            your mtaa&apos;s rules. Don&apos;t impersonate others, harass
            anyone, post scams or illegal content, or use the platform to spam
            businesses or residents.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Your content</h2>
          <p className="mt-2">
            You own what you post. By posting in a neighborhood you allow its
            members — and us, to operate the service — to view and display it.
            Don&apos;t post anything you don&apos;t have the right to share.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Fundraising &amp; payments</h2>
          <p className="mt-2">
            Harambee funds are raised and managed within your community.
            Contribution records are transparent to members but SmartJirani is
            not the custodian of funds and does not guarantee any campaign.
            Business services are provided by the listing business, not by
            SmartJirani.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Moderation</h2>
          <p className="mt-2">
            Mtaa admins moderate their communities; our trust &amp; safety team
            handles escalations. We may remove content or suspend accounts that
            break these terms, break the law, or put neighbors at risk.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Liability</h2>
          <p className="mt-2">
            SmartJirani is provided &quot;as is&quot; while we build toward full
            launch. We work hard to keep it available and safe, but we can&apos;t
            warrant that it will always be uninterrupted or error-free.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Contact</h2>
          <p className="mt-2">
            Questions about these terms:{" "}
            <a href="mailto:support@smartjirani.app" className="font-semibold text-emerald-brand hover:underline">
              support@smartjirani.app
            </a>
          </p>
        </section>
      </div>
    </article>
  );
}
