package fuel.retail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure Layer component.
 * Centralizes application home directory path resolution and environment variable routing.
 * Automatically detects and normalization IDE build targets vs production JAR roots.
 */
public class AppConfig {
    public static File APPLICATION_HOME;
    private static Logger log;
    private static final Map<String, String> configMap = new HashMap<>();
    private static boolean isInitialized = false;

    /**
     * Explicitly bootstraps application path routing and configurations.
     * Must be executed as the absolute first instruction in the main entry point.
     */
    public static synchronized void init() {
        if (isInitialized) return;

        // Step 1: Compute absolute physical execution context home directory
        File targetDir;
        try {
            String path = AppConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File sourceFile = new File(path);
            targetDir = sourceFile.isDirectory() ? sourceFile : sourceFile.getParentFile();
            
            // SMART NORMALIZATION FOR IDE: If running from target/classes/ or target/test-classes/, 
            // navigate up to the true project root folder layout
            String normalizedPath = targetDir.getAbsolutePath().replace(File.separatorChar, '/');
            if (normalizedPath.endsWith("/target/classes") || normalizedPath.endsWith("/target/test-classes")) {
                targetDir = targetDir.getParentFile().getParentFile();
            }
        } catch (Exception e) {
            targetDir = new File(".");
        }
        APPLICATION_HOME = targetDir;

        // Step 2: Inject mandatory logging routing targets into system environment before factories fire up
        System.setProperty("log_path", new File(APPLICATION_HOME, "logs").getAbsolutePath());
        File logConfig = new File(APPLICATION_HOME, "config/log4j2.xml");
        if (logConfig.exists()) {
            System.setProperty("log4j2.configurationFile", logConfig.getAbsolutePath());
        }

        // Step 3: Now it is completely safe to initialize Loggers
        log = LoggerFactory.getLogger(AppConfig.class);

        // Step 4: Extract parameters via native NIO lines reader
        File configFile = new File(APPLICATION_HOME, "config/config.properties");
        if (configFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(configFile.toURI()), StandardCharsets.UTF_8);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        configMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
                log.info("Configuration successfully loaded from absolute path: {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to read external configuration file properties", e);
            }
        } else {
            System.err.println("CRITICAL: External configuration file missing at: " + configFile.getAbsolutePath());
        }
        isInitialized = true;
    }

    public static String getExcelFile() {
        init();
        return configMap.getOrDefault("excel_file", "");
    }

    public static String getExcelSheet() {
        init();
        return configMap.getOrDefault("excel_sheet", "Лист22");
    }

    public static int getInitialTimeoutMs() {
        init();
        return Integer.parseInt(configMap.getOrDefault("initial_timeout_ms", "10000"));
    }

    public static int getMaxWriteAttempts() {
        init();
        return Integer.parseInt(configMap.getOrDefault("max_write_attempts", "3"));
    }
}
