package de.robnice.elly.ui.elly

import androidx.annotation.DrawableRes
import de.robnice.elly.R
import java.time.LocalDate
import java.time.Month

enum class EllySeason(@DrawableRes val image: Int) {
    DEFAULT(R.drawable.elly),
    CHRISTMAS(R.drawable.elly_costume_santa),
    EASTER(R.drawable.elly_costume_bunny),
    HALLOWEEN(R.drawable.elly_costume_zombie);

    companion object {
        fun of(date: LocalDate): EllySeason {
            val easter = easterSunday(date.year)
            return when {
                date.month == Month.DECEMBER && date.dayOfMonth >= 24 -> CHRISTMAS
                date.month == Month.JANUARY && date.dayOfMonth <= 6 -> CHRISTMAS
                !date.isBefore(easter.minusDays(7)) && !date.isAfter(easter.plusDays(7)) -> EASTER
                date.month == Month.OCTOBER && date.dayOfMonth >= 24 -> HALLOWEEN
                else -> DEFAULT
            }
        }

        // Anonymous Gregorian algorithm (Meeus/Jones/Butcher).
        fun easterSunday(year: Int): LocalDate {
            val a = year % 19
            val b = year / 100
            val c = year % 100
            val d = b / 4
            val e = b % 4
            val f = (b + 8) / 25
            val g = (b - f + 1) / 3
            val h = (19 * a + b - d - g + 15) % 30
            val i = c / 4
            val k = c % 4
            val l = (32 + 2 * e + 2 * i - h - k) % 7
            val m = (a + 11 * h + 22 * l) / 451
            val month = (h + l - 7 * m + 114) / 31
            val day = (h + l - 7 * m + 114) % 31 + 1
            return LocalDate.of(year, month, day)
        }
    }
}
