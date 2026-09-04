package com.inialpha.executiveai.ui.navigation

/** Every top-level destination in the app, per REQUIREMENTS.md section 14. */
sealed class ExecutiveDestination(val route: String) {
    object Onboarding : ExecutiveDestination("onboarding")
    object Dashboard : ExecutiveDestination("dashboard")
    object ImportantEmails : ExecutiveDestination("important_emails")
    object EmailInsight : ExecutiveDestination("email_insight/{emailId}") {
        fun createRoute(emailId: String) = "email_insight/$emailId"
        const val ARG_EMAIL_ID = "emailId"
    }
    object Upcoming : ExecutiveDestination("upcoming")
    object Tasks : ExecutiveDestination("tasks")
    object Reminders : ExecutiveDestination("reminders")
    object Calendar : ExecutiveDestination("calendar")
    object Assistant : ExecutiveDestination("assistant")
    object Accounts : ExecutiveDestination("accounts")
    object Settings : ExecutiveDestination("settings")
}

/** The five destinations surfaced directly in the bottom navigation bar. */
val bottomNavDestinations = listOf(
    ExecutiveDestination.Dashboard,
    ExecutiveDestination.Upcoming,
    ExecutiveDestination.Tasks,
    ExecutiveDestination.Calendar,
    ExecutiveDestination.Accounts,
)
