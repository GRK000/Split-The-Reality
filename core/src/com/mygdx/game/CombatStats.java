package com.mygdx.game;

public class CombatStats {
    public int maxHealth;
    public int health;
    public int attack;
    public int defense;
    public float speed;
    public float jumpPower;
    public float attackCooldown;
    public float critChance;

    public CombatStats(int maxHealth, int attack, int defense, float speed, float jumpPower, float attackCooldown, float critChance) {
        this.maxHealth = Math.max(1, maxHealth);
        this.health = this.maxHealth;
        this.attack = Math.max(1, attack);
        this.defense = Math.max(0, defense);
        this.speed = Math.max(0.5f, speed);
        this.jumpPower = Math.max(1f, jumpPower);
        this.attackCooldown = Math.max(0.05f, attackCooldown);
        this.critChance = Math.max(0f, Math.min(1f, critChance));
    }

    public int computeDamageAgainst(CombatStats target) {
        float reduced = attack - target.defense * 0.55f;
        if (Math.random() < critChance) {
            reduced *= 1.5f;
        }
        return Math.max(1, Math.round(reduced));
    }

    public void applyDamage(int rawDamage) {
        health -= Math.max(0, rawDamage);
        if (health < 0) health = 0;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void healToFull() {
        health = maxHealth;
    }
}

