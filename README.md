# VKRT Accessible Chess

[English](README.md) | [Español](README.es.md)


**Open-source, voice-first chess for Android — built around accessibility, a native C++20 chess core, and an optional Stockfish engine.**

Created and maintained by **Francisco Triemstra ([FranTriems](https://github.com/FranTriems))**, independent developer from Argentina and creator of **VKRT**.

[**Download the Android alpha**](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1) · [**Source code**](https://github.com/FranTriems/VKRT-Accessible-Chess) · [**Report an issue**](https://github.com/FranTriems/VKRT-Accessible-Chess/issues)

> **Alpha release — v0.5.0-alpha.1.** The Android APK has been installed and voice move recognition has been exercised on a Samsung Galaxy A20. This is an experimental preview, **not an accessibility-certified or production-signed release**. Complete independent, no-vision testing with TalkBack is still pending.

## What is VKRT Accessible Chess?

VKRT Accessible Chess is a native Android chess application designed to reduce reliance on visual controls. Instead of navigating a crowded interface or dragging pieces, players can use spoken instructions to explore the position, propose a move, review options, and confirm their choice.

The project preserves a **standalone VKRT Chess Core written in C++20**. An optional **Stockfish 19** integration provides advanced move suggestions through UCI, without replacing the project's own chess rules and game-state engine.

The goal is simple: **make playing chess possible with less visual and tactile complexity**, while remaining open to contributions from blind players, accessibility testers, and developers.

## Features

- **Voice-first interaction:** a single touch surface supports press-and-hold speech input; the accessibility interaction has a separate TalkBack path.
- **Spoken chess commands:** request help, inspect the board, list legal options, choose a move, and confirm or cancel without tapping individual squares.
- **Visible chessboard:** a compact board remains available for sighted players, but is not required for the main voice workflow.
- **Native C++20 chess core:** legal move validation and game-state management remain independent of Stockfish.
- **Move confirmation:** recognized speech cannot directly execute a chess move without a review/confirmation step.
- **Optional Stockfish 19:** local UCI-based suggestions with the upstream engine attributed under its own GPLv3 license.
- **Android-native implementation:** Java framework UI, JNI bridge, and CMake-built native code; no React Native or web runtime in the distributed application.
- **Open-source engineering:** source, tests, build guidance, licensing notices, and release checks are available for inspection.

## Language support

**Published v0.5.0-alpha.1:** Spanish (`es-AR`) voice interface only.

**R34 / v0.5.0-alpha.2-rc1 source candidate on main (bilingual APK not released):** selectable Spanish (`es-AR`) and English (`en-US`) interface, speech-recognition language request, spoken commands, contextual help and local TTS voice selection. The selection persists across restarts. The game saves and independent C++20 core remain unchanged. Recognition and voice availability depend on installed Android services, and English speech has **not yet been tested on a physical device**.

Switch languages by saying **“idioma inglés”**, **“idioma español”**, **“switch to English”**, or **“switch to Spanish”**, as recognized by the active speech provider. You can also say **“idioma”** or **“language”** to open the accessible chooser. TalkBack users have a **Choose language / Elegir idioma** accessibility action. Android speech services may not recognize a foreign-language switching phrase reliably; the chooser is the deterministic fallback.

## Voice commands — Spanish / English (R34 candidate)

| Say | What it does |
| --- | --- |
| `presentación` | Repeat the introduction |
| `ayuda` | Explain available commands |
| `e2 e4` or `peón de e2 a e4` | Propose a move |
| `origen e2`, then `destino e4` | Compose a move step by step |
| `opciones` | Read available legal moves |
| `opción dos` | Review the second listed move |
| `siguientes` / `anteriores` | Browse additional move options |
| `confirmar` / `cancelar` | Accept or reject a pending action |
| `tablero` / `fila dos` | Hear the board or a selected rank |
| `estado` / `historial` | Hear the current state or move history |
| `sugerencia` | Request an optional Stockfish suggestion |
| `nueva partida` | Request a new game, with confirmation |

English examples available in the R34 candidate: `help`, `introduction`, `board`, `rank two`, `status`, `history`, `legal moves`, `option two`, `next`, `previous`, `from e2 to e4`, `pawn from e2 to e4`, `confirm`, `cancel`, `new game`, `undo`, `suggestion`, `language Spanish`. The published alpha.1 APK remains Spanish-only.

The app depends on an Android speech recognition service. Some services may process audio remotely; availability, privacy behavior, and microphone permissions depend on the service and device settings. The app does not require Stockfish to validate its own chess moves.

## Download and installation

Download the **Android debug APK** from the [v0.5.0-alpha.1 release](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1).

The APK is an **experimental debug build**, not a Play Store or production-signed release. Install it only if you understand the preview status. Check the accompanying `SHA256SUMS.txt` when verifying downloaded artifacts. The release also contains corresponding source material, including the separately licensed Stockfish sources.

**Platform:** Android 8.0 (API 26) or newer, with `armeabi-v7a` or `arm64-v8a` support. Device-specific voice-service compatibility is not guaranteed.

## Architecture

| Directory | Purpose |
| --- | --- |
| `native/` | Independent C++20 chess rules, session handling, and tests |
| `android/` | Java Android UI, voice interaction, board view, JNI, and host tests |
| `scripts/` | Stockfish binary staging and provenance checks |
| `docs/` | Build instructions, accessibility status, release notes, and engine provenance |

### Run the native tests

With CMake, Ninja, and a C++20 compiler installed:

```sh
cmake -S native -B build-host -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build-host --config Release
ctest --test-dir build-host -C Release --output-on-failure
```

The R31 release qualification reported **8/8 native CTest cases passing**, together with Java command-grammar, UCI transport, and voice-first host tests. R34 adds bilingual parser and presentation regression tests; do not infer device acceptance from host tests. These results are **not** a substitute for device or accessibility testing.

To build the Android app with Stockfish, consult [`docs/BUILD_ANDROID_STOCKFISH.md`](docs/BUILD_ANDROID_STOCKFISH.md) and [`docs/STOCKFISH_PROVENANCE.json`](docs/STOCKFISH_PROVENANCE.json). The official Stockfish archives must be obtained and staged according to their verified provenance; they are not committed as application source binaries.

## Accessibility status and roadmap

**Current operator observation:** the published Spanish alpha starts on a Samsung Galaxy A20 and recognizes spoken chess moves. English is a development candidate pending device verification. **Not yet qualified:** completing a game independently with TalkBack and without vision, speech feedback timing, permission and failure recovery across devices, and the complete guided experience.

Priorities include better conversational guidance, independent blind-user testing, clearer speech feedback, broader Android compatibility, and real-device **English-language qualification**. See [`docs/ACCESSIBILITY_STATUS.md`](docs/ACCESSIBILITY_STATUS.md).

Please report accessibility barriers through [GitHub Issues](https://github.com/FranTriems/VKRT-Accessible-Chess/issues). Avoid posting personal recordings, private voice transcripts, or identifying information in public issues.

## Open source, author credit, and third-party licenses

**Project-owned source code:** offered under the [MIT License](LICENSE), where the contributor has the necessary rights.

**Project author:** **Francisco Triemstra — FranTriems (VKRT)**. Copyright **© 2026 FranTriems (VKRT)**. Reusers of the MIT-licensed source must retain the applicable copyright and license notice in copies or substantial portions of the software. See [`AUTHORS.md`](AUTHORS.md) and [`ATTRIBUTION.md`](ATTRIBUTION.md). MIT does not, by itself, require an in-app credit screen in every derived application.

**Stockfish 19:** © **Stockfish contributors**, licensed under **GPLv3**. Stockfish is an independent upstream project; VKRT does not claim authorship of its engine. A combined APK containing Stockfish is subject to the applicable GPL obligations. Consult [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) and [`LICENSES/GPL-3.0.txt`](LICENSES/GPL-3.0.txt) for details and corresponding-source information.

## Development and contributions

VKRT Accessible Chess is developed with **AI-assisted engineering governed by VKRT**: isolated changes, pinned inputs, native and host tests, independent evidence, and explicit acceptance gates. Passing automated tests is not presented as proof of real-world accessibility.

Contributions, reproducible bug reports, localization work, and accessibility feedback are welcome. Please preserve third-party authorship, license notices, and user privacy.

---

**Created by [Francisco Triemstra (FranTriems)](https://github.com/FranTriems) · VKRT · Argentina**

*Built to make chess more accessible, one move at a time.*
