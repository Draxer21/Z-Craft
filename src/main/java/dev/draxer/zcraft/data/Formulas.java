/*
 * Z-Craft — GPL-3.0-or-later.
 * The shape of these formulas (stat x class scaling x form multiplier, flat defense with an absorb
 * cap, the TP cost curve with its early-game discount) is adapted from DragonMine Z's StatsData
 * (GPL-3.0), see NOTICE.md. The constants under "Balance" are Z-Craft's own.
 */
package dev.draxer.zcraft.data;

/** All the numbers of the game in one place. Used by both sides, so it never touches the world. */
public final class Formulas {
    private Formulas() {}

    // ------------------------------------------------------------ Balance

    /** Highest value a single stat can reach. */
    public static final int MAX_STAT = 10_000;
    /** Ki pool of a character with no ENE at all. */
    public static final double BASE_ENERGY = 20.0;
    /** Stat damage is scaled down so early characters fit a vanilla world (zombies have 20 HP). */
    public static final double DAMAGE_SCALE = 0.25;
    /** Flat defense can never absorb more than this share of a hit. */
    public static final double MAX_ABSORB = 0.6;
    /** Ticks of holding the blast key for a full charge. */
    public static final int BLAST_MAX_CHARGE = 60;
    /** Cheapest a stat point can be. */
    public static final int MIN_STAT_COST = 16;
    /** Characters below this many total stats get a discount. */
    public static final int DISCOUNT_THRESHOLD = 140;
    public static final double STAT_COST_PER_POINT = 1.25;
    /** TP for landing a hit. */
    public static final double TP_PER_HIT = 2.0;
    /** TP for a kill, per point of the victim's max health. */
    public static final double TP_PER_KILL_HEALTH = 0.5;

    // ------------------------------------------------------------ derived values

    private static double scaled(PlayerData data, Stat stat) {
        return data.stat(stat) * data.characterClass.scaling(stat);
    }

    public static float maxEnergy(PlayerData data) {
        return (float) (BASE_ENERGY + scaled(data, Stat.ENE));
    }

    /** Ki regenerated per tick in base form. */
    public static float energyRegen(PlayerData data) {
        CharacterClass c = data.characterClass;
        return (float) ((c.baseEp5 + data.stat(Stat.ENE) * c.ep5PerEne) / 100.0);
    }

    /** Ki a form burns per tick. */
    public static float formDrain(PlayerData data) {
        Form form = data.activeForm();
        if (form == null || form.drain() <= 0) return 0f;
        return (float) (maxEnergy(data) * form.drain() * 0.05 / 20.0);
    }

    /** Extra melee damage on top of the weapon. */
    public static double meleeBonus(PlayerData data) {
        Form form = data.activeForm();
        return scaled(data, Stat.STR) * (form != null ? form.str() : 1.0) * DAMAGE_SCALE;
    }

    /** Damage of a ki attack at full charge. */
    public static double kiDamage(PlayerData data) {
        Form form = data.activeForm();
        return 2.0 + scaled(data, Stat.PWR) * (form != null ? form.pwr() : 1.0) * DAMAGE_SCALE * 4.0;
    }

    /** Flat damage taken off every hit. Armor counts too. */
    public static double defense(PlayerData data, double armor, double toughness) {
        Form form = data.activeForm();
        return scaled(data, Stat.RES) * (form != null ? form.def() : 1.0) + armor * 0.5 + toughness * 0.7;
    }

    /** What is left of a hit after flat defense. */
    public static float mitigate(float incoming, double defense) {
        if (incoming <= 0f) return incoming;
        double absorbed = Math.min(defense * DAMAGE_SCALE, incoming * MAX_ABSORB);
        return (float) (incoming - absorbed);
    }

    /** Extra max health. */
    public static double healthBonus(PlayerData data) {
        return scaled(data, Stat.VIT);
    }

    public static double speedMultiplier(PlayerData data) {
        Form form = data.activeForm();
        return form != null ? form.speed() : 1.0;
    }

    // ------------------------------------------------------------ training points

    /** TP for the next stat point when the character already has that many in total. */
    public static int statCost(PlayerData data, int totalStats) {
        double base = MIN_STAT_COST + totalStats * STAT_COST_PER_POINT;
        int discount = totalStats < DISCOUNT_THRESHOLD ? DISCOUNT_THRESHOLD - totalStats : 0;
        int cost = (int) (base * data.characterClass.tpCost()) - discount;
        return Math.max(MIN_STAT_COST, cost);
    }

    /** TP for {@code points} stat points in a row. */
    public static long statCost(PlayerData data, int totalStats, int points) {
        long cost = 0;
        for (int i = 0; i < points; i++) cost += statCost(data, totalStats + i);
        return cost;
    }

    /** TP to raise the transformation skill from {@code level} to the next one. */
    public static int formSkillCost(int level) {
        return 400 * (level + 1) * (level + 1);
    }
}
