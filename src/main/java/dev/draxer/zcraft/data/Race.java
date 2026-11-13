/*
 * Z-Craft — GPL-3.0-or-later.
 * The race list follows DragonMine Z (GPL-3.0), see NOTICE.md.
 */
package dev.draxer.zcraft.data;

import java.util.Locale;

/** A playable race. Each one has its own line of transformations (see {@link Form}). */
public enum Race {
    HUMAN(0x8FD3FF),
    SAIYAN(0xFFD23F),
    NAMEKIAN(0x7BE06A),
    FROST_DEMON(0xC08BFF),
    BIO_ANDROID(0x4FE0B0),
    MAJIN(0xFF8AC8);

    public static final Race[] VALUES = values();

    /** Aura and ki colour in base form (RGB). */
    public final int color;

    Race(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "race.zcraft." + id();
    }

    public static Race byId(String id) {
        for (Race r : VALUES) if (r.id().equals(id)) return r;
        return HUMAN;
    }

    public static Race byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : HUMAN;
    }
}
