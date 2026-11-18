package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceNoticeModelTest {
    @Test
    void readyZoneIsGreenAndFull() {
        ResourceNoticeModel model = ResourceNoticeModel.of("READY", 100, 0, false);
        assertEquals(ResourceNoticeModel.Kind.READY, model.kind());
        assertSame(ResourceNoticeModel.READY, model.ramp());
        assertEquals("ottoextra.resources.notice.ready", model.statusKey());
        assertEquals(1f, model.fraction());
        assertFalse(model.showsTime());
        assertEquals(ResourceNoticeModel.Kind.READY, ResourceNoticeModel.of("REGENERATING", 100, 30, false).kind());
    }

    @Test
    void recoveringIsGoldUntilTenMinutesThenRust() {
        ResourceNoticeModel later = ResourceNoticeModel.of("REGENERATING", 70, 72 * 60, false);
        assertEquals(ResourceNoticeModel.Kind.RECOVERING, later.kind());
        assertSame(ResourceNoticeModel.WAIT, later.ramp());
        assertEquals(0.7f, later.fraction(), 1e-6);
        assertTrue(later.showsTime());
        ResourceNoticeModel soon = ResourceNoticeModel.of("REGENERATING", 95, 10 * 60, false);
        assertEquals(ResourceNoticeModel.Kind.SOON, soon.kind());
        assertSame(ResourceNoticeModel.SOON, soon.ramp());
        assertEquals(ResourceNoticeModel.Kind.RECOVERING,
                ResourceNoticeModel.of("REGENERATING", 90, 10 * 60 + 1, false).kind());
    }

    @Test
    void minePastItsStageCanBeMinedAndIsGreen() {
        ResourceNoticeModel model = ResourceNoticeModel.of("REGENERATING", 70, 5 * 60, true);
        assertEquals(ResourceNoticeModel.Kind.HARVESTABLE, model.kind());
        assertSame(ResourceNoticeModel.READY, model.ramp());
        assertTrue(model.showsTime());
    }

    @Test
    void eventsUseTheirOwnLabelAndTheRustRamp() {
        ResourceNoticeModel started = ResourceNoticeModel.event(ResourceNoticeModel.Kind.STARTED, 0, 4 * 3600);
        assertEquals("ottoextra.resources.notice.started", started.statusKey());
        assertSame(ResourceNoticeModel.SOON, started.ramp());
        assertEquals(0f, started.fraction());
        assertEquals("4:00 h", started.clock());
        assertSame(ResourceNoticeModel.READY,
                ResourceNoticeModel.event(ResourceNoticeModel.Kind.RESET, 100, 0).ramp());
        assertFalse(ResourceNoticeModel.event(ResourceNoticeModel.Kind.RESET, 100, 0).showsTime());
        assertEquals("ottoextra.resources.notice.pending_outside",
                ResourceNoticeModel.event(ResourceNoticeModel.Kind.PENDING_OUTSIDE, 0, 60).statusKey());
    }

    @Test
    void timeIsRoundedUpToMinutesAndShownLikeTheSketches() {
        assertEquals("1:12 h", ResourceNoticeModel.of("REGENERATING", 70, 72 * 60, false).clock());
        assertEquals("8 min", ResourceNoticeModel.of("REGENERATING", 90, 7 * 60 + 1, false).clock());
        assertEquals("1 min", ResourceNoticeModel.of("REGENERATING", 99, 5, false).clock());
        assertEquals("1:05 h", ResourceNoticeModel.of("REGENERATING", 50, 65 * 60, false).clock());
        assertEquals(0, ResourceNoticeModel.of("READY", 100, 99, false).minutesUntilFull());
    }

    @Test
    void percentIsClamped() {
        assertEquals(0, ResourceNoticeModel.of("REGENERATING", -5, 60, false).percent());
        assertEquals(100, ResourceNoticeModel.event(ResourceNoticeModel.Kind.STARTED, 140, 60).percent());
    }

    @Test
    void noticeMovesOffTheHudPanel() {
        assertArrayEquals(new int[] {100, 6}, ResourceNotice.avoid(100, 6, 196, 30, null, 270));
        assertArrayEquals(new int[] {100, 6}, ResourceNotice.avoid(100, 6, 196, 30, new int[] {320, 60, 150, 60}, 270));
        assertArrayEquals(new int[] {100, 72}, ResourceNotice.avoid(100, 6, 196, 30, new int[] {250, 4, 150, 66}, 270));
        assertArrayEquals(new int[] {100, 198}, ResourceNotice.avoid(100, 250, 196, 30, new int[] {250, 230, 150, 30}, 270));
    }
}
