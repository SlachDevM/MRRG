# MRRG Manual Test Plan

## User Login

Test 1: On the login page of MRRG, use a correct email address and an incorrect password, then click on the log in button. The user should remain on the same page with the displayed error message "Incorrect email or password".

Test 2: On the login page of MRRG, use an incorrect email address and an incorrect password, then click on the log in button. The user should remain on the same page with the displayed error message "Incorrect email or password".

Test 3: On the login page of MRRG, use a correct email address and the correct password, then click on the log in button. The user should connect to the app and access the dashboard with his username displayed in the top left corner of the window.

Test 4: On the login page of MRRG, use the email address of a disabled user and the correct password, then click on the log in button. The user should remain on the login page and should not access the application.

Test 5: On the login page of MRRG, use the email address of a pending activation user and the correct password, then click on the log in button. The user should remain on the login page and should not access the application.

Test 6: Log in successfully, then refresh the browser page. The user should remain authenticated and stay inside the application.

Test 7: Log in successfully, then click on the logout button. The user should be redirected to the login page and should not be able to access protected pages using the browser back button.

---

## User Management

Test 1: As an admin, open the user management page and create a new employee with a valid email address, name and role. The new user should appear in the user list with the status "Pending Activation".

Test 2: As an admin, try to create a user with an email address that already exists. The user should not be created and an error message should be displayed.

Test 3: As an admin, deactivate an active user. The user should remain visible in the user list with the status "Disabled".

Test 4: As an admin, try to log in with a disabled user account. The login should fail and the user should not access the application.

Test 5: As an admin, reactivate a disabled user who had already activated his account before. The user status should become "Active".

Test 6: As an admin, reactivate a disabled user who never activated his account. The user status should become "Pending Activation".

Test 7: As an admin, click resend activation for a pending activation user. A new activation email should be generated and visible in Mailpit in local development.

Test 8: As a manager or employee, try to access the user management page directly by URL. Access should be denied.

---

## Account Activation

Test 1: Create a new user as admin, open the activation email in Mailpit, then open the activation link. The activation page should be displayed.

Test 2: On the activation page, enter a valid password and confirm it. The account should be activated and the user should be able to log in.

Test 3: On the activation page, enter two different passwords. The account should not be activated and an error message should be displayed.

Test 4: Try to reuse an activation link after the account has already been activated. The activation should fail or be rejected.

Test 5: Try to activate an account with an invalid token. The activation should fail and the user should not be activated.

---

## Job Creation

Test 1: As a manager or admin, create a new job with all required client and job information. The job should be created with the status "Pending".

Test 2: Try to create a job with missing required information. The job should not be created and validation errors should be displayed.

Test 3: Create a job with a valid job date in `yyyy-MM-dd` format. The job should appear on the correct calendar day without timezone shift.

Test 4: Create several jobs for different dates. Each job should appear only on its correct scheduled date.

Test 5: Refresh the page after creating a job. The job should still be visible with the same information.

---

## Job Scheduling

Test 1: As a manager or admin, assign a pending job to one or more workers and schedule it for a date. The job status should become "Scheduled".

Test 2: Try to schedule a job without assigning any worker. The action should be rejected or blocked.

Test 3: Reschedule an existing scheduled job to another date. The job should move to the new date and should no longer appear on the old date.

Test 4: Assign a job to a disabled worker if possible through the UI. The action should be rejected or the disabled worker should not be selectable.

Test 5: Open the weekly view after scheduling multiple jobs. Jobs should appear on the correct days.

---

## Job Workflow

Test 1: As a field worker on Android, open an assigned scheduled job and start the job. The job status should become "In Progress".

Test 2: Complete the job from Android with the required completion information. The job status should become "Waiting Manager Validation".

Test 3: As a manager on React, validate a completed job. The job status should become "Completed" or move to the done jobs section according to the current workflow.

Test 4: As a manager, reject or request a fix for a completed job if the workflow supports it. The job should return to the appropriate correction state.

Test 5: Archive a completed job. The job should disappear from active job lists and appear in the archive.

Test 6: Create a callback from an archived job. The archived job should be restored for follow-up work and should not create a duplicate new job.

---

## Job Archive and Done Jobs

Test 1: As a manager, open the Done Jobs section. Completed jobs should be visible there.

Test 2: Archive a done job. The job should move from Done Jobs to Archive.

Test 3: Open the Archive section. Archived jobs should be visible with their previous information.

Test 4: Trigger a callback on an archived job. The same job should return to the active workflow instead of creating a new job.

Test 5: Refresh the browser after archiving or restoring a job. The job should remain in the correct state.

---

## Notifications

Test 1: Assign a job to a worker. A notification should be created for the assigned worker.

Test 2: Open the notification center as the assigned worker. The new notification should be visible and unread.

Test 3: Mark one notification as read. The notification should be displayed as read and the unread count should decrease.

Test 4: Mark all notifications as read. The unread count should become zero.

Test 5: Disable Firebase or simulate FCM failure, then trigger a notification. The notification should still be saved and visible in the notification center.

Test 6: Log in on Android and verify that the FCM token is sent to the backend.

Test 7: Simulate a refreshed FCM token while the user is logged in. The backend should receive and store the new token.

---

## Android Login and Session

Test 1: On Android, use a correct email address and incorrect password. The user should remain on the login screen with an error message.

Test 2: On Android, use a correct email address and correct password. The user should access the mobile dashboard.

Test 3: Log in on Android, close the app, then reopen it. The user should remain logged in if the session is still valid.

Test 4: Log out on Android, then reopen the app. The user should remain logged out.

Test 5: Disable the user from the web admin panel, then try to use the Android app with the existing session. Backend-protected actions should be rejected.

---

## Android Offline Behaviour

Test 1: Log in on Android while online, load assigned jobs, then turn off the network. Cached jobs should remain visible.

Test 2: While offline, try to perform an action supported by offline mode. The action should be stored locally and clearly reflected in the UI.

Test 3: Restore the network connection. Pending local actions should synchronize automatically with the backend.

Test 4: Turn off the network during a job completion action. The app should not crash and should either retry later or clearly indicate that the action is pending.

Test 5: Kill and reopen the Android app while offline. Cached data and pending actions should still be available.

---

## Android Photos

Test 1: Open a job requiring photos and take a before photo. The photo should be displayed correctly in the job detail screen.

Test 2: Take an after photo and complete the job. The photos should be sent to the backend when online.

Test 3: Try to complete a job without required photos if photos are mandatory. The app should prevent completion or display a clear validation message.

Test 4: Take photos while offline. The app should store them safely and synchronize them when the network is restored.

Test 5: Deny camera permission, then try to take a photo. The app should display a clear message and should not crash.

---

## Date Handling

Test 1: Create a job in React for today. The job should appear on today’s date in React and Android.

Test 2: Create a job for tomorrow. The job should not appear under today’s jobs.

Test 3: Create a job near midnight local time. The job date should not shift to the previous or next day.

Test 4: Verify the API payload for job date. The value should be sent as `yyyy-MM-dd`, not as a timestamp.

Test 5: Verify that technical timestamps such as `createdAt` and `updatedAt` still behave normally.

---

## Permissions and Roles

Test 1: Log in as admin. The user should access user management, job management and administration features.

Test 2: Log in as manager. The user should access manager job workflows but should not have full admin-only access if restricted.

Test 3: Log in as employee. The user should only access assigned work and employee-allowed actions.

Test 4: Try to call an admin-only backend endpoint using a non-admin token. The backend should reject the request.

Test 5: Hide a UI button in React, then try the same action directly through the API with insufficient permissions. The backend should reject the action.

---

## Production Configuration Smoke Tests

Test 1: Start the local environment with Docker Compose. Backend, frontend, PostgreSQL and Mailpit should start successfully.

Test 2: Open the frontend locally and log in successfully.

Test 3: Create a user and verify that the activation email appears in Mailpit.

Test 4: Run the backend with production profile without required production secrets. The app should fail safely where required, especially for missing production JWT secret.

Test 5: Verify that production CORS does not allow wildcard origins.

Test 6: Verify that production Hibernate configuration remains `validate`.

---

## Regression Checklist Before Release

Test 1: Run backend tests. All tests should pass.

Test 2: Build the React frontend. The production build should complete successfully.

Test 3: Build Android debug. The debug build should complete successfully.

Test 4: Build Android release. The release build should complete, except for signing if production signing credentials are intentionally missing.

Test 5: Run Docker Compose config validation for development and production compose files. Both should be valid.

Test 6: Search the codebase for obsolete `Task` references. No obsolete Task feature should remain.

Test 7: Search the codebase for business job dates stored as timestamps. Business job dates should use `yyyy-MM-dd` / `LocalDate`.

Test 8: Search the codebase for hardcoded secrets. No real secret should be committed.
