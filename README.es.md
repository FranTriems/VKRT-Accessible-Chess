# VKRT Accessible Chess — Ajedrez accesible por voz

[English](README.md) | [Español](README.es.md)

**Ajedrez open source para Android, orientado al control por voz y a personas ciegas o con baja visión.** Incorpora un motor de reglas propio en C++20 y, opcionalmente, Stockfish 19 para sugerencias.

**Autor del proyecto:** **Francisco Triemstra ([FranTriems](https://github.com/FranTriems))**, desarrollador independiente de Argentina y creador de VKRT.

[**Descargar v0.5.0-alpha.2 (Español / English)**](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.2) · [**Código fuente**](https://github.com/FranTriems/VKRT-Accessible-Chess) · [**Reportar un problema**](https://github.com/FranTriems/VKRT-Accessible-Chess/issues)

> **Versión pública actual: v0.5.0-alpha.2 (español e inglés).** La APK bilingüe está publicada como versión preliminar. Los movimientos por voz en español se probaron en un Samsung Galaxy A20 con la alpha anterior. **El inglés y esta APK bilingüe todavía no se probaron en un teléfono real**; tampoco hay certificación de accesibilidad ni aceptación completa con TalkBack.

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

**Versión publicada alpha.2:** español (`es-AR`) e inglés (`en-US`) seleccionables para textos, comandos, ayuda contextual, idioma solicitado al reconocedor y selección de voces TTS instaladas. La elección se conserva al reiniciar.

**Límite de verificación:** la compilación Android y las pruebas automatizadas bilingües pasaron, pero no se verificó el reconocimiento inglés en un dispositivo real ni se completó la evaluación independiente con TalkBack y personas ciegas.

Para seleccionar idioma, decí **«idioma»**, **«idioma inglés»**, **«idioma español»** o, ya en inglés, **«language»**, **«switch to Spanish»**. En TalkBack se ofrece la acción **«Elegir idioma»**. El reconocedor puede no entender una frase en otro idioma; en ese caso se utiliza la acción accesible para cambiarlo.

| Español | Inglés (alpha.2) | Función |
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

La APK bilingüe está disponible en **[v0.5.0-alpha.2](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.2)**. La anterior [alpha.1 en español](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1) se conserva. Es una compilación **debug experimental**, no una aplicación de Play Store. Requiere Android 8.0 (API 26) o superior y ABI ARM compatible. Verificá la descarga con `SHA256SUMS.txt`; la release incluye el código etiquetado del proyecto y los fuentes de Stockfish 19 bajo su licencia.

**Nota de versión:** el tag de GitHub es `v0.5.0-alpha.2`, pero la APK, distribuida sin recompilar, conserva internamente el `versionName` `0.5.0-alpha.2-rc1`. Se trata del mismo binario R34 calificado.

Con CMake, Ninja y compilador C++20:

```sh
cmake -S native -B build-host -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build-host --config Release
ctest --test-dir build-host -C Release --output-on-failure
```

Para Android y Stockfish, consultá [`docs/BUILD_ANDROID_STOCKFISH.md`](docs/BUILD_ANDROID_STOCKFISH.md) y [`docs/STOCKFISH_PROVENANCE.json`](docs/STOCKFISH_PROVENANCE.json).

## Historial de versiones

| Versión | Distribución | Estado de verificación |
| --- | --- | --- |
| [v0.5.0-alpha.2](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.2) | APK debug bilingüe (`en-US` / `es-AR`) | Compilación y pruebas automatizadas PASS; inglés y TalkBack reales pendientes |
| [v0.5.0-alpha.1](https://github.com/FranTriems/VKRT-Accessible-Chess/releases/tag/v0.5.0-alpha.1) | APK debug anterior en español | Reconocimiento de jugadas observado en Galaxy A20; aceptación independiente pendiente |

Los archivos de `alpha.2` incluyen la APK, un ZIP del código del proyecto etiquetado, el ZIP de fuentes oficiales de Stockfish 19 y `SHA256SUMS.txt`. Se recomienda descargar de GitHub Releases y verificar los hashes.

## Accesibilidad y estado real

El desarrollador comprobó que la alpha anterior en español abre en Samsung Galaxy A20 y reconoce jugadas. **La APK bilingüe alpha.2 está publicada pero todavía no fue probada en el dispositivo.** R34 pasó **8/8 pruebas C++20** y **56 casos de comandos bilingües**, además de controles Java/UCI. La APK `alpha.2` tiene SHA-256 `2b06a3b55885a3ac06cfa6d0d4d06a566c707cb5a87027715c2640b89cf308fc`; faltan la prueba real del inglés, una partida íntegra sin visión y el recorrido completo con TalkBack. Se agradecen pruebas y sugerencias, sin compartir grabaciones ni datos privados.

## Autoría y licencias

**Código propio:** Copyright © 2026 **FranTriems (VKRT)**, ofrecido bajo licencia **MIT** cuando se dispone de los derechos necesarios. Toda redistribución de este código o partes sustanciales debe conservar los avisos de autoría y licencia. Ver [`LICENSE`](LICENSE), [`AUTHORS.md`](AUTHORS.md) y [`ATTRIBUTION.md`](ATTRIBUTION.md).

**Stockfish 19:** Copyright de sus autores originales, bajo **GPLv3**. VKRT no reclama autoría sobre Stockfish. La APK que lo incluye queda sujeta a las obligaciones aplicables de GPLv3. Consultá [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Desarrollo y colaboración

El proyecto usa ingeniería asistida por IA bajo gobernanza VKRT: cambios aislados, evidencias de procedencia, pruebas de regresión y aceptación humana antes de publicar. Los informes de tests no sustituyen las pruebas de accesibilidad reales.

*Creado para hacer el ajedrez más accesible, una jugada a la vez.*
