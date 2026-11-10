package com.example.util

import com.example.data.database.AppSettingsEntity
import java.time.LocalDate

/**
 * Single source of truth for the dates the app allows the user to browse.
 *
 * The dashboard and the detailed day log both use these helpers so the two
 * calendars can never disagree about the start date or about future days.
 */
object DateBounds {

    fun startDate(settings: AppSettingsEntity?): LocalDate? {
        if (settings?.useStartDate != true) return null
        return runCatching { settings.startDate?.let(LocalDate::parse) }.getOrNull()
    }

    fun earliestAllowedDate(settings: AppSettingsEntity?): LocalDate? {
        if (settings?.blockBeforeStartDate != true) return null
        return startDate(settings)
    }

    fun latestAllowedDate(settings: AppSettingsEntity?, today: LocalDate = LocalDate.now()): LocalDate? {
        return if (settings?.blockAfterToday != false) today else null
    }

    fun clamp(date: LocalDate, settings: AppSettingsEntity?, today: LocalDate = LocalDate.now()): LocalDate {
        var result = date
        earliestAllowedDate(settings)?.let { earliest ->
            if (result < earliest) result = earliest
        }
        latestAllowedDate(settings, today)?.let { latest ->
            if (result > latest) result = latest
        }
        return result
    }

    fun canGoEarlier(date: LocalDate, settings: AppSettingsEntity?): Boolean {
        val earliest = earliestAllowedDate(settings) ?: return true
        return date > earliest
    }

    fun canGoLater(date: LocalDate, settings: AppSettingsEntity?, today: LocalDate = LocalDate.now()): Boolean {
        val latest = latestAllowedDate(settings, today) ?: return true
        return date < latest
    }

    /**
     * Whether new data may be added to [date].
     *
     * Today and past days are always allowed. Future days are only allowed
     * when "block adding after today" is switched off in the settings.
     */
    fun isAddAllowed(
        date: LocalDate,
        settings: AppSettingsEntity?,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        if (!date.isAfter(today)) return true
        return settings?.blockAfterToday == false
    }
}
