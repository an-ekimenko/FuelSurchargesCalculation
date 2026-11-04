package fuel.retail.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import fuel.retail.model.FuelPriceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistence and Formatting Layer component.
 * Responsible exclusively for target delivery lifecycles, flat character-delimited layout formatting,
 * metadata padding, and emitting structured process termination receipts for external job schedulers.
 */
public class ResultFileWriter {
    private static final Logger log = LoggerFactory.getLogger(ResultFileWriter.class);
    private static final String CSV_FILE_NAME = "FuelSurchargesCalculation.txt";

    /**
     * Serializes collected domain entries into a flat, semicolon-delimited text record 
     * using uniform UTF-8 encoding.
     *
     * @param outputDir Target directory path context extracted from configuration.
     * @param entries   List of populated {@link FuelPriceEntry} items to write.
     * @return Generated file handle reference.
     * @throws IOException If storage write locks or directory allocation failures occur.
     */
    public File writeCsv(String outputDir, List<FuelPriceEntry> entries) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists() && dir.mkdirs()) {
            log.debug("Created target directories for CSV: {}", outputDir);
        }
        
        File txtFile = new File(dir, CSV_FILE_NAME);
        log.info("Saving parsed entities to file: {}", txtFile.getAbsolutePath());
		
        try (Writer writer = Files.newBufferedWriter(txtFile.toPath(), StandardCharsets.UTF_8)) {
            for (FuelPriceEntry entry : entries) { 
                writer.append(entry.toCsvRow(";")).append('\n');
            }
            writer.flush();
        }
        log.info("Successfully written {} lines to output CSV file.", entries.size());
        return txtFile;
    }

    /**
     * Emits a strict analytical tracking receipt formatted as a standalone YAML block 
     * inside the 'exchange/result.yaml' directory boundary.
     *
     * @param returnedCode Integer token representing lifecycle status (0 = Success, 1 = Fault).
     */
    public void writeYamlStatus(int returnedCode) {
        File dir = new File("exchange");
        if (!dir.exists() && dir.mkdirs()) {
            log.debug("Created directory for status verification: {}", dir.getAbsolutePath());
        }

        File yamlFile = new File(dir, "result.yaml");
        log.info("Writing execution status to: {}", yamlFile.getAbsolutePath());

        // Format execution timestamp to match the analytical target layout pattern (yyyy-MM-dd:HH:mm:ss)
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(new Date());

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(yamlFile), "UTF-8"))) {
            writer.append("output:\n");
            writer.append("  datetime: \"").append(currentDateTime).append("\"\n");
            writer.append("  returned_code: ").append(String.valueOf(returnedCode)).append("\n");
            writer.flush();
            log.info("Status file updated with returned_code: {}", returnedCode);
        } catch (IOException e) {
            log.error("Failed to write yaml status file to disk layout", e);
        }
    }
}
