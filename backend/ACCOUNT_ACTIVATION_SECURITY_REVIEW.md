# Account Activation Security Review & Hardening

## Summary of Changes

This document outlines the security review and hardening of the account activation workflow to ensure proper protection of sensitive tokens and restricted access to public registration endpoints.

## 1. Public Registration Restriction

### Issue
The `POST /api/auth/register` endpoint was publicly accessible and allowed immediate account activation (enabled=true), bypassing the controlled account activation workflow.

### Solution
- **Modified `AuthService.register()`**: New user registrations are now created with `enabled=false`, requiring email activation
- **Modified `AuthController`**: Added a `RegisterEndpointProperty` inner class that checks the active profile
  - **Development profiles** (dev, development, local): Endpoint is ENABLED for testing
  - **Production profiles** (prod, production, etc.): Endpoint returns `403 Forbidden` with message: "Public registration is disabled. Contact an administrator to request account activation."

### Security Impact
✓ Prevents unauthorized public account creation in production
✓ Enforces controlled account activation workflow
✓ Allows bootstrap testing in development environments

## 2. Existing Users Migration (Backward Compatibility)

### Issue
The new `enabled` field defaulted to `false` in Java code, but existing users in the database would have NULL values, potentially locking them out.

### Solution
Updated `DatabaseSchemaUpdater` to:
1. Add column with safe default: `ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE`
2. Explicitly migrate any NULL or FALSE values: `UPDATE users SET enabled = TRUE WHERE enabled IS NULL OR enabled = FALSE`

### Security Impact
✓ Existing users remain enabled and can log in after migration
✓ Backward compatible with existing databases
✓ No accidental account lockouts

## 3. Email Activation Token Logging

### Issue
Activation tokens containing sensitive URL information were being logged in all environments, potentially exposing tokens in production logs.

### Solution
- Enhanced `EmailService` to ONLY log activation links in development profiles
- Production profiles use `sendEmailViaSMTP()` which does not log the token
- Added explicit security warnings in code comments

### Logging Behavior
- **Development** (dev, development, local): Logs activation link with clear "Development Mode Only" header
- **Production** (all other profiles): Logs warning that email is not configured but does NOT log the token

### Security Impact
✓ Activation tokens never exposed in production logs
✓ Prevents token leakage through log aggregation systems
✓ Clear indication of environment in log messages

## 4. Security Check: Inactive User Authentication

### Implementation
- `AuthService.login()` checks `user.getEnabled()` before issuing JWT tokens
- Inactive users receive: `403 Forbidden` with message "Account is not activated"
- No JWT token is issued to inactive users

### Verification
✓ Tested in `AuthServiceTest.login_shouldThrowForbidden_whenAccountNotActivated()`
✓ Inactive users cannot authenticate
✓ Inactive users cannot receive JWT tokens

## 5. Test Coverage

### New Tests Added

#### AuthControllerTest.java
- Profile-based endpoint availability tests
- Registration disabled/enabled validation
- Login and activation endpoint tests

#### EmailServiceTest.java
- Profile detection for development vs production
- Token logging behavior verification
- Link formatting for deep links and HTTPS URLs

#### DatabaseSchemaUpdaterTest.java
- Backward compatibility verification
- Enabled column migration
- Data preservation checks

#### Updated AuthServiceTest.java
- Modified: `register_shouldCreateEmployeeByDefault_whenRoleIsNull()` to verify `enabled=false`
- Existing: `login_shouldThrowForbidden_whenAccountNotActivated()` test

### Test Results
✅ BUILD SUCCESS - All tests pass
✅ No regressions in existing tests
✅ New security scenarios covered

## 6. Configuration Properties

### application.properties
```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
app.activation-token-expiration-hours=${ACTIVATION_TOKEN_EXPIRATION_HOURS:24}
app.activation-link-base-url=${ACTIVATION_LINK_BASE_URL:mrrg://activate-account}
```

### Environment Variables
- `SPRING_PROFILES_ACTIVE`: Controls registration endpoint availability
- `ACTIVATION_TOKEN_EXPIRATION_HOURS`: Token lifetime (default: 24 hours)
- `ACTIVATION_LINK_BASE_URL`: Base URL for activation links (default: deep link)

## 7. Workflow Summary

### For New Employees (Controlled Activation)
1. Manager/Admin calls: `POST /api/users/invitations`
2. Backend creates user with `enabled=false`
3. Backend generates secure activation token
4. Backend sends activation email (logs link in dev, sends email in prod)
5. Employee receives link and calls: `POST /api/auth/activate-account` with token + password
6. Backend validates token, sets password, enables account, invalidates token
7. Employee can now log in normally

### For Direct Registration (Development Only)
1. Client calls: `POST /api/auth/register` (only works in dev/development/local)
2. Backend creates user with `enabled=false`
3. Backend returns LoginResponse but user still cannot log in
4. User must complete activation flow via email token

## 8. Security Best Practices Applied

1. ✅ **Principle of Least Privilege**: Registration restricted to dev environments in production
2. ✅ **Token Security**: No tokens logged in production
3. ✅ **Backward Compatibility**: Existing users automatically enabled
4. ✅ **Clear Error Messages**: Users know why they can't register/log in
5. ✅ **Profile-Based Configuration**: Different behavior per environment
6. ✅ **Test Coverage**: Security scenarios explicitly tested
7. ✅ **Defense in Depth**: Multiple checks (endpoint disable + login check)

## Deployment Checklist

- [ ] Set `SPRING_PROFILES_ACTIVE=prod` (or production environment name) in production
- [ ] Do NOT expose `POST /api/auth/register` in production
- [ ] Configure real email service in production (Spring Mail integration)
- [ ] Set `ACTIVATION_LINK_BASE_URL` to your domain or deep link scheme
- [ ] Review and approve all email templates before production
- [ ] Monitor activation failures and email delivery
- [ ] Test complete activation flow in staging environment
- [ ] Verify existing users can still log in after deployment
