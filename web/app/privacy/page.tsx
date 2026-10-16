import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Privacy policy",
  description: "How SmartJirani collects, uses and protects your data.",
  alternates: { canonical: "/privacy" },
};

export default function PrivacyPage() {
  return (
    <article className="mx-auto max-w-3xl px-4 py-16 sm:px-6 md:py-20">
      <h1 className="text-4xl font-extrabold tracking-tight text-ink">Privacy policy</h1>
      <p className="mt-2 text-sm text-outline-brand">Last updated: October 2026</p>
      <div className="mt-8 space-y-8 text-sm leading-6 text-ink-variant">
        <section>
          <h2 className="text-lg font-bold text-ink">What we collect</h2>
          <p className="mt-2">
            Your account (email, display name, avatar), the neighborhood you
            join, and the content you post — posts, comments, listings, reviews,
            RSVPs and messages. If you grant permissions, we store photos you
            upload. We do not track your location in the background; location is
            only used when you choose to share it on a post or profile.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">How your data is used</h2>
          <p className="mt-2">
            To run the service: showing your posts to your mtaa, delivering
            notifications, calculating karma, and keeping the platform safe.
            Your privacy settings control what neighbors see — including
            whether your house number is visible. We never sell your data.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Who can see what</h2>
          <p className="mt-2">
            Content in a neighborhood is visible to its members. Messages are
            visible only to you and the people you chat with. Admins of your
            mtaa can see member lists and reported content. Our team only
            accesses content when handling a report or a technical issue.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Your choices</h2>
          <p className="mt-2">
            You can edit or delete most of your content at any time, adjust
            visibility per item in privacy settings, and request account
            deletion by emailing{" "}
            <a href="mailto:privacy@smartjirani.app" className="font-semibold text-emerald-brand hover:underline">
              privacy@smartjirani.app
            </a>
            . Deleting your account removes your profile and personal data,
            except records we must keep for safety and legal reasons.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Security</h2>
          <p className="mt-2">
            Data is encrypted in transit and access is restricted by row-level
            security — every request is checked against who you are and what
            you&apos;re allowed to see.
          </p>
        </section>
        <section>
          <h2 className="text-lg font-bold text-ink">Contact</h2>
          <p className="mt-2">
            Questions about privacy:{" "}
            <a href="mailto:privacy@smartjirani.app" className="font-semibold text-emerald-brand hover:underline">
              privacy@smartjirani.app
            </a>
          </p>
        </section>
      </div>
    </article>
  );
}
