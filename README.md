# FuelSurchargesCalculation

A high-performance, fault-tolerant Java integration utility engineered to extract critical fuel surcharge metrics from master corporate Excel workbooks (`.xls`) and transform them into a normalized, flat, semicolon-delimited CSV format ready for downstream data warehouse ingestion (SAP BW).

## Architectural Design

The application utilizes a clean, decoupled component architecture strictly aligned with the principles of **Separation of Concerns (SoC)** and the **Single Responsibility Principle (SRP)**:

```text
  [ Infrastructure & Config ]               [ Domain ]             [ Business Logic / ETL ]
   ┌────────────────────────────────┐       ┌──────────────┐       ┌────────────────────────┐
   │ fuel.retail.config.AppConfig   │       │FuelPriceEntry│       │     RawFileParser      │
   │  (Path Resolution, Bootstrap,  │       │ (Lombok POJO)│       │ (Extraction, Formula   │
   │        NIO Settings)           │       └──────▲───────┘       │    Evaluation Engine)  │
   └───────────────┬────────────────┘              │               └───────────┬────────────┘
                   │                               │                           │
                   │ Triggers AppConfig.init()     │ Populates entities        │
                   └───────────────────────────────┼───────────────────────────┘
                                                   │
                                                   │ Transmits typed collections
                                           ┌───────┴────────┐
                                           │ResultFileWriter│
                                           │ (CSV Persistence│
                                           │ & YAML Status) │
                                           └────────────────┘
```

### Component Breakdown

*   **`fuel.retail.config.AppConfig` (Configuration & Bootstrap Layer)**: The absolute initialization anchor of the system. Resolves runtime home path scopes, injects log routing metadata directly into the active JVM context, and parses external properties using native Java NIO stream boundaries.
*   **`fuel.retail.Main` (Orchestrator Layer)**: The primary application execution entry point. Coordinates the synchronized lifecycle invocation, encapsulates the isolated temporary file mirroring procedures, and enforces the fault-tolerant **Retry Policy** loop boundaries.
*   **`fuel.retail.parser.RawFileParser` (Extraction & Transformation Layer)**: Handles structural worksheet row inspections, tracks localized row parameters using the Stateful Tracking Pattern to resolve merged date cells, and triggers explicit formula cache updates.
*   **`fuel.retail.writer.ResultFileWriter` (Persistence & Formatting Layer)**: Handles character-delimited file serializations (`FuelSurchargesCalculation.txt`) matching strict UTF-8 boundaries and generates structural process check receipts for task coordinators.
*   **`fuel.retail.model.FuelPriceEntry` (Domain Data Layer)**: A lightweight Plain Old Java Object (POJO) optimized via **Project Lombok** annotations (`@Data`, `@Builder`), providing a strongly typed entity container for transactional parsing data.

---

## Technical Highlights

### 1. Anti-Deadlock Synchronized Bootstrap
To circumvent initialization race conditions within logging factory components, the application enforces a strict, explicit bootstrap sequence via `AppConfig.init()` invoked as the absolute first instruction inside the `main()` method. This guarantees all structural properties and logging destinations are mapped inside the JVM system layer before SLF4J/Log4j2 instantiate their runtime appenders.

### 2. Intelligent Environment Normalization (IDE vs Production)
A dedicated structural normalization filter is embedded directly within the path resolution phase. When running inside an Integrated Development Environment (IDE) where the active resource context resolves to `target/classes/`, the config engine automatically navigates up to the true project root folder layout. In production (`.jar`), it maps directly to the container folder, eliminating environmental deployment friction.

### 3. Native NIO Character Layer Protection
To allow end-users (such as system administrators or accountants) to safely modify settings using basic text editors (like Notepad) without learning complex Unicode escape symbols (`\uXXXX`), the application processes configurations using native `Files.readAllLines(..., StandardCharsets.UTF_8)` streams. All Cyrillic properties (e.g., `excel_sheet=Лист22`) are extracted natively without formatting corruption.

### 4. Exponential Backoff Retry Policy for Network Publication
In enterprise environments, writing output target buffers to distributed file system nodes can collide with active scheduling processes (e.g., a SAP BW extractor pulling data from `FuelSurchargesCalculation.txt` concurrently). To mitigate transient infrastructure collisions, the orchestration engine encapsulates the write subsystem inside an automated loop using an Exponential Backoff Policy:
*   Transient `IOException` locks trigger a warning state rather than a crash sequence.
*   The execution thread enters a controlled sleep window before recycling attempts, scaling the pause delay progressively using the configured deployment profile parameters:
    $$\text{timeout} = \text{initialTimeoutMs} \times (1 + \text{attempt}^{\text{attempt} - 1})$$
*   Example delays using `initial_timeout_ms=10000` (10 seconds):
    *   *Failure 1 (Attempt 1)*: $10000 \times (1 + 1^0) = 20,000$ ms delay before retry.
    *   *Failure 2 (Attempt 2)*: $10000 \times (1 + 2^1) = 30,000$ ms delay before retry.

---

## Deployment Topology & Dependency Isolation

The build model isolates external application dependencies. Third-party asset packages are automatically moved one directory level above the execution root, enabling clean component distribution on production hosts. The internal subpackages (`config`, `model`, `parser`, `writer`) are cleanly encapsulated inside the parent root package `fuel.retail`:

```text
D:/IT/Java/prj/excel/            # Automation root directory
├── dependencies/               # Shared libraries (Apache POI, Log4j2, Lombok)
│   ├── poi-5.2.5.jar
│   ├── log4j-core-2.20.0.jar
│   └── ...
└── FuelSurchargesCalculation/  # Application deployment home workspace
    ├── config/
    │   ├── config.properties   # External environment flat property bundle (UTF-8)
    │   └── log4j2.xml          # Logging appender layout rules
    ├── exchange/
    │   └── result.yaml         # Downstream automation process check receipt
    ├── logs/
    │   └── application.log     # Production rotating execution log asset
    ├── src/
    │   ├── main/
    │   │   └── java/
    │   │       └── fuel/
    │   │           └── retail/
    │   │               ├── config/
    │   │               │   └── AppConfig.java
    │   │               ├── model/
    │   │               │   └── FuelPriceEntry.java
    │   │               ├── parser/
    │   │               │   └── RawFileParser.java
    │   │               ├── writer/
    │   │               │   └── ResultFileWriter.java
    │   │               └── Main.java
    │   └── test/
    │       └── java/
    │           └── fuel/
    │               └── retail/
    │                   ├── model/
    │                   │   └── FuelPriceEntryTest.java
    │                   └── parser/
    │                       └── RawFileParserTest.java
    └── FuelSurchargesCalculation.jar  # Executable application binary module
```

---

## Configuration Specification

### External Properties (`config/config.properties`)

The application expects an environment-specific configuration file named `config.properties` inside the `config/` directory. For deployment setups, copy the anonymized distribution template (`config.properties.dist`) and populate the variables with your target infrastructure nodes:

```properties
# Resilient corporate UNC network share path to the source master Excel workbook (.xls)
# Syntax format: \\<server-domain-or-ip>\<share-name>\<folder-path>\<filename>.XLS
excel_file=

# Targeted worksheet identification token required for core extraction loops
excel_sheet=Лист22

# Resilient Retry Policy constraints used to mitigate transient network share locks
# The initial baseline cooldown delay interval mapped in milliseconds (10000ms = 10 seconds)
initial_timeout_ms=10000

# Absolute limitation threshold for operational loop recovery attempts (Default: 3 tries)
max_write_attempts=3
```

### Logging Topologies (`config/log4j2.xml`)
The Dual-Channel appender layout pipes events simultaneously: applying live color highlighting to the console layer and pushing persistent records into a rolling archive limited to 10 MB chunks.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %highlight{%-5level}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=cyan, TRACE=black} %logger{36} - %msg%n"/>
        </Console>

        <RollingFile name="FileAppender" 
                     fileName="\${sys:log_path:-logs}/application.log"
                     filePattern="\${sys:log_path:-logs}/archive/application-%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
                <SizeBasedTriggeringPolicy size="10 MB"/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
    </Appenders>

    <Loggers>
        <Logger name="org.apache.poi" level="WARN" additivity="false">
            <AppenderRef ref="ConsoleAppender"/>
            <AppenderRef ref="FileAppender"/>
        </Logger>
        <Root level="INFO">
            <AppenderRef ref="ConsoleAppender"/>
            <AppenderRef ref="FileAppender"/>
        </Root>
    </Loggers>
</Configuration>
```
---

## Testing Framework & Test Scenarios

The utility includes an isolated test suite powered by **JUnit 5** and **Mockito** frameworks to ensure transactional reliability and prevent regression bugs. To guarantee fast execution without database or filesystem friction, the business logic layer is tested in total isolation using mock objects.

### Covered Test Scenarios

#### 1. Domain Serialization Verification (`FuelPriceEntryTest`)
*   **Standard CSV Serializer Layout**: Validates that all domain parameters, date markers, and fuel maps are concatenated correctly using target delimiters (`;`) matching downstream integration contracts.
*   **Null-Safety Constraints Handler**: Ensures that if database fields or attributes are completely empty or missing, the serializer routes them as empty string blocks instead of failing with a `NullPointerException`.

#### 2. ETL Processing Engine Validation (`RawFileParserTest`)
*   **Numerical Scaling Control Precision**: Uses Mockito to stub a numeric binary cell stream (`HSSFCell`) returning unrounded data (e.g., `2480.556`) alongside a mocked `HSSFCellStyle`. Validates that `RawFileParser` executes the proper `Math.round()` routines to enforce exact double decimals (`2480.56`).
*   **Dynamic Margin Percentage Transformer**: Simulates percentage binary inputs (e.g., `0.0254`) associated with custom `0.0%` cell data format layouts. Validates that the engine successfully transforms them into scaled single decimal values (`2.5`) matching internal retail constraints.


## Build and Execution Controls

### Compiling and Running Unit Tests
To trigger the automated verification pipeline and execute all isolated business logic assertions, run the following command in the project root directory:
```bash
mvn test
```

### Compiling and Packaging Production Bundles
To compile the source code, isolate external system dependencies into the designated directory structure, and route the executable asset directly to the project root, execute the standard Maven lifecycle:
```bash
mvn clean package
```

### Production Execution
The application can be launched from any location within the OS host console layer by passing the absolute reference path to the target jar (internal runtime dependencies will resolve dynamically relative to the deployment asset location):
```bash
java -jar "<Path to the root of the project>\FuelSurchargesCalculation.jar"
```
