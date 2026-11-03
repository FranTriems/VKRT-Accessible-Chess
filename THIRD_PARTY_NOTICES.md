# Copyright del código propio

El código propio de VKRT Accessible Chess se atribuye a **FranTriems (VKRT)**, Copyright (c) 2026, bajo la licencia MIT incluida en `LICENSE`, sujeto a los derechos existentes. Consultar `AUTHORS.md` y `ATTRIBUTION.md`.

---

# Third-party notices — v0.5.0-alpha.1

**Stockfish 19 (`sf_19`)**, © Stockfish contributors, official project: https://github.com/official-stockfish/Stockfish . License: **GNU GPL version 3**. This app's Android APK includes official Stockfish 19 binaries for `arm64-v8a` and `armeabi-v7a` as `libstockfish.so` (separate UCI process). Source archive distributed with the release: `Stockfish-sf_19-official-tag-source.zip`, SHA-256 `024a509d7af387218bd59c559473a71a1d6b411885010543891495119a671119`. The license text is in `LICENSES/GPL-3.0.txt` and inside the source archive.

The original binaries are from official GitHub release assets tagged `sf_19`. The app's own C++20/Java code is distributed in the repository and the separate application source ZIP. See `docs/STOCKFISH_PROVENANCE.json` for exact archive and ELF digests and `docs/BUILD_ANDROID_STOCKFISH.md` for build instructions.

The Android platform, installed speech recognition services, TTS, build tools and Gradle are not distributed as application libraries. Android recognition services *may* use external servers, depending on device configuration and consent; the app itself declares no INTERNET permission. We make no claim that upstream Stockfish is developed by VKRT.

Copyright/provenance and combined-work GPL compliance remain subject to maintainer review. This is an alpha release, not a claim of certified accessibility.
