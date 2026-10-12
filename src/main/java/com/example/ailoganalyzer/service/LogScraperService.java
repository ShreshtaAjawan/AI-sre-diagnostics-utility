package com.example.ailoganalyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.RandomAccessFile;
import java.util.Map;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class LogScraperService {

    private final ChatModel chatModel;

    private static final Logger log = LoggerFactory.getLogger(LogScraperService.class);

    public LogScraperService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private long lastKnownPosition =0;

    private final String LOG_FILE_PATH = "C:\\Users\\Shreshta\\IdeaProjects\\targetservice\\shared-logs\\targetLogs.log";

    private final Map<String, String> internalRunbooks = Map.of(
            "SQLException", """
            POLICY: Do not reboot the database cluster.
            RESOLUTION: Instruct the developer to ensure 'spring.datasource.hikari.maximum-pool-size' 
            is scaled to at least 50 in application.properties.
            """,
            "NullPointerException", """
            POLICY: Any crash inside an AuthenticationFilter or JWT validation class requires security audit logs.
            RESOLUTION: Advise the developer to implement object null-checks around the header parsing logic.
            """,
            "RuntimeException", """
            POLICY: External HTTP dependencies must fail gracefully without blocking application threads.
            RESOLUTION: Recommend wrapping downstream WebClient calls inside a Resilience4j Circuit Breaker.
            """
    );
    @Scheduled(fixedDelay = 90000)
    public void scrapeLogs() {
        File file = new File(LOG_FILE_PATH);
        if (!file.exists()) {
            log.info("file not exists");
            return; // Skip if the target app hasn't written any logs
        }

        try (RandomAccessFile reader = new RandomAccessFile(file, "r")) {
            log.info("file exists");
            reader.seek(lastKnownPosition); // Jump straight to the new logs
            log.info("got the last position");
            String line;
            StringBuilder errorBatch = new StringBuilder();
            boolean capturingError = false;
            while ((line = reader.readLine()) != null) {
                // If we see an ERROR or standard stack trace lines
                if (line.contains("ERROR") || line.contains("Exception") || line.contains("\tat ")) {
                    capturingError = true;
                    log.info("error found");
                    errorBatch.append(line).append("\n");
                } else if (capturingError && line.matches("^\\d{4}-\\d{2}-\\d{2}T.*")) {
                    // Stop capturing when a normal healthy log line starts with a timestamp
                    processErrorWithAI(errorBatch.toString());
                    errorBatch.setLength(0);
                    capturingError = false;
                }
            }

            if (capturingError && !errorBatch.toString().isEmpty()) {
                processErrorWithAI(errorBatch.toString());
            }
            lastKnownPosition = reader.getFilePointer(); // Save position for next 5 seconds

        } catch (Exception e) {
            System.err.println("Scraper error: " + e.getMessage());
        }
    }

    /**
     * Passes the log text and matching internal runbook context directly to Spring AI.
     */
    private void processErrorWithAI(String rawErrorLog) {
        if (rawErrorLog.trim().isEmpty()) return;

        System.out.println("\n🔍 [SRE Agent] New log exception detected. Matching internal playbooks...");
        // Find a matching corporate runbook rule based on text keywords
        String corporateContext = "No specific internal company policy found for this exception.";
        for (Map.Entry<String, String> entry : internalRunbooks.entrySet()) {
            if (rawErrorLog.contains(entry.getKey())) {
                corporateContext = entry.getValue();
                break;
            }
        }

        try{
            // System instructions given to the LLM
            String systemInstruction = """
                You are an autonomous AI Site Reliability Engineer (SRE).
                An enterprise application has crashed. You must parse the error log and generate a professional fix.
                
                CRITICAL INSTRUCTION: You must check the INTERNAL CORPORATE CONTEXT below. If a corporate 
                rule is found, your resolution MUST comply with company policy.
                
                INTERNAL CORPORATE CONTEXT:
                """ + corporateContext + """
                
                Please output your analysis cleanly matching this exact markdown schema:
                ### 🚨 ROOT EXCEPTION DETECTED
                (Specify exact exception class name and line number if visible)
                
                ### 📋 INTERNAL POLICY RECOGNITION
                (State whether an internal company policy was matched and summarize it)
                
                ### 🛠️ MANDATED FIX ACTIONS
                (Provide exact code change snippets or configuration adjustment steps)
                """;

            PromptTemplate promptTemplate = new PromptTemplate(systemInstruction);

            // Bind raw stack trace text into the template variable
            Prompt formattedPrompt = promptTemplate.create(Map.of("crashReport", rawErrorLog));

            String aiDiagnosis = String.valueOf(chatModel.call(formattedPrompt));

            log.info("SYSTEM HEALTH DIAGNOSTIC REPORT GENERATED BY SPRING AI:");
            log.info(aiDiagnosis);

        }catch(Exception e){
            log.error("AI API Integration Exception: " + e.getMessage());
            e.printStackTrace();
        }


    }
}
