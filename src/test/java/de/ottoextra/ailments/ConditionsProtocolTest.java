package de.ottoextra.ailments;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionsProtocolTest {

    private static MenuReader.SlotView slot(int i, boolean container, String id, String name, String... lore) {
        return new MenuReader.SlotView(i, container, id, name, List.of(lore));
    }

    private static List<MenuReader.SlotView> menu() {
        List<MenuReader.SlotView> s = new ArrayList<>();
        s.add(slot(0, true, "minecraft:book", "§6Meine Fertigkeiten"));
        s.add(slot(1, true, "minecraft:book", "Meine Quests"));
        s.add(slot(5, true, "minecraft:clock", "Kalender"));
        s.add(slot(36, true, "minecraft:amethyst_shard", "§cVerdreckt", "", "Effekte:", "- Gestank", "",
                "Ich habe lange kein Wasser mehr gesehen."));
        s.add(slot(40, true, "minecraft:amethyst_shard", "Segen der Ahnen", "Effekte:", "- Stärke"));
        return s;
    }

    @Test
    void detectsMenuByButtons() {
        assertTrue(MenuReader.isMenu("minecraft:generic_9x6", "", menu(), false));
        assertFalse(MenuReader.isMenu("minecraft:generic_9x3", "", menu(), false));
        assertFalse(MenuReader.isMenu("minecraft:generic_9x6", "", List.of(slot(0, true, "minecraft:book",
                "Meine Quests")), false), "one button alone is not enough without a hint");
    }

    @Test
    void hintsLowerTheBarToOneButton() {
        List<MenuReader.SlotView> one = List.of(slot(3, true, "minecraft:clock", "Kalender"));
        assertTrue(MenuReader.isMenu("minecraft:generic_9x6", "ꈃ쇳", one, false));
        assertTrue(MenuReader.isMenu("minecraft:generic_9x6", "x", one, true));
        assertFalse(MenuReader.isMenu("minecraft:generic_9x6", "ꈃ쇳", List.of(), false));
    }

    @Test
    void playerInventorySlotsDoNotCountAsButtons() {
        List<MenuReader.SlotView> s = List.of(slot(0, false, "minecraft:book", "Kalender"),
                slot(1, false, "minecraft:book", "Mein Gefolge"));
        assertFalse(MenuReader.isMenu("minecraft:generic_9x6", "", s, false));
    }

    @Test
    void recognisesOpenMessageAndMenuItem() {
        assertTrue(MenuReader.isOpenMessage("§a[Info] §7Du öffnest dein Menü."));
        assertFalse(MenuReader.isOpenMessage("Du schließt dein Menü."));
        assertTrue(MenuReader.isMenuItem("minecraft:enchanted_book", "§eMenü", List.of()));
        assertTrue(MenuReader.isMenuItem("minecraft:enchanted_book", "?", List.of("§7Öffne dein Menü.")));
        assertFalse(MenuReader.isMenuItem("minecraft:book", "Menü", List.of()));
    }

    @Test
    void readsOnlyShardsInSlots36To53() {
        List<MenuReader.SlotView> s = new ArrayList<>(menu());
        s.add(slot(35, true, "minecraft:amethyst_shard", "Zu früh"));
        s.add(slot(54, true, "minecraft:amethyst_shard", "Zu spät"));
        s.add(slot(53, false, "minecraft:amethyst_shard", "Inventar"));
        s.add(slot(41, true, "minecraft:stone", "Kein Splitter"));
        s.add(slot(53, true, "minecraft:amethyst_shard", "Letzter"));
        List<Condition> out = MenuReader.readConditions(s);
        assertEquals(List.of("Verdreckt", "Segen der Ahnen", "Letzter"), out.stream().map(Condition::name).toList());
    }

    @Test
    void classifiesKindAndEffects() {
        List<Condition> out = MenuReader.readConditions(menu());
        assertEquals(new Condition("Verdreckt", List.of("Gestank"), "leiden"), out.get(0));
        assertEquals(new Condition("Segen der Ahnen", List.of("Stärke"), "status"), out.get(1));
        assertEquals("leiden", Condition.kindOf("Übermüdet"));
        assertEquals("status", Condition.kindOf("Zustand"));
    }

    @Test
    void enforcesProtocolLimits() {
        List<String> lore = new ArrayList<>(List.of("Effekte:"));
        for (int i = 0; i < 12; i++) lore.add("- E" + i);
        Condition c = Condition.fromItem("N".repeat(60), lore).orElseThrow();
        assertEquals(40, c.name().length());
        assertEquals(8, c.effects().size());
        assertTrue(Condition.fromItem("  ", List.of()).isEmpty());
        List<MenuReader.SlotView> many = new ArrayList<>();
        for (int i = 36; i <= 53; i++) many.add(slot(i, true, "minecraft:amethyst_shard", "C" + i));
        assertEquals(18, MenuReader.readConditions(many).size());
    }

    @Test
    void putBodyFollowsProtocol() {
        String json = ConditionWire.putBody(MenuReader.readConditions(menu()), 1234L);
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(1234L, o.get("observedAt").getAsLong());
        assertEquals(2, o.getAsJsonArray("conditions").size());
        JsonObject first = o.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals(3, first.size());
        assertEquals("Verdreckt", first.get("name").getAsString());
        assertEquals("Gestank", first.getAsJsonArray("effects").get(0).getAsString());
        assertEquals("leiden", first.get("kind").getAsString());
        assertEquals(2, o.size(), "only conditions and observedAt");
        assertTrue(JsonParser.parseString(ConditionWire.putBody(List.of(), 1L)).getAsJsonObject()
                .getAsJsonArray("conditions").isEmpty());
    }

    @Test
    void parsesGetResponseAndSkipsBrokenEntries() {
        UUID a = UUID.randomUUID();
        String json = "{\"serverTime\":5,\"players\":{\"" + a + "\":{\"conditions\":[{\"name\":\"Pocken\","
                + "\"effects\":[\"Fieber\"],\"kind\":\"leiden\"},{\"nope\":1}],\"observedAt\":3,\"updatedAt\":4},"
                + "\"not-a-uuid\":{\"conditions\":[]}}}";
        Map<UUID, ConditionWire.Remote> out = ConditionWire.parseGet(json);
        assertEquals(1, out.size());
        assertEquals(List.of(new Condition("Pocken", List.of("Fieber"), "leiden")), out.get(a).conditions());
        assertEquals(4L, out.get(a).updatedAt());
        assertTrue(ConditionWire.parseGet("garbage").isEmpty());
        assertTrue(ConditionWire.parseGet(null).isEmpty());
    }

    @Test
    void uuidQueryIsCommaSeparatedAndCappedAt100() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 120; i++) ids.add(UUID.randomUUID());
        assertEquals(100, ConditionWire.uuidQuery(ids).split(",").length);
        assertEquals("", ConditionWire.uuidQuery(List.of()));
    }

    @Test
    void endpointRules() {
        assertEquals("https://ottoextra.dev", ConditionEndpoint.base("https://ottoextra.dev/").orElseThrow().toString());
        assertTrue(ConditionEndpoint.base("http://localhost:8080").isPresent());
        assertTrue(ConditionEndpoint.base("http://127.0.0.1").isPresent());
        assertTrue(ConditionEndpoint.base("http://ottoextra.dev").isEmpty());
        assertTrue(ConditionEndpoint.base("https://evil.example").isEmpty());
        assertTrue(ConditionEndpoint.base("https://user@ottoextra.dev").isEmpty());
        assertTrue(ConditionEndpoint.base("").isEmpty());
        assertEquals("https://ottoextra.dev/api/v1/conditions/me",
                ConditionEndpoint.resolve("https://ottoextra.dev", "/api/v1/conditions/me").orElseThrow().toString());
    }

    private static AutoCheckGuard.Inputs ok() {
        return new AutoCheckGuard.Inputs(true, true, false, 60_000, 60_000, false, false, false, false, false,
                false, true);
    }

    @Test
    void guardAllowsQuietPlayerOnly() {
        assertNull(AutoCheckGuard.blockReason(ok()));
        var i = ok();
        assertEquals("recent damage", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 9_999,
                60_000, false, false, false, false, false, false, true)));
        assertEquals("recent attack", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 60_000,
                5_000, false, false, false, false, false, false, true)));
        assertEquals("combat boss bar", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, true,
                60_000, 60_000, false, false, false, false, false, false, true)));
        assertEquals("screen open", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 60_000,
                60_000, true, false, false, false, false, false, true)));
        assertEquals("Werkstatt job running", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false,
                60_000, 60_000, false, true, false, false, false, false, true)));
        assertEquals("moving", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 60_000,
                60_000, false, false, true, false, false, false, true)));
        assertEquals("sprinting", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 60_000,
                60_000, false, false, false, true, false, false, true)));
        assertEquals("flying", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 60_000,
                60_000, false, false, false, false, true, false, true)));
        assertEquals("riding", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 60_000,
                60_000, false, false, false, false, false, true, true)));
        assertEquals("window unfocused", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false,
                60_000, 60_000, false, false, false, false, false, false, false)));
        assertEquals("not on Ottonien", AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, false, false,
                60_000, 60_000, false, false, false, false, false, false, true)));
        assertNull(AutoCheckGuard.blockReason(new AutoCheckGuard.Inputs(true, true, false, 10_000, 10_000, false,
                false, false, false, false, false, true)), "exactly 10 s counts as over");
    }

    private static final List<Condition> A = List.of(new Condition("Pocken", List.of(), "leiden"));
    private static final List<Condition> B = List.of();

    @Test
    void sendsOnChangeAndHeartbeatOnly() {
        SharePlanner p = new SharePlanner();
        long t = 1_000_000L;
        assertEquals(SharePlanner.Action.NONE, p.next(t, true, null), "nothing read yet");
        assertEquals(SharePlanner.Action.PUT, p.next(t, true, A));
        p.wrote(t, A);
        assertEquals(SharePlanner.Action.NONE, p.next(t + 1_000, true, B), "write gap of 10 s");
        assertEquals(SharePlanner.Action.PUT, p.next(t + 10_000, true, B), "change after the gap");
        p.wrote(t + 10_000, B);
        assertEquals(SharePlanner.Action.NONE, p.next(t + 200_000, true, B));
        assertEquals(SharePlanner.Action.PUT, p.next(t + 10_000 + SharePlanner.HEARTBEAT_MS, true, B), "heartbeat");
    }

    @Test
    void inactiveSharingNeverPutsAndSendsAgainAfterSwitchingOn() {
        SharePlanner p = new SharePlanner();
        assertEquals(SharePlanner.Action.NONE, p.next(5_000_000L, false, A));
        p.wrote(5_000_000L, A);
        assertEquals(SharePlanner.Action.NONE, p.next(5_002_000L, false, A), "removal is deleteDue's job");
        p.deleted(5_002_000L);
        assertEquals(SharePlanner.Action.PUT, p.next(5_020_000L, true, A), "sends again after switching on");
    }

    @Test
    void deleteIsDueWhenACopyMayExistAndSharingIsOffOrConsentGone() {
        SharePlanner p = new SharePlanner();
        long t = 7_000_000L;
        assertFalse(p.deleteDue(t, false, false, true), "no copy, nothing to delete");
        assertFalse(p.deleteDue(t, true, true, true), "sharing active");
        assertTrue(p.deleteDue(t, true, false, true), "switched off");
        assertTrue(p.deleteDue(t, true, true, false), "consent revoked");
        p.deleteAttempted(t);
        assertFalse(p.deleteDue(t + 59_999, true, false, true), "retry at most once a minute");
        assertTrue(p.deleteDue(t + 60_000, true, false, true));
        p.failed(t + 60_000);
        assertFalse(p.deleteDue(t + 61_000 + SharePlanner.RETRY_AFTER_ERROR_MS - 2_000, true, false, true));
    }

    @Test
    void failuresBackOffAndQueriesAreRateLimited() {
        SharePlanner p = new SharePlanner();
        long t = 9_000_000L;
        p.failed(t);
        assertEquals(SharePlanner.Action.NONE, p.next(t + 30_000, true, A));
        assertEquals(SharePlanner.Action.PUT, p.next(t + SharePlanner.RETRY_AFTER_ERROR_MS, true, A));
        assertTrue(p.queryDue(t));
        p.queried(t);
        assertFalse(p.queryDue(t + 59_999));
        assertTrue(p.queryDue(t + 60_000));
        p.queryFailed(t + 60_000);
        assertFalse(p.queryDue(t + 120_000 - 1));
    }
}
