# GPS Data Implementation

## Overview
The app now captures GPS data at two different points:
1. **Individual Test GPS**: Captured when each test is validated (when the test is taken)
2. **Upload GPS**: Captured live when the upload is created (when tests are submitted)

## Data Structure

### TestRecord Model
Each test record contains:
- `latitude: Double?` - GPS latitude when test was validated
- `longitude: Double?` - GPS longitude when test was validated
- `validationTimestamp: Long` - Timestamp when test was taken

### Upload Model
Each upload contains:
- `latitude: Double?` - Live GPS latitude at upload time
- `longitude: Double?` - Live GPS longitude at upload time
- `uploadTimestamp: Long` - Timestamp when upload was created
- `glucoseRecord: TestRecord?` - Contains individual glucose test GPS + timestamp
- `creatinineRecord: TestRecord?` - Contains individual creatinine test GPS + timestamp
- `cholesterolRecord: TestRecord?` - Contains individual cholesterol test GPS + timestamp

## Implementation Details

### 1. Test Validation GPS (ValidationActivity.kt)
When a user validates a test result, the app:
- Requests location permission if not granted
- Uses `FusedLocationProviderClient.lastLocation` to get current GPS
- Stores GPS coordinates in the TestRecord along with validation timestamp
- Falls back to null GPS if location is unavailable

### 2. Upload GPS (UploadSelectionActivity.kt)
When a user creates an upload, the app:
- Requests location permission if not granted
- Calls `getCurrentLocationAndShowDialog()` which fetches live GPS
- Passes the Location object to `showValidationDialog(location: Location?)`
- Uses the live GPS coordinates when creating the Upload object
- Individual test records retain their original GPS from when they were validated

### 3. Display (UploadDetailActivity.kt)
The upload detail screen shows:
- **Upload GPS**: The live GPS coordinates from when the upload was created
- **Individual Test Details**: For each test (glucose, creatinine, cholesterol):
  - Test value and unit
  - **Date and time** when the test was taken (format: "15 Jan 2024 at 2:30 PM")
  - GPS coordinates from when the test was validated

### 4. WhatsApp Sharing (PastUploadsAdapter.kt)
Each upload in the past uploads list now has a "Share via WhatsApp" button that:
- Directly shares the PDF to WhatsApp without opening the detail view
- Includes upload month and device ID in the message
- Shows an error if WhatsApp is not installed
- Uses FileProvider for secure file sharing

## Permission Handling
The app handles location permissions at two points:
1. **ValidationActivity**: Requests `ACCESS_FINE_LOCATION` before saving test
2. **UploadSelectionActivity**: Requests `ACCESS_FINE_LOCATION` before creating upload

Permission request codes:
- `ValidationActivity.LOCATION_PERMISSION_REQUEST = 101`
- `UploadSelectionActivity.LOCATION_PERMISSION_REQUEST = 102`

If permission is denied, the app:
- Still allows test validation/upload creation
- Sets GPS fields to null
- Shows appropriate error messages

## User Experience

### Creating a Test
1. User scans device, captures image, validates result
2. App requests location permission (if first time)
3. GPS is captured at validation time
4. Test saved with GPS coordinates

### Creating an Upload
1. User selects month and tests to upload
2. Clicks "Upload" button
3. App requests location permission (if first time)
4. App fetches current GPS location
5. Shows confirmation dialog with selected tests
6. Creates upload with live GPS + all individual test data

### Viewing Upload Details
1. User opens past upload
2. Sees upload GPS (where upload was created)
3. Sees each test with:
   - Test value
   - **Date and time** test was taken
   - GPS where test was taken

### Sharing from Past Uploads List
1. User views past uploads list
2. Each upload has a "Share via WhatsApp" button
3. Tap button to share PDF directly to WhatsApp
4. Message includes month and device ID
5. No need to open detail view first

## Example Output

**Past Uploads List View:**
```
January 2024                    [DPHS-1]
Uploaded on Jan 15, 2024 at 2:30 PM
3 test records
Glucose (1) • Creatinine (1) • Cholesterol (1)
[Share via WhatsApp] button
```

**Upload Detail View:**
```
Upload Details
Month: January 2024
Device ID: DPHS-1
Date: Jan 15, 2024 at 2:30 PM
Tests: 3 records
Upload GPS: 13.082680, 80.270721

Test Records Breakdown
Glucose:
• 120 mg/dL
  15 Jan 2024 at 10:30 AM
  GPS: 13.082500, 80.270500

Creatinine:
• 1.2 mg/dL
  12 Jan 2024 at 11:15 AMand when each test was performed
- **Upload Location**: Track where uploads are being submitted from
- **Temporal Data**: Complete timestamps with hours/minutes for precise tracking
- **Verification**: Ensure tests are being conducted at authorized locations and times
- **Data Analysis**: Analyze test distribution across locations and time patterns
- **Quick Sharing**: Share reports directly from the list view via WhatsApp
- **User Convenience**: No need to open detail view to share PDF
  10 Jan 2024 at 9:45 AM
  GPS: 13.082300, 80.270300
```

## Benefits
- **Audit Trail**: Know exactly where each test was performed
- **Upload Location**: Track where uploads are being submitted from
- **Temporal Data**: Combine GPS with timestamps for complete context
- **Verification**: Ensure tests are being conducted at authorized locations
- **Data Analysis**: Analyze test distribution across different PHC locations
