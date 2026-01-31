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
import org.opencv.core.CvType
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfDouble
import com.example.prototype_ocr.data.TestType
import com.example.prototype_ocr.data.DeviceType


data class OcrResult(
    val value: Double,
    val rawText: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect? = null
)

class MLKitOcrEngine(private val deviceType: DeviceType = DeviceType.HORIBA) {

    companion object {
        init {
            System.loadLibrary("opencv_java4")
        }
    }

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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

    suspend fun processImage(bitmap: Bitmap): Pair<OcrResult ?, String> {
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
     */
    private fun preprocessRobonik(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Convert to HSV to better isolate blue background
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV)

        // Convert to grayscale for text extraction
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply bilateral filter to preserve edges while smoothing
        val filtered = Mat()
        Imgproc.bilateralFilter(gray, filtered, 9, 75.0, 75.0)

        // Apply CLAHE for better contrast
        val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(filtered, enhanced)

        // Apply adaptive thresholding to separate white text from background
        val threshold = Mat()
        Imgproc.adaptiveThreshold(
            enhanced,
            threshold,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )

        // Invert so text is black on white (better for OCR)
        val inverted = Mat()
        Core.bitwise_not(threshold, inverted)

        // Slight morphological operations to clean up noise
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        val morphed = Mat()
        Imgproc.morphologyEx(inverted, morphed, Imgproc.MORPH_CLOSE, kernel)

        val outputBitmap = Bitmap.createBitmap(
            morphed.cols(),
            morphed.rows(),
            Bitmap.Config.ARGB_8888
        )

        Utils.matToBitmap(morphed, outputBitmap)

        src.release()
        hsv.release()
        gray.release()
        filtered.release()
        enhanced.release()
        threshold.release()
        inverted.release()
        kernel.release()
        morphed.release()

        return outputBitmap
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