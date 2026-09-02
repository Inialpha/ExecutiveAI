package com.inialpha.executiveai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inialpha.executiveai.ui.theme.Card
import com.inialpha.executiveai.ui.theme.Cyan
import com.inialpha.executiveai.ui.theme.Navy
import com.inialpha.executiveai.ui.theme.Teal
import com.inialpha.executiveai.ui.theme.TextSecondary

@Composable
fun ExecutiveAIApp() {
    var selected by remember { mutableIntStateOf(0) }
    val destinations = listOf("Today", "Tasks", "Calendar", "Email", "Settings")
    val icons = listOf(Icons.Outlined.CheckCircle, Icons.Outlined.TaskAlt, Icons.Outlined.CalendarMonth, Icons.Outlined.Email, Icons.Outlined.Tune)

    Scaffold(
        containerColor = Navy,
        bottomBar = {
            NavigationBar(containerColor = Card) {
                destinations.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Dashboard(modifier = Modifier.padding(padding))
    }
}

@Composable
private fun Dashboard(modifier: Modifier = Modifier) {
    val priorities = listOf(
        "Review project proposal" to "Due today · 4:00 PM",
        "Team meeting" to "Tomorrow · 10:00 AM",
        "Submit application" to "Friday · 12:00 PM"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Navy).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good evening", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Executive Command Center", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Cyan)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Teal)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Your day at a glance", color = Navy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("3 priorities · 1 meeting · 2 unread important emails", color = Navy)
                }
            }
        }
        item { SectionTitle("Today") }
        items(priorities) { (title, detail) ->
            PriorityCard(title, detail)
        }
        item { SectionTitle("Needs your attention") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp))
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("2 important emails", fontWeight = FontWeight.SemiBold)
                        Text("Executive AI found actions requiring review", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun PriorityCard(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(Cyan, RoundedCornerShape(50)))
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(detail, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
