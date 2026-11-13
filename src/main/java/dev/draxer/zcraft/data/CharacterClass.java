/*
 * Z-Craft — GPL-3.0-or-later.
 * Class defaults (starting stats, scalings, energy regeneration and TP multipliers) are taken from
 * DragonMine Z's default stats config (GPL-3.0), see NOTICE.md.
 */
package dev.draxer.zcraft.data;

import java.util.Locale;

/** A fighting style: where the starting points go and how well each stat pays off. */
public enum CharacterClass {
    //            starting stats (STR SKP RES VIT PWR ENE)   scalings: STR  SKP  DEF    VIT  PWR  ENE   Ep5   Ep5/ENE
    WARRIOR(      10, 0, 5, 5, 0, 0,                         1.4, 1.0, 0.24,  1.8, 0.5, 1.5,  4.0,  0.08),
    SPIRITUALIST( 0, 0, 0, 0, 10, 10,                        0.3, 0.5, 0.156, 1.4, 1.9, 3.7,  8.0,  0.20),
    MARTIAL_ARTIST(0, 10, 0, 10, 0, 0,                       0.8, 1.8, 0.18,  2.2, 0.6, 1.6,  4.0,  0.08),
    BERSERKER(    10, 0, 0, 10, 0, 0,                        1.7, 0.8, 0.18,  3.0, 0.4, 1.3,  2.0,  0.04),
    PALADIN(      0, 5, 10, 5, 0, 0,                         0.8, 1.2, 0.336, 2.0, 0.6, 1.2,  4.0,  0.08),
    TANK(         0, 0, 10, 10, 0, 0,                        0.6, 0.7, 0.384, 2.5, 0.5, 0.8,  5.0,  0.10),
    CLERIC(       0, 0, 5, 0, 0, 15,                         0.5, 0.5, 0.168, 1.2, 0.8, 3.0,  12.0, 0.24),
    UNVERSED(     4, 3, 3, 4, 3, 3,                          0.9, 0.9, 0.22,  1.9, 0.85, 1.75, 5.0, 0.10);

    public static final CharacterClass[] VALUES = values();

    private final int[] base;
    private final double[] scaling;
    /** Energy regenerated every 5 seconds, before the ENE bonus. */
    public final double baseEp5;
    /** Extra energy every 5 seconds per point of ENE. */
    public final double ep5PerEne;

    CharacterClass(int str, int skp, int res, int vit, int pwr, int ene,
                   double strScale, double skpScale, double defScale, double vitScale, double pwrScale, double eneScale,
                   double baseEp5, double ep5PerEne) {
        this.base = new int[] {str, skp, res, vit, pwr, ene};
        this.scaling = new double[] {strScale, skpScale, defScale, vitScale, pwrScale, eneScale};
        this.baseEp5 = baseEp5;
        this.ep5PerEne = ep5PerEne;
    }

    public int base(Stat stat) {
        return base[stat.ordinal()];
    }

    /** How much one point of the stat is worth for this class. */
    public double scaling(Stat stat) {
        return scaling[stat.ordinal()];
    }

    /** Multiplier on training points earned. */
    public double tpGain() {
        return this == TANK || this == CLERIC ? 1.25 : 1.0;
    }

    /** Multiplier on what a stat point costs. */
    public double tpCost() {
        return this == CLERIC ? 0.9 : 1.0;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "class.zcraft." + id();
    }

    public static CharacterClass byId(String id) {
        for (CharacterClass c : VALUES) if (c.id().equals(id)) return c;
        return WARRIOR;
    }

    public static CharacterClass byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : WARRIOR;
    }
}
