# VKRT Accessible Chess — Ajedrez accesible por voz

[English](README.md) | [Español](README.es.md)

**Ajedrez open source para Android, orientado al control por voz y a personas ciegas o con baja visión.** Incorpora un motor de reglas propio en C++20 y, opcionalmente, Stockfish 19 para sugerencias.

**Autor del proyecto:** **Francisco Triemstra ([FranTriems](https://github.com/FranTriems))**, desarrollador independiente de Argentina y creador de VKRT.

[**Descargar la versión pública actual**](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1) · [**Código fuente**](https://github.com/FranTriems/VKRT-Accessible-Chess) · [**Reportar un problema**](https://github.com/FranTriems/VKRT-Accessible-Chess/issues)

> **Importante:** La versión pública `v0.5.0-alpha.1` funciona en español. R34 es un **candidato de desarrollo** con español e inglés seleccionables. El código fuente de R34 se publica en `main`, pero **todavía no se ha publicado una APK bilingüe ni probado el inglés en un teléfono real**. No hay certificación de accesibilidad.

## ¿Qué es?

VKRT Accessible Chess es una aplicación de ajedrez nativa para Android que reduce la dependencia de controles visuales. En lugar de arrastrar piezas, podés hablarle al juego, consultar el tablero, pedir jugadas legales, elegir una opción y confirmar o cancelar una acción.

El motor **VKRT Chess Core (C++20)** controla independientemente la legalidad de las jugadas. Stockfish 19 es un motor de terceros opcional y no reemplaza nuestro núcleo. El tablero visual sigue disponible, pero no es necesario tocar sus casillas.

## Características

- Superficie única para hablar: mantené pulsada la pantalla y soltá al terminar.
- Con TalkBack: activación accesible para iniciar o detener la escucha y acción alternativa de entrada manual.
- Jugadas por coordenadas o piezas; selección de jugadas legales por opciones numeradas.
- Confirmación obligatoria antes de aplicar un movimiento reconocido por voz.
- Lectura de casillas, filas, estado, historial y ayuda contextual.
- Opcionalmente, sugerencias mediante Stockfish 19 por UCI.
- Núcleo C++20, interfaz Java nativa, JNI, CMake y Gradle.

## Idiomas

**Versión publicada alpha.1:** español (`es-AR`).

**Candidato R34 / alpha.2-rc1 (pendiente de prueba):** español (`es-AR`) e inglés (`en-US`) para textos, órdenes reconocidas, ayuda y selección de voces TTS locales instaladas. El idioma elegido se conserva al reiniciar.

Para seleccionar idioma, decí **«idioma»**, **«idioma inglés»**, **«idioma español»** o, ya en inglés, **«language»**, **«switch to Spanish»**. En TalkBack se ofrece la acción **«Elegir idioma»**. El reconocedor puede no entender una frase en otro idioma; en ese caso se utiliza la acción accesible para cambiarlo.

| Español | Inglés (R34) | Función |
| --- | --- | --- |
| `ayuda` | `help` | Ayuda contextual |
| `presentación` | `introduction` | Introducción |
| `tablero` | `board` | Describir tablero |
| `fila dos` | `rank two` | Leer fila |
| `opciones` | `legal moves` | Enumerar jugadas |
| `opción dos` | `option two` | Elegir opción |
| `e2 e4` | `from e2 to e4` | Proponer movimiento |
| `peón de e2 a e4` | `pawn from e2 to e4` | Proponer movimiento por pieza |
| `confirmar` | `confirm` | Aceptar jugada pendiente |
| `cancelar` | `cancel` | Cancelar |
| `sugerencia` | `suggestion` | Consultar Stockfish |
| `nueva partida` | `new game` | Solicitar partida nueva |

La voz depende del servicio de reconocimiento instalado en el Android. Algunos servicios procesan audio por red. La app solicita consentimiento cuando corresponde, pero no se garantiza reconocimiento totalmente offline.

## Descarga y compilación

La APK pública actual está en [GitHub Releases](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1). Es una compilación **debug experimental**, no una aplicación de Play Store. Android 8.0 (API 26) o posterior en ABI ARM compatible.

Con CMake, Ninja y compilador C++20:

```sh
cmake -S native -B build-host -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build-host --config Release
ctest --test-dir build-host -C Release --output-on-failure
```

Para Android y Stockfish, consultá [`docs/BUILD_ANDROID_STOCKFISH.md`](docs/BUILD_ANDROID_STOCKFISH.md) y [`docs/STOCKFISH_PROVENANCE.json`](docs/STOCKFISH_PROVENANCE.json).

## Accesibilidad y estado real

El desarrollador comprobó que la versión española abre en Samsung Galaxy A20 y reconoce jugadas. Falta completar una partida íntegra sin visión, verificar todos los flujos con TalkBack y comprobar el idioma inglés en el dispositivo. Se agradecen pruebas y sugerencias, sin publicar grabaciones ni datos privados.

## Autoría y licencias

**Código propio:** Copyright © 2026 **FranTriems (VKRT)**, ofrecido bajo licencia **MIT** cuando se dispone de los derechos necesarios. Toda redistribución de este código o partes sustanciales debe conservar los avisos de autoría y licencia. Ver [`LICENSE`](LICENSE), [`AUTHORS.md`](AUTHORS.md) y [`ATTRIBUTION.md`](ATTRIBUTION.md).

**Stockfish 19:** Copyright de sus autores originales, bajo **GPLv3**. VKRT no reclama autoría sobre Stockfish. La APK que lo incluye queda sujeta a las obligaciones aplicables de GPLv3. Consultá [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Desarrollo y colaboración

El proyecto usa ingeniería asistida por IA bajo gobernanza VKRT: cambios aislados, evidencias de procedencia, pruebas de regresión y aceptación humana antes de publicar. Los informes de tests no sustituyen las pruebas de accesibilidad reales.

**Creado por [Francisco Triemstra (FranTriems)](https://github.com/FranTriems) · VKRT · Argentina.**

*Para que jugar ajedrez dependa cada vez menos de poder ver.*
