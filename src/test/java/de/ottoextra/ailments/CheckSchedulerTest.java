package de.ottoextra.ailments;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckSchedulerTest {

    private static CheckScheduler.Decision decide(boolean manual, boolean auto, long now, long next,
                                                  String guard, boolean item) {
        return CheckScheduler.decide(manual, auto, now, next, () -> guard, () -> item);
    }

    @Test
    void idleStatesAreNotLogged() {
        assertEquals(CheckScheduler.Kind.DISABLED, decide(false, false, 100, 50, null, true).kind());
        assertEquals(CheckScheduler.Kind.UNSCHEDULED, decide(false, true, 100, 0, null, true).kind());
        var wait = decide(false, true, 100, 500, null, true);
        assertEquals(CheckScheduler.Kind.WAIT, wait.kind());
        assertFalse(wait.loggable());
    }

    @Test
    void dueCheckReportsBlockReasonOrMissingItem() {
        var blocked = decide(false, true, 1_000, 500, "moving", true);
        assertEquals(CheckScheduler.Kind.BLOCKED, blocked.kind());
        assertEquals("moving", blocked.reason());
        assertTrue(blocked.loggable());
        var none = decide(false, true, 1_000, 500, null, false);
        assertEquals(CheckScheduler.Kind.NO_MENU_ITEM, none.kind());
        assertTrue(none.loggable());
        assertEquals(CheckScheduler.Kind.GO, decide(false, true, 1_000, 500, null, true).kind());
        assertEquals(CheckScheduler.Kind.GO, decide(false, true, 500, 500, null, true).kind(), "exactly due");
    }

    @Test
    void manualIgnoresSettingAndIntervalButNotGuards() {
        assertEquals(CheckScheduler.Kind.GO, decide(true, false, 100, 0, null, true).kind());
        assertEquals(CheckScheduler.Kind.GO, decide(true, true, 100, 99_999, null, true).kind());
        var blocked = decide(true, false, 100, 0, "combat boss bar", true);
        assertEquals(CheckScheduler.Kind.BLOCKED, blocked.kind());
        assertEquals("combat boss bar", blocked.reason());
    }

    @Test
    void guardIsOnlyAskedWhenDue() {
        boolean[] asked = {false};
        CheckScheduler.decide(false, true, 100, 500, () -> {
            asked[0] = true;
            return null;
        }, () -> true);
        assertFalse(asked[0]);
    }

    @Test
    void menuItemFromTheRealServerStack() {
        // name {"text":"","extra":[{"text":"Menü","color":"green"}]} reads as "Menü"; lore "Öffne dein Menü."
        assertTrue(MenuReader.isMenuItem("minecraft:enchanted_book", "Menü", List.of("Öffne dein Menü.")));
        assertTrue(MenuReader.isMenuItem("minecraft:enchanted_book", "§aMenü", List.of("§8Öffne dein Menü.")));
        assertTrue(MenuReader.isMenuItem("minecraft:enchanted_book", "Anderer Name", List.of("§8Öffne dein Menü.")));
        assertFalse(MenuReader.isMenuItem("minecraft:enchanted_book", "Zauberbuch", List.of("Ein Buch")));
        assertFalse(MenuReader.isMenuItem("minecraft:book", "Menü", List.of("Öffne dein Menü.")));
    }
}
