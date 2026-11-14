# Genesis Calendar

Android calendar project. The application source will be added before enabling the GitHub Actions APK build.

Build target: Android SDK 35, Java 17, Gradle 8.9.

Signing: debug APKs from different GitHub Actions runs may use different debug signing keys. For install-over-existing updates use a stable release keystore stored as GitHub Actions secrets, never in the repository.
