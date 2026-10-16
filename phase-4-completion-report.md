# Phase 4 Completion Report: AI Tracker Dashboard

**Date:** 2026-10-06  
**Status:** ✅ Complete

## Overview
Built a Next.js dashboard page that displays AI feature usage, performance metrics, and costs from the `ai_metrics` table.

## Implementation

### Files Created/Modified

1. **web/app/ai-tracker/page.tsx** (created)
   - Server component that fetches from Supabase REST API
   - No external dependencies (no @supabase/supabase-js, no shadcn/ui)
   - Uses plain `fetch()` against PostgREST endpoint
   - Matches existing Savannah Emerald design system

2. **web/.env.local** (created)
   - Supabase URL and anon key for the web project
   - Copied from root .env

3. **web/lib/site.ts** (modified)
   - Added "AI Tracker" nav link to site header

### Dashboard Features

**Key Metrics Cards (4):**
- Total AI Calls (real vs mock breakdown)
- Success Rate (successful vs failed)
- Average Latency (across all providers)
- Total Cost (estimated USD)

**Provider Breakdown Table:**
- Calls per provider (groq, gemini, mock)
- Mock call count
- Success rate per provider

**Feature Usage Table:**
- Calls per feature (triage, assistant, summarize, dedup, analyze, transcribe, insights, route)
- Average latency per feature
- Success rate per feature

**Recent Activity Table:**
- Last 50 AI calls
- Timestamp, feature, provider, model, latency, status
- Color-coded badges (green for success, red for failure, gray for mock)

### Technical Decisions

1. **Static Export Compatibility**
   - Removed `export const dynamic = "force-dynamic"` (incompatible with `output: "export"`)
   - Removed `cache: "no-store"` from fetch (forces dynamic rendering)
   - Dashboard data is fetched at build time and baked into HTML
   - To get live data, would need to switch to server-rendered deployment or rebuild site after AI calls

2. **No External Dependencies**
   - Web project is a marketing site with minimal dependencies
   - Adding @supabase/supabase-js and shadcn/ui would bloat the bundle
   - Plain fetch against REST API is lighter and works for read-only dashboard

3. **Design System Alignment**
   - Used existing Tailwind tokens: linen, terracotta, emerald-brand, ink, clay, etc.
   - Rounded-2xl borders, extrabold headings, consistent spacing
   - Matches services/communities/business page patterns

### Current State

**ai_metrics table:** Empty (0 rows)

This is expected — the civic-ai edge function was just deployed with metrics recording wired in, but no AI calls have been made from the Android app yet.

Once users start using AI features in the app:
- Triage report → records metric
- AI assistant chat → records metric
- Summarize report → records metric
- Dedup check → records metric
- Analyze image → records metric
- Transcribe audio → records metric
- Smart routing → records metric
- Weekly insights → records metric

The dashboard will automatically populate on next site rebuild.

### Build Verification

```
✓ Compiled successfully in 14.4s
✓ Generating static pages (17/17)
✓ Exporting (2/2)
```

Build passes. Page is statically generated and exported.

### Visual Verification

Dev server tested at http://localhost:3003/ai-tracker:
- ✅ Page loads successfully
- ✅ "AI Tracker" heading renders
- ✅ "Total AI Calls" metric card displays
- ✅ Empty state messages show ("No AI calls recorded yet")
- ✅ Fetches from Supabase REST API (returns empty array)
- ✅ Navigation link appears in site header

## Next Steps

1. **Test AI features in Android app** — trigger triage, assistant, summarize, etc. to populate ai_metrics
2. **Rebuild site** — run `npm run build` in web/ to refresh dashboard data
3. **Deploy site** — push updated static export to hosting
4. **Monitor dashboard** — verify metrics appear correctly after AI calls

## Notes

- Dashboard shows "No AI calls recorded yet" empty state when table is empty
- All metrics are read from `ai_metrics` table via anon key (RLS allows authenticated read)
- Cost tracking is estimated — actual costs depend on provider pricing
- Latency is measured client-side in edge function (includes network round-trip)
