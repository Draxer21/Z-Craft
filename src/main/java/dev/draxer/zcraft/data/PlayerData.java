/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.Nullable;

/**
 * Everything Z-Craft knows about one player's character. Lives on the player as an attachment
 * (saved with the world, kept after death) and is mirrored to the client by {@code SyncPayload}.
 */
public final class PlayerData {
    public static final MapCodec<PlayerData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("created").forGetter(d -> d.created),
            Codec.STRING.fieldOf("race").forGetter(d -> d.race.id()),
            Codec.STRING.fieldOf("class").forGetter(d -> d.characterClass.id()),
            Codec.INT.listOf().fieldOf("stats").forGetter(PlayerData::statList),
            Codec.DOUBLE.fieldOf("tp").forGetter(d -> d.tp),
            Codec.FLOAT.fieldOf("energy").forGetter(d -> d.energy),
            Codec.STRING.fieldOf("form").forGetter(d -> d.form),
            Codec.INT.fieldOf("form_skill").forGetter(d -> d.formSkill)
    ).apply(i, PlayerData::new));

    /** False until the player picks a race and class. */
    public boolean created;
    public Race race = Race.HUMAN;
    public CharacterClass characterClass = CharacterClass.WARRIOR;
    private final int[] stats = new int[Stat.VALUES.length];
    /** Training points, spent on stats and on the transformation skill. */
    public double tp;
    /** Current ki. */
    public float energy;
    /** Id of the active form, empty in base form. */
    public String form = "";
    /** Transformation skill: forms up to this level are unlocked. */
    public int formSkill;

    // Not saved: what the player is doing right now.
    public boolean charging;
    public int blastCharge;

    public PlayerData() {}

    private PlayerData(boolean created, String race, String characterClass, List<Integer> stats, double tp, float energy, String form, int formSkill) {
        this.created = created;
        this.race = Race.byId(race);
        this.characterClass = CharacterClass.byId(characterClass);
        for (int i = 0; i < this.stats.length && i < stats.size(); i++) this.stats[i] = stats.get(i);
        this.tp = tp;
        this.energy = energy;
        this.form = form;
        this.formSkill = formSkill;
    }

    private List<Integer> statList() {
        List<Integer> list = new ArrayList<>(stats.length);
        for (int value : stats) list.add(value);
        return list;
    }

    public int stat(Stat stat) {
        return stats[stat.ordinal()];
    }

    public void setStat(Stat stat, int value) {
        stats[stat.ordinal()] = Math.max(0, Math.min(Formulas.MAX_STAT, value));
    }

    public int totalStats() {
        int total = 0;
        for (int value : stats) total += value;
        return total;
    }

    public @Nullable Form activeForm() {
        Form active = Form.byId(form);
        return active != null && active.race() == race ? active : null;
    }

    /** Aura colour right now (RGB). */
    public int color() {
        Form active = activeForm();
        return active != null ? active.color() : race.color;
    }

    /** Starts a fresh character of that race and class. */
    public void create(Race race, CharacterClass characterClass) {
        this.created = true;
        this.race = race;
        this.characterClass = characterClass;
        for (Stat stat : Stat.VALUES) setStat(stat, characterClass.base(stat));
        this.tp = 0;
        this.form = "";
        this.formSkill = 0;
        this.energy = Formulas.maxEnergy(this);
    }

    public void reset() {
        created = false;
        java.util.Arrays.fill(stats, 0);
        tp = 0;
        energy = 0;
        form = "";
        formSkill = 0;
        charging = false;
        blastCharge = 0;
    }

    /** A copy that is safe to hand to another thread. */
    public PlayerData snapshot() {
        PlayerData copy = new PlayerData(created, race.id(), characterClass.id(), statList(), tp, energy, form, formSkill);
        copy.charging = charging;
        copy.blastCharge = blastCharge;
        return copy;
    }

    // ------------------------------------------------------------ network

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(created);
        buf.writeVarInt(race.ordinal());
        buf.writeVarInt(characterClass.ordinal());
        for (int value : stats) buf.writeVarInt(value);
        buf.writeDouble(tp);
        buf.writeFloat(energy);
        buf.writeUtf(form);
        buf.writeVarInt(formSkill);
        buf.writeBoolean(charging);
        buf.writeVarInt(blastCharge);
    }

    public static PlayerData read(FriendlyByteBuf buf) {
        PlayerData data = new PlayerData();
        data.created = buf.readBoolean();
        data.race = Race.byIndex(buf.readVarInt());
        data.characterClass = CharacterClass.byIndex(buf.readVarInt());
        for (int i = 0; i < data.stats.length; i++) data.stats[i] = buf.readVarInt();
        data.tp = buf.readDouble();
        data.energy = buf.readFloat();
        data.form = buf.readUtf();
        data.formSkill = buf.readVarInt();
        data.charging = buf.readBoolean();
        data.blastCharge = buf.readVarInt();
        return data;
    }
}
