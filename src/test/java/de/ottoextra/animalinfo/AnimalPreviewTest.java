package de.ottoextra.animalinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimalPreviewTest {
    private static final float YAW = AnimalPreview.VIEW_YAW - 180.0f;
    private static final float PITCH = AnimalPreview.VIEW_PITCH;

    @Test
    void horseFillsTheBoxWithPadding() {
        // Adult horse: 1.3965 x 1.6 blocks in a 24 x 24 box.
        AnimalPreview.Fit fit = AnimalPreview.fit(1.3965f, 1.6f, YAW, PITCH, 24, 24, 2);
        assertTrue(fit.width() <= 20.0001f && fit.height() <= 20.0001f);
        // One side touches the inner box.
        assertEquals(20.0f, Math.max(fit.width(), fit.height()), 0.001f);
        assertTrue(fit.scale() > 8.0f, "much larger than the old fixed scale of 8 px per block");
    }

    @Test
    void babiesAreScaledUpToTheSameSize() {
        AnimalPreview.Fit adult = AnimalPreview.fit(1.3965f, 1.6f, YAW, PITCH, 24, 24, 2);
        AnimalPreview.Fit baby = AnimalPreview.fit(1.3965f * 0.5f, 1.6f * 0.5f, YAW, PITCH, 24, 24, 2);
        assertEquals(adult.scale() * 2.0f, baby.scale(), 0.01f);
        assertEquals(adult.width(), baby.width(), 0.01f);
        assertEquals(adult.height(), baby.height(), 0.01f);
    }

    @Test
    void tallAndFlatAnimalsAreLimitedByTheirLargerSide() {
        // Chicken (0.4 x 0.7) is limited by its height, a wide box by the width.
        AnimalPreview.Fit chicken = AnimalPreview.fit(0.4f, 0.7f, YAW, PITCH, 24, 24, 2);
        assertEquals(20.0f, chicken.height(), 0.001f);
        AnimalPreview.Fit wide = AnimalPreview.fit(1.3965f, 1.6f, YAW, PITCH, 16, 40, 1);
        assertEquals(14.0f, wide.width(), 0.001f);
        assertTrue(wide.height() <= 38.0f);
    }

    @Test
    void facingTheViewerIsNarrowerThanTheThreeQuarterView() {
        AnimalPreview.Fit front = AnimalPreview.fit(0.9f, 1.4f, 0.0f, 0.0f, 100, 100, 0);
        AnimalPreview.Fit turned = AnimalPreview.fit(0.9f, 1.4f, YAW, 0.0f, 100, 100, 0);
        assertTrue(turned.width() / turned.scale() > front.width() / front.scale());
        assertEquals(0.9f, front.width() / front.scale(), 0.001f);
    }

    @Test
    void degenerateInputStaysUsable() {
        AnimalPreview.Fit fit = AnimalPreview.fit(0.0f, -1.0f, YAW, PITCH, 0, 0, 2);
        assertTrue(fit.scale() >= 1.0f);
        assertFalse(Float.isNaN(fit.width()) || Float.isNaN(fit.height()));
    }
}
