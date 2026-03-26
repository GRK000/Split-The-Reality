package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Enemy {
    public enum Type { MAGE_BLUE, MAGE_MAGENTA, NECROMANCER, NIGHTBORNE }
    public enum HazardType { OBELISK, DARK_ORB }

    public static final class Hazard {
        public final HazardType type;
        public float x, y, w, h;
        public float vx, vy;
        public float telegraphTimer;
        public float lifetime;
        public int damage;

        Hazard(HazardType type, float x, float y, float w, float h, float vx, float vy, float telegraphTimer, float lifetime, int damage) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.vx = vx;
            this.vy = vy;
            this.telegraphTimer = telegraphTimer;
            this.lifetime = lifetime;
            this.damage = damage;
        }

        public boolean isTelegraphing() {
            return telegraphTimer > 0f;
        }

        public void toRect(Rectangle out) {
            out.set(x, y, w, h);
        }
    }

    private enum AnimState { IDLE, ATTACK }

    private static final float NIGHTBORNE_ATTACK_DURATION = 0.46f;
    private static final float NIGHTBORNE_MELEE_RANGE = 90f;

    public final Type type;
    public final CombatStats stats;

    public float x, y, w, h;
    public boolean alive = true;

    private AnimState animState = AnimState.IDLE;
    private int frame;
    private float animTimer;

    private float attackCooldownTimer;
    private float attackTimer;
    private boolean swingHitDone;
    private boolean facingRight = true;
    private int lastHitAttackSerial = -1;

    private final List<Hazard> hazards = new ArrayList<>();

    private Enemy(Type type, CombatStats stats, float x, float y, float w, float h) {
        this.type = type;
        this.stats = stats;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static Enemy create(Type type, float x, float y, float tile, int floorIndex) {
        int floor = Math.max(0, floorIndex);
        float scale = 1f + floor * 0.10f;
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                return new Enemy(type, new CombatStats(45 + floor * 7, 10 + floor * 2, 5 + floor, 1.5f, 0f, 1.8f / scale, 0f), x, y, tile * 1.8f, tile * 1.8f);
            case NECROMANCER:
                return new Enemy(type, new CombatStats(60 + floor * 10, 14 + floor * 3, 7 + floor, 1.3f, 0f, 1.6f / scale, 0.06f), x, y, tile * 5f, tile * 5f);
            case NIGHTBORNE:
                return new Enemy(type, new CombatStats(85 + floor * 14, 16 + floor * 4, 9 + floor, 2.2f, 0f, 1.3f / scale, 0.1f), x, y, tile * 2.5f, tile * 3f);
            default:
                return new Enemy(type, new CombatStats(85 + floor * 14, 16 + floor * 4, 9 + floor, 2.2f, 0f, 1.3f / scale, 0.1f), x, y, tile * 2.7f, tile * 2.3f);
        }
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public float getFootY() {
        return y;
    }

    public void setFootY(float footY) {
        y = footY;
    }

    public void update(float delta, float playerX, float playerY) {
        if (!alive) return;

        attackCooldownTimer = Math.max(0f, attackCooldownTimer - delta);
        updateHazards(delta);

        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                updateMageAttack(playerX, playerY);
                break;
            case NECROMANCER:
                updateNecromancerAttack(playerX, playerY);
                break;
            case NIGHTBORNE:
                updateNightborneAttack(delta, playerX);
                break;
        }

        updateAnim(delta);
    }

    private void updateMageAttack(float playerX, float playerY) {
        if (attackCooldownTimer > 0f) return;
        attackCooldownTimer = stats.attackCooldown;
        float obeliskX = playerX - 14f;
        float startY = playerY + 220f;
        hazards.add(new Hazard(HazardType.OBELISK, obeliskX, startY, 50f, 92f, 0f, -380f, 0.55f, 2.6f, stats.attack));
    }

    private void updateNecromancerAttack(float playerX, float playerY) {
        if (attackCooldownTimer > 0f)
            return;
        attackCooldownTimer = stats.attackCooldown;
        float cx = x + w * 0.5f;
        float cy = y + h * 0.52f;
        float dx = playerX - cx;
        float dy = playerY - cy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) len = 1f;
        float speed = 220f;
        hazards.add(new Hazard(HazardType.DARK_ORB, cx - 14f, cy - 14f, 28f, 28f, dx / len * speed, dy / len * speed, 0f, 3.2f, stats.attack));
    }

    private void updateNightborneAttack(float delta, float playerX) {
        float dx = playerX - (x + w * 0.5f);
        if (Math.abs(dx) <= NIGHTBORNE_MELEE_RANGE && attackCooldownTimer <= 0f && animState != AnimState.ATTACK) {
            animState = AnimState.ATTACK;
            attackTimer = NIGHTBORNE_ATTACK_DURATION;
            attackCooldownTimer = stats.attackCooldown;
            swingHitDone = false;
            frame = 0;
            animTimer = 0f;
        }

        if (animState == AnimState.ATTACK) {
            attackTimer -= delta;
            if (attackTimer <= 0f) {
                animState = AnimState.IDLE;
                attackTimer = 0f;
                swingHitDone = false;
            }
        }
    }

    private void updateHazards(float delta) {
        Iterator<Hazard> it = hazards.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            h.lifetime -= delta;
            if (h.lifetime <= 0f) {
                it.remove();
                continue;
            }
            if (h.telegraphTimer > 0f) {
                h.telegraphTimer -= delta;
                continue;
            }
            h.x += h.vx * delta;
            h.y += h.vy * delta;
        }
    }

    private void updateAnim(float delta) {
        animTimer += delta;
        while (animTimer >= getFrameTime()) {
            animTimer -= getFrameTime();
            frame = (frame + 1) % getFrameCount();
        }
    }

    public int getFrameWidth() {
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                return 64;
            case NECROMANCER:
                return 160;
            case NIGHTBORNE:
            default:
                return 80;
        }
    }

    public int getFrameHeight() {
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                return 80;
            case NECROMANCER:
                return 128;
            case NIGHTBORNE:
            default:
                return 80;
        }
    }

    public int getFrameCount() {
        if (type == Type.NIGHTBORNE && animState == AnimState.ATTACK) return 12;
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                return 14;
            case NECROMANCER:
                return 7;
            case NIGHTBORNE:
            default:
                return 9;
        }
    }

    public int getRowFromTop() {
        if (type == Type.NIGHTBORNE && animState == AnimState.ATTACK) {
            return 2;
        }
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                return 0;
            case NECROMANCER:
                return 4;
            case NIGHTBORNE:
            default:
                return 4;
        }
    }

    public float getFrameTime() {
        if (type == Type.NIGHTBORNE && animState == AnimState.ATTACK)
            return 0.045f;
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                return 0.09f;
            case NECROMANCER:
                return 0.11f;
            case NIGHTBORNE:
            default:
                return 0.10f;
        }
    }

    public int getCurrentFrame() {
        return frame;
    }

    public void getBounds(Rectangle out) {
        out.set(x, y, w, h);
    }

    public void getHurtBounds(Rectangle out) {
        float insetX;
        float insetY;
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                insetX = w * 0.18f;
                insetY = h * 0.06f;
                break;
            case NECROMANCER:
                insetX = w * 0.28f;
                insetY = h * 0.18f;
                break;
            case NIGHTBORNE:
            default:
                insetX = w * 0.30f;
                insetY = h * 0.20f;
                break;
        }
        out.set(x + insetX, y + insetY, Math.max(8f, w - insetX * 2f), Math.max(8f, h - insetY * 2f));
    }

    public List<Hazard> getHazards() {
        return hazards;
    }

    public int getKillRewardCoins() {
        int base;
        switch (type) {
            case MAGE_BLUE:
            case MAGE_MAGENTA:
                base = 9;
                break;
            case NECROMANCER:
                base = 14;
                break;
            case NIGHTBORNE:
            default:
                base = 16;
                break;
        }
        return base + Math.max(0, stats.attack / 3);
    }

    public boolean canMeleeHitPlayer(Rectangle playerRect, Rectangle tmpEnemyRect) {
        if (type != Type.NIGHTBORNE || animState != AnimState.ATTACK || swingHitDone) return false;
        float progress = 1f - (attackTimer / Math.max(0.01f, NIGHTBORNE_ATTACK_DURATION));
        if (progress < 0.35f || progress > 0.65f) return false;
        tmpEnemyRect.set(x, y, w, h);
        if (tmpEnemyRect.overlaps(playerRect)) {
            swingHitDone = true;
            return true;
        }
        return false;
    }

    public int receiveHit(int rawDamage, int attackSerial) {
        if (!alive) return 0;
        if (attackSerial == lastHitAttackSerial) return 0;
        lastHitAttackSerial = attackSerial;

        int dealt = Math.max(1, rawDamage - Math.round(stats.defense * 0.35f));
        stats.applyDamage(dealt);
        alive = stats.isAlive();
        return dealt;
    }
}

