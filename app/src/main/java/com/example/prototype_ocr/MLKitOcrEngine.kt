package com.example.prototype_ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.regex.Pattern
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfDouble
import com.example.prototype_ocr.data.TestType
import com.example.prototype_ocr.data.DeviceType


data class OcrResult(
    val value: Double,
    val rawText: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect? = null,
    val stripResults: List<StripOcrResult>? = null // For debugging strip-based OCR
)

data class StripOcrResult(
    val stripIndex: Int,
    val text: String,
    val value: Double?,
    val boundingBox: android.graphics.Rect
)

class MLKitOcrEngine(private val deviceType: DeviceType = DeviceType.HORIBA) {

    companion object {
        init {
            System.loadLibrary("opencv_java4")
        }
        const val HORIBA_STRIP_COUNT = 5 // Number of horizontal strips for Horiba
    }

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    var useStripProcessing = true // Enable/disable strip-based processing for Horiba

    // Regex patterns for mg/dL detection optimized for Horiba displays
    private val mgDlPatterns = listOf(
        // Strict patterns requiring explicit mg/dL format
        Pattern.compile("""(\d+\s*(?:\.\s*\d+)?)\s*mg\s*/\s*d[lL]""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(\d+(?:\.\d+)?)\s*mg\s*/\s*dl""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(\d+(?:\.\d+)?)\s*mg/dL""", Pattern.CASE_INSENSITIVE),
        // Looser patterns but still require both mg and dL components
        Pattern.compile("""(\d+(?:\.\d+)?)\s+mg\s+d[lL]""", Pattern.CASE_INSENSITIVE),
        // Pattern for cases where mg and dL might be on same line but separated
        Pattern.compile("""Result\s*:?\s*(\d+(?:\.\d+)?)\s*mg/dL""", Pattern.CASE_INSENSITIVE)
    )

    suspend fun processImage(bitmap: Bitmap): Pair<OcrResult?, String> {
        // Use strip processing for Horiba if enabled
        if (deviceType == DeviceType.HORIBA && useStripProcessing) {
            return processImageWithStrips(bitmap)
        }

        // Original processing for Robonik or when strip processing is disabled
        return suspendCancellableCoroutine { continuation ->
            // Apply preprocessing to improve OCR accuracy
            val preprocessedBitmap = preprocessImage(bitmap)
            val inputImage = InputImage.fromBitmap(preprocessedBitmap, 0)

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = extractMgDlFromText(visionText.text, visionText.textBlocks)
                    val allText = visionText.text // Capture all detected text
                    continuation.resume(Pair(result, allText))
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    /**
     * Process image using horizontal strips for better text ordering
     * Specifically for Horiba devices where text order matters
     */
    private suspend fun processImageWithStrips(bitmap: Bitmap): Pair<OcrResult?, String> {
        val preprocessedBitmap = preprocessImage(bitmap)
        val stripHeight = preprocessedBitmap.height / HORIBA_STRIP_COUNT
        val stripResults = mutableListOf<StripOcrResult>()
        val allTextLines = mutableListOf<String>()

        // Process each horizontal strip
        for (i in 0 until HORIBA_STRIP_COUNT) {
            val top = i * stripHeight
            val bottom = if (i == HORIBA_STRIP_COUNT - 1) preprocessedBitmap.height else (i + 1) * stripHeight
            val height = bottom - top

            // Extract strip bitmap
            val stripBitmap = Bitmap.createBitmap(
                preprocessedBitmap,
                0,
                top,
                preprocessedBitmap.width,
                height
            )

            // Process strip with OCR
            val stripText = processStrip(stripBitmap)
            if (stripText.isNotEmpty()) {
                allTextLines.add(stripText)

                // Try to find mg/dL value in this strip
                val value = findMgDlValue(stripText)
                stripResults.add(
                    StripOcrResult(
                        stripIndex = i,
                        text = stripText,
                        value = value,
                        boundingBox = android.graphics.Rect(0, top, preprocessedBitmap.width, bottom)
                    )
                )
            }

            stripBitmap.recycle()
        }

        // Combine all strip texts in order
        val combinedText = allTextLines.joinToString("\n")

        // Find the best mg/dL value from all strips
        val bestStrip = stripResults.firstOrNull { it.value != null }

        val result = if (bestStrip != null) {
            val confidence = calculateConfidence(bestStrip.text, bestStrip.value!!)
            if (confidence >= 0.7f) {
                OcrResult(
                    value = bestStrip.value,
                    rawText = bestStrip.text,
                    confidence = confidence,
                    boundingBox = bestStrip.boundingBox,
                    stripResults = stripResults
                )
            } else null
        } else {
            // Fallback: try to extract from combined text
            val value = findMgDlValue(combinedText)
            if (value != null) {
                val confidence = calculateConfidence(combinedText, value)
                if (confidence >= 0.7f) {
                    OcrResult(
                        value = value,
                        rawText = combinedText,
                        confidence = confidence,
                        stripResults = stripResults
                    )
                } else null
            } else null
        }

        return Pair(result, combinedText)
    }

    /**
     * Process a single horizontal strip and return detected text
     */
    private suspend fun processStrip(stripBitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(stripBitmap, 0)

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { _ ->
                    continuation.resume("") // Return empty string on failure
                }
        }
    }
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        return when (deviceType) {
            DeviceType.HORIBA -> preprocessHoriba(bitmap)
            DeviceType.ROBONIK -> preprocessRobonik(bitmap)
        }
    }

    /**
     * Horiba preprocessing - Dark background with light text
     * Current logic for dark LCD with light/white text
     */
    private fun preprocessHoriba(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        val contrast = computeContrast(gray)

        val processed = when {
            contrast < 25 -> heavyEnhance(gray)
            contrast < 55 -> mediumEnhance(gray)
            else -> gray
        }

        val outputBitmap = Bitmap.createBitmap(
            processed.cols(),
            processed.rows(),
            Bitmap.Config.ARGB_8888
        )

        Utils.matToBitmap(processed, outputBitmap)

        src.release()
        gray.release()
        if (processed !== gray) processed.release()

        return outputBitmap
    }

    /**
     * Robonik preprocessing - Light blue background with dark blue text and white values
     * Optimized for light blue LCD with dark blue text and white numbers
     * Uses adaptive threshold selection (140-190 range) for varying lighting conditions
     */
    private fun preprocessRobonik(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        // Try multiple threshold values and select the best result
        val thresholdRange = 140..170 step 10
        var bestResult: Mat? = null
        var bestScore = 0.0

        for (threshold in thresholdRange) {
            val bw = Mat()
            Imgproc.threshold(gray, bw, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)

            // Invert: white text on black background for OCR
            val inverted = Mat()
            Core.bitwise_not(bw, inverted)

            // Score based on text-like regions (white pixels in reasonable distribution)
            val score = evaluateThreshold(inverted)

            if (score > bestScore) {
                bestScore = score
                bestResult?.release()
                bestResult = inverted.clone()
            }

            bw.release()
            inverted.release()
        }

        // Apply morphological operations to clean up the best result
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        val cleaned = Mat()
        Imgproc.morphologyEx(bestResult!!, cleaned, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.morphologyEx(cleaned, cleaned, Imgproc.MORPH_OPEN, kernel)

        // Convert back to bitmap
        val resultBitmap = Bitmap.createBitmap(
            cleaned.cols(),
            cleaned.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(cleaned, resultBitmap)

        // Cleanup
        mat.release()
        gray.release()
        bestResult.release()
        cleaned.release()
        kernel.release()

        return resultBitmap
    }

    /**
     * Evaluate threshold quality based on text-like characteristics
     * Returns a score between 0.0 (poor) and 1.0 (excellent)
     */
    private fun evaluateThreshold(mat: Mat): Double {
        // Calculate score based on:
        // 1. Number of white pixels (should be moderate, not too many/few)
        // 2. Distribution of white pixels (should have some clustering for text)

        val totalPixels = (mat.rows() * mat.cols()).toDouble()
        val whitePixels = Core.countNonZero(mat).toDouble()
        val whiteRatio = whitePixels / totalPixels

        // Ideal white ratio for text: 10-30%
        val ratioScore = when {
            whiteRatio < 0.05 -> 0.0  // Too little text
            whiteRatio > 0.40 -> 0.0  // Too much noise
            whiteRatio in 0.10..0.30 -> 1.0  // Ideal range
            else -> 0.5  // Acceptable but not ideal
        }

        // Edge density as a proxy for text clarity
        val edges = Mat()
        Imgproc.Canny(mat, edges, 50.0, 150.0)
        val edgePixels = Core.countNonZero(edges).toDouble()
        val edgeScore = (edgePixels / totalPixels).coerceIn(0.0, 1.0)
        edges.release()

        // Combined score
        return ratioScore * 0.7 + edgeScore * 0.3
    }
    private fun computeContrast(mat: Mat): Double {
        val mean = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(mat, mean, std)
        return std.toArray()[0]
    }

    private fun heavyEnhance(src: Mat): Mat {
        val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(src, enhanced)

        Imgproc.GaussianBlur(enhanced, enhanced, Size(3.0, 3.0), 0.0)
        Core.addWeighted(enhanced, 1.4, enhanced, -0.4, 0.0, enhanced)

        return enhanced
    }
    private fun mediumEnhance(src: Mat): Mat {
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(src, enhanced)
        return enhanced
    }


//    private fun preprocessImage(bitmap: Bitmap): Bitmap {
//        try {
//            // Convert bitmap to OpenCV Mat
//            val mat = Mat()
//            Utils.bitmapToMat(bitmap, mat)
//
//            // 1. Convert to grayscale (reduces color channel noise)
//            val grayMat = Mat()
//            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
//
//            // 2. Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
//            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
//            val contrastMat = Mat()
//            clahe.apply(grayMat, contrastMat)
//
//            // 3. Apply slight Gaussian blur to reduce noise
//            val blurredMat = Mat()
//            Imgproc.GaussianBlur(contrastMat, blurredMat, Size(3.0, 3.0), 0.0)
//
//            // 4. Simple sharpening using unsharp mask technique
//            val sharpenedMat = Mat()
//            Core.addWeighted(contrastMat, 1.5, blurredMat, -0.5, 0.0, sharpenedMat)
//
//            // Convert back to bitmap
//            val preprocessedBitmap = Bitmap.createBitmap(
//                sharpenedMat.cols(),
//                sharpenedMat.rows(),
//                Bitmap.Config.ARGB_8888
//            )
//            Utils.matToBitmap(sharpenedMat, preprocessedBitmap)
//
//            // Clean up OpenCV resources
//            mat.release()
//            grayMat.release()
//            contrastMat.release()
//            blurredMat.release()
//            sharpenedMat.release()
//
//            return preprocessedBitmap
//
//        } catch (e: Exception) {
//            // If preprocessing fails, return original bitmap
//            e.printStackTrace()
//            return bitmap
//        }
//    }

    private fun extractMgDlFromText(
        fullText: String,
        textBlocks: List<com.google.mlkit.vision.text.Text.TextBlock>
    ): OcrResult? {

        // Look for mg/dL values in individual text blocks first (more accurate positioning)
        for (block in textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                val mgDlValue = findMgDlValue(lineText)

                if (mgDlValue != null) {
                    val confidence = calculateConfidence(lineText, mgDlValue)
                    // Only return if confidence is above threshold
                    if (confidence >= 0.7f) {
                        return OcrResult(
                            value = mgDlValue,
                            rawText = lineText,
                            confidence = confidence,
                            boundingBox = line.boundingBox
                        )
                    }
                }
            }
            // Handle split case:
            // Result 121.1
            // mg/dL (on another line)
            val blockHasUnit = normalizeUnits(normalizeOcrText(block.text)).contains("mg/dl")

            if (blockHasUnit) {
                for (line in block.lines) {
                    val normalizedLine = normalizeUnits(normalizeOcrText(line.text))

                    if (normalizedLine.contains("result")) {
                        val numberMatch = Pattern.compile("""\b(\d+(?:\.\d+)?)\b""").matcher(line.text)

                        if (numberMatch.find()) {
                            val value = numberMatch.group(1)?.toDoubleOrNull()
                            if (value != null) {
                                return OcrResult(
                                    value = value,
                                    rawText = line.text,
                                    confidence = 0.8f,
                                    boundingBox = line.boundingBox
                                )
                            }
                        }
                    }
                }
            }

        }
                // Fallback: search in full text
        val mgDlValue = findMgDlValue(fullText)
        return if (mgDlValue != null) {
            val confidence = calculateConfidence(fullText, mgDlValue)
            // Only return if confidence is above threshold
            if (confidence >= 0.7f) {
                OcrResult(
                    value = mgDlValue,
                    rawText = fullText,
                    confidence = confidence
                )
            } else null
        } else null
    }
    private fun normalizeOcrText(text: String): String {
        return text
            .lowercase()
            .replace(" ", "")
            .replace(":", "")
            .replace(";", "")
            .replace(",", "")
    }
    private fun normalizeUnits(text: String): String {
        return text
            .replace("resut", "result")
            .replace("resutt", "result")
            .replace("recut", "result")
            .replace("resu1t", "result")
            .replace("mgfdl", "mg/dl")
            .replace("mgfdi", "mg/dl")
            .replace("mgd1", "mg/dl")
            .replace("mgdi", "mg/dl")
            .replace("mgdl", "mg/dl")
            .replace("ngdl", "mg/dl")
            .replace("m9/dl", "mg/dl")
            .replace("mgidl", "mg/dl")
            .replace("mg/d1", "mg/dl")
            .replace("mgd/", "mg/dl")
    }
    private fun findMgDlValue(text: String): Double? {
        val normalized = normalizeUnits(normalizeOcrText(text))

        for (pattern in mgDlPatterns) {
            val matcher = pattern.matcher(normalized)
            if (matcher.find()) {
                return matcher.group(1)?.toDoubleOrNull()
            }
        }
        return null
    }


    private fun calculateConfidence(text: String, value: Double): Float {
        var confidence = 0.5f // Lower base confidence to be more strict

        // Higher confidence for specific patterns we expect in Horiba
        when {
            text.contains("mg/dL", ignoreCase = true) -> confidence += 0.4f
            text.contains("Result", ignoreCase = true) && text.contains("mg", ignoreCase = true) -> confidence += 0.2f
            text.contains("mg", ignoreCase = true) && text.contains("dL", ignoreCase = true) -> confidence += 0.3f

            // Confidence based on value range (typical medical values)
            value in 70.0..400.0 -> confidence += 0.2f
            value in 50.0..600.0 -> confidence += 0.1f
            value == 0.0 -> confidence -= 0.5f // Heavily penalize 0.0 values
        }

        // Additional validation: if no "dL" found, reduce confidence significantly
        if (!text.contains("dL", ignoreCase = true) && !text.contains("dl", ignoreCase = true)) {
            confidence -= 0.4f
        }

        return confidence.coerceIn(0.0f, 1.0f)
    }

    fun cleanup() {
        textRecognizer.close()
    }

    /**
     * Detect test type from OCR text
     * Returns detected TestType or GLUCOSE as default
     */
    fun detectTestType(text: String): TestType {
        val upperText = text.uppercase()

        return when {
            // Cholesterol patterns
            upperText.contains("CHOLE") ||
            upperText.contains("CHOL") ||
            upperText.contains("CHO") -> TestType.CHOLESTEROL

            // Glucose patterns
            upperText.contains("GLU") ||
            upperText.contains("GLUCOSE") -> TestType.GLUCOSE

            // Creatinine patterns
            upperText.contains("CRE") ||
            upperText.contains("CREAT") ||
            upperText.contains("CREATININE") -> TestType.CREATININE

            // Default to glucose if nothing detected
            else -> TestType.GLUCOSE
        }
    }
}