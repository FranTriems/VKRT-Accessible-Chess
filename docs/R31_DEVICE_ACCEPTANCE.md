# R31 acceptance plan — needs voluntary real-device witness

Do not call the release accessible solely because build and automated tests pass.

1. With TalkBack OFF and microphone allowed: hold anywhere, say `ayuda`, release; hear instructions. Hold, say `e2 e4`; hear the proposed move; hold, say `confirmar`; one move applied. Verify tapping quickly also starts a one-shot session without unintentional moves.
2. With TalkBack ON: one focusable voice surface. Explore without other interactive stops. Double-tap to activate. Voice and TalkBack must not compete; custom action for typed coordinate input works if recognition unavailable.
3. First launch of a clean install: presentation narrated. Upgrading a prior R30 install may retain previous save and first-run preference; verify introduction through `presentación`.
4. Voice flows: `opciones`, `opción tres`, `siguientes`, `anteriores`, `origen g1`, `destino f3`, `tablero`, `fila 2`, `historial`, `estado`, `nueva partida`, `deshacer`, `cancelar`. No speech command changes a game without reviewing and confirming it.
5. Voice service: local on-device service where available; remote consent only once; Android RECORD_AUDIO permission only when absent; denied permission and device settings guidance. `revocar consentimiento` restores the consent gate. No audio/transcripts saved by the app.
6. Confirm Stockfish UCI responds and every suggestion is checked by the same C++20 game core, with no automatic move on error/timeout.
7. Rotate/lock/unlock, app background/resume, lower hardware specs, high Android font scaling, and invalid-save recovery. Verify no dead-ends without a microphone.

Witness must be invited, not coerced; do not record identifying data, audio or raw transcripts. Report blocking details without personal identifiers. Public release remains HOLD for GPLv3/source-compliance and accessibility testing.
