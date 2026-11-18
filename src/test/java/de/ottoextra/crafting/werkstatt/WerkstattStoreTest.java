package de.ottoextra.crafting.werkstatt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WerkstattStoreTest {
    @TempDir
    Path dir;

    private static WerkstattJob job(String name) {
        WerkstattJob j = new WerkstattJob();
        j.name = name;
        j.lore = new ArrayList<>(RecipeLoreTest.EISENBARREN);
        j.path = new ArrayList<>(List.of(new WerkstattJob.PathStep(10, "Zeugschmieden")));
        j.slot = 15;
        j.quantity = 20;
        return j;
    }

    @Test void savedAndQueueSurviveReopenPerAccount() {
        WerkstattStore a = WerkstattStore.open(dir, "Play.Ottonien.com", "Alice");
        a.data().saved.add(job("Eisenbarren"));
        a.data().queue.add(job("Stahlbarren"));
        a.save();
        WerkstattStore again = WerkstattStore.open(dir, "play.ottonien.com ", "alice");
        assertEquals("Eisenbarren", again.data().saved.get(0).name);
        assertEquals("Stahlbarren", again.data().queue.get(0).name);
        assertEquals("Zeugschmieden", again.data().queue.get(0).path.get(0).name);
        WerkstattStore bob = WerkstattStore.open(dir, "play.ottonien.com", "Bob");
        assertTrue(bob.data().saved.isEmpty(), "accounts are separate");
    }

    @Test void fileNameIsTheHashOfTheServerNotTheAddress() {
        String name = WerkstattStore.fileName("play.ottonien.com");
        assertTrue(name.matches("[0-9a-f]{64}\\.json"));
        assertEquals(name, WerkstattStore.fileName(" PLAY.ottonien.com"));
        WerkstattStore.open(dir, "play.ottonien.com", "x").save();
        assertTrue(Files.exists(dir.resolve(name)));
    }

    @Test void runningJobIsStoppedAfterReloadAndBrokenFileStartsEmpty() throws Exception {
        WerkstattStore s = WerkstattStore.open(dir, "srv", "acc");
        WerkstattJob j = job("Eisenbarren");
        j.status = WerkstattJob.Status.RUNNING;
        j.done = 6;
        s.data().queue.add(j);
        s.save();
        WerkstattJob back = WerkstattStore.open(dir, "srv", "acc").data().queue.get(0);
        assertEquals(WerkstattJob.Status.STOPPED, back.status);
        assertEquals(StopReason.DISCONNECTED, back.stopReason);
        assertEquals(6, back.done);
        Files.writeString(dir.resolve(WerkstattStore.fileName("srv")), "{ kaputt");
        assertTrue(WerkstattStore.open(dir, "srv", "acc").data().queue.isEmpty());
    }

    @Test void recipeKeyAndPathText() {
        WerkstattJob j = job("Eisenbarren");
        assertEquals("Zeugschmieden > Slot 15", j.pathText());
        assertEquals(j.recipeKey(), j.copyFresh().recipeKey());
        assertEquals(0, j.copyFresh().done);
    }
}
