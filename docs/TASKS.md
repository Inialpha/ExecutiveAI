# Executive AI — implementation checklist

Legend: [x] done this pass · [~] scaffolded, needs another pass · [ ] not started

## Foundation
- [x] Gradle: Kotlin/KSP/Room/Retrofit/Compose/Credentials/WorkManager deps
- [x] Package structure (domain/data/di/notification/voice/ui/viewmodel)

## Domain & persistence
- [x] Domain models (Account, EmailMessage, EmailInsight, ExecutiveItem, CalendarEvent)
- [x] Room entities, DAOs, AppDatabase, JSON converters for insight sub-lists

## Auth
- [x] GoogleAuthManager on Identity.getAuthorizationClient (current, non-deprecated API)
- [x] Incremental scope requests (identity → gmail.readonly → calendar.readonly/.events)
- [ ] Verified end-to-end against a real Cloud Console project (needs SETUP.md steps 1-4)

## Gmail
- [x] REST DTOs + API interface
- [x] MIME → plain-text mapper (multipart, base64url, html fallback)
- [x] Sync with empty-inbox / malformed-message / auth-expired / network-error handling
- [ ] Pagination beyond first page (`nextPageToken` is modeled but not yet followed)
- [ ] Incremental sync (currently always re-fetches most-recent N; no delta/history-based sync)

## AI gateway
- [x] Request/response DTOs matching REQUIREMENTS.md contract
- [x] Dynamic current_datetime with offset (no hard-coded dates)
- [x] No client-side chunking; malformed individual results skipped, not fatal
- [x] Proposal creation: every event/action/deadline/reminder → PROPOSED ExecutiveItem
- [ ] Confirm live response envelope shape against the real backend (see ARCHITECTURE.md caveat)

## Calendar
- [x] Read sync (REST DTOs, API, repository, Room)
- [x] Event creation (only invoked after user acceptance)
- [ ] Conflict detection (accepted proposed EVENT vs. existing CalendarEvent overlap) — not yet implemented
- [ ] Event update/delete

## Proposal workflow
- [x] PROPOSED / EDITED / ACCEPTED / REJECTED / COMPLETED state machine
- [x] Accept/Edit/Reject/Complete wired from Tasks/Reminders/EmailInsight screens

## Notifications / reminders
- [x] Channel setup, AlarmManager scheduling (exact where permitted, inexact fallback)
- [x] Boot-reschedule via WorkManager
- [ ] Real notification icon asset (placeholder in use — see SETUP.md)
- [ ] Calendar-event and task-due-date notifications (only REMINDER-type items are scheduled today)

## Voice
- [x] Native SpeechRecognizer wrapper, on-device preferred
- [x] Transcript → PROPOSED VOICE_COMMAND item
- [ ] Real intent/entity extraction from transcript (currently the raw transcript becomes the title)

## UI / navigation
- [x] All 11 destinations + Scaffold (bottom nav + top bar overflow)
- [x] Onboarding, Dashboard, Important Emails, Email Insight, Upcoming, Tasks, Reminders,
      Calendar, Assistant, Accounts, Settings — all reading real ViewModel state
- [x] Loading / empty / error states via shared components
- [ ] Accessibility pass (content descriptions, touch targets, contrast audit)
- [ ] Edit-item UI (ViewModel.edit() exists; screens call accept/reject but not a real edit dialog yet)

## Security
- [x] No Groq key, no OAuth tokens, no signing keys anywhere in source
- [x] Android OAuth client ID left in place (not a secret) with explanatory comment

## Not yet done at all
- [ ] Automated tests (unit or instrumented) — none written this pass
- [ ] Actual `./gradlew assembleDebug` run (blocked by sandbox network — see SETUP.md #7)
- [ ] App icon / branding assets beyond the existing theme colors
