import type { Metadata } from "next";
import Link from "next/link";
import { DownloadPanel } from "./download-panel";

export const metadata: Metadata = {
  title: "Download the app",
  description:
    "Get SmartJirani on Android. Direct APK download with published SHA-256 checksums, install steps and update history.",
  alternates: { canonical: "/download" },
};

const steps = [
  {
    title: "Download the APK",
    body: "Tap the download button above. The file is signed by SmartJirani and its SHA-256 checksum is published with every release.",
  },
  {
    title: "Allow installs from this source",
    body: "Android will ask for permission to install the app from your browser. This is normal for apps outside the Play Store — tap Allow.",
  },
  {
    title: "Install and open",
    body: "Open the installed app, create your profile with your real name, and join your mtaa.",
  },
];

const changelog = [
  {
    version: "Coming next",
    items: [
      "First public APK release with direct download",
      "Realtime messaging and neighborhood feed",
      "Voice and video calls between jiranis",
    ],
  },
];

export default function DownloadPage() {
  return (
    <>
      <section className="bg-linen">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <div className="max-w-2xl">
            <span className="inline-flex items-center gap-2 rounded-full border border-emerald-mist bg-white px-3 py-1.5 text-xs font-bold text-emerald-brand">
              Android · Free
            </span>
            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-ink sm:text-5xl">
              Get SmartJirani
            </h1>
            <p className="mt-4 text-lg leading-8 text-ink-variant">
              Join your mtaa in minutes. Free for residents, always.
            </p>
            <DownloadPanel />
          </div>

          <div className="mt-12 grid gap-6 md:grid-cols-3">
            {steps.map((s, i) => (
              <div
                key={s.title}
                className="rounded-3xl border border-clay-deep bg-white p-6"
              >
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-clay text-sm font-extrabold text-emerald-brand">
                  {i + 1}
                </span>
                <h2 className="mt-3 text-base font-bold text-ink">{s.title}</h2>
                <p className="mt-2 text-sm leading-6 text-ink-variant">{s.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-12 sm:px-6 md:py-16">
        <div className="grid gap-10 md:grid-cols-2">
          <div>
            <h2 className="text-2xl font-extrabold tracking-tight text-ink">
              Why direct APK?
            </h2>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-ink-variant">
              <li className="flex gap-2">
                <Check /> Works on Android 8.0 and above, including phones without
                Play Store access.
              </li>
              <li className="flex gap-2">
                <Check /> Every build is versioned and checksum-verified — you can
                confirm you got the real file.
              </li>
              <li className="flex gap-2">
                <Check /> The app tells you when a new version is ready and links
                you straight here.
              </li>
            </ul>
          </div>
          <div>
            <h2 className="text-2xl font-extrabold tracking-tight text-ink">
              Update history
            </h2>
            {changelog.map((c) => (
              <div
                key={c.version}
                className="mt-4 rounded-3xl border border-clay-deep bg-white p-6"
              >
                <p className="text-sm font-bold text-emerald-brand">{c.version}</p>
                <ul className="mt-3 space-y-2">
                  {c.items.map((item) => (
                    <li key={item} className="flex gap-2 text-sm leading-6 text-ink-variant">
                      <span className="text-terracotta" aria-hidden>
                        •
                      </span>
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
            <p className="mt-4 text-sm text-ink-variant">
              Questions about installing?{" "}
              <Link href="/help" className="font-semibold text-emerald-brand hover:underline">
                Read the help center
              </Link>
              .
            </p>
          </div>
        </div>
      </section>
    </>
  );
}

function Check() {
  return (
    <svg
      viewBox="0 0 20 20"
      className="mt-1 h-4 w-4 shrink-0 text-emerald-brand"
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
