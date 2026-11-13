/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.network;

import dev.draxer.zcraft.ZCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the player pressed a key or clicked something in the character menu. */
public record ActionPayload(Action action, int a, int b) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionPayload> TYPE = new CustomPacketPayload.Type<>(ZCraft.id("action"));

    public static final StreamCodec<FriendlyByteBuf, ActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.action.ordinal());
                buf.writeVarInt(payload.a);
                buf.writeVarInt(payload.b);
            },
            buf -> {
                int action = buf.readVarInt();
                Action[] values = Action.values();
                return new ActionPayload(values[Math.max(0, Math.min(values.length - 1, action))], buf.readVarInt(), buf.readVarInt());
            });

    public ActionPayload(Action action) {
        this(action, 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        /** Started holding the charge key. */
        CHARGE_START,
        CHARGE_STOP,
        /** Started holding the blast key. */
        BLAST_START,
        /** Let go of the blast key: fire. */
        BLAST_RELEASE,
        /** Go up one form. */
        TRANSFORM,
        /** Back to base form. */
        DESCEND,
        /** Create the character: a = race index, b = class index. */
        CREATE,
        /** Buy stat points: a = stat index, b = how many. */
        RAISE_STAT,
        /** Buy one level of the transformation skill. */
        RAISE_FORM_SKILL
    }
}
