# Android Bounding Box Configuration Guide

## Overview
The OverlayView now supports **configurable crop regions** for Robonik pipeline, specifically designed for extracting mg/dL values and test types.

## Default Crop Regions

### 1. Test Type (Top-Center)
- **Position:** Top-center of parent bounding box
- **Color:** Cyan
- **Default coordinates:**
  - X: 35% from left (0.35)
  - Y: 5% from top (0.05)
  - Width: 30% of parent (0.30)
  - Height: 25% of parent (0.25)

### 2. mg/dL Value (Middle-Left)
- **Position:** Middle-left of parent bounding box
- **Color:** Yellow
- **Default coordinates:**
  - X: 5% from left (0.05)
  - Y: 35% from top (0.35)
  - Width: 40% of parent (0.40)
  - Height: 30% of parent (0.30)

## Editing Crop Regions

### In Code

```kotlin
// Get reference to OverlayView
val overlayView: OverlayView = findViewById(R.id.overlayView)

// Enable crop regions
overlayView.enableCropRegions(true)

// Update Test Type region
overlayView.updateCropRegion(
    name = "Test Type",
    x = 0.35f,      // 35% from left
    y = 0.05f,      // 5% from top
    width = 0.30f,  // 30% width
    height = 0.25f  // 25% height
)

// Update mg/dL Value region
overlayView.updateCropRegion(
    name = "mg/dL Value",
    x = 0.05f,      // 5% from left
    y = 0.35f,      // 35% from top
    width = 0.40f,  // 40% width
    height = 0.30f  // 30% height
)
```

### Add Custom Crop Region

```kotlin
overlayView.addCropRegion(
    name = "Custom Region",
    x = 0.5f,
    y = 0.5f,
    width = 0.2f,
    height = 0.2f,
    color = Color.GREEN
)
```

### Reset to Defaults

```kotlin
overlayView.resetToDefaultCrops()
```

### Get Crop Region Data

```kotlin
val cropRegions = overlayView.getCropRegions()
// Returns: List<Map<String, Any>> with coordinates
```

## Using with Server OCR

```kotlin
// Initialize OCR repository
val serverOcrRepo = ServerOcrRepository(ApiClient.api)
val authRepo = AuthRepository(context)

// Get token
val token = authRepo.getToken() ?: return

// Get crop regions from overlay
val cropRegions = overlayView.getCropRegions().map { region ->
    CropRegionRequest(
        name = region["name"] as String,
        x = region["x"] as Float,
        y = region["y"] as Float,
        width = region["width"] as Float,
        height = region["height"] as Float
    )
}

// Process with server OCR
lifecycleScope.launch {
    val result = serverOcrRepo.processCroppedRegions(
        bitmap = capturedBitmap,
        cropRegions = cropRegions,
        token = token
    )
    
    result.onSuccess { ocrResult ->
        ocrResult.regions.forEach { region ->
            Log.d("OCR", "${region.name}: ${region.text}")
            when (region.name) {
                "Test Type" -> {
                    // Handle test type
                    val testType = region.text
                }
                "mg/dL Value" -> {
                    // Handle mg/dL value
                    val value = region.text.replace("[^0-9.]".toRegex(), "")
                }
            }
        }
    }
}
```

## Configuration via SharedPreferences

```kotlin
// Enable crop-based OCR
OcrConfig.ocrMode = OcrConfig.OcrMode.SERVER_CROPS
OcrConfig.useCropRegions = true

// Toggle individual regions
OcrConfig.testTypeCropEnabled = true
OcrConfig.valueCropEnabled = true
```

## Coordinate System

All coordinates are **relative** to the parent bounding box:

```
Parent Box:
┌─────────────────────────┐
│ (0,0)         (1,0)      │
│                          │
│   ┌────────┐             │  ← Test Type crop
│   │ Cyan   │             │
│   └────────┘             │
│                          │
│ ┌──────────┐             │  ← mg/dL crop
│ │ Yellow   │             │
│ └──────────┘             │
│                          │
│ (0,1)         (1,1)      │
└─────────────────────────┘
```

- **(0, 0)** = Top-left corner
- **(1, 1)** = Bottom-right corner
- **(0.5, 0.5)** = Center

## Visual Testing

```kotlin
// In your camera preview activity
class CameraActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        
        val overlayView: OverlayView = findViewById(R.id.overlayView)
        
        // Show crop regions for testing
        overlayView.enableCropRegions(true)
        
        // Optional: Enable strip visualization
        overlayView.enableStripVisualization(true, strips = 5)
    }
}
```

## Layout XML

```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- Camera Preview -->
    <androidx.camera.view.PreviewView
        android:id="@+id/previewView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    
    <!-- Overlay with crop regions -->
    <com.example.prototype_ocr.OverlayView
        android:id="@+id/overlayView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
        
</FrameLayout>
```

## Fine-Tuning Tips

### For Robonik Devices:
1. **Test Type** is usually at the top-center of the test strip
2. **mg/dL Value** is typically on the left-middle area
3. Adjust based on your specific device layout

### Calibration Process:
1. Capture sample image
2. View with crop regions enabled
3. Adjust x, y, width, height values
4. Test OCR accuracy
5. Iterate until optimal

### Common Adjustments:

```kotlin
// Make Test Type region wider
overlayView.updateCropRegion("Test Type", 0.25f, 0.05f, 0.50f, 0.25f)

// Move Value region higher
overlayView.updateCropRegion("mg/dL Value", 0.05f, 0.25f, 0.40f, 0.30f)

// Make Value region taller for larger fonts
overlayView.updateCropRegion("mg/dL Value", 0.05f, 0.35f, 0.40f, 0.40f)
```

## Files Modified

- [OverlayView.kt](../Prototype_ocr/app/src/main/java/com/example/prototype_ocr/OverlayView.kt) - Crop regions implementation
- [OcrConfig.kt](../Prototype_ocr/app/src/main/java/com/example/prototype_ocr/api/OcrConfig.kt) - Configuration management
- [ServerOcrRepository.kt](../Prototype_ocr/app/src/main/java/com/example/prototype_ocr/api/ServerOcrRepository.kt) - Server OCR integration

## Next Steps

1. **Test visually** - Enable crop regions and verify positioning
2. **Adjust coordinates** - Fine-tune for your specific device
3. **Test OCR** - Process images and verify text extraction
4. **Production** - Disable visualization, keep OCR enabled
