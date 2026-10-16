import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "For businesses & fundis",
  description:
    "Get discovered by neighbors who need your products and services. Create a free business profile on SmartJirani, list what you offer and start receiving leads.",
  alternates: { canonical: "/business" },
};

const benefits = [
  {
    title: "Get discovered nearby",
    body: "Your shop, hustle or skill shows up when neighbors search your category — no paying for ads the whole estate ignores.",
  },
  {
    title: "Leads, not spam",
    body: "People who contact you are from your own mtaa and ready to buy. One real lead beats a thousand flyer views.",
  },
  {
    title: "Trust built in",
    body: "Ratings and endorsements from real residents build your reputation automatically as you serve them.",
  },
  {
    title: "AI-assisted listings",
    body: "Coming soon: describe your business in a sentence and Jirani AI drafts a polished listing — English or Kiswahili.",
  },
];

const steps = [
  {
    n: "1",
    title: "Create your profile",
    body: "Sign up in the app and switch on a business profile. Add your category, location and what you offer.",
  },
  {
    n: "2",
    title: "List products & services",
    body: "Add prices, photos and opening hours. Neighbors see your listing when they browse your category.",
  },
  {
    n: "3",
    title: "Serve your mtaa",
    body: "Get chat and call leads straight in the app. Great service earns reviews — and reviews earn more leads.",
  },
];

export default function BusinessPage() {
  return (
    <>
      <section className="bg-linen">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <div className="max-w-2xl">
            <span className="inline-flex items-center gap-2 rounded-full border border-amber-brand/50 bg-white px-3 py-1.5 text-xs font-bold text-amber-deep">
              Free during early access
            </span>
            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-ink sm:text-5xl">
              Your next customers live next door
            </h1>
            <p className="mt-4 text-lg leading-8 text-ink-variant">
              Every estate buys plumbing, food, transport, tailoring and repairs
              every week. SmartJirani puts your business in front of the
              neighbors who need you — with zero commission during early access.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                href="/download"
                className="rounded-full bg-emerald-brand px-7 py-3.5 text-center text-base font-bold text-white transition-colors hover:bg-emerald-pressed"
              >
                Create your business profile
              </Link>
              <Link
                href="/help"
                className="rounded-full border border-outline-soft bg-white px-7 py-3.5 text-center text-base font-bold text-ink transition-colors hover:bg-clay"
              >
                See how it works
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-12 sm:px-6 md:py-16">
        <div className="grid gap-5 sm:grid-cols-2">
          {benefits.map((b) => (
            <div key={b.title} className="rounded-3xl border border-clay-deep bg-white p-6">
              <h2 className="text-lg font-bold text-ink">{b.title}</h2>
              <p className="mt-2 text-sm leading-6 text-ink-variant">{b.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-clay py-16 md:py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <h2 className="text-3xl font-extrabold tracking-tight text-ink">
            Three steps to your first mtaa customer
          </h2>
          <ol className="mt-10 grid gap-8 md:grid-cols-3">
            {steps.map((s) => (
              <li key={s.n}>
                <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-brand text-lg font-extrabold text-amber-deep">
                  {s.n}
                </span>
                <h3 className="mt-4 text-lg font-bold text-ink">{s.title}</h3>
                <p className="mt-2 text-sm leading-6 text-ink-variant">{s.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="flex flex-col items-center gap-5 rounded-[2rem] bg-emerald-deep px-6 py-14 text-center">
          <h2 className="max-w-xl text-3xl font-extrabold tracking-tight text-white">
            List your business free
          </h2>
          <p className="max-w-md text-base leading-7 text-emerald-mist">
            No commission during early access. All we ask: serve your mtaa well.
          </p>
          <Link
            href="/download"
            className="rounded-full bg-mint px-8 py-3.5 text-base font-bold text-emerald-deep transition-colors hover:bg-emerald-mist"
          >
            Get started
          </Link>
        </div>
      </section>
    </>
  );
}
