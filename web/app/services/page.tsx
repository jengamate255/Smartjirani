import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Find trusted fundis & services",
  description:
    "Hire plumbers, electricians, cleaners, mechanics and more from your own neighborhood — rated and reviewed by the people who live around you.",
  alternates: { canonical: "/services" },
};

const categories = [
  { name: "Plumbing", emoji: "🔧" },
  { name: "Electrical", emoji: "💡" },
  { name: "Carpentry", emoji: "🪚" },
  { name: "Masonry & welding", emoji: "🧱" },
  { name: "Cleaning", emoji: "🧹" },
  { name: "Moving & transport", emoji: "🚚" },
  { name: "Salon & barber", emoji: "💈" },
  { name: "Mechanics & car wash", emoji: "🚗" },
  { name: "Tutoring", emoji: "📚" },
  { name: "Childcare", emoji: "🧸" },
  { name: "Cooking & catering", emoji: "🍲" },
  { name: "Gardening", emoji: "🌱" },
];

const steps = [
  {
    title: "Search your mtaa",
    body: "Browse categories or ask Jirani AI in plain English or Kiswahili — \"naitaji plumber karibu na Wema Court\".",
  },
  {
    title: "Check trust before you call",
    body: "Every fundi has a profile with karma, endorsements and reviews from neighbors you share a gate with.",
  },
  {
    title: "Chat, call, book",
    body: "Message or call straight from the app, agree on the job, and leave a review so the next neighbor knows what to expect.",
  },
];

export default function ServicesPage() {
  return (
    <>
      <section className="bg-linen">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <div className="max-w-2xl">
            <p className="text-sm font-bold uppercase tracking-widest text-terracotta">
              Services &amp; fundis
            </p>
            <h1 className="mt-3 text-4xl font-extrabold tracking-tight text-ink sm:text-5xl">
              Find a fundi you can actually trust
            </h1>
            <p className="mt-4 text-lg leading-8 text-ink-variant">
              Stop asking &quot;una mtu wa plumbing?&quot; in five different groups.
              The skilled people you need are already in your mtaa — SmartJirani
              makes them easy to find and easy to trust.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                href="/download"
                className="rounded-full bg-emerald-brand px-7 py-3.5 text-center text-base font-bold text-white transition-colors hover:bg-emerald-pressed"
              >
                Find services near you
              </Link>
              <Link
                href="/business"
                className="rounded-full border border-outline-soft bg-white px-7 py-3.5 text-center text-base font-bold text-ink transition-colors hover:bg-clay"
              >
                I offer a service
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-12 sm:px-6 md:py-16">
        <h2 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">
          Popular categories
        </h2>
        <div className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {categories.map((c) => (
            <div
              key={c.name}
              className="flex items-center gap-3 rounded-2xl border border-clay-deep bg-white px-4 py-3.5"
            >
              <span className="text-xl" aria-hidden>
                {c.emoji}
              </span>
              <span className="text-sm font-bold text-ink">{c.name}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-clay py-16 md:py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <h2 className="text-3xl font-extrabold tracking-tight text-ink">
            How hiring works
          </h2>
          <ol className="mt-10 grid gap-8 md:grid-cols-3">
            {steps.map((s, i) => (
              <li key={s.title}>
                <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta text-lg font-extrabold text-white">
                  {i + 1}
                </span>
                <h3 className="mt-4 text-lg font-bold text-ink">{s.title}</h3>
                <p className="mt-2 text-sm leading-6 text-ink-variant">{s.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="flex flex-col items-center gap-5 rounded-[2rem] border border-clay-deep bg-linen px-6 py-14 text-center">
          <h2 className="max-w-xl text-3xl font-extrabold tracking-tight text-ink">
            Are you a fundi or run a service?
          </h2>
          <p className="max-w-md text-base leading-7 text-ink-variant">
            Get discovered by neighbors who need exactly what you offer — no
            commission during early access.
          </p>
          <Link
            href="/business"
            className="rounded-full bg-terracotta px-8 py-3.5 text-base font-bold text-white transition-colors hover:bg-terracotta-deep"
          >
            List your services free
          </Link>
        </div>
      </section>
    </>
  );
}
