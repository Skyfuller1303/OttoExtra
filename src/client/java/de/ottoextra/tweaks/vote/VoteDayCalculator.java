package de.ottoextra.tweaks.vote;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class VoteDayCalculator {

    private VoteDayCalculator() {
    }

    public static LocalDate voteDay(Instant instant, ZoneId zoneId, int resetHour) {
        int safeResetHour = Math.clamp(resetHour, 0, 23);
        ZonedDateTime localTime = instant.atZone(zoneId);
        LocalDate date = localTime.toLocalDate();
        if (localTime.toLocalTime().isBefore(LocalTime.of(safeResetHour, 0))) {
            return date.minusDays(1);
        }
        return date;
    }
}
