package com.example.prototype_ocr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import android.os.Environment
import java.io.File
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.FileOutputStream
import android.content.Intent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.prototype_ocr.data.RecordsManager
import com.example.prototype_ocr.data.TestRecord
import com.example.prototype_ocr.data.TestType
import com.example.prototype_ocr.data.DeviceType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location


class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var imageCapture: ImageCapture
    private lateinit var recordsManager: RecordsManager
    private lateinit var deviceType: DeviceType
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    private val ocrExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var mlkitOcrEngine: MLKitOcrEngine
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Get device type from intent
        val deviceTypeName = intent.getStringExtra("device_type") ?: DeviceType.HORIBA.name
        deviceType = DeviceType.valueOf(deviceTypeName)
        
        // Initialize OCR engine with device type
        mlkitOcrEngine = MLKitOcrEngine(deviceType)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize RecordsManager
        recordsManager = RecordsManager(this)

        val captureButton = findViewById<Button>(R.id.captureButton)

        captureButton.setOnClickListener {
            capturePhoto()
        }

        val galleryButton = findViewById<Button>(R.id.galleryButton)

        galleryButton.setOnClickListener {
            startActivity(Intent(this, RecordsActivity::class.java))
        }

        overlayView = findViewById(R.id.overlayView)
        previewView = findViewById(R.id.previewView)

        // Enable strip visualization for Horiba devices, or crop regions for Robonik
        if (deviceType == DeviceType.HORIBA) {
            overlayView.enableStripVisualization(true, MLKitOcrEngine.HORIBA_STRIP_COUNT)
        } else if (deviceType == DeviceType.ROBONIK) {
            overlayView.enableCropRegions(true)
        }

        // Check and request permissions
        requestPermissions()
    }
    
    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                1001
            )
        } else {
            startCamera()
            getLastLocation()
        }
    }
    
    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty()) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            
            if (allGranted) {
                startCamera()
                getLastLocation()
            } else {
                // At minimum, camera permission is needed
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startCamera()
                }
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(this))
    }
    private fun capturePhoto() {
        val photoFile = File(
            getAppImageDir(),
            "capture_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    processImage(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(
                        this@MainActivity,
                        "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun processImage(photoFile: File) {
        mainScope.launch {
            try {
                val fullBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val croppedBitmap = cropImageToOverlay(fullBitmap)

                // Process with ML Kit OCR
                val (ocrResult, allDetectedText) = withContext(Dispatchers.IO) {
                    mlkitOcrEngine.processImage(croppedBitmap)
                }
                
                // Show result validation UI with all detected text
                showResultValidation(ocrResult, allDetectedText, croppedBitmap, photoFile)
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Processing failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                photoFile.delete()
            }
        }
    }

    private fun cropImageToOverlay(fullBitmap: Bitmap): Bitmap {
        val overlayRect = overlayView.getBoxRect()

        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()
        val imageWidth = fullBitmap.width.toFloat()
        val imageHeight = fullBitmap.height.toFloat()

        val previewAspect = previewWidth / previewHeight
        val imageAspect = imageWidth / imageHeight

        val scale: Float
        val dx: Float
        val dy: Float

        if (imageAspect > previewAspect) {
            scale = previewHeight / imageHeight
            dx = (imageWidth * scale - previewWidth) / 2f
            dy = 0f
        } else {
            scale = previewWidth / imageWidth
            dx = 0f
            dy = (imageHeight * scale - previewHeight) / 2f
        }

        val left = ((overlayRect.left + dx) / scale).toInt().coerceAtLeast(0)
        val top = ((overlayRect.top + dy) / scale).toInt().coerceAtLeast(0)
        val right = ((overlayRect.right + dx) / scale).toInt().coerceAtMost(fullBitmap.width)
        val bottom = ((overlayRect.bottom + dy) / scale).toInt().coerceAtMost(fullBitmap.height)

        return Bitmap.createBitmap(
            fullBitmap,
            left,
            top,
            right - left,
            bottom - top
        )
    }

    private fun showResultValidation(
        ocrResult: OcrResult?,
        allDetectedText: String,
        croppedBitmap: Bitmap,
        originalPhotoFile: File
    ) {
        // Auto-detect test type from OCR text
        val detectedTestType = mlkitOcrEngine.detectTestType(allDetectedText)
        
        // Always show dialog with detected text, whether mg/dL value was found or not
        ResultValidationDialog.show(
            this,
            ocrResult, // May be null if no mg/dL value detected
            allDetectedText, // All text detected by ML Kit
            croppedBitmap,
            detectedTestType, // Pass detected test type for pre-selection
            onSave = { result, testType ->
                saveTestRecord(result, testType, allDetectedText, croppedBitmap)
                originalPhotoFile.delete()
            },
            onDiscard = {
                originalPhotoFile.delete()
                Toast.makeText(this, "Result discarded", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveTestRecord(
        ocrResult: OcrResult?,
        testType: TestType,
        allDetectedText: String,
        croppedBitmap: Bitmap
    ) {
        val testNumber = recordsManager.getNextTestNumber(testType)
        val timestamp = System.currentTimeMillis()
        val filename = "test_${testNumber}_${testType.name.lowercase()}_${timestamp}.jpg"
        
        val imageFile = File(getAppImageDir(), filename)
        
        try {
            // Save image
            FileOutputStream(imageFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // Create TestRecord
            val testRecord = TestRecord(
                testNumber = testNumber,
                testType = testType,
                resultValue = ocrResult?.value,
                rawOcrText = allDetectedText,
                confidence = ocrResult?.confidence,
                validationTimestamp = timestamp,
                imageFileName = filename,
                isValidResult = ocrResult != null,
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude
            )
            
            // Save to RecordsManager
            val saved = recordsManager.saveRecord(testRecord)
            
            if (saved) {
                val message = if (ocrResult != null) {
                    "Test ${testNumber} saved: ${ocrResult.value} mg/dL (${testType.displayName})"
                } else {
                    "Test ${testNumber} saved: ${testType.displayName} (Generic OCR)"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                imageFile.delete() // Clean up if save failed
                Toast.makeText(this, "Failed to save test record", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            imageFile.delete() // Clean up on error
            Toast.makeText(this, "Failed to save test record", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppImageDir(): File {
        val dir = File(filesDir, "lcd_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override fun onDestroy() {
        super.onDestroy()
        mlkitOcrEngine.cleanup()
        mainScope.cancel()
        ocrExecutor.shutdown()
    }

}
