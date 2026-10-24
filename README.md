# Namaz Streak — Android project

This is a native Android Studio starter project (Kotlin + Jetpack Compose), not a precompiled APK.

## Build the APK
1. Install Android Studio on a computer.
2. Open this folder in Android Studio and let Gradle sync.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.
5. Transfer it to your Android phone and install it. Android may ask you to allow installs from that source.

## Included
- Native Android UI
- Five daily prayer checkboxes
- Local persistence with SharedPreferences
- Current/longest streak summary and milestones
- Notification channel and reminder scheduling starter

## Important limitations before daily use
- Reminder times are example defaults, not verified prayer times. Change them for your local timetable.
- This starter schedules the next occurrence only; repeating daily reminders need rescheduling after each alarm or a robust exact-alarm/WorkManager implementation.
- Android 13+ asks for notification permission.
- The calendar/history screen and editable per-prayer times are not yet implemented in this starter.
- For a production release, test on-device and review Android exact-alarm / battery restrictions.
