# MM #10 — GUI: Экран-витрина (ClassSelectionScreen)

## Роль

Java-разработчик. Реализуешь GUI экран выбора класса для Minecraft мода. Используешь ванильные средства: `Screen`, `GuiGraphics`, `Widget`.

## Контекст

Посмотри проект

## Задача

Реализуй `gui/ClassSelectionScreen.java` — экран-витрина со всеми классами.

## Дизайн

```
╔══════════════════════════════════════════════════════════════╗
║                    ВЫБЕРИ СВОЙ ПУТЬ                          ║
║                                                              ║
║   ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐           ║
║   │ icon │  │ icon │  │ icon │  │ icon │  │ icon │           ║
║   │Вампир│  │Голем │  │Фантом│  │Аквам.│  │Берсерк│          ║
║   └──────┘  └──────┘  └──────┘  └──────┘  └──────┘           ║
║                          И т.д                               ║
║              ┌──────────────────────────────┐                ║
║              │  Lore-текст при наведении    │                ║
║              └──────────────────────────────┘                ║
║                                                              ║
║                              [Случайный]                     ║
╚══════════════════════════════════════════════════════════════╝
```

## Режимы

- `mode = 0` (первый выбор): заголовок «ВЫБЕРИ СВОЙ ПУТЬ», ESC не закрывает экран
- `mode = 1` (перерождение): заголовок «ПЕРЕРОЖДЕНИЕ», текущий класс с золотой рамкой и меткой «Текущий», ESC закрывает

## Требования

### Сетка карточек

Автоматическая адаптация:

| Классов | Сетка | Размер карточки | Скролл |
|---------|-------|----------------|--------|
| 3–6 | 3×2 | 64×64 | Нет |
| 7–15 | 5×3 | 48×48 | Нет |
| 16–24 | 6×4 | 40×40 | Нет |
| 25+ | 6×4 + скролл | 40×40 | Да, вертикальный |

Расчёт: `cols = Math.min(6, Math.max(3, (int)Math.ceil(Math.sqrt(classCount * 1.5))))`. Или hardcoded таблица.

### Карточка класса

Рисуется кастомно через `GuiGraphics`:
1. **Фон**: `guiGraphics.fill(x, y, x+size, y+size, bgColor)` — полупрозрачный чёрный
2. **Рамка**: `guiGraphics.renderOutline(x, y, size, size, classColor)` — цвет из JSON
3. **Иконка**: `guiGraphics.blit(icon, x+offset, y+offset, iconSize, iconSize)` — текстура из JSON
4. **Название**: `guiGraphics.drawCenteredString(font, name, centerX, y+size+2, classColor)`
5. **Значок роли** (угол): маленькая иконка 8×8 — мечи/щит/перо/глаз по category

### Наведение

При `isMouseOver(mouseX, mouseY)` на карточке:
- Карточка увеличивается: рисовать с `scale * 1.1` через `poseStack.scale()`. Анимация: интерполяция `currentScale → targetScale` с `lerp(current, target, 0.3f)`
- Внизу экрана: Lore-блок:
  - Курсивная цитата: `Component.translatable(loreKey).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)`
  - Механика одной строкой
  - Три маркера силы: полоски заполнения по category (damage/tank/mobility → ⚔️🛡️💨)

### Фильтры (если > 20 классов)

Кнопки сверху: `[Все] [⚔️] [🛡️] [💨] [🔮]`
- Реализация: `List<ClassCategory> activeFilters`
- При клике по фильтру: toggle в activeFilters, пересчитать видимые классы
- Анимация: карточки плавно исчезают/появляются (alpha transition)

### Клик по карточке

ЛКМ → `Minecraft.getInstance().setScreen(new ClassDetailScreen(playerClass, mode))` — переход к досье.

### Анимация перехода

1. Запомнить позицию кликнутой карточки
2. Анимация «раскрытия»: карточка масштабируется на весь экран за 300мс
3. Остальные карточки уходят в alpha=0
4. По завершении: открыть `ClassDetailScreen`

Упрощённая реализация без анимации: просто `setScreen()`. Анимация — бонус.

### Кнопка «Случайный»

Внизу, по центру. `Button.builder(...)`. При нажатии: `Random.nextInt(classes.size())`, открыть досье этого класса.

### Клавиатурная навигация

- Стрелки: перемещение по сетке, `selectedIndex` ← ↑ → ↓
- Enter: эквивалент ЛКМ на `selectedIndex`
- ESC: `onClose()` (только если mode == 1)
- Tab: переключение фильтров

### Звуки

- Наведение: `player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.1f, 1.5f)` — тихо, высокий pitch
- Клик: кастомный звук или `SoundEvents.BOOK_PAGE_TURN`

## Структура класса

```java
public class ClassSelectionScreen extends Screen {
    private final int mode; // 0=first, 1=rebirth
    private List<PlayerClass> allClasses;
    private List<PlayerClass> filteredClasses;
    private Set<ClassCategory> activeFilters;
    private int hoveredIndex = -1;
    private int selectedIndex = -1; // keyboard nav
    private float[] cardScales; // анимация увеличения при наведении
    private int scrollOffset = 0;
    private @Nullable PlayerClass hoveredClass; // для lore display
    
    // Layout
    private int gridCols, gridRows, cardSize, gridStartX, gridStartY, cardSpacing;
    
    @Override protected void init(); // рассчитать layout, создать кнопки
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button);
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers);
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta);
    @Override public void onClose(); // только если mode==1
    
    private void renderCard(GuiGraphics g, PlayerClass cls, int x, int y, int size, float scale, boolean hovered, boolean current);
    private void renderLoreBlock(GuiGraphics g, PlayerClass cls);
    private void renderFilters(GuiGraphics g, int mouseX, int mouseY);
    private void recalculateLayout();
    private int getCardIndexAt(double mouseX, double mouseY);
}
```

## Формат вывода

Один файл `ClassSelectionScreen.java`, полностью.

## Чего НЕ делать

- Не используй `AbstractContainerScreen` — это не инвентарь, а чистый `Screen`
- Не создавай отдельные Widget для карточек — рисуй кастомно для контроля анимаций
- Не хардкодь размеры — всё должно масштабироваться от `this.width` и `this.height`
