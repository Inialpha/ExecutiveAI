package com.inialpha.executiveai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.inialpha.executiveai.data.local.dao.AccountDao
import com.inialpha.executiveai.data.local.dao.CalendarEventDao
import com.inialpha.executiveai.data.local.dao.EmailDao
import com.inialpha.executiveai.data.local.dao.ExecutiveItemDao
import com.inialpha.executiveai.data.local.dao.InsightDao
import com.inialpha.executiveai.data.local.entity.AccountEntity
import com.inialpha.executiveai.data.local.entity.CalendarEventEntity
import com.inialpha.executiveai.data.local.entity.EmailEntity
import com.inialpha.executiveai.data.local.entity.ExecutiveItemEntity
import com.inialpha.executiveai.data.local.entity.InsightEntity

@Database(
    entities = [
        AccountEntity::class,
        EmailEntity::class,
        InsightEntity::class,
        ExecutiveItemEntity::class,
        CalendarEventEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun emailDao(): EmailDao
    abstract fun insightDao(): InsightDao
    abstract fun executiveItemDao(): ExecutiveItemDao
    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        const val DATABASE_NAME = "executive_ai.db"
    }
}
