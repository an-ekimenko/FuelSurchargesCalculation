package fuel.retail.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain model entity representing a single transactional row containing 
 * key performance indicators (KPIs), metrics, and fuel values.
 * Leverages Project Lombok to abstract boilerplate code (getters, setters, builders).
 */
@Data
@NoArgsConstructor

@Builder
public class FuelPriceEntry {
    
    /**
     * The processing date boundary normalized to the "yyyyMMdd" pattern.
     */
    private String date;
    
    /**
     * The business metric or performance attribute description (e.g., margins, wholesale/retail prices).
     */
    private String characteristic;
    
    /**
     * Strongly typed container mapping fuel column names to their evaluated scalar values.
     * Initialized directly as a final field to ensure it is never null when instantiated via 
     * custom constructors, preserving horizontal cell sequences during extraction loops.
     */
    private final Map<String, String> fuelValues = new LinkedHashMap<>();

    /**
     * Convenience constructor designed for targeted structural initialization 
     * during sequential state-tracking parsing loops.
     * Guarantees that the underlying fuel values map is initialized and ready for data insertion.
     *
     * @param date           The resolved contextual date component.
     * @param characteristic The specific structural indicator classification.
     */
    public FuelPriceEntry(String date, String characteristic) {
        this.date = date;
        this.characteristic = characteristic;
    }

    /**
     * Appends an individual evaluated metric or price token to the targeted fuel classification node.
     *
     * @param fuelType Normalized identification label of the fuel type.
     * @param value    Calculated target scalar record value.
     */
    public void addFuelValue(String fuelType, String value) {
        this.fuelValues.put(fuelType, value);
    }

    /**
     * Serializes internal business domain attributes into a flat, normalized 
     * character-delimited string record optimized for down-stream ETL storage ingestion (e.g., SAP BW).
     *
     * @param separator Selected data token delimiter symbol (e.g., ";").
     * @return Flat text line layout mapping all extracted entry metrics.
     */
    public String toCsvRow(String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(date != null ? date : "").append(separator);
        sb.append(characteristic != null ? characteristic : "").append(separator);
        
        for (String value : fuelValues.values()) {
            sb.append(value != null ? value : "").append(separator);
        }
        
        return sb.toString();
    }
}
