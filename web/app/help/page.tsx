import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Help center",
  description:
    "Answers about getting started, verification, neighborhoods, safety, downloads and updates on SmartJirani.",
  alternates: { canonical: "/help" },
};

const faqs: { section: string; id: string; items: { q: string; a: string }[] }[] = [
  {
    section: "Getting started",
    id: "getting-started",
    items: [
      {
        q: "What is SmartJirani?",
        a: "SmartJirani is a neighborhood app for Kenya. It brings baraza meetings, harambee fundraising, urgent needs, street notices, a lend-and-borrow locker, trusted fundis and messaging into one place — organized around the estate, court or village where you actually live.",
      },
      {
        q: "How do I join my neighborhood?",
        a: "Download the app, create a profile with your real name, then search for your estate or court. If it already exists, request to join. If not, create it and invite your neighbors with the share link.",
      },
      {
        q: "Is SmartJirani free?",
        a: "Yes — SmartJirani is free for residents. Business profiles are also free during early access.",
      },
    ],
  },
  {
    section: "Account & verification",
    id: "verification",
    items: [
      {
        q: "Why do I need a real-name profile?",
        a: "Trust is the product. When you know the person lending you a ladder or watching your child is a real neighbor, everything works better. Your full details are never shown publicly — only your display name and avatar.",
      },
      {
        q: "What is karma?",
        a: "Karma is your reputation score. You earn it by helping with urgent needs, responding to barazas, sharing items and getting endorsements. High-karma neighbors get a Jirani wa Kuaminika (trusted neighbor) badge.",
      },
      {
        q: "How do I change my display name or photo?",
        a: "Open the app, go to your profile, and tap edit. Your display name and avatar are what neighbors see on posts, reviews and services.",
      },
    ],
  },
  {
    section: "Safety & privacy",
    id: "safety",
    items: [
      {
        q: "Who can see my house number?",
        a: "You control this in your privacy settings. By default your precise house number is hidden — neighbors see only your street or block unless you choose to show it.",
      },
      {
        q: "What happens when I report someone or a post?",
        a: "Reports go to your mtaa's admins for review. Serious reports (harassment, scams, threats) are escalated to SmartJirani's trust & safety team. Abusive accounts can be suspended and banned.",
      },
      {
        q: "How do I avoid scams?",
        a: "Stick to in-app chat until you've met, never send money for 'booking fees' to strangers, check reviews before hiring a fundi, and report anything suspicious. Our scam detection flags common patterns automatically.",
      },
    ],
  },
  {
    section: "Downloads & updates",
    id: "downloads",
    items: [
      {
        q: "Where do I download the app?",
        a: "From our download page. You'll install directly from the APK — the page explains each step and publishes the file's SHA-256 checksum so you can verify it.",
      },
      {
        q: "How will I know there's an update?",
        a: "The app checks for new versions and tells you when one is ready, with a link straight to the download page and its release notes.",
      },
      {
        q: "Is SmartJirani on the Play Store?",
        a: "Play Store release is planned after the direct-APK launch. Join the app now and you'll get a note when it happens.",
      },
    ],
  },
];

export default function HelpPage() {
  return (
    <>
      <section className="bg-linen">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <div className="max-w-2xl">
            <h1 className="text-4xl font-extrabold tracking-tight text-ink sm:text-5xl">
              Help center
            </h1>
            <p className="mt-4 text-lg leading-8 text-ink-variant">
              Quick answers about SmartJirani. If you can&apos;t find what you
              need, reach us in the app or by email.
            </p>
          </div>
          <nav aria-label="Sections" className="mt-8 flex flex-wrap gap-2">
            {faqs.map((f) => (
              <a
                key={f.id}
                href={`#${f.id}`}
                className="rounded-full border border-outline-soft bg-white px-4 py-2 text-sm font-semibold text-ink transition-colors hover:bg-clay"
              >
                {f.section}
              </a>
            ))}
          </nav>
        </div>
      </section>

      <section className="mx-auto max-w-3xl px-4 py-12 sm:px-6 md:py-16">
        {faqs.map((group) => (
          <div key={group.id} id={group.id} className="mb-12 scroll-mt-24">
            <h2 className="text-2xl font-extrabold tracking-tight text-ink">
              {group.section}
            </h2>
            <div className="mt-5 space-y-3">
              {group.items.map((item) => (
                <details
                  key={item.q}
                  className="group rounded-2xl border border-clay-deep bg-white px-5 py-4 open:shadow-sm"
                >
                  <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-base font-bold text-ink [&::-webkit-details-marker]:hidden">
                    {item.q}
                    <svg
                      viewBox="0 0 20 20"
                      className="h-5 w-5 shrink-0 text-outline-brand transition-transform group-open:rotate-180"
                      fill="currentColor"
                      aria-hidden
                    >
                      <path
                        fillRule="evenodd"
                        d="M5.3 7.3a1 1 0 0 1 1.4 0L10 10.6l3.3-3.3a1 1 0 1 1 1.4 1.4l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 0 1 0-1.4Z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </summary>
                  <p className="mt-3 text-sm leading-6 text-ink-variant">{item.a}</p>
                </details>
              ))}
            </div>
          </div>
        ))}

        <div className="rounded-3xl bg-clay p-6 sm:p-8">
          <h2 className="text-xl font-extrabold text-ink">Still stuck?</h2>
          <p className="mt-2 text-sm leading-6 text-ink-variant">
            Email us at{" "}
            <a
              href="mailto:support@smartjirani.app"
              className="font-semibold text-emerald-brand hover:underline"
            >
              support@smartjirani.app
            </a>{" "}
            or report a problem directly in the app (Profile → Help &amp; support).
            We read everything.
          </p>
        </div>
      </section>
    </>
  );
}
