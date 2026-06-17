# Frontend Architecture - Quick Reference

## 🗂️ Directory Structure

```
frontend/src/
├── config/
│   └── apiConfig.js                 # API base URL configuration
├── constants/
│   └── jobConfig.js                 # Job types, statuses, endpoints, etc.
├── services/
│   └── apiClient.js                 # Unified API client for all requests
├── utils/
│   ├── dateUtils.js                 # Date manipulation & formatting
│   ├── photoUtils.js                # Photo encoding/decoding
│   └── permissionUtils.js           # Permission checks & role logic
├── components/
├── context/
├── pages/
├── styles/
└── hooks/
```

---

## 📚 Import Patterns

### API Client (Centralized HTTP requests)
```javascript
import apiClient from '../services/apiClient';

// In component initialization
apiClient.setToken(authToken);

// Making requests
const data = await apiClient.get('/api/jobs/pending');
const created = await apiClient.post('/api/jobs', newJob);
const updated = await apiClient.put(`/api/jobs/${id}`, updatedJob);
await apiClient.delete(`/api/jobs/${id}`);
```

### Constants (All magic strings)
```javascript
import {
  JOB_TYPES,
  JOB_STATUSES,
  PRIORITY_LEVELS,
  PRIORITY_COLORS,
  API_ENDPOINTS,
  EMPTY_JOB_FORM,
  DEFAULT_START_HOUR,
} from '../constants/jobConfig';

// Usage
if (job.status === JOB_STATUSES.DONE) { ... }
PRIORITY_LEVELS.map(level => <option>{level.label}</option>)
fetch(API_ENDPOINTS.JOBS_PENDING)
```

### Date Utilities
```javascript
import {
  timestampToDateInput,
  dateInputToTimestamp,
  startOfDay,
  endOfDay,
  isSameDay,
  formatJobTypeLabel,
  getMonday,
} from '../utils/dateUtils';

// Usage
const dateStr = timestampToDateInput(timestamp);
const timestamp = dateInputToTimestamp(dateStr);
const start = startOfDay(new Date());
```

### Photo Utilities
```javascript
import {
  toPhotoSrc,
  extractBase64,
  parsePhotosFromJob,
  photosToPayload,
  sanitizeClientName,
} from '../utils/photoUtils';

// Usage
const src = toPhotoSrc(photoData);
const photos = parsePhotosFromJob(job, 'beforePhotos', 'beforePhoto');
const payload = photosToPayload(photos);
```

### Permission Utilities
```javascript
import {
  getJobPermissions,
  formatWorkers,
} from '../utils/permissionUtils';

// Usage
const perms = getJobPermissions(job, user, isAssignedWorker);
if (perms.canComplete) { ... }
if (perms.canUploadPhotos) { ... }
```

---

## 🔄 Common Operations

### Fetch a Job
```javascript
import apiClient from '../services/apiClient';
import { API_ENDPOINTS } from '../constants/jobConfig';

const job = await apiClient.get(`${API_ENDPOINTS.JOBS}/123`);
```

### Create a Job
```javascript
const newJob = {
  clientName: 'John Doe',
  clientPhone: '0412 345 678',
  clientAddress: '123 Main St',
  jobTypes: 'FULL_REGUTTER',
  priorityLevel: 2,
};
const created = await apiClient.post(API_ENDPOINTS.JOBS, newJob);
```

### Update Job Status
```javascript
import { JOB_STATUSES } from '../constants/jobConfig';

const updated = await apiClient.put(
  `${API_ENDPOINTS.JOBS}/123`,
  { status: JOB_STATUSES.SCHEDULED }
);
```

### Handle Permission-Based UI
```javascript
import { getJobPermissions } from '../utils/permissionUtils';

const permissions = getJobPermissions(job, user, isAssignedWorker);

return (
  <>
    {permissions.canComplete && <button>Complete</button>}
    {permissions.canArchive && <button>Archive</button>}
    {permissions.canUploadPhotos && <input type="file" />}
  </>
);
```

---

## 🚨 Error Handling

API errors have typed classifications:

```javascript
try {
  const data = await apiClient.get('/api/jobs/pending');
} catch (err) {
  if (err.type === 'UNAUTHORIZED') {
    // Handle auth error - redirect to login
    navigate('/login');
  } else if (err.type === 'NETWORK_ERROR') {
    // Handle network error
    setError('Check your internet connection');
  } else if (err.type === 'SERVER_ERROR') {
    // Handle server error
    setError('Server error. Please try again later.');
  } else {
    setError(err.message);
  }
}
```

---

## 📦 Environment Setup

### Create `.env.local` file:
```env
VITE_API_BASE_URL=http://localhost:4000
```

For production:
```env
VITE_API_BASE_URL=https://api.example.com
```

Vite automatically loads from `.env.local` (which is gitignored).

---

## ✅ Checklist for New Components

When creating a new component:
- [ ] Import `apiClient` for API calls
- [ ] Use constants from `jobConfig` (no magic strings)
- [ ] Import utilities instead of duplicating logic
- [ ] Use `getJobPermissions()` for permission checks
- [ ] Set token on apiClient if needed: `apiClient.setToken(token)`
- [ ] Handle errors with proper error types

---

## 🔌 Adding New API Endpoints

1. Add to `API_ENDPOINTS` in `constants/jobConfig.js`:
```javascript
export const API_ENDPOINTS = {
  // ... existing
  NEW_FEATURE: '/api/new-feature',
};
```

2. Use in component:
```javascript
import { API_ENDPOINTS } from '../constants/jobConfig';
const data = await apiClient.get(API_ENDPOINTS.NEW_FEATURE);
```

---

## 📝 Notes

- Always use `apiClient` instead of raw `fetch()` calls
- Always use constants instead of magic strings
- Always extract utility functions to reusable modules
- Permissions should use `getJobPermissions()` helper
- Keep date logic in `dateUtils.js`
- Keep photo logic in `photoUtils.js`
