# Android packaging

Android application: Android framework Java shell + C++20 core via JNI. No AndroidX/Compose/React Native/Expo/npm. The app has **no INTERNET permission**. It uses the device's installed TTS service; its availability and voices are not guaranteed.

Pinned build references: Android Gradle Plugin 9.4.1 + Gradle 9.6.0; JDK 17+, Android SDK 36, compatible NDK and CMake. These are **build tools**, not runtime dependencies. Do not download or install automatically in the VKRT onepass.

There is no Gradle wrapper jar committed, intentionally. If compatible Gradle is already installed, run `gradle --offline :app:assembleDebug` in this directory, otherwise record `HOLD_ANDROID_TOOLCHAIN_NOT_BOUND`. Do not claim an APK built on the basis of host CMake tests. An Android debug APK is a *development artifact*, not a certified accessibility release.

Original legacy React Native/Expo code is kept as private reference at `../reference_legacy/`, outside Android packaging and public release scope.
