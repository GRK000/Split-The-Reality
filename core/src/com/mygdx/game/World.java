package com.mygdx.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.Random;

public class World {

    public static final int TILE_SIZE_SRC = 16;
    private static final int TILE_SIZE_SRC_DIM1 = 32;
    private static final int TILESET_DIM1_MARGIN = 0;
    private static final int TILESET_DIM1_SPACING = 2;
    private static final int TILESET_DIM1_COLS = 8;
    private static final int TILESET_DIM1_ROWS = 8;
    private int tileSize;

    // Tilesets
    private Texture tilesetDim0;
    private TextureRegion[][] tilesDim0;
    private Texture tilesetDim1;
    private TextureRegion[][] tilesDim1;

    // Mapas
    private static final int NUM_DIMS = 2;
    private int[][][] mapas;         // [dim][y][x]
    private int mapAncho, mapAlto;

    // Generación incremental
    private static final int INITIAL_WIDTH = 120;
    private static final int CHUNK_SIZE    = 60;
    private static final int MARGEN_GEN    = 30;
    private int[] generadoHastaX = new int[NUM_DIMS];

    // Configuración de dificultad
    public enum Difficulty { EASY, NORMAL, HARD }

    private static final class DifficultyConfig {
        final int minPlatW, maxPlatW;
        final int minGapX, maxGapX;
        final int minPlatY, maxPlatY;
        final int maxGroundGap;
        final int switchBlockSize;
        final int switchGapStart;
        final int switchGapWidth;
        final float groundThreshold;

        DifficultyConfig(int minPlatW, int maxPlatW, int minGapX, int maxGapX,
                         int minPlatY, int maxPlatY, int maxGroundGap,
                         int switchBlockSize, int switchGapStart, int switchGapWidth,
                         float groundThreshold) {
            this.minPlatW = minPlatW;
            this.maxPlatW = maxPlatW;
            this.minGapX = minGapX;
            this.maxGapX = maxGapX;
            this.minPlatY = minPlatY;
            this.maxPlatY = maxPlatY;
            this.maxGroundGap = maxGroundGap;
            this.switchBlockSize = switchBlockSize;
            this.switchGapStart = switchGapStart;
            this.switchGapWidth = switchGapWidth;
            this.groundThreshold = groundThreshold;
        }
    }

    private static final float INACTIVE_DIM_ALPHA = 0.25f;

    // Parámetros de suelo
    // El suelo NO es continuo: mediante una suma de ondas sinusoidales por dimensión se generan huecos. Cada dimensión tiene una fase diferente.
    public static final int SUELO_INICIAL = 24;  // tiles garantizados al inicio para evitar muertes injustas por falta de suelo.
    private int spawnTileX = 0;

    private Random[] randoms = new Random[NUM_DIMS];
    private int[] ultimoSueloX = new int[NUM_DIMS];
    private DifficultyConfig config;
    private float difficultyPhaseOffset;
    private int floorIndex;
    private int sectionSizeTiles;
    private int sectionOffsetTiles;
    private int sectionBridgePadding;
    private float noiseFreqA;
    private float noiseFreqB;
    private float noiseAmpA;
    private float noiseAmpB;
    private float groundBias;

    // Dimensión activa
    private int currentDimension = 0;

    // CONSTANTES DE TILES
    private static final int TILE_COLS = 8;

    public static final int TILE_VACIO = -1;

    // Dimensión 0
    public static final int D0_SUELO_INF = tileId(4, 1);
    public static final int D0_SUELO_MED = tileId(3, 1);
    public static final int D0_SUELO_SUP = tileId(2, 1);
    public static final int D0_PLAT      = tileId(2, 3);

    // Dimensión 1
    public static final int D1_SUELO_INF = tileId(4, 1);
    public static final int D1_SUELO_MED = tileId(3, 1);
    public static final int D1_SUELO_SUP = tileId(0, 1);
    public static final int D1_PLAT      = tileId(1, 1);

    public World(int screenWidth, int screenHeight, int tileSize, Difficulty difficulty) {
        this(screenWidth, screenHeight, tileSize, difficulty, 0, 0);
    }

    public World(int screenWidth, int screenHeight, int tileSize, Difficulty difficulty, int floorIndex, int spawnTileX) {
        this.tileSize = tileSize;
        this.floorIndex = floorIndex;
        mapAlto  = screenHeight / tileSize + 2;
        mapAncho = INITIAL_WIDTH;
        this.config = getConfig(difficulty);
        this.difficultyPhaseOffset = difficulty.ordinal() * 1.97f + floorIndex * 0.73f;

        mapas = new int[NUM_DIMS][mapAlto][mapAncho];

        tilesetDim0 = new Texture("tileset_dim0.png");
        tilesDim0   = TextureRegion.split(tilesetDim0, TILE_SIZE_SRC, TILE_SIZE_SRC);
        tilesetDim0.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        tilesetDim1 = new Texture("tileset_dim1.png");
        tilesetDim1.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tilesDim1   = splitWithSpacing(
                tilesetDim1,
                TILE_SIZE_SRC_DIM1,
                TILE_SIZE_SRC_DIM1,
                TILESET_DIM1_MARGIN,
                TILESET_DIM1_SPACING,
                TILESET_DIM1_COLS,
                TILESET_DIM1_ROWS
        );

        this.spawnTileX = spawnTileX;

        // Semillas por dificultad: reproducible dentro de cada dificultad y distinta entre ellas.
        long salt = 1000L * (difficulty.ordinal() + 1L);
        long floorSalt = 7919L * (floorIndex + 1L);
        randoms[0] = new Random(42L + salt + floorSalt);
        randoms[1] = new Random(137L + salt * 3L + floorSalt * 5L);

        Random profileRnd = new Random(911L + floorSalt * 13L + difficulty.ordinal() * 31L);
        sectionSizeTiles = config.switchBlockSize + 8 + Math.min(28, floorIndex * 2) + profileRnd.nextInt(7);
        sectionOffsetTiles = profileRnd.nextInt(Math.max(1, sectionSizeTiles));
        sectionBridgePadding = 2 + profileRnd.nextInt(2);
        noiseFreqA = 0.22f + profileRnd.nextFloat() * 0.18f;
        noiseFreqB = 0.54f + profileRnd.nextFloat() * 0.26f;
        noiseAmpA = 4.1f + profileRnd.nextFloat() * 1.9f;
        noiseAmpB = 1.9f + profileRnd.nextFloat() * 1.6f;
        groundBias = (profileRnd.nextFloat() - 0.5f) * 0.45f;

        for (int d = 0; d < NUM_DIMS; d++)
            for (int y = 0; y < mapAlto; y++)
                for (int x = 0; x < mapAncho; x++)
                    mapas[d][y][x] = TILE_VACIO;

        for (int d = 0; d < NUM_DIMS; d++) 
            ultimoSueloX[d] = -1;
    }

    // Generación
    public void generarNivel() {
        for (int d = 0; d < NUM_DIMS; d++)
            generarHasta(d, mapAncho);
    }

    private void generarHasta(int dim, int targetX) {
        if (targetX > mapAncho)
            expandirMapa(targetX + CHUNK_SIZE);

        int startX     = generadoHastaX[dim];
        Random  rnd    = randoms[dim];
        int[][] mapa   = mapas[dim];
        boolean esDim0 = (dim == 0);

        int topGroundTile = 2;

        // Suelo en secciones alternas por dimensión para forzar cambios de mundo.
        for (int x = startX; x < targetX; x++) {
            boolean suelo;
            if (x < SUELO_INICIAL) {
                suelo = true;
            } else if (isNearSpawn(x)) {
                    suelo = true;
            } else if (isSectionBridge(x)) {
                // En fronteras de sección, ambas dimensiones tienen apoyo para permitir el cambio.
                suelo = true;
            } else {
                suelo = requiredDimensionAt(x) == dim && haySuelo(dim, x);
            }

            if (suelo && ultimoSueloX[dim] >= 0 && x - ultimoSueloX[dim] > config.maxGroundGap) {
                suelo = false;
            }

            if (!suelo && ultimoSueloX[dim] >= 0 && x - ultimoSueloX[dim] >= config.maxGroundGap) {
                suelo = true;
            }

            if (suelo) {
                mapa[0][x] = esDim0 ? D0_SUELO_INF : D1_SUELO_INF;
                mapa[1][x] = esDim0 ? D0_SUELO_MED : D1_SUELO_MED;
                mapa[2][x] = esDim0 ? D0_SUELO_SUP : D1_SUELO_SUP;
                ultimoSueloX[dim] = x;
            }
        }

        // Plataformas alcanzables y con altura limitada por el salto.
        int x = Math.max(startX, 10 + dim * 3 + floorIndex % 5);
        int platTile = esDim0 ? D0_PLAT : D1_PLAT;
        int lastY = topGroundTile + 1;
        int extraGap = 1 + floorIndex / 2;
        int gapMin = config.minGapX + extraGap;
        int gapMax = config.maxGapX + extraGap + 2;
        if (gapMax < gapMin) gapMax = gapMin;

        while (x < targetX - config.maxPlatW) {
            int gap = gapMin + rnd.nextInt(gapMax - gapMin + 1);
            x += gap;
            if (x >= targetX - config.maxPlatW)
                break;

            if (x >= SUELO_INICIAL && requiredDimensionAt(x) != dim && !isSectionBridge(x)) {
                x++;
                continue;
            }

            if (isSectionBridge(x) && rnd.nextFloat() < 0.55f) {
                x++;
                continue;
            }

            int ancho = config.minPlatW + rnd.nextInt(config.maxPlatW - config.minPlatW + 1);
            int maxY  = Math.min(config.maxPlatY, mapAlto - 2);
            int minY  = Math.max(config.minPlatY, topGroundTile + 1);
            if (maxY < minY) break;

            int yMinReach = Math.max(minY, lastY - 1);
            int riseCap = 2 + Math.min(2, floorIndex / 3);
            int yMaxReach = Math.min(maxY, lastY + riseCap);
            int y = yMinReach + rnd.nextInt(yMaxReach - yMinReach + 1);

            for (int i = 0; i < ancho; i++) {
                if (x + i < mapAncho && y < mapAlto) mapa[y][x + i] = platTile;
            }
            lastY = y;
            x += ancho;
        }

        generadoHastaX[dim] = targetX;
    }

    /**
     * Determina de forma completamente determinista si la columna x
     * debe tener suelo en la dimensión dada.
     *
     * Usa la suma de dos sinusoides con frecuencias distintas.
     * Cada dimensión tiene una fase diferente (dim * 7.391 rad),
     * por lo que sus patrones de suelo son complementarios:
     * donde una dimensión tiene hueco, la otra suele tener suelo.
     *
     * Umbral 1.2 -> aproximadamente el 40 % de los tiles tiene suelo.
     */
    private boolean haySuelo(int dim, int x) {
        if (x < SUELO_INICIAL)
            return true;
        if (Math.abs(x - spawnTileX) < 12)
            return true;
        double fase = dim * 7.391 + difficultyPhaseOffset;
        double onda = Math.sin(x * noiseFreqA + fase) * noiseAmpA
                + Math.sin(x * noiseFreqB + fase * 1.73) * noiseAmpB;
        return onda > config.groundThreshold + groundBias;
    }

    private int requiredDimensionAt(int x) {
        if (x < SUELO_INICIAL) return 0;
        int bloque = (x + sectionOffsetTiles) / sectionSizeTiles;
        return Math.floorMod(bloque + floorIndex, NUM_DIMS);
    }

    private boolean isSectionBridge(int x) {
        if (x < SUELO_INICIAL) return false;
        int local = Math.floorMod(x + sectionOffsetTiles, sectionSizeTiles);
        return local < sectionBridgePadding || local >= sectionSizeTiles - sectionBridgePadding;
    }

    private static DifficultyConfig getConfig(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return new DifficultyConfig(4, 7, 1, 2, 3, 5, 2, 32, 12, 5, 1.10f);
            case HARD:
                return new DifficultyConfig(3, 5, 1, 3, 3, 5, 3, 28, 11, 6, 1.78f);
            case NORMAL:
            default:
                return new DifficultyConfig(3, 6, 1, 3, 3, 6, 3, 26, 10, 6, 1.55f);
        }
    }

    private void expandirMapa(int nuevoAncho) {
        int[][][] nuevo = new int[NUM_DIMS][mapAlto][nuevoAncho];
        for (int d = 0; d < NUM_DIMS; d++)
            for (int y = 0; y < mapAlto; y++) {
                System.arraycopy(mapas[d][y], 0, nuevo[d][y], 0, mapAncho);
                for (int x = mapAncho; x < nuevoAncho; x++) nuevo[d][y][x] = TILE_VACIO;
            }
        mapas    = nuevo;
        mapAncho = nuevoAncho;
    }

    /**
     * Llamar cada frame para asegurar que el mundo está generado
     * por delante del jugador. Genera ambas dimensiones a la vez.
     */
    public void asegurarGenerado(float jugadorX) {
        int tileX = (int) (jugadorX / tileSize);
        if (tileX + MARGEN_GEN > generadoHastaX[0]) {
            int target = tileX + MARGEN_GEN + CHUNK_SIZE;
            for (int d = 0; d < NUM_DIMS; d++)
                generarHasta(d, target);
        }
    }

    // CAMBIO DE DIMENSIÓN
    public void switchDimension() {
        currentDimension = 1 - currentDimension;
    }

    public int getCurrentDimension() {
        return currentDimension;
    }

    // RENDER

    public void render(SpriteBatch batch, float camX, float viewWidth) {
        int startTX = Math.max(0,       (int) ((camX - viewWidth / 2) / tileSize) - 1);
        int endTX   = Math.min(mapAncho, (int) ((camX + viewWidth / 2) / tileSize) + 2);

        int inactiveDim = 1 - currentDimension;
        drawDimension(batch, inactiveDim, startTX, endTX, INACTIVE_DIM_ALPHA);
        drawDimension(batch, currentDimension, startTX, endTX, 1f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawDimension(SpriteBatch batch, int dim, int startTX, int endTX, float alpha) {
        int[][] mapa = mapas[dim];
        TextureRegion[][] activeTiles = (dim == 0) ? tilesDim0 : tilesDim1;

        batch.setColor(1f, 1f, 1f, alpha);

        for (int y = 0; y < mapAlto; y++)
            for (int x = startTX; x < endTX; x++) {
                int tile = mapa[y][x];
                if (tile != TILE_VACIO) {
                    batch.draw(
                            activeTiles[tile / TILE_COLS][tile % TILE_COLS],
                            x * tileSize, y * tileSize,
                            tileSize, tileSize
                    );
                }
            }
    }

    // COLISIONES
    private int[][] getActiveMapa() { return mapas[currentDimension]; }

    private int getActiveTile(int tx, int ty) {
        if (tx < 0 || tx >= mapAncho || ty < 0 || ty >= mapAlto) return TILE_VACIO;
        return getActiveMapa()[ty][tx];
    }

    private boolean isPlatformTile(int tile) {
        return tile == D0_PLAT || tile == D1_PLAT;
    }

    // Un tile sólido es cualquier tile que no sea vacío, incluyendo plataformas.
    private boolean isSolid(int tx, int ty) {
        return getActiveTile(tx, ty) != TILE_VACIO;
    }

    // Un tile completamente sólido es cualquier tile que no sea vacío ni plataforma.
    private boolean isFullSolid(int tx, int ty) {
        int tile = getActiveTile(tx, ty);
        return tile != TILE_VACIO && !isPlatformTile(tile);
    }

    private boolean isSolidW(float wx, float wy) {
        return isSolid((int)(wx / tileSize), (int)(wy / tileSize));
    }

    private boolean isFullSolidW(float wx, float wy) {
        return isFullSolid((int)(wx / tileSize), (int)(wy / tileSize));
    }

    private float findLandingY(float sampleX, float prevFootY, float newFootY) {
        int tx = (int)(sampleX / tileSize);
        int fromTy = (int)Math.floor(newFootY / tileSize);
        int toTy = (int)Math.floor((prevFootY - 0.001f) / tileSize);

        for (int ty = toTy; ty >= fromTy; ty--) {
            int tile = getActiveTile(tx, ty);
            if (tile == TILE_VACIO) continue;

            float tileTop = (ty + 1) * tileSize;
            if (prevFootY >= tileTop && newFootY <= tileTop) {
                return tileTop;
            }
        }
        return Float.MIN_VALUE;
    }

    // Detecta colisión al caer, evitando enganchar plataformas desde abajo.
    public float checkColisionSuelo(float px, float prevPy, float py, float pw, float velY) {
        if (velY > 0) return Float.MIN_VALUE;

        float m = pw / 3f;
        float prevFootY = prevPy;
        float newFootY = py;

        float hitL = findLandingY(px + m, prevFootY, newFootY);
        float hitR = findLandingY(px + pw - m, prevFootY, newFootY);

        if (hitL == Float.MIN_VALUE) return hitR;
        if (hitR == Float.MIN_VALUE) return hitL;
        return Math.max(hitL, hitR);
    }

    // Busca el primer suelo completo (sin plataformas) al caer desde searchTop hasta searchBottom.
    public float findFullSolidGroundY(float px, float searchTop, float searchBottom, float pw) {
        float m = pw / 3f;
        int fromTy = (int) Math.floor(searchBottom / tileSize);
        int toTy = (int) Math.floor((searchTop - 0.001f) / tileSize);

        for (int ty = toTy; ty >= fromTy; ty--) {
            int txL = (int) ((px + m) / tileSize);
            int txR = (int) ((px + pw - m) / tileSize);
            if (isFullSolid(txL, ty) || isFullSolid(txR, ty)) {
                return (ty + 1) * tileSize;
            }
        }
        return Float.MIN_VALUE;
    }

    private float findCeilingY(float sampleX, float prevHeadY, float newHeadY, float ph) {
        int tx = (int)(sampleX / tileSize);
        int fromTy = (int)Math.floor(prevHeadY / tileSize);
        int toTy = (int)Math.floor((newHeadY + 0.001f) / tileSize);

        for (int ty = fromTy; ty <= toTy; ty++) {
            int tile = getActiveTile(tx, ty);
            // Las plataformas no bloquean al subir para evitar choque invisible por debajo.
            if (tile == TILE_VACIO || isPlatformTile(tile)) continue;

            float tileBottom = ty * tileSize;
            if (prevHeadY <= tileBottom && newHeadY >= tileBottom) {
                return tileBottom - ph;
            }
        }
        return Float.MAX_VALUE;
    }

    /*public float checkColisionTecho(float px, float prevPy, float py, float pw, float ph, float velY) {
        if (velY <= 0) return Float.MAX_VALUE;

        float m = pw / 3f;
        float prevHeadY = prevPy + ph;
        float newHeadY = py + ph;

        float hitL = findCeilingY(px + m, prevHeadY, newHeadY, ph);
        float hitR = findCeilingY(px + pw - m, prevHeadY, newHeadY, ph);

        if (hitL == Float.MAX_VALUE) return hitR;
        if (hitR == Float.MAX_VALUE) return hitL;
        return Math.min(hitL, hitR);
    }*/

    /** Colisión lateral derecha con bloqueo total, incluyendo plataformas. */
    public float resolveColisionDerecha(float nuevaX, float y, float pw, float ph) {
        float m      = 6f;
        float rightX = nuevaX + pw + 1;
        if (isFullSolidW(rightX, y + m) || isFullSolidW(rightX, y + ph - m)) {
            return (int)(rightX / tileSize) * tileSize - pw - 0.1f;
        }
        return nuevaX;
    }

    /** Colisión lateral izquierda con bloqueo total, incluyendo plataformas. */
    public float resolveColisionIzquierda(float nuevaX, float y, float pw, float ph) {
        float m     = 6f;
        float leftX = nuevaX - 1;
        if (isFullSolidW(leftX, y + m) || isFullSolidW(leftX, y + ph - m)) {
            return ((int)(leftX / tileSize) + 1) * tileSize + 0.1f;
        }
        return nuevaX;
    }

    // UTILIDADES
    public int getTileSize() { return tileSize; }
    public int getMapAncho() { return mapAncho; }
    public int getMapAlto()  { return mapAlto;  }

    private boolean isNearSpawn(int x) {
        return Math.abs(x - spawnTileX) <= 12;
    }
    private static TextureRegion[][] splitWithSpacing(
            Texture texture,
            int tileW,
            int tileH,
            int margin,
            int spacing,
            int cols,
            int rows
    ) {
        TextureRegion[][] regions = new TextureRegion[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = margin + col * (tileW + spacing);
                int y = margin + row * (tileH + spacing);
                regions[row][col] = new TextureRegion(texture, x, y, tileW, tileH);
            }
        }
        return regions;
    }

    private static int tileId(int fila, int col) {
        return fila * TILE_COLS + col;
    }

    public void dispose() {
        tilesetDim0.dispose();
        tilesetDim1.dispose();
    }
}