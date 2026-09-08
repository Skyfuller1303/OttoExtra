package de.ottoextra.tweaks.vote;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoteDayCalculatorTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void timeBeforeResetBelongsToPreviousVoteDay() {
        LocalDate voteDay = VoteDayCalculator.voteDay(
                Instant.parse("2026-07-28T01:59:59Z"),
                BERLIN,
                4
        );

        assertEquals(LocalDate.of(2026, 7, 27), voteDay);
    }

    @Test
    void resetStartsNewVoteDayAtFourBerlinTime() {
        LocalDate voteDay = VoteDayCalculator.voteDay(
                Instant.parse("2026-07-28T02:00:00Z"),
                BERLIN,
                4
        );

        assertEquals(LocalDate.of(2026, 7, 28), voteDay);
    }

    @Test
    void berlinWinterOffsetIsHandled() {
        LocalDate voteDay = VoteDayCalculator.voteDay(
                Instant.parse("2026-12-15T03:00:00Z"),
                BERLIN,
                4
        );

        assertEquals(LocalDate.of(2026, 12, 15), voteDay);
    }
}
