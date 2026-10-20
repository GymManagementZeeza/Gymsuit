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

    @Value("${app.openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${app.openai.model:${OPENAI_MODEL:gpt-4o-mini}}")
    private String openaiModel;

    @Value("${app.openai.base-url:${OPENAI_BASE_URL:https://api.openai.com/v1}}")
    private String openaiBaseUrl;

    @Value("${app.openrouter.api-key:${OPENROUTER_API_KEY:}}")
    private String openrouterApiKey;

    @Value("${app.openrouter.model:${OPENROUTER_MODEL:google/gemini-2.5-flash}}")
    private String openrouterModel;

    @Value("${app.openrouter.base-url:${OPENROUTER_BASE_URL:https://openrouter.ai/api/v1}}")
    private String openrouterBaseUrl;

    private final RestClient restClient;
    private final Gson gson;

    public AiService() {
        this.gson = new Gson();
        this.restClient = RestClient.builder().build();
    }

    public String summarize(Object data) {
        final String activeApiKey;
        final String activeModel;
        final String activeBaseUrl;
        final String providerName;

        if (isValidApiKey(openaiApiKey)) {
            activeApiKey = openaiApiKey.trim();
            activeModel = (openaiModel != null && !openaiModel.trim().isEmpty()) ? openaiModel.trim() : "gpt-4o-mini";
            activeBaseUrl = (openaiBaseUrl != null && !openaiBaseUrl.trim().isEmpty()) ? openaiBaseUrl.trim() : "https://api.openai.com/v1";
            providerName = "OpenAI";
        } else if (isValidApiKey(openrouterApiKey)) {
            activeApiKey = openrouterApiKey.trim();
            activeModel = (openrouterModel != null && !openrouterModel.trim().isEmpty()) ? openrouterModel.trim() : "google/gemini-2.5-flash";
            activeBaseUrl = (openrouterBaseUrl != null && !openrouterBaseUrl.trim().isEmpty()) ? openrouterBaseUrl.trim() : "https://openrouter.ai/api/v1";
            providerName = "OpenRouter";
        } else {
            log.error("Neither a valid OPENAI_API_KEY nor OPENROUTER_API_KEY is configured.");
            throw new BadRequestException("AI service is not configured with a valid API key. Please check the OPENAI_API_KEY credential in Jenkins.");
        }

        try {
            String dataJson = (data instanceof String strData)
                    ? strData
                    : gson.toJson(data);

            Map<String, Object> requestPayload = Map.of(
                    "model", activeModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                                            You are a professional health and fitness analyst writing a brief summary directly to the user.
                                            The provided JSON input contains non-clinical, de-identified daily aggregate fitness and activity statistics (such as steps taken, calories burned, workout duration, and sleep hours).
                                            It does not contain any personally identifiable information (PII), names, medical records, or user IDs.
                                            Follow these rules strictly:
                                            1. Only mention metrics that are actually present in the data. A missing or null field means there is no data for it — never invent, estimate, or guess numbers.
                                            2. If device_sdk_available is false or no health metrics are present at all, briefly say that no health data is available yet and suggest enabling Health Connect permissions. Do not fabricate activity, sleep, or weight stats.
                                            3. Start with the most notable real highlight from their actual data.
                                            4. Address the user in second person ("you", "your") in a neutral, professional tone. No jokes, slang, exclamation marks or emojis.
                                            5. Be direct and specific: state the figures and what they mean, then give one practical recommendation. Avoid filler, timestamps, or raw machine data representations.
                                            6. Keep the entire response concise: 2 to 3 short sentences.
                                            7. Return ONLY the summary message text, with no markdown headers or bullet points.
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
                    .uri(activeBaseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBodyJson)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new BadRequestException("Empty response received from " + providerName + " AI service.");
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
                                String text = contentElem.getAsString().trim();
                                // Guard against raw AI safety/moderation refusal messages leaking to users
                                String lower = text.toLowerCase();
                                if (lower.contains("user safety:") || lower.contains("safety category") || lower.contains("pii/privacy")) {
                                    log.warn("Upstream AI model returned a safety refusal: {}", text);
                                    return "Your summary is unavailable right now. Log more activity in Health Connect to get detailed insights.";
                                }
                                return text;
                            }
                        }
                    }
                }
            }

            log.error("Unexpected {} response format: {}", providerName, responseBody);
            throw new BadRequestException("Failed to parse completion message from AI response.");

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling {} AI API: {}", providerName, e.getMessage(), e);
            throw new BadRequestException("AI summarization failed: " + e.getMessage());
        }
    }

    private boolean isValidApiKey(String key) {
        if (key == null || key.isBlank()) return false;
        String trimmed = key.trim();
        return !trimmed.contains("\n") && !trimmed.contains("\r") && !trimmed.contains(" ") && trimmed.length() > 10;
    }
}
