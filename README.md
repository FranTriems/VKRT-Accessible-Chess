    ██╗   ██╗██╗  ██╗██████╗ ████████╗
    ██║   ██║██║ ██╔╝██╔══██╗╚══██╔══╝
    ██║   ██║█████╔╝ ██████╔╝   ██║
    ╚██╗ ██╔╝██╔═██╗ ██╔══██╗   ██║
     ╚████╔╝ ██║  ██╗██║  ██║   ██║
      ╚═══╝  ╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝

# VKRT Accessible Chess — Ajedrez accesible por voz - Creado por Francisco Triemstra

**Versión:** `v0.5.0-alpha.1` (pre-release). **Plataforma:** Android, C++20 nativo y Java sin frameworks de UI externos.

Aplicación de ajedrez *voice-first* en español. Podés mantener pulsada la pantalla para hablar, proponer movimientos por coordenadas o por pieza, recibir ayuda contextual y confirmar/cancelar sin mirar el tablero. También muestra un tablero visual para quien quiera verlo.

**Estado observado:** compilación y pruebas automáticas satisfactorias; una instalación en Samsung Galaxy A20 con reconocimiento de jugadas validado por el desarrollador. **Todavía no** hay aceptación integral con TalkBack ni certificación de accesibilidad. Hay áreas de UX en desarrollo.

## Funciones

- Motor de reglas y partida independiente VKRT Chess Core C++20; verificación de legalidad y confirmación antes de aplicar movimientos.
- Modo de voz en español: `ayuda`, `presentación`, `e2 e4`, `peón de e2 a e4`, `opciones`, `opción uno`, `confirmar`, `cancelar`, `tablero`, `fila dos`, `sugerencia`.
- Alternativa de entrada manual mediante acciones de accesibilidad. Reconocimiento de voz dependiente de los servicios del dispositivo Android; no se garantiza funcionamiento offline.
- Stockfish 19 opcional para sugerencias mediante protocolo UCI; ejecución local sin permisos INTERNET de la aplicación. Stockfish es un proyecto independiente y de terceros.

## Descargar e instalar

Ver **[Releases](https://github.com/FranTriems/VKRT-Accessible-Chess/releases)** y elegir `v0.5.0-alpha.1`.

El archivo `VKRT-Accessible-Chess-v0.5.0-alpha.1-android-debug.apk` es **una APK de depuración y prueba**, no una distribución de producción ni una aplicación certificada. Instalar solamente si se entiende su carácter experimental; los datos de partidas no están respaldados por un servidor.

## Compilación y arquitectura

- `native/`: C++20 y pruebas CMake/CTest.
- `android/`: Java Android, JNI, TalkBack, voz y tablero.
- `scripts/stage_stockfish.py`: prepara de manera verificable los binarios oficiales Stockfish desde los dos paquetes Android de `sf_19` sin descargarlos automáticamente.
- `docs/BUILD_ANDROID_STOCKFISH.md`: requisitos y órdenes de compilación.
- `docs/STOCKFISH_PROVENANCE.json`: hashes y origen de los binarios del release.

El núcleo VKRT no necesita Stockfish. La variante APK de la release incorpora binarios Stockfish opcionales. No se incluyen ROMs, partidas privadas, grabaciones, claves ni logs de sesión.

## Licencias

El código propio de VKRT Accessible Chess está ofrecido bajo MIT, cuando su procedencia y titularidad lo permitan, con **Copyright (c) 2026 FranTriems (VKRT)**. Se debe conservar el aviso de copyright y el texto MIT al redistribuir el código o partes sustanciales. Consultar [`AUTHORS.md`](AUTHORS.md) y [`ATTRIBUTION.md`](ATTRIBUTION.md). Stockfish 19 está bajo GPLv3; la APK combinada se distribuye sujeta a las obligaciones GPLv3 aplicables. La fuente original Stockfish `sf_19` y la licencia se adjuntan en **la misma release**. Ver `THIRD_PARTY_NOTICES.md` y `docs/RELEASE_NOTES_ALPHA.md`. La licencia MIT del núcleo no reduce las obligaciones GPL del conjunto distribuido.

## Colaborar

Se agradecen reportes de fallos de reconocimiento, navegación TalkBack, movimientos, y compatibilidad con dispositivos modestos. No incluyas grabaciones, transcripciones privadas ni información de otras personas en reportes públicos.

Desarrollado con la metodología VKRT: candidato aislado, controles de evidencia y pruebas independientes. La IA puede asistir, pero los tests y la aprobación del desarrollador deciden la publicación.
