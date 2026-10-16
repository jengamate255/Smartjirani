# Phase 0 status (auto-resume notes) — updated before reboot 2026-10-09 ~02:45

## Done
- Gradle 8.11.1 unix `gradlew` added; `secret()` in app/build.gradle.kts; Spotless 8.10.3 + ktlint 1.8.0 + Kover 0.9.11 wired; `.editorconfig`; lint block; CI workflow updated.
- Disk: C: ~14 GB free. `.gradle` -> `E:\gradle-home` (junction at C:\Users\Dave\.gradle). Android SDK -> `E:\AndroidSdk` (ANDROID_HOME/ANDROID_SDK_ROOT env + local.properties + install_stream.bat repointed).
- ktlint filename fixes: `Spacing.kt` -> `SmartSpacing.kt`, `LiveKitTokenHelper.kt` -> `LiveKitTokenResponse.kt` (+ docs/designs/README.md).
- `spotlessApply` PASSED; `spotlessCheck` PASSED (EXIT=0, spotless_check.log).

## Running (auto-resumes at logon via scheduled task "SJGradleRun")
- `_run_gradle.bat` chains: lintDebug -> test -> assembleDebug, logs: lint_debug.log / test_run.log / assemble_debug.log (EXIT=<code> appended per step).
- Task hardened: battery/idle stops disabled, ExecutionTimeLimit=PT0S.

## Next steps after the three logs finish
1. Fix any lint errors (lint_debug.log), failing tests (test_run.log), or compile errors (assemble_debug.log).
2. Clean up: delete `_run_gradle.bat` + `SJGradleRun` task + *.log in repo root when Phase 0 verified; `git init` NOT done (needs explicit user request).
3. Report Phase 0 complete, then Phase A: push notifications (migrations 025_push_channel.sql; FCM sender; notification_deliveries.channel include 'push').

## Machine notes
- 2 cores; MT5 bots + other dev servers share CPU (user chose not to pause them). E: reads ~9.5 MB/s. Long Gradle startups (30-60 min) are normal on this box.
- Tool shell kills long commands sometimes (spurious ChildProcess.kill) — builds run via scheduled task to survive; poll logs, don't relaunch while a java.exe exists.
- supabase CLI crashes shell (needed later for db push).
