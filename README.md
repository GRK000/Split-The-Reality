# Split the Reality

Juego 2D estilo action-platformer hecho con **LibGDX**. El loop central mezcla:

- desplazamiento lateral,
- cambio entre 2 dimensiones del mundo,
- combate contra enemigos con ataques melee y proyectiles,
- progresion por plantas (floors) con dificultad creciente,
- economia simple (oro + tienda de mejoras).

## 1) Stack tecnico

- **Lenguaje:** Java
- **Engine:** LibGDX `1.12.1`
- **Build:** Gradle Wrapper (`gradle-8.5`)
- **Modulos:** `core`, `desktop`, `android`, `ios`, `html`
- **Render / utilidades LibGDX:** `SpriteBatch`, `ShapeRenderer`, `OrthographicCamera`, `TextureRegion`

Archivos base:

- `build.gradle`
- `settings.gradle`
- `core/src/com/mygdx/game/MyGdxGame.java`
- `core/src/com/mygdx/game/World.java`

## 2) Arquitectura del proyecto

### `core`
Contiene la logica compartida del juego:

- `MyGdxGame`: game loop, input, menus, UI, flujo de plantas, combate.
- `World`: generacion procedural por tiles, dimensiones, colisiones.
- `Player`: stats, ataque, invulnerabilidad temporal, mejoras.
- `Enemy`: IA basica por tipo, animaciones, hazards/proyectiles.
- `CombatStats`: calculo de dano y estado de vida.

### `desktop`
Entrada para escritorio con LWJGL3:

- `desktop/src/com/mygdx/game/DesktopLauncher.java`

### `android`
Launcher Android y configuracion de APK:

- `android/src/com/mygdx/game/AndroidLauncher.java`
- `android/build.gradle`
- `android/AndroidManifest.xml`

### `ios`, `html`
Soporte multiplataforma generado por plantilla LibGDX.

## 3) Mecanicas principales

## Movimiento y control

- Movimiento horizontal por teclado (`A/D`, flechas) o joystick tactil.
- Salto (`SPACE`, `W`, `UP` o boton tactil).
- Cambio de dimension (`Q/E` o boton tactil).
- Ataque (`F`, `J`, `CTRL derecho` o boton tactil).
- Pausa (`ESC`, `P` o boton tactil).

La hitbox del jugador es mas pequena que el sprite para mejorar el feeling de colision.

## Cambio de dimension

El mundo se renderiza con 2 capas (dimension activa e inactiva). La inactiva se dibuja con alpha reducido (`INACTIVE_DIM_ALPHA = 0.25f`) para dar contexto visual sin perder legibilidad.

## Combate

- El jugador usa ataque melee por ventana de tiempo (`attackDuration`) y cooldown (`attackCooldown`).
- El dano final se calcula con reduccion por defensa.
- Hay probabilidad de critico en `CombatStats`.
- Se generan textos flotantes de dano para feedback.

## Enemigos

Tipos actuales (`Enemy.Type`):

- `MAGE_BLUE`
- `MAGE_MAGENTA`
- `NECROMANCER`
- `NIGHTBORNE`

Comportamiento:

- **Mages:** invocan obeliscos que caen desde arriba (`HazardType.OBELISK`) con telegraph.
- **Necromancer:** dispara orbes dirigidos al jugador (`HazardType.DARK_ORB`).
- **Nightborne:** ataque melee por ventana de animacion cuando esta cerca.

Cada enemigo escala stats por `floorIndex`.

## Progresion

- El juego avanza por plantas infinitas (`floorIndex`).
- Al completar una planta, se otorga oro y se inicia la siguiente.
- La direccion de avance alterna por planta (este/oeste).
- Dificultad efectiva sube por planta (con tope en `HARD`).

## Tienda

Mejoras comprables con oro:

- Vida maxima
- Ataque
- Defensa
- Velocidad
- Potencia de salto

La tienda esta disponible desde menu principal y desde pausa.

## 4) Generacion tecnica del mundo (`World`)

El mapa usa tiles y se genera incrementalmente por chunks.

### Modelo general

- `NUM_DIMS = 2` dimensiones.
- `mapas[dim][y][x]` guarda tiles por dimension.
- `TILE_VACIO = -1` representa vacio.
- Se expande dinamicamente (`expandirMapa`) cuando hace falta.

### Generacion incremental

- Anchura inicial: `INITIAL_WIDTH = 120`
- Chunk de extension: `CHUNK_SIZE = 60`
- Margen de pre-generacion: `MARGEN_GEN = 30`
- `asegurarGenerado(jugadorX)` mantiene mundo generado por delante de camara/jugador.

### Suelo procedural determinista

`haySuelo(dim, x)` usa suma de sinusoides con frecuencias y amplitudes configuradas por semilla:

- `onda = sin(x * noiseFreqA + fase) * noiseAmpA + sin(x * noiseFreqB + fase * 1.73) * noiseAmpB`
- Se compara contra `groundThreshold + groundBias`.

Caracteristicas:

- Determinista por dificultad y planta (semillas reproducibles).
- Soporta huecos, pero limita gaps extremos con `maxGroundGap`.
- `SUELO_INICIAL` garantiza zona segura de inicio.

### Forzado de cambio de dimension

`requiredDimensionAt(x)` divide el mundo en secciones y define cual dimension debe tener suelo en cada bloque.

`isSectionBridge(x)` crea zonas puente en bordes de seccion donde ambas dimensiones pueden dar apoyo, para evitar softlocks al cambiar de dimension.

### Plataformas

Se colocan plataformas alcanzables con restricciones de:

- ancho minimo/maximo,
- separacion horizontal,
- rango de altura,
- pendiente maxima entre plataformas sucesivas.

Estos parametros viven en `DifficultyConfig` y cambian por `EASY/NORMAL/HARD`.

### Colisiones

Sistema por tiles en la dimension activa:

- `checkColisionSuelo(...)`: aterrizaje al caer (sin enganchar por debajo de plataformas).
- `findFullSolidGroundY(...)`: busca suelo completo para spawn/respawn seguro.
- `resolveColisionDerecha/Izquierda(...)`: bloqueo lateral contra tiles solidos completos.

## 5) Flujo de juego y estados

`MyGdxGame` define `GameState`:

- `MAIN_MENU`
- `DIFFICULTY_MENU`
- `SHOP_MENU`
- `PAUSED`
- `GAME_OVER`
- `PLAYING`

Ciclo durante `PLAYING`:

1. Input y acciones (mover, saltar, atacar, cambiar dimension).
2. Fisica simple (gravedad + colisiones por tiles).
3. Update de enemigos y hazards.
4. Resolucion de dano y muertes.
5. Progreso de planta y transicion si llega al objetivo.
6. Render de fondo, mundo, entidades, HUD y overlays.

## 6) Setup del entorno

## Requisitos

- **Git**
- **JDK** instalado
- **Android SDK** (solo si vas a compilar Android)

Notas importantes de compatibilidad:

- El codigo fuente esta configurado con `sourceCompatibility = 1.8`.
- El proyecto usa Android Gradle Plugin `8.0.2`, que en entornos modernos suele requerir JDK mas reciente para build de Android.
- Si aparece error de version Java, revisa `JAVA_HOME` y la configuracion del IDE/Gradle.

## Clonar e iniciar

```powershell
git clone <tu-repo>
Set-Location "SplitTheReality"
```

## Ejecutar en desktop (Windows)

```powershell
.\gradlew.bat :desktop:run
```

## Generar jar desktop

```powershell
.\gradlew.bat :desktop:dist
```

## Compilar Android (debug APK)

Asegurate de tener `local.properties` con `sdk.dir=...` o variable `ANDROID_HOME`.

```powershell
.\gradlew.bat :android:assembleDebug
```

Para lanzar en dispositivo/emulador (si `adb` esta disponible):

```powershell
.\gradlew.bat :android:run
```

## 7) Estructura de assets

- `assets/` contiene sprites, fondos, tilesets y audio.
- Ejemplos clave:
  - `tileset_dim0.png`, `tileset_dim1.png`
  - `RUN.png`, `JUMP.png`, `IDLE.png`, `ATTACK 1.png`
  - `sounds/song.mp3`

El modulo `desktop` toma recursos desde `../assets` como working directory.

## 8) Puntos tecnicos a extender

- Balance de `DifficultyConfig` por planta.
- Nuevos patrones de hazards y enemigos.
- Persistencia de progreso (save/load).
- Separacion de sistemas (input/combat/ui) en clases dedicadas para escalar mantenibilidad.

---

Si quieres, se puede agregar una segunda version del README orientada a contribucion (convenciones de codigo, roadmap y checklist de PR).
