    ██╗   ██╗██╗  ██╗██████╗ ████████╗
    ██║   ██║██║ ██╔╝██╔══██╗╚══██╔══╝
    ██║   ██║█████╔╝ ██████╔╝   ██║
    ╚██╗ ██╔╝██╔═██╗ ██╔══██╗   ██║
     ╚████╔╝ ██║  ██╗██║  ██║   ██║
      ╚═══╝  ╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝

# VKRT Accessible Chess

[English](README.md) | [Español](README.es.md)


**Open-source, voice-first chess for Android — built around accessibility, a native C++20 chess core, and an optional Stockfish engine.**

Created and maintained by **Francisco Triemstra ([FranTriems](https://github.com/FranTriems))**, independent developer from Argentina and creator of **VKRT** Software Factory.

[**Download v0.5.0-alpha.2 (English / Español)**](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.2) · [**Source code**](https://github.com/FranTriems/VKRT-Accessible-Chess) · [**Report an issue**](https://github.com/FranTriems/VKRT-Accessible-Chess/issues)

> **Current pre-release — v0.5.0-alpha.2 (English / Español).** Both languages are implemented in the downloadable experimental Android debug APK. Spanish voice move recognition was exercised on a Samsung Galaxy A20 with the earlier alpha; **English recognition and this bilingual APK have not yet been tested on a physical device**. This is not a production-signed or accessibility-certified release. Independent no-vision and TalkBack acceptance remain pending.

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

**Published v0.5.0-alpha.2:** selectable **English (`en-US`) and Spanish (`es-AR`)** interface text, spoken command grammar, context-sensitive help, recognition-language request and TTS voice selection. Your language choice persists across restarts. The independent C++20 game core and Stockfish integration remain unchanged.

**Testing boundary:** the Android build and bilingual command tests passed, but **English recognition on a real device, complete TalkBack interaction, and independent blind-user acceptance remain unverified**. Recognition and voice availability depend on installed Android services and voices.

Switch languages using **“language”**, **“switch to English”** or **“switch to Spanish”**. TalkBack also exposes a **Choose language** accessibility action. Because speech recognizers may not reliably understand a command spoken in the other language, use the accessible chooser if the spoken switch fails.

## Voice commands (English)

These are implemented examples from the English command grammar in `v0.5.0-alpha.2`, not a claim of successful English recognition on all Android devices.

| Say in English | Action |
| --- | --- |
| `introduction` | Hear the introduction again |
| `help` | Hear contextual instructions |
| `board` | Hear the current board position |
| `rank two` | Read the second rank |
| `square e4` | Identify square e4 |
| `status` | Hear whose turn it is and the game status |
| `history` | Hear the move history |
| `legal moves` | List available legal moves |
| `option two` | Review the second numbered option |
| `next` / `previous` | Navigate groups of available moves |
| `from e2 to e4` | Propose a coordinate move |
| `pawn from e2 to e4` | Propose a move with the piece name |
| `from e2`, then `to e4` | Enter a move in two steps |
| `castle kingside` | Propose kingside castling, if legal |
| `confirm` / `cancel` | Accept or reject a pending action |
| `undo` | Request to take back the last move |
| `new game` | Request a new game (confirmation required) |
| `suggestion` | Request an optional Stockfish move suggestion |
| `language` | Open the accessible language chooser |
| `switch to Spanish` | Select Spanish speech and interface language |

A recognized move must pass native legality checks and explicit confirmation before being applied. Recognition accuracy and spoken feedback depend on the installed Android speech and TTS services.

The app depends on an Android speech recognition service. Some services may process audio remotely; availability, privacy behavior, and microphone permissions depend on the service and device settings. The app does not require Stockfish to validate its own chess moves.

## Download and installation

Download the **English / Spanish Android debug APK** from the **[v0.5.0-alpha.2 pre-release](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.2)**. The previous [Spanish-only alpha.1](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1) remains available.

The APK is an **experimental debug build**, not a Play Store or production-signed release. Install it only if you understand the preview status. Check the accompanying `SHA256SUMS.txt` when verifying downloaded artifacts. The release also contains the tagged project source and the separately licensed Stockfish 19 source.

**Version note:** the GitHub release tag is `v0.5.0-alpha.2`, while the unchanged APK reports internal `versionName` `0.5.0-alpha.2-rc1`. Both refer to the same qualified R34 build, not two different binaries.

**Platform:** Android 8.0 (API 26) or newer, with `armeabi-v7a` or `arm64-v8a` support. Device-specific voice-service compatibility is not guaranteed.

## Release history

| Version | Distribution | Verification status |
| --- | --- | --- |
| [v0.5.0-alpha.2](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.2) | Experimental bilingual Android debug APK (`en-US` / `es-AR`) | Native and bilingual host tests passed; English and TalkBack real-device acceptance pending |
| [v0.5.0-alpha.1](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1) | Earlier Spanish-only Android debug APK | Spanish move recognition observed on a Samsung Galaxy A20; independent accessibility acceptance pending |

For alpha.2, the release assets are the Android APK, tagged project source ZIP, official Stockfish 19 source ZIP, and `SHA256SUMS.txt`. Use the SHA-256 file and official release assets rather than third-party repackaging.

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

The alpha.2 qualification reported **8/8 native CTest cases passing**, **56 bilingual voice-command cases**, Spanish compatibility checks, and Java/UCI and voice-first host checks. The APK was built and signature-matched on Windows; its SHA-256 is `2b06a3b55885a3ac06cfa6d0d4d06a566c707cb5a87027715c2640b89cf308fc`. Automated checks do **not** replace English speech or TalkBack testing on a device.

To build the Android app with Stockfish, consult [`docs/BUILD_ANDROID_STOCKFISH.md`](docs/BUILD_ANDROID_STOCKFISH.md) and [`docs/STOCKFISH_PROVENANCE.json`](docs/STOCKFISH_PROVENANCE.json). The official Stockfish archives must be obtained and staged according to their verified provenance; they are not committed as application source binaries.

## Accessibility status and roadmap

**Current operator observation:** the earlier Spanish alpha starts on a Samsung Galaxy A20 and recognizes spoken chess moves. **The bilingual alpha.2 is publicly downloadable, but its English voice behavior has not yet been tested on a physical device.** Completing a game independently without vision, complete TalkBack workflows, speech timing, permission recovery and broad device compatibility are also not yet qualified.

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

*Built to make chess more accessible, one move at a time.*
