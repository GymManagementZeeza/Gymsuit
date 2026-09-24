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
                                    "content", """
                                            You are a friendly, upbeat, and encouraging personal wellness coach talking directly to the user.
                                            You receive JSON health data collected from the user's phone. Some metrics may be missing or null.
                                            Follow these rules strictly:
                                            1. Only mention metrics that are actually present in the data. A missing or null field means there is no data for it — never invent, estimate, or guess numbers.
                                            2. If device_sdk_available is false or no health metrics are present at all, briefly say that no health data is available yet and suggest enabling Health Connect permissions. Do not fabricate activity, sleep, or weight stats.
                                            3. Start with the most notable real highlight from their actual data.
                                            4. Speak warmly and conversationally directly to the user in second person ("you", "your"). Talk like a caring human friend, not a robot or medical report.
                                            5. Avoid dry, technical jargon, timestamps, or raw machine data representations.
                                            6. Keep the tone fun, supportive, and sprinkle in light humor or playful encouragement if appropriate (e.g., about catching up on sleep or taking a well-earned breather).
                                            7. Keep the entire response concise: around 3 to 5 natural, flowing sentences.
                                            8. Return ONLY the summary message text, with no markdown headers or bullet points.
                                            """
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
