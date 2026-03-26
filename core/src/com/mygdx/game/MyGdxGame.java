package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class MyGdxGame extends ApplicationAdapter {

	// Pantalla
	int width, height;

	//Cámaras
	OrthographicCamera cam;    // sigue al jugador
	OrthographicCamera uiCam;

	// Render
	SpriteBatch batch;
	ShapeRenderer shapes;

	// Personaje
	Texture textura, texturaJump, texturaIdle, texturaAttack;
	Texture backgroundLoopTexture, backgroundEndTexture, backgroundMenuTexture, blackPixelTexture;
	Texture mageBlueTexture, mageMagentaTexture, necromancerTexture, nightborneTexture, obeliskTexture;
	TextureRegion bgRegion;
	Sprite personaje;
	static final int ANCHO_PERSONA  = 96;
	static final int ALTO_PERSONA   = 60;
	static final int FRAME_W = 96;
	static final int FRAME_H = 84;
	static final float HITBOX_W = 44f;
	static final float HITBOX_H = 50f;
	static final float HITBOX_OFF_X = (ANCHO_PERSONA - HITBOX_W) * 0.5f;

	static final float HITBOX_OFF_Y = (ALTO_PERSONA - HITBOX_H) * 0.5f + 8f;

	int paso = 0, cambiopaso = 0;
	int menuIdleFrame = 0;
	float menuIdleTimer = 0f;
	static final int NUM_FRAMES_RUN  = 8;
	static final int NUM_FRAMES_JUMP = 5;
	static final int NUM_FRAMES_IDLE = 7;
	static final int NUM_FRAMES_ATTACK = 6;
	static final float DIM_FLASH_DURATION = 0.08f;
	static final float ATTACK_REACH = 44f;
	static final float ATTACK_BOX_H = 40f;
	static final float ICON_ALPHA = 0.88f;
	static final int FLOOR_BASE_TILES = 180;
	static final int FLOOR_GROW_TILES = 40;
	static final float FLOOR_TEXT_DURATION = 2.8f;

	private enum AnimState { RUN, JUMP, IDLE, ATTACK }

	private static final class DamageText {
		float x;
		float y;
		String text;
		float timer;
		float r;
		float g;
		float b;

		DamageText(float x, float y, String text, float r, float g, float b) {
			this.x = x;
			this.y = y;
			this.text = text;
			this.timer = 0.6f;
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}

	private AnimState animState = AnimState.RUN;
	float dimFlashTimer = 0f;
	Player player;
	final Rectangle attackRectTmp = new Rectangle();
	final Rectangle enemyRectTmp = new Rectangle();
	final Rectangle playerRectTmp = new Rectangle();
	final Rectangle hazardRectTmp = new Rectangle();
	List<Enemy> enemies = new ArrayList<>();
	List<DamageText> damageTexts = new ArrayList<>();

	boolean saltando = false;
	boolean enSuelo  = false;
	float velocidadY = 0f;
	float velocidadX = 0f;
	static final float GRAVEDAD      = -0.5f;
	boolean mirandoDerecha = true;

	World world;

	private enum GameState { MAIN_MENU, DIFFICULTY_MENU, SHOP_MENU, PAUSED, GAME_OVER, PLAYING }
	private GameState gameState = GameState.MAIN_MENU;

	BitmapFont font;
	GlyphLayout glyph;

	Rectangle btnPlayRect;
	Rectangle btnEasyRect;
	Rectangle btnNormalRect;
	Rectangle btnHardRect;
	Rectangle btnBackRect;
	Rectangle btnShopRect;
	Rectangle btnShopHpRect;
	Rectangle btnShopAtkRect;
	Rectangle btnShopDefRect;
	Rectangle btnShopSpdRect;
	Rectangle btnShopJumpRect;
	Rectangle btnShopBackRect;
	Rectangle btnPauseResumeRect;
	Rectangle btnPauseMenuRect;
	Rectangle btnPauseShopRect;
	Rectangle btnGameOverMenuRect;
	Rectangle authorRect;
	String authorInfo = "Autor: Gorka Tamayo";
	boolean authorEditing = false;
	static final int AUTHOR_MAX_CHARS = 42;
	static final int FLOOR_REWARD_BASE = 24;
	static final int FLOOR_REWARD_SCALE = 8;
	static final int SHOP_COST_HP = 45;
	static final int SHOP_COST_ATK = 55;
	static final int SHOP_COST_DEF = 50;
	static final int SHOP_COST_SPD = 60;
	static final int SHOP_COST_JUMP = 48;

	World.Difficulty selectedBaseDifficulty = World.Difficulty.NORMAL;
	int floorIndex = 0;
	boolean advanceRight = true;
	float floorStartX = 10f;
	float floorGoalX = 10f;
	float floorProgress = 0f;
	float floorMessageTimer = 0f;
	String floorMessage = "";
	boolean shopBackToGame = false;
	int playerLives = 3;

	Music music;

	float joyBaseX, joyBaseY;      // posición fija del aro exterior
	float joyThumbX, joyThumbY;    // posición actual del pulgar
	float joyDirX = 0f;            // dirección normalizada [-1, 1]
	int   joyPointer = -1;         // índice de toque activo (-1 = libre)

	static final float JOY_OUTER_R = 90f;
	static final float JOY_INNER_R = 38f;

	float btnJumpX, btnJumpY;
	float btnDimX,  btnDimY;
	float btnAttackX, btnAttackY;
	float btnPauseX, btnPauseY;
	static final float BTN_R = 58f;
	static final float PAUSE_BTN_R = 44f;

	// Color de fondo según dimensión RGB

	static final float[][] BG = {
			{ 0.10f, 0.12f, 0.22f },   // Dim 0 -> Color fons blau fosc
			{ 0.22f, 0.05f, 0.28f }    // Dim 1 -> Color fons violeta
	};

	@Override
	public void create() {
		width  = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();

		cam   = new OrthographicCamera(); cam.setToOrtho(false, width, height);
		uiCam = new OrthographicCamera(); uiCam.setToOrtho(false, width, height);

		batch  = new SpriteBatch();
		shapes = new ShapeRenderer();
		font = new BitmapFont();
		font.getData().setScale(1.5f);
		glyph = new GlyphLayout();

		textura = new Texture(Gdx.files.internal("RUN.png"));
		texturaJump = new Texture(Gdx.files.internal("JUMP.png"));
		texturaIdle = new Texture(Gdx.files.internal("IDLE.png"));
		texturaAttack = new Texture(Gdx.files.internal("ATTACK 1.png"));
		player = Player.createDefault();

		mageBlueTexture = new Texture(Gdx.files.internal("mage_guardian-blue.png"));
		mageMagentaTexture = new Texture(Gdx.files.internal("mage_guardian-magenta.png"));
		necromancerTexture = new Texture(Gdx.files.internal("Necromancer_creativekind-Sheet.png"));
		nightborneTexture = new Texture(Gdx.files.internal("nightborne/NightBorne.png"));
		obeliskTexture = new Texture(Gdx.files.internal("FlyingObelisk_no_lightnings_no_letter.png"));

		Pixmap pm0 = new Pixmap(Gdx.files.internal("background1.png"));
		Pixmap pm1 = new Pixmap(Gdx.files.internal("background2.png"));

		// Crea un Pixmap el doble de ancho y pega los dos
		Pixmap combined = new Pixmap(pm0.getWidth() + pm1.getWidth(), pm0.getHeight(), pm0.getFormat());
		combined.drawPixmap(pm0, 0, 0);
		combined.drawPixmap(pm1, pm0.getWidth(), 0);

		backgroundLoopTexture = new Texture(combined);
		bgRegion = new TextureRegion(backgroundLoopTexture);
		backgroundLoopTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
		backgroundEndTexture = new Texture(Gdx.files.internal("backgroundEnd.png"));
		backgroundMenuTexture = new Texture(Gdx.files.internal("backgroundMenu.png"));
		Pixmap blackPixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		blackPixel.setColor(0f, 0f, 0f, 1f);
		blackPixel.fill();
		blackPixelTexture = new Texture(blackPixel);
		blackPixel.dispose();
		pm0.dispose();
		pm1.dispose();
		combined.dispose();

		personaje = new Sprite(textura);
		personaje.setSize(ANCHO_PERSONA, ALTO_PERSONA);
		personaje.setRegion(0, 0, FRAME_W, FRAME_H);

		personaje.setPosition(10, 96);

		music = Gdx.audio.newMusic(Gdx.files.internal("sounds/song.mp3"));
		music.setLooping(true);
		music.play();

		// Joystick: esquina inferior izquierda
		joyBaseX  = width * 0.11f;
		joyBaseY  = height * 0.16f;
		joyThumbX = joyBaseX;
		joyThumbY = joyBaseY;

		// Botones: esquina inferior derecha
		btnJumpX = width * 0.88f; // Salto
		btnJumpY = height * 0.14f;
		btnDimX  = width * 0.70f; // Cambiar dimensión
		btnDimY  = height * 0.14f;
		btnAttackX = width * 0.79f; // Atacar
		btnAttackY = height * 0.30f;
		btnPauseX = width * 0.93f; // Pausa (esquina superior derecha)
		btnPauseY = height * 0.90f;

		initMenuButtons();

		// Input multi-touch
		Gdx.input.setInputProcessor(new InputAdapter() {

			@Override
			public boolean touchDown(int sx, int sy, int pointer, int button) {
				float wy = height - sy; // LibGDX: Y de pantalla está invertido

				if (isMenuState()) {
					authorEditing = authorRect != null && authorRect.contains(sx, wy);
				}

				if (gameState == GameState.MAIN_MENU) {
					if (btnPlayRect.contains(sx, wy)) gameState = GameState.DIFFICULTY_MENU;
					else if (btnShopRect.contains(sx, wy)) {
						shopBackToGame = false;
						gameState = GameState.SHOP_MENU;
					}
					return true;
				}

				if (gameState == GameState.SHOP_MENU) {
					if (btnShopHpRect.contains(sx, wy)) tryBuyUpgrade(1);
					else if (btnShopAtkRect.contains(sx, wy)) tryBuyUpgrade(2);
					else if (btnShopDefRect.contains(sx, wy)) tryBuyUpgrade(3);
					else if (btnShopSpdRect.contains(sx, wy)) tryBuyUpgrade(4);
					else if (btnShopJumpRect.contains(sx, wy)) tryBuyUpgrade(5);
					else if (btnShopBackRect.contains(sx, wy)) {
						if (shopBackToGame) {
							shopBackToGame = false;
							gameState = GameState.PLAYING;
						} else {
							gameState = GameState.MAIN_MENU;
						}
					}
					return true;
				}

				if (gameState == GameState.PAUSED) {
					if (btnPauseResumeRect.contains(sx, wy)) resumeGame();
					else if (btnPauseMenuRect.contains(sx, wy)) goToMainMenuFromPause();
					else if (btnPauseShopRect.contains(sx, wy)) goToShopFromPause();
					return true;
				}

				if (gameState == GameState.GAME_OVER) {
					if (btnGameOverMenuRect.contains(sx, wy)) gameState = GameState.MAIN_MENU;
					return true;
				}

				if (gameState == GameState.DIFFICULTY_MENU) {
					if (btnEasyRect.contains(sx, wy)) startGame(World.Difficulty.EASY);
					else if (btnNormalRect.contains(sx, wy)) startGame(World.Difficulty.NORMAL);
					else if (btnHardRect.contains(sx, wy)) startGame(World.Difficulty.HARD);
					else if (btnBackRect.contains(sx, wy)) gameState = GameState.MAIN_MENU;
					return true;
				}

				// Botón pausa (esquina superior derecha)
				if (dist(sx, wy, btnPauseX, btnPauseY) < PAUSE_BTN_R) {
					pauseGame();
					return true;
				}

				// Botón cambiar dimensión
				if (dist(sx, wy, btnDimX, btnDimY) < BTN_R) {
					trySwitchDimension();
					return true;
				}
				// Botón atacar
				if (dist(sx, wy, btnAttackX, btnAttackY) < BTN_R) {
					tryAttack();
					return true;
				}
				// Botón saltar
				if (dist(sx, wy, btnJumpX, btnJumpY) < BTN_R) {
					tryJump();
					return true;
				}
				// Joystick (mitad izquierda de la pantalla)
				if (joyPointer == -1 && sx < width * 0.45f) {
					joyPointer = pointer;
					updateJoystick(sx, wy);
				}
				return true;
			}

			@Override
			public boolean touchDragged(int sx, int sy, int pointer) {
				if (gameState != GameState.PLAYING) return true;
				if (pointer == joyPointer) updateJoystick(sx, height - sy);
				return true;
			}

			@Override
			public boolean touchUp(int sx, int sy, int pointer, int button) {
				if (gameState != GameState.PLAYING) return true;
				if (pointer == joyPointer) {
					joyPointer = -1;
					joyDirX   = 0f;
					joyThumbX = joyBaseX;
					joyThumbY = joyBaseY;
				}
				return true;
			}

			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.P) {
					if (gameState == GameState.PLAYING) {
						pauseGame();
						return true;
					}
					if (gameState == GameState.PAUSED) {
						resumeGame();
						return true;
					}
				}
				return false;
			}

			@Override
			public boolean keyTyped(char character) {
				if (!isMenuState() || !authorEditing) return false;

				if (character == '\b') {
					if (authorInfo.length() > 0) authorInfo = authorInfo.substring(0, authorInfo.length() - 1);
					return true;
				}
				if (character == '\r' || character == '\n') {
					authorEditing = false;
					return true;
				}
				if (character >= 32 && character <= 126 && authorInfo.length() < AUTHOR_MAX_CHARS) {
					authorInfo += character;
					return true;
				}
				return false;
			}
		});
	}

	private void initMenuButtons() {
		float bw = width * 0.46f;
		float bh = Math.max(64f, height * 0.085f);

		btnPlayRect = new Rectangle((width - bw) / 2f, height * 0.47f, bw, bh);
		btnShopRect = new Rectangle((width - bw) / 2f, height * 0.30f, bw, bh);

		float rightBw = Math.max(250f, width * 0.31f);
		float rightBh = Math.max(58f, height * 0.078f);
		float rightGap = Math.max(14f, rightBh * 0.30f);
		float rightMargin = Math.max(24f, width * 0.05f);
		float rightTotalH = rightBh * 4f + rightGap * 3f;
		float rightY0 = (height - rightTotalH) * 0.45f;
		float rightX = width - rightMargin - rightBw;

		btnBackRect   = new Rectangle(rightX, rightY0, rightBw, rightBh);
		btnHardRect   = new Rectangle(rightX, rightY0 + (rightBh + rightGap), rightBw, rightBh);
		btnNormalRect = new Rectangle(rightX, rightY0 + 2f * (rightBh + rightGap), rightBw, rightBh);
		btnEasyRect   = new Rectangle(rightX, rightY0 + 3f * (rightBh + rightGap), rightBw, rightBh);

		float shopW = Math.max(300f, width * 0.42f);
		float shopH = Math.max(56f, height * 0.074f);
		float shopGap = Math.max(12f, shopH * 0.24f);
		float shopX = (width - shopW) * 0.5f;
		float shopTop = height * 0.70f;
		btnShopHpRect = new Rectangle(shopX, shopTop, shopW, shopH);
		btnShopAtkRect = new Rectangle(shopX, shopTop - (shopH + shopGap), shopW, shopH);
		btnShopDefRect = new Rectangle(shopX, shopTop - 2f * (shopH + shopGap), shopW, shopH);
		btnShopSpdRect = new Rectangle(shopX, shopTop - 3f * (shopH + shopGap), shopW, shopH);
		btnShopJumpRect = new Rectangle(shopX, shopTop - 4f * (shopH + shopGap), shopW, shopH);
		btnShopBackRect = new Rectangle(shopX, shopTop - 5f * (shopH + shopGap), shopW, shopH);

		float pauseW = Math.max(280f, width * 0.38f);
		float pauseH = Math.max(56f, height * 0.074f);
		float pauseGap = Math.max(12f, pauseH * 0.24f);
		float pauseX = (width - pauseW) * 0.5f;
		float pauseTop = height * 0.62f;
		btnPauseResumeRect = new Rectangle(pauseX, pauseTop, pauseW, pauseH);
		btnPauseMenuRect = new Rectangle(pauseX, pauseTop - (pauseH + pauseGap), pauseW, pauseH);
		btnPauseShopRect = new Rectangle(pauseX, pauseTop - 2f * (pauseH + pauseGap), pauseW, pauseH);

		btnGameOverMenuRect = new Rectangle((width - pauseW) * 0.5f, height * 0.42f, pauseW, pauseH);


		float fieldW = Math.max(220f, width * 0.34f);
		float fieldH = Math.max(42f, height * 0.062f);
		float margin = Math.max(12f, width * 0.02f);
		authorRect = new Rectangle(margin, margin, fieldW, fieldH);
	}

	private boolean isMenuState() {
		return gameState == GameState.MAIN_MENU || gameState == GameState.DIFFICULTY_MENU || gameState == GameState.SHOP_MENU;
	}

	private void pauseGame() {
		if (gameState != GameState.PLAYING) return;
		gameState = GameState.PAUSED;
		joyPointer = -1;
		joyDirX = 0f;
		joyThumbX = joyBaseX;
		joyThumbY = joyBaseY;
	}

	private void resumeGame() {
		if (gameState == GameState.PAUSED) gameState = GameState.PLAYING;
	}

	private void goToMainMenuFromPause() {
		gameState = GameState.MAIN_MENU;
	}

	private void goToShopFromPause() {
		shopBackToGame = true;
		gameState = GameState.SHOP_MENU;
	}

	private void startGame(World.Difficulty difficulty) {
		selectedBaseDifficulty = difficulty;
		playerLives = 3;
		shopBackToGame = false;
		startFloor(0);
		gameState = GameState.PLAYING;
	}

	private void startFloor(int newFloorIndex) {
		if (world != null) world.dispose();
		floorIndex = newFloorIndex;
		advanceRight = (floorIndex % 2 == 0);
		World.Difficulty floorDifficulty = getDifficultyForFloor(floorIndex);

		float tile = 32f;
		float floorTiles = FLOOR_BASE_TILES + floorIndex * FLOOR_GROW_TILES + floorIndex * floorIndex * 8f;
		float floorDistance = floorTiles * tile;
		if (advanceRight) {
			floorStartX = Math.max(10f, (World.SUELO_INICIAL - 10) * tile);
			floorGoalX  = floorStartX + floorDistance;
		} else {
			floorGoalX  = 10f;
			floorStartX = floorGoalX + floorDistance;
		}

		int spawnTile = (int)(floorStartX / tile);
		world = new World(width, height, 32, floorDifficulty, floorIndex, spawnTile);
		world.generarNivel();

		velocidadX = 0f;
		velocidadY = 0f;
		saltando = false;
		enSuelo = false;
		joyDirX = 0f;
		joyPointer = -1;
		joyThumbX = joyBaseX;
		joyThumbY = joyBaseY;
		player.resetForFloor();
		float spawnGroundY = findSpawnGroundY(floorStartX, HITBOX_W);
		setBodyPosition(floorStartX, spawnGroundY);
		aterrizar();
		floorProgress = 0f;
		floorMessage = "Planta " + (floorIndex + 1) + " - " + (advanceRight ? "Avanza hacia el este" : "Avanza hacia el oeste");
		floorMessageTimer = FLOOR_TEXT_DURATION;

		float ensureX = Math.max(floorStartX, floorGoalX) + width;
		world.asegurarGenerado(ensureX);
		damageTexts.clear();
		spawnEnemiesForFloor();
	}

	private float findSpawnGroundY(float bodyX, float bodyW) {
		float searchTop = height + world.getTileSize() * 8f;
		float searchBottom = -world.getTileSize() * 4f;
		float groundY = world.checkColisionSuelo(bodyX, searchTop, searchBottom, bodyW, -1f);
		if (groundY == Float.MIN_VALUE) {
			groundY = world.findFullSolidGroundY(bodyX, searchTop, searchBottom, bodyW);
		}
		if (groundY == Float.MIN_VALUE) {
			groundY = world.getTileSize() * 3f;
		}
		return groundY;
	}

	private World.Difficulty getDifficultyForFloor(int index) {
		int level = selectedBaseDifficulty.ordinal() + index;
		if (level < 0) level = 0;
		if (level > World.Difficulty.HARD.ordinal()) level = World.Difficulty.HARD.ordinal();
		return World.Difficulty.values()[level];
	}

	private float clamp01(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	private float smoothStep(float edge0, float edge1, float x) {
		float t = clamp01((x - edge0) / (edge1 - edge0));
		return t * t * (3f - 2f * t);
	}

	private boolean updateFloorProgressAndTryAdvance(float bodyX) {
		float total = Math.abs(floorGoalX - floorStartX);
		if (total <= 0.001f) {
			floorProgress = 1f;
		} else if (advanceRight) {
			floorProgress = clamp01((bodyX - floorStartX) / total);
		} else {
			floorProgress = clamp01((floorStartX - bodyX) / total);
		}

		if (floorProgress >= 0.999f) {
			int reward = FLOOR_REWARD_BASE + floorIndex * FLOOR_REWARD_SCALE;
			player.addCoins(reward);
			floorMessage = "Planta superada +" + reward + " oro";
			floorMessageTimer = FLOOR_TEXT_DURATION;
			startFloor(floorIndex + 1);
			return true;
		}
		return false;
	}

	private void tryBuyUpgrade(int id) {
		switch (id) {
			case 1:
				if (player.trySpendCoins(SHOP_COST_HP)) player.upgradeHealth();
				break;
			case 2:
				if (player.trySpendCoins(SHOP_COST_ATK)) player.upgradeAttack();
				break;
			case 3:
				if (player.trySpendCoins(SHOP_COST_DEF)) player.upgradeDefense();
				break;
			case 4:
				if (player.trySpendCoins(SHOP_COST_SPD)) player.upgradeSpeed();
				break;
			case 5:
				if (player.trySpendCoins(SHOP_COST_JUMP)) player.upgradeJump();
				break;
			default:
				break;
		}
	}

	// HELPERS DE INPUT
	private void updateJoystick(float tx, float ty) {
		float dx  = tx - joyBaseX;
		float dy  = ty - joyBaseY;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len > JOY_OUTER_R) { dx = dx / len * JOY_OUTER_R; dy = dy / len * JOY_OUTER_R; }
		joyThumbX = joyBaseX + dx;
		joyThumbY = joyBaseY + dy;
		joyDirX   = dx / JOY_OUTER_R;
	}

	private float dist(float x1, float y1, float x2, float y2) {
		float dx = x1 - x2, dy = y1 - y2;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	private void tryJump() {
		if (enSuelo) doJump();
	}

	private void trySwitchDimension() {
		if (world == null)
			return;
		world.switchDimension();
		dimFlashTimer = DIM_FLASH_DURATION;
	}

	private void tryAttack() {
		if (gameState != GameState.PLAYING) return;
		if (player.tryStartAttack() && enSuelo) setAnimState(AnimState.ATTACK);
	}

	private float readKeyboardAxisX() {
		float dir = 0f;
		if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
		if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;
		return dir;
	}

	private float getMoveAxisX() {
		float keyboardX = readKeyboardAxisX();
		if (Math.abs(keyboardX) > 0.01f) {
			if (joyPointer == -1) {
				joyThumbX = joyBaseX + keyboardX * JOY_OUTER_R;
				joyThumbY = joyBaseY;
			}
			return keyboardX;
		}

		if (joyPointer == -1) {
			joyThumbX = joyBaseX;
			joyThumbY = joyBaseY;
		}
		return joyDirX;
	}

	private void handleKeyboardActions() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
				|| Gdx.input.isKeyJustPressed(Input.Keys.W)
				|| Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
			tryJump();
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.Q)
				|| Gdx.input.isKeyJustPressed(Input.Keys.E)) {
			trySwitchDimension();
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.F)
				|| Gdx.input.isKeyJustPressed(Input.Keys.J)
				|| Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_RIGHT)) {
			tryAttack();
		}
	}

	private float getBodyX() { return personaje.getX() + HITBOX_OFF_X; }
	private float getBodyY() { return personaje.getY() + HITBOX_OFF_Y; }

	private void setBodyPosition(float bodyX, float bodyY) {
		personaje.setPosition(bodyX - HITBOX_OFF_X, bodyY - HITBOX_OFF_Y);
	}

	// ACCIONES DEL PERSONAJE
	private void doJump() {
		saltando   = true;
		enSuelo    = false;
		velocidadY = player.getJumpPower();
		setAnimState(AnimState.JUMP);
	}

	private void aterrizar() {
		saltando  = false;
		enSuelo   = true;
		if (player.isAttacking())
			setAnimState(AnimState.ATTACK);
		else
			setAnimState(Math.abs(velocidadX) > 0.2f ? AnimState.RUN : AnimState.IDLE);
	}

	private void respawn() {
		velocidadX = 0f; velocidadY = 0f; joyDirX = 0f;
		saltando = false;
		enSuelo = false;
		player.resetForFloor();
		player.getStats().health = player.getStats().maxHealth;
		setAnimState(AnimState.IDLE);
		setBodyPosition(floorStartX, findSpawnGroundY(floorStartX, HITBOX_W));
	}

	private void applyFrame(int frameIndex) {
		personaje.setRegion(frameIndex * FRAME_W, 0, FRAME_W, FRAME_H);
		personaje.setFlip(!mirandoDerecha, false);
	}

	private void setAnimState(AnimState next) {
		if (animState == next) return;
		animState = next;
		paso = 0;
		cambiopaso = 0;
		switch (animState) {
			case JUMP:
				personaje.setTexture(texturaJump);
				break;
			case IDLE:
				personaje.setTexture(texturaIdle);
				break;
			case ATTACK:
				personaje.setTexture(texturaAttack);
				break;
			case RUN:
			default:
				personaje.setTexture(textura);
				break;
		}
		applyFrame(0);
	}

	// ANIMACIÓN
	private void updateAnimation() {
		if (player.isAttacking() && enSuelo) {
			setAnimState(AnimState.ATTACK);
			float progress = player.getAttackProgress();
			int frame = Math.min(NUM_FRAMES_ATTACK - 1, Math.max(0, (int) (progress * NUM_FRAMES_ATTACK)));
			applyFrame(frame);
		} else if (saltando || !enSuelo) {
			setAnimState(AnimState.JUMP);
			// Animación de salto
			cambiopaso = (cambiopaso + 1) % NUM_FRAMES_JUMP;
			if (cambiopaso == 0 && paso < NUM_FRAMES_JUMP - 1) {
				paso++;
				applyFrame(paso);
			}
		} else if (Math.abs(velocidadX) > 0.2f) {
			setAnimState(AnimState.RUN);
			// Animación de correr
			cambiopaso = (cambiopaso + 1) % NUM_FRAMES_RUN;
			if (cambiopaso == 0) {
				paso = (paso + 1) % NUM_FRAMES_RUN;
				applyFrame(paso);
			}
		} else { // Quieto
			setAnimState(AnimState.IDLE);
			cambiopaso = (cambiopaso + 1) % 8;
			if (cambiopaso == 0) {
				paso = (paso + 1) % NUM_FRAMES_IDLE;
				applyFrame(paso);
			}
		}
	}

	// RENDER PRINCIPAL
	@Override
	public void render() {
		if (gameState != GameState.PLAYING) {
			renderMenus();
			return;
		}

		float delta = Gdx.graphics.getDeltaTime();
		dimFlashTimer = Math.max(0f, dimFlashTimer - delta);
		floorMessageTimer = Math.max(0f, floorMessageTimer - delta);
		player.update(delta);
		if (!player.isAttacking() && enSuelo && animState == AnimState.ATTACK) {
			setAnimState(Math.abs(velocidadX) > 0.2f ? AnimState.RUN : AnimState.IDLE);
		}
		handleKeyboardActions();

		// Movimiento horizontal por joystick/teclado
		velocidadX = getMoveAxisX() * player.getMoveSpeed();
		if (velocidadX >  0.1f)
			mirandoDerecha = true;
		else if (velocidadX < -0.1f)
			mirandoDerecha = false;

		// Gravedad
		float bodyX = getBodyX();
		float bodyY = getBodyY();
		float yAnterior = bodyY;
		velocidadY += GRAVEDAD;
		if (velocidadY < -15f) velocidadY = -15f;
		float nuevaY = bodyY + velocidadY;

		// Colisión suelo / plataformas
		float suelo = world.checkColisionSuelo(
				bodyX, yAnterior, nuevaY, HITBOX_W, velocidadY);
		if (suelo != Float.MIN_VALUE) {
			nuevaY    = suelo;
			velocidadY = 0f;
			if (saltando || !enSuelo) aterrizar();
		} else {
			enSuelo = false;
		}

		// Colisión techo
		/*float techo = world.checkColisionTecho(
				bodyX, yAnterior, nuevaY, HITBOX_W, HITBOX_H, velocidadY);
		if (techo != Float.MAX_VALUE) {
			nuevaY    = techo;
			velocidadY = -2.5f;
			enSuelo = false;
		}*/

		// Caída fuera del mundo -> respawn
		if (nuevaY < -height) {
			respawn();
			nuevaY = 3 * world.getTileSize();
			bodyX = getBodyX();
		}

		setBodyPosition(bodyX, nuevaY);

		// Animación
		updateAnimation();

		// Movimiento horizontal + colisión paredes
		float nuevaX = bodyX + velocidadX;
		if (nuevaX < 0)
			nuevaX = 0;

		if (velocidadX > 0)
			nuevaX = world.resolveColisionDerecha(nuevaX, nuevaY, HITBOX_W, HITBOX_H);
		else if (velocidadX < 0)
			nuevaX = world.resolveColisionIzquierda(nuevaX, nuevaY, HITBOX_W, HITBOX_H);

		setBodyPosition(nuevaX, nuevaY);
		updateEnemiesAndCombat(delta);
		if (handlePlayerLifeByHealth()) {
			return;
		}
		updateDamageTexts(delta);
		if (updateFloorProgressAndTryAdvance(nuevaX)) return;

		// Generación dinámica del mundo
		float generationAnchor = advanceRight ? Math.max(nuevaX, floorGoalX) : floorStartX;
		world.asegurarGenerado(generationAnchor + width);

		// Cámara
		cam.position.x = Math.max(width / 2f, personaje.getX() + ANCHO_PERSONA / 2f);
		cam.update();

		// DIBUJO

		// Fondo según dimensión activa
		float[] bg = BG[world.getCurrentDimension()];
		Gdx.gl.glClearColor(bg[0], bg[1], bg[2], 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// Mundo y personaje
		batch.setProjectionMatrix(cam.combined);
		batch.begin();
		float viewLeft = cam.position.x - width * 0.5f;
		float parallax = 0.2f;
		float uvWidth  = (float) width / backgroundLoopTexture.getWidth(); // cuánta textura cabe en pantalla
		float u1       = (cam.position.x * parallax) / backgroundLoopTexture.getWidth();
		float u2       = u1 + uvWidth;
		bgRegion.setRegion(u1, 0f, u2, 1f);
		batch.draw(bgRegion, viewLeft, 0f, width, height);

		float darkBlend = smoothStep(0.74f, 0.92f, floorProgress);
		if (darkBlend > 0f) {
			batch.setColor(1f, 1f, 1f, Math.min(0.42f, 0.42f * darkBlend));
			batch.draw(blackPixelTexture, viewLeft, 0f, width, height);
			batch.setColor(1f, 1f, 1f, 1f);
		}

		float stripBlend = smoothStep(0.84f, 0.96f, floorProgress);
		float endBlend = smoothStep(0.90f, 1f, floorProgress);
		float endWidth = width * endBlend;
		float stripW = width * 0.08f * stripBlend;

		if (stripW > 0.5f) {
			batch.setColor(1f, 1f, 1f, Math.min(0.9f, 0.3f + 0.6f * stripBlend));
			if (advanceRight) {
				float stripX = Math.max(viewLeft, viewLeft + width - endWidth - stripW);
				batch.draw(blackPixelTexture, stripX, 0f, stripW, height);
			} else {
				float stripX = Math.min(viewLeft + width - stripW, viewLeft + endWidth);
				batch.draw(blackPixelTexture, stripX, 0f, stripW, height);
			}
			batch.setColor(1f, 1f, 1f, 1f);
		}

		if (endWidth > 0.5f) {
			batch.setColor(1f, 1f, 1f, Math.max(0.25f, endBlend));
			if (advanceRight) {
				batch.draw(backgroundEndTexture, viewLeft + width - endWidth, 0f, endWidth, height);
			} else {
				batch.draw(backgroundEndTexture, viewLeft, 0f, endWidth, height);
			}
			batch.setColor(1f, 1f, 1f, 1f);
		}
		world.render(batch, cam.position.x, width);
		drawEnemies();
		drawDamageTextsInWorld();
		personaje.draw(batch);
		batch.end();

		drawUI();
		drawDimensionFlash();
	}

	private void renderMenus() {
		Gdx.gl.glClearColor(0.04f, 0.05f, 0.08f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapes.setProjectionMatrix(uiCam.combined);
		batch.setProjectionMatrix(uiCam.combined);

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		if (gameState == GameState.MAIN_MENU) {
			batch.begin();
			batch.setColor(1f, 1f, 1f, 1f);
			batch.draw(backgroundMenuTexture, 0f, 0f, width, height);
			batch.setColor(0.02f, 0.03f, 0.05f, 0.38f);
			batch.draw(blackPixelTexture, 0f, 0f, width, height);
			batch.setColor(1f, 1f, 1f, 1f);
			batch.end();

			shapes.begin(ShapeRenderer.ShapeType.Filled);
			shapes.setColor(0.18f, 0.20f, 0.32f, 1f);
			shapes.rect(btnPlayRect.x, btnPlayRect.y, btnPlayRect.width, btnPlayRect.height);
			shapes.setColor(0.22f, 0.30f, 0.20f, 1f);
			shapes.rect(btnShopRect.x, btnShopRect.y, btnShopRect.width, btnShopRect.height);
			shapes.end();

			batch.begin();
			drawCentered("Split the Reality", height * 0.72f);
			drawCentered("Jugar", btnPlayRect.y + btnPlayRect.height * 0.62f);
			drawCentered("Tienda", btnShopRect.y + btnShopRect.height * 0.62f);
			font.draw(batch, "Oro: " + player.getCoins(), width * 0.06f, height * 0.92f);
			batch.end();
		} else if (gameState == GameState.SHOP_MENU) {
			batch.begin();
			batch.setColor(1f, 1f, 1f, 1f);
			batch.draw(backgroundMenuTexture, 0f, 0f, width, height);
			batch.setColor(0.02f, 0.03f, 0.05f, 0.52f);
			batch.draw(blackPixelTexture, 0f, 0f, width, height);
			batch.setColor(1f, 1f, 1f, 1f);
			batch.end();

			shapes.begin(ShapeRenderer.ShapeType.Filled);
			drawMenuButton(btnShopHpRect, 0.45f, 0.85f, 0.46f);
			drawMenuButton(btnShopAtkRect, 0.95f, 0.42f, 0.34f);
			drawMenuButton(btnShopDefRect, 0.44f, 0.66f, 0.96f);
			drawMenuButton(btnShopSpdRect, 0.96f, 0.78f, 0.30f);
			drawMenuButton(btnShopJumpRect, 0.82f, 0.52f, 0.98f);
			drawMenuButton(btnShopBackRect, 0.78f, 0.78f, 0.80f);
			shapes.end();

			batch.begin();
			drawCentered("Tienda", height * 0.90f);
			String goldText = "Oro disponible: " + player.getCoins();
			glyph.setText(font, goldText);
			font.draw(batch, goldText, width - glyph.width - 18f, height - 18f);
			drawCenteredInRect("Vida +20   (" + SHOP_COST_HP + ")", btnShopHpRect);
			drawCenteredInRect("Ataque +3   (" + SHOP_COST_ATK + ")", btnShopAtkRect);
			drawCenteredInRect("Defensa +2   (" + SHOP_COST_DEF + ")", btnShopDefRect);
			drawCenteredInRect("Velocidad +0.35   (" + SHOP_COST_SPD + ")", btnShopSpdRect);
			drawCenteredInRect("Salto +0.8   (" + SHOP_COST_JUMP + ")", btnShopJumpRect);
			drawCenteredInRect("Volver", btnShopBackRect);
			batch.end();
		} else if (gameState == GameState.PAUSED) {
			batch.begin();
			batch.setColor(0.02f, 0.03f, 0.05f, 0.78f);
			batch.draw(blackPixelTexture, 0f, 0f, width, height);
			batch.setColor(1f, 1f, 1f, 1f);
			batch.end();

			shapes.begin(ShapeRenderer.ShapeType.Filled);
			drawMenuButton(btnPauseResumeRect, 0.34f, 0.82f, 0.92f);
			drawMenuButton(btnPauseMenuRect, 0.95f, 0.44f, 0.38f);
			drawMenuButton(btnPauseShopRect, 0.42f, 0.78f, 0.44f);
			shapes.end();

			batch.begin();
			drawCentered("Pausa", height * 0.84f);
			drawCenteredInRect("Reanudar", btnPauseResumeRect);
			drawCenteredInRect("Menu principal", btnPauseMenuRect);
			drawCenteredInRect("Ir a tienda", btnPauseShopRect);
			batch.end();
		} else if (gameState == GameState.GAME_OVER) {
			batch.begin();
			batch.setColor(0.01f, 0.01f, 0.02f, 0.86f);
			batch.draw(blackPixelTexture, 0f, 0f, width, height);
			batch.setColor(1f, 1f, 1f, 1f);
			batch.end();

			shapes.begin(ShapeRenderer.ShapeType.Filled);
			drawMenuButton(btnGameOverMenuRect, 0.90f, 0.34f, 0.34f);
			shapes.end();

			batch.begin();
			drawCentered("Game Over - Planta " + (floorIndex + 1), height * 0.72f);
			drawCenteredInRect("Volver al menu", btnGameOverMenuRect);
			batch.end();
		} else if (gameState == GameState.DIFFICULTY_MENU) {
			menuIdleTimer += Gdx.graphics.getDeltaTime();
			while (menuIdleTimer >= 0.11f) {
				menuIdleTimer -= 0.11f;
				menuIdleFrame = (menuIdleFrame + 1) % NUM_FRAMES_IDLE;
			}

			batch.begin();
			batch.setColor(1f, 1f, 1f, 1f);
			batch.draw(backgroundEndTexture, 0f, 0f, width, height);
			batch.setColor(0.05f, 0.07f, 0.11f, 0.62f);
			batch.draw(blackPixelTexture, 0f, 0f, width, height);
			batch.setColor(1f, 1f, 1f, 1f);
			batch.end();

			shapes.begin(ShapeRenderer.ShapeType.Filled);
			float panelX = width * 0.07f;
			float panelY = height * 0.16f;
			float panelW = Math.min(width * 0.37f, btnEasyRect.x - panelX - width * 0.045f);
			float panelH = height * 0.60f;
			shapes.setColor(0.03f, 0.05f, 0.08f, 0.44f);
			shapes.rect(panelX, panelY, panelW, panelH);

			drawMenuButton(btnEasyRect, 0.18f, 0.82f, 0.42f);
			drawMenuButton(btnNormalRect, 0.35f, 0.56f, 0.98f);
			drawMenuButton(btnHardRect, 0.95f, 0.36f, 0.30f);
			drawMenuButton(btnBackRect, 0.78f, 0.78f, 0.80f);
			shapes.end();

			shapes.begin(ShapeRenderer.ShapeType.Line);
			shapes.setColor(0.72f, 0.86f, 1f, 0.28f);
			shapes.rect(panelX, panelY, panelW, panelH);
			shapes.end();

			batch.begin();
			float idleMaxW = panelW;
			float idleMaxH = panelH;
			float idleW = Math.min(idleMaxW, idleMaxH * (FRAME_W / (float) FRAME_H));
			float idleH = idleW * (FRAME_H / (float) FRAME_W);
			float idleX = panelX + (panelW - idleW) * 0.5f;
			float idleY = panelY + (panelH - idleH) * 0.36f;
			int srcX = menuIdleFrame * FRAME_W;
			batch.draw(texturaIdle, idleX, idleY, idleW, idleH, srcX, 0, FRAME_W, FRAME_H, false, false);

			drawCenteredInXRange("Elegir dificultad", btnEasyRect.x, btnEasyRect.x + btnEasyRect.width, height * 0.92f);
			drawCenteredInRect("Facil", btnEasyRect);
			drawCenteredInRect("Normal", btnNormalRect);
			drawCenteredInRect("Dificil", btnHardRect);
			drawCenteredInRect("Volver", btnBackRect);
			batch.end();
		}

		if (isMenuState()) drawAuthorField();
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	private void drawAuthorField() {
		if (authorRect == null) return;

		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(0f, 0f, 0f, 0.48f);
		shapes.rect(authorRect.x, authorRect.y, authorRect.width, authorRect.height);
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		if (authorEditing) shapes.setColor(0.52f, 0.86f, 1f, 1f);
		else shapes.setColor(1f, 1f, 1f, 0.72f);
		shapes.rect(authorRect.x, authorRect.y, authorRect.width, authorRect.height);
		shapes.end();

		batch.begin();
		String cursor = (authorEditing && ((System.currentTimeMillis() / 450L) % 2L == 0L)) ? "_" : "";
		String text = authorInfo + cursor;
		glyph.setText(font, text);
		float maxWidth = authorRect.width - 16f;
		if (glyph.width > maxWidth && text.length() > 3) {
			while (glyph.width > maxWidth && text.length() > 3) {
				text = text.substring(1);
				glyph.setText(font, text);
			}
		}
		font.draw(batch, text, authorRect.x + 8f, authorRect.y + authorRect.height * 0.68f);
		batch.end();
	}

	private void drawMenuButton(Rectangle rect, float r, float g, float b) {
		shapes.setColor(0.04f, 0.05f, 0.08f, 0.84f);
		shapes.rect(rect.x, rect.y, rect.width, rect.height);

		shapes.setColor(r * 0.32f, g * 0.32f, b * 0.32f, 0.92f);
		shapes.rect(rect.x + 3f, rect.y + 3f, rect.width - 6f, rect.height - 6f);

		float accentW = Math.max(6f, rect.width * 0.032f);
		shapes.setColor(r, g, b, 0.95f);
		shapes.rect(rect.x + 3f, rect.y + 3f, accentW, rect.height - 6f);

		shapes.setColor(r * 0.55f, g * 0.55f, b * 0.55f, 0.38f);
		shapes.rect(rect.x, rect.y, rect.width, 1.5f);
		shapes.rect(rect.x, rect.y + rect.height - 1.5f, rect.width, 1.5f);
		shapes.rect(rect.x, rect.y, 1.5f, rect.height);
		shapes.rect(rect.x + rect.width - 1.5f, rect.y, 1.5f, rect.height);
	}

	private void drawCentered(String text, float y) {
		glyph.setText(font, text);
		float x = (width - glyph.width) / 2f;
		font.draw(batch, glyph, x, y);
	}

	private void drawCenteredInXRange(String text, float xMin, float xMax, float y) {
		glyph.setText(font, text);
		float span = Math.max(0f, xMax - xMin);
		float x = xMin + (span - glyph.width) * 0.5f;
		font.draw(batch, glyph, x, y);
	}

	private void drawCenteredInRect(String text, Rectangle rect) {
		drawCenteredInXRange(text, rect.x, rect.x + rect.width, rect.y + rect.height * 0.62f);
	}

	private Rectangle getAttackRect() {
		float bodyX = getBodyX();
		float bodyY = getBodyY();
		float attackW = ATTACK_REACH;
		float x = mirandoDerecha
				? bodyX + HITBOX_W * 0.55f
				: bodyX - attackW - HITBOX_W * 0.10f;
		float y = bodyY + (HITBOX_H - ATTACK_BOX_H) * 0.5f;
		attackRectTmp.set(x, y, attackW, ATTACK_BOX_H);
		return attackRectTmp;
	}

	private Enemy createEnemy(Enemy.Type type, float x, float y) {
		float tile = world != null ? world.getTileSize() : 32f;
		Enemy enemy = Enemy.create(type, x, y, tile, floorIndex);
		enemy.setFacingRight(advanceRight);
		float enemyGroundY = findSpawnGroundY(enemy.x, enemy.w);
		enemy.setFootY(enemyGroundY + getEnemyFootOffsetY(type, tile));
		return enemy;
	}

	private float getEnemyFootOffsetY(Enemy.Type type, float tile) {
		switch (type) {
			case NECROMANCER:
				return -tile * 0.12f;
			case NIGHTBORNE:
				return -tile * 0.18f;
			case MAGE_BLUE:
			case MAGE_MAGENTA:
			default:
				return 0f;
		}
	}

	private void spawnEnemiesForFloor() {
		enemies.clear();
		if (world == null)
			return;

		Enemy.Type[] cycle = {
				Enemy.Type.MAGE_BLUE,
				Enemy.Type.MAGE_MAGENTA,
				Enemy.Type.NECROMANCER,
				Enemy.Type.NIGHTBORNE
		};

		float tile = world.getTileSize();
		float minX = Math.min(floorStartX, floorGoalX) + tile * 20f;
		float maxX = Math.max(floorStartX, floorGoalX) - tile * 16f;
		if (maxX <= minX) return;

		int enemyCount = 4 + Math.min(6, floorIndex);
		float stride = (maxX - minX) / Math.max(1, enemyCount - 1);
		float spawnY = tile * 3f;

		for (int i = 0; i < enemyCount; i++) {
			Enemy.Type type = cycle[(i + floorIndex) % cycle.length];
			float x = minX + stride * i;
			enemies.add(createEnemy(type, x, spawnY));
		}
	}

	private Texture getEnemyTexture(Enemy.Type type) {
		switch (type) {
			case MAGE_BLUE:
				return mageBlueTexture;
			case MAGE_MAGENTA:
				return mageMagentaTexture;
			case NECROMANCER:
				return necromancerTexture;
			case NIGHTBORNE:
			default:
				return nightborneTexture;
		}
	}

	private int getSrcYFromTopRow(Texture texture, int frameH, int rowFromTop) {
		int safeFrameH = Math.max(1, frameH);
		int maxRows = Math.max(1, texture.getHeight() / safeFrameH);
		int safeRow = Math.max(0, Math.min(rowFromTop, maxRows - 1));
		return texture.getHeight() - (safeRow + 1) * safeFrameH;
	}
	private void updateEnemiesAndCombat(float delta) {
		playerRectTmp.set(getBodyX(), getBodyY(), HITBOX_W, HITBOX_H);
		for (Enemy enemy : enemies) {
			if (!enemy.alive) continue;
			enemy.update(delta, getBodyX() + HITBOX_W * 0.5f, getBodyY() + HITBOX_H * 0.5f);

			if (enemy.canMeleeHitPlayer(playerRectTmp, enemyRectTmp)) {
				int taken = player.receiveDamage(enemy.stats.attack);
				if (taken > 0) addPlayerDamageText(taken);
			}

			for (Enemy.Hazard hazard : enemy.getHazards()) {
				if (hazard.isTelegraphing()) continue;
				hazard.toRect(hazardRectTmp);
				if (hazardRectTmp.overlaps(playerRectTmp)) {
					int taken = player.receiveDamage(hazard.damage);
					if (taken > 0) addPlayerDamageText(taken);
				}
			}
		}

		if (!player.isAttacking()) return;
		Rectangle attackRect = getAttackRect();
		int attackSerial = player.getAttackSerial();
		for (Enemy enemy : enemies) {
			if (!enemy.alive) continue;
			enemy.getHurtBounds(enemyRectTmp);
			if (attackRect.overlaps(enemyRectTmp)) {
				int dealt = enemy.receiveHit(player.getStats().attack, attackSerial);
				if (dealt <= 0) continue;
				addEnemyDamageText(enemy.x + enemy.w * 0.5f, enemy.y + enemy.h * 0.85f, dealt);
				if (!enemy.alive) {
					int reward = enemy.getKillRewardCoins();
					player.addCoins(reward);
				}
			}
		}
	}

	private void drawEnemies() {
		for (Enemy enemy : enemies) {
			if (!enemy.alive)
				continue;
			Texture texture = getEnemyTexture(enemy.type);
			int frameW = enemy.getFrameWidth();
			int frameH = enemy.getFrameHeight();
			int totalFrames = enemy.getFrameCount();
			int srcX = (enemy.getCurrentFrame() % Math.max(1, totalFrames)) * frameW;
			int srcY = getSrcYFromTopRow(texture, frameH, enemy.getRowFromTop());
			if (srcX + frameW > texture.getWidth()) srcX = Math.max(0, texture.getWidth() - frameW);
			if (srcY + frameH > texture.getHeight()) srcY = Math.max(0, texture.getHeight() - frameH);
			boolean directional = enemy.type == Enemy.Type.NECROMANCER || enemy.type == Enemy.Type.NIGHTBORNE;
			boolean flipX = directional && !enemy.isFacingRight();
			batch.draw(texture, enemy.x, enemy.y, enemy.w, enemy.h, srcX, srcY, frameW, frameH, flipX, false);

			for (Enemy.Hazard hazard : enemy.getHazards()) {
				if (hazard.type == Enemy.HazardType.OBELISK) {
					int obeliskFrames = 13;
					int fw = Math.max(1, obeliskTexture.getWidth() / obeliskFrames);
					int f = (int) ((System.currentTimeMillis() / 90L) % obeliskFrames);
					float alpha = hazard.isTelegraphing() ? 0.45f : 1f;
					batch.setColor(1f, 1f, 1f, alpha);
					batch.draw(obeliskTexture, hazard.x, hazard.y, hazard.w, hazard.h, f * fw, 0, fw, obeliskTexture.getHeight(), false, false);
					batch.setColor(1f, 1f, 1f, 1f);
				} else {
					batch.setColor(0.55f, 0.10f, 0.70f, 0.95f);
					batch.draw(blackPixelTexture, hazard.x, hazard.y, hazard.w, hazard.h);
					batch.setColor(1f, 1f, 1f, 1f);
				}
			}
		}
	}

	private boolean handlePlayerLifeByHealth() {
		if (player.getStats().health > 0) return false;
		playerLives--;
		if (playerLives <= 0) {
			playerLives = 0;
			gameState = GameState.GAME_OVER;
			return true;
		}
		respawn();
		return true;
	}

	private void addEnemyDamageText(float x, float y, int value) {
		damageTexts.add(new DamageText(x, y, "-" + value, 1f, 0.42f, 0.35f));
	}

	private void addPlayerDamageText(int value) {
		float px = getBodyX() + HITBOX_W * 0.5f;
		float py = getBodyY() + HITBOX_H + 10f;
		damageTexts.add(new DamageText(px, py, "-" + value, 1f, 0.86f, 0.25f));
	}

	private void updateDamageTexts(float delta) {
		for (int i = damageTexts.size() - 1; i >= 0; i--) {
			DamageText dt = damageTexts.get(i);
			dt.timer -= delta;
			dt.y += 38f * delta;
			if (dt.timer <= 0f) damageTexts.remove(i);
		}
	}

	private void drawDamageTextsInWorld() {
		for (DamageText dt : damageTexts) {
			float alpha = clamp01(dt.timer / 0.6f);
			font.setColor(dt.r, dt.g, dt.b, alpha);
			font.draw(batch, dt.text, dt.x, dt.y);
		}
		font.setColor(1f, 1f, 1f, 1f);
	}

	private void drawUI() {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shapes.setProjectionMatrix(uiCam.combined);

		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(0.4f, 0.4f, 0.4f, 0.30f);
		shapes.circle(joyBaseX, joyBaseY, JOY_OUTER_R);
		shapes.setColor(0.95f, 0.95f, 0.95f, 0.65f);
		shapes.circle(joyThumbX, joyThumbY, JOY_INNER_R);

		shapes.setColor(0.15f, 0.75f, 0.20f, 0.50f);
		shapes.circle(btnJumpX, btnJumpY, BTN_R);

		if (world.getCurrentDimension() == 0) {
			shapes.setColor(0.55f, 0.20f, 0.85f, 0.55f);
		} else {
			shapes.setColor(0.90f, 0.60f, 0.15f, 0.55f);
		}
		shapes.circle(btnDimX, btnDimY, BTN_R);

		shapes.setColor(0.90f, 0.22f, 0.22f, 0.58f);
		shapes.circle(btnAttackX, btnAttackY, BTN_R);

		shapes.setColor(0.20f, 0.24f, 0.32f, 0.66f);
		shapes.circle(btnPauseX, btnPauseY, PAUSE_BTN_R);
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(1f, 1f, 1f, 0.45f);
		shapes.circle(joyBaseX, joyBaseY, JOY_OUTER_R);
		shapes.circle(btnJumpX, btnJumpY, BTN_R);
		shapes.circle(btnDimX, btnDimY, BTN_R);
		shapes.circle(btnAttackX, btnAttackY, BTN_R);
		shapes.circle(btnPauseX, btnPauseY, PAUSE_BTN_R);

		shapes.setColor(1f, 1f, 1f, ICON_ALPHA);
		float jumpStemTop = btnJumpY + BTN_R * 0.28f;
		float jumpStemBottom = btnJumpY - BTN_R * 0.22f;
		float jumpArrowHalf = BTN_R * 0.15f;
		shapes.line(btnJumpX, jumpStemBottom, btnJumpX, jumpStemTop);
		shapes.line(btnJumpX, jumpStemTop, btnJumpX - jumpArrowHalf, jumpStemTop - jumpArrowHalf);
		shapes.line(btnJumpX, jumpStemTop, btnJumpX + jumpArrowHalf, jumpStemTop - jumpArrowHalf);

		float orbitR = BTN_R * 0.33f;
		shapes.arc(btnDimX, btnDimY, orbitR, 38f, 230f, 18);
		shapes.arc(btnDimX, btnDimY, orbitR, 218f, 230f, 18);
		float ah = BTN_R * 0.10f;
		float p1x = btnDimX + orbitR * 0.85f;
		float p1y = btnDimY + orbitR * 0.45f;
		shapes.line(p1x, p1y, p1x - ah, p1y + ah * 0.35f);
		shapes.line(p1x, p1y, p1x - ah * 0.15f, p1y - ah);
		float p2x = btnDimX - orbitR * 0.85f;
		float p2y = btnDimY - orbitR * 0.45f;
		shapes.line(p2x, p2y, p2x + ah, p2y - ah * 0.35f);
		shapes.line(p2x, p2y, p2x + ah * 0.15f, p2y + ah);

		float swordL = BTN_R * 0.30f;
		shapes.line(btnAttackX - swordL, btnAttackY - swordL, btnAttackX + swordL, btnAttackY + swordL);
		float slashL = BTN_R * 0.22f;
		shapes.line(btnAttackX - slashL, btnAttackY + slashL * 0.6f, btnAttackX + slashL, btnAttackY - slashL * 0.6f);

		float pauseBarHalfH = PAUSE_BTN_R * 0.26f;
		float pauseBarGap = PAUSE_BTN_R * 0.12f;
		shapes.line(btnPauseX - pauseBarGap, btnPauseY - pauseBarHalfH, btnPauseX - pauseBarGap, btnPauseY + pauseBarHalfH);
		shapes.line(btnPauseX + pauseBarGap, btnPauseY - pauseBarHalfH, btnPauseX + pauseBarGap, btnPauseY + pauseBarHalfH);
		shapes.end();

		drawFloorHud();
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	private void drawFloorHud() {
		float barW = width * 0.52f;
		float barH = 18f;
		float barX = (width - barW) * 0.5f;
		float barY = height - 34f;

		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(0f, 0f, 0f, 0.45f);
		shapes.rect(barX, barY, barW, barH);
		float fill = Math.max(0f, barW * floorProgress - 4f);
		shapes.setColor(0.18f, 0.85f, 0.64f, 0.88f);
		shapes.rect(barX + 2f, barY + 2f, fill, barH - 4f);
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(1f, 1f, 1f, 0.75f);
		shapes.rect(barX, barY, barW, barH);
		shapes.end();

		batch.setProjectionMatrix(uiCam.combined);
		batch.begin();
		drawCentered("Planta " + (floorIndex + 1) + "  -  " + (int) (floorProgress * 100f) + "%", barY + 30f);
		font.draw(batch, "PH: " + player.getStats().health + "/" + player.getStats().maxHealth + "   Vidas: " + playerLives, 18f, height - 16f);
		if (floorMessageTimer > 0f) {
			float alpha = clamp01(floorMessageTimer / FLOOR_TEXT_DURATION);
			float oldR = font.getColor().r;
			float oldG = font.getColor().g;
			float oldB = font.getColor().b;
			float oldA = font.getColor().a;
			font.setColor(0.92f, 0.96f, 1f, alpha);
			drawCentered(floorMessage, height * 0.82f);
			font.setColor(oldR, oldG, oldB, oldA);
		}
		batch.end();
	}

	private void drawDimensionFlash() {
		if (dimFlashTimer <= 0f || world == null) return;

		float t = dimFlashTimer / DIM_FLASH_DURATION;
		float alpha = Math.min(0.33f, t * 0.33f);
		float[] bg = BG[world.getCurrentDimension()];

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapes.setProjectionMatrix(uiCam.combined);
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(
				Math.min(1f, bg[0] + 0.60f),
				Math.min(1f, bg[1] + 0.60f),
				Math.min(1f, bg[2] + 0.60f),
				alpha
		);
		shapes.rect(0f, 0f, width, height);
		shapes.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	@Override
	public void dispose() {
		batch.dispose();
		shapes.dispose();
		font.dispose();
		textura.dispose();
		texturaJump.dispose();
		texturaIdle.dispose();
		texturaAttack.dispose();
		mageBlueTexture.dispose();
		mageMagentaTexture.dispose();
		necromancerTexture.dispose();
		nightborneTexture.dispose();
		obeliskTexture.dispose();

		backgroundLoopTexture.dispose();
		backgroundEndTexture.dispose();
		backgroundMenuTexture.dispose();
		blackPixelTexture.dispose();
		if (world != null) world.dispose();
		music.dispose();
	}
}

