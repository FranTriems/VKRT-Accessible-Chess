# R34 bilingual Android acceptance (NOT yet device-qualified)

R34 extends the R31 voice-first Android shell with independent, explicit English (`en-US`) and Spanish (`es-AR`) commands, display strings, confirmation prompts, contextual help and non-network Android TTS voice selection. Switching languages persists across launches. The game and native rules are NOT changed.

## Human witness on Galaxy A20

1. Update the previously tested R31 debug app using `adb install -r` *only after* build receipt confirms APK SHA and signer equality. Verify the existing saved game survives.
2. Spanish regression: press-and-hold, `ayuda`, `peón de e2 a e4`, `confirmar`, `opciones`, `tablero`, `nueva partida`, `cancelar`.
3. Switch: say `idioma`, then choose English, or say `idioma inglés`. With TalkBack, use the explicit `Choose language` accessibility action rather than switching gestures or disabling TalkBack.
4. English: `introduction`, `help`, `board`, `rank two`, `from e2 to e4`, `pawn from e2 to e4`, `legal moves`, `option two`, `confirm`, `cancel`, `undo`, `new game`, `suggestion`, `language Spanish`.
5. English TTS: confirm a **non-network installed en voice** is present. With TalkBack on, app TTS must remain silent and TalkBack must announce prompts. With no matching local voice, text/reader still work; do not claim speech works.
6. Speech service failure: missing recognition service, missing en model/locale, denied microphone, remote-service consent, low confidence, silence or network requirement must never execute a move. Use TalkBack keyboard action as fallback.
7. Confirm move selections, option paging, castling and four promotions for both languages; game data and notation remain common.
8. Check intro and help in both languages, large font, lifecycle resume, no unrequested new-game reset, and no duplicate spoken messages.

**Status: HOLD_REAL_ANDROID_ENGLISH_AND_TALKBACK_ACCEPTANCE**. No phone tests performed by this package. Application public release and GPLv3 combined-binary review remain separate explicit gates.
