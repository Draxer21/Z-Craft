/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.draxer.zcraft.ZCraft;
import dev.draxer.zcraft.data.PlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /zcraft tp <amount>  gives training points;
 * /zcraft reset        deletes the character so it can be created again.
 * Both only work in creative mode.
 */
@EventBusSubscriber(modid = ZCraft.MOD_ID)
public final class ZCommands {
    private ZCommands() {}

    private static ServerPlayer creativePlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player && player.isCreative()) return player;
        source.sendFailure(Component.translatable("message.zcraft.command.creative"));
        return null;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("zcraft")
                .then(Commands.literal("tp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 10_000_000)).executes(context -> {
                            ServerPlayer player = creativePlayer(context.getSource());
                            if (player == null) return 0;
                            PlayerData data = ZCraft.data(player);
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            data.tp += amount;
                            ZServer.sync(player);
                            context.getSource().sendSuccess(() -> Component.translatable("message.zcraft.tp_gained", amount), false);
                            return amount;
                        })))
                .then(Commands.literal("reset").executes(context -> {
                    ServerPlayer player = creativePlayer(context.getSource());
                    if (player == null) return 0;
                    ZCraft.data(player).reset();
                    ZServer.refresh(player);
                    context.getSource().sendSuccess(() -> Component.translatable("message.zcraft.reset"), false);
                    return 1;
                })));
    }
}
