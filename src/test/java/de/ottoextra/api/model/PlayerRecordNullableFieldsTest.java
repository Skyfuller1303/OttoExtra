package de.ottoextra.api.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerRecordNullableFieldsTest {
    @Test
    void missingMoneyAndWarningFieldsStayUnavailableWhileZeroStaysZero() {
        Gson gson = new Gson();
        PlayerRecord missing = gson.fromJson("{}", PlayerRecord.class);
        PlayerRecord explicitNull = gson.fromJson(
                "{\"money\":null,\"warns\":null,\"remaining_warns\":null}", PlayerRecord.class);
        PlayerRecord zero = gson.fromJson(
                "{\"money\":0,\"warns\":0,\"remaining_warns\":0}", PlayerRecord.class);

        assertNull(missing.money());
        assertNull(explicitNull.money());
        assertNull(explicitNull.warns());
        assertNull(explicitNull.remaining_warns());
        assertEquals(0, zero.money());
        assertEquals(0, zero.warns());
        assertEquals(0, zero.remaining_warns());
    }
}
