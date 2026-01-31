# Server Integration Implementation

## Overview
Complete backend API integration for uploading medical test records to a Node.js/Express server. The app can now upload test data with images and PDFs to a remote server while maintaining local storage.

## Files Created

### 1. API Models (`api/ApiModels.kt`)
Data classes for API communication:
- **UploadRequest**: Request body with upload info, tests, and PDF
- **UploadInfo**: Upload metadata (id, timestamp, GPS, deviceId, monthName)
- **TestInfo**: Individual test data (id, type, value, timestamp, GPS, image, OCR data)
- **ApiResponse<T>**: Generic response wrapper
- **UploadResponse**: Server success response with URLs
- **TestResponse**: Individual test in response
- **LocationData**: GPS coordinates

### 2. API Interface (`api/MedicalOcrApi.kt`)
Retrofit interface with endpoints:
- `POST /api/upload` - Upload test records
- `GET /api/uploads` - Get all uploads (optional)
- `GET /api/upload/:id` - Get specific upload (optional)
- `DELETE /api/upload/:id` - Delete upload (optional)
- `GET /api/stats` - Get statistics (optional)

### 3. API Client (`api/ApiClient.kt`)
Retrofit configuration:
- Base URL: `http://10.0.2.2:3000` (emulator default)
- OkHttpClient with logging interceptor
- 30-second timeouts for connect/read/write
- GsonConverterFactory for JSON
- Method to create API with custom base URL

### 4. API Utilities (`api/ApiUtils.kt`)
Helper functions:
- `bitmapToBase64()` - Convert Bitmap to base64 JPEG
- `pdfToBase64()` - Convert PDF bytes to base64
- `getMonthName()` - Format timestamp to "January 2026"
- `isValidDeviceId()` - Validate DPHS-X pattern
- `generateUUID()` - Create UUID strings
- `getFormattedDateTime()` - Format timestamps
- `validateTestTypes()` - Check for duplicate test types

### 5. Upload Repository (`api/UploadRepository.kt`)
Main upload logic:
- `uploadToServer()` - Complete upload with Upload object
- `uploadTestRecords()` - Simplified upload with parameters
- Validates device ID format
- Checks test count (1-3)
- Prevents duplicate test types
- Converts images to base64
- Encodes PDF to base64
- Handles API responses
- Returns `Result<UploadResponse>`

### 6. Server Upload Helper (`api/ServerUploadHelper.kt`)
UI integration:
- `uploadToServer()` - Upload with progress dialog
- `showUploadDialog()` - Show confirmation with server option
- Loads test images from storage
- Handles success/failure with user feedback
- Provides local-only fallback option

## Configuration

### Server URL
Update in `ApiClient.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:3000"
```

**Options:**
- **Emulator**: `http://10.0.2.2:3000` (default, localhost on PC)
- **Real Device** (same network): `http://192.168.X.X:3000` (your PC's local IP)
- **Production**: `https://your-server.com`

**Find your PC's IP:**
- Windows: CMD → `ipconfig` → look for IPv4 Address
- Use that IP with port 3000

### Dependencies Added
In `app/build.gradle.kts`:
```gradle
// Retrofit for API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

### Permissions Added
In `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Usage Flow

### 1. User Creates Upload
1. User scans QR code for device (DPHS-1, DPHS-2, etc.)
2. Selects tests for current month
3. Clicks "Upload" button
4. App fetches current GPS location
5. Generates PDF with test reports

### 2. Upload Process
1. Upload saved locally first (existing behavior)
2. Dialog appears: "Upload to Server" or "Save Locally Only"
3. If "Upload to Server":
   - Progress dialog shows
   - Images loaded from `lcd_images/` directory
   - Images converted to base64 JPEG (85% quality)
   - PDF converted to base64
   - JSON request created with all data
   - POST to server
   - Response processed
4. Success or failure dialog shown

### 3. Server Response
**Success (201):**
- Upload ID from server
- Device ID confirmation
- Number of tests processed
- URLs for PDF and images
- Formatted timestamps and locations

**Failure (400/500):**
- Error message from server
- Data still saved locally
- User informed of failure

## Data Flow

### Local to Server Mapping

**Upload Object → UploadInfo:**
- `id` → upload ID (UUID)
- `uploadTimestamp` → timestamp (Long)
- `latitude/longitude` → GPS at upload time
- `deviceId` → QR scanned device ID
- `monthName` → "January 2026" format

**TestRecord → TestInfo:**
- `id` → test ID (UUID)
- `testType.name` → "GLUCOSE", "CREATININE", "CHOLESTEROL"
- `resultValue` → test result (Double, nullable)
- `testType.unit` → "mg/dL"
- `validationTimestamp` → when test was taken
- `latitude/longitude` → GPS when test was validated
- `confidence` → OCR confidence (0-1)
- `rawOcrText` → raw OCR output
- `imageFileName` → filename → loaded → base64
- `imageType` → "jpeg"

**PDF File:**
- Read from `upload_pdfs/` directory
- Converted to base64
- Sent as `pdfBase64` field

## Integration Points

### Modified Files

**UploadSelectionActivity.kt:**
- Import ServerUploadHelper
- Changed `createUpload()` to show server dialog
- Upload saved locally first, then option for server
- Progress and success/failure handling

## Validation & Error Handling

### Pre-Upload Validation
- ✓ Device ID must match "DPHS-\d+" pattern
- ✓ Must have 1-3 test records
- ✓ No duplicate test types allowed
- ✓ All test images must exist
- ✓ PDF file must exist

### Network Error Handling
- Connection timeouts (30s)
- HTTP error codes captured
- Error messages from server parsed
- Network exceptions caught
- User-friendly error messages

### Fallback Behavior
- If server upload fails, data is saved locally
- User can retry later
- Local data always preserved
- No data loss on network failure

## Testing Checklist

### Before Testing
- [ ] Update BASE_URL in ApiClient.kt
- [ ] Server is running on specified URL/port
- [ ] Device/emulator can reach server IP
- [ ] INTERNET permission in manifest

### Test Cases
1. **Single Test Upload**
   - Upload with 1 test (glucose only)
   - Verify JSON structure
   - Check server receives all fields

2. **Multiple Tests Upload**
   - Upload with 2 tests (glucose + creatinine)
   - Upload with 3 tests (all three types)
   - Verify no duplicates sent

3. **GPS Data**
   - With GPS enabled → check coordinates sent
   - Without GPS → check null values handled
   - Both upload-level and test-level GPS

4. **Error Scenarios**
   - Server offline → local save works
   - Invalid device ID → validation error
   - Missing images → error message
   - Network timeout → graceful failure

5. **Base64 Encoding**
   - Images decoded correctly on server
   - PDF decoded correctly on server
   - No line breaks in base64 strings
   - Size limits respected

## API Request Example

```json
POST http://YOUR_SERVER:3000/api/upload
Content-Type: application/json

{
  "upload": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1738339800000,
    "latitude": 13.082680,
    "longitude": 80.270721,
    "deviceId": "DPHS-1",
    "monthName": "January 2026"
  },
  "tests": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "type": "GLUCOSE",
      "value": 120.5,
      "unit": "mg/dL",
      "timestamp": 1738252200000,
      "latitude": 13.082500,
      "longitude": 80.270500,
      "confidence": 0.95,
      "rawText": "Glucose 120.5 mg/dL",
      "imageBase64": "base64_encoded_jpeg...",
      "imageType": "jpeg"
    }
  ],
  "pdfBase64": "base64_encoded_pdf..."
}
```

## Response Example

```json
{
  "success": true,
  "data": {
    "uploadId": "550e8400-e29b-41d4-a716-446655440000",
    "deviceId": "DPHS-1",
    "uploadTime": "31 Jan 2026 at 2:30 PM",
    "uploadLocation": {
      "latitude": 13.082680,
      "longitude": 80.270721
    },
    "pdfUrl": "http://server:3000/api/download/pdf/550e8400-...",
    "testsCount": 1,
    "tests": [
      {
        "id": "660e8400-...",
        "type": "GLUCOSE",
        "displayName": "Glucose",
        "value": 120.5,
        "unit": "mg/dL",
        "testTime": "15 Jan 2026 at 10:30 AM",
        "confidence": 0.95,
        "imageUrl": "http://server:3000/api/download/image/.../test-1",
        "testLocation": {
          "latitude": 13.082500,
          "longitude": 80.270500
        }
      }
    ]
  },
  "message": "Upload successful",
  "timestamp": 1738339800000
}
```

## Troubleshooting

### "Connection refused"
- Check server is running
- Verify BASE_URL matches server
- Emulator: use 10.0.2.2, not localhost
- Real device: ensure same network as PC

### "Invalid device ID"
- Device ID must match pattern: DPHS-1, DPHS-23, etc.
- Cannot be empty
- Check QR code scan result

### "Missing images"
- Ensure images saved in `lcd_images/` directory
- Check imageFileName matches actual file
- Verify file exists before upload

### "Request timeout"
- Large images may take time
- Check network speed
- Increase timeout if needed
- Compress images more (lower quality)

### "Base64 decode error on server"
- Using Base64.NO_WRAP flag (no line breaks)
- Not including data URI prefix
- Image format is JPEG

## Future Enhancements

1. **Retry Logic**: Auto-retry failed uploads
2. **Queue System**: Background upload queue
3. **Sync Status**: Track which uploads synced to server
4. **Settings**: Configure server URL in app
5. **Batch Upload**: Upload multiple past uploads
6. **Offline Mode**: Queue uploads when offline
7. **Progress Tracking**: Detailed upload progress
8. **Server Validation**: Test connection before upload

## Build Status
✅ All files created successfully
✅ Dependencies added
✅ Permissions configured
✅ Build successful
✅ Ready for testing

## Next Steps
1. Start your backend server
2. Update BASE_URL in ApiClient.kt
3. Test connection with one upload
4. Verify data received correctly on server
5. Check server logs for any issues
6. Test error scenarios
