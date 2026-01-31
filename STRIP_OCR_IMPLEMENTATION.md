# Horizontal Strip OCR Implementation for Horiba

## Overview
Implemented horizontal strip-based OCR processing specifically for Horiba devices to fix text ordering issues. The image is divided into multiple horizontal strips, and each strip is processed separately before combining the results in the correct order.

## Key Features

### 1. Visual Strip Overlay (OverlayView.kt)
- **Green horizontal lines** divide the capture box into strips
- Number of strips: **5** (configurable)
- Strips are shown only for Horiba devices
- Helps visualize where OCR will scan

### 2. Strip-Based Processing (MLKitOcrEngine.kt)
- Image divided into 5 equal horizontal strips
- Each strip processed independently with OCR
- Results combined in top-to-bottom order
- Preserves text ordering from display

### 3. Device-Specific Behavior
- **Horiba**: Strip processing enabled by default
- **Robonik**: Uses original full-image processing
- Can toggle strip processing on/off via `useStripProcessing` flag

## Implementation Details

### Data Structures

```kotlin
data class OcrResult(
    val value: Double,
    val rawText: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect?,
    val stripResults: List<StripOcrResult>? // Debug info
)

data class StripOcrResult(
    val stripIndex: Int,        // 0-4 for 5 strips
    val text: String,           // Text found in this strip
    val value: Double?,         // mg/dL value if found
    val boundingBox: Rect       // Position of strip
)
```

### Processing Flow

1. **Image Preprocessing**: Apply CLAHE enhancement (same as before)
2. **Strip Division**: Divide image into 5 horizontal regions
3. **Individual OCR**: Process each strip separately
4. **Text Combination**: Join all strip texts in order (top to bottom)
5. **Value Extraction**: Find best mg/dL value from any strip
6. **Result Assembly**: Return combined result with strip debugging info

### OverlayView Configuration

```kotlin
// Enable strip visualization (MainActivity.kt)
if (deviceType == DeviceType.HORIBA) {
    overlayView.enableStripVisualization(true, MLKitOcrEngine.HORIBA_STRIP_COUNT)
}

// Toggle visualization
overlayView.enableStripVisualization(enabled: Boolean, strips: Int)
```

### MLKitOcrEngine Configuration

```kotlin
// In MLKitOcrEngine
companion object {
    const val HORIBA_STRIP_COUNT = 5
}

var useStripProcessing = true  // Toggle strip mode

// Processing decision
if (deviceType == DeviceType.HORIBA && useStripProcessing) {
    return processImageWithStrips(bitmap)
}
```

## Benefits

### Ordering Accuracy
- Text extracted top-to-bottom consistently
- Reduces out-of-order OCR results
- Each strip is small, easier for OCR to process

### Debugging Capability
- Strip visualization shows exactly where OCR scans
- Can adjust strip count to fine-tune
- `stripResults` field contains per-strip debug data

### Flexibility
- Can toggle strip processing without code changes
- Adjust strip count via constant
- Strip visualization can be turned on/off

## Usage Instructions

### For Users
1. When using Horiba device, green lines will appear in the capture box
2. Align device LCD display with the strips
3. Try to position text clearly within individual strips
4. Capture photo as normal

### For Developers/Testing

**Adjust strip count:**
```kotlin
// Change in MLKitOcrEngine.kt
const val HORIBA_STRIP_COUNT = 7  // Try different values
```

**Disable strip processing:**
```kotlin
// In MainActivity.onCreate or MLKitOcrEngine initialization
mlkitOcrEngine.useStripProcessing = false
```

**Hide strip visualization:**
```kotlin
// In MainActivity
overlayView.enableStripVisualization(false)
```

**Access debug data:**
```kotlin
val (ocrResult, allText) = mlkitOcrEngine.processImage(bitmap)
ocrResult?.stripResults?.forEach { strip ->
    Log.d("Strip ${strip.stripIndex}", strip.text)
}
```

## Current Configuration

- **Strip Count**: 5 horizontal strips
- **Enabled For**: Horiba only
- **Visual Overlay**: Green lines (3px width)
- **Default State**: Enabled
- **Strip Height**: Image height / 5 (equal divisions)

## Future Enhancements

1. **Adaptive Strip Count**: Auto-adjust based on image size
2. **Overlap Processing**: Process overlapping strips to catch text at boundaries
3. **Strip Selection**: Only process strips that contain text
4. **Confidence Scoring**: Compare strip vs full-image results
5. **Configuration UI**: Let users toggle strip mode via settings

## Testing Notes

- Test with various Horiba displays to ensure strips align well
- If text falls between strips, adjust strip count or add overlap
- Monitor `stripResults` to see which strips contain the target value
- Compare results with/without strip processing using the toggle flag

## Files Modified

1. **OverlayView.kt**: Added strip visualization with green lines
2. **MLKitOcrEngine.kt**: Added strip processing logic and data structures
3. **MainActivity.kt**: Enabled strip visualization for Horiba devices

Build Status: ✅ Successful
