package com.zeezaglobal.gymmanagement.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Serves the bundled exercise GIFs (own dataset, in
 * src/main/resources/exercise-gifs/) on demand so the mobile apps don't
 * have to bundle them. Public — GIFs are static demo content with no
 * user data, and the apps load them in a WebView without auth headers.
 *
 * To add a GIF: drop the file in src/main/resources/exercise-gifs/ and
 * add one line to the name -> file map in the Android/iOS ExerciseMedia.
 */
@RestController
@RequestMapping("/api/exercise-gifs")
public class ExerciseGifController {

    private static final Pattern FILENAME = Pattern.compile("[a-z0-9-]+\\.gif");

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getGif(@PathVariable String filename) throws IOException {
        if (!FILENAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }
        ClassPathResource resource = new ClassPathResource("exercise-gifs/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = resource.getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(bytes);
    }
}
