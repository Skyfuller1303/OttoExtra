package de.ottoextra.nametags;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NametagIdentityTest {
    @Test
    void sameLabelDoesNotGiveAnimalAPlayerIdentityAndReusedStatesAreCleared() {
        var player = new EntityRenderState();
        var animal = new EntityRenderState();
        player.displayName = animal.displayName = Text.literal("Alice");
        UUID uuid = UUID.randomUUID();
        NametagService.rememberIdentity(player, uuid, "Alice");
        assertEquals("Alice", NametagService.accountFor(player));
        assertEquals(uuid, NametagService.playerUuidFor(player));
        assertNull(NametagService.accountFor(animal));
        assertNull(NametagService.playerUuidFor(animal));
        NametagService.rememberPlayer(player, null);
        assertNull(NametagService.accountFor(player));
        assertNull(NametagService.playerUuidFor(player));
    }
}
