# AI-Powered SRE Log Diagnostics Utility

An out-of-band, high-performance background diagnostics utility designed to monitor, scrape, parse, and analyze real-time system log streams. By combining low-overhead system monitoring with GenAI-driven context matching, the utility transforms unstructured runtime failures and complex multi-line stack traces into structured, policy-compliant architectural resolutions.

## 🚀 Key Architectural Highlights

* **Low-Memory Tail Engine:** Utilizes an optimized background thread scheduler loop backed by `RandomAccessFile` to process continuous, real-time log byte streams with minimal memory overhead by tracking byte positions (`lastKnownPosition`).
* **GenAI Prompt Templating:** Leverages Spring AI `PromptTemplate` to inject unstructured Java exception strings dynamically into structured system prompts, preventing common API breaking errors during remote engine communication.
* **Runbook-Driven RAG Wrapper:** Features a hardcoded, in-memory context mapping table (`internalRunbooks`) that dynamically matches exception signatures against strict corporate recovery guidelines, forcing the `ChatModel` engine to prescribe policy-compliant solutions.

---

## 🛠️ Tech Stack

* **Core Runtime:** Java 21
* **Framework:** Spring Boot 3.x, Spring AI
* **LLM Integration Intermediary:** Spring AI `ChatModel` interface
* **Scheduling Framework:** Spring Task Execution Scheduler (`@Scheduled`)
* **I/O Engine:** Java NIO & I/O (`RandomAccessFile`, `File`)

---

## ⚙️ Core Architecture Workflow

---

## 🚀 Local Installation & Configuration

### 1. Prerequisites
* **Java SDK 21** or higher installed.
* An active, running instance of the core backend application writing telemetry streams to the shared log directory.
* A valid API Key for your underlying Spring AI LLM provider configured in your host environment.

### 2. Environment Setup
Configure your Spring AI connection keys inside your terminal environment before boot:
```bash
# Example if using Google Gemini provider setup
export SPRING_AI_GEMINI_API_KEY="your-api-key-here"
```

### 3. Application Path Configuration
The scraper tracks target service system behavior directly via the physical hard drive pipeline. The hardcoded target file pointer is configured as:
```java
private final String LOG_FILE_PATH = "var\\targetservice\\shared-logs\\targetLogs.log";
```
*Ensure that the folder directory `var\targetservice\shared-logs\` exists on your host OS system before initialization.*

### 4. Build and Run
```bash
./mvnw clean package
java -jar target/sre-log-diagnostics-0.0.1-SNAPSHOT.jar
```

---

## 📝 Corporate Runbook Enforcements

The utility evaluates incoming stack traces against hardcoded enterprise governance configurations before invoking the AI agent. The following signatures trigger distinct structural instructions:

* **`SQLException`**: Disallows database cluster rebooting; mandates scaling `spring.datasource.hikari.maximum-pool-size` to at least 50.
* **`NullPointerException`**: Checks for security boundary filter failures; mandates object null-checks around incoming authentication header parsing blocks.
* **`RuntimeException`**: Protects downstream microservice dependencies; mandates wrapping WebClient instances inside an isolated Resilience4j Circuit Breaker network.

---

## 🔍 Diagnostic Output Schema
All AI-generated analysis outputs are emitted directly to the console logs via SLF4J, matching this exact structural markdown standard:

```markdown
### 🚨 ROOT EXCEPTION DETECTED
[Identified Exception Class Name and File Line Reference]

### 📋 INTERNAL POLICY RECOGNITION
[Summary of matching company compliance rules]

### 🛠️ MANDATED FIX ACTIONS
[Precise configuration adjustments or Java code fix snippets]
```