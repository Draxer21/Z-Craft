/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.server;

import dev.draxer.zcraft.ZCraft;
import dev.draxer.zcraft.data.CharacterClass;
import dev.draxer.zcraft.data.Form;
import dev.draxer.zcraft.data.Formulas;
import dev.draxer.zcraft.data.PlayerData;
import dev.draxer.zcraft.data.Race;
import dev.draxer.zcraft.data.Stat;
import dev.draxer.zcraft.network.ActionPayload;
import dev.draxer.zcraft.network.SyncPayload;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Everything that happens to a character runs here, on the server. */
@EventBusSubscriber(modid = ZCraft.MOD_ID)
public final class ZServer {
    private ZServer() {}

    private static final Identifier MELEE_MOD = ZCraft.id("melee");
    private static final Identifier HEALTH_MOD = ZCraft.id("health");
    private static final Identifier SPEED_MOD = ZCraft.id("speed");

    /** Share of the ki pool refilled per tick while charging. */
    private static final float CHARGE_RATE = 0.012f;
    /** Share of the ki pool a blast eats per tick of charging. */
    private static final float BLAST_COST = 0.004f;
    /** Share of the ki pool needed to transform. */
    private static final float TRANSFORM_COST = 0.10f;

    // ------------------------------------------------------------ syncing

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPayload(ZCraft.data(player).snapshot()));
    }

    /** Re-applies what the stats and the form do to the vanilla attributes. */
    public static void refresh(ServerPlayer player) {
        PlayerData data = ZCraft.data(player);
        boolean on = data.created;
        set(player.getAttribute(Attributes.ATTACK_DAMAGE), MELEE_MOD, on ? Formulas.meleeBonus(data) : 0, AttributeModifier.Operation.ADD_VALUE);
        set(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_MOD, on ? Formulas.healthBonus(data) : 0, AttributeModifier.Operation.ADD_VALUE);
        set(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MOD, on ? Formulas.speedMultiplier(data) - 1.0 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        data.energy = Math.min(data.energy, Formulas.maxEnergy(data));
        sync(player);
    }

    private static void set(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        if (Math.abs(amount) < 1.0e-6) attribute.removeModifier(id);
        else attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerData data = ZCraft.data(player);
        data.form = "";
        data.charging = false;
        data.blastCharge = 0;
        data.energy = Formulas.maxEnergy(data);
        refresh(player);
        player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    // ------------------------------------------------------------ actions

    public static void handleAction(ActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        PlayerData data = ZCraft.data(player);
        if (payload.action() == ActionPayload.Action.CREATE) {
            if (!data.created) {
                data.create(Race.byIndex(payload.a()), CharacterClass.byIndex(payload.b()));
                refresh(player);
                player.setHealth(player.getMaxHealth());
                player.sendOverlayMessage(Component.translatable("message.zcraft.created",
                        Component.translatable(data.race.key()), Component.translatable(data.characterClass.key())));
            }
            return;
        }
        if (!data.created) return;
        switch (payload.action()) {
            case CHARGE_START -> data.charging = true;
            case CHARGE_STOP -> data.charging = false;
            case BLAST_START -> data.blastCharge = Math.max(data.blastCharge, 1);
            case BLAST_RELEASE -> {
                if (data.blastCharge > 0) fireBlast(player, data);
                data.blastCharge = 0;
            }
            case TRANSFORM -> transform(player, data);
            case DESCEND -> {
                if (data.activeForm() != null) {
                    data.form = "";
                    refresh(player);
                }
            }
            case RAISE_STAT -> raiseStat(player, data, Stat.byId(payload.a()), payload.b());
            case RAISE_FORM_SKILL -> raiseFormSkill(player, data);
            default -> {}
        }
        sync(player);
    }

    private static void raiseStat(ServerPlayer player, PlayerData data, Stat stat, int wanted) {
        int points = Math.max(0, Math.min(wanted, Formulas.MAX_STAT - data.stat(stat)));
        int bought = 0;
        while (bought < points) {
            int cost = Formulas.statCost(data, data.totalStats());
            if (data.tp < cost) break;
            data.tp -= cost;
            data.setStat(stat, data.stat(stat) + 1);
            bought++;
        }
        if (bought == 0) {
            player.sendOverlayMessage(Component.translatable("message.zcraft.no_tp"));
            return;
        }
        float before = Formulas.maxEnergy(data);
        refresh(player);
        if (stat == Stat.ENE) data.energy = Math.min(Formulas.maxEnergy(data), data.energy + (Formulas.maxEnergy(data) - before));
    }

    private static void raiseFormSkill(ServerPlayer player, PlayerData data) {
        if (data.formSkill >= Form.line(data.race).size()) return;
        int cost = Formulas.formSkillCost(data.formSkill);
        if (data.tp < cost) {
            player.sendOverlayMessage(Component.translatable("message.zcraft.no_tp"));
            return;
        }
        data.tp -= cost;
        data.formSkill++;
        Form unlocked = Form.line(data.race).get(data.formSkill - 1);
        player.sendOverlayMessage(Component.translatable("message.zcraft.form_unlocked", Component.translatable(unlocked.key())));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void transform(ServerPlayer player, PlayerData data) {
        List<Form> line = Form.line(data.race);
        Form current = data.activeForm();
        int next = current == null ? 0 : current.level();
        if (next >= line.size() || next >= data.formSkill) {
            if (data.formSkill == 0) player.sendOverlayMessage(Component.translatable("message.zcraft.no_form"));
            return;
        }
        float cost = Formulas.maxEnergy(data) * TRANSFORM_COST;
        if (data.energy < cost) {
            player.sendOverlayMessage(Component.translatable("message.zcraft.no_ki"));
            return;
        }
        data.energy -= cost;
        Form form = line.get(next);
        data.form = form.id();
        refresh(player);
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.4f);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.3, 0.6, 0.3, 0.15);
        aura(level, player, form.color(), 40);
        player.sendOverlayMessage(Component.translatable(form.key()));
    }

    // ------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerData data = ZCraft.data(player);
        if (!data.created) return;
        ServerLevel level = (ServerLevel) player.level();
        float max = Formulas.maxEnergy(data);
        boolean busy = false;

        if (data.charging) {
            // Charging is fast but roots you in place.
            data.energy = Math.min(max, data.energy + max * CHARGE_RATE + 0.2f);
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            if (player.tickCount % 2 == 0) aura(level, player, data.color(), 6);
            busy = true;
        } else if (data.activeForm() == null && data.energy < max) {
            data.energy = Math.min(max, data.energy + Formulas.energyRegen(data));
        }

        if (data.blastCharge > 0) {
            float cost = max * BLAST_COST + 0.05f;
            if (data.blastCharge < Formulas.BLAST_MAX_CHARGE && data.energy >= cost) {
                data.energy -= cost;
                data.blastCharge++;
            }
            if (player.tickCount % 3 == 0) {
                Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0, 0.3, 0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 4, 0.15, 0.15, 0.15, 0.02);
            }
            busy = true;
        }

        Form form = data.activeForm();
        if (form != null) {
            data.energy -= Formulas.formDrain(data);
            if (data.energy <= 0f) {
                data.energy = 0f;
                data.form = "";
                refresh(player);
                player.sendOverlayMessage(Component.translatable("message.zcraft.ki_depleted"));
            } else if (player.tickCount % 4 == 0) {
                aura(level, player, form.color(), 3);
            }
            busy = true;
        }

        if (player.tickCount % (busy ? 2 : 10) == 0) sync(player);
    }

    private static void aura(ServerLevel level, ServerPlayer player, int color, int count) {
        level.sendParticles(new DustParticleOptions(color, 1.2f),
                player.getX(), player.getY() + 1.0, player.getZ(), count, 0.4, 0.8, 0.4, 0.01);
    }

    // ------------------------------------------------------------ ki blast

    /** A beam from the eyes: stops at blocks and hurts every living thing it crosses. */
    private static void fireBlast(ServerPlayer player, PlayerData data) {
        ServerLevel level = (ServerLevel) player.level();
        float charge = Math.min(1f, (float) data.blastCharge / Formulas.BLAST_MAX_CHARGE);
        double range = 8 + 24 * charge;
        float damage = (float) (Formulas.kiDamage(data) * (0.25 + 0.75 * charge));
        double width = 0.4 + 0.8 * charge;

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(range));
        var blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        AABB area = new AABB(start, end).inflate(width);
        for (Entity entity : level.getEntities(player, area, e -> e instanceof LivingEntity && e.isAlive())) {
            AABB box = entity.getBoundingBox().inflate(width);
            if (box.clip(start, end).isPresent() || box.contains(start)) {
                entity.hurtServer(level, player.damageSources().indirectMagic(player, player), damage);
                entity.push(direction.x * charge, 0.2 * charge, direction.z * charge);
            }
        }

        double length = start.distanceTo(end);
        DustParticleOptions beam = new DustParticleOptions(data.color(), 1.5f + charge);
        for (double d = 0.8; d < length; d += 0.4) {
            Vec3 p = start.add(direction.scale(d)).subtract(0, 0.2, 0);
            level.sendParticles(beam, p.x, p.y, p.z, 2, width * 0.3, width * 0.3, width * 0.3, 0);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1 + (int) (charge * 3), 0.3, 0.3, 0.3, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.6f - charge);
    }

    // ------------------------------------------------------------ combat

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        // Defense: resistance and armor take a flat bite out of the hit.
        if (victim instanceof ServerPlayer player) {
            PlayerData data = ZCraft.data(player);
            if (data.created) {
                double defense = Formulas.defense(data, player.getArmorValue(), player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
                event.setAmount(Formulas.mitigate(event.getAmount(), defense));
            }
        }
        // Training points for every hit landed on something else.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim && event.getAmount() > 0f) {
            PlayerData data = ZCraft.data(attacker);
            if (data.created) data.tp += Formulas.TP_PER_HIT * data.characterClass.tpGain();
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim) return;
        PlayerData data = ZCraft.data(attacker);
        if (!data.created) return;
        double gained = victim.getMaxHealth() * Formulas.TP_PER_KILL_HEALTH * data.characterClass.tpGain();
        data.tp += gained;
        attacker.sendOverlayMessage(Component.translatable("message.zcraft.tp_gained", Math.round(gained)));
        sync(attacker);
    }
}
