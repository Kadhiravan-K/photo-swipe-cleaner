package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun getBestShotSelection(bitmaps: List<Bitmap>): BestShotResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not set or is placeholder")
            return null
        }

        try {
            val partsArray = JSONArray()

            bitmaps.forEachIndexed { index, bitmap ->
                val imagePart = JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", bitmap.toBase64())
                    })
                }
                partsArray.put(imagePart)
            }

            val promptPart = JSONObject().apply {
                put("text", """
                    You are a professional photographer assistant.
                    Above are ${bitmaps.size} similar photos. Analyze their composition, sharpness, lighting, overexposure, and blur, and select the BEST shot.
                    Recommend deleting the others.
                    Output your response STRICTLY as a JSON object with this format, do not include markdown blocks:
                    {
                      "bestIndex": 0,
                      "explanation": "Brief explanation why this shot is selected as the best and why others should be deleted."
                    }
                """.trimIndent())
            }
            partsArray.put(promptPart)

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", partsArray)
                }))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.4)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API request failed: ${response.code} ${response.message}")
                    return null
                }

                val responseBodyStr = response.body?.string() ?: return null
                val responseJson = JSONObject(responseBodyStr)
                
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanJsonStr = textResponse.replace("```json", "").replace("```", "").trim()
                val parsedResult = JSONObject(cleanJsonStr)

                return BestShotResponse(
                    bestIndex = parsedResult.optInt("bestIndex", 0),
                    explanation = parsedResult.optString("explanation", "Selected by AI analysis")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getBestShotSelection", e)
            return null
        }
    }

    suspend fun getDetailedImageAnalysis(bitmap: Bitmap): GeminiImageDetails? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not set or is placeholder")
            return null
        }

        try {
            val partsArray = JSONArray()

            val imagePart = JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", bitmap.toBase64())
                })
            }
            partsArray.put(imagePart)

            val promptPart = JSONObject().apply {
                put("text", """
                    Analyze this photo carefully. Perform two key tasks:
                    1. Face Recognition: Identify and count any human faces in the image. If faces of specific famous people or common names are recognizable, guess/detail them. Otherwise, list description-based labels for each unique face (e.g. "baby boy", "smiling woman with brown hair", "man with glasses").
                    2. Document / Text analysis: Handwrite or print OCR extraction of any text, tags, watermarks, receipts, signs, or text-heavy elements visible in the graphic.
                    
                    Output your response STRICTLY as a JSON object with this format, do not include markdown blocks:
                    {
                      "facesCount": 0,
                      "faceNames": "comma-separated list of recognized individuals or face descriptions",
                      "extractedText": "all readable words and phrases detected in the image"
                    }
                """.trimIndent())
            }
            partsArray.put(promptPart)

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", partsArray)
                }))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini image analysis API request failed: ${response.code}")
                    return null
                }

                val responseBodyStr = response.body?.string() ?: return null
                val responseJson = JSONObject(responseBodyStr)
                
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanJsonStr = textResponse.replace("```json", "").replace("```", "").trim()
                val parsedResult = JSONObject(cleanJsonStr)

                return GeminiImageDetails(
                    facesCount = parsedResult.optInt("facesCount", 0),
                    faceNames = parsedResult.optString("faceNames", ""),
                    extractedText = parsedResult.optString("extractedText", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getDetailedImageAnalysis", e)
            return null
        }
    }

    suspend fun getMemoryHighlights(
        monthName: String,
        statsText: String
    ): MemoryHighlightsResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        try {
            val partsArray = JSONArray()
            val promptPart = JSONObject().apply {
                put("text", """
                    You are a personalized gallery assistant.
                    Here are the photo gallery statistical metrics for $monthName:
                    $statsText
                    
                    Generate a fun, engaging, and brief monthly summary. Formulate the average photographed topic/tag, storage trend, and clean up advice based on the metrics.
                    Your summary must be personal and warm.
                    Output your response STRICTLY as a JSON object with this format, do not include markdown blocks:
                    {
                      "summary": "Brief 3-4 sentence paragraph summarizing the highlights of this month, e.g., 'You spent a lot of time capturing outdoor moments...'",
                      "mostPhotographed": "Short phrase identifying the main subject / active category",
                      "storageTrend": "Brief estimation of space savings if blurry photo suggestions are resolved"
                    }
                """.trimIndent())
            }
            partsArray.put(promptPart)

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", partsArray)
                }))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.7)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBodyStr = response.body?.string() ?: return null
                val responseJson = JSONObject(responseBodyStr)
                
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanJsonStr = textResponse.replace("```json", "").replace("```", "").trim()
                val parsedResult = JSONObject(cleanJsonStr)

                return MemoryHighlightsResponse(
                    summary = parsedResult.optString("summary", ""),
                    mostPhotographed = parsedResult.optString("mostPhotographed", ""),
                    storageTrend = parsedResult.optString("storageTrend", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getMemoryHighlights", e)
            return null
        }
    }

    suspend fun getCleanupTips(statsText: String): List<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return emptyList()
        }

        try {
            val partsArray = JSONArray()
            val promptPart = JSONObject().apply {
                put("text", """
                    Here are some gallery statistics for a user:
                    $statsText
                    
                    Generate 3 smart, actionable gallery clean-up suggestions based on these fields (e.g., recommend removing specific screenshots or dark downloads).
                    Output your response STRICTLY as a JSON array of strings, do not include markdown blocks:
                    [
                      "Tip 1...",
                      "Tip 2...",
                      "Tip 3..."
                    ]
                """.trimIndent())
            }
            partsArray.put(promptPart)

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", partsArray)
                }))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.5)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val responseStr = response.body?.string() ?: return emptyList()
                val responseJson = JSONObject(responseStr)
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanJsonStr = textResponse.replace("```json", "").replace("```", "").trim()
                val arr = JSONArray(cleanJsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating cleanup tips", e)
            return emptyList()
        }
    }
}

data class BestShotResponse(
    val bestIndex: Int,
    val explanation: String
)

data class MemoryHighlightsResponse(
    val summary: String,
    val mostPhotographed: String,
    val storageTrend: String
)

data class GeminiImageDetails(
    val facesCount: Int,
    val faceNames: String,
    val extractedText: String
)
