package com.zeezaglobal.gymmanagement.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zeezaglobal.gymmanagement.dto.WorkoutSyncRequest;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a free-text description of a day's training ("bench press 3x10 at 40kg,
 * then a 5k run in 28 minutes") into structured workout entries using the same
 * OpenAI chat-completions setup as {@link FoodRecognitionService}. Calories are
 * left null so {@link WorkoutIntakeService} estimates them from the member's
 * weight, unless the member stated a figure.
 */
@Service
@Slf4j
public class WorkoutAiService {

    private static final int MAX_WORKOUTS = 20;

    @Value("${app.openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${app.openai.model:${OPENAI_MODEL:gpt-4o-mini}}")
    private String openaiModel;

    @Value("${app.openai.base-url:${OPENAI_BASE_URL:https://api.openai.com/v1}}")
    private String openaiBaseUrl;

    private final RestClient restClient = RestClient.builder().build();
    private final Gson gson = new Gson();

    public List<WorkoutSyncRequest.Entry> parse(String text, LocalDate date) {
        if (openaiApiKey == null || openaiApiKey.isBlank() || openaiApiKey.trim().length() <= 10) {
            throw new BadRequestException("AI workout logging is not configured (missing OPENAI_API_KEY).");
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
                "max_tokens", 1500,
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
            log.error("Workout AI request failed: {}", e.getMessage());
            throw new BadRequestException("Couldn't reach the AI service. Please try again.");
        }
        return toEntries(body, date);
    }

    private List<WorkoutSyncRequest.Entry> toEntries(String responseBody, LocalDate date) {
        String content;
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```(?:json)?", "").replaceAll("```$", "").trim();
            }
        } catch (Exception e) {
            throw new BadRequestException("The AI returned an unreadable answer. Please try again.");
        }

        JsonObject parsed;
        try {
            parsed = gson.fromJson(content, JsonObject.class);
        } catch (Exception e) {
            throw new BadRequestException("The AI returned an unreadable answer. Please try again.");
        }
        JsonArray workouts = parsed != null && parsed.has("workouts") && parsed.get("workouts").isJsonArray()
                ? parsed.getAsJsonArray("workouts") : new JsonArray();

        List<WorkoutSyncRequest.Entry> entries = new ArrayList<>();
        int index = 0;
        for (JsonElement el : workouts) {
            if (!el.isJsonObject() || entries.size() >= MAX_WORKOUTS) continue;
            JsonObject w = el.getAsJsonObject();
            String title = str(w, "title");
            if (title == null || title.isBlank()) continue;

            Integer setCount = null;
            Integer totalReps = null;
            Double volume = null;
            if (w.has("sets") && w.get("sets").isJsonArray() && !w.getAsJsonArray("sets").isEmpty()) {
                int sets = 0, reps = 0;
                double vol = 0;
                for (JsonElement s : w.getAsJsonArray("sets")) {
                    if (!s.isJsonObject()) continue;
                    int r = intVal(s.getAsJsonObject(), "reps");
                    double kg = num(s.getAsJsonObject(), "weightKg");
                    if (r <= 0) continue;
                    sets++;
                    reps += r;
                    vol += r * Math.max(kg, 0);
                }
                if (sets > 0) {
                    setCount = sets;
                    totalReps = reps;
                    volume = vol > 0 ? vol : null;
                }
            }

            Integer duration = w.has("durationMinutes") && !w.get("durationMinutes").isJsonNull()
                    ? Integer.valueOf(intVal(w, "durationMinutes")) : null;
            if (duration != null && duration <= 0) duration = null;
            if (duration == null && setCount != null) duration = Math.max(setCount * 3, 1);
            if (duration == null) duration = 30;

            String displayTitle = title.trim();
            double distance = num(w, "distanceKm");
            if (distance > 0 && setCount == null) {
                displayTitle += " · " + (distance % 1 == 0 ? String.valueOf((long) distance) : String.valueOf(distance)) + " km";
            }

            LocalTime time = LocalTime.NOON.plusMinutes(index);
            String t = str(w, "time");
            if (t != null) {
                try { time = LocalTime.parse(t); } catch (Exception ignored) { }
            }
            LocalDateTime start = LocalDateTime.of(date, time);
            double kcal = num(w, "caloriesBurned");

            entries.add(new WorkoutSyncRequest.Entry(
                    "ai-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                    displayTitle,
                    start,
                    start.plusMinutes(duration),
                    duration,
                    kcal > 0 ? kcal : null,
                    setCount,
                    totalReps,
                    volume,
                    str(w, "activityType"),
                    "AI"));
            index++;
        }
        if (entries.isEmpty()) {
            throw new BadRequestException("I couldn't find any workouts in that. Try e.g. \"bench press 3 sets of 10 at 40 kg and a 30 minute run\".");
        }
        return entries;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : null;
    }

    private static int intVal(JsonObject o, String k) {
        return (int) Math.round(num(o, k));
    }

    private static double num(JsonObject o, String k) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static final String SYSTEM_PROMPT = """
            You turn a person's description of the exercise they did into structured workout entries
            for a fitness app. Respond ONLY with JSON, no markdown:
            {"workouts":[{
              "title": "Bench Press",                // exercise or activity name, title case
              "activityType": "STRENGTH",            // STRENGTH, RUNNING, CYCLING, SWIMMING, WALKING, HIKING, ROWING, ELLIPTICAL, HIIT, YOGA, DANCE or OTHER
              "sets": [{"reps": 10, "weightKg": 40}],// strength only; one object PER SET (expand "3x10 at 40kg" into 3 objects). weightKg 0 for bodyweight. Convert lb to kg.
              "durationMinutes": 30,                 // for cardio/other; omit or null for strength if not stated
              "distanceKm": 5,                       // optional, convert miles to km
              "time": "18:30",                       // optional HH:mm 24h if the person said when, else null
              "caloriesBurned": null                 // only if the person stated it, else null
            }]}
            Rules: one entry per distinct exercise or activity. Never invent exercises, sets, weights or
            durations the person did not mention. If nothing workout-related is described, return {"workouts":[]}.
            """;
}
