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
 * On-device wellness summary engine.
 * Generates concise, factual summaries without
 * any network connection or heavy model downloads. Never fabricates missing metrics.
 */
class OnDeviceRuleInsightEngine : LocalAiEngine {

    companion object {
        private const val TAG = "LocalAiEngine"
    }

    override suspend fun generateSummary(request: AiSummarizeRequest): String = withContext(Dispatchers.Default) {
        if (!request.deviceSdkAvailable) {
            return@withContext "Health Connect is not connected. Grant permission to see your activity, sleep and heart rate summary."
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
            return@withContext "No health data has been recorded today. Check that Health Connect permissions are enabled."
        }

        // --- RULE 1: STEPS ---
        if (steps != null && prevSteps != null && prevSteps > 0) {
            val stepDelta = steps - prevSteps
            val stepPercentChange = (stepDelta.toDouble() / prevSteps.toDouble()) * 100.0

            if (stepPercentChange >= 75.0 && stepDelta >= 3500) {
                sentences.add("Steps increased ${stepPercentChange.toInt()}% to ${"%,d".format(steps)}, up from ${"%,d".format(prevSteps)} yesterday.")
            } else if (stepPercentChange <= -50.0 && prevSteps >= 7000) {
                sentences.add("Steps fell ${Math.abs(stepPercentChange).toInt()}% to ${"%,d".format(steps)}, down from ${"%,d".format(prevSteps)} yesterday.")
            }
        } else if (steps != null) {
            if (steps >= 14000) {
                sentences.add("You recorded ${"%,d".format(steps)} steps, a high activity level.")
            } else if (steps in 1..900) {
                sentences.add("Only ${"%,d".format(steps)} steps recorded so far today.")
            }
        }

        // --- RULE 2: SLEEP ---
        if (sleepMinutes != null && prevSleepMinutes != null && prevSleepMinutes > 0) {
            val sleepDelta = sleepMinutes - prevSleepMinutes
            val sleepDeltaHours = Math.abs(sleepDelta) / 60.0
            val formatted = sleepFormatted ?: "${sleepMinutes / 60}h ${sleepMinutes % 60}m"

            if (sleepDelta <= -150) {
                sentences.add("Sleep was $formatted, ${String.format("%.1f", sleepDeltaHours)} hours less than yesterday. Prioritise rest tonight.")
            } else if (sleepDelta >= 180) {
                sentences.add("Sleep was $formatted, ${String.format("%.1f", sleepDeltaHours)} hours more than yesterday.")
            }
        } else if (sleepMinutes != null) {
            val formatted = sleepFormatted ?: "${sleepMinutes / 60}h ${sleepMinutes % 60}m"
            if (sleepMinutes < 300) {
                sentences.add("Sleep was only $formatted, below the recommended 7 to 9 hours.")
            } else if (sleepMinutes >= 630) {
                sentences.add("Sleep was $formatted, longer than the typical 7 to 9 hours.")
            }
        }

        // --- RULE 3: ACTIVE CALORIES ---
        if (calories != null && prevCalories != null && prevCalories > 0) {
            val calDelta = calories - prevCalories
            val calPercentChange = (calDelta / prevCalories) * 100.0

            if (calPercentChange >= 80.0 && calDelta >= 300.0) {
                sentences.add("Active calories rose ${calPercentChange.toInt()}% to ${calories.toInt()} kcal, from ${prevCalories.toInt()} kcal yesterday.")
            } else if (calPercentChange <= -60.0 && prevCalories >= 500.0) {
                sentences.add("Active calories fell ${Math.abs(calPercentChange).toInt()}% compared with yesterday.")
            }
        } else if (calories != null && calories >= 750) {
            sentences.add("Active calories reached ${calories.toInt()} kcal today.")
        }

        // --- RULE 4: WORKOUT SESSIONS ---
        if (exercises.isNotEmpty()) {
            val workout = exercises.firstOrNull()
            val workoutTitle = workout?.title?.ifBlank { "workout" } ?: "workout"
            if (sentences.isEmpty()) {
                sentences.add("You completed a $workoutTitle session today.")
            }
        }

        if (sentences.isEmpty()) {
            sentences.add("Your activity, sleep and heart rate are steady with no notable changes today.")
        }

        sentences.joinToString(" ")
    }
}

/**
 * Hybrid Local AI Engine.
 * Attempts to run local quantized weights (e.g. gemma-2b, phi-2) if an on-device
 * model file is detected in app storage.
 * Gracefully defaults to the OnDeviceRuleInsightEngine otherwise.
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

        Log.d(TAG, "Using on-device rule insight engine (100% local, sudden-change detection)")
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
            You are a professional health and fitness analyst writing a brief summary for the user, in second person ("you").
            Rules:
            1. State only metrics that changed notably (sudden rise or drop in steps, sleep or calories). Ignore ordinary numbers.
            2. Use a neutral, professional tone. No jokes, slang, exclamation marks or emojis.
            3. Never invent numbers.
            4. Keep it concise: 2-3 short sentences, ending with one practical recommendation. No markdown headers or bullet points.
            Input data:
            ${request.healthData.toJsonObject()}
        """.trimIndent()
    }
}
