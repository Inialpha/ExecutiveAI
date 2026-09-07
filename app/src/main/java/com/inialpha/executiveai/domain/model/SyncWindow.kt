package com.inialpha.executiveai.domain.model

/** A user-selectable "how far back to sync" duration — e.g. 24 [HOURS], 7 [DAYS]. */
enum class SyncWindowUnit {
    HOURS,
    DAYS,
}

/**
 * [amount] of [unit] — e.g. SyncWindow(24, HOURS) or SyncWindow(7, DAYS). Deliberately just two
 * plain fields (not a fixed enum of presets) so more units (e.g. MINUTES, WEEKS) can be added
 * later without changing this model — see [com.inialpha.executiveai.data.settings.SyncSettingsRepository].
 */
data class SyncWindow(val amount: Int, val unit: SyncWindowUnit) {
    fun toMillis(): Long = when (unit) {
        SyncWindowUnit.HOURS -> amount * 60L * 60L * 1000L
        SyncWindowUnit.DAYS -> amount * 24L * 60L * 60L * 1000L
    }

    fun label(): String {
        val unitLabel = when (unit) {
            SyncWindowUnit.HOURS -> if (amount == 1) "hour" else "hours"
            SyncWindowUnit.DAYS -> if (amount == 1) "day" else "days"
        }
        return "Last $amount $unitLabel"
    }

    companion object {
        val DEFAULT = SyncWindow(24, SyncWindowUnit.HOURS)

        /** The preset options surfaced in the picker UI — see REQUIREMENTS change request section 4. */
        val PRESETS = listOf(
            SyncWindow(1, SyncWindowUnit.HOURS),
            SyncWindow(2, SyncWindowUnit.HOURS),
            SyncWindow(6, SyncWindowUnit.HOURS),
            SyncWindow(8, SyncWindowUnit.HOURS),
            SyncWindow(12, SyncWindowUnit.HOURS),
            SyncWindow(24, SyncWindowUnit.HOURS),
            SyncWindow(2, SyncWindowUnit.DAYS),
            SyncWindow(3, SyncWindowUnit.DAYS),
            SyncWindow(7, SyncWindowUnit.DAYS),
        )
    }
}
