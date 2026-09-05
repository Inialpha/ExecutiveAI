package com.inialpha.executiveai.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inialpha.executiveai.domain.model.Account
import com.inialpha.executiveai.ui.components.LoadingState
import com.inialpha.executiveai.ui.navigation.ExecutiveDestination
import com.inialpha.executiveai.ui.navigation.ExecutiveNavGraph
import com.inialpha.executiveai.ui.navigation.bottomNavDestinations
import com.inialpha.executiveai.viewmodel.executiveAIContainer

/**
 * App root: decides Onboarding vs. Dashboard as the start destination based on whether any
 * Google account is already connected, then hosts the nav graph inside a bottom-nav Scaffold.
 *
 * Primary navigation is Home | Emails | Calendar | Menu. Connected Accounts and Reminders live
 * in the Menu (top bar overflow) rather than the bottom bar — see [ExecutiveTopBar].
 */
@Composable
fun ExecutiveAIApp() {
    val container = executiveAIContainer()

    // Explicit nullable local state (rather than collectAsStateWithLifecycle(initialValue = null),
    // which type-mismatches against the non-nullable Flow<List<Account>>) so we can distinguish
    // "still resolving" (null) from "resolved, zero accounts" (empty list).
    var accounts by remember { mutableStateOf<List<Account>?>(null) }
    LaunchedEffect(Unit) {
        container.accountRepository.observeAccounts().collect { accounts = it }
    }

    when (val currentAccounts = accounts) {
        null -> LoadingState() // still resolving whether any account is connected
        else -> {
            val startDestination = if (currentAccounts.isEmpty()) ExecutiveDestination.Onboarding.route else ExecutiveDestination.Dashboard.route
            ExecutiveAIScaffold(startDestination = startDestination)
        }
    }
}

@Composable
private fun ExecutiveAIScaffold(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            if (currentRoute != ExecutiveDestination.Onboarding.route) {
                ExecutiveTopBar(navController)
            }
        },
        bottomBar = {
            if (currentRoute != ExecutiveDestination.Onboarding.route && currentRoute != ExecutiveDestination.EmailInsight.route) {
                ExecutiveBottomBar(navController, currentRoute)
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            ExecutiveNavGraph(navController = navController, startDestination = startDestination)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ExecutiveTopBar(navController: NavHostController) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Executive AI") },
        actions = {
            IconButton(onClick = { navController.navigate(ExecutiveDestination.Assistant.route) }) {
                Icon(Icons.Filled.Mic, contentDescription = "AI Assistant")
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Connected accounts") },
                    leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    onClick = { menuExpanded = false; navController.navigate(ExecutiveDestination.Accounts.route) },
                )
                DropdownMenuItem(
                    text = { Text("Reminders") },
                    leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    onClick = { menuExpanded = false; navController.navigate(ExecutiveDestination.Reminders.route) },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = { menuExpanded = false; navController.navigate(ExecutiveDestination.Settings.route) },
                )
            }
        },
    )
}

@Composable
private fun ExecutiveBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        bottomNavDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(iconFor(destination), contentDescription = null) },
                label = { Text(labelFor(destination)) },
            )
        }
    }
}

private fun iconFor(destination: ExecutiveDestination) = when (destination) {
    ExecutiveDestination.Dashboard -> Icons.Filled.Home
    ExecutiveDestination.Emails -> Icons.Filled.Email
    ExecutiveDestination.Calendar -> Icons.Filled.CalendarMonth
    ExecutiveDestination.Tasks -> Icons.Filled.CheckCircle
    else -> Icons.Filled.Home
}

private fun labelFor(destination: ExecutiveDestination) = when (destination) {
    ExecutiveDestination.Dashboard -> "Home"
    ExecutiveDestination.Emails -> "Emails"
    ExecutiveDestination.Calendar -> "Calendar"
    ExecutiveDestination.Tasks -> "Tasks"
    else -> ""
}
