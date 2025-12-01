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
                String finalQuery = """
                    WITH high_salary_payments AS ( 
                        SELECT DISTINCT p.EMP_ID 
                        FROM PAYMENTS p 
                        WHERE p.AMOUNT > 70000 
                    ), 
                    qualified_employees AS ( 
                        SELECT 
                            d.DEPARTMENT_ID, 
                            d.DEPARTMENT_NAME, 
                            e.EMP_ID, 
                            e.FIRST_NAME, 
                            e.LAST_NAME, 
                            e.DOB, 
                            FLOOR(DATEDIFF('2025-12-01', e.DOB) / 365.25) AS AGE, 
                            CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS FULL_NAME 
                        FROM high_salary_payments h 
                        JOIN EMPLOYEE e ON h.EMP_ID = e.EMP_ID 
                        JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID 
                    ), 
                    dept_stats AS ( 
                        SELECT 
                            DEPARTMENT_ID, 
                            DEPARTMENT_NAME, 
                            AVG(AGE) AS AVERAGE_AGE, 
                            COUNT(*) AS cnt 
                        FROM qualified_employees 
                        GROUP BY DEPARTMENT_ID, DEPARTMENT_NAME 
                    ), 
                    employee_lists AS ( 
                        SELECT 
                            DEPARTMENT_ID, 
                            LISTAGG(FULL_NAME, ', ') WITHIN GROUP (ORDER BY FULL_NAME) AS raw_list 
                        FROM ( 
                            SELECT 
                                DEPARTMENT_ID, 
                                FULL_NAME, 
                                ROW_NUMBER() OVER (PARTITION BY DEPARTMENT_ID ORDER BY FULL_NAME) AS rn 
                            FROM qualified_employees 
                        ) ranked 
                        WHERE rn <= 10 
                        GROUP BY DEPARTMENT_ID 
                    ) 
                    SELECT 
                        ds.DEPARTMENT_NAME, 
                        ROUND(ds.AVERAGE_AGE, 2) AS AVERAGE_AGE, 
                        COALESCE(el.raw_list, '') AS EMPLOYEE_LIST 
                    FROM dept_stats ds 
                    LEFT JOIN employee_lists el ON ds.DEPARTMENT_ID = el.DEPARTMENT_ID 
                    ORDER BY ds.DEPARTMENT_ID DESC;""";

                solutionBody.put("finalQuery", finalQuery);

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