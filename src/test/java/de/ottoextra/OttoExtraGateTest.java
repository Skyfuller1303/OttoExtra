package de.ottoextra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OttoExtraGateTest {

    @Test
    void acceptsOfficialHostAndSubdomains() {
        assertTrue(OttoExtraGate.isOttonienAddress("ottonien.com"));
        assertTrue(OttoExtraGate.isOttonienAddress("OTTONIEN.COM:25565"));
        assertTrue(OttoExtraGate.isOttonienAddress("play.ottonien.com"));
        assertTrue(OttoExtraGate.isOttonienAddress("play.ottonien.com."));
    }

    @Test
    void rejectsLookalikeAndUnrelatedHosts() {
        assertFalse(OttoExtraGate.isOttonienAddress("ottonien.example.org"));
        assertFalse(OttoExtraGate.isOttonienAddress("evil-ottonien.com"));
        assertFalse(OttoExtraGate.isOttonienAddress("ottonien.com.example.org"));
        assertFalse(OttoExtraGate.isOttonienAddress("example.org"));
    }
}
