/*
 * Z-Craft — GPL-3.0-or-later.
 * Form lines, multipliers, energy drain and unlock levels come from DragonMine Z's default forms
 * (GPL-3.0), see NOTICE.md.
 */
package dev.draxer.zcraft.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A transformation.
 *
 * @param id     unique name, also the translation key suffix
 * @param race   who can use it
 * @param level  transformation skill needed to unlock it (1 = first form)
 * @param str    multiplier on melee and strike damage
 * @param def    multiplier on defense
 * @param pwr    multiplier on ki damage
 * @param speed  multiplier on movement speed
 * @param drain  energy drain weight (0 = free to hold)
 * @param color  aura colour (RGB)
 */
public record Form(String id, Race race, int level, double str, double def, double pwr, double speed, double drain, int color) {
    private static final Map<Race, List<Form>> BY_RACE = new EnumMap<>(Race.class);
    private static final Map<String, Form> BY_ID = new java.util.HashMap<>();

    private static void add(Race race, String id, double str, double def, double pwr, double speed, double drain, int color) {
        List<Form> line = BY_RACE.computeIfAbsent(race, r -> new ArrayList<>());
        Form form = new Form(id, race, line.size() + 1, str, def, pwr, speed, drain, color);
        line.add(form);
        BY_ID.put(id, form);
    }

    static {
        add(Race.SAIYAN, "super_saiyan", 1.6, 1.375, 1.6, 1.0, 0.101, 0xFFE14A);
        add(Race.SAIYAN, "super_saiyan_grade_2", 1.85, 1.575, 1.85, 0.9, 0.163, 0xFFE14A);
        add(Race.SAIYAN, "super_saiyan_grade_3", 3.0, 2.15, 3.0, 0.7, 0.831, 0xFFE14A);
        add(Race.SAIYAN, "super_saiyan_mastered", 1.85, 1.5, 1.85, 1.0, 0.041, 0xFFEE80);
        add(Race.SAIYAN, "super_saiyan_2", 2.45, 1.9375, 2.45, 1.0, 0.25, 0xFFE14A);
        add(Race.SAIYAN, "super_saiyan_3", 3.3, 2.65, 3.3, 1.0, 0.618, 0xFFD000);
        add(Race.SAIYAN, "super_saiyan_4", 4.75, 3.5625, 4.75, 1.0, 0.523, 0xE03030);

        add(Race.HUMAN, "buffed", 1.7, 1.425, 1.5, 1.0, 0.109, 0xFFFFFF);
        add(Race.HUMAN, "full_power", 2.25, 1.9375, 2.05, 1.0, 0.25, 0xFFFFFF);
        add(Race.HUMAN, "overdrive", 3.15, 2.65, 2.85, 1.0, 0.618, 0xFF7A3A);
        add(Race.HUMAN, "solaris", 4.6, 3.6, 4.25, 1.0, 0.479, 0xFFC040);

        add(Race.NAMEKIAN, "giant", 2.15, 1.8625, 2.15, 1.0, 0.132, 0x7BE06A);
        add(Race.NAMEKIAN, "namekian_full_power", 3.15, 2.5125, 3.15, 1.0, 0.319, 0xB0FF9A);
        add(Race.NAMEKIAN, "super_namekian", 4.75, 3.5625, 4.75, 1.0, 0.588, 0xE0FFD0);

        add(Race.FROST_DEMON, "second_form", 1.75, 1.425, 1.75, 1.0, 0.0, 0xC08BFF);
        add(Race.FROST_DEMON, "third_form", 2.25, 1.9375, 2.25, 1.0, 0.0, 0xC08BFF);
        add(Race.FROST_DEMON, "final_form", 2.85, 2.3625, 2.85, 1.0, 0.0, 0xE0C8FF);
        add(Race.FROST_DEMON, "frost_full_power", 3.45, 2.725, 3.45, 1.0, 0.409, 0xE0C8FF);
        add(Race.FROST_DEMON, "fifth_form", 4.9, 3.6875, 4.9, 1.0, 0.62, 0xFFD23F);

        add(Race.BIO_ANDROID, "semi_perfect", 1.85, 1.575, 1.85, 1.0, 0.0, 0x4FE0B0);
        add(Race.BIO_ANDROID, "perfect", 2.6, 2.225, 2.6, 1.0, 0.0, 0x4FE0B0);
        add(Race.BIO_ANDROID, "super_perfect", 3.35, 2.65, 3.35, 1.0, 0.293, 0xFFE14A);
        add(Race.BIO_ANDROID, "ultra_perfect", 4.9, 3.6875, 4.9, 0.6, 0.62, 0xFFE14A);

        add(Race.MAJIN, "kid", 1.85, 1.575, 1.85, 1.0, 0.0, 0xFF8AC8);
        add(Race.MAJIN, "evil", 2.45, 2.15, 2.45, 1.0, 0.0, 0xB0B0B0);
        add(Race.MAJIN, "super", 3.3, 2.8, 3.3, 1.0, 0.0, 0xFF8AC8);
        add(Race.MAJIN, "ultra", 4.75, 3.725, 4.75, 1.0, 0.479, 0xFF5AA8);
    }

    /** The race's forms, weakest first. */
    public static List<Form> line(Race race) {
        return BY_RACE.getOrDefault(race, List.of());
    }

    /** The form with that id, or null for base form. */
    public static @Nullable Form byId(String id) {
        return id.isEmpty() ? null : BY_ID.get(id);
    }

    public String key() {
        return "form.zcraft." + id;
    }
}
