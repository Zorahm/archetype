# MM #12 — HUD-оверлей способностей и ресурса

## Роль

Java-разработчик. Реализуешь HUD-оверлей, отображающий способности и ресурс поверх игрового экрана.

## Контекст

Посмотри проект

## Задача

Реализуй `gui/AbilityHudOverlay.java` — HUD, рисуемый каждый кадр поверх игры.

## Дизайн

```
[R]🩸  [V]🦇  [G]🔒       🩸 ████████░░ 73/100
```

Справа от хотбара (по умолчанию). Позиция настраивается.

## Элементы

### Слоты способностей (3 штуки)

Каждый слот:
- **Фон**: квадрат 20×20, полупрозрачный чёрный `0x80000000`
- **Иконка способности**: 16×16, по центру квадрата
- **Клавиша**: мелким шрифтом сверху-слева (8px), `"R"` / `"V"` / `"G"` — из KeyMapping.getKey().getDisplayName()
- **Кулдаун**: если на кулдауне:
  - Иконка затемняется сверху вниз (overlay с alpha). Высота overlay = `cooldownProgress * slotHeight`
  - По центру иконки: оставшиеся секунды (`remaining / 20`, округление вверх), белый текст с тенью
- **Заблокирована**: если `unlockLevel > playerLevel`:
  - Иконка замка вместо иконки способности
  - Подпись `"Ур.N"` серым

### Полоска ресурса (если есть)

Справа от слотов, с отступом 8px:
- Иконка ресурса (8×8)
- Полоска: ширина 60px, высота 8px
  - Фон: `0x40000000`
  - Заполнение: цвет из `ResourceDefinition.color`
  - Ширина заполнения: `(current / max) * barWidth`
- Текст: `"73/100"` мелким шрифтом справа от полоски

### Позиции HUD

Из конфига `abilityBarPosition`:
- `hotbar_right` (default): справа от хотбара, вертикально выровнено по центру хотбара
- `hotbar_left`: слева от хотбара
- `top_center`: верх экрана, по центру

### Масштаб

Из конфига `hudScale` (float, 0.5–2.0, default 1.0):
```java
poseStack.pushPose();
poseStack.scale(hudScale, hudScale, 1.0f);
// рисовать в нативных координатах
// mouseX/Y нужно делить на hudScale
poseStack.popPose();
```

## Анимации

- **Использование способности**: flash-эффект на слоте (белая рамка, fade за 200мс). Трекать через `lastUsedTick[slot]` из ClientClassData
- **Ресурс < 20%**: полоска мигает (alpha пульсирует)
- **Кулдаун заканчивается**: краткая подсветка слота

## Реализация

```java
public class AbilityHudOverlay {
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 4;
    private static final int RESOURCE_BAR_WIDTH = 60;
    private static final int RESOURCE_BAR_HEIGHT = 8;
    
    /**
     * Вызывается каждый кадр из EventTranslator (Forge: RenderGuiEvent.Post, Fabric: HudRenderCallback).
     * @param graphics GuiGraphics
     * @param partialTick partial tick для анимаций
     */
    public static void render(GuiGraphics graphics, float partialTick) {
        ClientClassData data = ClientClassData.getInstance();
        if (!data.hasClass()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return; // не рисовать в меню
        
        // Получить позицию из конфига
        // Рассчитать startX, startY
        // Рисовать слоты
        // Рисовать ресурс
    }
    
    private static void renderAbilitySlot(GuiGraphics g, int x, int y, int slotIndex, ClientClassData data, float partialTick);
    private static void renderResourceBar(GuiGraphics g, int x, int y, ClientClassData data, float partialTick);
    private static void renderCooldownOverlay(GuiGraphics g, int x, int y, int size, float progress);
}
```

### Не рисовать когда

- `mc.options.hideGui` (F1)
- `mc.screen != null` (открыт любой GUI)
- Нет класса
- Игрок мёртв / spectator

## Формат вывода

Один файл `gui/AbilityHudOverlay.java`, полностью.
