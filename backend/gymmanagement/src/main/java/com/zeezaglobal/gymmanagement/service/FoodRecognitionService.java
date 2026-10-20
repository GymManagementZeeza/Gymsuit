package com.zeezaglobal.gymmanagement.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zeezaglobal.gymmanagement.dto.FoodRecognitionResponse;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes uploaded food photos using OpenAI Vision to identify the dish and
 * estimate nutritional values (calories, protein, carbs, fat, serving size).
 */
@Service
@Slf4j
public class FoodRecognitionService {

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

    public FoodRecognitionService() {
        this.gson = new Gson();
        this.restClient = RestClient.builder().build();
    }

    /*
     * =========================================================================
     * Legacy Python Inference Service Code (Commented Out)
     * =========================================================================
     *
     * private final RestTemplate restTemplate;
     * private final String inferenceUrl;
     *
     * public FoodRecognitionService(
     *         @Value("${app.food-recognition.url:http://localhost:8001/predict}") String inferenceUrl) {
     *     this.inferenceUrl = inferenceUrl;
     *     SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
     *     factory.setConnectTimeout(3000);
     *     factory.setReadTimeout(15000);
     *     this.restTemplate = new RestTemplate(factory);
     * }
     *
     * public FoodRecognitionResponse recognizeLegacy(MultipartFile image) throws IOException {
     *     ByteArrayResource resource = new ByteArrayResource(image.getBytes()) {
     *         @Override
     *         public String getFilename() {
     *             return image.getOriginalFilename() != null ? image.getOriginalFilename() : "dish.jpg";
     *         }
     *     };
     *     MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
     *     body.add("image", resource);
     *
     *     HttpHeaders headers = new HttpHeaders();
     *     headers.setContentType(MediaType.MULTIPART_FORM_DATA);
     *     HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
     *
     *     try {
     *         FoodRecognitionResponse response =
     *                 restTemplate.postForObject(inferenceUrl, request, FoodRecognitionResponse.class);
     *         if (response == null) {
     *             throw new FoodRecognitionUnavailableException("Inference service returned no prediction.");
     *         }
     *         return response;
     *     } catch (RestClientException e) {
     *         log.warn("Food recognition service unreachable at {}: {}", inferenceUrl, e.getMessage());
     *         throw new FoodRecognitionUnavailableException(
     *                 "Food recognition service is not running. Start it with: "
     *                         + "uvicorn inference_server:app --port 8001 (from FoodImageTraining/)");
     *     }
     * }
     * =========================================================================
     */

    public FoodRecognitionResponse recognize(MultipartFile image) throws IOException {
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
            throw new BadRequestException("AI food recognition service is not configured with a valid API key. Please check the OPENAI_API_KEY credential in Jenkins.");
        }

        byte[] imageBytes = image.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String contentType = (image.getContentType() != null && !image.getContentType().isBlank())
                ? image.getContentType() : "image/jpeg";
        String dataUri = "data:" + contentType + ";base64," + base64Image;

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", "Identify the food item or dish in this image and estimate the single-serving nutrition values (calories, protein, carbs, fat, and typical serving description)."
        );

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of(
                        "url", dataUri,
                        "detail", "low"
                )
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent)
        );

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", """
                        You are an expert food nutrition analyzer for a mobile fitness app.
                        Analyze the food photo provided.
                        Identify the dish name in natural title case (e.g. "Chicken Biryani", "Dosa", "Kerala Porotta", "Black Coffee", "Masala Chai", "Egg Roast").
                        Estimate typical single-serving nutritional values realistically.
                        Respond ONLY with a JSON object in this format:
                        {
                          "dish": "Primary Dish Name",
                          "confidence": 0.95,
                          "calories": 300,
                          "proteinG": 8,
                          "carbsG": 40,
                          "fatG": 12,
                          "serving": "1 plate",
                          "candidates": [
                            {"dish": "Primary Dish Name", "confidence": 0.95},
                            {"dish": "Alternative Possible Dish", "confidence": 0.65}
                          ]
                        }
                        Output valid JSON only with no markdown formatting or backticks.
                        """
        );

        Map<String, Object> requestPayload = Map.of(
                "model", activeModel,
                "messages", List.of(systemMessage, userMessage),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 600
        );

        String requestBodyJson = gson.toJson(requestPayload);

        try {
            log.info("Analyzing food photo ({} bytes) using {} vision model ({})",
                    imageBytes.length, providerName, activeModel);

            String responseBody = restClient.post()
                    .uri(activeBaseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBodyJson)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new BadRequestException("Empty response received from " + providerName + " vision API.");
            }

            return parseAiFoodResponse(responseBody);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error analyzing food image with {} API: {}", providerName, e.getMessage(), e);
            throw new BadRequestException("Food recognition failed: " + e.getMessage());
        }
    }

    private FoodRecognitionResponse parseAiFoodResponse(String responseBody) {
        JsonObject root = gson.fromJson(responseBody, JsonObject.class);
        if (root == null || !root.has("choices")) {
            throw new BadRequestException("Invalid response format from AI vision service.");
        }
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BadRequestException("No choices returned from AI vision service.");
        }
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new BadRequestException("No message content in AI vision response.");
        }
        String content = message.get("content").getAsString().trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();

        JsonObject foodObj = gson.fromJson(content, JsonObject.class);
        String dish = foodObj.has("dish") ? foodObj.get("dish").getAsString().trim() : "Unknown Dish";
        double confidence = foodObj.has("confidence") ? foodObj.get("confidence").getAsDouble() : 0.90;
        int calories = foodObj.has("calories") ? foodObj.get("calories").getAsInt() : 0;
        int proteinG = foodObj.has("proteinG") ? foodObj.get("proteinG").getAsInt() : 0;
        int carbsG = foodObj.has("carbsG") ? foodObj.get("carbsG").getAsInt() : 0;
        int fatG = foodObj.has("fatG") ? foodObj.get("fatG").getAsInt() : 0;
        String serving = foodObj.has("serving") ? foodObj.get("serving").getAsString().trim() : "1 serving";

        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put(dish, confidence);

        if (foodObj.has("candidates")) {
            JsonArray candidates = foodObj.getAsJsonArray("candidates");
            for (JsonElement elem : candidates) {
                if (elem.isJsonObject()) {
                    JsonObject c = elem.getAsJsonObject();
                    if (c.has("dish")) {
                        String cDish = c.get("dish").getAsString().trim();
                        double cConf = c.has("confidence") ? c.get("confidence").getAsDouble() : 0.50;
                        probabilities.putIfAbsent(cDish, cConf);
                    }
                }
            }
        }

        FoodRecognitionResponse.NutritionInfo nutrition = new FoodRecognitionResponse.NutritionInfo(
                dish, serving, calories, proteinG, carbsG, fatG
        );

        log.info("Recognized dish: '{}' ({} kcal, {}P, {}C, {}F, serving='{}')",
                dish, calories, proteinG, carbsG, fatG, serving);

        return new FoodRecognitionResponse(dish, confidence, probabilities, nutrition);
    }

    private boolean isValidApiKey(String key) {
        if (key == null || key.isBlank()) return false;
        String trimmed = key.trim();
        return !trimmed.contains("\n") && !trimmed.contains("\r") && !trimmed.contains(" ") && trimmed.length() > 10;
    }

    /** Retained for backwards compatibility with controller exception handling. */
    public static class FoodRecognitionUnavailableException extends RuntimeException {
        public FoodRecognitionUnavailableException(String message) {
            super(message);
        }
    }
}
