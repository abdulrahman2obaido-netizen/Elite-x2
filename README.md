# ELITE X — Snapdragon Gaming Performance Suite

Native Android app (Java) with Shizuku UserService, genuine Android Game Mode commands, system statistics, movable CPU/GPU HUD overlay, SAF file manager / editor, automatic backup & restore, cache controls, optional background process cleanup, and network diagnostic.

## Build the APK

**Easy: GitHub Actions** — upload this folder as a GitHub repository, open **Actions → Build ELITE X APK → Run workflow**, then download artifact **EliteX-debug-apk**. Its `app-debug.apk` is installable on Android 10+ (minimum API 29).

**Android Studio** — open `EliteX/`, install Android SDK 35 and JDK 17, sync Gradle, choose **Build → Build APK(s)**. Output: `app/build/outputs/apk/debug/app-debug.apk`. Gradle 8.9, Android Gradle Plugin 8.7.3, dependencies from Maven Central and Google Maven. The build requires Internet for the initial dependency download.

## Device setup

1. Install Shizuku from https://shizuku.rikka.app/download/.
2. Start Shizuku via Wireless Debugging / ADB, or root where available. Grant ELITE X Shizuku access.
3. Grant **Display over other apps** and notification permission for the overlay.
4. Select an installed game in **GAMES**. Apply Performance/Standard/Battery from **BOOST**.
5. Choose a user-accessible directory in **FILES**. Files are opened through Android Storage Access Framework, not full system storage.

## Real technical limitations — no simulated numbers

- The app does not need root for supported Game Mode ADB-shell features, but not all ROMs/games support every operation. It always surfaces command failures.
- `GPU` utilization only appears where the device's driver exposes a readable KGSL sysfs metric to Shizuku's identity. Unsupported devices show **N/A**. CPU can also show N/A when procfs is blocked; readings are actual samples, never fabricated.
- The overlay's RAM is real system RAM used/total; thermal state comes from Android; the displayed temperature is **battery** temperature, not CPU/GPU temperature.
- `am kill-all` requests Android to kill *safe-to-kill* background processes, not services or all apps. Some restart. Do not expect FPS improvements.
- Standard `pm trim-caches 2G` and optional deep `pm trim-caches 512G` ask Android to free disposable cache toward target free-space levels; doesn't guarantee bytes freed or clean RAM. Local app cache is separately deleted.
- Game Mode is Android's official `cmd game mode [standard|performance|battery] package` shell command; changing it doesn't imply overclocking or a guaranteed FPS/latency change.
- Fixed Performance Mode is designed for benchmark repeatability, not maximum performance; may heat up device. Always turn off after testing.
- SAF cannot bypass Android's restrictions on `Android/data`, `/data/data`, OBB or other protected files. Don't expect access to private game files without root / special access.
- Unreal Engine `.ini` presets are examples and may be ignored by games. The editor requires explicit Save and backs up the original into internal app storage before writing; restoring also creates a new backup.
- Network diagnostic measures TCP connection setup to 1.1.1.1:443, **not** actual game ping. It does not route/optimize network traffic.
- Only explicitly allowlisted operations can be executed in the privileged service. Package input is validated. Force-stop requires the user's confirmation and blocks Android core packages.
- Side-load APK debug builds for personal testing; review safety and compatibility before publication.

## Project tree

- `MainActivity.java` — dashboard, navigation, performance profiles, app picker, confirmation screens.
- `OverlayService.java` — foreground CPU/GPU/RAM overlay that can be dragged or stopped.
- `EliteUserService.java` — Shizuku (shell identity) implementation and hardware sampling.
- `ShizukuBridge.java` — permission flow, user service binding, async IPC.
- `GameFileManager.java` — SAF folders, text editor, presets, backup and restore.
- `DeviceStats.java` — MemoryInfo, thermal API, battery temperature, local cache cleanup.
- `.github/workflows/build-apk.yml` — automatic debug APK build.

## Verification

The Java files passed a syntax-only parser check and all Android XML files passed an XML validity check. The application **has not been compiled or executed on an Android device** in this environment, because the Android SDK and build dependencies are unavailable here. The included CI pipeline is the recommended APK-generation method; test on the target handset.

## License

Original source authored for this request. Shizuku libraries retain their own upstream license and attribution.
