/*
 * Z-Craft — GPL-3.0-or-later.
 */
package dev.draxer.zcraft.client;

import dev.draxer.zcraft.data.CharacterClass;
import dev.draxer.zcraft.data.Form;
import dev.draxer.zcraft.data.Formulas;
import dev.draxer.zcraft.data.PlayerData;
import dev.draxer.zcraft.data.Race;
import dev.draxer.zcraft.data.Stat;
import dev.draxer.zcraft.network.ActionPayload.Action;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The character menu. Before a character exists it is the creation screen (pick a race and a
 * class); afterwards it shows the stats and lets you spend training points.
 */
public final class CharacterScreen extends Screen {
    private record Button(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final int GOLD = 0xFFFFD23F;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int DIM = 0xFF9AA3AB;

    /** Rebuilt on every frame, so clicks always match what is drawn. */
    private final List<Button> buttons = new ArrayList<>();
    private int race;
    private int characterClass;

    public CharacterScreen() {
        super(Component.translatable("gui.zcraft.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Button button : List.copyOf(buttons)) {
            if (button.contains(event.x(), event.y())) {
                button.action.run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void button(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Component label, boolean selected,
                        int mouseX, int mouseY, Runnable action) {
        Button button = new Button(x, y, w, h, action);
        buttons.add(button);
        boolean hover = button.contains(mouseX, mouseY);
        graphics.fill(x, y, x + w, y + h, selected ? GOLD : hover ? 0xFFBFC7CE : 0xFF4A5056);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, selected ? 0xFF3A3220 : hover ? 0xFF30363C : 0xFF1C2024);
        graphics.centeredText(this.font, label, x + w / 2, y + (h - 8) / 2, selected ? GOLD : TEXT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        buttons.clear();
        graphics.fill(0, 0, this.width, this.height, 0xD0101418);
        graphics.centeredText(this.font, this.title, this.width / 2, 10, GOLD);
        if (ZClient.data.created) sheet(graphics, mouseX, mouseY);
        else creation(graphics, mouseX, mouseY);
    }

    // ------------------------------------------------------------ creation

    private void creation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int w = 84;
        int h = 18;
        int gap = 4;

        graphics.centeredText(this.font, Component.translatable("gui.zcraft.pick_race"), cx, 30, DIM);
        int perRow = 3;
        int left = cx - (perRow * w + (perRow - 1) * gap) / 2;
        for (int i = 0; i < Race.VALUES.length; i++) {
            int index = i;
            button(graphics, left + (i % perRow) * (w + gap), 42 + (i / perRow) * (h + gap), w, h,
                    Component.translatable(Race.VALUES[i].key()), race == i, mouseX, mouseY, () -> race = index);
        }

        int classTop = 42 + 2 * (h + gap) + 14;
        graphics.centeredText(this.font, Component.translatable("gui.zcraft.pick_class"), cx, classTop - 12, DIM);
        perRow = 4;
        left = cx - (perRow * w + (perRow - 1) * gap) / 2;
        for (int i = 0; i < CharacterClass.VALUES.length; i++) {
            int index = i;
            button(graphics, left + (i % perRow) * (w + gap), classTop + (i / perRow) * (h + gap), w, h,
                    Component.translatable(CharacterClass.VALUES[i].key()), characterClass == i, mouseX, mouseY, () -> characterClass = index);
        }

        // What the chosen class starts with.
        CharacterClass chosen = CharacterClass.byIndex(characterClass);
        int y = classTop + 2 * (h + gap) + 8;
        StringBuilder line = new StringBuilder();
        for (Stat stat : Stat.VALUES) {
            if (line.length() > 0) line.append("   ");
            line.append(stat.name()).append(' ').append(chosen.base(stat));
        }
        graphics.centeredText(this.font, Component.literal(line.toString()), cx, y, TEXT);
        graphics.centeredText(this.font, Component.translatable(chosen.key() + ".desc"), cx, y + 12, DIM);

        Race chosenRace = Race.byIndex(race);
        List<Form> line2 = Form.line(chosenRace);
        if (!line2.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.zcraft.first_form", Component.translatable(line2.get(0).key())),
                    cx, y + 24, 0xFF000000 | chosenRace.color);
        }

        button(graphics, cx - 60, y + 42, 120, 20, Component.translatable("gui.zcraft.create"), false, mouseX, mouseY, () -> {
            ZClient.send(Action.CREATE, race, characterClass);
            ZClient.data.created = true; // shows the sheet right away; the server's answer replaces this copy
            ZClient.data.race = Race.byIndex(race);
            ZClient.data.characterClass = CharacterClass.byIndex(characterClass);
        });
    }

    // ------------------------------------------------------------ character sheet

    private void sheet(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PlayerData data = ZClient.data;
        int left = this.width / 2 - 150;
        int right = this.width / 2 + 10;
        int y = 30;

        Form form = data.activeForm();
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.who",
                Component.translatable(data.race.key()), Component.translatable(data.characterClass.key())), left, y, 0xFF000000 | data.race.color);
        graphics.text(this.font, Component.translatable("gui.zcraft.hud.tp", (long) data.tp), right, y, GOLD);
        y += 16;

        // Stats, each with +1 and +10.
        int total = data.totalStats();
        for (Stat stat : Stat.VALUES) {
            graphics.text(this.font, Component.translatable(stat.key()), left, y + 4, TEXT);
            String value = String.valueOf(data.stat(stat));
            graphics.text(this.font, value, left + 96 - this.font.width(value), y + 4, GOLD);
            int index = stat.ordinal();
            button(graphics, left + 102, y, 20, 16, Component.literal("+1"), false, mouseX, mouseY, () -> ZClient.send(Action.RAISE_STAT, index, 1));
            button(graphics, left + 124, y, 24, 16, Component.literal("+10"), false, mouseX, mouseY, () -> ZClient.send(Action.RAISE_STAT, index, 10));
            y += 19;
        }
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.cost", Formulas.statCost(data, total), Formulas.statCost(data, total, 10)), left, y + 2, DIM);

        // What those stats are worth.
        int ry = 46;
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.ki", Math.round(data.energy), Math.round(Formulas.maxEnergy(data))), right, ry, TEXT);
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.melee", one(Formulas.meleeBonus(data))), right, ry + 12, TEXT);
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.ki_damage", one(Formulas.kiDamage(data))), right, ry + 24, TEXT);
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.defense", one(Formulas.defense(data, 0, 0) * Formulas.DAMAGE_SCALE)), right, ry + 36, TEXT);
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.health", one(Formulas.healthBonus(data))), right, ry + 48, TEXT);

        // Transformations.
        ry += 70;
        List<Form> line = Form.line(data.race);
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.forms", data.formSkill, line.size()), right, ry, GOLD);
        graphics.text(this.font, Component.translatable("gui.zcraft.sheet.current",
                form != null ? Component.translatable(form.key()) : Component.translatable("gui.zcraft.sheet.base")), right, ry + 12, TEXT);
        if (data.formSkill < line.size()) {
            Form next = line.get(data.formSkill);
            graphics.text(this.font, Component.translatable("gui.zcraft.sheet.next", Component.translatable(next.key())), right, ry + 24, 0xFF000000 | next.color());
            button(graphics, right, ry + 38, 140, 18, Component.translatable("gui.zcraft.sheet.unlock", Formulas.formSkillCost(data.formSkill)),
                    false, mouseX, mouseY, () -> ZClient.send(Action.RAISE_FORM_SKILL, 0, 0));
        } else {
            graphics.text(this.font, Component.translatable("gui.zcraft.sheet.all_forms"), right, ry + 24, DIM);
        }

        graphics.centeredText(this.font, Component.translatable("gui.zcraft.sheet.keys",
                ZClient.CHARGE.getTranslatedKeyMessage(), ZClient.BLAST.getTranslatedKeyMessage(),
                ZClient.TRANSFORM.getTranslatedKeyMessage(), ZClient.DESCEND.getTranslatedKeyMessage()), this.width / 2, this.height - 18, DIM);
    }

    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
