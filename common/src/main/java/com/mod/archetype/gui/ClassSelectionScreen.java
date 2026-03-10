package com.mod.archetype.gui;

import com.mod.archetype.core.ClassCategory;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ClassSelectionScreen extends Screen {

    private final int mode; // 0=first, 1=rebirth
    private List<PlayerClass> allClasses;
    private List<PlayerClass> filteredClasses;
    private final Set<ClassCategory> activeFilters = new HashSet<>();
    private int hoveredIndex = -1;
    private int selectedIndex = -1;
    private float[] cardScales;
    private int scrollOffset = 0;
    @Nullable
    private PlayerClass hoveredClass;

    private int gridCols, gridRows, cardSize, gridStartX, gridStartY, cardSpacing;

    public ClassSelectionScreen(int mode) {
        super(Component.translatable(mode == 0 ? "gui.archetype.selection.title" : "gui.archetype.rebirth.title"));
        this.mode = mode;
    }

    @Override
    protected void init() {
        allClasses = new ArrayList<>(ClassRegistry.getInstance().getAll());
        allClasses.sort(Comparator.comparing(c -> c.getId().toString()));
        filteredClasses = new ArrayList<>(allClasses);
        recalculateLayout();
        cardScales = new float[filteredClasses.size()];
        Arrays.fill(cardScales, 1.0f);

        // Random button
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.archetype.random"),
                        btn -> {
                            if (!filteredClasses.isEmpty()) {
                                PlayerClass randomClass = filteredClasses.get(new Random().nextInt(filteredClasses.size()));
                                Minecraft.getInstance().setScreen(new ClassDetailScreen(randomClass, mode));
                            }
                        })
                .bounds(width / 2 - 50, height - 30, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Title
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);

        // Cards
        hoveredIndex = -1;
        hoveredClass = null;
        for (int i = 0; i < filteredClasses.size(); i++) {
            int row = i / gridCols;
            int col = i % gridCols;
            int x = gridStartX + col * (cardSize + cardSpacing);
            int y = gridStartY + row * (cardSize + cardSpacing + 12) - scrollOffset;

            if (y + cardSize < 25 || y > height - 50) continue;

            PlayerClass cls = filteredClasses.get(i);
            boolean hovered = mouseX >= x && mouseX <= x + cardSize && mouseY >= y && mouseY <= y + cardSize;
            if (hovered) {
                hoveredIndex = i;
                hoveredClass = cls;
            }

            // Animate scale
            float targetScale = hovered ? 1.1f : 1.0f;
            if (i < cardScales.length) {
                cardScales[i] = Mth.lerp(0.3f, cardScales[i], targetScale);
            }

            boolean isCurrent = mode == 1 && ClientClassData.getInstance().hasClass()
                    && cls.getId().equals(ClientClassData.getInstance().getClassId());
            renderCard(graphics, cls, x, y, cardSize, i < cardScales.length ? cardScales[i] : 1.0f, hovered, isCurrent);
        }

        // Lore block
        if (hoveredClass != null) {
            renderLoreBlock(graphics, hoveredClass);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCard(GuiGraphics g, PlayerClass cls, int x, int y, int size, float scale, boolean hovered, boolean isCurrent) {
        var pose = g.pose();
        if (scale != 1.0f) {
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            pose.pushPose();
            pose.translate(cx, cy, 0);
            pose.scale(scale, scale, 1);
            pose.translate(-cx, -cy, 0);
        }

        // Background
        int bgColor = hovered ? 0xC0333333 : 0x80000000;
        g.fill(x, y, x + size, y + size, bgColor);

        // Border
        int borderColor = isCurrent ? 0xFFFFD700 : (0xFF000000 | cls.getColor());
        g.renderOutline(x, y, size, size, borderColor);

        // Name below card
        Component name = Component.translatable(cls.getNameKey());
        g.drawCenteredString(font, name, x + size / 2, y + size + 2, 0xFF000000 | cls.getColor());

        if (scale != 1.0f) {
            pose.popPose();
        }
    }

    private void renderLoreBlock(GuiGraphics g, PlayerClass cls) {
        int loreY = height - 55;
        List<String> loreKeys = cls.getLoreKeys();
        if (!loreKeys.isEmpty()) {
            Component lore = Component.translatable(loreKeys.get(0)).withStyle(s -> s.withItalic(true).withColor(0xAAAAAA));
            g.drawCenteredString(font, lore, width / 2, loreY, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < filteredClasses.size()) {
            Minecraft.getInstance().setScreen(new ClassDetailScreen(filteredClasses.get(hoveredIndex), mode));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && mode == 0) {
            return true; // ESC blocked on first selection
        }
        // Arrow key navigation
        if (keyCode == 265) { // UP
            selectedIndex = Math.max(0, selectedIndex - gridCols);
            return true;
        }
        if (keyCode == 264) { // DOWN
            selectedIndex = Math.min(filteredClasses.size() - 1, selectedIndex + gridCols);
            return true;
        }
        if (keyCode == 263) { // LEFT
            selectedIndex = Math.max(0, selectedIndex - 1);
            return true;
        }
        if (keyCode == 262) { // RIGHT
            selectedIndex = Math.min(filteredClasses.size() - 1, selectedIndex + 1);
            return true;
        }
        if (keyCode == 257 && selectedIndex >= 0 && selectedIndex < filteredClasses.size()) { // ENTER
            Minecraft.getInstance().setScreen(new ClassDetailScreen(filteredClasses.get(selectedIndex), mode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= (int) (delta * 20);
        scrollOffset = Math.max(0, scrollOffset);
        return true;
    }

    @Override
    public void onClose() {
        if (mode == 1) {
            super.onClose();
        }
    }

    private void recalculateLayout() {
        int count = filteredClasses.size();
        if (count <= 6) {
            gridCols = 3;
            cardSize = 64;
        } else if (count <= 15) {
            gridCols = 5;
            cardSize = 48;
        } else if (count <= 24) {
            gridCols = 6;
            cardSize = 40;
        } else {
            gridCols = 6;
            cardSize = 40;
        }
        gridRows = (int) Math.ceil((double) count / gridCols);
        cardSpacing = 8;
        int gridWidth = gridCols * (cardSize + cardSpacing) - cardSpacing;
        gridStartX = (width - gridWidth) / 2;
        gridStartY = 30;
    }
}
