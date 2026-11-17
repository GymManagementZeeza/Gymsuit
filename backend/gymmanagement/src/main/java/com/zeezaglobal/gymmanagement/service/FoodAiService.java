package com.zeezaglobal.gymmanagement.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zeezaglobal.gymmanagement.dto.FoodIntakeSyncRequest;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns a free-text description of what someone ate ("2 idlis and sambar for
 * breakfast, chicken biryani for lunch") into structured food-log entries with
 * estimated nutrition, using the same OpenAI chat-completions setup as
 * {@link FoodRecognitionService} and {@link WorkoutAiService}.
 */
@Service
@Slf4j
public class FoodAiService {

    private static final int MAX_ENTRIES = 30;
    private static final Set<String> MEALS = Set.of("BREAKFAST", "LUNCH", "DINNER", "SNACK");

    @Value("${app.openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${app.openai.model:${OPENAI_MODEL:gpt-4o-mini}}")
    private String openaiModel;

    @Value("${app.openai.base-url:${OPENAI_BASE_URL:https://api.openai.com/v1}}")
    private String openaiBaseUrl;

    private final RestClient restClient = RestClient.builder().build();
    private final Gson gson = new Gson();

    public List<FoodIntakeSyncRequest.Entry> parse(String text, LocalDate date) {
        if (openaiApiKey == null || openaiApiKey.isBlank() || openaiApiKey.trim().length() <= 10) {
            throw new BadRequestException("AI meal logging is not configured (missing OPENAI_API_KEY).");
        }
        String model = (openaiModel != null && !openaiModel.isBlank()) ? openaiModel.trim() : "gpt-4o-mini";
        String baseUrl = (openaiBaseUrl != null && !openaiBaseUrl.isBlank())
                ? openaiBaseUrl.trim() : "https://api.openai.com/v1";

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", text.trim())),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 2000,
                "temperature", 0.1
        );

        String body;
        try {
            body = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gson.toJson(payload))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Food AI request failed: {}", e.getMessage());
            throw new BadRequestException("Couldn't reach the AI service. Please try again.");
        }
        return toEntries(body, date);
    }

    private List<FoodIntakeSyncRequest.Entry> toEntries(String responseBody, LocalDate date) {
        JsonObject parsed;
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```(?:json)?", "").replaceAll("```$", "").trim();
            }
            parsed = gson.fromJson(content, JsonObject.class);
        } catch (Exception e) {
            throw new BadRequestException("The AI returned an unreadable answer. Please try again.");
        }
        JsonArray foods = parsed != null && parsed.has("foods") && parsed.get("foods").isJsonArray()
                ? parsed.getAsJsonArray("foods") : new JsonArray();

        List<FoodIntakeSyncRequest.Entry> entries = new ArrayList<>();
        for (JsonElement el : foods) {
            if (!el.isJsonObject() || entries.size() >= MAX_ENTRIES) continue;
            JsonObject f = el.getAsJsonObject();
            String dish = str(f, "dish");
            if (dish == null || dish.isBlank()) continue;
            String meal = str(f, "meal");
            meal = meal != null && MEALS.contains(meal.trim().toUpperCase()) ? meal.trim().toUpperCase() : "SNACK";
            entries.add(new FoodIntakeSyncRequest.Entry(
                    "ai-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                    date,
                    meal,
                    dish.trim(),
                    (int) Math.round(Math.max(num(f, "calories"), 0)),
                    Math.max(num(f, "proteinG"), 0),
                    Math.max(num(f, "carbsG"), 0),
                    Math.max(num(f, "fatG"), 0),
                    null));
        }
        if (entries.isEmpty()) {
            throw new BadRequestException("I couldn't find any food in that. Try e.g. \"2 idlis with sambar for breakfast and chicken biryani for lunch\".");
        }
        return entries;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : null;
    }

    private static double num(JsonObject o, String k) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static final String SYSTEM_PROMPT = """
            You turn a person's description of what they ate into structured food-log entries for a
            nutrition app. Respond ONLY with JSON, no markdown:
            {"foods":[{
              "meal": "BREAKFAST",   // BREAKFAST, LUNCH, DINNER or SNACK. Use the meal the person names; if none is named infer from the dish/time words, else SNACK
              "dish": "Idli with Sambar (2 idlis)", // natural title case, include the quantity eaten in brackets when stated
              "calories": 220,       // kcal for the quantity eaten
              "proteinG": 7, "carbsG": 40, "fatG": 3
            }]}
            Rules: one entry per distinct dish or drink. Estimate realistic nutrition for the stated quantity
            (typical Indian/Western portion sizes if no quantity is given). Do not invent food that was not mentioned.
            If nothing edible is described, return {"foods":[]}.
            """;
}
