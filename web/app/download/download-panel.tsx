"use client";

import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";

type ReleaseInfo = {
  version: string;
  apkUrl: string;
  sha256?: string;
  sizeMb?: number;
  releasedAt?: string;
};

const VERSION_API =
  process.env.NEXT_PUBLIC_VERSION_API_URL || "";

/**
 * Phase 3 will deploy the version Edge Function; until NEXT_PUBLIC_VERSION_API_URL
 * is set this renders a graceful "coming soon" state and the page stays honest.
 */
export function DownloadPanel() {
  const [release, setRelease] = useState<ReleaseInfo | null>(null);
  const [state, setState] = useState<"loading" | "ready" | "unavailable">(
    VERSION_API ? "loading" : "unavailable"
  );

  useEffect(() => {
    if (!VERSION_API) return;
    let cancelled = false;
    fetch(VERSION_API, { headers: { Accept: "application/json" } })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((data: ReleaseInfo) => {
        if (cancelled) return;
        if (data?.apkUrl) {
          setRelease(data);
          setState("ready");
        } else {
          setState("unavailable");
        }
      })
      .catch(() => {
        if (!cancelled) setState("unavailable");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (state === "ready" && release) {
    return (
      <div className="mt-8">
        <a
          href={release.apkUrl}
          className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-emerald-brand px-8 py-4 text-base font-bold text-white shadow-sm transition-colors hover:bg-emerald-pressed sm:w-auto"
          download
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M12 4v11m0 0 4-4m-4 4-4-4M4 19h16" />
          </svg>
          Download APK ({release.version})
        </a>
        <p className="mt-3 text-xs text-outline-brand">
          {(release.sizeMb ? `~${release.sizeMb} MB · ` : "") +
            (release.releasedAt
              ? `Updated ${new Date(release.releasedAt).toLocaleDateString("en-KE", { day: "numeric", month: "long", year: "numeric" })}`
              : "Latest release")}
        </p>
        {release.sha256 && (
          <p className="mt-3 max-w-md break-all rounded-xl bg-clay px-3 py-2 font-mono text-[11px] leading-5 text-ink-variant">
            SHA-256: {release.sha256}
          </p>
        )}
        <div className="mt-6 flex max-w-md items-center gap-4 rounded-2xl bg-clay p-4">
          <QRCodeSVG
            value={release.apkUrl}
            size={104}
            bgColor="transparent"
            fgColor="#00442F"
            level="M"
          />
          <div>
            <p className="text-sm font-bold text-ink">Scan to install</p>
            <p className="mt-1 text-xs leading-5 text-ink-variant">
              Point your phone camera at the code to open this download
              directly on your Android device.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mt-8">
      <div className="inline-flex w-full flex-col items-start gap-2 sm:w-auto">
        <span className="inline-flex w-full cursor-not-allowed items-center justify-center gap-2 rounded-full bg-emerald-mist/40 px-8 py-4 text-base font-bold text-emerald-deep/60 sm:w-auto">
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M12 4v11m0 0 4-4m-4 4-4-4M4 19h16" />
          </svg>
          APK download
        </span>
        <p className="text-xs font-semibold text-terracotta-deep">
          {state === "loading"
            ? "Checking for the latest release…"
            : "First public APK lands with our next release — Play Store to follow."}
        </p>
      </div>
    </div>
  );
}
