/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft;

import dev.draxer.zcraft.data.PlayerData;
import dev.draxer.zcraft.network.ActionPayload;
import dev.draxer.zcraft.network.SyncPayload;
import dev.draxer.zcraft.server.ZServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Z-Craft: races, stats, ki and transformations. */
@Mod(ZCraft.MOD_ID)
public final class ZCraft {
    public static final String MOD_ID = "zcraft";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    /** The player's character: saved with the world and kept after death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerData>> DATA = ATTACHMENTS.register("character",
            () -> AttachmentType.builder(() -> new PlayerData()).serialize(PlayerData.CODEC).copyOnDeath().build());

    public ZCraft(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(ZCraft::registerPayloads);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static PlayerData data(Player player) {
        return player.getData(DATA);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC, ZServer::handleAction)
                .playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC);
    }
}
