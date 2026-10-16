import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "SmartJirani for communities",
  description:
    "Give your estate or village real tools: baraza meetings with RSVPs, street notices, polls, harambee funds and an urgent-needs channel that actually reaches people.",
  alternates: { canonical: "/communities" },
};

const benefits = [
  {
    title: "Baraza meetings & polls",
    body: "Publish agendas, collect RSVPs, run votes on security, water and projects — and keep minutes where everyone can find them.",
  },
  {
    title: "Street notices",
    body: "Water cuts, power maintenance, lost items — official notices from your association, not rumors from the group chat.",
  },
  {
    title: "Harambee funds",
    body: "Raise for funerals, medical bills and mtaa projects with transparent contribution tracking everyone can see.",
  },
  {
    title: "Urgent needs",
    body: "A dedicated channel for emergencies. When someone shouts, the whole mtaa hears — with helpers responding in minutes.",
  },
  {
    title: "Resident directory",
    body: "Know who your neighbors are, with karma and endorsements that show who shows up for the community.",
  },
  {
    title: "Direct messaging & calls",
    body: "Chat one-on-one or in mtaa groups, with voice and video calls when typing won't do.",
  },
];

const start = [
  {
    n: "1",
    title: "Create your neighborhood",
    body: "Pick your estate, court or village name and set it up as the admin — it takes about two minutes.",
  },
  {
    n: "2",
    title: "Invite your neighbors",
    body: "Share the join link or QR code on your gate WhatsApp group. Neighbors verify with their real profiles.",
  },
  {
    n: "3",
    title: "Set your mtaa rules",
    body: "Decide who can post notices, run barazas and manage funds. Add admins from trusted residents.",
  },
];

export default function CommunitiesPage() {
  return (
    <>
      <section className="bg-linen">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <div className="max-w-2xl">
            <p className="text-sm font-bold uppercase tracking-widest text-terracotta">
              For resident associations &amp; villages
            </p>
            <h1 className="mt-3 text-4xl font-extrabold tracking-tight text-ink sm:text-5xl">
              Your mtaa deserves better than a WhatsApp group
            </h1>
            <p className="mt-4 text-lg leading-8 text-ink-variant">
              Group chats bury the notice about water cuts under 200 forwards.
              SmartJirani gives your community a proper home — with structure,
              accountability and a memory.
            </p>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-12 sm:px-6 md:py-16">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
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
            Start your mtaa in three steps
          </h2>
          <ol className="mt-10 grid gap-8 md:grid-cols-3">
            {start.map((s) => (
              <li key={s.n}>
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

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="flex flex-col items-center gap-5 rounded-[2rem] bg-emerald-deep px-6 py-14 text-center">
          <h2 className="max-w-xl text-3xl font-extrabold tracking-tight text-white">
            Bring SmartJirani to your community
          </h2>
          <p className="max-w-md text-base leading-7 text-emerald-mist">
            Free for residents and associations. If your mtaa grows, we grow
            with it.
          </p>
          <Link
            href="/download"
            className="rounded-full bg-mint px-8 py-3.5 text-base font-bold text-emerald-deep transition-colors hover:bg-emerald-mist"
          >
            Download the app
          </Link>
        </div>
      </section>
    </>
  );
}
