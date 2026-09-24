package com.zeezaglobal.gymmanagement.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiService {

    @Value("${app.openrouter.api-key:}")
    private String apiKey;

    @Value("${app.openrouter.model:openrouter/free}")
    private String model;

    @Value("${app.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    private final RestClient restClient;
    private final Gson gson;

    public AiService() {
        this.gson = new Gson();
        this.restClient = RestClient.builder().build();
    }

    public String summarize(Object data) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OPENROUTER_API_KEY is not configured.");
            throw new BadRequestException("AI service is not configured. OPENROUTER_API_KEY is missing.");
        }

        try {
            String dataJson = (data instanceof String strData)
                    ? strData
                    : gson.toJson(data);

            Map<String, Object> requestPayload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "Summarize the given JSON in 3-5 plain-English sentences. Return only the summary."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", dataJson
                            )
                    )
            );

            String requestBodyJson = gson.toJson(requestPayload);

            String responseBody = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBodyJson)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new BadRequestException("Empty response received from OpenRouter AI service.");
            }

            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            if (root != null && root.has("choices")) {
                JsonArray choices = root.getAsJsonArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message.has("content")) {
                            JsonElement contentElem = message.get("content");
                            if (!contentElem.isJsonNull()) {
                                return contentElem.getAsString().trim();
                            }
                        }
                    }
                }
            }

            log.error("Unexpected OpenRouter response format: {}", responseBody);
            throw new BadRequestException("Failed to parse completion message from AI response.");

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling OpenRouter AI API: {}", e.getMessage(), e);
            throw new BadRequestException("AI summarization failed: " + e.getMessage());
        }
    }
}
