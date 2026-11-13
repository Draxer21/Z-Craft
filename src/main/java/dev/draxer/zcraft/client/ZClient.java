/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.draxer.zcraft.ZCraft;
import dev.draxer.zcraft.data.Form;
import dev.draxer.zcraft.data.Formulas;
import dev.draxer.zcraft.data.PlayerData;
import dev.draxer.zcraft.network.ActionPayload;
import dev.draxer.zcraft.network.ActionPayload.Action;
import dev.draxer.zcraft.network.SyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client side: keys, the ki HUD and the copy of the character the server sends us. */
@EventBusSubscriber(modid = ZCraft.MOD_ID, value = Dist.CLIENT)
public final class ZClient {
    private ZClient() {}

    /** Our own character as the server last described it. */
    public static PlayerData data = new PlayerData();

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ZCraft.id("main"));
    public static final KeyMapping MENU = key("menu", InputConstants.KEY_V);
    public static final KeyMapping CHARGE = key("charge", InputConstants.KEY_C);
    public static final KeyMapping BLAST = key("blast", InputConstants.KEY_R);
    public static final KeyMapping TRANSFORM = key("transform", InputConstants.KEY_G);
    public static final KeyMapping DESCEND = key("descend", InputConstants.KEY_H);

    private static boolean wasCharging;
    private static boolean wasBlasting;

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.zcraft." + name, code, CATEGORY);
    }

    /** The one place that opens screens (this call is what differs between Minecraft versions). */
    public static void open(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static void send(Action action, int a, int b) {
        ClientPacketDistributor.sendToServer(new ActionPayload(action, a, b));
    }

    // ------------------------------------------------------------ registration

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(MENU);
        event.register(CHARGE);
        event.register(BLAST);
        event.register(TRANSFORM);
        event.register(DESCEND);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(SyncPayload.TYPE, (payload, context) -> data = payload.data());
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ZCraft.id("ki_bar"), ZClient::renderHud);
    }

    // ------------------------------------------------------------ keys

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            wasCharging = wasBlasting = false;
            data = new PlayerData();
            return;
        }
        while (MENU.consumeClick()) open(new CharacterScreen());

        // Held keys: only tell the server when they change. Keys read as released while a screen is open.
        boolean charging = CHARGE.isDown();
        if (charging != wasCharging) send(charging ? Action.CHARGE_START : Action.CHARGE_STOP, 0, 0);
        wasCharging = charging;

        boolean blasting = BLAST.isDown();
        if (blasting != wasBlasting) send(blasting ? Action.BLAST_START : Action.BLAST_RELEASE, 0, 0);
        wasBlasting = blasting;

        while (TRANSFORM.consumeClick()) send(Action.TRANSFORM, 0, 0);
        while (DESCEND.consumeClick()) send(Action.DESCEND, 0, 0);
    }

    // ------------------------------------------------------------ HUD

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int x = 8;
        int y = 8;
        if (!data.created) {
            graphics.text(mc.font, Component.translatable("gui.zcraft.hud.create", MENU.getTranslatedKeyMessage()), x, y, 0xFFE8E8E8);
            return;
        }
        Form form = data.activeForm();
        Component title = form != null ? Component.translatable(form.key()) : Component.translatable(data.race.key());
        int color = 0xFF000000 | data.color();
        graphics.text(mc.font, title, x, y, color);

        int w = 110;
        int h = 6;
        int barY = y + 11;
        float max = Formulas.maxEnergy(data);
        int fill = max <= 0 ? 0 : Math.round(w * Math.min(1f, data.energy / max));
        graphics.fill(x - 1, barY - 1, x + w + 1, barY + h + 1, 0xC0000000);
        graphics.fill(x, barY, x + fill, barY + h, color);
        if (data.blastCharge > 0) {
            int charge = Math.round(w * Math.min(1f, (float) data.blastCharge / Formulas.BLAST_MAX_CHARGE));
            graphics.fill(x, barY + h + 2, x + charge, barY + h + 4, 0xFFFFFFFF);
        }
        graphics.text(mc.font, Math.round(data.energy) + " / " + Math.round(max), x + w + 5, barY - 1, 0xFFFFFFFF);
        graphics.text(mc.font, Component.translatable("gui.zcraft.hud.tp", (long) data.tp), x, barY + h + 7, 0xFFBFC7CE);
    }
}
