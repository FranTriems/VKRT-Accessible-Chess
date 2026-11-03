# Build and corresponding-source instructions (v0.5.0-alpha.1)

## Host tests

Requires a C++20 compiler, CMake and CTest:

```sh
cmake -S native -B build-host -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build-host
ctest --test-dir build-host --output-on-failure -C Release
```

## Android

Requires a locally installed JDK 17, Android SDK platform 34, NDK 27.0.12077973, CMake 3.22.1, Android Gradle Plugin 9.4.1 and compatible Gradle. Local build tool versions are declared under `android/app/build.gradle`; Gradle may require its dependencies cached or an authorized download. No cloud accounts, AI models or proprietary libraries are required to run the app.

The app source does not store or commit GPL-covered engine binaries. To reconstruct the **Stockfish-enabled** edition:

1. Download the official Stockfish 19 Android ARM64 and ARMv7 NEON archives from https://github.com/official-stockfish/Stockfish/releases/tag/sf_19 . Their hashes must match `docs/STOCKFISH_PROVENANCE.json`.
2. Place them in a single local directory. Run `python scripts/stage_stockfish.py --archives PATH_TO_ARCHIVES --project . --metadata docs/STOCKFISH_PROVENANCE.json`.
3. Supply your local Android SDK/JDK environment. From `android/`, run `gradle --offline :app:assembleDebug` if the build dependencies are cached. Otherwise obtain matching versions through your normal trusted build tool setup.
4. The resulting debug APK is `android/app/build/outputs/apk/debug/app-debug.apk`. Its hash may differ from the release APK due to timestamps, signing and toolchain differences; this does not automatically mean the source differs.

For the core-only edition, omit staging the Stockfish binaries; the application must handle their absence gracefully.

Stockfish source code is attached to the release as `Stockfish-sf_19-official-tag-source.zip` from the upstream tag `sf_19` with SHA-256 `024a509d7af387218bd59c559473a71a1d6b411885010543891495119a671119`. Its `src/Makefile`, network/model assets and build instructions belong to that archive. The app code, JNI/Java integration and staging scripts are in this repository. If you distribute modified binaries, also distribute the corresponding modified source and build scripts.

Note: the developer's original R31 build used a pre-existing Android SDK/NDK and Gradle cache. Exact bit-for-bit reproducibility has not been demonstrated. Do not assume source release equals audited production build.
