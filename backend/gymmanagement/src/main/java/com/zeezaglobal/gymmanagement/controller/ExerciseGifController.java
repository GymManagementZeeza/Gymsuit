package com.zeezaglobal.gymmanagement.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Serves exercise demonstration GIFs and provides dynamic listing of all
 * available exercise GIFs so mobile clients automatically stay updated when
 * new GIFs are uploaded daily to the backend.
 */
@RestController
@RequestMapping("/api/exercise-gifs")
public class ExerciseGifController {

    private static final Pattern FILENAME = Pattern.compile("[a-z0-9-]+\\.gif");

    public record ExerciseDto(
            String id,
            String name,
            String body_part,
            String equipment,
            String primary_muscle,
            List<String> secondary_muscles,
            List<String> instructions,
            String gif,
            String img
    ) {}

    /**
     * Returns the dynamic list of all exercises with GIFs available on the backend.
     * Called whenever the mobile workout screen is loaded to reflect latest uploads.
     */
    @GetMapping
    public ResponseEntity<List<ExerciseDto>> listExercises() {
        List<ExerciseDto> list = getAllExerciseGifs();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(list);
    }

    /**
     * Serves the GIF binary stream on demand.
     */
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getGif(@PathVariable String filename) throws IOException {
        if (!FILENAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = findGifResource(filename);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = resource.getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(bytes);
    }

    private Resource findGifResource(String filename) {
        // 1. Check classpath
        ClassPathResource cp = new ClassPathResource("exercise-gifs/" + filename);
        if (cp.exists()) {
            return cp;
        }

        // 2. Check external dynamic upload directory if specified
        String customDir = System.getProperty("exercise.gifs.dir", System.getenv("EXERCISE_GIFS_DIR"));
        if (customDir != null && !customDir.isBlank()) {
            File f = new File(customDir, filename);
            if (f.exists() && f.isFile()) {
                return new FileSystemResource(f);
            }
        }
        return null;
    }

    private List<ExerciseDto> getAllExerciseGifs() {
        Set<String> filenames = new TreeSet<>();

        // 1. Scan classpath resources
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:exercise-gifs/*.gif");
            for (Resource r : resources) {
                String fn = r.getFilename();
                if (fn != null && FILENAME.matcher(fn).matches()) {
                    filenames.add(fn);
                }
            }
        } catch (Exception ignored) {}

        // 2. Scan external directory if configured
        try {
            String customDir = System.getProperty("exercise.gifs.dir", System.getenv("EXERCISE_GIFS_DIR"));
            if (customDir != null && !customDir.isBlank()) {
                File dir = new File(customDir);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> FILENAME.matcher(name).matches());
                    if (files != null) {
                        for (File f : files) {
                            filenames.add(f.getName());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Default baseline fallback if classpath scanning is restricted
        if (filenames.isEmpty()) {
            filenames.addAll(List.of(
                    "bench-press.gif",
                    "cable-lat-pulldown-full-range-of-motion.gif",
                    "cable-pulldown.gif",
                    "deadlift.gif",
                    "dumbbell-curl.gif",
                    "dumbbell-front-raise.gif",
                    "dumbbell-lateral-raise.gif",
                    "hammer-curl.gif",
                    "machine-chest-fly.gif",
                    "squat.gif",
                    "weighted-squat.gif"
            ));
        }

        List<ExerciseDto> result = new ArrayList<>();
        for (String fn : filenames) {
            result.add(buildExerciseDto(fn));
        }
        return result;
    }

    private ExerciseDto buildExerciseDto(String filename) {
        String base = filename.replace(".gif", "");
        switch (filename) {
            case "bench-press.gif":
                return new ExerciseDto("bench-press", "Barbell Bench Press", "chest", "barbell", "pectorals", List.of("triceps", "shoulders"), List.of("Lie flat on the bench.", "Grip barbell slightly wider than shoulder-width.", "Lower bar to chest and press up smoothly."), filename, "");
            case "machine-chest-fly.gif":
                return new ExerciseDto("machine-chest-fly", "Lever Seated Fly", "chest", "leverage machine", "pectorals", List.of("anterior deltoid"), List.of("Adjust seat so handles align with mid-chest.", "Bring arms together in an arc.", "Control release back to stretch."), filename, "");
            case "cable-pulldown.gif":
                return new ExerciseDto("cable-pulldown", "Cable Pulldown", "back", "cable", "latissimus dorsi", List.of("biceps", "rhomboids"), List.of("Grip wide bar above.", "Pull bar down towards upper chest.", "Squeeze shoulder blades and return slowly."), filename, "");
            case "cable-lat-pulldown-full-range-of-motion.gif":
                return new ExerciseDto("cable-lat-pulldown", "Cable Lat Pulldown Full Range Of Motion", "back", "cable", "latissimus dorsi", List.of("biceps", "rear deltoid"), List.of("Maintain upright posture.", "Pull down through full range of motion.", "Extend arms fully at top."), filename, "");
            case "deadlift.gif":
                return new ExerciseDto("deadlift", "Barbell Deadlift", "upper legs", "barbell", "glutes", List.of("hamstrings", "erector spinae"), List.of("Feet hip-width apart under the barbell.", "Hinge at hips, grip bar firmly.", "Drive through feet and stand tall."), filename, "");
            case "dumbbell-curl.gif":
                return new ExerciseDto("dumbbell-curl", "Dumbbell Biceps Curl", "upper arms", "dumbbell", "biceps", List.of("forearms"), List.of("Stand with dumbbells at sides.", "Curl weights upward keeping elbows fixed.", "Lower slowly to full extension."), filename, "");
            case "hammer-curl.gif":
                return new ExerciseDto("hammer-curl", "Dumbbell Hammer Curl", "upper arms", "dumbbell", "brachioradialis", List.of("biceps", "forearms"), List.of("Hold dumbbells with neutral palms-in grip.", "Curl dumbbells while keeping palms facing each other.", "Lower under control."), filename, "");
            case "dumbbell-front-raise.gif":
                return new ExerciseDto("dumbbell-front-raise", "Dumbbell Front Raise", "shoulders", "dumbbell", "anterior deltoids", List.of("upper chest"), List.of("Stand tall holding dumbbells on front thighs.", "Raise arms forward to shoulder level.", "Lower under control."), filename, "");
            case "dumbbell-lateral-raise.gif":
                return new ExerciseDto("dumbbell-lateral-raise", "Dumbbell Lateral Raise", "shoulders", "dumbbell", "lateral deltoids", List.of("trapezius"), List.of("Hold dumbbells at sides with slight elbow bend.", "Raise arms out to sides until parallel to floor.", "Lower slowly."), filename, "");
            case "weighted-squat.gif":
                return new ExerciseDto("weighted-squat", "Barbell Full Squat", "upper legs", "barbell", "quadriceps", List.of("glutes", "hamstrings"), List.of("Bar across upper traps, feet shoulder-width.", "Squat deep breaking parallel.", "Drive through heels back to top."), filename, "");
            case "squat.gif":
                return new ExerciseDto("squat", "Bodyweight Squat", "upper legs", "body weight", "quadriceps", List.of("glutes", "calves"), List.of("Stand with feet shoulder-width apart.", "Sit hips down and back.", "Stand up tall."), filename, "");
            default:
                // Intelligently infer metadata for daily newly uploaded GIFs
                String cleanName = Arrays.stream(base.split("-"))
                        .filter(s -> !s.isEmpty())
                        .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                        .collect(Collectors.joining(" "));

                String lower = base.toLowerCase();
                String bodyPart = "other";
                String muscle = "general";
                if (lower.contains("chest") || lower.contains("bench") || lower.contains("fly") || lower.contains("pushup")) {
                    bodyPart = "chest";
                    muscle = "pectorals";
                } else if (lower.contains("back") || lower.contains("pulldown") || lower.contains("row") || lower.contains("lat")) {
                    bodyPart = "back";
                    muscle = "latissimus dorsi";
                } else if (lower.contains("curl") || lower.contains("bicep") || lower.contains("tricep") || lower.contains("arm")) {
                    bodyPart = "upper arms";
                    muscle = lower.contains("tricep") ? "triceps" : "biceps";
                } else if (lower.contains("shoulder") || lower.contains("raise") || lower.contains("press") || lower.contains("deltoid")) {
                    bodyPart = "shoulders";
                    muscle = "deltoids";
                } else if (lower.contains("squat") || lower.contains("leg") || lower.contains("lunge") || lower.contains("calf")) {
                    bodyPart = "upper legs";
                    muscle = "quadriceps";
                } else if (lower.contains("ab") || lower.contains("crunch") || lower.contains("plank") || lower.contains("core")) {
                    bodyPart = "waist";
                    muscle = "abdominals";
                }

                String equipment = "body weight";
                if (lower.contains("barbell")) equipment = "barbell";
                else if (lower.contains("dumbbell")) equipment = "dumbbell";
                else if (lower.contains("cable")) equipment = "cable";
                else if (lower.contains("machine") || lower.contains("lever")) equipment = "machine";

                return new ExerciseDto(base, cleanName, bodyPart, equipment, muscle, List.of(), List.of("Perform exercise with proper form and control."), filename, "");
        }
    }
}
