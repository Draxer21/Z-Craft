/*
 * Z-Craft — GPL-3.0-or-later.
 * The six-stat model is adapted from DragonMine Z (GPL-3.0), see NOTICE.md.
 */
package dev.draxer.zcraft.data;

/** The six trainable stats of a character. */
public enum Stat {
    /** Strength: melee damage. */
    STR,
    /** Strike power: damage of physical techniques. */
    SKP,
    /** Resistance: defense against incoming damage. */
    RES,
    /** Vitality: extra health. */
    VIT,
    /** Ki power: damage of ki attacks. */
    PWR,
    /** Energy: size of the ki pool and how fast it refills. */
    ENE;

    public static final Stat[] VALUES = values();

    public String key() {
        return "stat.zcraft." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Stat byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : STR;
    }
}
