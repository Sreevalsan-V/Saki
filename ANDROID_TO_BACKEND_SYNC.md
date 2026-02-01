# Android → Backend Synchronization Guide

**Last Updated**: February 1, 2026  
**Android Version**: 2.1.0  
**Purpose**: Inform backend team of Android implementation and required backend support

---

## � IMMEDIATE ACTION REQUIRED

### Upload Failing: Missing User Details

**Issue**: Android uploads are failing with "Missing user details" error.

**Diagnosis**: The login endpoint (`/api/auth/login`) is likely returning `null` or empty strings for required user fields.

**Required Fix**:
1. Check your users table - ensure all users have these fields populated:
   - `phcName` - Primary Health Center name
   - `hubName` - Hub/Zone name  
   - `blockName` - Block name
   - `districtName` - District name

2. Update login response to include ALL fields:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user-001",
      "name": "Dr. Rajesh Kumar",
      "phcName": "Primary Health Center - Chennai North",  // Must not be null
      "hubName": "Zone 3 Hub",                            // Must not be null
      "blockName": "Teynampet Block",                     // Must not be null
      "districtName": "Chennai"                           // Must not be null
    }
  }
}
```

3. Test with: `POST /api/auth/login` using credentials: `healthworker1` / `password123`

---

## �🔄 Change Summary

This document tracks the Android app's current implementation and communicates what the backend needs to support. Review this file when implementing backend endpoints for the Medical Device OCR app.

---

## 📋 Current Implementation Status

| Feature | Android Status | Backend Required | Priority | Notes |
|---------|---------------|------------------|----------|-------|
| Panel ID + User Details Upload | ✅ Implemented | ⚠️ **CRITICAL** | **HIGH** | Upload now sends panelId + 6 user fields |
| JWT Authentication | ✅ Implemented | ✅ Done | Medium | Working with offline support |
| GPS Tracking (Dual) | ✅ Implemented | ⚠️ Needs Update | **HIGH** | Upload GPS + per-test GPS |
| Strip-based OCR | ✅ Implemented | ⚠️ Check Support | Medium | Horiba device-specific processing |
| PDF Generation | ✅ Implemented | ⚠️ Storage Required | Medium | Combined 1-3 tests per upload |
| WhatsApp Sharing | ✅ Implemented | N/A | Low | Local feature only |

---

## 🆕 CRITICAL: Panel ID + User Details Structure (HIGH PRIORITY)

### What Android Sends Now

**Major Change**: Upload requests now include **both Panel ID (from QR scan) and User Details (from login)**.

#### Upload Info Structure
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1738339800000,
  "latitude": 13.082680,
  "longitude": 80.270721,
  
  // Panel identification (from QR scan)
  "panelId": "DPHS-1",
  
  // User identification (from login)
  "userId": "user-001",
  "userName": "Dr. Rajesh Kumar",
  "phcName": "Primary Health Center - Chennai North",
  "hubName": "Zone 3 Hub",
  "blockName": "Teynampet Block",
  "districtName": "Chennai",
  
  "monthName": "February 2026"
}
```

### What Backend Must Implement

#### 1. Update `/api/uploads` Endpoint
```javascript
// Backend must accept these fields in UploadInfo
POST /api/uploads
Content-Type: multipart/form-data
Authorization: Bearer JWT_TOKEN

Request Body:
{
  upload: {
    id: String,
    timestamp: Long,
    latitude: Double | null,
    longitude: Double | null,
    
    // NEW REQUIRED FIELDS
    panelId: String,           // "DPHS-1", "DPHS-2", etc.
    userId: String,
    userName: String,
    phcName: String,
    hubName: String,
    blockName: String,
    districtName: String,
    
    monthName: String
  },
  tests: [ /* test info array */ ],
  pdfBase64: String
}
```

#### 2. Update Response Structure
```json
{
  "uploadId": "550e8400-e29b-41d4-a716-446655440000",
  "panelId": "DPHS-1",          // Backend must return this
  "userId": "user-001",          // Backend must return this
  "userName": "Dr. Rajesh Kumar",
  "phcName": "Primary Health Center - Chennai North",
  "uploadTime": "2026-02-01T14:30:00Z",
  "uploadLocation": {
    "latitude": 13.082680,
    "longitude": 80.270721
  },
  "pdfUrl": "http://server/uploads/DPHS-1/February-2026/upload-id.pdf",
  "testsCount": 3,
  "tests": [ /* test responses */ ]
}
```

#### 3. Database Schema Update Required
```sql
-- Backend must add these columns to uploads table
ALTER TABLE uploads ADD COLUMN panelId VARCHAR(50) NOT NULL;
ALTER TABLE uploads ADD COLUMN userId VARCHAR(100) NOT NULL;
ALTER TABLE uploads ADD COLUMN userName VARCHAR(255) NOT NULL;
ALTER TABLE uploads ADD COLUMN phcName VARCHAR(255);
ALTER TABLE uploads ADD COLUMN hubName VARCHAR(255);
ALTER TABLE uploads ADD COLUMN blockName VARCHAR(255);
ALTER TABLE uploads ADD COLUMN districtName VARCHAR(255);

-- Add indexes for efficient queries
CREATE INDEX idx_uploads_panelId ON uploads(panelId);
CREATE INDEX idx_uploads_userId ON uploads(userId);
CREATE INDEX idx_uploads_composite ON uploads(panelId, userId, timestamp);
```

#### 4. File Storage Structure Update
```
/uploads/
  └── {panelId}/              # "DPHS-1", "DPHS-2", etc.
      └── {monthName}/        # "February 2026"
          └── {uploadId}/
              ├── combined.pdf
              ├── test-1-glucose.jpg
              ├── test-2-creatinine.jpg
              └── metadata.json
```

#### 5. Validation Required
```javascript
// Backend must validate
- panelId: Required, non-blank, pattern "DPHS-\d+"
- userId: Required, non-blank
- userName: Required, non-blank
- phcName: Required, non-blank
- hubName: Required, non-blank
- blockName: Required, non-blank
- districtName: Required, non-blank
```

---

## 🔐 Authentication System

### What Android Implements

#### Login Flow
```kotlin
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "healthworker1",
  "password": "password123"
}

Expected Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user-001",
      "username": "healthworker1",
      "name": "Dr. Rajesh Kumar",
      "role": "Health Worker",
      "email": "rajesh.kumar@dpha.tn.gov.in",
      "phoneNumber": "+91 9876543210",
      "phcName": "Primary Health Center - Chennai North",
      "hubName": "Zone 3 Hub",
      "blockName": "Teynampet Block",
      "districtName": "Chennai",
      "state": "Tamil Nadu"
    }
  },
  "message": "Login successful",
  "timestamp": 1738339800000
}
```

#### Android Behavior
- **Offline Support**: Caches user data in SharedPreferences for offline login
- **Token Storage**: Stores JWT securely in SharedPreferences
- **Auto-login**: Validates cached credentials on app launch
- **Session Management**: Uses token for all authenticated API calls

#### What Backend Must Support
1. JWT token generation with 24-hour expiry
2. User data must include: phcName, hubName, blockName, districtName, state
3. Token validation endpoint for session checks
4. Secure password hashing (bcrypt recommended)

---

## 📍 GPS Tracking (Dual Location System)

### What Android Implements

**Two Types of GPS Data**:
1. **Upload GPS**: Live location when creating upload (latitude, longitude in UploadInfo)
2. **Test GPS**: Location where each individual test was taken (latitude, longitude in TestInfo)

#### Data Structure
```json
{
  "upload": {
    "latitude": 13.082680,    // Where user creates upload
    "longitude": 80.270721,
    "timestamp": 1738339800000
  },
  "tests": [
    {
      "id": "test-1",
      "latitude": 13.081234,  // Where this specific test was taken
      "longitude": 80.269876,
      "timestamp": 1738252200000  // When test was validated
    }
  ]
}
```

#### What Backend Must Store
- Upload-level GPS (upload location)
- Test-level GPS (per-test locations)
- Both timestamps (upload time + test validation time)

---

## 📊 Test Records Structure

### What Android Sends

```json
{
  "tests": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "type": "GLUCOSE",           // "GLUCOSE", "CREATININE", or "CHOLESTEROL"
      "value": 120.5,              // Test result value
      "unit": "mg/dL",
      "timestamp": 1738252200000,  // When test was taken/validated
      "latitude": 13.081234,       // Where test was taken
      "longitude": 80.269876,
      "confidence": 0.95,          // OCR confidence (0.0-1.0)
      "rawText": "120.5",          // Raw OCR output
      "imageBase64": "data:image/jpeg;base64,/9j/4AAQ...",  // Test strip image
      "imageType": "jpeg"
    }
  ]
}
```

### Test Type Values
- `"GLUCOSE"` - Blood glucose test
- `"CREATININE"` - Creatinine test  
- `"CHOLESTEROL"` - Cholesterol test

### Validation Rules (Android enforces these)
- **1-3 tests per upload** (minimum 1, maximum 3)
- **No duplicate test types** in single upload
- **All tests must have images** (base64 encoded JPEG)
- **Values are optional** (can be null if OCR failed)

---

## 📄 PDF Generation

### What Android Generates

**PDF Content**:
- Header: Panel ID, Month, User Name, PHC
- Upload timestamp and GPS location
- Test results table (1-3 tests)
- Individual test details with images
- Footer: Generated timestamp

**PDF Filename Format**:
```
upload_{timestamp}.pdf
Example: upload_1738339800000.pdf
```

### What Backend Must Do
- Accept PDF as base64 string in `pdfBase64` field
- Decode and store as binary file
- Return PDF URL in response: `{baseUrl}/uploads/{panelId}/{monthName}/{uploadId}.pdf`
- Support PDF download via GET request

---

## 🎯 Device Type Support

### QR Code Format
Android scans QR codes with pattern: `DPHS-{number}`

Examples:
- `DPHS-1` (Glucose device)
- `DPHS-2` (Creatinine device)
- `DPHS-3` (Cholesterol device)

### OCR Processing
- **Horiba Device-Specific**: Strip-based OCR for medical test strips
- **ML Kit Text Recognition**: Google's ML Kit for text extraction
- **OpenCV Integration**: Image preprocessing for better accuracy

---

## 🔄 API Endpoints Android Uses

### 1. Authentication
```
POST /api/auth/login          - User login
GET  /api/auth/verify         - Token validation (optional)
GET  /api/auth/profile        - Get user profile (optional)
```

### 2. Uploads
```
POST /api/uploads             - Create new upload (REQUIRED)
GET  /api/uploads             - List all uploads (optional)
GET  /api/uploads/:id         - Get upload details (optional)
DELETE /api/uploads/:id       - Delete upload (optional)
```

### 3. Health Check
```
GET /api/health               - Server health status
```

---

## 📦 Complete Upload Request Example

```json
POST /api/uploads
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "upload": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1738339800000,
    "latitude": 13.082680,
    "longitude": 80.270721,
    "panelId": "DPHS-1",
    "userId": "user-001",
    "userName": "Dr. Rajesh Kumar",
    "phcName": "Primary Health Center - Chennai North",
    "hubName": "Zone 3 Hub",
    "blockName": "Teynampet Block",
    "districtName": "Chennai",
    "monthName": "February 2026"
  },
  "tests": [
    {
      "id": "test-glucose-001",
      "type": "GLUCOSE",
      "value": 120.5,
      "unit": "mg/dL",
      "timestamp": 1738252200000,
      "latitude": 13.081234,
      "longitude": 80.269876,
      "confidence": 0.95,
      "rawText": "120.5",
      "imageBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD...",
      "imageType": "jpeg"
    },
    {
      "id": "test-creatinine-002",
      "type": "CREATININE",
      "value": 1.2,
      "unit": "mg/dL",
      "timestamp": 1738252260000,
      "latitude": 13.081234,
      "longitude": 80.269876,
      "confidence": 0.92,
      "rawText": "1.2",
      "imageBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD...",
      "imageType": "jpeg"
    }
  ],
  "pdfBase64": "JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9UeXBlL..."
}
```

---

## ✅ Backend Implementation Checklist

### Phase 1: Critical Updates (Required Immediately)
- [ ] Update `/api/uploads` to accept panelId + 6 user fields
- [ ] Update database schema with new columns
- [ ] Update file storage to use panelId-based structure
- [ ] Update response to include panelId and user fields
- [ ] Add validation for all 7 new fields

### Phase 2: Authentication (Already Done)
- [x] JWT authentication system
- [x] User login endpoint
- [x] Token validation
- [x] User profile endpoint with complete fields

### Phase 3: Data Storage
- [ ] Store both upload GPS and per-test GPS
- [ ] Handle base64 PDF decoding and storage
- [ ] Store base64 test images (or convert and store)
- [ ] Create proper file directory structure

### Phase 4: Query Support
- [ ] Filter uploads by panelId
- [ ] Filter uploads by userId
- [ ] Filter uploads by date range
- [ ] Support composite queries (panelId + userId + date)

### Phase 5: Response Format
- [ ] Return panelId in all upload responses
- [ ] Return user details in responses
- [ ] Provide proper PDF URLs
- [ ] Include test count and details

---

## 🔧 Backend Configuration Needed

### Environment Variables
```env
# Required for Android app
BASE_URL=http://192.168.1.103:3000
JWT_SECRET=your-secret-key-here
JWT_EXPIRY=24h

# File upload settings
MAX_FILE_SIZE=50MB
UPLOAD_DIR=/uploads
```

### CORS Configuration
```javascript
// Android needs these headers
app.use(cors({
  origin: '*',  // Or specific Android app package
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

### Network Security
```xml
<!-- Android expects HTTP for local testing -->
<!-- Use HTTPS for production -->
Development: http://192.168.1.103:3000
Production: https://your-domain.com
```

---

## 🐛 Known Android Behaviors

### ⚠️ CRITICAL: Login Response Must Include All User Fields

**Common Upload Failure**: "Missing user details. Please ensure your profile is complete."

**Root Cause**: Backend login response is returning `null` or missing values for required fields.

**Required Fields in Login Response**:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user-001",           // REQUIRED
      "username": "healthworker1",
      "name": "Dr. Rajesh Kumar",  // REQUIRED
      "phcName": "...",            // REQUIRED - cannot be null/empty
      "hubName": "...",            // REQUIRED - cannot be null/empty
      "blockName": "...",          // REQUIRED - cannot be null/empty
      "districtName": "...",       // REQUIRED - cannot be null/empty
      "state": "Tamil Nadu"        // Optional
    }
  }
}
```

**Fix**: Backend must ensure all user records in the database have non-null, non-empty values for these 6 fields.

### Image Size
- Android sends JPEG images base64-encoded
- Typical size: 50-200 KB per test image
- PDF size: 200-500 KB for 1-3 tests
- Backend should support at least 10MB total request size

### Timestamps
- All timestamps are in **milliseconds** (not seconds)
- Format: Unix timestamp in milliseconds since epoch
- Example: `1738339800000` = February 1, 2026, 2:30 PM

### GPS Precision
- Latitude/Longitude: 6 decimal places
- Can be `null` if GPS permission denied
- Format: Double (not String)

### Month Name Format
- Always in English
- Format: "Month YYYY" (e.g., "February 2026")
- Locale-independent

---

## 📞 Error Handling

### What Android Expects

#### Success Response (200 OK)
```json
{
  "uploadId": "550e8400-e29b-41d4-a716-446655440000",
  "panelId": "DPHS-1",
  "userId": "user-001",
  "userName": "Dr. Rajesh Kumar",
  "phcName": "Primary Health Center - Chennai North",
  "uploadTime": "2026-02-01T14:30:00Z",
  "pdfUrl": "http://server/uploads/...",
  "testsCount": 2,
  "tests": [ /* array */ ]
}
```

#### Error Response (4xx/5xx)
```json
{
  "success": false,
  "message": "Validation failed: panelId is required",
  "timestamp": 1738339800000
}
```

### Android Error Handling
- **Network errors**: Shows "Upload failed" with local save option
- **401 Unauthorized**: Redirects to login
- **400 Bad Request**: Shows validation error to user
- **500 Server Error**: Shows "Server error, data saved locally"

---

## 📅 Android Change History

| Date | Version | Changes | Backend Action Required |
|------|---------|---------|------------------------|
| 2026-02-01 | 2.1.0 | Added panelId + user details to uploads | **UPDATE /api/uploads endpoint** |
| 2026-01-31 | 2.0.0 | Implemented login with offline support | Already done ✅ |
| 2026-01-30 | 1.5.0 | Added dual GPS tracking | Update schema for per-test GPS |
| 2026-01-28 | 1.2.0 | Added PDF generation | Implement PDF storage |
| 2026-01-25 | 1.0.0 | Initial upload implementation | Already done ✅ |

---

## 🧪 Test Data for Backend

### Test User Credentials
Android development uses these accounts:

```javascript
{
  username: "healthworker1",
  password: "password123",
  name: "Dr. Rajesh Kumar",
  phcName: "Primary Health Center - Chennai North",
  hubName: "Zone 3 Hub",
  blockName: "Teynampet Block",
  districtName: "Chennai",
  state: "Tamil Nadu"
}
```

### Test Panel IDs
```
DPHS-1  (Glucose testing device)
DPHS-2  (Creatinine testing device)
DPHS-3  (Cholesterol testing device)
```

### Sample Upload JSON
See [Complete Upload Request Example](#-complete-upload-request-example) above.

---

## 📚 Related Android Documentation

- [SERVER_SETUP_PROMPT.md](./SERVER_SETUP_PROMPT.md) - Complete backend setup guide
- [BACKEND_AUTH_SETUP.md](./BACKEND_AUTH_SETUP.md) - Authentication implementation
- [SERVER_INTEGRATION.md](./SERVER_INTEGRATION.md) - Android-side API integration
- [GPS_IMPLEMENTATION.md](./GPS_IMPLEMENTATION.md) - GPS tracking details

---

## ⚠️ Critical Reminders for Backend Team

### Must-Have for Android App to Work
1. ✅ **panelId field** in upload request and response
2. ✅ **6 user fields** (userId, userName, phcName, hubName, blockName, districtName)
3. ✅ **Per-test GPS** coordinates storage
4. ✅ **Base64 PDF** decoding and storage
5. ✅ **Base64 images** for each test
6. ✅ **Timestamps in milliseconds** (not seconds)

### Data Validation
```javascript
// Backend MUST validate
- panelId: /^DPHS-\d+$/ (e.g., "DPHS-1")
- Test types: "GLUCOSE" | "CREATININE" | "CHOLESTEROL"
- Test count: 1-3 per upload
- No duplicate test types per upload
- All 7 identification fields non-empty
```

### Response Requirements
```javascript
// Android expects these fields in response
{
  uploadId: String,      // Required
  panelId: String,       // Required (NEW)
  userId: String,        // Required (NEW)
  userName: String,      // Required (NEW)
  phcName: String,       // Required (NEW)
  uploadTime: String,    // ISO 8601 format
  pdfUrl: String,        // Full URL to PDF
  testsCount: Number,
  tests: Array
}
```

---

## 🔄 Sync Status

**Last Android Update**: February 1, 2026  
**Waiting for Backend**: panelId implementation in upload endpoint

### Current Sync Status
- ✅ Authentication endpoints - **SYNCED**
- ⚠️ Upload endpoint - **NEEDS UPDATE** (panelId + user fields)
- ✅ Health check - **SYNCED**
- ⏳ Statistics endpoint - **NOT IMPLEMENTED** (optional)

---

## 📞 Communication

### When Android Changes
1. Android team updates this file with new requirements
2. Android team notifies Backend team
3. Backend team reviews and estimates work
4. Backend team implements and updates their sync file
5. Both teams coordinate testing

### Questions or Issues?
- Check [BACKEND_AUTH_SETUP.md](./BACKEND_AUTH_SETUP.md) for complete API specs
- Review [SERVER_SETUP_PROMPT.md](./SERVER_SETUP_PROMPT.md) for full setup guide
- Contact Android development team for clarifications

---

**Document Version**: 2.1  
**Maintained by**: Android Development Team  
**Next Review**: When major Android features are added
