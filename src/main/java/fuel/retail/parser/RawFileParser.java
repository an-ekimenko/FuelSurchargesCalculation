package fuel.retail.parser;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.WorkbookFactory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fuel.retail.model.FuelPriceEntry;

/**
 * Extraction and Transformation Layer (ETL) component.
 * Responsible for parsing the structural layout of the source Excel (.xls) file,
 * resolving sequential state-tracking date boundaries, and evaluating formulas.
 */
public class RawFileParser {
    private static final Logger log = LoggerFactory.getLogger(RawFileParser.class);

    private HSSFWorkbook book;
    private HSSFSheet sheet;
    private FormulaEvaluator evaluator;
    
    private String lastFoundDate = ""; 
    private final DateFormat df = new SimpleDateFormat("yyyyMMdd");
    private final List<FuelPriceEntry> listOfEntries = new ArrayList<>();
    private final List<String> fuelHeaderNames = new ArrayList<>();
    
    private final double scale = Math.pow(10, 2);
    private final double scale_p = Math.pow(10, 1);
    private final Pattern dateRegExPattern = Pattern.compile("\\d{8}");

    /**
     * Parses the targeted Excel worksheet and maps rows into a collection of domain entities.
     *
     * @param filePath  Absolute or relative path to the source Excel file.
     * @param sheetName Target worksheet identifier.
     * @return List of populated {@link FuelPriceEntry} objects.
     * @throws IOException            If network or local filesystem stream extraction fails.
     * @throws InvalidFormatException If the source binary layout is corrupted.
     */
    public List<FuelPriceEntry> parse(String filePath, String sheetName) throws IOException, InvalidFormatException {
        listOfEntries.clear();
        fuelHeaderNames.clear();
        lastFoundDate = "";

        openBook(filePath);
        
        if (book != null) {
            evaluator = book.getCreationHelper().createFormulaEvaluator();
            
            // Direct extraction by explicit name parameter as executed in pre-refactored state
            sheet = book.getSheet(sheetName);
            
            if (sheet == null) {
                book.close();
                throw new IOException("The Excel sheet '" + sheetName + "' was not found in the workbook.");
            }
            
            parseSheet();
            book.close();     
        } else {
            throw new IOException("Error reading the Excel workbook from: " + filePath);
        }
        
        return listOfEntries;
    }
    
    /**
     * Decodes the resource path and initializes the HSSFWorkbook workbook instance.
     */
    private void openBook(String path) throws InvalidFormatException, IOException {
        String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");
        File file = new File(decodedPath);
        book = (HSSFWorkbook) WorkbookFactory.create(file);
        log.debug("Workbook successfully opened from path: {}", decodedPath);
    }
    
    /**
     * Iterates over horizontal index layers, tracks dynamic headers, 
     * and maps row-by-row properties into target domain wrappers.
     */
    private void parseSheet() {
        int rowStart = sheet.getFirstRowNum();
        int rowEnd = sheet.getLastRowNum();

        // Step 1: Scan layout parameters to dynamically resolve fuel type header columns
        for (int rw = rowStart; rw <= rowEnd; rw++) {
            HSSFRow row = sheet.getRow(rw);
            if (row == null) continue;
            
            HSSFCell cellB = row.getCell(1);
            String cellBValue = readCell(cellB);
            
            if ("Характеристика".equalsIgnoreCase(cellBValue)) {
                short maxCol = row.getLastCellNum();
                for (short col = 2; col < maxCol; col++) {
                    String fuelName = readCell(row.getCell(col));
                    fuelHeaderNames.add("blank".equals(fuelName) ? "Unknown_Fuel_" + col : fuelName);
                }
                rowStart = rw + 1; // Content metrics begin strictly on the next line layer
                log.info("Detected header row. Fuel columns count: {}", fuelHeaderNames.size());
                break;
            }
        }

        // Step 2: Extract metric records sequentially using the Stateful Tracking Pattern
        for (int rw = rowStart; rw <= rowEnd; rw++) {
            HSSFRow row = sheet.getRow(rw);
            if (row == null) continue;
		    
            // Intercept data context date markers in Column A (Index 0)
            HSSFCell firstCell = row.getCell(0);
            String firstCellValue = readCell(firstCell);
			
            if (firstCellValue != null && !"blank".equals(firstCellValue) && !firstCellValue.isEmpty()) {
                Matcher regExMatcher = dateRegExPattern.matcher(firstCellValue);
                if (regExMatcher.find()) {
                    lastFoundDate = firstCellValue;
                }
            }

            // Extract characteristic classification markers in Column B (Index 1)
            HSSFCell characteristicCell = row.getCell(1);
            String characteristic = readCell(characteristicCell);
            if ("blank".equals(characteristic) || characteristic.isEmpty()) {
                continue; 
            }

            // Build structured domain entry container
            FuelPriceEntry entry = new FuelPriceEntry(lastFoundDate, characteristic);

            // Populate inner values map corresponding to dynamic fuel positions (Index 2 onwards)
            short maxCol = row.getLastCellNum();
            int fuelIndex = 0;
            for (short col = 2; col < maxCol; col++) {
                String cellValue = readCell(row.getCell(col));
                String cleanValue = "blank".equals(cellValue) ? "" : cellValue;
                
                String fuelName = fuelIndex < fuelHeaderNames.size() 
                        ? fuelHeaderNames.get(fuelIndex) 
                        : "Unknown_Fuel_" + col;
                
                entry.addFuelValue(fuelName, cleanValue);
                fuelIndex++;
            }
			
            listOfEntries.add(entry);
        }
        log.info("Parsing completed. Successfully extracted {} data entries.", listOfEntries.size());
    }

    /**
     * Resolves the POI cell type format, evaluates formula caches, 
     * and performs precise mathematical rounding controls.
     *
     * @param cell Raw POI cell resource reference.
     * @return String representation of the cell value or "blank".
     */
    private String readCell(HSSFCell cell) {
        if (cell == null) return "blank";
		
        // Explicitly trigger evaluation to fetch actual values from external formula relations
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            try {
                evaluator.evaluateFormulaCell(cell);
            } catch (Exception ignored) {}
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getRichStringCellValue().getString().trim();	
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return df.format(cell.getDateCellValue());
                } else {
                    double value = cell.getNumericCellValue();
                    String formatStr = cell.getCellStyle().getDataFormatString();
                    if (formatStr != null && formatStr.contains("0.0%")) {
                        return String.valueOf(Math.round(value * 100 * scale_p) / scale_p);
                    } else {
                        return String.valueOf(Math.round(value * scale) / scale);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    case NUMERIC: 
                        double value = cell.getNumericCellValue();
                        String formatStr = cell.getCellStyle().getDataFormatString();
                        if (formatStr != null && formatStr.contains("0.0%")) {
                            return String.valueOf(Math.round(value * 100 * scale_p) / scale_p);
                        } else {
                            return String.valueOf(Math.round(value * scale) / scale);
                        }
                    case STRING:
                        return cell.getStringCellValue().trim();
                    case BLANK:
                    case ERROR:
                    default:
                        return "blank";
                } 
            case BLANK:
            default:
                return "blank";
        }	
    }
}
