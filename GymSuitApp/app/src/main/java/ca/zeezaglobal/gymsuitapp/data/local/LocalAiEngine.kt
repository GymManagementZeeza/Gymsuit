package ca.zeezaglobal.gymsuitapp.data.local

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.model.AiSummarizeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface LocalAiEngine {
    suspend fun generateSummary(request: AiSummarizeRequest): String
}

/**
 * High-quality on-device wellness coach engine.
 * Generates natural, conversational, personalized coaching summaries without
 * any network connection or heavy model downloads. Never fabricates missing metrics.
 */
class OnDeviceRuleInsightEngine : LocalAiEngine {

    companion object {
        private const val TAG = "LocalAiEngine"
    }

    override suspend fun generateSummary(request: AiSummarizeRequest): String = withContext(Dispatchers.Default) {
        if (!request.deviceSdkAvailable) {
            return@withContext "Health Connect isn't connected yet! We can't spy on your workout heroics (or your afternoon couch hibernation) until you grant permission."
        }

        val data = request.healthData
        val sentences = mutableListOf<String>()

        val steps = data.todaySteps
        val prevSteps = data.previousSteps

        val calories = data.todayActiveCaloriesKcal
        val prevCalories = data.previousActiveCaloriesKcal

        val sleepMinutes = data.latestSleepMinutes
        val prevSleepMinutes = data.previousSleepMinutes
        val sleepFormatted = data.latestSleepFormatted

        val heartRate = data.latestHeartRateBpm
        val exercises = data.exerciseSessions

        val hasAnyData = steps != null || calories != null || sleepMinutes != null || heartRate != null || exercises.isNotEmpty()

        if (!hasAnyData) {
            return@withContext "Zero data logged for today! Either you discovered teleportation, or you've perfected the ancient art of becoming a statue. Go move a muscle!"
        }

        // --- RULE 1: SUDDEN CHANGE IN STEPS ---
        if (steps != null && prevSteps != null && prevSteps > 0) {
            val stepDelta = steps - prevSteps
            val stepPercentChange = (stepDelta.toDouble() / prevSteps.toDouble()) * 100.0

            if (stepPercentChange >= 75.0 && stepDelta >= 3500) {
                // Massive surge in steps
                sentences.add("Whoa, someone set your sneakers on fire! You leaped from ${"%,d".format(prevSteps)} to ${"%,d".format(steps)} steps today (+${stepPercentChange.toInt()}% surge). Were you running away from responsibilities or chasing down the ice cream truck?")
            } else if (stepPercentChange <= -50.0 && prevSteps >= 7000) {
                // Sudden step cliff
                sentences.add("Your step count took a hilarious nose-dive from ${"%,d".format(prevSteps)} yesterday down to ${"%,d".format(steps)} today (-${Math.abs(stepPercentChange).toInt()}% drop). Did your couch develop gravitational pull?")
            }
        } else if (steps != null) {
            // Absolute extremes if no previous day baseline exists
            if (steps >= 14000) {
                sentences.add("You clocked a wild ${"%,d".format(steps)} steps! Are you training for an ultramarathon or did you lose your car keys in a corn maze?")
            } else if (steps in 1..900) {
                sentences.add("You've only clocked ${"%,d".format(steps)} steps today. Even a three-toed sloth is looking at your step tracker with judgment.")
            }
        }

        // --- RULE 2: SUDDEN CHANGE IN SLEEP ---
        if (sleepMinutes != null && prevSleepMinutes != null && prevSleepMinutes > 0) {
            val sleepDelta = sleepMinutes - prevSleepMinutes
            val sleepDeltaHours = Math.abs(sleepDelta) / 60.0

            if (sleepDelta <= -150) { // Lost 2.5+ hours of sleep suddenly
                val formatted = sleepFormatted ?: "${sleepMinutes / 60}h ${sleepMinutes % 60}m"
                sentences.add("Sleep alert: you dropped ${String.format("%.1f", sleepDeltaHours)} hours of sleep compared to yesterday, waking up after just $formatted. Fueled purely by iced coffee and chaotic energy today!")
            } else if (sleepDelta >= 180) { // Gained 3+ hours of sleep suddenly
                val formatted = sleepFormatted ?: "${sleepMinutes / 60}h ${sleepMinutes % 60}m"
                sentences.add("Rip Van Winkle award goes to you today! You slept $formatted (${String.format("%.1f", sleepDeltaHours)} hours longer than yesterday). Your bed must be thanking you for the thorough inspection.")
            }
        } else if (sleepMinutes != null) {
            if (sleepMinutes < 300) { // Under 5 hours
                sentences.add("A whopping ${sleepFormatted ?: "${sleepMinutes / 60}h"} of sleep logged. You're practically operating in zombie mode — please avoid operating heavy machinery and don't reply to risky texts.")
            } else if (sleepMinutes >= 630) { // Over 10.5 hours
                sentences.add("${sleepFormatted ?: "${sleepMinutes / 60}h"} of slumber recorded! That wasn't just a nap, you were in hibernation.")
            }
        }

        // --- RULE 3: SUDDEN CHANGE IN ACTIVE CALORIES / WORKOUT SURGE ---
        if (calories != null && prevCalories != null && prevCalories > 0) {
            val calDelta = calories - prevCalories
            val calPercentChange = (calDelta / prevCalories) * 100.0

            if (calPercentChange >= 80.0 && calDelta >= 300.0) {
                sentences.add("Calorie burn exploded by +${calPercentChange.toInt()}% today (${calories.toInt()} kcal vs ${prevCalories.toInt()} kcal yesterday). Absolute beast mode!")
            } else if (calPercentChange <= -60.0 && prevCalories >= 500.0) {
                sentences.add("Active calorie burn tanked by ${Math.abs(calPercentChange).toInt()}% compared to yesterday. Today was clearly dedicated to aggressive energy conservation.")
            }
        } else if (calories != null && calories >= 750) {
            sentences.add("Torched ${calories.toInt()} active calories today! You basically incinerated dinner before even eating it.")
        }

        // --- RULE 4: UNUSUAL WORKOUT SPIKES ---
        if (exercises.isNotEmpty()) {
            val workout = exercises.firstOrNull()
            val workoutTitle = workout?.title?.ifBlank { "sweat session" } ?: "sweat session"
            if (sentences.isEmpty()) {
                sentences.add("You decided to surprise your muscles with a surprise $workoutTitle today! Hopefully they forgive you by tomorrow morning.")
            }
        }

        // --- FALLBACK IF NO WILD METRIC SHIFTS HAPPENED ---
        // If everything was ordinary and stable, give a funny, self-aware roast about being steady
        if (sentences.isEmpty()) {
            val steadyJokes = listOf(
                "No wild plot twists in your metrics today — you were suspiciously consistent. Keep it up, steady Eddie!",
                "Your numbers today are so consistent you might actually be a well-calibrated robot. Keep rolling!",
                "No dramatic health drama or sudden marathons detected today. Just smooth, respectable, drama-free living.",
                "Smooth sailing across your metrics today. Neither lazy nor completely out of your mind — perfectly balanced, as all things should be."
            )
            val jokeIndex = ((steps ?: 0L) % steadyJokes.size).toInt()
            sentences.add(steadyJokes[jokeIndex])
        } else {
            // Add a punchy funny signoff
            val wittySignoffs = listOf(
                "Drink some water and stay legendary!",
                "Don't let your couch plot revenge tomorrow!",
                "Keep this energy up and tomorrow might just be legendary!",
                "Listen to your body (and maybe stretch before getting off that chair)!"
            )
            val signoffIndex = ((steps ?: 0L) % wittySignoffs.size).toInt()
            sentences.add(wittySignoffs[signoffIndex])
        }

        sentences.joinToString(" ")
    }
}

/**
 * Hybrid Local AI Engine.
 * Attempts to run local quantized weights (e.g. gemma-2b, phi-2) if an on-device
 * model file is detected in app storage.
 * Gracefully defaults to the witty OnDeviceRuleInsightEngine otherwise.
 */
class HybridLocalAiEngine(
    private val context: Context,
    private val fallbackEngine: LocalAiEngine = OnDeviceRuleInsightEngine()
) : LocalAiEngine {

    companion object {
        private const val TAG = "HybridLocalAiEngine"
        private val LOCAL_MODEL_CANDIDATES = listOf(
            "llm_model.bin",
            "gemma-2b-it-gpu-int4.bin",
            "gemma-2b-it-cpu-int4.bin",
            "gemma-2b-it-cpu-int8.bin"
        )
    }

    override suspend fun generateSummary(request: AiSummarizeRequest): String {
        val modelFile = findLocalModelFile()
        if (modelFile != null && modelFile.exists() && modelFile.length() > 10_000_000L) {
            Log.i(TAG, "Found local neural LLM model file at: ${modelFile.absolutePath} (${modelFile.length() / (1024 * 1024)} MB)")
            val neuralResult = runNeuralInferenceIfAvailable(modelFile, request)
            if (!neuralResult.isNullOrBlank()) {
                return neuralResult
            }
        }

        Log.d(TAG, "Using witty on-device rule insight engine (100% local, sudden-change detection)")
        return fallbackEngine.generateSummary(request)
    }

    private fun findLocalModelFile(): File? {
        val filesDir = context.filesDir
        for (candidate in LOCAL_MODEL_CANDIDATES) {
            val file = File(filesDir, candidate)
            if (file.exists() && file.isFile) {
                return file
            }
        }
        return null
    }

    private suspend fun runNeuralInferenceIfAvailable(modelFile: File, request: AiSummarizeRequest): String? {
        return try {
            val llmClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference")
            val optionsClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions")
            val builderClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions\$Builder")

            val builder = builderClass.getDeclaredConstructor(Context::class.java).newInstance(context)
            builderClass.getMethod("setModelPath", String::class.java).invoke(builder, modelFile.absolutePath)
            val options = builderClass.getMethod("build").invoke(builder)

            val createMethod = llmClass.getMethod("createFromOptions", Context::class.java, optionsClass)
            val llmInstance = createMethod.invoke(null, context, options)

            val prompt = buildPrompt(request)
            val generateMethod = llmClass.getMethod("generateResponse", String::class.java)
            val response = generateMethod.invoke(llmInstance, prompt) as? String
            response?.trim()
        } catch (e: Throwable) {
            Log.w(TAG, "MediaPipe LlmInference not instantiated or failed: ${e.message}. Falling back to rule engine.")
            null
        }
    }

    private fun buildPrompt(request: AiSummarizeRequest): String {
        return """
            You are a witty, hilarious personal fitness buddy talking directly to the user in second person ("you").
            Rules:
            1. ONLY highlight metrics that experienced a sudden change, surge, or drop (e.g. huge step jump/fall, sleep spike or crash, massive calorie burn). Ignore ordinary numbers.
            2. Be genuinely funny and roast them playfully with light humor (e.g. comparing sudden inactivity to becoming a statue or intense steps to running away from adulthood).
            3. Never invent numbers.
            4. Keep it concise: 2-3 snappy sentences. No markdown headers or bullet points.
            Input data:
            ${request.healthData.toJsonObject()}
        """.trimIndent()
    }
}
