package de.ottoextra.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceProposalsTest {
    private static final long T0 = 1_000_000L;
    private static final String DIM = "minecraft:overworld";

    private static ResourceProposals.Proposal make(ResourceProposals proposals, String name, long now) {
        return proposals.create("IRON_MINE", name, DIM, 10, 64, -20, now);
    }

    @Test
    void createStartsAtDefaultRadiusThreeAtThePosition() {
        ResourceProposals proposals = new ResourceProposals();
        assertNull(proposals.current(T0));
        ResourceProposals.Proposal proposal = make(proposals, "Nordmine", T0);
        assertEquals(3, proposal.radius());
        assertEquals(ResourceZone.point(10, 64, -20, 3), proposal.zone());
        assertEquals(proposal, proposals.current(T0 + 1));
    }

    @Test
    void radiusChangesAreClampedToOneAndSixtyFour() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        assertEquals(4, proposals.adjustRadius(4, T0 + 1).radius());
        assertEquals(1, proposals.adjustRadius(0, T0 + 2).radius());
        assertEquals(1, proposals.adjustRadius(-7, T0 + 3).radius());
        assertEquals(64, proposals.adjustRadius(65, T0 + 4).radius());
        assertEquals(64, proposals.adjustRadius(1000, T0 + 5).radius());
        assertNull(new ResourceProposals().adjustRadius(5, T0));
    }

    @Test
    void confirmReturnsTheProposalOnceAndClearsIt() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        proposals.adjustRadius(9, T0 + 10);
        ResourceProposals.Proposal confirmed = proposals.confirm(T0 + 20);
        assertNotNull(confirmed);
        assertEquals(9, confirmed.radius());
        assertNull(proposals.confirm(T0 + 21));
        assertNull(proposals.current(T0 + 21));
    }

    @Test
    void discardClearsAndReportsWhetherThereWasOne() {
        ResourceProposals proposals = new ResourceProposals();
        assertFalse(proposals.discard());
        make(proposals, "A", T0);
        assertTrue(proposals.discard());
        assertNull(proposals.current(T0));
        assertFalse(proposals.discard());
    }

    @Test
    void proposalExpiresAfterOneHundredTwentySecondsAndSaysSoOnce() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        assertNotNull(proposals.current(T0 + ResourceProposals.LIFETIME_MILLIS));
        assertFalse(proposals.expireIfDue(T0 + ResourceProposals.LIFETIME_MILLIS));
        assertNull(proposals.current(T0 + ResourceProposals.LIFETIME_MILLIS + 1));
        assertNull(proposals.confirm(T0 + ResourceProposals.LIFETIME_MILLIS + 1));
        make(proposals, "B", T0);
        assertTrue(proposals.expireIfDue(T0 + ResourceProposals.LIFETIME_MILLIS + 1));
        assertFalse(proposals.expireIfDue(T0 + ResourceProposals.LIFETIME_MILLIS + 2));
    }

    @Test
    void radiusChangeRestartsTheLifetime() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        proposals.adjustRadius(5, T0 + 100_000L);
        assertNotNull(proposals.current(T0 + 200_000L));
        assertNull(proposals.current(T0 + 100_000L + ResourceProposals.LIFETIME_MILLIS + 1));
    }

    @Test
    void newCreateReplacesThePendingOne() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "Alt", T0);
        proposals.adjustRadius(20, T0 + 1);
        ResourceProposals.Proposal fresh = make(proposals, "Neu", T0 + 2);
        assertEquals("Neu", proposals.current(T0 + 3).name());
        assertEquals(3, fresh.radius());
        assertEquals("Neu", proposals.confirm(T0 + 4).name());
    }

    @Test
    void editingPausesExpiryAndCancelKeepsTheProposalPending() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        assertNotNull(proposals.beginEdit(T0 + 10));
        long late = T0 + 10 * ResourceProposals.LIFETIME_MILLIS;
        assertNotNull(proposals.current(late));
        assertFalse(proposals.expireIfDue(late));
        proposals.endEdit(false, late);
        assertNotNull(proposals.current(late + 1));
        assertNull(proposals.current(late + ResourceProposals.LIFETIME_MILLIS + 1));
    }

    @Test
    void savingFromTheEditorCompletesTheProposal() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        proposals.beginEdit(T0 + 1);
        proposals.endEdit(true, T0 + 2);
        assertNull(proposals.current(T0 + 3));
        assertNull(new ResourceProposals().beginEdit(T0));
    }

    @Test
    void createDuringAnEditStartsFreshAndNotEditing() {
        ResourceProposals proposals = new ResourceProposals();
        make(proposals, "A", T0);
        proposals.beginEdit(T0 + 1);
        make(proposals, "B", T0 + 2);
        assertNull(proposals.current(T0 + 2 + ResourceProposals.LIFETIME_MILLIS + 1));
    }

    @Test
    void zoneColoursAreDistinctPerKindAndAlphaIsReplaced() {
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (String base : ResourcePolicy.BASES) {
            colors.add(ResourceZoneColors.typeColor(ResourcePolicy.compose(base, true)));
        }
        assertEquals(ResourcePolicy.BASES.size(), colors.size());
        assertEquals(ResourceZoneColors.typeColor("IRON_MINE"), ResourceZoneColors.typeColor("IRON_DEPOSIT"));
        assertEquals(0x38D6A27A, ResourceZoneColors.withAlpha(0xFFD6A27A, 0x38));
    }
}
