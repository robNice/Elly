package de.robnice.elly.ui.elly

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EllySeasonTest {

    @Test
    fun easterSunday_matchesKnownDates() {
        val known = mapOf(
            2026 to LocalDate.of(2026, 4, 5),
            2027 to LocalDate.of(2027, 3, 28),
            2028 to LocalDate.of(2028, 4, 16),
            2029 to LocalDate.of(2029, 4, 1),
            2030 to LocalDate.of(2030, 4, 21),
            2031 to LocalDate.of(2031, 4, 13),
            2032 to LocalDate.of(2032, 3, 28),
            2033 to LocalDate.of(2033, 4, 17),
            2034 to LocalDate.of(2034, 4, 9),
            2035 to LocalDate.of(2035, 3, 25),
            2036 to LocalDate.of(2036, 4, 13)
        )
        known.forEach { (year, date) -> assertEquals(date, EllySeason.easterSunday(year)) }
    }

    @Test
    fun christmas_runsFrom24DecemberTo6January() {
        assertEquals(EllySeason.DEFAULT, EllySeason.of(LocalDate.of(2026, 12, 23)))
        assertEquals(EllySeason.CHRISTMAS, EllySeason.of(LocalDate.of(2026, 12, 24)))
        assertEquals(EllySeason.CHRISTMAS, EllySeason.of(LocalDate.of(2027, 1, 6)))
        assertEquals(EllySeason.DEFAULT, EllySeason.of(LocalDate.of(2027, 1, 7)))
    }

    @Test
    fun easter_runsFromPalmSundayToTheSundayAfterEaster() {
        assertEquals(EllySeason.DEFAULT, EllySeason.of(LocalDate.of(2027, 3, 20)))
        assertEquals(EllySeason.EASTER, EllySeason.of(LocalDate.of(2027, 3, 21)))
        assertEquals(EllySeason.EASTER, EllySeason.of(LocalDate.of(2027, 4, 4)))
        assertEquals(EllySeason.DEFAULT, EllySeason.of(LocalDate.of(2027, 4, 5)))
    }

    @Test
    fun halloween_runsFrom24To31October() {
        assertEquals(EllySeason.DEFAULT, EllySeason.of(LocalDate.of(2026, 10, 23)))
        assertEquals(EllySeason.HALLOWEEN, EllySeason.of(LocalDate.of(2026, 10, 24)))
        assertEquals(EllySeason.HALLOWEEN, EllySeason.of(LocalDate.of(2026, 10, 31)))
        assertEquals(EllySeason.DEFAULT, EllySeason.of(LocalDate.of(2026, 11, 1)))
    }
}
