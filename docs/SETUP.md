# Manual setup required (cannot be automated from source code)

These steps must be completed by a developer with access to the Google Cloud Console and the
Play Console — per REQUIREMENTS.md section 22, source code intentionally does not attempt to
create or modify Google Cloud credentials.

1. **Enable APIs** on the Google Cloud project tied to `google_android_oauth_client_id`
   (`app/src/main/res/values/strings.xml`):
   - Gmail API
   - Google Calendar API
2. **OAuth consent screen**: configure it (support email, scopes below, test users while in
   Testing publishing status).
3. **Scopes actually requested by this app** (see `data/auth/GoogleAuthManager.kt` →
   `AccountAuthScopes`):
   - `.../auth/userinfo.email`, `.../auth/userinfo.profile` (identify a connected account)
   - `.../auth/gmail.readonly`
   - `.../auth/calendar.readonly`
   - `.../auth/calendar.events` (only used once a proposed event is accepted and mirrored into
     Calendar — `CalendarRepository.createEvent`)
4. **Android OAuth client**: already present (`app/src/main/res/values/strings.xml`). Confirm its
   package name (`com.inialpha.executiveai`) and SHA-1
   (`D0:A1:3A:66:DB:8C:94:07:5E:E1:51:A2:F8:25:E9:74:1F:AE:76:E2`, the debug key) match what's
   registered in Cloud Console. A **separate SHA-1 must be added for the release signing key**
   once one exists.
5. **Production verification**: if this app moves beyond a handful of test users, Google will
   require OAuth verification for the Gmail/Calendar scopes above (a review process, not a code
   change).
6. **KSP version pin**: `build.gradle.kts` pins a KSP version by convention
   (`<kotlinVersion>-<kspVersion>`) — verify the exact current release for Kotlin 2.3.21 at
   https://github.com/google/ksp/releases before the first build; this could not be verified from
   this environment.
7. **First real build**: this repository was authored without access to the Android SDK /
   `dl.google.com` / `maven.google.com` (network-restricted development sandbox), so `./gradlew
   assembleDebug` has not actually been run against this code. Expect to spend a first pass fixing
   any import/API-surface mismatches a real Android Studio sync will surface — the code was
   written carefully against known-correct Compose/Room/Retrofit/Identity APIs, but this has not
   been compiler-verified.
8. **Notification icon**: `notification/NotificationHelper.kt` currently uses a placeholder
   system icon (`android.R.drawable.ic_dialog_info`) — swap in a real monochrome notification icon
   asset before shipping.
