# MM #11 — GUI: Экран-досье (ClassDetailScreen + ClassInfoScreen)

## Роль

Java-разработчик. Реализуешь два GUI экрана: детальный обзор класса и информация о текущем классе.

## Контекст

Посмотри проект

## Задача

Реализуй два экрана:
1. `gui/ClassDetailScreen.java` — досье класса (при выборе)
2. `gui/ClassInfoScreen.java` — информация о текущем классе (по клавише J в игре)

---

## ClassDetailScreen — Экран-досье

### Дизайн

Скроллируемая страница. Секции сверху вниз:

**1. Шапка**
- Иконка класса (крупная, 48×48)
- Название (цвет класса, крупный шрифт через `poseStack.scale(1.5f)`)
- Курсивная цитата (lore)

**2. Характеристики**
- Заголовок секции: `"── ХАРАКТЕРИСТИКИ ──"` серой линией
- Для каждого атрибута с модификатором ≠ 0: визуальная полоска
- Средний уровень (ваниль) = середина полоски
- Выше нормы: полоска длиннее, зелёный цвет
- Ниже нормы: полоска короче, красный цвет
- Справа: числовое значение и разница: `"+1.0"` или `"−4"`
- Отображаемые атрибуты: HP, Attack Damage, Movement Speed, Armor, Armor Toughness, Attack Speed, Knockback Resistance + кастомные

**3. Активные способности**
- Заголовок: `"── СПОСОБНОСТИ ──"`
- Для каждой способности:
  ```
  [R]  Кровавый укус                             КД: 5с
       Описание способности серым текстом.
  ```
- Слева: клавиша в `[квадратных скобках]` (из текущих keybinds)
- Справа: кулдаун в секундах (`cooldownTicks / 20`)
- Если `unlockLevel > 1`: плашка `"Ур. N"` рядом с кулдауном
- Тултип при наведении: точные числа из params (урон, радиус, длительность, стоимость)

**4. Пассивные особенности**
- Заголовок: `"── ПАССИВНЫЕ ──"`
- Бонусы (positive=true): `"✔"` зелёный маркер + название + тире + описание
- Штрафы (positive=false): `"✘"` красный маркер
- Порядок: сначала бонусы, потом штрафы

**5. Ресурс** (если есть)
- Заголовок: `"── РЕСУРС ──"`
- Полоска с цветом ресурса, подпись макс. значения
- Описание в 1–2 строки: как тратится, как восполняется

**6. Кнопка «Выбрать класс»**
- Крупная, по центру, внизу контента
- При клике:
  - Первый выбор (mode=0): отправить `ClassSelectPacket(classId, false)` сразу
  - Перерождение (mode=1): показать подтверждение inline — текст + кнопки Да/Нет

**7. Кнопка «Назад»**
- Левый верхний угол, вне скролла
- → `setScreen(new ClassSelectionScreen(mode))`

### Скролл

- `scrollOffset` (float) — текущее смещение в пикселях
- Колёсико мыши: `scrollOffset += delta * scrollSpeed`
- Плавная инерция: `scrollVelocity`, уменьшается каждый кадр
- Clamp: `0 ≤ scrollOffset ≤ contentHeight - viewHeight`
- Рисовать через `guiGraphics.enableScissor(left, top, right, bottom)` для clip

### Анимации

- Секции появляются с fade-in при скролле (alpha от 0 до 1 когда входят в видимую область)
- Полоски характеристик «наливаются» от 0 до целевого значения при первом отображении

### Подтверждение перерождения

Overlay поверх текущего экрана:
```
    Ты потеряешь все способности Вампира.
    Продолжить?
    
    [Да]          [Нет]
```
- Фон затемняется (`fill(0, 0, width, height, 0x80000000)`)
- «Да» → отправить `ClassSelectPacket(classId, viaItem)`
- «Нет» → скрыть overlay

### Структура

```java
public class ClassDetailScreen extends Screen {
    private final PlayerClass playerClass;
    private final int mode;
    private final boolean viaItem;
    
    private float scrollOffset = 0;
    private float scrollVelocity = 0;
    private float contentHeight = 0;
    private boolean showConfirmation = false;
    
    // Анимации
    private float barAnimProgress = 0; // 0→1 для полосок характеристик
    private long openTime;
    
    @Override protected void init();
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta);
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button);
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers);
    
    private int renderHeader(GuiGraphics g, int y);          // → return новый y после секции
    private int renderAttributes(GuiGraphics g, int y);
    private int renderActiveAbilities(GuiGraphics g, int y);
    private int renderPassives(GuiGraphics g, int y);
    private int renderResource(GuiGraphics g, int y);
    private int renderSelectButton(GuiGraphics g, int y);
    private void renderConfirmation(GuiGraphics g);
    private void renderAttributeBar(GuiGraphics g, String name, double value, double base, int x, int y, int width);
}
```

---

## ClassInfoScreen — Информация в игре

Открывается по клавише J. Та же информация, что ClassDetailScreen, но вместо кнопки «Выбрать»:

- **Текущий уровень**: `"Уровень 7"` + полоска опыта
- **Полоска опыта**: `current / needForNext` XP, заполненность в %
- **Текущий ресурс**: полоска с реальным значением из `ClientClassData`

Кнопка «Выбрать» отсутствует. Кнопка «Назад» → `onClose()` (ESC тоже).

### Структура

```java
public class ClassInfoScreen extends Screen {
    // Получает данные из ClientClassData.getInstance()
    // Рендер аналогичен ClassDetailScreen, но с дополнительными секциями
    // Переиспользовать рендер-методы через общий базовый класс или утилиту
}
```

**Совет:** вынеси общие методы рендеринга (полоски, секции, текст) в `gui/ClassScreenRenderer.java` — утилитный класс со static-методами.

## Формат вывода

1. `gui/ClassScreenRenderer.java` — утилиты рендеринга
2. `gui/ClassDetailScreen.java`
3. `gui/ClassInfoScreen.java`
