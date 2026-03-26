package com.mygdx.game;

public class Player {
    private static final float DEFAULT_ATTACK_DURATION = 0.30f;
    private static final float HURT_INVULN_TIME = 0.60f;

    private final CombatStats stats;
    private boolean attacking;
    private float attackTimer;
    private float attackDuration;
    private float attackCooldownTimer;
    private float hurtInvulnTimer;
    private int coins;
    private int attackSerial;

    public Player(CombatStats stats) {
        this.stats = stats;
        this.attackDuration = DEFAULT_ATTACK_DURATION;
    }

    public static Player createDefault() {
        CombatStats base = new CombatStats(
                120,
                18,
                8,
                4f,
                10f,
                0.12f,
                0.07f
        );
        return new Player(base);
    }

    public CombatStats getStats() {
        return stats;
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        coins += Math.max(0, amount);
    }

    public boolean trySpendCoins(int amount) {
        int cost = Math.max(0, amount);
        if (coins < cost) return false;
        coins -= cost;
        return true;
    }

    public void resetRunEconomy() {
        coins = 0;
    }

    public void update(float delta) {
        attackCooldownTimer = Math.max(0f, attackCooldownTimer - delta);
        hurtInvulnTimer = Math.max(0f, hurtInvulnTimer - delta);
        if (attacking) {
            attackTimer -= delta;
            if (attackTimer <= 0f) {
                attacking = false;
                attackTimer = 0f;
            }
        }
    }

    public boolean tryStartAttack() {
        if (attackCooldownTimer > 0f) return false;
        attacking = true;
        attackTimer = attackDuration;
        attackCooldownTimer = stats.attackCooldown;
        attackSerial++;
        return true;
    }

    public int getAttackSerial() {
        return attackSerial;
    }

    public boolean isAttacking() {
        return attacking;
    }

    public float getAttackProgress() {
        if (!attacking) return 1f;
        float p = 1f - attackTimer / Math.max(0.01f, attackDuration);
        return Math.max(0f, Math.min(1f, p));
    }

    public boolean canReceiveDamage() {
        return hurtInvulnTimer <= 0f;
    }

    public int receiveDamage(int rawDamage) {
        if (!canReceiveDamage()) return 0;
        int reduced = Math.max(1, Math.round(rawDamage - stats.defense * 0.45f));
        stats.applyDamage(reduced);
        hurtInvulnTimer = HURT_INVULN_TIME;
        return reduced;
    }

    public void resetForFloor() {
        attacking = false;
        attackTimer = 0f;
        attackCooldownTimer = 0f;
        hurtInvulnTimer = 0f;
    }

    public float getMoveSpeed() {
        return stats.speed;
    }

    public float getJumpPower() {
        return stats.jumpPower;
    }

    public void upgradeHealth() {
        stats.maxHealth += 20;
        stats.health = Math.min(stats.maxHealth, stats.health + 20);
    }

    public void upgradeAttack() {
        stats.attack += 3;
    }

    public void upgradeDefense() {
        stats.defense += 2;
    }

    public void upgradeSpeed() {
        stats.speed += 0.35f;
    }

    public void upgradeJump() {
        stats.jumpPower += 0.8f;
    }
}

