package ca.zeezaglobal.gymsuitapp.data.food

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.exp

data class FoodPrediction(val classId: String, val probability: Float)

/**
 * On-device Food-101 classifier (Swin transformer, ONNX Runtime). Runs fully offline.
 * Model: onnx-community/swin-finetuned-food101-ONNX, 8-bit quantized (Apache-2.0), 101 Food-101 classes.
 */
class FoodClassifier(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession? by lazy {
        try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            env.createSession(bytes, OrtSession.SessionOptions())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load food model", e)
            null
        }
    }

    suspend fun classify(bitmap: Bitmap, topK: Int = 3): List<FoodPrediction> = withContext(Dispatchers.Default) {
        val s = session ?: return@withContext emptyList()
        try {
            val input = preprocess(bitmap)
            val inputName = s.inputNames.first()
            OnnxTensor.createTensor(env, FloatBuffer.wrap(input), longArrayOf(1, 3, SIZE.toLong(), SIZE.toLong())).use { tensor ->
                s.run(mapOf(inputName to tensor)).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    val logits = (result[0].value as Array<FloatArray>)[0]
                    val probs = softmax(logits)
                    probs.withIndex()
                        .sortedByDescending { it.value }
                        .take(topK)
                        .map { FoodPrediction(FoodNutrition.CLASSES[it.index], it.value) }
                        .also { Log.d(TAG, "predictions=$it") }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed", e)
            emptyList()
        }
    }

    /** Resize to 224x224 (as the model's preprocessor config specifies), ImageNet-normalise, channels-first. */
    private fun preprocess(src: Bitmap): FloatArray {
        val cropped = Bitmap.createScaledBitmap(src, SIZE, SIZE, true)
        val pixels = IntArray(SIZE * SIZE)
        cropped.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        val out = FloatArray(3 * SIZE * SIZE)
        for (i in pixels.indices) {
            val p = pixels[i]
            out[i] = (((p shr 16) and 0xFF) / 255f - MEAN[0]) / STD[0]
            out[SIZE * SIZE + i] = (((p shr 8) and 0xFF) / 255f - MEAN[1]) / STD[1]
            out[2 * SIZE * SIZE + i] = ((p and 0xFF) / 255f - MEAN[2]) / STD[2]
        }
        return out
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = FloatArray(logits.size) { exp(logits[it] - max) }
        val sum = exps.sum()
        return FloatArray(exps.size) { exps[it] / sum }
    }

    companion object {
        private const val TAG = "FoodClassifier"
        private const val MODEL_ASSET = "food101_swin_int8.onnx"
        private const val SIZE = 224
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
