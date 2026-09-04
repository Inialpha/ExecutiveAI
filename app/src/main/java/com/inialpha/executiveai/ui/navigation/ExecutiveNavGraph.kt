package com.inialpha.executiveai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.inialpha.executiveai.ui.screens.accounts.AccountsScreen
import com.inialpha.executiveai.ui.screens.assistant.AssistantScreen
import com.inialpha.executiveai.ui.screens.calendar.CalendarScreen
import com.inialpha.executiveai.ui.screens.dashboard.DashboardScreen
import com.inialpha.executiveai.ui.screens.emailinsight.EmailInsightScreen
import com.inialpha.executiveai.ui.screens.emails.ImportantEmailsScreen
import com.inialpha.executiveai.ui.screens.onboarding.OnboardingScreen
import com.inialpha.executiveai.ui.screens.reminders.RemindersScreen
import com.inialpha.executiveai.ui.screens.settings.SettingsScreen
import com.inialpha.executiveai.ui.screens.tasks.TasksScreen
import com.inialpha.executiveai.ui.screens.upcoming.UpcomingScreen

@Composable
fun ExecutiveNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(ExecutiveDestination.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(ExecutiveDestination.Dashboard.route) {
                        popUpTo(ExecutiveDestination.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(ExecutiveDestination.Dashboard.route) {
            DashboardScreen(
                onOpenImportantEmails = { navController.navigate(ExecutiveDestination.ImportantEmails.route) },
                onOpenEmail = { emailId -> navController.navigate(ExecutiveDestination.EmailInsight.createRoute(emailId)) },
                onOpenUpcoming = { navController.navigate(ExecutiveDestination.Upcoming.route) },
                onOpenAssistant = { navController.navigate(ExecutiveDestination.Assistant.route) },
                onOpenAccounts = { navController.navigate(ExecutiveDestination.Accounts.route) },
            )
        }
        composable(ExecutiveDestination.ImportantEmails.route) {
            ImportantEmailsScreen(
                onOpenEmail = { emailId -> navController.navigate(ExecutiveDestination.EmailInsight.createRoute(emailId)) },
            )
        }
        composable(
            route = ExecutiveDestination.EmailInsight.route,
            arguments = listOf(navArgument(ExecutiveDestination.EmailInsight.ARG_EMAIL_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val emailId = backStackEntry.arguments?.getString(ExecutiveDestination.EmailInsight.ARG_EMAIL_ID).orEmpty()
            EmailInsightScreen(emailId = emailId)
        }
        composable(ExecutiveDestination.Upcoming.route) { UpcomingScreen() }
        composable(ExecutiveDestination.Tasks.route) { TasksScreen() }
        composable(ExecutiveDestination.Reminders.route) { RemindersScreen() }
        composable(ExecutiveDestination.Calendar.route) { CalendarScreen() }
        composable(ExecutiveDestination.Assistant.route) { AssistantScreen() }
        composable(ExecutiveDestination.Accounts.route) { AccountsScreen() }
        composable(ExecutiveDestination.Settings.route) {
            SettingsScreen(
                onOpenAccounts = { navController.navigate(ExecutiveDestination.Accounts.route) },
                onOpenReminders = { navController.navigate(ExecutiveDestination.Reminders.route) },
            )
        }
    }
}
