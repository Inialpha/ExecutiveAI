package com.inialpha.executiveai.di

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.room.Room
import com.inialpha.executiveai.data.auth.GoogleAuthManager
import com.inialpha.executiveai.data.local.AppDatabase
import com.inialpha.executiveai.data.repository.AccountRepository
import com.inialpha.executiveai.data.repository.CalendarRepository
import com.inialpha.executiveai.data.repository.EmailRepository
import com.inialpha.executiveai.data.repository.ExecutiveItemRepository
import com.inialpha.executiveai.data.repository.InsightRepository
import com.inialpha.executiveai.voice.SpeechRecognizerManager

/**
 * Simple, dependency-free service locator. Kept intentionally manual (no Hilt/Dagger) per
 * REQUIREMENTS.md's "avoid unnecessary dependencies" / "avoid unnecessary architectural
 * complexity" guidance — the object graph here is small and static enough not to need a DI
 * framework's code generation.
 *
 * [googleAuthManager] is the one Activity-scoped exception: it must be created in
 * MainActivity.onCreate (before STARTED) because it registers an ActivityResultLauncher, so it's
 * attached via [attachActivity] rather than being available from construction.
 */
class AppContainer(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    ).build()

    val accountRepository = AccountRepository(database.accountDao())
    val emailRepository = EmailRepository(database.emailDao())
    val insightRepository = InsightRepository(database.emailDao(), database.insightDao(), database.executiveItemDao())
    val calendarRepository = CalendarRepository(database.calendarEventDao())
    val executiveItemRepository = ExecutiveItemRepository(database.executiveItemDao())

    val speechRecognizerManager = SpeechRecognizerManager(context.applicationContext)

    var googleAuthManager: GoogleAuthManager? = null
        private set

    fun attachActivity(activity: ComponentActivity) {
        googleAuthManager = GoogleAuthManager(activity)
    }

    fun detachActivity() {
        googleAuthManager = null
    }
}
