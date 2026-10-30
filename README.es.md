# ASCENT

[English](README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [Deutsch](README.de.md) · **[Español](README.es.md)** · [Français](README.fr.md) · [Italiano](README.it.md) · [Русский](README.ru.md) · [Svenska](README.sv.md)

Una app Android para quien empieza en alpinismo de altitud y escalada en hielo. El temario está ordenado por altitud: cada lección superada te sube un poco más arriba.

Dos itinerarios de curso se cambian libremente. **Alta montaña** tiene seis unidades y 68 lecciones, desde la introducción a 5.000 m hasta 8.000 m — de 3.200 m a 8.848 m de principio a fin. **Senderismo** tiene cuatro unidades y 21 lecciones, desde la caminata de un día hasta las rutas largas — de 200 m a 5.630 m. Cada itinerario lleva su propio progreso, sus insignias y su cola de repaso.

Versión actual v0.21.0 (versionCode 129). El trabajo de idiomas cerró con la v0.19.24 — las nueve lenguas traducidas coinciden con el inglés entrada por entrada (10.554 / 10.554). La v0.20 va de insignias: los cuarenta logros se dibujan ahora como objetos 3D en tiempo real en lugar de imágenes planas, y sus iconos salen de los mismos modelos — las ilustraciones antiguas de IA ya no están. El diálogo de desbloqueo usa el mismo motor y lanza confeti desde los dos bordes de la pantalla. Los modelos viajan dentro del APK, así que el paquete pasó de 21 MB a unos 41 MB. La v0.20.3 arregla una molestia vieja: bajabas por una lista, abrías una pantalla secundaria y al volver la lista saltaba al principio — ahora se queda donde la dejaste. La v0.20.6–v0.20.7 sube el dibujado de las insignias a calidad máxima por defecto: bloom más fino que ya no se apaga por los primeros fotogramas lentos, rebaja en dos pasos y 4x MSAA con comprobación de capacidad y vuelta atrás silenciosa.

v0.20.19 – v0.21.0 renovaron toda la interfaz con acabado de arcilla: sombras dobles suaves dibujadas fuera de pantalla y cacheadas, fichas de color planas, colores por categoría en el manual y bloques de color cuya profundidad viene de una sombra de contacto, no de un degradado.

## Instalación

Lo más fácil es el APK debug ya compilado de [Releases](https://github.com/Solar-CV/AscentApp/releases). Al empujar una etiqueta `v*`, GitHub Actions compila y adjunta un APK nuevo.

- Paquete: `com.ascent.climb`
- Mínimo: Android 8.0 (API 26), **solo arm64-v8a** — los móviles de solo 32 bits no pueden instalarlo
- Tamaño: unos 41 MB (38 MB de modelos sin comprimir, unos 11 MB tras empaquetar)
- Permisos: red y estado de red — para comprobar actualizaciones del banco de preguntas y enviar comentarios. Todo funciona sin conexión; el banco incluido siempre está ahí

Pasa el APK al móvil, permite instalar desde orígenes desconocidos y toca instalar. En el primer arranque no hay que iniciar sesión.

## Qué hay dentro

### La ruta

La pantalla de inicio es una ruta desde el campo base hacia arriba, dividida en unidades de altitud (seis en alta montaña, cuatro en senderismo). Se pasa de una a otra deslizando en horizontal, dos unidades por página. Las lecciones se desbloquean en línea: apruebas una y se abre la siguiente. Los nodos muestran estrellas, la lección actual lleva un aviso de «empezar» y la última de cada unidad va marcada en oro.

Dos píldoras arriba cambian de itinerario; al tocarlas se abre una hoja inferior. Al cambiar se vuelve a la primera página y la elección se recuerda.

El progreso no es un número guardado. Se recalcula a partir de las lecciones completadas, así que si más adelante se ajusta la curva de altitud, las partidas antiguas se recalculan en vez de quedarse atascadas.

### Banco de preguntas

1.533 preguntas formales más 154 copias de repaso insertadas automáticamente, 1.687 elementos en pantalla. El volumen sube con la altitud: unas 15 preguntas por lección en la introducción de 5.000 m, 30 en las unidades técnicas de 7.000 m y 8 en senderismo.

Ocho tipos de pregunta: elección única, elección múltiple, ordenación, emparejamiento, rellenar huecos, cálculo, punto caliente (tocar un punto de la imagen) y escenario (un enunciado que se responde en varios pasos).

Al terminar una lección entras directamente a rehacer las que fallaste. Las que aciertas suman; el resto pasa al centro de repaso. El repaso usa repetición espaciada a 0, 1, 3, 7, 16 y 35 días. Las preguntas críticas para la seguridad llevan peso y exigen confirmar que lo has entendido antes de seguir.

### Manual

35 artículos largos, de 4 a 8 secciones cada uno, ordenados por material, técnica, medicina, salud, tiempo, meteorología, decisiones, criterio, seguridad, riesgo y rescate en aludes. El manual está filtrado por itinerario: las piezas de nieve y hielo solo aparecen en alta montaña, mientras que medicina de altura, hipotermia, navegación, acampada y liderazgo son comunes a los dos. Las fuentes son material médico y técnico público, tres documentos comerciales de guías y transcripciones editadas de siete libros chinos de montaña. Por acuerdo de producto, los libros chinos no se citan por título; los documentos de guías sí.

### Entrenamiento, repaso, perfil

Entrenamiento da un porcentaje de preparación y la altitud del pico objetivo. Repaso pone delante las preguntas que vencen hoy y los fallos críticos de seguridad. El perfil reúne la cabecera, la altitud actual, el resumen de capacidades, la entrada a las insignias, el área de cuenta y una entrada de comentarios. El engranaje abre los ajustes, el avatar la página de perfil.

El perfil solo muestra las insignias desbloqueadas. Las 40 están en el muro de insignias, las bloqueadas como vista previa en gris y los logros ocultos como «?».

Las insignias son objetos 3D reales: marcos metálicos, esmalte, relieve. Toca cualquier insignia desbloqueada en el perfil o en el muro y se abre a pantalla completa: arrastra para rotar, pellizca para acercar hasta unas 5,5×, con dos dedos desplazas la vista sobre el relieve, el doble toque restablece, y se mece sola despacio cuando sueltas. Los iconos salen de los propios modelos, así que el muro y la vista 3D muestran el mismo objeto. Las cuarenta tienen ya modelo; el dibujo a mano queda como reserva para insignias que se añadan después. Al desbloquear un logro en cualquier punto, el diálogo muestra la misma insignia 3D, sobre un pedestal claro, con cintas entrando desde los dos bordes.

### Ajustes

La apariencia sigue al sistema o se fija en claro u oscuro, se guarda y la barra de estado acompaña. El idioma sigue al sistema o se elige a mano; interfaz y contenido cambian juntos, sin reiniciar. La sección del banco muestra la versión actual, busca actualizaciones y restaura el banco incluido. El modo desarrollador se esconde detrás del número de versión en «Acerca de»: siete toques.

### Comentarios

Perfil → Comentarios y sugerencias, entre la tarjeta de cuenta y el aviso de seguridad. Elige un tipo — problema, idea, corrección de contenido —, escribe el mensaje, deja opcionalmente un correo para la respuesta y adjunta opcionalmente el diagnóstico. El envío llega directo al mantenedor mediante Formspree; si no hay red, la pantalla ofrece mandar el mismo texto por correo y lo copia al portapapeles si no existe app de correo.

El diagnóstico está desactivado por defecto y cubre versión de la app, versión de Android, modelo del dispositivo, idioma, versión del banco, itinerario actual, cifras de progreso y la última excepción no capturada. Sin texto de lecciones, sin historial de respuestas, sin cuenta, sin portapapeles.

## Diez idiomas

Los textos de interfaz y el contenido del curso están en:

简体中文 (origen), 繁體中文, English, 日本語, Deutsch, Français, Español, Italiano, Русский, Svenska

Las diez lenguas están completas. Cada una tiene su capa de entrada, los resúmenes de lección, el cuerpo del manual y el banco de preguntas, y su conjunto de claves coincide con el inglés entrada por entrada — **10.554 / 10.554**, sin faltar, sin duplicar y sin claves huérfanas.

| Capa | Elementos | Las 9 lenguas traducidas |
|---|---|---|
| Entrada (lecciones, unidades, títulos del manual, picos) | 117 | 100 % |
| Puntos clave de las lecciones | 443 | 100 % |
| Cuerpo del manual | 378 | 100 % |
| Banco de preguntas (enunciado, opciones, explicación) | 9.422 | 100 % |

La terminología sigue el uso montañero de cada lengua en vez de traducir literalmente: sueco stegjärn / isyxa / standplats, alemán Steigeisen / Pickel, ruso кошки / ледоруб, italiano corda fissa / bloccante / sosta. Las dosis médicas, los niveles de alud, las pendientes y las temperaturas se trasladan sin cambios — solo se adapta el formato numérico, y las tres convenciones se mantienen separadas a propósito:

- Inglés, chino y japonés: `6.5 °C`, `5,000 m`
- Alemán, español e italiano: `6,5 °C`, `1.000 m` (coma decimal, punto de millar)
- Francés y sueco: `6,5 °C`, `5 000 m` (coma decimal, espacio de millar)

El trato al lector también cambia: el alemán usa la forma impersonal con *man*, el español tutea con *tú*, el francés trata de *vous*, el sueco y el italiano de *du* / *tu*, mientras que los resúmenes de lección son impersonales en todas.

Si algún elemento falta en el idioma elegido, el orden es: idioma elegido → inglés → chino original. Ese camino ya es solo una red de seguridad.

## Temario

**Alta montaña**

| Unidad | Altitud | Lecciones | Preguntas | Por lección |
|---|---|---|---|---|
| 5000M · Introducción | 5.000 m | 16 | 249 | 16 |
| 5000M · Progresión | 5.300 m | 12 | 221 | 18 |
| 6000M | 6.000 m | 14 | 316 | 22 |
| 7000M · Campamento técnico C3 | 7.000 m | 6 | 185 | 31 |
| 7000M · Campamento alto C4 | 7.500 m | 10 | 279 | 28 |
| 8000M | 8.000 m | 10 | 248 | 24 |

**Senderismo**

| Unidad | Altitud | Lecciones | Preguntas | Por lección |
|---|---|---|---|---|
| Caminatas de un día | 1.200 m | 5 | 40 | 8 |
| Varios días y acampada | 2.500 m | 8 | 79 | 10 |
| Senderismo de altura | 3.800 m | 5 | 44 | 8 |
| Rutas largas y emergencias | 4.500 m | 3 | 26 | 8 |

38 picos y rutas objetivo en total: 30 de alta montaña, desde Aotaina a 5.211 m hasta los catorce ochomiles; 8 de senderismo, desde el sendero de Huihang hasta la ruta de los Tres Pasos del Everest.

## De dónde viene el contenido

Dos corrientes principales alimentan las preguntas y el manual.

Las fuentes públicas incluyen la actualización de 2024 de las guías clínicas de la Wilderness Medical Society para el mal agudo de montaña, el CDC Yellow Book, material técnico del American Alpine Institute y del American Alpine Club, REI Expert Advice, las normas de equipo de UIAA y CE, y datos de aludes de avalanche.org.

Los tres documentos de guía de SEVEN SUMMIT TREKS — mal de altura, equipo y práctica general de expedición — son la segunda corriente.

Más adelante se editaron transcripciones corregidas de siete libros chinos de montaña para el manual. El conocimiento y los casos están reescritos con palabras propias y referencia de página; el texto escaneado no se guarda en el repositorio.

| Fuente | Se usa para |
|---|---|
| WMS / CDC / StatPearls | Preguntas y manual de medicina de altura |
| AAI / AAC / REI / BMC | Técnica de nieve, hielo y progresión |
| Normas UIAA / CE | Preguntas de material |
| avalanche.org | Criterio de aludes |
| Documentos de SEVEN SUMMIT TREKS | Manual h17–h22 y preguntas relacionadas |
| 7 libros chinos de montaña | Manual h23–h31 |

## Límite de seguridad

La app cubre solo teoría y preparación cognitiva. Las habilidades prácticas — nieve y hielo, cuerdas, escalada en hielo, rescate — deben enseñarse y evaluarse en el terreno por un instructor titulado.

Nada de esto es consejo médico. Las decisiones sobre medicación y primeros auxilios corresponden a un médico.

No se promete cumbre ni se garantiza ningún resultado de aclimatación.

Las lecciones marcadas como que requieren instructor lo dicen con claridad al terminar la teoría.

## Compilarlo tú mismo

Todo el conjunto de herramientas puede vivir en tu carpeta personal: sin Homebrew, sin Android Studio. Lo que usa esta máquina:

- `~/android-toolchain/jdk`: Temurin OpenJDK 17
- `~/android-toolchain/sdk`: Android SDK con platforms;android-34, build-tools;34.0.0, platform-tools
- `~/android-toolchain/gradle-8.9`: Gradle 8.9

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/android-toolchain/sdk"

cd AscentApp
"$HOME/android-toolchain/gradle-8.9/bin/gradle" assembleDebug
```

El repositorio incluye además el wrapper de Gradle 8.9, así que una máquina sin Gradle también sirve:

```bash
cd AscentApp
./gradlew assembleDebug
```

El resultado queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura del proyecto

```
AscentApp/
├── import/                      Redacción de preguntas (Markdown, de libros impresos)
├── knowledge/                   Base de conocimiento de las fuentes impresas
├── tools/
│   ├── contentcheck/            Comprobaciones de contenido + exportación (JVM pura)
│   ├── i18ncheck.py             Cobertura por idioma en las cuatro capas
│   ├── qbank/build.mjs          Compilador Markdown → banco de preguntas
│   └── poc3d/                   Sincronía de modelos, recorte de iconos, horneado emisivo
└── app/src/main/
    ├── res/values*/             Textos de interfaz en 10 idiomas
    ├── assets/
    │   ├── badges/*.glb         40 modelos de insignia
    │   └── env/studio_ibl.ktx   Entorno IBL generado por código
    └── java/com/ascent/climb/
        ├── data/
        │   ├── Unit1~5.kt      Contenido de las lecciones principales
        │   ├── NewLessons.kt    Lecciones del temario nuevo
        │   ├── i18n/Pack*.kt    Traducciones de contenido (entrada / resumen / manual / banco)
        │   ├── Diagnostics.kt   Registro de fallos + instantánea del entorno para comentarios
        │   ├── FeedbackSender.kt  POST a Formspree (HttpURLConnection, sin dependencias)
        │   ├── BundledBank.kt   Ensamblado del pool de lecciones e inserción de repasos
        │   └── bank/            Configuración del banco, decodificación JSON, red, repositorio
        └── ui/                  Pantallas y componentes (Badge3D.kt = escena Filament, encuadre, luces)
```

## Cambiar contenido

Para añadir preguntas a una lección existente, edita `data/Expansion*.kt`. Para una lección nueva, escribe `Lesson(...)` en `data/NewLessons.kt` y añade su id a la unidad correspondiente en `Curriculum.kt`. El orden y la pertenencia a unidades viven solo en `Curriculum.kt`.

Después lanza las comprobaciones:

```bash
bash tools/contentcheck/run.sh
python3 tools/i18ncheck.py
```

El primer script sincroniza el contenido de la app en un proyecto JVM puro, ejecuta la lógica de ensamblado real y revisa número de preguntas, progresión de dificultad, respuestas fuera de rango, preguntas vacías, lógica de desbloqueo y definiciones de logros; luego exporta `bank/bank.json` y `bank/manifest.json`. El segundo informa la cobertura por idioma en las cuatro capas y compara las claves. Ninguno necesita emulador.

Tras tocar el banco, sube `BUNDLED_VERSION` en `data/bank/BankConfig.kt`; si no, los clientes creerán que nada ha cambiado.

## Actualizaciones en línea del banco

El banco tiene tres capas: caché local, incluido y delta en línea. El orden de consulta es caché → incluido; la copia en línea solo sobrescribe la caché cuando es más nueva.

Así que un avión, un valle sin cobertura, un servidor caído, un JSON mal formado, un sha256 que no cuadra o una versión más vieja dejan todo como está. Las actualizaciones se comprueban en silencio tras arrancar, como mucho cada seis horas y solo si hay una URL configurada.

Para publicar un banco nuevo: cambia el contenido → sube `BUNDLED_VERSION` → ejecuta `contentcheck` → sube `bank/bank.json` y `bank/manifest.json` a un servidor o CDN → la app lo toma en el siguiente arranque, sin publicar versión.

Las versiones antiguas de la app no se rompen con un banco más nuevo. `minAppVersion` en `manifest.json` bloquea clientes demasiado viejos, los campos desconocidos se ignoran y una pregunta mal formada se salta en vez de ser fatal.

## Cambios recientes

**v0.20.19 – v0.21.0: superficies de arcilla**

- Las tarjetas son de arcilla: cuatro capas por tarjeta — sombra exterior, luz de borde, un breve brillo superior y un sombreado interior. La sombra se dibuja con `BlurMaskFilter` fuera de pantalla y se cachea por tamaño y radio. `Modifier.blur()` exige API 31 y el proyecto arranca en 26; `Modifier.shadow()` no puede hacer una sombra de dos capas con color
- Cinco pantallas rediseñadas: perfil, repaso, entrenamiento y manual, y luego la página «Mi». El brillo se recogió en un labio de 20 dp, el contorno pasó de 1 píxel físico a dp y la sombra se dividió en volumen más contacto — antes las tarjetas parecían semitransparentes porque el brillo se comía sus bordes
- Cada categoría del manual tiene su color (material azul acero, cuerdas turquesa, nieve y hielo índigo, medicina rojo, criterio ámbar, planificación verde, liderazgo magenta); las fichas son color plano sin degradado y la portada del detalle toma el color de la categoría
- Varios logros desbloqueados seguidos ya no se superponen: la vista 3D se reconstruye por id de logro y el fondo permanece opaco entre diálogos

**v0.20.18: nuevo diorama del Everest**

- El modelo de «Climbing Grand Slam» se rehízo pensando en el detalle: las banderas de oración pasan a 0,42 × 0,26 mm (antes invisibles en el móvil), sus cuerdas se apoyan en dos montones de piedra y cada bandera se comprueba contra la nieve, y las piezas pequeñas muestrean la malla del terreno en vez de lanzar rayos a toda la escena
- El icono viene del nuevo render hero; la pose y el encuadre no cambian

**v0.20.17: se acabó el giro accidental**

- Al levantar un dedo tras el pellizco se interpretaba como arrastre de un dedo, con el punto de referencia a un palmo de distancia: la insignia saltaba y ese salto pasaba al impulso. Ahora el gesto es una máquina de estados: al empezar el gesto de dos dedos se ignora el dedo restante, y el pellizco no deja inercia
- Con la vista ampliada nada se mueve solo, y volver a la cara frontal es un giro suave en vez de un salto

**v0.20.16: acércate a los detalles**

- El pellizco llegaba como máximo a 1,25×, así que los nombres de los picos y las alturas seguían ilegibles en el móvil. Ahora llega a 5,5×
- Con dos dedos a distancia fija, arrastrar desplaza la vista hasta las esquinas; con un dedo sigue girando. Doble toque restablece zoom y desplazamiento, y bajo el título de la insignia hay una breve guía de gestos

**v0.20.15: las cuarenta insignias ya son 3D**

- Llegan las seis últimas: Eight K Grad (birrete), Final Exam (medalla de cumbre con bandera), Five Zones (placa con suela de zapatilla), Fourteen Peaks (panorámica 2,5:1 de los catorce ochomiles), Two Tracks (bandeja de dos vías) y Full Route (un bloque de terreno del Everest, la única pieza que no es medalla)
- El icono de cada insignia es ahora el render del propio modelo, así que las ilustraciones dibujadas desaparecen del muro
- Full Route necesita su propia pose: se apoya en su base, así que se salta el giro de 90° de las medallas planas y se gira la placa hacia la cámara; su render está sobre gris, no negro, y el recorte se hace contra ese gris
- Modelos 34 → 40; paquete 39,4 → 40,6 MB

**v0.20.14: las insignias se abren en cualquier sitio**

- Ya no hace falta entrar en la zona de pruebas de desarrollo: toca cualquier insignia desbloqueada en «Mi perfil» o en el muro y se abre a pantalla completa. La vista se superpone a la app en vez de sustituir la página, así que al volver sigues donde estabas —con la posición de la lista— y la barra inferior queda tapada
- «Trail Finish» seguía mostrando el icono de reserva compartido aunque su modelo ya estaba; ahora apunta al icono renderizado
- «Two Tracks» tenía como icono la fotografía de una persona —un marcador heredado—. Ahora usa una placa dibujada (esmalte azul marino, borde de latón, dos rutas discontinuas al pico) y la foto ya no viaja en el paquete

**v0.20.13: seis modelos de insignia más**

- La biblioteca pasa de 28 a 34: 6.000 m de aprendizaje, regreso, huellas de siete días y de tres semanas, veinte carreras y un mosquetón en D para «la seguridad primero»; self_arrest reexportado
- El recorte de iconos ahora también elimina el fondo *encerrado* por la forma de la insignia: el agujero central del mosquetón y las perforaciones de una tarjeta troquelada salían como discos negros macizos. Cada región cerrada se juzga por separado: un color uniforme igual al fondo cuenta como agujero, los degradados se quedan. Paquete 38.5 → 39.4 MB

**v0.20.11: iluminación y fondo de la vista 3D de insignias**

- La vista del emblema usaba dos luces, así que el metal solo tenía un brillo curvo y el relieve quedaba plano. Ahora son cinco luces direccionales — clave, superior, relleno frontal, contorno y un rebote tenue desde abajo — con la iluminación total sin cambios: gana profundidad sin lavarse
- Un segundo fondo más oscuro (un atardecer synthwave desenfocado hasta dejar solo manchas de color) va con el modo oscuro. La píldora de arriba a la derecha alterna claro y oscuro, sigue el ajuste del sistema por defecto y recuerda la elección; solo afecta a esta pantalla

**v0.20.8 – v0.20.10: pantalla de inicio**

- La pantalla de inicio pasa a arcilla: un disco blanco con sombreado ceñido y de borde nítido, la marca del pico y, en lugar del eslogan anterior, una cita al azar de un montañero — Mallory, Whymper, Muir, Boukreev, Viesturs, Tabei, Messner, Lowe o Bonatti. Se desvanece a los 1,7 s y con movimiento reducido apenas aparece. Nada más de la app ha cambiado Desde la v0.20.9 el splash del sistema ya no muestra ningún logo: el tema de arranque es solo el mismo color de fondo, así que la pantalla de arcilla empieza sobre un campo blanco (o casi negro). Cada cita va en su idioma original

**v0.20.6 – v0.20.7: calidad de dibujado de las insignias**

- El bloom va por defecto a máxima calidad: 256 px, cuatro niveles, MEDIUM. La rebaja automática antigua juzgaba desde el primer fotograma, así que unos cientos de milisegundos compilando shaders bastaban para apagarlo el resto de la sesión. Ahora espera 2,5 s, tolera fotogramas de 50 ms (20 fps) durante tres ventanas seguidas y baja en dos pasos: completo → reducido → apagado
- 4x MSAA se activa donde el controlador lo admite, comprobando la capacidad antes de crear la swap chain; si el intento falla, se vuelve a dibujar sin MSAA y se recuerda el fallo, para que una insignia nunca deje de verse por un ajuste de antialiasing

**v0.20.4 – v0.20.5: correcciones pequeñas**

- El modo desarrollador es invisible hasta desbloquearlo: toda la sección de ajustes, incluido «Desactivado · toca 7 veces el número de versión», deja de dibujarse para el usuario normal. El contador de siete toques sigue funcionando, pero ya no cuenta en voz alta
- La posición de desplazamiento también sobrevive dentro de la zona de pruebas de logros: entrar en la vista 3D de una insignia y volver, y pasar de ajustes a la zona de pruebas y volver
- El diálogo de desbloqueo ya no se cierra con un toque perdido: el primer toque en el fondo solo lo arma y muestra «Toca otra vez para cerrar»; el segundo cierra. «Aceptar» y el gesto de atrás siguen cerrando en un paso

**v0.20.0 – v0.20.3: insignias 3D**

- 34 logros se dibujan en 3D en tiempo real (Filament 1.75, glTF, metal y esmalte PBR). Arrastra para girar, pellizca para acercar, se mece despacio al soltar; el movimiento automático se queda en la cara frontal porque el reverso de una medalla es una placa lisa
- Desde la v0.20.15 las cuarenta tienen modelo; la tarjeta inclinada queda como recurso para futuras insignias
- Los diálogos de desbloqueo y vista previa usan el mismo motor, con un pedestal claro detrás del modelo (un modelo oscuro sobre tarjeta oscura desaparece) y 56 cintas desde los dos bordes. «Reducir movimiento» desactiva las cintas
- La página de exhibición se apoya en un paisaje muy desenfocado, generado por código (27 KB): cielo, cuatro crestas, niebla, sin licencias de fotos
- Direcciones de arrastre invertidas según los comentarios; la escena se aclaró y luego se replegó un poco para que las partes emisivas conserven contraste
- Las luces creadas dentro de los modelos ya sobreviven: las entidades `KHR_lights_punctual` se añaden explícitamente a la escena (las entidades del asset glTF no las incluyen) y su intensidad se escala a la exposición de la escena — 54 cd junto a una luz principal de 66.000 lux son invisibles si no. `KHR_materials_emissive_strength` se hornea en `emissiveFactor` al compilar, y el bloom queda activo, desactivándose solo si el tiempo de fotograma supera los 28 ms
- La biblioteca de modelos pasó de 17 a 34 en este tramo: rachas, perfectos, repaso, retirada, fin de senderismo y tres reexportaciones; paquete 21 → 39 MB
- **Los iconos de insignia salen ahora de los modelos**: el render frontal se recorta de su fondo negro a un icono transparente de 256 px, así que el muro, el diálogo de desbloqueo y la vista 3D muestran el mismo objeto. Las ilustraciones de IA de esas insignias se descartaron
- Ganancia del foco simplificada a 5× (el foco propio del modelo apunta al cuerpo de la medalla, ninguna ganancia lo hace visible de frente — eso se arregla en el origen de Blender), bloom bajado a 0,05
- v0.20.3: la posición de una lista sobrevive al viaje a una pantalla secundaria (ajustes → idioma, manual → artículo, perfil → ajustes, repaso → lección), y volver de una lección ya no reinicia el paginador de la ruta a la primera página

**v0.19.25 – v0.19.30: trabajo en la app**

- Logros realineados con el banco de 89 lecciones. `full_route` ahora significa las 68 lecciones de alta montaña — antes decía 66 y se juzgaba contra el itinerario abierto, así que terminar solo senderismo daba el «gran slam teórico». `five_zones` pasó a «seis zonas», acorde con las seis unidades de alta montaña. Cinco logros de senderismo añadidos, 35 → **40**
- El progreso de logros se calcula sobre los dos itinerarios en vez de sobre el elegido, así que cambiar de itinerario ya no mueve la meta
- Iconos de itinerario redibujados como siluetas macizas — pico nevado y bota de montaña — sustituyendo los caracteres `▲` / `≡` de la hoja; las píldoras superiores llevan los mismos dos a 14 dp
- Logo de arranque reducido un 40 %
- Pantalla de comentarios en el perfil, que publica en Formspree con salida por correo y portapapeles; el gestor de fallos guarda las últimas cinco excepciones no capturadas en el almacenamiento privado de la app
- Perfil y portada contaban el total de lecciones completadas contra las del itinerario actual, mostrando «68 / 21» al cambiar; corregido

La v0.19 es la ampliación de contenido; la estructura del banco y la lógica de puntuación no cambian.

- Lecciones 47 → 89: alta montaña ahora 68 (unidades de 6000 / 7000 / 8000 m, los catorce ochomiles), más 21 lecciones nuevas de senderismo
- Preguntas 711 → 1.533 (9.422 claves), manual 31 → 35 artículos (enterramiento y reanimación en alud, el trío de aludes, cuerdas fijas, cruce de ríos), banco v15 → v19
- **Nueve idiomas completos en las cuatro capas de contenido**: inglés, chino tradicional, japonés, ruso, alemán, español, francés, sueco e italiano. Sus claves coinciden con el inglés (10.554 / 10.554), así que nada vuelve atrás en tiempo de ejecución
- El alemán usa la forma impersonal con man, el español tutea con tú, el francés trata de vous, el sueco y el italiano de du / tu; cada uno sigue sus convenciones numéricas — alemán, español e italiano escriben 1.000 m, francés y sueco 5 000 m, todos con coma decimal
- Se eliminaron claves huérfanas de ediciones anteriores (`q.l2#5.c`, `m15#17.c`, `m16#18.c`, `m16#22.c`, `m16#24.c`) en los nueve idiomas, dejando cada conjunto limpio e idéntico
- El chino tradicional pasa por OpenCC s2twp más una tabla de terminología, validado por regresión contra las traducciones existentes (cero diferencias); el japonés es traducción real, con un registro para el banco y otro para el manual
- `tools/i18n/` ofrece tres comandos (`status.py` / `prep.py` / `apply.py`) para los nueve idiomas; el chino tradicional tiene además `tw.py` y `tw_entry.py`
- El banco está repartido en archivos: un solo archivo Kotlin de unas 2.500 entradas tumba el compilador, así que cada una de las nueve lenguas se parte en tres (`Pack*Quiz.kt` / `Quiz2` / `Quiz3`)

De la v0.16 a la v0.18 todo fue trabajo multilingüe; la estructura del banco y la lógica de puntuación no cambiaron.

- Textos de interfaz divididos en 10 conjuntos `res/values*`; el idioma se fija a mano o sigue al sistema, con efecto inmediato
- Nueva capa de contenido `data/i18n/`, cuatro paquetes por idioma: entrada, puntos clave, cuerpo del manual, banco de preguntas. 4.727 elementos por idioma
- Las cuatro capas completas en los nueve idiomas traducidos (zh-TW, en, ja, de, fr, es, it, ru, sv); los ocho distintos del inglés verificados clave a clave contra el original, cero faltantes
- Terminología alineada con el uso de cada idioma; dosis, niveles de alud, pendientes y temperaturas sin tocar
- Añadido `tools/i18ncheck.py` para ver la cobertura de las cuatro capas con un comando — se acabó el «traducido, pero falta una lección»
- De paso se corrigieron varios textos: un error de coma antes de raya (140 en sueco, 5 en alemán y en español), dos respuestas de rellenar con comilla sobrante y tres decimales franceses

Antes:

- v0.15.1: limpieza de textos dentro de la app, fuera los marcadores literales `**`, reescritura de varias explicaciones, la pantalla de acceso deja de prometer una sincronización no implementada
- v0.15.0: nueve artículos más del manual a partir de libros chinos (h23–h31), nueva categoría «liderazgo en el medio natural», 22 → 31 artículos
- v0.14.0: seis artículos de guía de SEVEN SUMMIT TREKS
- v0.13.x: ajustes, página de perfil, muro de insignias; gesto de atrás en pantallas secundarias

## Licencia

PolyForm Noncommercial License 1.0.0 — solo uso no comercial. El uso comercial requiere permiso escrito aparte; véanse `LICENSE` y `NOTICE`.
