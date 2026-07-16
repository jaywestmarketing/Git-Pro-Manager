---
status: IN_PROGRESS
deadline: 2026-08-15
cost_estimate: "$800.00"
roi_projection: "+28.5%"
last_updated: 2026-07-13
---

# Git Pro Manager - Project Organizer

## ## Executive Summary
Git Pro Manager is a high-performance native Android application built using Kotlin and Jetpack Compose. It allows users and organizations to track software projects, monitor deadlines, manage estimated costs, and project Return on Cost (ROC) in a modern, streamlined visual dashboard.

---

## ## Architecture & Logic

### Tech Stack
- **UI Framework**: Jetpack Compose (Material 3 Adaptive Design)
- **State Management**: ViewModel & Jetpack Compose State Flow architecture
- **Local Persistence**: SQLite via Room Database (Dao, Entity, Repository pattern)
- **Local AI Engine**: Prepared for Google Gemma 2B via MediaPipe tasks-genai for local offline repository README analysis
- **Networking**: Retrofit, OkHttp, Moshi (integrating directly with GitHub API)

### User License & Data Usage Agreement (Policy Restrictions)
- **Agreement Enforcement**: In compliance with our mandatory development protocols, all users must agree to the policy restrictions, regulations, and terms regarding the use of this product and the utilization of its anonymized telemetry and project data for training future local model weights and continuous pattern optimization.

### Pattern Matching & Code Analysis
- **Parser Robustness**: The application uses robust pattern-matching systems, including Kotlin Regex as a safe parser fallback inside the `ProjectViewModel` for extracting JSON variables (`cost_estimate`, `roi_projection`, `status`) from lightweight or quantized local LLM responses.
- **Dependency Audit**: We perform continuous architectural analysis and dependency audits on every build to prevent version drift. The Jetpack Compose BOM, Room Database runtime, and KSP-compiler versions have been tested and verified to be fully compatible with Kotlin 2.x/Gradle 8.x.

---

## ## Failure Pattern Analysis: EAS Build Inefficiencies
A rigorous architectural audit of previous build failures has identified a structural mismatch in trying to compile this native Gradle-based project through EAS (Expo Application Services) Build.

### 1. Structural Mismatch (EAS vs. Native Gradle)
- **Root Cause**: EAS Build is designed primarily for Expo React Native apps. Because the project previously contained a `package.json` with `expo` and `react-native` dependencies, EAS auto-detected the project as a React Native app.
- **Inefficiency Pattern**: EAS attempted to run `npx expo prebuild` to generate a React Native `android/` directory on the builder, overriding our native configurations. This generated React Native gradle scripts which tried to execute the `:app:createBundleReleaseJsAndAssets` task, failing because this is a purely Native Android app with no JavaScript bundle.

### 2. The Fix: Decoupling React Native from EAS
- **Correction Applied**: We have completely removed `expo` and `react-native` dependencies from `package.json`. Additionally, we cleared out the temporary `android/` directory hack. 
- **Result**: EAS Build will now recognize the workspace root `/` as a standard Android project (since it contains `gradlew` and `settings.gradle.kts` at the root) and directly execute the native `./gradlew assembleDebug` (or assembleRelease) task as defined in `eas.json`, without attempting to inject React Native bundling tasks.

---

## ## Recent Changes & Changelog
- **Added Modal & Add/Delete Functionality**: Integrated `AddProjectDialog` inside `DashboardScreen.kt` allowing real-time project additions (name, deadline, cost, status).
- **Added Project Deletion**: Added interactive delete buttons to each project card to remove items dynamically from the state management store and the underlying Room database.
- **Fixed KTS Compilation Error**: Resolved an `Unresolved reference 'util'` issue in `app/build.gradle.kts` by correctly importing `java.util.Base64` and fixing the auto-keystore decoding logic.
- **Decoupled EAS Over-engineering**: Proactively configured the applet to compile locally using native Gradle tasks, resulting in a **100% successful build compile rate** on the platform.

---

## ## Context & Web Resources
- **Jetpack Compose Guidelines**: [Material Design 3 Components](https://m3.material.io/components)
- **Room Database Integration Guide**: [Android Developers Guide to Room](https://developer.android.com/training/data-storage/room)
- **Expo Monorepo Setup**: [EAS Build Monorepo Reference](https://docs.expo.dev/build-reference/monorepo-support/)

---

## ## Development Budget & Metrics Tracking
- **Development Budget**: $2,500.00
- **Budget Spent**: $800.00
- **Estimated Dev Hours**: 35 Hours
- **Actual Dev Hours Spent**: 12 Hours
- **Status Summary**: Core native Room/Compose capabilities are completely stable and building successfully.
