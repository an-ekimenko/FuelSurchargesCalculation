package fuel.retail.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuelPriceEntryTest {

    @Test
    @DisplayName("Should correctly serialize entry attributes to flat CSV text format")
    void shouldSerializeToCsvRowCorrectly() {
        // Given
        FuelPriceEntry entry = new FuelPriceEntry("20261102", "опт. за тонну");
        entry.addFuelValue("92-К5", "2480.5");
        entry.addFuelValue("95-евро", "2700.0");

        // When
        String csvRow = entry.toCsvRow(";");

        // Then
        assertEquals("20261102;опт. за тонну;2480.5;2700.0;", csvRow, 
            "The generated CSV row layout does not match standard data constraints.");
    }

    @Test
    @DisplayName("Should handle missing dynamic attributes without falling into NullPointerException errors")
    void shouldHandleNullValuesSafelyInCsvSerialization() {
        // Given
        FuelPriceEntry entry = new FuelPriceEntry(null, null);

        // When & Then
        assertDoesNotThrow(() -> {
            String csvRow = entry.toCsvRow(";");
            assertEquals(";;", csvRow, "Null boundaries must be serialized as empty string blocks.");
        });
    }
}
