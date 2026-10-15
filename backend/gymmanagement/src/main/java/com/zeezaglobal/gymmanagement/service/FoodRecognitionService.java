package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.FoodRecognitionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Forwards a dish photo to the Python food-recognition service
 * (FoodImageTraining/inference_server.py) and returns its prediction.
 *
 * The model lives in Python (PyTorch), not in this JVM — the Java backend is
 * a thin proxy so training and serving stay in one place. If the Python
 * service is down, callers get a 503 telling them to start it.
 */
@Service
@Slf4j
public class FoodRecognitionService {

    private final RestTemplate restTemplate;
    private final String inferenceUrl;

    public FoodRecognitionService(
            @Value("${app.food-recognition.url:http://localhost:8001/predict}") String inferenceUrl) {
        this.inferenceUrl = inferenceUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public FoodRecognitionResponse recognize(MultipartFile image) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return image.getOriginalFilename() != null
                        ? image.getOriginalFilename() : "dish.jpg";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            FoodRecognitionResponse response =
                    restTemplate.postForObject(inferenceUrl, request, FoodRecognitionResponse.class);
            if (response == null) {
                throw new FoodRecognitionUnavailableException("Inference service returned no prediction.");
            }
            return response;
        } catch (RestClientException e) {
            log.warn("Food recognition service unreachable at {}: {}", inferenceUrl, e.getMessage());
            throw new FoodRecognitionUnavailableException(
                    "Food recognition service is not running. Start it with: "
                            + "uvicorn inference_server:app --port 8001 (from FoodImageTraining/)");
        }
    }

    /** Thrown when the Python inference service cannot be reached. */
    public static class FoodRecognitionUnavailableException extends RuntimeException {
        public FoodRecognitionUnavailableException(String message) {
            super(message);
        }
    }
}
