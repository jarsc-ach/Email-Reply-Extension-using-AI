package com.email.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        String prompt = buildPrompt(emailRequest);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-1.5-flash:generateContent")
                        .queryParam("key", geminiApiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Blocking for simplicity

        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Error processing response: " + e.getMessage();
        }
    }

   private String buildPrompt(EmailRequest emailRequest) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are an AI assistant helping write email replies.\n");
    prompt.append("Generate ONLY the body content for a REPLY to the email below (no greetings like 'Dear' or sign-offs like 'Best regards').\n");
    prompt.append("Respond as if you're replying to this email — don't summarize or rephrase it.\n");
    prompt.append("Maintain a ").append(emailRequest.getTone() != null ? emailRequest.getTone() : "professional").append(" tone.\n");
    prompt.append("Do not include subject line or original email content in the reply.\n\n");
    prompt.append("----- ORIGINAL EMAIL -----\n");
    prompt.append(emailRequest.getEmailContent());

    return prompt.toString();
}


}
