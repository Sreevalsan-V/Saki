# Server Setup Prompt for Medical Device OCR Upload System

I need you to create a backend server that can receive and process medical test record uploads from an Android application. Here are the requirements:

## Project Overview
Build a Node.js/Express server (or Python/Flask if you prefer) that handles uploads from a medical device OCR Android application used by health centers in India.

## Data Model

### Upload Object Structure
Each upload contains:
- **id**: Unique identifier (UUID)
- **uploadTimestamp**: Long (milliseconds since epoch - when upload was created)
- **uploadDateTime**: String (human-readable format: "31 Jan 2026 at 2:30 PM")
- **monthName**: String (e.g., "January 2026")
- **deviceId**: String (QR code scanned from device, format: "DPHS-1", "DPHS-2", etc.)
- **latitude**: Double (nullable, live GPS latitude at upload time)
- **longitude**: Double (nullable, live GPS longitude at upload time)
- **pdfFileName**: String (combined PDF file name)
- **testRecords**: Array of TestRecord objects (contains all tests with full details)

### TestRecord Object Structure
Each individual test record contains:
- **id**: String (UUID)
- **testName**: String (formatted as "test-1", "test-2", "test-3" based on order in upload)
- **testNumber**: Integer (per test type numbering: Test 1 Glucose, Test 2 Glucose, etc.)
- **testType**: String ("GLUCOSE", "CREATININE", or "CHOLESTEROL")
- **testDisplayName**: String (e.g., "Glucose", "Creatinine", "Cholesterol")
- **resultValue**: Double (nullable, test result value)
- **unit**: String (e.g., "mg/dL")
- **rawOcrText**: String (raw OCR output)
- **confidence**: Float (nullable, OCR confidence 0-1)
- **validationTimestamp**: Long (milliseconds - when test was taken/validated)
- **validationDateTime**: String (human-readable format: "15 Jan 2026 at 10:30 AM")
- **imageFileName**: String (LCD display image filename)
- **imageData**: Binary/Base64 (actual image file of the LCD display)
- **isValidResult**: Boolean
- **latitude**: Double (nullable, GPS latitude where test was taken)
- **longitude**: Double (nullable, GPS longitude where test was taken)

## API Endpoints Required

### 1. POST /api/upload
- Accept multipart/form-data
- Receive:
  - **uploadData** (JSON): Complete upload object with all metadata
    - Upload timestamp (long) and formatted datetime
    - Live GPS coordinates (latitude, longitude) captured at upload time
    - Device ID (DPHS-X format)
    - Month name
  - **testRecords** (JSON Array): Array of test records, each containing:
    - Test name (test-1, test-2, test-3)
    - Test type and display name
    - Result value and unit, month
- Return list of uploads with complete metadata including:
  - Upload timestamp and GPS location (where upload was created)
  - All test records with individual timestamps and GPS locations
  - Test count and summary
- Files not included in response (only URLs/pathtime
    - GPS coordinates (latitude, longitude) where test was taken
    - Image filename
    - Raw OCR text and confidence
  - **pdf** (File): Combined PDF report file
  - **test-1-image** (File): Image of first test LCD display
  - **test-2-image** (File): Image of second test LCD display (if exists)
  - **test-3-image** (File): Image of third test LCD display (if exists)
- Validate all required fields are present
- Store files in organized directory structure: `/uploads/{deviceId}/{monthName}/{uploadId}/`
  - Save PDF as: `combined_report.pdf`
  - Save images as: `test-1.jpg`, `test-2.jpg`, `test-3.jpg`
- Store complete metadata in database with all GPS and timestamp data
- Return success/failure response with upload ID

### 2. GET /api/uploads
- Query parameters: deviceId (optional), startDate, endDate
- Return complete upload metadata including:
  - Upload timestamp, datetime, and GPS coordinates
  - All test records with timestamps, GPS locations, and test names
  - Device ID and month
- Include download URLs for:
  - PDF report
  - Each test image (test-1, test-2,testName
- Stream individual test images by test name (test-1, test-2, test-3)
- Alternative: GET /api/download/image/:id/:testType (
### 3. GET /api/upload/:id
- Return specific upload metadata
- Include download URLs for PDF and images

### 4. GET /api/download/pdf/:id
- Stream PDF file for download

### 5. GET /api/download/image/:id/:imageType
- Stream individual test images (imageType: glucose, creatinine, cholesterol)
s: deviceId, startDate, endDate
- Return statistics:
  - Total uploads per month
  - Average test values per test type
  - Upload timeline with GPS locations
  - Test frequency by location
  - Time patterns (hourly distribution of tests)
### 7. GET /api/stats
- Query parameter: deviceId
- Return statistics:
   - Store all timestamp data (both Long and formatted datetime)
   - Store GPS coordinates for uploads and individual tests
   - Index on deviceId, uploadTimestamp, monthName for efficient queries
3. **File Storage**: Organize files in filesystem or use cloud storage (S3/Google Cloud Storage)
   - Directory structure: `/uploads/{deviceId}/{monthName}/{uploadId}/`
   - Store PDF and all test images
4. **Validation**: 
   - Validate device IDs match pattern "DPHS-\\d+"
   - Validate GPS coordinates are valid latitude/longitude
   - Validate timestamps are reasonable (not future dates)
   - Validate test images are present for each test record
5. **Error Handling**: Comprehensive error handling with meaningful messages
6. **Logging**: Log all upload attempts with:
   - Device ID and upload timestamp including:
  - All test data with timestamps and GPS coordinates
  - Upload locations and times
  - Test-by-test breakdown with images
- Email notifications on successful uploads
- Dashboard API for admin panel:
  - Total devices and uploads per device
  - Test statistics with time patterns
  - GPS-based location analytics
  - Timeline visualization of tests and uploads
- Data export with all metadata (timestamps, GPS, images)
- Geospatial queries (find uploads within radius of location)
- Time-based analytics (hourly/daily test patterns)
7. **CORS**: Enable CORS for mobile app requests
8. **Environment Config**: Use .env for configuration (database URL, storage path, JWT secret)
9. **Data Integrity**: Ensure atomic uploads (all files and metadata succeed or fail together
1. **Authentication**: Implement JWT-based authentication
2. **Database**: Use MongoDB or PostgreSQL for metadata storage
3. **File Storage**: Organize files in filesystem or use cloud storage (S3/Google Cloud Storage)
4. **Validation**: Validate device IDs match pattern "DPHS-\\d+"
5. **Error Handling**: Comprehensive error handling with meaningful messages
6. **Logging**: Log all upload attempts, successes, and failures
7. **CORS**: Enable CORS for mobile app requests
8. **Environment Config**: Use .env for configuration (database URL, storage path, JWT secret)

## Additional Features
- Generate summary reports (CSV/Excel) for a date range
- Email notifications on successful uploads
- Dashboard API for admin panel (total devices, uploads per device, test statistics)
- Data backup/export functionality

## Deliverables
1. Complete server code with folder structure
2. Database schema/models
3. API documentation (Postman collection or OpenAPI spec)
4. Setup instructions (README.md)
5. Docker configuration (optional but preferred)
6. Sample .env file

## Tech Stack Preference
Choose the best stack you're comfortable with:
- Option 1: Node.js + Express + MongoDB + Multer
- Option 2: Python + Flask + PostgreSQL + SQLAlchemy
- Option 3: Node.js + Express + PostgreSQL + Sequelize

Please create a complete, production-ready server with proper error handling, validation, and security best practices.

## Complete Upload Data Example

### JSON Payload Structure
```json
{
  "uploadData": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "uploadTimestamp": 1738339800000,
    "uploadDateTime": "31 Jan 2026 at 2:30 PM",
    "monthName": "January 2026",
    "deviceId": "DPHS-1",
    "latitude": 13.082680,
    "longitude": 80.270721,
    "pdfFileName": "combined_upload_1738339800000.pdf"
  },
  "testRecords": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "testName": "test-1",
      "testNumber": 1,
      "testType": "GLUCOSE",
      "testDisplayName": "Glucose",
      "resultValue": 120.5,
      "unit": "mg/dL",
      "rawOcrText": "Glucose 120.5 mg/dL",
      "confidence": 0.95,
      "validationTimestamp": 1738252200000,
      "validationDateTime": "15 Jan 2026 at 10:30 AM",
      "imageFileName": "glucose_1738252200000.jpg",
      "isValidResult": true,
      "latitude": 13.082500,
      "longitude": 80.270500
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440002",
      "testName": "test-2",
      "testNumber": 1,
      "testType": "CREATININE",
      "testDisplayName": "Creatinine",
      "resultValue": 1.2,
      "unit": "mg/dL",
      "rawOcrText": "Creatinine 1.2 mg/dL",
      "confidence": 0.92,
      "validationTimestamp": 1738165800000,
      "validationDateTime": "12 Jan 2026 at 11:15 AM",
      "imageFileName": "creatinine_1738165800000.jpg",
      "isValidResult": true,
      "latitude": 13.082400,
      "longitude": 80.270400
    },
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "testName": "test-3",
      "testNumber": 1,
      "testType": "CHOLESTEROL",
      "testDisplayName": "Cholesterol",
      "resultValue": 200.0,
      "unit": "mg/dL",
      "rawOcrText": "Cholesterol 200 mg/dL",
      "confidence": 0.89,
      "validationTimestamp": 1738079400000,
      "validationDateTime": "10 Jan 2026 at 9:45 AM",
      "imageFileName": "cholesterol_1738079400000.jpg",
      "isValidResult": true,
      "latitude": 13.082300,
      "longitude": 80.270300
    }
  ]
}
```

### Multipart Form Data Structure
```
POST /api/upload
Content-Type: multipart/form-data

Fields:
- uploadData: [JSON string from above]
- testRecords: [JSON array string from above]
- pdf: [File] combined_upload_1738339800000.pdf
- test-1-image: [File] glucose_1738252200000.jpg
- test-2-image: [File] creatinine_1738165800000.jpg
- test-3-image: [File] cholesterol_1738079400000.jpg
```

### Key Data Points to Capture

**Upload Level (captured at upload time):**
- Live GPS coordinates (where upload button was pressed)
- Current timestamp (when upload was created)
- Device ID from QR scan
- Month name for organization

**Test Level (captured when each test was validated):**
- GPS coordinates (where test was performed)
- Validation timestamp (when test was taken)
- Test images (LCD display photos)
- Test name (test-1, test-2, test-3 for API organization)
- Test type and number (for categorization)
- Result values and OCR data

This structure ensures complete audit trail with location and time data for both the upload event and each individual test event.
