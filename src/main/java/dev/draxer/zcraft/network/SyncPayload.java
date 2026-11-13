/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.network;

import dev.draxer.zcraft.ZCraft;
import dev.draxer.zcraft.data.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: the player's own character, for the HUD and the menu. */
public record SyncPayload(PlayerData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPayload> TYPE = new CustomPacketPayload.Type<>(ZCraft.id("sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.data.write(buf),
            buf -> new SyncPayload(PlayerData.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
