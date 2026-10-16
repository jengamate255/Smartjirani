import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "SmartJirani — Your mtaa, connected",
  description:
    "Barazas, harambees, trusted fundis, urgent help and everyday sharing — one app for Kenya's neighborhoods. Free for residents. English & Kiswahili.",
  alternates: { canonical: "/" },
};

const features = [
  {
    title: "Barazas & events",
    body: "Agendas, RSVPs and minutes for every mtaa meeting — no more handwritten noticeboards.",
    icon: (
      <path d="M3 11v2a1 1 0 0 0 1 1h2l4 4V6L6 10H4a1 1 0 0 0-1 1Zm12-2a4 4 0 0 1 0 6m2.5-9a8 8 0 0 1 0 12" />
    ),
    tint: "bg-emerald-mist/40",
    ring: "ring-emerald-mist",
  },
  {
    title: "Harambee fundraising",
    body: "Pull together for school fees, medical bills and projects — with transparent contributions.",
    icon: (
      <path d="M12 20s-7-4.5-9-9a5 5 0 0 1 9-3 5 5 0 0 1 9 3c-2 4.5-9 9-9 9Z" />
    ),
    tint: "bg-terracotta-mist",
    ring: "ring-terracotta-mist",
  },
  {
    title: "Urgent needs",
    body: "Lost a child? Water pipe burst? Rally the whole mtaa in minutes, not hours.",
    icon: (
      <path d="M12 3a6 6 0 0 0-6 6v3.5L4.5 15h15L18 12.5V9a6 6 0 0 0-6-6Zm-2 15a2 2 0 0 0 4 0" />
    ),
    tint: "bg-terracotta-mist",
    ring: "ring-terracotta-mist",
  },
  {
    title: "Mtaa locker",
    body: "Lend and borrow ladders, drills and generators from neighbors you trust.",
    icon: (
      <path d="M4 8l8-4 8 4v8l-8 4-8-4V8Zm0 0 8 4 8-4m-8 4v8" />
    ),
    tint: "bg-emerald-mist/40",
    ring: "ring-emerald-mist",
  },
  {
    title: "Trusted fundis & services",
    body: "Find plumbers, electricians and cleaners nearby — rated and reviewed by your own mtaa.",
    icon: (
      <path d="M14.5 6.5a4 4 0 0 0-5.6 5L3 17.4V21h3.6l5.9-5.9a4 4 0 0 0 5-5.6l-2.9 2.9-2.5-.5-.5-2.5 2.9-2.9Z" />
    ),
    tint: "bg-amber-brand/25",
    ring: "ring-amber-brand/40",
  },
  {
    title: "Jirani AI",
    body: "Ask anything about your mtaa — in English or Kiswahili. Coming soon to every neighborhood.",
    icon: (
      <path d="M12 3l1.8 4.7L18.5 9l-4.7 1.8L12 15.5l-1.8-4.7L5.5 9l4.7-1.3L12 3Zm6 10l.9 2.3 2.3.9-2.3.9L18 21l-.9-2.3-2.3-.9 2.3-.9L18 13Z" />
    ),
    tint: "bg-mint/50",
    ring: "ring-mint",
  },
];

const steps = [
  {
    n: "1",
    title: "Create your profile",
    body: "Sign up with your real name and verify to build trust with your neighbors.",
  },
  {
    n: "2",
    title: "Join your mtaa",
    body: "Find your estate or village and connect with the people on your street.",
  },
  {
    n: "3",
    title: "Share, hire, meet",
    body: "Post, borrow, hire fundis and show up for each other — and earn karma for it.",
  },
];

function PhoneMockup() {
  return (
    <div className="relative mx-auto w-[290px] rotate-1 rounded-[2.5rem] border-[10px] border-ink bg-ink shadow-2xl sm:w-[320px]">
      <div className="overflow-hidden rounded-[1.9rem] bg-canvas">
        {/* status bar */}
        <div className="flex items-center justify-between bg-emerald-brand px-5 pb-2 pt-3 text-[10px] font-bold text-white">
          <span>9:41</span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-2 w-2 rounded-full bg-mint" />
            Mtaa
          </span>
        </div>
        {/* app bar */}
        <div className="flex items-center justify-between px-4 pb-2 pt-3">
          <div>
            <p className="text-[10px] font-semibold text-outline-brand">Good morning</p>
            <p className="text-sm font-extrabold text-ink">Wema Court 🏡</p>
          </div>
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-brand text-[10px] font-bold text-mint">
            AK
          </div>
        </div>
        {/* urgent need card */}
        <div className="mx-4 rounded-2xl bg-terracotta-mist p-3">
          <p className="text-[10px] font-bold uppercase tracking-wide text-terracotta-deep">
            Urgent need
          </p>
          <p className="mt-1 text-xs font-semibold leading-5 text-ink">
            Looking for 2 people to help push a car out of the gate — B3 entrance
          </p>
          <div className="mt-2 flex items-center gap-2">
            <span className="rounded-full bg-terracotta px-2.5 py-1 text-[10px] font-bold text-white">
              I can help
            </span>
            <span className="text-[10px] font-semibold text-terracotta-deep">
              3 jiranis responded
            </span>
          </div>
        </div>
        {/* feed card */}
        <div className="mx-4 mt-3 rounded-2xl border border-clay-deep bg-white p-3">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-amber-brand text-[10px] font-bold text-amber-deep">
              JM
            </div>
            <div>
              <p className="text-xs font-bold text-ink">Juma M.</p>
              <p className="text-[10px] text-outline-brand">Baraza · 2h ago</p>
            </div>
          </div>
          <p className="mt-2 text-xs leading-5 text-ink-variant">
            Minutes from Saturday&apos;s baraza are up. Vote on the security guard
            schedule before Friday 🗳️
          </p>
          <div className="mt-2.5 flex items-center gap-4 text-[10px] font-semibold text-outline-brand">
            <span className="text-terracotta">♥ 24</span>
            <span>💬 9 replies</span>
          </div>
        </div>
        {/* harambee card */}
        <div className="mx-4 my-3 mb-5 rounded-2xl bg-emerald-deep p-3">
          <p className="text-[10px] font-bold uppercase tracking-wide text-mint">
            Harambee · Water project
          </p>
          <div className="mt-2 h-2 overflow-hidden rounded-full bg-white/20">
            <div className="h-full w-3/4 rounded-full bg-mint" />
          </div>
          <div className="mt-1.5 flex justify-between text-[10px] font-semibold text-white">
            <span>KSh 34,500 raised</span>
            <span className="text-mint">76%</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function HomePage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden bg-linen">
        <div
          aria-hidden
          className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-mint/30 blur-3xl"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute -left-24 top-40 h-72 w-72 rounded-full bg-amber-brand/15 blur-3xl"
        />
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 pb-16 pt-12 sm:px-6 md:grid-cols-2 md:pb-24 md:pt-20">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-emerald-mist bg-white px-3 py-1.5 text-xs font-bold text-emerald-brand">
              <span className="h-2 w-2 rounded-full bg-terracotta" />
              Built for Kenyan mtaani
            </span>
            <h1 className="mt-5 text-4xl font-extrabold leading-[1.08] tracking-tight text-ink sm:text-5xl">
              Your mtaa,{" "}
              <span className="text-emerald-brand">finally connected.</span>
            </h1>
            <p className="mt-5 max-w-lg text-lg leading-8 text-ink-variant">
              SmartJirani brings barazas, harambees, trusted fundis, urgent help
              and everyday sharing into one app — for the people who actually
              live on your street.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                href="/download"
                className="rounded-full bg-emerald-brand px-7 py-3.5 text-center text-base font-bold text-white shadow-sm transition-colors hover:bg-emerald-pressed"
              >
                Download the app
              </Link>
              <a
                href="#features"
                className="rounded-full border border-outline-soft bg-white px-7 py-3.5 text-center text-base font-bold text-ink transition-colors hover:bg-clay"
              >
                Explore features
              </a>
            </div>
            <ul className="mt-7 flex flex-wrap gap-x-5 gap-y-2 text-sm font-semibold text-ink-variant">
              <li className="flex items-center gap-1.5">
                <Check /> Free for residents
              </li>
              <li className="flex items-center gap-1.5">
                <Check /> English &amp; Kiswahili
              </li>
              <li className="flex items-center gap-1.5">
                <Check /> Android
              </li>
            </ul>
          </div>
          <PhoneMockup />
        </div>
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-24">
        <div className="max-w-2xl">
          <p className="text-sm font-bold uppercase tracking-widest text-terracotta">
            Everything in one place
          </p>
          <h2 className="mt-3 text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">
            Made for how neighborhoods actually work
          </h2>
          <p className="mt-4 text-lg leading-8 text-ink-variant">
            Not another group chat. SmartJirani gives your mtaa real tools with
            real accountability.
          </p>
        </div>
        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((f) => (
            <div
              key={f.title}
              className="rounded-3xl border border-clay-deep bg-white p-6 transition-shadow hover:shadow-lg"
            >
              <div
                className={`flex h-12 w-12 items-center justify-center rounded-2xl ${f.tint} ring-4 ${f.ring}/30`}
              >
                <svg
                  viewBox="0 0 24 24"
                  className="h-6 w-6 text-emerald-deep"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden
                >
                  {f.icon}
                </svg>
              </div>
              <h3 className="mt-4 text-lg font-bold text-ink">{f.title}</h3>
              <p className="mt-2 text-sm leading-6 text-ink-variant">{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="bg-clay py-16 md:py-24">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="max-w-2xl">
            <p className="text-sm font-bold uppercase tracking-widest text-terracotta">
              How it works
            </p>
            <h2 className="mt-3 text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">
              Up and running in one evening
            </h2>
          </div>
          <ol className="mt-12 grid gap-8 md:grid-cols-3">
            {steps.map((s) => (
              <li key={s.n} className="relative">
                <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-brand text-lg font-extrabold text-mint">
                  {s.n}
                </span>
                <h3 className="mt-4 text-lg font-bold text-ink">{s.title}</h3>
                <p className="mt-2 text-sm leading-6 text-ink-variant">{s.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* Trust */}
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-24">
        <div className="rounded-[2rem] bg-emerald-deep px-6 py-12 sm:px-12 md:py-16">
          <div className="grid items-center gap-10 md:grid-cols-[1.2fr_1fr]">
            <div>
              <h2 className="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
                Trust is the whole point
              </h2>
              <p className="mt-4 max-w-xl text-base leading-7 text-emerald-mist">
                Every resident has a real profile. Every favor is remembered.
                Reports go to real people in your community — not a black box.
              </p>
            </div>
            <ul className="grid gap-4 sm:grid-cols-2">
              {[
                ["Karma you earn", "Help out and rise to Jirani wa Kuaminika."],
                ["Real profiles", "Display names and verifications, not bots."],
                ["Community moderation", "Neighbors review reported content."],
                ["Your data, your call", "You choose what your mtaa sees."],
              ].map(([title, body]) => (
                <li
                  key={title}
                  className="rounded-2xl bg-white/10 p-4 ring-1 ring-white/15"
                >
                  <p className="text-sm font-bold text-mint">{title}</p>
                  <p className="mt-1 text-xs leading-5 text-white/80">{body}</p>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
        <div className="flex flex-col items-center gap-6 rounded-[2rem] border border-clay-deep bg-linen px-6 py-14 text-center">
          <h2 className="max-w-xl text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">
            Ready to meet your neighbors?
          </h2>
          <p className="max-w-md text-base leading-7 text-ink-variant">
            Download SmartJirani, join your mtaa, and be the neighbor everyone
            is glad to have.
          </p>
          <Link
            href="/download"
            className="rounded-full bg-terracotta px-8 py-3.5 text-base font-bold text-white shadow-sm transition-colors hover:bg-terracotta-deep"
          >
            Get SmartJirani — free
          </Link>
        </div>
      </section>
    </>
  );
}

function Check() {
  return (
    <svg
      viewBox="0 0 20 20"
      className="h-4 w-4 text-emerald-brand"
      fill="currentColor"
      aria-hidden
    >
      <path
        fillRule="evenodd"
        d="M16.7 5.3a1 1 0 0 1 0 1.4l-7.5 7.5a1 1 0 0 1-1.4 0L3.3 9.7a1 1 0 0 1 1.4-1.4l3.8 3.8 6.8-6.8a1 1 0 0 1 1.4 0Z"
        clipRule="evenodd"
      />
    </svg>
  );
}
