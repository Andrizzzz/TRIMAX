# Implementation Plan: Extract Hardcoded Strings & Improve Accessibility

This plan outlines the refactoring of hardcoded strings and content descriptions from key XML layout files into `res/values/strings.xml`, improving localization support and accessibility across the TRIMAX app.

## User Review Required

> [!IMPORTANT]
> This refactoring will extract hardcoded UI text and content descriptions from core layout files into `strings.xml`. No business logic or Java/Kotlin code will be altered.

## Open Questions

- None.

## Proposed Changes

### Core Layouts & String Extraction

#### [MODIFY] [strings.xml](file:///C:/Users/lance/AndroidStudioProjects/TRIMAX2/app/src/main/res/values/strings.xml)
- Add string resources for Profile, Dashboard, Login, and Booking layouts.

#### [MODIFY] [activity_profile.xml](file:///C:/Users/lance/AndroidStudioProjects/TRIMAX2/app/src/main/res/layout/activity_profile.xml)
- Replace hardcoded `android:text` and `android:contentDescription` attributes with `@string/...` references.

#### [MODIFY] [activity_login.xml](file:///C:/Users/lance/AndroidStudioProjects/TRIMAX2/app/src/main/res/layout/activity_login.xml)
- Replace hardcoded text and descriptions with `@string/...` references.

#### [MODIFY] [activity_dashboard.xml](file:///C:/Users/lance/AndroidStudioProjects/TRIMAX2/app/src/main/res/layout/activity_dashboard.xml)
- Replace hardcoded text and descriptions with `@string/...` references.

#### [MODIFY] [activity_bookings.xml](file:///C:/Users/lance/AndroidStudioProjects/TRIMAX2/app/src/main/res/layout/activity_bookings.xml)
- Replace hardcoded text and descriptions with `@string/...` references.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure all layout references compile successfully.
- Run `./gradlew lintDebug` to verify hardcoded string warnings are resolved for refactored layouts.

### Manual Verification
- Verify layouts render correctly in Android Studio preview or on emulator/device.
