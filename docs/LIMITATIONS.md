# Android Platform Constraints & Limitations

HexZie adheres strictly to Android security architecture and Google Play platform policies. This document details the platform constraints and how HexZie complies with them.

---

## 1. Silent Installation & Elevated Actions
- **Platform Reality**: Android does not permit third-party applications to silently install or update APKs without user confirmation. 
- **HexZie Implementation**: When `install_app` is invoked, HexZie hands the APK package to the Android PackageInstaller system dialog where the user must tap "Install". Silent root/exploit methods are forbidden.
- **Optional Shizuku Support**: For advanced power users who have enabled Shizuku (ADB-level privileges), `pm install` can be dispatched with user-authorized Shizuku bindings.

## 2. Background Microphone & Camera Start
- **Platform Reality**: Beginning with Android 11 (API 30) and strengthened in Android 14/15, apps cannot start microphone or camera foreground services directly from the background without a user interaction or an explicit notification intent.
- **HexZie Implementation**: HexZie starts `HexzieVoiceService` while the application is in the foreground, showing a persistent notification (`foregroundServiceType="microphone"`). If restarted post-boot, HexZie posts a user-visible notification prompt to resume voice listening.

## 3. Accessibility Services & Restricted Settings (Android 13+)
- **Platform Reality**: Sideloaded APKs on Android 13+ have accessibility services disabled by default under "Restricted settings".
- **HexZie Implementation**: In the Onboarding guide, HexZie explains how the user can enable restricted settings in Android Settings -> Apps -> HexZie -> Top-right 3 dots -> "Allow restricted settings".

## 4. Secure Windows (FLAG_SECURE)
- **Platform Reality**: Banking apps, password managers, and DRM video players set `WindowManager.LayoutParams.FLAG_SECURE`.
- **HexZie Implementation**: HexZie honors `FLAG_SECURE`. Screen captures will return blank pixels and accessibility node inspection is restricted. HexZie gracefully reports to the user that the active screen is protected.

## 5. Screen Capture (MediaProjection)
- **Platform Reality**: Starting a live screen capture session requires the user to approve the Android system MediaProjection consent dialog every time.
- **HexZie Implementation**: HexZie prompts the user via the system dialog and never attempts to bypass or overlay this prompt.
