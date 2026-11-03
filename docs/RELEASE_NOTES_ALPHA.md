# v0.5.0-alpha.1 — Preview Android voice-first

**Tipo:** pre-release / alpha, APK de depuración.

Calificación conservada del R31: 8/8 CTest C++20; gramática Java y UCI; 32 casos de comandos, integridad de librerías Stockfish ARM32/ARM64 y firma de depuración compatible con R30. El desarrollador reportó que la APK R31 se instaló en el Galaxy A20 y reconoce movimientos por voz, pero quedan mejoras de UX.

### Limitaciones conocidas

- No existe todavía una prueba integral con una persona ciega y TalkBack completando una partida; la accesibilidad funcional completa no está certificada.
- El reconocimiento depende del motor de voz del dispositivo y puede requerir conectividad o permisos, según el servicio configurado.
- La APK está firmada con clave de depuración, no con clave de producción. La release no es una versión estable.
- Están pendientes ajustes de presentación, guía conversacional, ergonomía y más pruebas de resiliencia.
- No se ha verificado reproducibilidad bit-a-bit de los binarios upstream de Stockfish.

### Material incluido en la release

- `VKRT-Accessible-Chess-v0.5.0-alpha.1-android-debug.apk` — APK Android.
- `VKRT-Accessible-Chess-v0.5.0-alpha.1-source.zip` — código de aplicación y documentación.
- `Stockfish-sf_19-official-tag-source.zip` — fuentes originales GPLv3 de Stockfish 19.
- `SHA256SUMS.txt` — hashes de todos los artefactos.

Fuentes y avisos GPL adjuntos para cumplimiento de distribución. No se afirma que el equipo VKRT sea autor de Stockfish.

### Créditos y licencias

Código propio VKRT Accessible Chess: Copyright (c) 2026 **FranTriems (VKRT)**, MIT; conservar sus avisos según `LICENSE`. Stockfish 19: Copyright **Stockfish contributors**, GPLv3; fuente y licencia distribuidas por separado. Véanse `AUTHORS.md`, `ATTRIBUTION.md`, `THIRD_PARTY_NOTICES.md`. Alpha sin certificación de accesibilidad.
