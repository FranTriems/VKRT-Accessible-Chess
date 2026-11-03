# R31 Voice-first, blind-first contract (candidate; not device-qualified)

## Identity
- Android Java framework + JNI + independent VKRT C++20 rules/session + optional Stockfish UCI.
- Preserve native/*, MoveGrammar.java, StockfishUci.java, license/AGENTS.md, Android manifest, and signed/qualified R30 base bytes.
- The board is VISUAL ONLY, never mandatory to play; it must not produce 64 TalkBack focus stops.
- One full-screen voice surface is the only ordinary touch interaction.
- Without TalkBack: press and hold to start microphone, release after speaking. Quick tap starts a bounded one-shot session.
- With TalkBack: focus the surface; double-tap to start/stop, or double-tap and hold; never appropriate system gestures.
- An accessibility custom action can open typed-coordinate fallback without the microphone. No other touch navigation required.

## Voice grammar and safety
- First-run presentation; voice `presentación`, `ayuda`, `repetir`, `estado`, `tablero`, `fila 2`, `historial`.
- Moves by `e2 e4`, `peón de e2 a e4`, `origen e2`, `destino e4`; choices by `opciones`, `opción 2`, `siguientes`, `anteriores`.
- `confirmar` applies only an explicitly reviewed candidate (or a deliberately requested reset/undo).
- `cancelar` never changes game. Suggestion from Stockfish is rechecked against the C++20 core.
- `nueva partida` and `deshacer` have spoken confirmation. A corrupt private save must remain recoverable by voice.
- Spoken commands are parsed deterministically and matched to exact actions. No LLM needed.
- Stockfish remains optional; 1 thread and 16 MB hash configured in the existing R32 UCI adapter.

## Speech/permissions
- Android RECORD_AUDIO permission only requested if not granted, with guidance after denial.
- If only a remote recognizer is available: disclose possible third-party transmission and request consent once; store boolean consent, not audio/transcripts. Voice `revocar consentimiento` resets it.
- One recognition session per deliberate interaction, timed out at 15 seconds. No continuous background recording.
- App TTS is muted while an accessibility service is active; accessibility events can be announced from the single focused surface.
- Android may require regranting mic permission if a user selects a temporary OS permission. The app cannot override that OS policy.

## Qualification gates
- Exactly 8 C++ CTest + independent Java grammar/UCI/UX tests; Android debug APK offline, both JNI and exact same Stockfish binaries as R30, signer equality.
- No Codex, no installation, no network, no push, no release during this pass.
- Android runtime, TalkBack/gesture behavior, TTS timing, live speech capture, device-specific voice consent and blind-user acceptance remain HOLD until tested on device.
