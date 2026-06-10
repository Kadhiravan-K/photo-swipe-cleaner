package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.InputStream
import kotlin.math.abs

object ImageAnalysisEngine {
    private const val TAG = "ImageAnalysisEngine"

    data class LocalAnalysisResult(
        val blurScore: Float,
        val sharpnessScore: Float,
        val brightnessScore: Float,
        val duplicateHash: String,
        val screenshotProbability: Float
    )

    fun analyzeImage(context: Context, uri: Uri, filePath: String): LocalAnalysisResult? {
        var inputStream: InputStream? = null
        return try {
            val contentResolver = context.contentResolver
            
            // First decode with inJustDecodeBounds to get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                return null
            }

            // Downscale to 128x128 for general processing to save memory
            options.apply {
                inJustDecodeBounds = false
                // Calculate sample size to downscale efficiently
                inSampleSize = calculateInSampleSize(originalWidth, originalHeight, 128, 128)
            }
            inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) {
                return null
            }

            // Now perform analysis on this bitmap
            val brightness = calculateBrightness(bitmap)
            val sharpness = calculateSharpness(bitmap)
            val blur = (1.0f - sharpness).coerceIn(0.0f, 1.0f)
            
            // Build 8x8 average hash
            val dHash = buildAverageHash(bitmap)
            
            // Screenshot detection path check & aspect ratio check
            val isScreenshotInPath = filePath.lowercase().contains("screenshot") || 
                                     filePath.lowercase().contains("screen_shot")
            
            val aspectRatio = originalHeight.toFloat() / originalWidth.toFloat()
            val isPhoneScreenRatio = (aspectRatio >= 1.7f && aspectRatio <= 2.3f) || 
                                     ((1f / aspectRatio) >= 1.7f && (1f / aspectRatio) <= 2.3f)
            
            val screenshotProbability = when {
                isScreenshotInPath -> 1.0f
                isPhoneScreenRatio -> 0.7f
                else -> 0.1f
            }

            bitmap.recycle()

            LocalAnalysisResult(
                blurScore = blur,
                sharpnessScore = sharpness,
                brightnessScore = brightness,
                duplicateHash = dHash,
                screenshotProbability = screenshotProbability
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image $uri", e)
            null
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun calculateBrightness(bitmap: Bitmap): Float {
        var totalLuminance = 0.0
        val width = bitmap.width
        val height = bitmap.height
        val count = width * height
        val pixels = IntArray(count)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (color in pixels) {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val l = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            totalLuminance += l
        }
        return (totalLuminance / count).toFloat().coerceIn(0.0f, 1.0f)
    }

    private fun calculateSharpness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        var totalDiff = 0.0
        var pixelCount = 0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Horizontal differences
        for (y in 0 until height) {
            for (x in 0 until width - 1) {
                val idx1 = y * width + x
                val idx2 = y * width + (x + 1)
                
                val r1 = (pixels[idx1] shr 16) and 0xFF
                val g1 = (pixels[idx1] shr 8) and 0xFF
                val b1 = pixels[idx1] and 0xFF
                val l1 = 0.2126 * r1 + 0.7152 * g1 + 0.0722 * b1
                
                val r2 = (pixels[idx2] shr 16) and 0xFF
                val g2 = (pixels[idx2] shr 8) and 0xFF
                val b2 = pixels[idx2] and 0xFF
                val l2 = 0.2126 * r2 + 0.7152 * g2 + 0.0722 * b2

                totalDiff += abs(l1 - l2)
                pixelCount++
            }
        }

        // Vertical differences
        for (x in 0 until width) {
            for (y in 0 until height - 1) {
                val idx1 = y * width + x
                val idx2 = (y + 1) * width + x
                
                val r1 = (pixels[idx1] shr 16) and 0xFF
                val g1 = (pixels[idx1] shr 8) and 0xFF
                val b1 = pixels[idx1] and 0xFF
                val l1 = 0.2126 * r1 + 0.7152 * g1 + 0.0722 * b1
                
                val r2 = (pixels[idx2] shr 16) and 0xFF
                val g2 = (pixels[idx2] shr 8) and 0xFF
                val b2 = pixels[idx2] and 0xFF
                val l2 = 0.2126 * r2 + 0.7152 * g2 + 0.0722 * b2

                totalDiff += abs(l1 - l2)
                pixelCount++
            }
        }

        if (pixelCount == 0) return 0.5f

        val avgDiff = (totalDiff / pixelCount) / 255.0
        val score = (avgDiff * 15.0).toFloat().coerceIn(0.0f, 1.0f)
        return score
    }

    private fun buildAverageHash(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val width = 8
        val height = 8
        val pixels = IntArray(64)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val luminances = DoubleArray(64)
        var sum = 0.0
        for (i in 0 until 64) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val l = 0.2126 * r + 0.7152 * g + 0.0722 * b
            luminances[i] = l
            sum += l
        }
        val average = sum / 64.0

        var hash = 0L
        for (i in 0 until 64) {
            if (luminances[i] >= average) {
                hash = hash or (1L shl i)
            }
        }
        
        scaled.recycle()
        return String.format("%016x", hash)
    }

    fun calculateHammingDistance(hash1: String, hash2: String): Int {
        return try {
            val h1 = java.lang.Long.parseUnsignedLong(hash1, 16)
            val h2 = java.lang.Long.parseUnsignedLong(hash2, 16)
            java.lang.Long.bitCount(h1 xor h2)
        } catch (e: Exception) {
            64
        }
    }
}
