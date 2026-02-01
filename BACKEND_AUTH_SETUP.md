# BACKEND USER AUTHENTICATION SETUP PROMPT

---

## Context
You are working on the **Node.js/Express backend** for the Medical Device OCR app. The Android app now uses a **dual identification system**:
1. **Panel ID** (from QR scan): Identifies the testing device/panel (e.g., "DPHS-1", "DPHS-2")
2. **User Details** (from login): Identifies the healthcare worker performing tests

## Current Backend Status
- **Base URL**: `http://192.168.1.103:3000`
- **Existing Endpoint**: `POST /api/upload` (for test data uploads) ✅ **NEEDS UPDATE for panelId**
- **Server Port**: 3000

## Required Implementation

### 1. User Authentication Endpoints

#### A. Login Endpoint
**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "username": "healthworker1",
  "password": "password123"
}
```

**Success Response (200)**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // JWT token (optional for now)
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

**Error Response (401)**:
```json
{
  "success": false,
  "data": null,
  "message": "Invalid username or password",
  "timestamp": 1738339800000
}
```

#### B. Get User Profile (Optional for now)
**Endpoint**: `GET /api/auth/profile`

**Headers**: `Authorization: Bearer <token>`

**Success Response (200)**:
```json
{
  "success": true,
  "data": {
    "id": "user-001",
    "username": "healthworker1",
    "name": "Dr. Rajesh Kumar",
    "role": "Health Worker",
    "email": "rajesh.kumar@dpha.tn.gov.in",
    "phoneNumber": "+91 9876543210",
    "healthCenter": "Primary Health Center - Chennai North",
    "district": "Chennai",
    "state": "Tamil Nadu"
  },
  "message": "Profile retrieved successfully",
  "timestamp": 1738339800000
}
```

### 2. Three Test Users to Create

Please create these three users in your database:

#### User 1: Health Worker
```javascript
{
  id: "user-001",
  username: "healthworker1",
  password: "password123",  // Hash this using bcrypt
  name: "Dr. Rajesh Kumar",
  role: "Health Worker",
  email: "rajesh.kumar@dpha.tn.gov.in",
  phoneNumber: "+91 9876543210",
  phcName: "Primary Health Center - Chennai North",
  hubName: "Zone 3 Hub",
  blockName: "Teynampet Block",
  districtName: "Chennai",
  state: "Tamil Nadu"
}
```

#### User 2: Lab Technician
```javascript
{
  id: "user-002",
  username: "labtech1",
  password: "labtech123",  // Hash this using bcrypt
  name: "Ms. Priya Sharma",
  role: "Lab Technician",
  email: "priya.sharma@dpha.tn.gov.in",
  phoneNumber: "+91 9876543211",
  phcName: "District Hospital - Coimbatore",
  hubName: "Zone 5 Hub",
  blockName: "RS Puram Block",
  districtName: "Coimbatore",
  state: "Tamil Nadu"
}
```

#### User 3: Administrator
```javascript
{
  id: "user-003",
  username: "admin1",
  password: "admin123",  // Hash this using bcrypt
  name: "Dr. Suresh Babu",
  role: "Administrator",
  email: "suresh.babu@dpha.tn.gov.in",
  phoneNumber: "+91 9876543212",
  phcName: "Directorate of Public Health",
  hubName: "Central Hub",
  blockName: "HQ Block",
  districtName: "Chennai",
  state: "Tamil Nadu"
}
```

### 3. Database Schema

If using MongoDB:
```javascript
const userSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  username: { type: String, required: true, unique: true },
  password: { type: String, required: true },  // Hashed
  name: { type: String, required: true },
  role: { type: String, required: true },
  email: { type: String },
  phoneNumber: { type: String },
  healthCenter: { type: String },
  district: { type: String },
  state: { type: String },
  createdAt: { type: Date, default: Date.now },
  lastLogin: { type: Date }
});
```

If using JSON file (simpler for now):
```json
{
  "users": [
    {
      "id": "user-001",
      "username": "healthworker1",
      "password": "$2b$10$...",  // bcrypt hash
      "name": "Dr. Rajesh Kumar",
      "role": "Health Worker",
      "email": "rajesh.kumar@dpha.tn.gov.in",
      "phoneNumber": "+91 9876543210",
      "healthCenter": "Primary Health Center - Chennai North",
      "district": "Chennai",
      "state": "Tamil Nadu"
    }
  ]
}
```

### 4. Implementation Requirements

#### Security
- ✓ **Password Hashing**: Use `bcrypt` to hash passwords (salt rounds: 10)
- ✓ **Input Validation**: Validate username and password are not empty
- ✓ **Case Sensitivity**: Usernames should be case-insensitive
- ✓ **Rate Limiting**: (Optional) Prevent brute force attacks

#### Response Format
All responses must follow this structure:
```json
{
  "success": Boolean,
  "data": Object | null,
  "message": String,
  "timestamp": Number  // milliseconds since epoch
}
```

#### Password Hashing Example
```javascript
const bcrypt = require('bcryptjs');

// When creating user
const hashedPassword = await bcrypt.hash(plainPassword, 10);

// When verifying login
const isValid = await bcrypt.compare(plainPassword, hashedPassword);
```

### 5. Dependencies to Install

```bash
npm install bcryptjs
```

Optional (for JWT tokens):
```bash
npm install jsonwebtoken
```

### 6. Testing Credentials

After implementation, test with:
- **Username**: `healthworker1`, **Password**: `password123`
- **Username**: `labtech1`, **Password**: `labtech123`
- **Username**: `admin1`, **Password**: `admin123`

### 7. Integration with Existing Upload Endpoint

⚠️ **CRITICAL UPDATE REQUIRED**: The `/api/upload` endpoint needs modification.

**Upload Request Now Includes**:
1. **panelId** (from QR scan) - "DPHS-1", "DPHS-2", etc.
2. **User Details** (from login) - userId, userName, phcName, hubName, blockName, districtName

**Updated UploadInfo Structure**:
```json
{
  "id": "upload-uuid",
  "timestamp": 1738339800000,
  "latitude": 11.0168,
  "longitude": 76.9558,
  "panelId": "DPHS-1",
  "userId": "user-001",
  "userName": "Dr. Rajesh Kumar",
  "phcName": "Primary Health Center - Chennai North",
  "hubName": "Zone 3 Hub",
  "blockName": "Teynampet Block",
  "districtName": "Chennai",
  "monthName": "February 2026"
}
```

**Required Backend Changes**:
1. Update upload endpoint to accept both `panelId` and user fields
2. Store panelId with each upload record
3. Return panelId in UploadResponse
4. Index uploads by both panelId and userId for queries

### 8. Error Handling

**Common Error Codes**:
- **400**: Bad Request (missing fields)
- **401**: Unauthorized (invalid credentials)
- **404**: User not found
- **500**: Server error

**Example Error Response**:
```json
{
  "success": false,
  "data": null,
  "message": "Username is required",
  "timestamp": 1738339800000
}
```

### 9. Expected Behavior

**First Login (with internet)**:
- Android app sends username/password to `/api/auth/login`
- Server validates credentials and returns user data
- App caches user data locally in SharedPreferences
- User can now work offline

**Subsequent Logins (offline)**:
- App checks cached credentials locally
- No server call needed
- User can create test records offline

**Upload (requires internet)**:
- When user uploads test data, internet is required
- Uses existing `/api/upload` endpoint

### 10. File Structure Suggestion

```
backend/
├── routes/
│   ├── auth.js        # Login and profile routes
│   └── upload.js      # Existing upload routes
├── models/
│   └── User.js        # User model/schema
├── middleware/
│   └── auth.js        # Authentication middleware (optional)
├── data/
│   └── users.json     # User data (if not using DB)
└── server.js          # Main server file
```

### 11. Quick Implementation Checklist

- [ ] Install bcryptjs: `npm install bcryptjs`
- [ ] Create three users with hashed passwords
- [ ] Create `POST /api/auth/login` endpoint
- [ ] Validate username/password
- [ ] Compare password hash with bcrypt
- [ ] Return user data on success
- [ ] Return error on invalid credentials
- [ ] Test with all three user accounts
- [ ] Verify response format matches Android expectations

### 12. Testing the Integration

**Using Postman/cURL**:
```bash
curl -X POST http://192.168.1.103:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"healthworker1","password":"password123"}'
```

**Expected Android Flow**:
1. User opens app → sees LoginActivity
2. Enters username and password
3. App calls `/api/auth/login`
4. Server validates and returns user data
5. App saves data locally
6. User navigates to home screen
7. Subsequent logins work offline

### 13. Optional Enhancements (Future)

- JWT token generation and validation
- Password reset functionality
- User registration endpoint
- Role-based access control
- Session management
- Multi-device login tracking
- User activity logs

---

## Summary

**What You Need to Do:**
1. Create `POST /api/auth/login` endpoint
2. Store three users (healthworker1, labtech1, admin1) with hashed passwords
3. Validate credentials and return user data
4. Follow the exact JSON response format shown above
5. Test with all three users

**Android Side is Already Done:**
- Login UI ✅
- Offline authentication ✅
- User data caching ✅
- Profile page ✅
- Navigation drawer ✅

**Once you implement this, the full login flow will work end-to-end!**
