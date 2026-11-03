# VKRT Accessible Chess — optional Stockfish 19 (sf_19)

## Components and independent authority

* `native/` C++20 is authoritative for legal moves, position, result, repetition, SAN, and saving. No Stockfish code is added there.
* `StockfishUci.java` implements a **separate-process UCI transport**, uses `uci`/`isready`/`position fen`/`go movetime`, and accepts only a syntactically valid UCI move.
* The Android worker passes that move through `nativeRequest(oldState,"propose",move)`, which enforces C++20 legality and never mutates the game; the user confirms separately. Timeout, missing engine, illegal move and crash all fail without modifying the position.
* Core-only APK still builds and runs, without Stockfish and without microphone permission being necessary to play.
* Stockfish edition is separately assembled from official Stockfish 19 Android release binaries. The executable lives as `lib/<ABI>/libstockfish.so` in the APK and Android extracts it into its protected `nativeLibraryDir`. Never copy an executable to app-writable storage.

## Low-end device budgets

* UCI Threads=1, Hash=16 MiB, MultiPV=1, Ponder=false.
* User selectable thinking budget 300/650/1500/3000 ms; watchdog terminates blocked processes.
* User selectable approximate strength 1320/1500/1900/2400 Elo, or unlimited; these are engine targets, not measured mobile ratings. No additional internet, service account, telemetry, framework or bundled neural model beyond the official Stockfish binaries and NNUE inside them.
* Stockfish process runs only upon explicit request; each proposal launches an ephemeral process, so memory/energy use and load time must be measured on-device. A persistent bounded engine can be a later optimization.

## License and release gate

* Our stand-alone C++20 core is independent of Stockfish; its existing MIT LICENSE is unchanged.
* Stockfish 19 is GPLv3. Distributing a combined APK, even with a separate process, requires legal review of whether the combined work must be distributed under GPLv3. Do not assume a separate process by itself creates an exemption.
* For an APK with Stockfish, publish the app's complete corresponding source, the unmodified official Stockfish source (including dependencies/NNUE build instructions, patches if any), exact release version, binary/source hashes, copyright and GPL notices, and appropriate build/install scripts. Provide a GPL-compatible license for the combined distribution as required. This kit deliberately keeps `PUBLIC_RELEASE=HOLD` until independently adjudicated.
* API download asset digests are checked against GitHub release metadata; GitHub authenticity and source-to-binary reproducibility still require independent review.

Official sources: https://stockfishchess.org/download/  https://github.com/official-stockfish/Stockfish/releases/tag/sf_19
