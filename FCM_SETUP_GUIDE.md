# Firebase Cloud Messaging (FCM) Setup Guide

## Overview
This project is configured to send push notifications to Android devices via Firebase Cloud Messaging (FCM). The setup is optional but required to enable push notifications.

## Prerequisites
- A Firebase project (create one at https://console.firebase.google.com)
- An Android app registered in your Firebase project
- A service account private key JSON file from Firebase

## Setup Instructions

### Step 1: Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Create a project"
3. Follow the setup wizard
4. Add an Android app to your project

### Step 2: Generate Service Account Key
1. In Firebase Console, go to **Project Settings** (gear icon)
2. Click the **Service Accounts** tab
3. Click **Generate New Private Key**
4. Save the downloaded JSON file as: `backend/firebase-service-account.json`

### Step 3: Configure Docker (Already Done)
The `docker-compose.yml` is already configured to:
- Mount the service account JSON file into the Docker container
- Set the `FIREBASE_SERVICE_ACCOUNT_PATH` environment variable
- Initialize Firebase Admin SDK on backend startup

### Step 4: Start Services
```bash
# Make sure firebase-service-account.json exists in backend/ folder
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Step 5: Verify FCM is Working
Check the backend logs for:
```
Firebase Admin SDK initialized successfully
```

If you see:
```
Firebase service account path not configured. FCM notifications will be skipped.
```
Then the file is missing or path is incorrect.

## How It Works

1. **User registers FCM token**
   ```
   PUT /api/users/me/fcm-token
   Body: { "token": "device-fcm-token-here" }
   Response: 204 No Content
   ```

2. **Notification is created** (via job events, etc.)
   - Saved to PostgreSQL database
   - FCM message sent to user's device
   - If FCM fails, notification still persists in DB

3. **Android app receives push notification**
   - Notification title (e.g., "Job Assigned")
   - Notification body (message text)
   - Data payload with: notificationId, jobId, notificationType

## Troubleshooting

### Firebase not initializing?
- Check `backend/firebase-service-account.json` exists
- Verify file permissions (should be readable by Docker)
- Check Docker logs: `docker-compose logs backend`

### Android app not receiving notifications?
1. Ensure FCM token is registered: `PUT /api/users/me/fcm-token`
2. Check notifications are being created in database
3. Verify Firebase project setup is correct
4. Enable Firebase Cloud Messaging in your Android app manifest

### Testing notifications
1. Create a new job or trigger a notification via the web UI
2. Check database: notifications should be created
3. Check Android logcat for FCM messages

## File Structure
```
backend/
├── firebase-service-account.json  (NOT in git, you must add this)
├── Dockerfile
└── ...
docker-compose.yml                 (configured to mount the file)
.env.example                        (reference for setup)
.gitignore                          (firebase-service-account.json is ignored)
```

## Important Security Notes
- 🔒 **NEVER commit** `firebase-service-account.json` to Git
- 🔒 **NEVER share** the service account JSON publicly
- ✅ File is protected in `.gitignore`
- ✅ Only readable by Docker container (mounted as read-only)

## Production Deployment
For production, ensure:
1. Service account JSON is stored securely (e.g., environment variables, secret management)
2. Mount it securely into the container
3. Use appropriate IAM permissions for the service account
4. Monitor FCM delivery failures in logs
