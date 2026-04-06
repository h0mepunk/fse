package com.example.fse.domain.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Грубая оценка фазы по датам начала месячных (календарный метод).
 * После [minPeriodsForPrediction] дат начала оценивается средняя длина цикла.
 */
object CyclePhaseCalculator {

    const val MIN_PERIODS_FOR_PREDICTION = 3

    enum class PhaseKind {
        MENSTRUATION,
        OVULATION,
        OTHER
    }

    data class DayPhase(
        val kind: PhaseKind,
        /** Текст для UI */
        val labelRu: String
    )

    /**
     * @param periodStarts уникальные даты первого дня кровотечения, по возрастанию
     */
    fun phaseForDate(date: LocalDate, periodStarts: List<LocalDate>): DayPhase? {
        val sorted = periodStarts.distinct().sorted()
        if (sorted.isEmpty()) return null
        if (sorted.size < MIN_PERIODS_FOR_PREDICTION) {
            return DayPhase(PhaseKind.OTHER, "Укажите ещё даты")
        }

        val intervals = sorted.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in 15..45 }
        if (intervals.isEmpty()) return DayPhase(PhaseKind.OTHER, "Нет данных")

        val avgCycle = intervals.average().roundToInt().coerceIn(21, 40)

        val lastStartOnOrBefore = sorted.lastOrNull { !it.isAfter(date) }
            ?: return null

        val dayInCycle = ChronoUnit.DAYS.between(lastStartOnOrBefore, date).toInt() + 1
        if (dayInCycle < 1) return DayPhase(PhaseKind.OTHER, "—")

        if (dayInCycle in 1..5) {
            return DayPhase(PhaseKind.MENSTRUATION, "Месячные")
        }

        val ovulationCenter = (avgCycle - 14).coerceIn(8, avgCycle - 8)
        if (dayInCycle in (ovulationCenter - 2)..(ovulationCenter + 2)) {
            return DayPhase(PhaseKind.OVULATION, "Овуляция")
        }

        return DayPhase(PhaseKind.OTHER, "—")
    }
}
