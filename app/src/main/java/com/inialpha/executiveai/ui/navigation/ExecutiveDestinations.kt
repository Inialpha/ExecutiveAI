package com.inialpha.executiveai.ui.navigation

/** Every top-level destination in the app. */
sealed class ExecutiveDestination(val route: String) {
    object Onboarding : ExecutiveDestination("onboarding")

    /** Displayed as "Home" in the bottom nav — kept as "Dashboard" internally (route/screen/ViewModel
     * names unchanged) since this is a label-only rename; see ui/ExecutiveAIApp.kt's labelFor(). */
    object Dashboard : ExecutiveDestination("dashboard")

    /** Emails processed & summarized by the AI, separated into per-account tabs. */
    object Emails : ExecutiveDestination("emails")

    object EmailInsight : ExecutiveDestination("email_insight/{emailId}") {
        fun createRoute(emailId: String) = "email_insight/$emailId"
        const val ARG_EMAIL_ID = "emailId"
    }

    /** The complete event area: calendar view, upcoming, add/edit/delete, AI-generated events, sync. */
    object Calendar : ExecutiveDestination("calendar")

    object Tasks : ExecutiveDestination("tasks")
    object Reminders : ExecutiveDestination("reminders")
    object Assistant : ExecutiveDestination("assistant")
    object Accounts : ExecutiveDestination("accounts")
    object Settings : ExecutiveDestination("settings")
}

/** Primary bottom navigation: Home | Emails | Calendar | Tasks. Accounts is reached via the Menu. */
val bottomNavDestinations = listOf(
    ExecutiveDestination.Dashboard,
    ExecutiveDestination.Emails,
    ExecutiveDestination.Calendar,
    ExecutiveDestination.Tasks,
)
