# System Context

## Project
Amazon IAP Kotlin — a demonstration of the Amazon In-App Purchasing (IAP) API on Amazon Fire Tablets.

## Architecture
- Kotlin-based Android app (`app` module) + bridge abstraction layer (`bridge` module)
- Payment / IAP integration via Amazon's `PurchasingService` static API
- `IapService` interface wraps `PurchasingService` static calls; `RealIapService` is the production implementation
- `CoherenceBridge` accepts an `IapService` (defaulting to `RealIapService`) for dependency injection and JVM unit testability
- ViewBinding enabled in the `app` module

## Key Constraints
- No breaking API changes
- Must support offline fallback
- Secure all payment flows
- JVM unit tests use MockK 1.13.8 + JUnit 4.13.2; `testOptions { unitTests.returnDefaultValues = true }` in `bridge/build.gradle`
- Kotlin version: 1.9.24; `jvmTarget`/`VERSION_11` in `bridge/build.gradle`

## Patterns
- Prefer composition over inheritance
- Use existing utility classes (`IapService`, `RealIapService`)
- Inject dependencies rather than calling statics directly

## Known Risks
- Null pointer issues in payment flow
- Async race conditions in purchase response handlers
