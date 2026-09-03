You are working on an existing Android application called Executive AI. Your job is to inspect the existing repository first, understand what has already been implemented, and then continue development without breaking existing functionality.

PRODUCT PURPOSE

Executive AI is not simply an email reader or reminder app. It is a personal executive assistant.

Its purpose is to help a person who has many responsibilities turn information into organized action.

The core concept is:

Information → Understanding → Decision → Commitment → Reminder → Execution

The application should continuously help the user answer:

- What is important?
- What do I need to do?
- What is coming up?
- What deadlines am I approaching?
- What have I committed to?
- What should I be reminded about?
- What requires my attention today?

Gmail is only the first information source. The architecture must allow Calendar and future information sources to be added.

---

EXISTING ANDROID STACK

Inspect the repository and preserve the existing implementation.

The application currently uses:

- Kotlin
- Jetpack Compose
- Material 3
- Android architecture with ViewModels/repositories
- Room/local persistence
- Android GitHub Actions
- compileSdk 36
- targetSdk 36
- minSdk 26
- AGP 9.4.0
- Kotlin 2.3.21
- Gradle 9.6
- JDK 17

Do not unnecessarily replace the existing architecture or dependencies.

The current visual direction is:

- Background: "#07111F"
- Primary: "#0EA5A4"
- Accent: "#22D3EE"
- Cards: "#102033"
- Text: "#F8FAFC"

The design should feel like a premium executive command center: clean, professional, readable, focused, and not cluttered.

---

GOOGLE ACCOUNT ARCHITECTURE

A user must be able to connect multiple Google/Gmail accounts.

For example:

- personal@gmail.com
- work@gmail.com
- business@gmail.com

Each account must be independently identifiable.

Google authentication and Gmail/Calendar access happen directly on Android using Google's current Android authorization approach.

Do NOT send Google OAuth tokens to our backend.

Store the necessary account information securely on the device.

The Connected Accounts screen must allow the user to:

- Add Google account
- See connected accounts
- Select/manage accounts
- Remove/disconnect an account
- Know which accounts are being synchronized

Request only the permissions actually required.

Gmail currently requires read access for Executive AI's intelligence functionality.

Calendar should initially support reading calendar information and later event creation/update.

GOOGLE OAUTH CLIENT CONFIGURATION

The Android OAuth client ID is a public identifier, not a secret, and may be committed to the repository.

The configured Android OAuth client ID is stored in:

app/src/main/res/values/strings.xml

Resource name:

google_android_oauth_client_id

Do not put Google OAuth access tokens, refresh tokens, private keys, keystores, or other credentials in source control.

The Android OAuth client must be registered in Google Cloud with:

- Application type: Android
- Package name: com.inialpha.executiveai
- The SHA-1 certificate fingerprint corresponding to the signing key used by the build variant.

The current development/debug SHA-1 is documented by the GitHub Actions signing-report workflow. Production/release builds will require their own release/Play App Signing certificate configuration.

Required Google APIs and OAuth consent configuration must be enabled/configured in Google Cloud before live Gmail/Calendar synchronization can work.

---

FRONTEND RESPONSIBILITY

Android is responsible for:

1. Google authentication
2. Gmail API access
3. Calendar API access
4. Retrieving emails
5. Managing multiple Google accounts
6. Local persistence with Room
7. Synchronization state
8. Displaying emails and insights
9. Displaying calendar events
10. User interaction
11. Accept/Edit/Reject decisions
12. Creating calendar events/reminders/tasks
13. Android notifications and alarms
14. Voice input using Android's native SpeechRecognizer
15. Dashboard and executive workflow

The backend should NOT retrieve Gmail.

The backend should NOT receive Google OAuth tokens.

The backend should NOT become the application's database.

---

EXISTING AI BACKEND

We already have an existing backend:

Repository: Inialpha/EmailManager

Endpoint:
"POST https://emailmanager-hz68.onrender.com/extract-insights-from-emails/"

The backend exists primarily as an AI gateway for email intelligence.

The Android application retrieves Gmail messages itself and sends the relevant email information to this endpoint.

The backend then sends each email individually to Groq and returns structured intelligence.

IMPORTANT:

- Android sends all selected/retrieved emails for an account in one HTTP request.
- The backend processes the emails individually/sequentially.
- Do NOT implement chunking on Android.
- Do NOT modify the existing "summarizer.py".
- Do NOT break the existing desktop/web functionality in EmailManager.
- Do NOT move Gmail retrieval into the backend.
- Do NOT store raw Gmail content permanently on the backend.

The request structure is approximately:

{
  "current_datetime": "2026-09-03T20:04:00+01:00",
  "emails": [
    {
      "id": "gmail-message-id",
      "thread_id": "gmail-thread-id",
      "sender": "person@example.com",
      "subject": "Project meeting tomorrow",
      "content": "Email body...",
      "snippet": "Short Gmail snippet..."
    }
  ]
}

Android must dynamically provide the current date/time and timezone offset.

The backend returns structured intelligence similar to:

{
  "id": "gmail-message-id",
  "thread_id": "gmail-thread-id",
  "sender": "person@example.com",
  "subject": "Project meeting tomorrow",
  "is_important": true,
  "summary": "Meeting to discuss the project.",
  "events": [
    {
      "title": "Project meeting",
      "date": "2026-09-04",
      "time": "10:00",
      "location": "Main office",
      "description": "Discuss the new project."
    }
  ],
  "actions": [
    {
      "title": "Bring project documents",
      "description": "Bring the project documents to the meeting.",
      "due_date": null
    }
  ],
  "deadlines": [
    {
      "title": "Final proposal",
      "date": "2026-09-10",
      "description": "Submit the final proposal."
    }
  ],
  "reminders": [
    {
      "title": "Project meeting",
      "datetime": "2026-09-04T10:00:00+01:00",
      "reason": "Meeting identified from email."
    }
  ]
}

Do not hard-code this sample data. Build proper Kotlin data models matching the backend contract.

The Gmail "id" and "thread_id" must always be preserved so Executive AI can associate insights with the original Gmail message/thread.

---

IMPORTANT PRODUCT WORKFLOW

The application should follow this workflow:

Google Accounts
      ↓
Gmail / Calendar
      ↓
Android Synchronization
      ↓
Local Executive AI Data
      ↓
Email Intelligence Backend
      ↓
Structured Insights
      ↓
Executive AI Dashboard
      ↓
User Review
      ↓
Accept / Edit / Reject
      ↓
Task / Event / Reminder / Alarm
      ↓
Android Notification
      ↓
User Execution
      ↓
Completed

AI suggestions must NOT automatically become commitments without user control.

For example, if an email says:

"Let's meet Friday at 2 PM."

Executive AI should propose:

Meeting — Friday, 2:00 PM

The user can:

- Accept
- Edit
- Reject

Only after acceptance should the application create the appropriate calendar event/reminder/task.

---

IMPLEMENT THIS MILESTONE

Do NOT stop after implementing one phase and wait for instructions.

Work through the following as one integrated milestone:

1. Complete the application UI/navigation

Implement and connect the major screens:

- Onboarding
- Executive Dashboard
- Important Emails
- Email Insight
- Upcoming
- Tasks/Actions
- Reminders
- Calendar
- AI Assistant
- Connected Accounts
- Settings

Use real navigation and proper state management, not isolated mock screens.

2. Google account integration

Implement multiple Google account support and the required authorization architecture.

Make Connected Accounts functional.

3. Gmail integration

Implement Gmail retrieval directly from Android.

Create proper Gmail/domain models.

Support multiple accounts.

Synchronize relevant emails into Room.

Preserve Gmail message/thread IDs.

Display real Gmail information in the application.

4. AI intelligence integration

Connect Android to the existing EmailManager endpoint.

Send the required email fields plus "current_datetime".

Parse the structured response into Kotlin models.

Display:

- summaries
- important emails
- events
- actions
- deadlines
- reminders

Handle loading, errors, empty states, and partial failures properly.

5. Calendar integration

Implement the foundation for Google Calendar synchronization.

Display upcoming calendar events.

Associate calendar information with the executive dashboard.

Create the architecture required for future event creation/update and conflict detection.

6. Executive action architecture

Implement the data/state architecture for:

- proposed events
- proposed tasks
- deadlines
- reminders
- user acceptance
- editing
- rejection
- completion

The goal is that later phases can add Android alarms/notifications without redesigning the application.

---

CRITICAL DEVELOPMENT RULES

Before changing anything:

1. Inspect the entire existing repository.
2. Understand the current architecture.
3. Identify what is already working.
4. Preserve working functionality.
5. Do not unnecessarily rewrite existing code.
6. Do not introduce secrets into the APK.
7. Never embed the Groq API key in Android.
8. Never send Google OAuth tokens to the AI backend.
9. Keep the AI backend stateless regarding Gmail data.
10. Do not modify "summarizer.py".
11. Do not break existing EmailManager desktop/web functionality.
12. Keep the Android application responsible for scheduling, notifications, reminders, and alarms.
13. Use proper separation between UI, ViewModels, repositories, data sources, and domain models.
14. Use Room for appropriate local persistence.
15. Handle network failures gracefully.
16. Avoid unnecessary dependencies.
17. Keep the architecture extensible for future WhatsApp, documents, voice, and other information sources.

Use Mermaid when documenting workflows or architecture.

---

TESTING AND COMPLETION

Do not merely write code.

After implementation:

- Build the Android project.
- Fix compilation errors.
- Fix dependency/configuration problems.
- Verify navigation.
- Verify Room/database integration.
- Verify Google authentication architecture.
- Verify Gmail integration code.
- Verify backend request/response models.
- Verify Calendar integration architecture.
- Run available tests.
- Ensure GitHub Actions remains compatible.
- Review for obvious security problems.
- Review for broken existing functionality.

Do not stop because one phase is complete.

Continue through the entire milestone and leave the repository in a buildable, coherent, integrated state.

At the end, provide a concise summary of:

1. What was implemented
2. Files/components changed
3. What was tested
4. Any remaining limitations
5. Exactly what I need to configure manually, such as Google Cloud OAuth credentials/API enablement

The objective is not to create several disconnected features.

The objective is to turn the existing application into a coherent Executive AI personal assistant foundation where information from multiple Google accounts flows into intelligence, then into user-approved actions and eventually execution.
