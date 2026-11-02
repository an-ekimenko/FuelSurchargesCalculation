package fuel.retail.parser;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle; 

import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RawFileParserTest {

    @Test
    @DisplayName("Should evaluate numeric cell formats with controlled mathematical scaling controls")
    void shouldEvaluateNumericCellWithCorrectRounding() throws Exception {
        // Given
        RawFileParser parser = new RawFileParser();
        HSSFCell mockCell = mock(HSSFCell.class);
        
       HSSFCellStyle mockStyle = mock(HSSFCellStyle.class);

        // Scenario: Test regular scale control (e.g., fuel pricing decimals)
        when(mockCell.getCellType()).thenReturn(CellType.NUMERIC);
        when(mockCell.getNumericCellValue()).thenReturn(2480.556);
        when(mockCell.getCellStyle()).thenReturn(mockStyle);
        when(mockStyle.getDataFormatString()).thenReturn("0.00");

        // Reflective execution path access for isolated private utility methods
        Method readCellMethod = RawFileParser.class.getDeclaredMethod("readCell", HSSFCell.class);
        readCellMethod.setAccessible(true);

        // When
        String resultValue = (String) readCellMethod.invoke(parser, mockCell);

        // Then
        assertEquals("2480.56", resultValue, "The numerical parser precision scale did not execute Math.round limits accurately.");
    }

    @Test
    @DisplayName("Should extract percentage profiles scaling data to 1 decimal place format")
    void shouldEvaluatePercentageFormatCorrectly() throws Exception {
        // Given
        RawFileParser parser = new RawFileParser();
        HSSFCell mockCell = mock(HSSFCell.class);
        
        HSSFCellStyle mockStyle = mock(HSSFCellStyle.class);

        // Scenario: Test percent math logic (0.0254 mapped to 2.5%)
        when(mockCell.getCellType()).thenReturn(CellType.NUMERIC);
        when(mockCell.getNumericCellValue()).thenReturn(0.0254);
        when(mockCell.getCellStyle()).thenReturn(mockStyle);
        when(mockStyle.getDataFormatString()).thenReturn("0.0%");

        Method readCellMethod = RawFileParser.class.getDeclaredMethod("readCell", HSSFCell.class);
        readCellMethod.setAccessible(true);

        // When
        String resultValue = (String) readCellMethod.invoke(parser, mockCell);

        // Then
        assertEquals("2.5", resultValue, "The custom margin percentage scaling layer failed precision controls verification.");
    }
}
