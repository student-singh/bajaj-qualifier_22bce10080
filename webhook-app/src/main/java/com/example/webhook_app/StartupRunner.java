package com.example.webhook_app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Profile("!test")
public class StartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class);

    @Override
    public void run(String... args) throws Exception {
        try {
            // Step 1: Create RestTemplate
            RestTemplate restTemplate = new RestTemplate();

            // Step 2: Prepare first POST request body (use YOUR details)
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("name", "Your Real Name");  // e.g., "John Doe"
            requestBody.put("regNo", "YourRegNo");     // e.g., "REG12347"
            requestBody.put("email", "your@email.com"); // e.g., "john@example.com"

            // Step 3: Set headers for JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Step 4: Create entity with body and headers
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Step 5: Send POST to generate webhook
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, entity, Map.class);

            // Step 6: Extract webhook URL and accessToken from response
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String webhookUrl = (String) responseBody.get("webhook");  // Assuming key is "webhook"
                String accessToken = (String) responseBody.get("accessToken");

                // Step 7: Prepare second POST body with your SQL query
                Map<String, String> solutionBody = new HashMap<>();
                solutionBody.put("finalQuery", "YOUR_FINAL_SQL_QUERY_HERE");  // Paste your solved query here

                // Step 8: Set headers for second request (JWT and JSON)
                HttpHeaders solutionHeaders = new HttpHeaders();
                solutionHeaders.setContentType(MediaType.APPLICATION_JSON);
                solutionHeaders.set("Authorization", "Bearer " + accessToken);  // JWT as Bearer token

                // Step 9: Create entity for second request
                HttpEntity<Map<String, String>> solutionEntity = new HttpEntity<>(solutionBody, solutionHeaders);

                // Step 10: Send POST to webhook URL
                ResponseEntity<String> solutionResponse = restTemplate.postForEntity(webhookUrl, solutionEntity, String.class);

                // Step 11: Print result (for testing)
                logger.info("Solution submitted: {}", solutionResponse.getStatusCode());
            } else {
                logger.warn("Error generating webhook: {}", response.getStatusCode());
            }
        } catch (RestClientException ex) {
            // Don't let external HTTP failures break application startup or tests
            logger.error("Failed to generate/submit webhook during startup: {}", ex.toString());
        } catch (Exception ex) {
            logger.error("Unexpected error in StartupRunner: {}", ex.toString());
        }
    }
}