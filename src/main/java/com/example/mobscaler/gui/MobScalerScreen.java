package com.example.mobscaler.gui;

import com.example.mobscaler.config.IndividualMobAttributes;
import com.example.mobscaler.config.IndividualMobConfig;
import com.example.mobscaler.config.IndividualMobManager;
import com.example.mobscaler.config.DimensionConfig;
import com.example.mobscaler.config.DimensionConfigManager;
import com.example.mobscaler.config.MobScalerConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import com.example.mobscaler.config.IndividualMobConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Method;
import java.util.Locale;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.PlayerConfig;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.ChatFormatting;


public class MobScalerScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MobScalerScreen.class);
    
    // Вспомогательный метод для получения локализованных строк
    private Component getTranslatedText(String key) {
        return Component.translatable(key);
    }
    
    // Вспомогательный метод для получения локализованных строк с параметрами
    private Component getTranslatedText(String key, Object... args) {
        return Component.translatable(key, args);
    }
    
    // Цвета интерфейса
    private static final int BACKGROUND_COLOR = 0x90000000; // Полупрозрачный черный цвет
    private static final int HEADER_COLOR = 0x9000BFFF;    // Полупрозрачный голубой цвет для заголовков
    private static final int TEXT_COLOR = 0xFFFFFFFF;      // Белый цвет для текста
    private static final int HIGHLIGHT_COLOR = 0x5000FF00; // Полупрозрачный зеленый для выделения
    private static final int BUTTON_COLOR = 0xFF4169E1;    // Королевский синий для кнопок
    private static final int BUTTON_HOVER_COLOR = 0xFF6495ED; // Светло-синий при наведении
    private static final int DELETE_BUTTON_COLOR = 0xFFDC143C; // Красный для кнопок удаления
    private static final int ADD_BUTTON_COLOR = 0xFF32CD32; // Зеленый для кнопок добавления
    private static final int CHECKBOX_CHECKED_COLOR = 0xFF00FF00; // Зеленый для отмеченных чекбоксов
    private static final int CHECKBOX_UNCHECKED_COLOR = 0xFFAAAAAA; // Серый для неотмеченных чекбоксов
    
    // Размеры элементов интерфейса
    private static final int FIELD_WIDTH = 35;     // Ширина полей ввода
    private static final int SPACING = 10;         // Отступ между элементами
    
    // Класс для сопоставления полей ввода с полями конфигурации
    private static class FieldMapping {
        private final String fieldName;

        private EditBox editBox;
        
        public FieldMapping(String fieldName, String displayName) {
            this.fieldName = fieldName;
        }
        
        public void setEditBox(EditBox editBox) {
            this.editBox = editBox;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        
        public EditBox getEditBox() {
            return editBox;
        }
        
        public double getValue() {
            try {
                String value = editBox.getValue().trim().replace(",", ".");
                if (value.isEmpty()) {
                    return 0.0;
                }
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                LOGGER.error("error parsing value: {}", e.getMessage());
                return 0.0;
            }
        }
    }
    
    // Класс для стилизованных кнопок
    public static class StyledButton extends Button {
        private final int defaultColor;
        private final int hoverColor;
        
        public StyledButton(int x, int y, int width, int height, Component message, OnPress onPress, int defaultColor, int hoverColor) {
            super(x, y, width, height, message, onPress);
            this.defaultColor = defaultColor;
            this.hoverColor = hoverColor;
        }
        
        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            if (defaultColor != 0x00000000) {
                int color = isHovered ? hoverColor : defaultColor;
                fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, color);
            }
            int textWidth = Minecraft.getInstance().font.width(this.getMessage());
            int textX = this.x + (this.width - textWidth) / 2;
            drawString(poseStack, Minecraft.getInstance().font, this.getMessage(), textX, this.y + (this.height - 8) / 2, TEXT_COLOR);
        }
    }
    
    private enum TabType {
        MODS, INDIVIDUAL_MOBS, DIMENSIONS, DIFFICULTY, PLAYER
    }
    private enum AttributeDisplayType {
        BASIC, ADVANCED
    }

    private TabType currentTab = TabType.MODS;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Button> contentButtons = new ArrayList<>();
    private final List<EditBox> textFields = new ArrayList<>();
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private final List<FieldMapping> fieldMappings = new ArrayList<>();
    
    // Флаг для отслеживания, показывать ли ночные настройки
    private boolean showNightSettings = false;
    
    private AttributeDisplayType currentAttributeDisplayType = AttributeDisplayType.BASIC;
    
    // Для хранения редактируемых данных
    private Map<String, IndividualMobAttributes> modConfigsCopy = new HashMap<>();
    private Map<String, IndividualMobConfig> mobConfigsCopy = new HashMap<>();
    private Map<String, DimensionConfig> dimensionConfigsCopy = new HashMap<>();
    private String selectedMod = null;
    private String selectedEntity = null;
    private String selectedDimension = null;
    
    // Добавляем перечисление для типов настроек
    private enum SettingsType {
        BASIC, NIGHT, CAVE
    }
    
    private SettingsType currentSettingsType = SettingsType.BASIC;
    
    // Класс для чекбоксов
    public static class CheckBox extends Button {
        private boolean checked;
        private final Component label;
        
        public CheckBox(int x, int y, int width, int height, Component label, boolean initialState, OnPress onPress) {
            super(x, y, width, height, Component.literal(""), onPress);
            this.label = label;
            this.checked = initialState;
        }
        
        public boolean isChecked() {
            return checked;
        }
        
        public void setChecked(boolean checked) {
            this.checked = checked;
        }
        
        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            fill(poseStack, this.x, this.y, this.x + 10, this.y + 10, isHovered ? CHECKBOX_UNCHECKED_COLOR : 0xFF888888);
            if (checked) {
                fill(poseStack, this.x + 2, this.y + 2, this.x + 8, this.y + 8, CHECKBOX_CHECKED_COLOR);
            }
            drawString(poseStack, Minecraft.getInstance().font, label, this.x + 15, this.y + 2, TEXT_COLOR);
        }
        
        @Override
        public void onClick(double mouseX, double mouseY) {
            this.checked = !this.checked;
            super.onClick(mouseX, mouseY);
        }
    }
    
    private class ModListScrollPanel extends net.minecraft.client.gui.components.AbstractWidget {
        private final List<Button> buttons = new ArrayList<>();
        private double scrollAmount;
        private boolean isDragging;
        private static final int SCROLL_SPEED = 15;
        private static final int BUTTON_HEIGHT = 25;
        private static final int SCROLLBAR_WIDTH = 6;

        public ModListScrollPanel(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        public void updateNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }

        public void addButton(Button button) {
            buttons.add(button);
            updateButtonPositions();
        }

        private void updateButtonPositions() {
            int currentY = y - (int)scrollAmount;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                
                // Определяем, является ли кнопка основной (кнопкой мода) или кнопкой удаления
                boolean isModButton = i % 2 == 0;
                
                if (isModButton) {
                    // Кнопка мода
                    button.x = x;
                    button.y = currentY;
                    
                    // Если есть соответствующая кнопка удаления
                    if (i + 1 < buttons.size()) {
                        Button deleteButton = buttons.get(i + 1);
                        deleteButton.x = x + 175 + 5;
                        deleteButton.y = currentY;
                    }
                    
                    currentY += BUTTON_HEIGHT;
                }
            }
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            // Отрисовка фона панели
            fill(poseStack, x, y, x + width, y + height, 0x80000000);

            // Настраиваем область отсечения
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int)(x * scale), (int)((Minecraft.getInstance().getWindow().getHeight() - (y + height) * scale)),
                          (int)(width * scale), (int)(height * scale));

            // Отрисовка кнопок
            for (Button button : buttons) {
                if (button.y + BUTTON_HEIGHT >= y && button.y <= y + height) {
                    // Корректируем позицию мыши для правильного определения наведения
                    button.render(poseStack, mouseX, mouseY, partialTick);
                }
            }

            // Отрисовка полосы прокрутки
            if ((buttons.size() / 2) * BUTTON_HEIGHT > height) {
                double contentHeight = (buttons.size() / 2) * BUTTON_HEIGHT;
                double scrollBarHeight = height * (height / contentHeight);
                double scrollBarY = y + (scrollAmount / contentHeight) * (height - scrollBarHeight);
                
                fill(poseStack, x + width - SCROLLBAR_WIDTH, y, x + width, y + height, 0x40000000);
                fill(poseStack, x + width - SCROLLBAR_WIDTH, (int)scrollBarY, x + width, 
                     (int)(scrollBarY + scrollBarHeight), 0x80FFFFFF);
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY)) {
                // Проверяем клик на полосе прокрутки
                if (mouseX >= x + width - SCROLLBAR_WIDTH && mouseX <= x + width) {
                    isDragging = true;
                    return true;
                }
                
                // Проверяем клики по кнопкам с учетом прокрутки
                double adjustedMouseY = mouseY + scrollAmount;
                for (Button b : buttons) {
                    if (mouseX >= b.x && mouseX <= b.x + b.getWidth() &&
                        adjustedMouseY >= b.y + scrollAmount && adjustedMouseY <= b.y + b.getHeight() + scrollAmount) {
                        return b.mouseClicked(mouseX, mouseY, button);
                    }
                }
                isDragging = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            isDragging = false;
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (isDragging) {
                // Проверяем, находится ли курсор над полосой прокрутки
                if (mouseX >= x + width - SCROLLBAR_WIDTH && mouseX <= x + width) {
                    double contentHeight = (buttons.size() / 2) * BUTTON_HEIGHT;
                    double maxScroll = Math.max(0, contentHeight - height);
                    
                    // Вычисляем новую позицию прокрутки на основе позиции курсора
                    double mouseRelativeY = mouseY - y;
                    double scrollPercentage = mouseRelativeY / height;
                    scrollAmount = maxScroll * scrollPercentage;
                    
                    // Ограничиваем значение прокрутки
                    scrollAmount = Math.max(0, Math.min(maxScroll, scrollAmount));
                } else {
                    // Если перетаскивание происходит не на полосе прокрутки,
                    // используем обычную логику прокрутки
                    double maxScroll = Math.max(0, (buttons.size() / 2) * BUTTON_HEIGHT - height);
                    scrollAmount = Math.max(0, Math.min(maxScroll, scrollAmount - dragY));
                }
                
                updateButtonPositions();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (isMouseOver(mouseX, mouseY)) {
                double maxScroll = Math.max(0, (buttons.size() / 2) * BUTTON_HEIGHT - height);
                scrollAmount = Math.max(0, Math.min(maxScroll, scrollAmount - delta * SCROLL_SPEED));
                updateButtonPositions();
                return true;
            }
            return false;
        }
    }

    private ModListScrollPanel modListPanel;
    
    public MobScalerScreen() {
        super(Component.translatable("gui.mobscaler.title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Очищаем списки
        this.clearWidgets();
        this.fieldMappings.clear();
        this.textFields.clear();
        this.checkBoxes.clear();
        this.contentButtons.clear();
        this.tabButtons.clear();
        this.labels.clear();
        
        // Инициализируем копии конфигураций
        initializeConfigCopies();
        
        // Инициализируем содержимое вкладки
        initTabContent();
        
        addCloseButton();
        addSaveButton();
    }
    
    private void initializeConfigCopies() {
        // Получаем копии текущих конфигураций
        modConfigsCopy = new HashMap<>(IndividualMobManager.getModConfigs());
        mobConfigsCopy = new HashMap<>(IndividualMobManager.getIndividualMobConfigs());
        dimensionConfigsCopy = new HashMap<>(DimensionConfigManager.getDimensionConfigs());
    }
    
    private void initTabContent() {
        // Удаляем предыдущие виджеты содержимого
        for (Button button : contentButtons) {
            this.removeWidget(button);
        }
        
        for (EditBox field : textFields) {
            this.removeWidget(field);
        }
        
        for (CheckBox checkbox : checkBoxes) {
            this.removeWidget(checkbox);
        }
        
        contentButtons.clear();
        textFields.clear();
        checkBoxes.clear();
        fieldMappings.clear();
        
        // Инициализируем содержимое в зависимости от текущей вкладки
        switch (currentTab) {
            case MODS:
                initModsTabContent();
                break;
            case INDIVIDUAL_MOBS:
                initMobsTabContent();
                break;
            case DIMENSIONS:
                initDimensionsTabContent();
                break;
            case DIFFICULTY:
                initSettingsTabContent();
                break;
            case PLAYER:
                initPlayerTabContent();
                break;
        }
    }
    
    private void initModsTabContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        
        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        if (selectedMod == null) {
            // Создаем панель прокрутки только если мод не выбран
            int panelX = 20;
            int panelY = 50;
            int panelWidth = 205;
            int panelHeight = this.height - panelY - 60; // Оставляем место внизу
            modListPanel = new ModListScrollPanel(panelX, panelY, panelWidth, panelHeight);
        
        // Получаем список модов
        List<String> mods = new ArrayList<>(modConfigsCopy.keySet());
        
        // Создаем кнопки для каждого мода
        for (String mod : mods) {
                // Создаем контейнер для мода (кнопка мода + кнопка удаления)
                Button modButton = new StyledButton(panelX, 0, 175, 20,
                    Component.literal(mod), button -> {
                selectedMod = mod;
                currentSettingsType = SettingsType.BASIC;
                initModAttributesContent();
            }, BUTTON_COLOR, BUTTON_HOVER_COLOR);
            
            // Добавляем кнопку удаления для каждого мода
                Button deleteButton = new StyledButton(panelX + 175 + 5, 0, 20, 20,
                    Component.literal("X"), button -> {
                    // Показываем диалог подтверждения
                    Minecraft.getInstance().setScreen(new DeleteConfirmationDialog(mod, () -> {
                deleteMod(mod);
                    }));
            }, DELETE_BUTTON_COLOR, 0xFFFF5555);
            
                modListPanel.addButton(modButton);
                modListPanel.addButton(deleteButton);
        }
        
        // Добавляем кнопку для создания нового мода
            Button addModButton = new StyledButton(panelX, panelY + panelHeight + 10, panelWidth, 20,
                getTranslatedText("gui.mobscaler.add_mod"), button -> {
            showAddModDialog();
        }, ADD_BUTTON_COLOR, 0xFF55FF55);
        this.addRenderableWidget(addModButton);
        } else {
            // Если мод выбран, инициализируем его настройки
            initModAttributesContent();
        }
    }
    
    private void showAddModDialog() {
        Minecraft.getInstance().setScreen(new AddModDialog());
    }
    
    private void createNewMod(String modId) {
        if (modId.isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.mod_id_empty"), false);
            return;
        }
        
        if (modConfigsCopy.containsKey(modId)) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.mod_exists"), false);
            return;
        }
        
        try {
            // Создаем новый конфиг с дефолтными значениями
            IndividualMobAttributes newConfig = IndividualMobAttributes.getDefault();
            
            // Добавляем новый конфиг в копию
            modConfigsCopy.put(modId, newConfig);
            
            // Добавляем новый конфиг в оба менеджера
            IndividualMobManager.getModConfigs().put(modId, newConfig);
            IndividualMobConfigManager.getModConfigs().put(modId, newConfig);
            
            // Сохраняем изменения в файл конфигурации
            IndividualMobConfigManager.saveModConfigs();
            
            // Перезагружаем конфигурацию
            MobScalerConfig.init();
            
            // Обновляем интерфейс
            this.init();
            
            // Выбираем новый мод
            selectedMod = modId;
            
            // Обновляем интерфейс
            initModAttributesContent();
            
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.mod_created"), false);
                
            // Закрываем диалог добавления мода
            Minecraft.getInstance().setScreen(MobScalerScreen.this);
        } catch (Exception e) {
            LOGGER.error("error creating new mod", e);
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.mod_creation", e.getMessage()), false);
        }
    }
    
    private void deleteMod(String modId) {
        try {
            
            // Удаляем мод из локальной копии конфигурации
            modConfigsCopy.remove(modId);
            
            // Синхронизируем конфигурации
            IndividualMobManager.clearAllModConfigs();
            IndividualMobConfigManager.getModConfigs().clear();
            IndividualMobConfigManager.getModConfigs().putAll(modConfigsCopy);
            
            // Сбрасываем выбранный мод, если он был удален
            if (selectedMod != null && selectedMod.equals(modId)) {
                selectedMod = null;
            }
            
            // Сохраняем изменения в файл конфигурации
            IndividualMobConfigManager.saveModConfigs();
            
            // Перезагружаем конфигурацию
            MobScalerConfig.init();
            
            // Обновляем интерфейс
            this.init();
            
            // Отображаем сообщение об успешном удалении
            if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.mod_deleted"), false);
            }
        } catch (Exception e) {
            LOGGER.error("error deleting mod: {}", modId, e);
            if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.error.mod_deletion", e.getMessage()), false);
            }
        }
    }
    
    private void initDimensionsTabContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        
        // Сбрасываем выбранное измерение при переходе на вкладку измерений
        selectedDimension = null;
        
        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        // Получаем список измерений
        List<String> dimensions = new ArrayList<>(dimensionConfigsCopy.keySet());
        
        // Создаем кнопки для каждого измерения
        int buttonY = 50;
        int startX = 20; // Начинаем с левого края с отступом
        
        for (String dimension : dimensions) {
            Button dimensionButton = new StyledButton(startX, buttonY, 180, 20, 
                    Component.literal(dimension), button -> {
                selectedDimension = dimension;
                currentSettingsType = SettingsType.BASIC;
                initDimensionSettingsContent();
            }, BUTTON_COLOR, BUTTON_HOVER_COLOR);
            this.addRenderableWidget(dimensionButton);
            
            // Обновляем обработчик кнопки удаления
            Button deleteButton = new StyledButton(startX + 185, buttonY, 20, 20, 
                    Component.literal("X"), button -> {
                // Показываем диалог подтверждения удаления измерения
                Minecraft.getInstance().setScreen(new DeleteDimensionConfirmationDialog(dimension, () -> {
                deleteDimension(dimension);
                }));
            }, DELETE_BUTTON_COLOR, 0xFFFF5555);
            this.addRenderableWidget(deleteButton);
            
            buttonY += 25;
        }
        
        // Добавляем кнопку для создания нового измерения
        Button addDimensionButton = new StyledButton(startX, buttonY, 205, 20, 
                getTranslatedText("gui.mobscaler.add_dimension"), button -> {
            showAddDimensionDialog();
        }, ADD_BUTTON_COLOR, 0xFF55FF55);
        this.addRenderableWidget(addDimensionButton);
    }
    
    private void showAddDimensionDialog() {
        Minecraft.getInstance().setScreen(new AddDimensionDialog());
    }
    
    private void createNewDimension(String dimensionId) {
        if (dimensionId.isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.dimension_id_empty"), false);
            return;
        }
        
        if (dimensionConfigsCopy.containsKey(dimensionId)) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.dimension_exists"), false);
            return;
        }
        
        try {
            // Создаем новый конфиг измерения
            DimensionConfig newConfig = null;
            
            // Берем первый доступный конфиг как шаблон или создаем новый
            if (!dimensionConfigsCopy.isEmpty()) {
                // Копируем первый доступный конфиг
                DimensionConfig template = dimensionConfigsCopy.values().iterator().next();
                newConfig = copyDimensionConfig(template);
            } else {
                // Если нет доступных конфигов, берем конфиг стандартного измерения
                newConfig = DimensionConfigManager.getDimensionConfigs().get("minecraft:overworld");
                if (newConfig == null) {
                    throw new IllegalStateException("error creating dimension config: template not found");
                }
                newConfig = copyDimensionConfig(newConfig);
            }
            
            // Устанавливаем дефолтные значения
            Field[] fields = newConfig.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                
                // Устанавливаем значения по умолчанию для различных типов полей
                if (field.getType() == double.class) {
                    if (fieldName.equals("caveHeight")) {
                        field.setDouble(newConfig, 60.0);
                    } else if (fieldName.endsWith("Multiplier")) {
                        field.setDouble(newConfig, 1.0);
                    } else if (fieldName.endsWith("Addition")) {
                        field.setDouble(newConfig, 0.0);
                    }
                } else if (field.getType() == boolean.class) {
                    field.setBoolean(newConfig, false);
                }
            }
            
            // Добавляем новый конфиг в копию
            dimensionConfigsCopy.put(dimensionId, newConfig);
            
            // Добавляем новый конфиг в оригинальную карту
            DimensionConfigManager.getDimensionConfigs().put(dimensionId, newConfig);
            
            // Выбираем новое измерение
            selectedDimension = dimensionId;
            
            // Закрываем диалог и возвращаемся к основному экрану
            Minecraft.getInstance().setScreen(MobScalerScreen.this);
            
            // Обновляем интерфейс для показа настроек нового измерения
            initDimensionSettingsContent();
            
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.dimension_created"), false);
        } catch (Exception e) {
            LOGGER.error("error creating new dimension", e);
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.dimension_creation", e.getMessage()), false);
        }
    }
    
    private DimensionConfig copyDimensionConfig(DimensionConfig original) {
        try {
            // Создаем новый экземпляр через клонирование полей
            DimensionConfig copy = DimensionConfigManager.getDimensionConfigs().get("minecraft:overworld");
            if (copy == null) {
                throw new IllegalStateException("error creating dimension config: template not found");
            }
            
            // Копируем значения всех полей из оригинала
            Field[] fields = original.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                field.set(copy, field.get(original));
            }
            
            return copy;
        } catch (Exception e) {
            LOGGER.error("error copying dimension config", e);
            throw new RuntimeException("error copying dimension config", e);
        }
    }
    
    // Класс для отображения текстовых меток
    private static class Label {
        private final String text;
        private final int x;
        private final int y;
        private final int color;
        private final boolean isBold;
        private final boolean isCenter;
        
        public Label(String text, int x, int y, int color) {
            this(text, x, y, color, false, false);
        }
        
        public Label(String text, int x, int y, int color, boolean isBold, boolean isCenter) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.isBold = isBold;
            this.isCenter = isCenter;
        }
        
        public void render(PoseStack poseStack, Font font) {
            Component textComponent = Component.literal(text);
            if (isBold) {
                textComponent = textComponent.copy().withStyle(ChatFormatting.BOLD);
            }
            
            if (isCenter) {
                font.drawShadow(poseStack, textComponent, Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - font.width(textComponent) / 2, y, color);
            } else {
                font.drawShadow(poseStack, textComponent, x, y, color);
            }
        }
    }
    
    private final List<Label> labels = new ArrayList<>();
    
    private void addLabel(String text, int x, int y) {
        Label label = new Label(text, x, y, TEXT_COLOR);
        labels.add(label);
    }
    
    /**
     * Добавляет жирный заголовок по центру экрана
     */
    private void addCenteredBoldLabel(String text, int y) {
        Label label = new Label(text, 0, y, TEXT_COLOR, true, true);
        labels.add(label);
    }
    
    /**
     * Добавляет кнопки вкладок в интерфейс
     */
    private void addTabButtons() {
        int tabWidth = 80;
        int tabHeight = 20;
        int tabY = 5;
        int tabSpacing = 5;
        int startX = 10;
        
        // Вкладка модов
        Button modsTabButton = new StyledButton(startX, tabY, tabWidth, tabHeight, 
                getTranslatedText("gui.mobscaler.tabs.mods"), button -> {
            currentTab = TabType.MODS;
            selectedMod = null; // Сбрасываем выбранный мод при переключении на вкладку модов
            this.init();
        }, currentTab == TabType.MODS ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(modsTabButton);
        this.tabButtons.add(modsTabButton);
        
        // Вкладка мобов
        Button mobsTabButton = new StyledButton(startX + tabWidth + tabSpacing, tabY, tabWidth, tabHeight, 
                getTranslatedText("gui.mobscaler.tabs.mobs"), button -> {
            currentTab = TabType.INDIVIDUAL_MOBS;
            this.init();
        }, currentTab == TabType.INDIVIDUAL_MOBS ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(mobsTabButton);
        this.tabButtons.add(mobsTabButton);
        
        // Вкладка измерений
        Button dimensionsTabButton = new StyledButton(startX + (tabWidth + tabSpacing) * 2, tabY, tabWidth, tabHeight, 
                getTranslatedText("gui.mobscaler.tabs.dimensions"), button -> {
            currentTab = TabType.DIMENSIONS;
            this.init();
        }, currentTab == TabType.DIMENSIONS ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(dimensionsTabButton);
        this.tabButtons.add(dimensionsTabButton);
        
        // Вкладка настроек сложности
        Button difficultyTabButton = new StyledButton(startX + (tabWidth + tabSpacing) * 3, tabY, tabWidth, tabHeight, 
                getTranslatedText("gui.mobscaler.tabs.difficulty"), button -> {
            currentTab = TabType.DIFFICULTY;
            this.init();
        }, currentTab == TabType.DIFFICULTY ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(difficultyTabButton);
        this.tabButtons.add(difficultyTabButton);
        
        // Вкладка настроек игрока
        Button playerTabButton = new StyledButton(startX + (tabWidth + tabSpacing) * 4, tabY, tabWidth, tabHeight, 
                getTranslatedText("gui.mobscaler.tabs.player"), button -> {
            currentTab = TabType.PLAYER;
            this.init();
        }, currentTab == TabType.PLAYER ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(playerTabButton);
        this.tabButtons.add(playerTabButton);
    }
    
    /**
     * Добавляет кнопку закрытия в интерфейс
     */
    private void addCloseButton() {
        Button closeButton = new StyledButton(
            this.width - 60, 
            10, 
            50, 
            20, 
            getTranslatedText("gui.mobscaler.buttons.close"), 
            button -> this.onClose(), 
            DELETE_BUTTON_COLOR, 
            BUTTON_HOVER_COLOR
        );
        this.addRenderableWidget(closeButton);
    }
    
    /**
     * Добавляет кнопку сохранения в интерфейс
     */
    private void addSaveButton() {
        Button saveButton = new StyledButton(
            this.width - 120, 
            10, 
            50, 
            20, 
            getTranslatedText("gui.mobscaler.buttons.save"), 
            button -> saveChanges(), 
            ADD_BUTTON_COLOR, 
            BUTTON_HOVER_COLOR
        );
        this.addRenderableWidget(saveButton);
    }
    
    /**
     * Инициализирует содержимое вкладки мобов
     */
    private void initMobsTabContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        this.labels.clear(); // Очищаем все метки
        this.textFields.clear(); // Очищаем текстовые поля
        this.fieldMappings.clear(); // Очищаем маппинги полей
        this.checkBoxes.clear(); // Очищаем чекбоксы

        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        // Размеры для сетки мобов
        final int ICON_SIZE = 32;
        final int SPACING = 10;
        final int ICONS_PER_ROW = (this.width - 40) / (ICON_SIZE + SPACING);
        
        // Начальные координаты для сетки
        int startX = (this.width - (ICONS_PER_ROW * (ICON_SIZE + SPACING) - SPACING)) / 2;
        int startY = 50;
        
        // Получаем список всех настроенных мобов
        List<String> configuredMobs = new ArrayList<>(mobConfigsCopy.keySet());
        
        // Создаем кнопки для каждого моба
        int currentX = startX;
        int currentY = startY;
        int mobsInCurrentRow = 0;
        
        for (String mobId : configuredMobs) {
            // Создаем кнопку с иконкой моба
            Button mobButton = new EntityIconButton(
                currentX, currentY, ICON_SIZE, ICON_SIZE,
                Component.literal(""),
                button -> {
                    selectedEntity = mobId;
                    initMobSettingsContent();
                },
                mobId
            );
            
            // Создаем кнопку удаления с повышенным Z-индексом
            Button deleteButton = new StyledButton(
                currentX + ICON_SIZE - 12, currentY, 12, 12,
                Component.literal("×"),
                button -> {
                    Minecraft.getInstance().setScreen(new DeleteMobConfirmationDialog(mobId, () -> {
                        deleteMob(mobId);
                    }));
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            ) {
                @Override
                public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                    poseStack.pushPose();
                    poseStack.translate(0, 0, 500.0F); // Увеличиваем Z-координату значительно выше, чем у сущности (200 -> 500)
                    super.render(poseStack, mouseX, mouseY, partialTick);
                    poseStack.popPose();
                }
            };
            
            // Сначала добавляем кнопку моба, затем кнопку удаления
            this.addRenderableWidget(deleteButton); // Сначала добавляем кнопку удаления
            this.addRenderableWidget(mobButton);    // Затем добавляем кнопку моба
            
            // Обновляем позицию для следующей иконки
            mobsInCurrentRow++;
            if (mobsInCurrentRow >= ICONS_PER_ROW) {
                currentX = startX;
                currentY += ICON_SIZE + SPACING;
                mobsInCurrentRow = 0;
            } else {
                currentX += ICON_SIZE + SPACING;
            }
        }
        
        // Добавляем кнопку для создания нового моба внизу по центру
        int addButtonWidth = 200;
        int addButtonHeight = 20;
        Button addMobButton = new StyledButton(
            (this.width - addButtonWidth) / 2,
            this.height - 40,
            addButtonWidth,
            addButtonHeight,
            getTranslatedText("gui.mobscaler.add_mob"),
            button -> showAddMobDialog(),
            ADD_BUTTON_COLOR,
            0xFF55FF55
        );
        this.addRenderableWidget(addMobButton);
    }
    
    // Класс для кнопки с иконкой сущности
    private class EntityIconButton extends Button {
        private final String entityId;
        private net.minecraft.world.entity.EntityType<?> entityType;
        private String displayName;
        
        public EntityIconButton(int x, int y, int width, int height, Component title, OnPress pressedAction, String entityId) {
            super(x, y, width, height, title, pressedAction);
            this.entityId = entityId;
            try {
                // Получаем тип сущности из регистра
                this.entityType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                    new net.minecraft.resources.ResourceLocation(entityId)
                );
                if (entityType != null) {
                    this.displayName = entityType.getDescription().getString();
                } else {
                    this.displayName = entityId;
                }
            } catch (Exception e) {
                LOGGER.error("error getting entity type for {}: {}", entityId, e.getMessage());
                this.displayName = entityId;
            }
        }
        
        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            // Рисуем фон кнопки
            fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 
                 this.isHovered ? 0x80FFFFFF : 0x80000000);
            
            // Рисуем иконку сущности
            if (entityType != null) {
                try {
                    // Создаем временную сущность для отрисовки
                    net.minecraft.world.entity.Entity entity = entityType.create(Minecraft.getInstance().level);
                    if (entity != null) {
                        // Вычисляем масштаб на основе размера сущности
                        float entityWidth = entity.getBbWidth();
                        float entityHeight = entity.getBbHeight();
                        float maxDimension = Math.max(entityWidth, entityHeight);
                        float scale = (this.height * 0.8f) / maxDimension;
                        
                        poseStack.pushPose();
                        
                        // Центр кнопки
                        float centerX = this.x + this.width/2;
                        float centerY = this.y + this.height/2;
                        
                        // Вычисляем углы поворота на основе позиции курсора
                        float deltaX = mouseX - centerX;
                        float deltaY = mouseY - centerY;
                        
                        // Нормализуем дельты относительно размера кнопки для более плавного поворота
                        float normalizedDeltaX = deltaX / (this.width / 2.0f);
                        float normalizedDeltaY = deltaY / (this.height / 2.0f);
                        
                        // Рассчитываем расстояние от центра для определения силы наклона
                        float distance = (float) Math.sqrt(normalizedDeltaX * normalizedDeltaX + normalizedDeltaY * normalizedDeltaY);
                        
                        // Рассчитываем угол yaw, чтобы моб смотрел на курсор
                        // Инвертируем X для правильного направления поворота
                        float yaw = (float) Math.toDegrees(Math.atan2(-normalizedDeltaX, normalizedDeltaY));
                        
                        // Ограничиваем углы поворота, чтобы моб не крутился слишком сильно
                        float maxYawDelta = 105.0f; // Максимальный угол поворота по горизонтали
                        float maxPitchDelta = 35.0f; // Максимальный угол поворота по вертикали
                        
                        // Рассчитываем pitch на основе вертикального положения курсора
                        // Используем нормализованную Y-координату для наклона
                        float pitch = -normalizedDeltaY * maxPitchDelta;
                        
                        // Если курсор далеко от центра, увеличиваем наклон
                        if (distance > 1.0f) {
                            pitch *= Math.min(1.5f, distance);
                        }
                        
                        // Ограничиваем yaw в диапазоне [-maxYawDelta, maxYawDelta]
                        yaw = Math.max(-maxYawDelta, Math.min(maxYawDelta, yaw));
                        // Ограничиваем pitch в диапазоне [-maxPitchDelta, maxPitchDelta]
                        pitch = Math.max(-maxPitchDelta, Math.min(maxPitchDelta, pitch));
                        
                        // Применяем трансформации
                        poseStack.translate(centerX, centerY, 200);
                        poseStack.scale(scale, scale, scale);
                        // Поворачиваем сущность на 180 градусов вокруг оси X чтобы исправить перевернутость
                        poseStack.mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(180.0F));
                        // Применяем поворот по горизонтали (yaw)
                        poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(180.0F + yaw));
                        // Применяем наклон по вертикали (pitch)
                        poseStack.mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(pitch));
                        
                        // Отрисовываем сущность
                        Minecraft.getInstance().getEntityRenderDispatcher().render(
                            entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, poseStack,
                            Minecraft.getInstance().renderBuffers().bufferSource(),
                            15728880
                        );
                        
                        poseStack.popPose();
                        
                        // Очищаем буфер рендера
                        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
                    }
                } catch (Exception e) {
                    LOGGER.error("error rendering entity: {}", entityId, e);
                }
            }
            
            // Если курсор наведен, показываем всплывающую подсказку
            if (this.isHovered) {
                renderTooltip(poseStack, mouseX, mouseY);
            }
        }
        
        private void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(getTranslatedText("gui.mobscaler.tooltip.id").getString() + entityId));
            tooltip.add(Component.literal(getTranslatedText("gui.mobscaler.tooltip.name").getString() + displayName));
            
            // Сдвигаем подсказку выше курсора и добавляем отступ
            int tooltipY = mouseY - 40; // Увеличиваем отступ вверх
            
            // Сохраняем текущее состояние матрицы и настройки рендера
            poseStack.pushPose();
            RenderSystem.disableDepthTest();
            
            // Устанавливаем Z-координату для отрисовки поверх всех элементов
            poseStack.translate(0, 0, 400.0F);
            
            // Отрисовываем подсказку
            Minecraft.getInstance().screen.renderComponentTooltip(
                poseStack,
                tooltip,
                mouseX,
                Math.max(0, tooltipY),
                Minecraft.getInstance().font
            );
            
            // Восстанавливаем состояние
            RenderSystem.enableDepthTest();
            poseStack.popPose();
        }
    }

    private void showAddMobDialog() {
        Minecraft.getInstance().setScreen(new AddMobDialog());
    }

    private void deleteMob(String mobId) {
        try {
            
            // Удаляем моба из копии конфигурации
            mobConfigsCopy.remove(mobId);
            
            // Удаляем из основного хранилища
            IndividualMobManager.removeIndividualMobConfig(mobId);
            
            // Удаляем из IndividualMobConfigManager
            IndividualMobConfigManager.getIndividualMobConfigs().remove(mobId);
            
            // Удаляем все модификаторы у существующих мобов этого типа
            com.example.mobscaler.events.EntityHandler.removeAllModifiersForEntityType(mobId);
            
            // Сохраняем изменения
            IndividualMobConfigManager.saveIndividualConfigs();
            
            // Перезагружаем конфигурацию
            MobScalerConfig.init();
            
            // Если удаленный моб был выбран, сбрасываем выбор
            if (mobId.equals(selectedEntity)) {
                selectedEntity = null;
            }
            
            // Обновляем интерфейс
            this.init();
            
            // Показываем сообщение об успешном удалении
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.mob_deleted"), false);
                
        } catch (Exception e) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.mob_deletion", e.getMessage()), false);
        }
    }
    
    /**
     * Инициализирует содержимое вкладки настроек
     */
    private void initSettingsTabContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        
        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        // Заголовок
        int startX = 60;
        int startY = 60;
        int spacing = 25;

        // Левая колонка - множители здоровья
        // Вместо прямой отрисовки добавляем метку
        addLabel(getTranslatedText("gui.mobscaler.health_multipliers").getString(), startX, startY);
        startY += spacing;
        
        addDifficultyField(startX, startY, getTranslatedText("gui.mobscaler.difficulty.health.peaceful").getString(), "difficulty.health.peaceful");
        addDifficultyField(startX, startY + spacing, getTranslatedText("gui.mobscaler.difficulty.health.easy").getString(), "difficulty.health.easy");
        addDifficultyField(startX, startY + spacing * 2, getTranslatedText("gui.mobscaler.difficulty.health.normal").getString(), "difficulty.health.normal");
        addDifficultyField(startX, startY + spacing * 3, getTranslatedText("gui.mobscaler.difficulty.health.hard").getString(), "difficulty.health.hard");
        
        // Правая колонка - множители урона
        int rightColumnX = this.width / 2 + 20;
        startY = 60; // Сбрасываем Y для правой колонки
        
        // Вместо прямой отрисовки добавляем метку
        addLabel(getTranslatedText("gui.mobscaler.damage_multipliers").getString(), rightColumnX, startY);
        startY += spacing;
        
        addDifficultyField(rightColumnX, startY, getTranslatedText("gui.mobscaler.difficulty.damage.peaceful").getString(), "difficulty.damage.peaceful");
        addDifficultyField(rightColumnX, startY + spacing, getTranslatedText("gui.mobscaler.difficulty.damage.easy").getString(), "difficulty.damage.easy");
        addDifficultyField(rightColumnX, startY + spacing * 2, getTranslatedText("gui.mobscaler.difficulty.damage.normal").getString(), "difficulty.damage.normal");
        addDifficultyField(rightColumnX, startY + spacing * 3, getTranslatedText("gui.mobscaler.difficulty.damage.hard").getString(), "difficulty.damage.hard");
    }

    private void addDifficultyField(int x, int y, String label, String configPath) {
        // Получаем значение из конфига
        double value = MobScalerConfig.getDifficultyValue(configPath);
        
        // Добавляем метку вместо прямой отрисовки
        addLabel(label + ":", x, y + 5);
        
        // Создаем поле ввода
        EditBox valueBox = new EditBox(this.font, x + 150, y, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 259) { // Код клавиши Backspace
                    String text = this.getValue();
                    if (!text.isEmpty()) {
                        this.setValue(text.substring(0, text.length() - 1));
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        
        valueBox.setValue(String.format("%.1f", value).replace(",", "."));
        this.addRenderableWidget(valueBox);
        this.textFields.add(valueBox);
        
        // Добавляем маппинг поля
        FieldMapping mapping = new FieldMapping(configPath, label);
        mapping.setEditBox(valueBox);
        this.fieldMappings.add(mapping);
    }

    private void saveDifficultySettings() {
        for (FieldMapping mapping : fieldMappings) {
            if (mapping.getFieldName().startsWith("difficulty.")) {
                try {
                    double value = mapping.getValue();
                    MobScalerConfig.setDifficultyValue(mapping.getFieldName(), value);
                } catch (Exception e) {
                    LOGGER.error("error saving difficulty value for {}: {}", 
                        mapping.getFieldName(), e.getMessage());
                }
            }
        }
        
        // Сохраняем конфигурацию
        MobScalerConfig.save();
    }
    

    private void initModAttributesContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        this.labels.clear(); // Очищаем все метки
        this.textFields.clear(); // Очищаем текстовые поля
        this.fieldMappings.clear(); // Очищаем маппинги полей
        this.checkBoxes.clear(); // Очищаем чекбоксы
        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        if (selectedMod == null) return;
        
        IndividualMobAttributes config = modConfigsCopy.get(selectedMod);
        if (config == null) return;
        
        // Заголовок с названием мода
        addCenteredBoldLabel(getTranslatedText("gui.mobscaler.mod.settings").getString() + ": " + selectedMod, 40);
        
        // Получаем текущие значения
        final AtomicBoolean enableNightScaling = new AtomicBoolean(false);
        final AtomicBoolean enableCaveScaling = new AtomicBoolean(false);
        
        try {
            // Используем рефлексию для доступа к приватным полям
            Field enableNightField = config.getClass().getDeclaredField("enableNightScaling");
            enableNightField.setAccessible(true);
            enableNightScaling.set(enableNightField.getBoolean(config));
            
            Field enableCaveField = config.getClass().getDeclaredField("enableCaveScaling");
            enableCaveField.setAccessible(true);
            enableCaveScaling.set(enableCaveField.getBoolean(config));
        } catch (Exception e) {
            LOGGER.error("error getting mod attributes", e);
        }
        
        // Добавляем кнопки для переключения между типами настроек
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonY = 60;
        int buttonSpacing = 10;
        int startX = 20;
        
        // Кнопка базовых настроек
        Button basicButton = new StyledButton(startX, buttonY, buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.settings.basic"), button -> {
            currentSettingsType = SettingsType.BASIC;
            initModAttributesContent();
        }, currentSettingsType == SettingsType.BASIC ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        basicButton.active = currentSettingsType != SettingsType.BASIC;
        this.addRenderableWidget(basicButton);
        
        // Кнопка ночных настроек
        Button nightButton = new StyledButton(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.settings.night"), button -> {
            if (enableNightScaling.get()) {
                currentSettingsType = SettingsType.NIGHT;
                initModAttributesContent();
            }
        }, currentSettingsType == SettingsType.NIGHT ? BUTTON_HOVER_COLOR : (enableNightScaling.get() ? BUTTON_COLOR : 0xFF888888), 
           enableNightScaling.get() ? BUTTON_HOVER_COLOR : 0xFF888888);
        nightButton.active = enableNightScaling.get() && currentSettingsType != SettingsType.NIGHT;
        this.addRenderableWidget(nightButton);
        
        // Кнопка пещерных настроек
        Button caveButton = new StyledButton(startX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.settings.cave"), button -> {
            if (enableCaveScaling.get()) {
                currentSettingsType = SettingsType.CAVE;
                initModAttributesContent();
            }
        }, currentSettingsType == SettingsType.CAVE ? BUTTON_HOVER_COLOR : (enableCaveScaling.get() ? BUTTON_COLOR : 0xFF888888), 
           enableCaveScaling.get() ? BUTTON_HOVER_COLOR : 0xFF888888);
        caveButton.active = enableCaveScaling.get() && currentSettingsType != SettingsType.CAVE;
        this.addRenderableWidget(caveButton);
        
        Button attributeTypeButton = new StyledButton(startX + (buttonWidth + buttonSpacing) * 2, buttonY + 25, buttonWidth, buttonHeight, 
        Component.literal(currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                getTranslatedText("gui.mobscaler.advanced").getString() : getTranslatedText("gui.mobscaler.basic").getString()), 
        button -> {
            // Переключаемся между основными и дополнительными атрибутами
            currentAttributeDisplayType = currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                    AttributeDisplayType.ADVANCED : AttributeDisplayType.BASIC;
            
            // Обновляем экран
            initModAttributesContent();
        },
        currentAttributeDisplayType == AttributeDisplayType.ADVANCED ? 0xFF4B0082 : 0xFF006400, // Индиго для доп., темно-зеленый для базовых
        currentAttributeDisplayType == AttributeDisplayType.ADVANCED ? 0xFF800080 : 0xFF008000); // Пурпурный для доп., зеленый для базовых

        this.addRenderableWidget(attributeTypeButton);
        this.contentButtons.add(attributeTypeButton);
        // Добавляем чекбоксы для включения/отключения ночного и пещерного масштабирования
        int checkboxY = buttonY + buttonHeight + 10;
        
        // Чекбокс для ночного масштабирования
        CheckBox nightCheckbox = new CheckBox(startX, checkboxY, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_night_scaling"), enableNightScaling.get(), 
                button -> updateModCheckboxes());
        this.addRenderableWidget(nightCheckbox);
        this.checkBoxes.add(nightCheckbox);
        
        // Чекбокс для пещерного масштабирования
        CheckBox caveCheckbox = new CheckBox(startX, checkboxY + 25, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_cave_scaling"), enableCaveScaling.get(), 
                button -> updateModCheckboxes());
        this.addRenderableWidget(caveCheckbox);
        this.checkBoxes.add(caveCheckbox);
        
        // Получаем значение enableGravity
        final AtomicBoolean enableGravity = new AtomicBoolean(false);
        try {
            Field enableGravityField = config.getClass().getDeclaredField("enableGravity");
            enableGravityField.setAccessible(true);
            enableGravity.set(enableGravityField.getBoolean(config));
        } catch (Exception e) {
            LOGGER.error("error getting enableGravity value", e);
        }
        
        CheckBox gravityEnabler = new CheckBox(startX, checkboxY + 50, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_gravity"), enableGravity.get(), 
                button -> updateModCheckboxes());
        this.addRenderableWidget(gravityEnabler);
        this.checkBoxes.add(gravityEnabler);
        
        // Получаем значение высоты пещеры
        final AtomicReference<Double> caveHeight = new AtomicReference<>(60.0);
        try {
            Field caveHeightField = config.getClass().getDeclaredField("caveHeight");
            caveHeightField.setAccessible(true);
            caveHeight.set(caveHeightField.getDouble(config));
        } catch (Exception e) {
            LOGGER.error("error getting cave height value", e);
        }
        
        // Поле для высоты пещеры (показываем только если включено пещерное масштабирование)
        if (enableCaveScaling.get()) {
            addLabel(getTranslatedText("gui.mobscaler.cave_height").getString(), startX + 200, checkboxY + 30);
            EditBox caveHeightBox = new EditBox(this.font, startX + 280, checkboxY + 25, 35, 20, Component.literal("")) {
                @Override
                public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                    if (keyCode == 259) { // Код клавиши Backspace
                        String text = this.getValue();
                        if (!text.isEmpty()) {
                            this.setValue(text.substring(0, text.length() - 1));
                            return true;
                        }
                    }
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
                
                @Override
                public void insertText(String text) {
                    if (enableCaveScaling.get()) {
                        // Заменяем запятые на точки при вводе
                        text = text.replace(",", ".");
                        // Проверяем, что после вставки текста значение останется допустимым числом
                        String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                    if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                            super.insertText(text);
                        }
                    }
                }
            };
            caveHeightBox.setValue(String.format("%.1f", caveHeight.get()).replace(",", "."));
            this.addRenderableWidget(caveHeightBox);
            this.textFields.add(caveHeightBox);
            
            // Добавляем маппинг для высоты пещеры
            FieldMapping caveHeightMapping = new FieldMapping("caveHeight", getTranslatedText("gui.mobscaler.cave_height").getString());
            caveHeightMapping.setEditBox(caveHeightBox);
            this.fieldMappings.add(caveHeightMapping);
        }
        
        // Получаем значение множителя гравитации
        final AtomicReference<Double> gravityMultiplier = new AtomicReference<>(1.0);
        try {
            Field gravityField = config.getClass().getDeclaredField("gravityMultiplier");
            gravityField.setAccessible(true);
            gravityMultiplier.set(gravityField.getDouble(config));
        } catch (Exception e) {
            LOGGER.error("error getting gravity multiplier value", e);
        }
        
        // Поле для множителя гравитации (справа от высоты пещеры)
        addLabel(getTranslatedText("gui.mobscaler.gravity_multiplier").getString(), startX + 205, checkboxY + 55);
        EditBox gravityBox = new EditBox(this.font, startX + 280, checkboxY + 50, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 259) { // Код клавиши Backspace
                    String text = this.getValue();
                    if (!text.isEmpty()) {
                        this.setValue(text.substring(0, text.length() - 1));
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        gravityBox.setValue(String.format("%.2f", gravityMultiplier.get()).replace(",", "."));
        this.addRenderableWidget(gravityBox);
        this.textFields.add(gravityBox);
        
        // Добавляем маппинг для множителя гравитации
        FieldMapping gravityMapping = new FieldMapping("gravityMultiplier", getTranslatedText("gui.mobscaler.gravity_multiplier").getString());
        gravityMapping.setEditBox(gravityBox);
        this.fieldMappings.add(gravityMapping);
        
        // Добавляем поля для настройки атрибутов в зависимости от выбранного типа
        int fieldsX = startX + (buttonWidth + buttonSpacing) * 3 + 5; // Начинаем правее последней кнопки
        int fieldsY = buttonY; // Начинаем на той же высоте, что и кнопки
        
        // Определяем префикс полей в зависимости от типа настроек
        String fieldPrefix = "";
        String titleSuffix = "";
        
        switch (currentSettingsType) {
            case NIGHT:
                fieldPrefix = "Night";
                titleSuffix = getTranslatedText("gui.mobscaler.settings.night_suffix").getString();
                break;
            case CAVE:
                fieldPrefix = "Cave";
                titleSuffix = getTranslatedText("gui.mobscaler.settings.cave_suffix").getString();
                break;
            default:
                fieldPrefix = "";
                titleSuffix = getTranslatedText("gui.mobscaler.settings.basic_suffix").getString();
                break;
        }
        
        // Добавляем заголовок для текущего режима настроек
        addLabel(getTranslatedText("gui.mobscaler.attributes.title").getString() + titleSuffix, fieldsX, fieldsY);
        
        // Добавляем поля для настройки атрибутов
        final int FIELD_SPACING = 25;
        
        // Используем разные атрибуты в зависимости от выбранного режима отображения
        if (currentAttributeDisplayType == AttributeDisplayType.BASIC) {
            // Базовые атрибуты
            addModField(fieldsX, fieldsY + FIELD_SPACING, getTranslatedText("gui.mobscaler.health").getString(), fieldPrefix + "Health", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 2, getTranslatedText("gui.mobscaler.armor").getString(), fieldPrefix + "Armor", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 3, getTranslatedText("gui.mobscaler.damage").getString(), fieldPrefix + "Damage", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 4, getTranslatedText("gui.mobscaler.speed").getString(), fieldPrefix + "Speed", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 5, getTranslatedText("gui.mobscaler.knockback_resistance").getString(), fieldPrefix + "KnockbackResistance", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 6, getTranslatedText("gui.mobscaler.attack_knockback").getString(), fieldPrefix + "AttackKnockback", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 7, getTranslatedText("gui.mobscaler.attack_speed").getString(), fieldPrefix + "AttackSpeed", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 8, getTranslatedText("gui.mobscaler.follow_range").getString(), fieldPrefix + "FollowRange", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 9, getTranslatedText("gui.mobscaler.flying_speed").getString(), fieldPrefix + "FlyingSpeed", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 10, getTranslatedText("gui.mobscaler.armor_toughness").getString(), fieldPrefix + "ArmorToughness", config);
        } else {
            // Дополнительные атрибуты
            addModField(fieldsX, fieldsY + FIELD_SPACING, getTranslatedText("gui.mobscaler.luck").getString(), fieldPrefix + "Luck", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 2, getTranslatedText("gui.mobscaler.swim_speed").getString(), fieldPrefix + "SwimSpeed", config);
            addModField(fieldsX, fieldsY + FIELD_SPACING * 3, getTranslatedText("gui.mobscaler.reach_distance").getString(), fieldPrefix + "ReachDistance", config);
        }
    }
    
    private void initDimensionSettingsContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        this.labels.clear(); // Очищаем все метки
        this.textFields.clear(); // Очищаем текстовые поля
        this.fieldMappings.clear(); // Очищаем маппинги полей
        this.labels.clear(); // Очищаем все метки
        
        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        if (selectedDimension == null) return;
        
        DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
        if (config == null) return;
        
        // Заголовок с названием измерения
        addCenteredBoldLabel(getTranslatedText("gui.mobscaler.dimension.settings").getString() + ": " + getDimensionDisplayName(selectedDimension), 40);
        
        // Получаем текущие значения
        final AtomicBoolean enableNightScaling = new AtomicBoolean(false);
        final AtomicBoolean enableCaveScaling = new AtomicBoolean(false);
        final AtomicReference<Double> caveHeight = new AtomicReference<>(60.0);
        
        try {
            // Используем рефлексию для доступа к приватным полям
            Field enableNightField = config.getClass().getDeclaredField("enableNightScaling");
            enableNightField.setAccessible(true);
            enableNightScaling.set(enableNightField.getBoolean(config));
            
            Field enableCaveField = config.getClass().getDeclaredField("enableCaveScaling");
            enableCaveField.setAccessible(true);
            enableCaveScaling.set(enableCaveField.getBoolean(config));
            
            Field caveHeightField = config.getClass().getDeclaredField("caveHeight");
            caveHeightField.setAccessible(true);
            caveHeight.set(caveHeightField.getDouble(config));
        } catch (Exception e) {
            LOGGER.error("error getting checkbox values", e);
        }
        
        // Добавляем кнопки для переключения между типами настроек
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonY = 60;
        int buttonSpacing = 10;
        int startX = 20;
        
        // Кнопка базовых настроек
        Button basicButton = new StyledButton(startX, buttonY, buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.settings.basic"), button -> {
            currentSettingsType = SettingsType.BASIC;
            initDimensionSettingsContent();
        }, currentSettingsType == SettingsType.BASIC ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
        basicButton.active = currentSettingsType != SettingsType.BASIC;
        this.addRenderableWidget(basicButton);
        
        // Кнопка ночных настроек
        Button nightButton = new StyledButton(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.settings.night"), button -> {
            if (enableNightScaling.get()) {
                currentSettingsType = SettingsType.NIGHT;
                initDimensionSettingsContent();
            }
        }, currentSettingsType == SettingsType.NIGHT ? BUTTON_HOVER_COLOR : (enableNightScaling.get() ? BUTTON_COLOR : 0xFF888888), 
           enableNightScaling.get() ? BUTTON_HOVER_COLOR : 0xFF888888);
        nightButton.active = enableNightScaling.get() && currentSettingsType != SettingsType.NIGHT;
        this.addRenderableWidget(nightButton);
        
        // Кнопка пещерных настроек
        Button caveButton = new StyledButton(startX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.settings.cave"), button -> {
            if (enableCaveScaling.get()) {
                currentSettingsType = SettingsType.CAVE;
                initDimensionSettingsContent();
            }
        }, currentSettingsType == SettingsType.CAVE ? BUTTON_HOVER_COLOR : (enableCaveScaling.get() ? BUTTON_COLOR : 0xFF888888), 
           enableCaveScaling.get() ? BUTTON_HOVER_COLOR : 0xFF888888);
        caveButton.active = enableCaveScaling.get() && currentSettingsType != SettingsType.CAVE;
        this.addRenderableWidget(caveButton);
        
        Button attributeTypeButton = new StyledButton(startX + (buttonWidth + buttonSpacing) * 2, buttonY + 25, buttonWidth, buttonHeight,
        Component.literal(currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                getTranslatedText("gui.mobscaler.advanced").getString() : getTranslatedText("gui.mobscaler.basic").getString()), 
        button -> {
            // Переключаемся между основными и дополнительными атрибутами
            currentAttributeDisplayType = currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                    AttributeDisplayType.ADVANCED : AttributeDisplayType.BASIC;
            
            // Обновляем экран
            initDimensionSettingsContent();
        },
        currentAttributeDisplayType == AttributeDisplayType.ADVANCED ? 0xFF4B0082 : 0xFF006400, // Индиго для доп., темно-зеленый для базовых
        currentAttributeDisplayType == AttributeDisplayType.ADVANCED ? 0xFF800080 : 0xFF008000); // Пурпурный для доп., зеленый для базовых

        this.addRenderableWidget(attributeTypeButton);
        this.contentButtons.add(attributeTypeButton);
        // Добавляем чекбоксы для включения/отключения ночного и пещерного масштабирования
        int checkboxY = buttonY + buttonHeight + 10;
        
        // Чекбокс для ночного масштабирования
        CheckBox nightCheckbox = new CheckBox(startX, checkboxY, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_night_scaling"), enableNightScaling.get(), 
                button -> updateDimensionCheckboxes());
        this.addRenderableWidget(nightCheckbox);
        this.checkBoxes.add(nightCheckbox);
        
        // Чекбокс для пещерного масштабирования
        CheckBox caveCheckbox = new CheckBox(startX, checkboxY + 25, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_cave_scaling"), enableCaveScaling.get(), 
                button -> updateDimensionCheckboxes());
        this.addRenderableWidget(caveCheckbox);
        this.checkBoxes.add(caveCheckbox);
        final AtomicBoolean enableGravity = new AtomicBoolean(false);
        try {
            Field enableGravityField = config.getClass().getDeclaredField("enableGravity");
            enableGravityField.setAccessible(true);
            enableGravity.set(enableGravityField.getBoolean(config));
        } catch (Exception e) {
            LOGGER.error("error getting enableGravity value", e);
        }
        
        // Чекбокс для включения/отключения модификатора гравитации
        CheckBox gravityEnabler = new CheckBox(startX, checkboxY + 50, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_gravity"), enableGravity.get(), 
                button -> updateDimensionCheckboxes());
        this.addRenderableWidget(gravityEnabler);
        this.checkBoxes.add(gravityEnabler);
        
        // Поле для высоты пещеры
        addLabel(getTranslatedText("gui.mobscaler.cave_height").getString() + ":", startX +210, checkboxY + 30);
        EditBox caveHeightBox = new EditBox(this.font, startX + 280, checkboxY + 25, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 259) { // Код клавиши Backspace
                    String text = this.getValue();
                    if (!text.isEmpty()) {
                        this.setValue(text.substring(0, text.length() - 1));
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        caveHeightBox.setValue(String.format("%.1f", caveHeight.get()).replace(",", "."));
        this.addRenderableWidget(caveHeightBox);
        this.textFields.add(caveHeightBox);

        // Добавляем маппинг для высоты пещеры
        FieldMapping caveHeightMapping = new FieldMapping("caveHeight", getTranslatedText("gui.mobscaler.cave_height").getString());
        caveHeightMapping.setEditBox(caveHeightBox);
        this.fieldMappings.add(caveHeightMapping);
         // Получаем значение множителя гравитации
         final AtomicReference<Double> gravityMultiplier = new AtomicReference<>(1.0);
         try {
             Field gravityField = config.getClass().getDeclaredField("gravityMultiplier");
             gravityField.setAccessible(true);
             gravityMultiplier.set(gravityField.getDouble(config));
         } catch (Exception e) {
             LOGGER.error("error getting gravity multiplier value", e);
         }
         
         // Поле для множителя гравитации (справа от высоты пещеры)
         addLabel(getTranslatedText("gui.mobscaler.gravity_multiplier").getString(), startX + 205, checkboxY + 55);
         EditBox gravityBox = new EditBox(this.font, startX + 280, checkboxY + 50, 35, 20, Component.literal("")) {
             @Override
             public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                 if (keyCode == 259) { // Код клавиши Backspace
                     String text = this.getValue();
                     if (!text.isEmpty()) {
                         this.setValue(text.substring(0, text.length() - 1));
                         return true;
                     }
                 }
                 return super.keyPressed(keyCode, scanCode, modifiers);
             }
             
             @Override
             public void insertText(String text) {
                 // Заменяем запятые на точки при вводе
                 text = text.replace(",", ".");
                 // Проверяем, что после вставки текста значение останется допустимым числом
                 String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                 if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                     super.insertText(text);
                 }
             }
         };
         gravityBox.setValue(String.format("%.2f", gravityMultiplier.get()).replace(",", "."));
         this.addRenderableWidget(gravityBox);
         this.textFields.add(gravityBox);
         
         // Добавляем маппинг для множителя гравитации
         FieldMapping gravityMapping = new FieldMapping("gravityMultiplier", getTranslatedText("gui.mobscaler.gravity_multiplier").getString());
         gravityMapping.setEditBox(gravityBox);
         this.fieldMappings.add(gravityMapping);

        // Добавляем секцию черных списков
        int blacklistY = checkboxY + 80;
        addLabel(getTranslatedText("gui.mobscaler.blacklists").getString() + ":", startX, blacklistY);
        
        // Заголовки для черных списков
        addLabel(getTranslatedText("gui.mobscaler.tabs.mods").getString() + ":", startX + 10, blacklistY + 25);
        addLabel(getTranslatedText("gui.mobscaler.tabs.mobs").getString() + ":", startX + 150, blacklistY + 25);
        
        // Получаем текущие черные списки
        List<String> modBlacklist = new ArrayList<>();
        List<String> entityBlacklist = new ArrayList<>();
        
        try {
            Field modBlacklistField = config.getClass().getDeclaredField("modBlacklist");
            modBlacklistField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> tempModBlacklist = (List<String>) modBlacklistField.get(config);
            modBlacklist = tempModBlacklist;
            
            Field entityBlacklistField = config.getClass().getDeclaredField("entityBlacklist");
            entityBlacklistField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> tempEntityBlacklist = (List<String>) entityBlacklistField.get(config);
            entityBlacklist = tempEntityBlacklist;
        } catch (Exception e) {
            LOGGER.error("error getting blacklist values", e);
        }
        
        // Кнопки для добавления в черные списки
        Button addModButton = new StyledButton(startX + 10, blacklistY + 45, 100, 20,
                getTranslatedText("gui.mobscaler.add_to_blacklist.mod"), button -> showAddModToBlacklistDialog(), 
                ADD_BUTTON_COLOR, 0xFF55FF55);
        this.addRenderableWidget(addModButton);
        
        Button addEntityButton = new StyledButton(startX + 200, blacklistY + 45, 100, 20,
                getTranslatedText("gui.mobscaler.add_to_blacklist.entity"), button -> showAddEntityToBlacklistDialog(), 
                ADD_BUTTON_COLOR, 0xFF55FF55);
        this.addRenderableWidget(addEntityButton);
        
        // Создаем прокручиваемые списки для модов и мобов
        int listY = blacklistY + 70;
        int listHeight = 150; // Высота области прокрутки
        
        // Создаем прокручиваемый список для модов
        BlacklistScrollPanel modBlacklistPanel = new BlacklistScrollPanel(
            startX + 10, listY, 130, listHeight, modBlacklist, true);
        this.addRenderableWidget(modBlacklistPanel);
        
        // Создаем прокручиваемый список для мобов
        BlacklistScrollPanel entityBlacklistPanel = new BlacklistScrollPanel(
            startX + 200, listY, 130, listHeight, entityBlacklist, false);
        this.addRenderableWidget(entityBlacklistPanel);

        // Добавляем поля для настройки атрибутов в зависимости от выбранного типа
        int fieldsX = startX + (buttonWidth + buttonSpacing) * 3 + 5; // Начинаем правее последней кнопки
        int fieldsY = buttonY; // Начинаем на той же высоте, что и кнопки

        // Определяем префикс полей в зависимости от типа настроек
        String fieldPrefix = "";
        String titleSuffix = "";

        switch (currentSettingsType) {
            case NIGHT:
                fieldPrefix = "Night";
                titleSuffix = getTranslatedText("gui.mobscaler.settings.night_suffix").getString();
                break;
            case CAVE:
                fieldPrefix = "Cave";
                titleSuffix = getTranslatedText("gui.mobscaler.settings.cave_suffix").getString();
                break;
            default:
                fieldPrefix = "";
                titleSuffix = getTranslatedText("gui.mobscaler.settings.basic_suffix").getString();
                break;
        }

        // Добавляем заголовок для текущего режима настроек
        addLabel(getTranslatedText("gui.mobscaler.attributes.title").getString() + titleSuffix, fieldsX, fieldsY);

        // Добавляем поля для настройки атрибутов
        final int FIELD_SPACING = 25;
        
        // Используем разные атрибуты в зависимости от выбранного режима отображения
        if (currentAttributeDisplayType == AttributeDisplayType.BASIC) {
            // Базовые атрибуты
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING, getTranslatedText("gui.mobscaler.health").getString(), fieldPrefix + "Health", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 2, getTranslatedText("gui.mobscaler.armor").getString(), fieldPrefix + "Armor", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 3, getTranslatedText("gui.mobscaler.damage").getString(), fieldPrefix + "Damage", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 4, getTranslatedText("gui.mobscaler.speed").getString(), fieldPrefix + "Speed", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 5, getTranslatedText("gui.mobscaler.knockback_resistance").getString(), fieldPrefix + "KnockbackResistance", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 6, getTranslatedText("gui.mobscaler.attack_knockback").getString(), fieldPrefix + "AttackKnockback", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 7, getTranslatedText("gui.mobscaler.attack_speed").getString(), fieldPrefix + "AttackSpeed", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 8, getTranslatedText("gui.mobscaler.follow_range").getString(), fieldPrefix + "FollowRange", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 9, getTranslatedText("gui.mobscaler.flying_speed").getString(), fieldPrefix + "FlyingSpeed", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 10, getTranslatedText("gui.mobscaler.armor_toughness").getString(), fieldPrefix + "ArmorToughness", config);
        } else {
            // Дополнительные атрибуты
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING, getTranslatedText("gui.mobscaler.luck").getString(), fieldPrefix + "Luck", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 2, getTranslatedText("gui.mobscaler.swim_speed").getString(), fieldPrefix + "SwimSpeed", config);
            addDimensionField(fieldsX, fieldsY + FIELD_SPACING * 3, getTranslatedText("gui.mobscaler.reach_distance").getString(), fieldPrefix + "ReachDistance", config);
        }
    }
    
    private void deleteDimension(String dimensionId) {
        try {
            // Проверяем, не пытаемся ли удалить стандартное измерение
            if (dimensionId.equals("minecraft:overworld") || 
                dimensionId.equals("minecraft:the_nether") || 
                dimensionId.equals("minecraft:the_end")) {
                Minecraft.getInstance().player.displayClientMessage(
                        getTranslatedText("gui.mobscaler.error.cannot_delete_standard_dimension", dimensionId), false);
                return;
            }
            
            // Удаляем измерение из копии конфигурации
            dimensionConfigsCopy.remove(dimensionId);
            
            // Удаляем измерение из основного хранилища
            DimensionConfigManager.getDimensionConfigs().remove(dimensionId);
            
            // Сохраняем изменения в файл
            DimensionConfigManager.saveConfigs();
            
            // Если удаленное измерение было выбрано, сбрасываем выбор
            if (dimensionId.equals(selectedDimension)) {
                selectedDimension = null;
            }
            
            // Обновляем интерфейс
            this.init();
            
            // Показываем сообщение об успешном удалении
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.dimension_deleted").getString()  + dimensionId), false);
            
            LOGGER.info(getTranslatedText("gui.mobscaler.dimension_deleted").getString() + dimensionId);
        } catch (Exception e) {
            LOGGER.error(getTranslatedText("gui.mobscaler.error.dimension_delete").getString() + dimensionId, e);
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.error.dimension_delete").getString() + e.getMessage()), false);
        }
    }
    
    private void saveChanges() {
        // Сохраняем изменения в зависимости от текущей вкладки
        switch (currentTab) {
            case MODS:
                saveModChanges();
                break;
            case INDIVIDUAL_MOBS:
                saveMobChanges();
                break;
            case DIMENSIONS:
                saveDimensionChanges();
                break;
            case DIFFICULTY:
                saveDifficultySettings();
                break;
            case PLAYER:
                savePlayerSettings();
                break;
        }
        
        // Показываем сообщение об успешном сохранении
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.settings_saved"), false);
        }
    }
    
    private void saveModChanges() {
        try {
            if (selectedMod != null) {
                IndividualMobAttributes config = modConfigsCopy.get(selectedMod);
                if (config != null) {
                    
                    // Сохраняем значения из всех полей ввода напрямую в config
                    for (FieldMapping mapping : fieldMappings) {
                        String fieldName = mapping.getFieldName().substring(0, 1).toLowerCase() + 
                                         mapping.getFieldName().substring(1);
                        EditBox editBox = mapping.getEditBox();
                        
                        if (editBox != null) {
                            try {
                                String value = editBox.getValue().trim().replace(",", ".");
                                if (!value.isEmpty()) {
                                    double numValue = Double.parseDouble(value);
                                    
                                    // Получаем доступ к полю напрямую в config
                                    Field field = config.getClass().getDeclaredField(fieldName);
                                    field.setAccessible(true);
                                    field.setDouble(config, numValue);
                                    
                                }
                            } catch (Exception e) {
                                LOGGER.error("error saving field {}: {} ({})", 
                                    fieldName, e.getMessage(), e.getClass().getSimpleName());
                            }
                        }
                    }
                    
                    // Сохраняем состояние чекбоксов напрямую в config
                    for (CheckBox checkbox : checkBoxes) {
                        String label = checkbox.label.getString();
                        try {
                            if (label.contains(getTranslatedText("gui.mobscaler.enable_night_scaling").getString())) {
                                Field field = config.getClass().getDeclaredField("enableNightScaling");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            } else if (label.contains(getTranslatedText("gui.mobscaler.enable_cave_scaling").getString())) {
                                Field field = config.getClass().getDeclaredField("enableCaveScaling");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            } else if (label.contains(getTranslatedText("gui.mobscaler.blacklists").getString())) {
                                Field field = config.getClass().getDeclaredField("isBlacklisted");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            } else if (label.contains(getTranslatedText("gui.mobscaler.enable_gravity").getString())) {
                                Field field = config.getClass().getDeclaredField("enableGravity");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            }
                        } catch (Exception e) {
                            LOGGER.error("error saving checkbox state", e);
                        }
                    }
                    
                    // Обновляем конфигурацию в менеджерах
                    IndividualMobManager.getModConfigs().put(selectedMod, config);
                    IndividualMobConfigManager.getModConfigs().put(selectedMod, config);
                    
                    // Сохраняем изменения в файл
                    IndividualMobConfigManager.saveModConfigs();
                    
                    // Обновляем конфигурацию
                    MobScalerConfig.init();
                    
                    // Отображаем сообщение об успешном сохранении
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.mod_settings_saved"), false);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("error saving mod settings", e);
        }
    }
    
    private void saveDimensionChanges() {
        try {
            if (selectedDimension != null) {
                DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
                if (config != null) {
                    
                    // Сохраняем значения из всех полей ввода напрямую в config
                    for (FieldMapping mapping : fieldMappings) {
                        String fieldName = mapping.getFieldName();
                        EditBox editBox = mapping.getEditBox();
                        
                        if (editBox != null) {
                            try {
                                String value = editBox.getValue().trim().replace(",", ".");
                                if (!value.isEmpty()) {
                                    double numValue = Double.parseDouble(value);
                                    
                                    // Получаем доступ к полю напрямую в config
                                    Field field = config.getClass().getDeclaredField(fieldName);
                                    field.setAccessible(true);
                                    field.setDouble(config, numValue);
                                    
                                }
                            } catch (Exception e) {
                                LOGGER.error("error saving field {}: {} ({})", 
                                    fieldName, e.getMessage(), e.getClass().getSimpleName());
                            }
                        }
                    }
                    
                    // Сохраняем состояние чекбоксов напрямую в config
                    for (CheckBox checkbox : checkBoxes) {
                        String label = checkbox.label.getString();
                        try {
                            if (label.contains(getTranslatedText("gui.mobscaler.enable_night_scaling").getString())) {
                                Field field = config.getClass().getDeclaredField("enableNightScaling");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            } else if (label.contains(getTranslatedText("gui.mobscaler.enable_cave_scaling").getString())) {
                                Field field = config.getClass().getDeclaredField("enableCaveScaling");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            } else if (label.contains(getTranslatedText("gui.mobscaler.blacklists").getString())) {
                                Field field = config.getClass().getDeclaredField("isBlacklisted");
                                field.setAccessible(true);
                                field.setBoolean(config, checkbox.isChecked());
                            }
                        } catch (Exception e) {
                            LOGGER.error("error saving checkbox state", e);
                        }
                    }
                    
                    // Черные списки сохраняются автоматически, так как мы работаем с ними напрямую через рефлексию
                    // и изменяем их в самом объекте config. Логируем для отладки.
                    try {
                        Field modBlacklistField = config.getClass().getDeclaredField("modBlacklist");
                        modBlacklistField.setAccessible(true);

                        Field entityBlacklistField = config.getClass().getDeclaredField("entityBlacklist");
                        entityBlacklistField.setAccessible(true);
                    } catch (Exception e) {
                        LOGGER.error("error logging blacklist values", e);
                    }
                    
                    // Обновляем конфигурацию в менеджере
                    DimensionConfigManager.getDimensionConfigs().put(selectedDimension, config);
                    
                    // Сохраняем все конфигурации в файл
                    DimensionConfigManager.saveConfigs();
                    
                    // Обновляем конфигурацию
                    MobScalerConfig.init();
                    
                    // Отображаем сообщение об успешном сохранении
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            Component.literal(getTranslatedText("gui.mobscaler.dimension_saved").getString()), false);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("error saving dimension settings", e);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.error.dimension_save").getString() + e.getMessage()), false);
            }
        }
    }
    
    private void addModField(int x, int y, String label, String fieldName, IndividualMobAttributes config) {
        // Получаем имя атрибута для отображения
        String attributeName = getAttributeDisplayName(fieldName);
        
        // Определяем базовое имя и префикс
        String prefix = "";
        String baseName = fieldName;
        if (fieldName.startsWith("night")) {
            prefix = "Night";
            baseName = fieldName.substring(5);
            // Делаем первую букву базового имени заглавной
            baseName = baseName.substring(0, 1).toUpperCase() + baseName.substring(1);
        } else if (fieldName.startsWith("cave")) {
            prefix = "Cave";
            baseName = fieldName.substring(4);
            // Делаем первую букву базового имени заглавной
            baseName = baseName.substring(0, 1).toUpperCase() + baseName.substring(1);
        }
        
        // Формируем имена полей с правильным регистром
        String additionField = (prefix.isEmpty() ? baseName.substring(0, 1).toLowerCase() + baseName.substring(1) : 
                              prefix.toLowerCase() + baseName) + "Addition";
        String multiplierField = (prefix.isEmpty() ? baseName.substring(0, 1).toLowerCase() + baseName.substring(1) : 
                                prefix.toLowerCase() + baseName) + "Multiplier";
        
        
        // Получаем текущие значения
        double additionValue = 0.0;
        double multiplierValue = 1.0;
        
        try {
            // Получаем значения напрямую из config
            Field additionFieldObj = config.getClass().getDeclaredField(additionField);
            additionFieldObj.setAccessible(true);
            additionValue = additionFieldObj.getDouble(config);
            
            Field multiplierFieldObj = config.getClass().getDeclaredField(multiplierField);
            multiplierFieldObj.setAccessible(true);
            multiplierValue = multiplierFieldObj.getDouble(config);
            

        } catch (Exception e) {
            LOGGER.error("error getting field values for {} and {}: {}", 
                additionField, multiplierField, e.getMessage());
        }
        
        // Позиционирование элементов
        int currentX = x;
        
        // Добавляем название атрибута как метку
        addLabel(attributeName, currentX, y + 5);
        currentX += 150;
        
        // Добавляем поле для значения добавления
        EditBox additionBox = new EditBox(this.font, currentX, y, FIELD_WIDTH, 20, Component.literal(""));
        additionBox.setValue(String.format("%.1f", additionValue).replace(",", "."));
        additionBox.setFilter(input -> input.isEmpty() || input.matches("^-?\\d*\\.?\\d*$"));
        additionBox.setEditable(true);
        this.addRenderableWidget(additionBox);
        currentX += FIELD_WIDTH + SPACING;
        
        // Добавляем символ умножения
        addLabel("×", currentX - 5, y + 5);
        
        // Добавляем поле для значения умножения
        EditBox multiplierBox = new EditBox(this.font, currentX + 10, y, FIELD_WIDTH, 20, Component.literal(""));
        multiplierBox.setValue(String.format("%.1f", multiplierValue).replace(",", "."));
        multiplierBox.setFilter(input -> input.isEmpty() || input.matches("^-?\\d*\\.?\\d*$"));
        multiplierBox.setEditable(true);
        this.addRenderableWidget(multiplierBox);
        
        // Добавляем маппинги полей
        FieldMapping additionMapping = new FieldMapping(additionField, "");
        additionMapping.setEditBox(additionBox);
        fieldMappings.add(additionMapping);
        
        FieldMapping multiplierMapping = new FieldMapping(multiplierField, "");
        multiplierMapping.setEditBox(multiplierBox);
        fieldMappings.add(multiplierMapping);
        
        // Добавляем поля в список для отслеживания
        textFields.add(additionBox);
        textFields.add(multiplierBox);
    }
    
    private String getAttributeDisplayName(String fieldName) {
        // Удаляем префиксы night и cave, но не добавляем их в отображаемое имя
        String name = fieldName;
        if (name.startsWith("night")) {
            name = name.substring(5);
        } else if (name.startsWith("cave")) {
            name = name.substring(4);
        }
        
        // Преобразуем имя атрибута в читаемый вид с использованием локализации
        String attributeName = switch (name.toLowerCase()) {
            case "health" -> getTranslatedText("gui.mobscaler.health").getString().replace(":", "");
            case "armor" -> getTranslatedText("gui.mobscaler.armor").getString().replace(":", "");
            case "damage" -> getTranslatedText("gui.mobscaler.damage").getString().replace(":", "");
            case "speed" -> getTranslatedText("gui.mobscaler.speed").getString().replace(":", "");
            case "knockbackresistance" -> getTranslatedText("gui.mobscaler.knockback_resistance").getString().replace(":", "");
            case "attackknockback" -> getTranslatedText("gui.mobscaler.attack_knockback").getString().replace(":", "");
            case "attackspeed" -> getTranslatedText("gui.mobscaler.attack_speed").getString().replace(":", "");
            case "followrange" -> getTranslatedText("gui.mobscaler.follow_range").getString().replace(":", "");
            case "flyingspeed" -> getTranslatedText("gui.mobscaler.flying_speed").getString().replace(":", "");
            case "armortoughness" -> getTranslatedText("gui.mobscaler.armor_toughness").getString().replace(":", "");
            case "luck" -> getTranslatedText("gui.mobscaler.luck").getString().replace(":", "");
            case "swimspeed" -> getTranslatedText("gui.mobscaler.swim_speed").getString().replace(":", "");
            case "reachdistance" -> getTranslatedText("gui.mobscaler.reach_distance").getString().replace(":", "");
            default -> name;
        };
        
        return attributeName;
    }
    
    private void updateModCheckboxes() {
        if (selectedMod != null) {
            IndividualMobAttributes config = modConfigsCopy.get(selectedMod);
            if (config == null) return;
            
            try {
                // Обновляем значения в конфигурации
                boolean enableNightScaling = false;
                boolean enableCaveScaling = false;
                boolean enableGravity = false;
                double caveHeight = -5.0; // Значение по умолчанию
                
                // Получаем значения чекбоксов
                for (CheckBox checkbox : checkBoxes) {
                    String label = checkbox.label.getString();
                    if (label.equals(getTranslatedText("gui.mobscaler.enable_night_scaling").getString())) {
                        enableNightScaling = checkbox.isChecked();
                    } else if (label.equals(getTranslatedText("gui.mobscaler.enable_cave_scaling").getString())) {
                        enableCaveScaling = checkbox.isChecked();
                    } else if (label.equals(getTranslatedText("gui.mobscaler.enable_gravity").getString())) {
                        enableGravity = checkbox.isChecked();
                    }
                }
                
                // Получаем значение высоты пещеры из поля ввода
                for (FieldMapping mapping : fieldMappings) {
                    if (mapping.getFieldName().equals("caveHeight")) {
                        EditBox editBox = mapping.getEditBox();
                        if (editBox != null) {
                            try {
                                String value = editBox.getValue().trim().replace(",", ".");
                                if (!value.isEmpty()) {
                                    caveHeight = Double.parseDouble(value);
                                }
                            } catch (NumberFormatException e) {
                                LOGGER.error("error parsing cave height value", e);
                            }
                        }
                        break;
                    }
                }
                
                // Обновляем значения напрямую в config
                Field enableNightField = config.getClass().getDeclaredField("enableNightScaling");
                enableNightField.setAccessible(true);
                enableNightField.setBoolean(config, enableNightScaling);
                
                Field enableCaveField = config.getClass().getDeclaredField("enableCaveScaling");
                enableCaveField.setAccessible(true);
                enableCaveField.setBoolean(config, enableCaveScaling);
                
                Field enableGravityField = config.getClass().getDeclaredField("enableGravity");
                enableGravityField.setAccessible(true);
                enableGravityField.setBoolean(config, enableGravity);
                
                // Обновляем значение высоты пещеры
                Field caveHeightField = config.getClass().getDeclaredField("caveHeight");
                caveHeightField.setAccessible(true);
                caveHeightField.setDouble(config, caveHeight);
                
                // Если ночное масштабирование отключено и мы находимся на вкладке ночных настроек,
                // переключаемся на основные настройки
                if (!enableNightScaling && currentSettingsType == SettingsType.NIGHT) {
                    currentSettingsType = SettingsType.BASIC;
                }
                
                // Если пещерное масштабирование отключено и мы находимся на вкладке пещерных настроек,
                // переключаемся на основные настройки
                if (!enableCaveScaling && currentSettingsType == SettingsType.CAVE) {
                    currentSettingsType = SettingsType.BASIC;
                }
                
                // Обновляем только содержимое, не перезагружая весь интерфейс
                initModAttributesContent();
            } catch (Exception e) {
                LOGGER.error("error updating checkboxes", e);
            }
        }
    }
    
    private void updateDimensionCheckboxes() {
        if (selectedDimension != null) {
            DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
            if (config == null) return;
            
            // Обновляем значения в конфигурации
            boolean enableNightScaling = false;
            boolean enableCaveScaling = false;
            boolean enableGravity = false;
            
            // Получаем значения чекбоксов
            for (CheckBox checkbox : checkBoxes) {
                String label = checkbox.label.getString();
                if (label.equals(getTranslatedText("gui.mobscaler.enable_night_scaling").getString())) {
                    enableNightScaling = checkbox.isChecked();
                } else if (label.equals(getTranslatedText("gui.mobscaler.enable_cave_scaling").getString())) {
                    enableCaveScaling = checkbox.isChecked();
                } else if (label.equals(getTranslatedText("gui.mobscaler.enable_gravity").getString())) {
                    enableGravity = checkbox.isChecked();
                }
            }
            
            try {
                // Обновляем значения напрямую в существующей конфигурации
                // Используем рефлексию для доступа к приватным полям
                java.lang.reflect.Field enableNightField = config.getClass().getDeclaredField("enableNightScaling");
                enableNightField.setAccessible(true);
                enableNightField.setBoolean(config, enableNightScaling);
                
                java.lang.reflect.Field enableCaveField = config.getClass().getDeclaredField("enableCaveScaling");
                enableCaveField.setAccessible(true);
                enableCaveField.setBoolean(config, enableCaveScaling);
                
                java.lang.reflect.Field enableGravityField = config.getClass().getDeclaredField("enableGravity");
                enableGravityField.setAccessible(true);
                enableGravityField.setBoolean(config, enableGravity);
                
                // Если ночное масштабирование отключено и мы находимся на вкладке ночных настроек,
                // переключаемся на основные настройки
                if (!enableNightScaling && currentSettingsType == SettingsType.NIGHT) {
                    currentSettingsType = SettingsType.BASIC;
                }
                
                // Если пещерное масштабирование отключено и мы находимся на вкладке пещерных настроек,
                // переключаемся на основные настройки
                if (!enableCaveScaling && currentSettingsType == SettingsType.CAVE) {
                    currentSettingsType = SettingsType.BASIC;
                }
                
                // Обновляем только содержимое, не перезагружая весь интерфейс
                initDimensionSettingsContent();
            } catch (Exception e) {
                LOGGER.error("error updating checkboxes", e);
            }
        }
    }
    
    private void addDimensionField(int x, int y, String label, String fieldName, DimensionConfig config) {
        // Получаем имя атрибута для отображения
        String attributeName = getAttributeDisplayName(fieldName);
        
        // Определяем базовое имя и префикс
        String prefix = "";
        String baseName = fieldName;
        if (fieldName.startsWith("night")) {
            prefix = "Night";
            baseName = fieldName.substring(5);
            baseName = baseName.substring(0, 1).toUpperCase() + baseName.substring(1);
        } else if (fieldName.startsWith("cave")) {
            prefix = "Cave";
            baseName = fieldName.substring(4);
            baseName = baseName.substring(0, 1).toUpperCase() + baseName.substring(1);
        }
        
        // Формируем имена полей с правильным регистром
        String additionField = (prefix.isEmpty() ? baseName.substring(0, 1).toLowerCase() + baseName.substring(1) : 
                              prefix.toLowerCase() + baseName) + "Addition";
        String multiplierField = (prefix.isEmpty() ? baseName.substring(0, 1).toLowerCase() + baseName.substring(1) : 
                                prefix.toLowerCase() + baseName) + "Multiplier";
        
        
        // Получаем текущие значения
        double additionValue = 0.0;
        double multiplierValue = 1.0;
        
        try {
            // Получаем значения напрямую из config
            Field additionFieldObj = config.getClass().getDeclaredField(additionField);
            additionFieldObj.setAccessible(true);
            additionValue = additionFieldObj.getDouble(config);
            
            Field multiplierFieldObj = config.getClass().getDeclaredField(multiplierField);
            multiplierFieldObj.setAccessible(true);
            multiplierValue = multiplierFieldObj.getDouble(config);

        } catch (Exception e) {
            LOGGER.error("error getting field values for {} and {}: {}", 
                additionField, multiplierField, e.getMessage());
        }
        
        // Позиционирование элементов
        int currentX = x;
        
        // Добавляем название атрибута как метку
        addLabel(attributeName, currentX, y + 5);
        currentX += 150; // Было 200, уменьшаем до 150
        
        // Добавляем поле для значения добавления
        EditBox additionBox = new EditBox(this.font, currentX, y, FIELD_WIDTH, 20, Component.literal(""));
        additionBox.setValue(String.format("%.1f", additionValue).replace(",", "."));
        additionBox.setFilter(input -> input.isEmpty() || input.matches("^-?\\d*\\.?\\d*$"));
        additionBox.setEditable(true);
        this.addRenderableWidget(additionBox);
        currentX += FIELD_WIDTH + SPACING;
        
        // Добавляем символ умножения
        addLabel("×", currentX - 5, y + 5);
        
        // Добавляем поле для значения умножения
        EditBox multiplierBox = new EditBox(this.font, currentX + 10, y, FIELD_WIDTH, 20, Component.literal(""));
        multiplierBox.setValue(String.format("%.1f", multiplierValue).replace(",", "."));
        multiplierBox.setFilter(input -> input.isEmpty() || input.matches("^-?\\d*\\.?\\d*$"));
        multiplierBox.setEditable(true);
        this.addRenderableWidget(multiplierBox);
        
        // Добавляем маппинги полей
        FieldMapping additionMapping = new FieldMapping(additionField, "");
        additionMapping.setEditBox(additionBox);
        fieldMappings.add(additionMapping);
        
        FieldMapping multiplierMapping = new FieldMapping(multiplierField, "");
        multiplierMapping.setEditBox(multiplierBox);
        fieldMappings.add(multiplierMapping);
        
        // Добавляем поля в список для отслеживания
        textFields.add(additionBox);
        textFields.add(multiplierBox);
    }
    
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Рисуем полупрозрачный фон
        this.renderBackground(poseStack);
        fill(poseStack, 0, 0, this.width, this.height, BACKGROUND_COLOR);
        
        // Рисуем основной заголовок
        drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.title").getString(), this.width / 2, 15, TEXT_COLOR);
        
        // Отрисовываем панель прокрутки, если она существует, мы на вкладке модов и мод не выбран
        if (currentTab == TabType.MODS && modListPanel != null && selectedMod == null) {
            modListPanel.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        // Отрисовываем все виджеты
        super.render(poseStack, mouseX, mouseY, partialTick);
        
        // Отрисовываем заголовок только для активной вкладки
        if (currentTab == TabType.MODS) {
            if (selectedMod != null) {
                // Заголовок с названием мода отрисовывается через addLabel
                fill(poseStack, 10, 35, this.width - 10, 55, HEADER_COLOR);
            }
        } else if (currentTab == TabType.DIMENSIONS) {
            if (selectedDimension != null) {
                // Заголовок с названием измерения отрисовывается через addLabel
                fill(poseStack, 10, 35, this.width - 10, 55, HEADER_COLOR);
            }
        } else if (currentTab == TabType.DIFFICULTY) {
            // Заголовок для вкладки сложности
            fill(poseStack, 10, 35, this.width - 10, 55, HEADER_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.difficulty.settings").getString(), this.width / 2, 40, TEXT_COLOR);
        }
        
        // Отрисовываем все метки
        for (Label label : labels) {
            label.render(poseStack, this.font);
        }
        
        // Отрисовываем все элементы интерфейса
        for (EditBox field : textFields) {
            field.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        for (CheckBox checkbox : checkBoxes) {
            checkbox.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        for (Button button : contentButtons) {
            button.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        for (Button button : tabButtons) {
            button.render(poseStack, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == TabType.MODS && modListPanel != null && selectedMod == null) {
            if (modListPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (currentTab == TabType.MODS && modListPanel != null && selectedMod == null) {
            if (modListPanel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (currentTab == TabType.MODS && modListPanel != null && selectedMod == null) {
            if (modListPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentTab == TabType.MODS && modListPanel != null && selectedMod == null) {
            if (modListPanel.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private class DeleteConfirmationDialog extends Screen {
        private final String modId;
        private final Runnable onConfirm;
        private final int dialogWidth = 300;
        private final int dialogHeight = 100;

        public DeleteConfirmationDialog(String modId, Runnable onConfirm) {
            super(getTranslatedText("gui.mobscaler.confirm.delete"));
            this.modId = modId;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            
            // Кнопка подтверждения
            Button confirmButton = new StyledButton(
                centerX - buttonWidth - 5, 
                centerY + 10, 
                buttonWidth, 
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.delete"),
                button -> {
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(MobScalerScreen.this);
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(confirmButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5, 
                centerY + 10, 
                buttonWidth, 
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> Minecraft.getInstance().setScreen(MobScalerScreen.this),
                BUTTON_COLOR,
                BUTTON_HOVER_COLOR
            );
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            // Рисуем фон диалога
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + 20, HEADER_COLOR);
            
            // Рисуем текст
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.confirm.delete").getString(), this.width / 2, dialogY + 6, TEXT_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.confirm.delete.mod").getString() + " " + modId + "?", 
                             this.width / 2, dialogY + 40, TEXT_COLOR);
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    private class AddModDialog extends Screen {
        private final int dialogWidth = 400;
        private final int dialogHeight = 300;
        private EditBox modIdField;
        private List<String> suggestions = new ArrayList<>();
        private int selectedSuggestion = -1;
        private int scrollOffset = 0;
        private static final int MAX_VISIBLE_SUGGESTIONS = 8;
        
        public AddModDialog() {
            super(Component.literal(getTranslatedText("gui.mobscaler.add_mod").getString()));
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Создаем поле ввода с автодополнением (перемещаем его ниже текста)
            modIdField = new EditBox(this.font, dialogX + 20, dialogY + 60, dialogWidth - 40, 20, Component.literal(""));
            modIdField.setMaxLength(50);
            modIdField.setValue("");
            modIdField.setResponder(this::updateSuggestions);
            this.addRenderableWidget(modIdField);
            
            // Кнопка создания
            Button createButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.create"),
                button -> {
                    String modId = modIdField.getValue().trim();
                    if (!modId.isEmpty()) {
                        createNewMod(modId);
                    }
                },
                ADD_BUTTON_COLOR,
                0xFF55FF55
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> {
                    this.onClose();
                    minecraft.setScreen(MobScalerScreen.this);
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(cancelButton);
            
            // Начальное обновление подсказок
            updateSuggestions("");
        }
        
        private void updateSuggestions(String input) {
            suggestions.clear();
            selectedSuggestion = -1;
            scrollOffset = 0;
            
            // Получаем список установленных модов из Minecraft
            net.minecraftforge.fml.ModList modList = net.minecraftforge.fml.ModList.get();
            
            // Фильтруем моды по введенному тексту
            String lowercaseInput = input.toLowerCase();
            modList.getMods().forEach(modContainer -> {
                String modId = modContainer.getModId();
                if (modId.toLowerCase().contains(lowercaseInput)) {
                    suggestions.add(modId);
                }
            });
            
            // Сортируем подсказки
            Collections.sort(suggestions);
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // ESC
                Minecraft.getInstance().setScreen(MobScalerScreen.this);
                return true;
            }
            
            if (!suggestions.isEmpty()) {
                if (keyCode == 264) { // Стрелка вниз
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        scrollOffset++;
                    }
                    return true;
                }
                if (keyCode == 265) { // Стрелка вверх
                    selectedSuggestion = Math.max(selectedSuggestion - 1, -1);
                    if (selectedSuggestion < scrollOffset) {
                        scrollOffset = Math.max(0, scrollOffset - 1);
                    }
                    return true;
                }
                if (keyCode == 257 && selectedSuggestion >= 0) { // Enter
                    modIdField.setValue(suggestions.get(selectedSuggestion));
                    return true;
                }
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            // Рисуем фон диалога
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Основной фон
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Заголовок
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + 30, HEADER_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.add_mod").getString(), this.width / 2, dialogY + 10, TEXT_COLOR);
            
            // Подсказка
            drawString(poseStack, this.font, getTranslatedText("gui.mobscaler.enter_mod_id").getString(), dialogX + 15, dialogY + 40, TEXT_COLOR);
            
            // Рисуем подсказки
            if (!suggestions.isEmpty()) {
                int suggestionY = dialogY + 80;
                int maxY = dialogY + dialogHeight - 60;
                
                // Фон для списка подсказок
                fill(poseStack, dialogX + 20, suggestionY, dialogX + dialogWidth - 20, maxY, 0x80000000);
                
                for (int i = scrollOffset; i < Math.min(scrollOffset + MAX_VISIBLE_SUGGESTIONS, suggestions.size()); i++) {
                    String suggestion = suggestions.get(i);
                    boolean isSelected = i == selectedSuggestion;
                    
                    // Подсветка выбранной подсказки
                    if (isSelected) {
                        fill(poseStack, dialogX + 20, suggestionY, dialogX + dialogWidth - 20, suggestionY + 12, HIGHLIGHT_COLOR);
                    }
                    
                    drawString(poseStack, this.font, suggestion, dialogX + 25, suggestionY, TEXT_COLOR);
                    suggestionY += 12;
                }
            }
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // Сначала проверяем клики по виджетам (кнопки, поля ввода и т.д.)
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Проверяем клик по подсказкам
            if (!suggestions.isEmpty()) {
                int suggestionY = dialogY + 80;
                int maxY = dialogY + dialogHeight - 60;
                
                if (mouseX >= dialogX + 20 && mouseX <= dialogX + dialogWidth - 20 &&
                    mouseY >= suggestionY && mouseY <= maxY) {
                    int clickedIndex = scrollOffset + (int)((mouseY - suggestionY) / 12);
                    if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
                        modIdField.setValue(suggestions.get(clickedIndex));
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!suggestions.isEmpty()) {
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int)delta, 
                    Math.max(0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS)));
                return true;
            }
            return false;
        }
    }

    private class AddDimensionDialog extends Screen {
        private final int dialogWidth = 400;
        private final int dialogHeight = 300;
        private EditBox dimensionIdField;
        private List<String> suggestions = new ArrayList<>();
        private int selectedSuggestion = -1;
        private int scrollOffset = 0;
        private static final int MAX_VISIBLE_SUGGESTIONS = 8;
        
        public AddDimensionDialog() {
            super(Component.literal(getTranslatedText("gui.mobscaler.add_dimension").getString()));
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Создаем поле ввода с автодополнением
            dimensionIdField = new EditBox(this.font, dialogX + 20, dialogY + 60, dialogWidth - 40, 20, Component.literal(""));
            dimensionIdField.setMaxLength(50);
            dimensionIdField.setValue("");
            dimensionIdField.setResponder(this::updateSuggestions);
            this.addRenderableWidget(dimensionIdField);
            
            // Кнопка создания
            Button createButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.create"),
                button -> {
                    String dimensionId = dimensionIdField.getValue().trim();
                    if (!dimensionId.isEmpty()) {
                        createNewDimension(dimensionId);
                    }
                },
                ADD_BUTTON_COLOR,
                0xFF55FF55
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> {
                    this.onClose();
                    minecraft.setScreen(MobScalerScreen.this);
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(cancelButton);
            
            // Начальное обновление подсказок
            updateSuggestions("");
        }
        
        private void updateSuggestions(String input) {
            suggestions.clear();
            selectedSuggestion = -1;
            scrollOffset = 0;
            
            // Получаем список измерений из игры
            if (Minecraft.getInstance().level != null) {
                for (ServerLevel level : Minecraft.getInstance().getSingleplayerServer().getAllLevels()) {
                    String dimensionKey = level.dimension().location().toString();
                    if (dimensionKey.toLowerCase().contains(input.toLowerCase())) {
                        suggestions.add(dimensionKey);
                    }
                }
            }
            
            // Сортируем подсказки
            Collections.sort(suggestions);
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // ESC
                Minecraft.getInstance().setScreen(MobScalerScreen.this);
                return true;
            }
            
            if (!suggestions.isEmpty()) {
                if (keyCode == 264) { // Стрелка вниз
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        scrollOffset++;
                    }
                    return true;
                }
                if (keyCode == 265) { // Стрелка вверх
                    selectedSuggestion = Math.max(selectedSuggestion - 1, -1);
                    if (selectedSuggestion < scrollOffset) {
                        scrollOffset = Math.max(0, scrollOffset - 1);
                    }
                    return true;
                }
                if (keyCode == 257 && selectedSuggestion >= 0) { // Enter
                    dimensionIdField.setValue(suggestions.get(selectedSuggestion));
                    return true;
                }
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            // Рисуем фон диалога
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Основной фон
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Заголовок
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + 30, HEADER_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.add_dimension").getString(), this.width / 2, dialogY + 10, TEXT_COLOR);
            
            // Подсказка
            drawString(poseStack, this.font, getTranslatedText("gui.mobscaler.enter_dimension_id").getString(), dialogX + 15, dialogY + 40, TEXT_COLOR);
            
            // Рисуем подсказки
            if (!suggestions.isEmpty()) {
                int suggestionY = dialogY + 80;
                int maxY = dialogY + dialogHeight - 60;
                
                // Фон для списка подсказок
                fill(poseStack, dialogX + 20, suggestionY, dialogX + dialogWidth - 20, maxY, 0x80000000);
                
                for (int i = scrollOffset; i < Math.min(scrollOffset + MAX_VISIBLE_SUGGESTIONS, suggestions.size()); i++) {
                    String suggestion = suggestions.get(i);
                    boolean isSelected = i == selectedSuggestion;
                    
                    // Подсветка выбранной подсказки
                    if (isSelected) {
                        fill(poseStack, dialogX + 20, suggestionY, dialogX + dialogWidth - 20, suggestionY + 12, HIGHLIGHT_COLOR);
                    }
                    
                    drawString(poseStack, this.font, suggestion, dialogX + 25, suggestionY, TEXT_COLOR);
                    suggestionY += 12;
                }
            }
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            if (!suggestions.isEmpty()) {
                int suggestionY = dialogY + 80;
                int maxY = dialogY + dialogHeight - 60;
                
                if (mouseX >= dialogX + 20 && mouseX <= dialogX + dialogWidth - 20 &&
                    mouseY >= suggestionY && mouseY <= maxY) {
                    int clickedIndex = scrollOffset + (int)((mouseY - suggestionY) / 12);
                    if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
                        dimensionIdField.setValue(suggestions.get(clickedIndex));
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!suggestions.isEmpty()) {
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int)delta, 
                    Math.max(0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS)));
                return true;
            }
            return false;
        }
    }

    private class DeleteDimensionConfirmationDialog extends Screen {
        private final String dimensionId;
        private final Runnable onConfirm;
        private final int dialogWidth = 300;
        private final int dialogHeight = 100;

        public DeleteDimensionConfirmationDialog(String dimensionId, Runnable onConfirm) {
            super(getTranslatedText("gui.mobscaler.confirm.delete.dimension"));
            this.dimensionId = dimensionId;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            
            // Кнопка подтверждения
            Button confirmButton = new StyledButton(
                centerX - buttonWidth - 5, 
                centerY + 10, 
                buttonWidth, 
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.delete"),
                button -> {
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(MobScalerScreen.this);
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(confirmButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5, 
                centerY + 10, 
                buttonWidth, 
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> Minecraft.getInstance().setScreen(MobScalerScreen.this),
                BUTTON_COLOR,
                BUTTON_HOVER_COLOR
            );
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            // Рисуем фон диалога
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + 20, HEADER_COLOR);
            
            // Рисуем текст
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.confirm.delete").getString(), this.width / 2, dialogY + 6, TEXT_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.confirm.delete.dimension").getString() + " " + dimensionId + "?", 
                             this.width / 2, dialogY + 40, TEXT_COLOR);
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    private void initMobSettingsContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        this.fieldMappings.clear();
        this.textFields.clear();
        this.checkBoxes.clear();
        this.labels.clear(); // Очищаем все метки

        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();

        if (selectedEntity == null) return;

        IndividualMobConfig config = mobConfigsCopy.get(selectedEntity);
        if (config == null) return;

        // Заголовок с названием моба
        addLabel(getTranslatedText("gui.mobscaler.mob.settings").getString() + selectedEntity, 20, 40);

        // Кнопки для переключения типа настроек
        int buttonWidth = 100;
        int startX = 20;
        int buttonY = 60;

        // Базовые настройки
        Button basicButton = new StyledButton(startX, buttonY, buttonWidth, 20,
                getTranslatedText("gui.mobscaler.settings.basic"), button -> {
                    currentSettingsType = SettingsType.BASIC;
                    initMobSettingsContent();
                }, currentSettingsType == SettingsType.BASIC ? HIGHLIGHT_COLOR : BUTTON_COLOR,
                BUTTON_HOVER_COLOR);
        this.addRenderableWidget(basicButton);

        // Ночные настройки
        Button nightButton = new StyledButton(startX + buttonWidth + 10, buttonY, buttonWidth, 20,
                getTranslatedText("gui.mobscaler.settings.night"), button -> {
                    if (config.getAttributes().getEnableNightScaling()) {
                        currentSettingsType = SettingsType.NIGHT;
                        initMobSettingsContent();
                    }
                }, currentSettingsType == SettingsType.NIGHT ? HIGHLIGHT_COLOR : 
                   (config.getAttributes().getEnableNightScaling() ? BUTTON_COLOR : 0xFF666666),
                config.getAttributes().getEnableNightScaling() ? BUTTON_HOVER_COLOR : 0xFF666666);
        nightButton.active = config.getAttributes().getEnableNightScaling();
        this.addRenderableWidget(nightButton);

        // Пещерные настройки
        Button caveButton = new StyledButton(startX + (buttonWidth + 10) * 2, buttonY, buttonWidth, 20,
                getTranslatedText("gui.mobscaler.settings.cave"), button -> {
                    if (config.getAttributes().getEnableCaveScaling()) {
                        currentSettingsType = SettingsType.CAVE;
                        initMobSettingsContent();
                    }
                }, currentSettingsType == SettingsType.CAVE ? HIGHLIGHT_COLOR : 
                   (config.getAttributes().getEnableCaveScaling() ? BUTTON_COLOR : 0xFF666666),
                config.getAttributes().getEnableCaveScaling() ? BUTTON_HOVER_COLOR : 0xFF666666);
        caveButton.active = config.getAttributes().getEnableCaveScaling();
        this.addRenderableWidget(caveButton);

        // Кнопка для переключения типа отображения атрибутов
        Button attributeDisplayButton = new StyledButton(startX + (buttonWidth + 10) * 2, buttonY + 30, buttonWidth, 20,
                currentAttributeDisplayType == AttributeDisplayType.BASIC ? getTranslatedText("gui.mobscaler.settings.advanced") : getTranslatedText("gui.mobscaler.settings.basic"),
                button -> {
                    currentAttributeDisplayType = currentAttributeDisplayType == AttributeDisplayType.BASIC ? AttributeDisplayType.ADVANCED : AttributeDisplayType.BASIC;
                    initMobSettingsContent();
                }, BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(attributeDisplayButton);

        // Чекбоксы
        int checkboxY = buttonY + 30;
        
        // Чекбокс для ночного масштабирования
        CheckBox nightCheckbox = new CheckBox(startX, checkboxY, 200, 20,
                getTranslatedText("gui.mobscaler.enable_night_scaling"),
                config.getAttributes().getEnableNightScaling(),
                button -> {
                    CheckBox cb = (CheckBox) button;
                    boolean isChecked = cb.isChecked();
                    
                    // Обновляем состояние в конфигурации
                    try {
                        // Создаем новый объект атрибутов с обновленным значением enableNightScaling
                        IndividualMobAttributes oldAttrs = config.getAttributes();
                        IndividualMobAttributes newAttrs = new IndividualMobAttributes(
                            isChecked, // Новое значение для enableNightScaling
                            oldAttrs.getEnableCaveScaling(),
                            oldAttrs.getCaveHeight(),
                            oldAttrs.getGravityMultiplier(),
                            oldAttrs.isGravityEnabled(),
                            // Остальные параметры остаются без изменений
                            oldAttrs.getHealthAddition(), oldAttrs.getHealthMultiplier(),
                            oldAttrs.getArmorAddition(), oldAttrs.getArmorMultiplier(),
                            oldAttrs.getDamageAddition(), oldAttrs.getDamageMultiplier(),
                            oldAttrs.getSpeedAddition(), oldAttrs.getSpeedMultiplier(),
                            oldAttrs.getKnockbackResistanceAddition(), oldAttrs.getKnockbackResistanceMultiplier(),
                            oldAttrs.getAttackKnockbackAddition(), oldAttrs.getAttackKnockbackMultiplier(),
                            oldAttrs.getAttackSpeedAddition(), oldAttrs.getAttackSpeedMultiplier(),
                            oldAttrs.getFollowRangeAddition(), oldAttrs.getFollowRangeMultiplier(),
                            oldAttrs.getFlyingSpeedAddition(), oldAttrs.getFlyingSpeedMultiplier(),
                            oldAttrs.getArmorToughnessAddition(), oldAttrs.getArmorToughnessMultiplier(),
                            oldAttrs.getLuckAddition(), oldAttrs.getLuckMultiplier(),
                            oldAttrs.getSwimSpeedAddition(), oldAttrs.getSwimSpeedMultiplier(),
                            oldAttrs.getReachDistanceAddition(), oldAttrs.getReachDistanceMultiplier(),
                            // Ночные атрибуты
                            oldAttrs.getNightHealthAddition(), oldAttrs.getNightHealthMultiplier(),
                            oldAttrs.getNightArmorAddition(), oldAttrs.getNightArmorMultiplier(),
                            oldAttrs.getNightDamageAddition(), oldAttrs.getNightDamageMultiplier(),
                            oldAttrs.getNightSpeedAddition(), oldAttrs.getNightSpeedMultiplier(),
                            oldAttrs.getNightKnockbackResistanceAddition(), oldAttrs.getNightKnockbackResistanceMultiplier(),
                            oldAttrs.getNightAttackKnockbackAddition(), oldAttrs.getNightAttackKnockbackMultiplier(),
                            oldAttrs.getNightAttackSpeedAddition(), oldAttrs.getNightAttackSpeedMultiplier(),
                            oldAttrs.getNightFollowRangeAddition(), oldAttrs.getNightFollowRangeMultiplier(),
                            oldAttrs.getNightFlyingSpeedAddition(), oldAttrs.getNightFlyingSpeedMultiplier(),
                            oldAttrs.getNightArmorToughnessAddition(), oldAttrs.getNightArmorToughnessMultiplier(),
                            oldAttrs.getNightLuckAddition(), oldAttrs.getNightLuckMultiplier(),
                            oldAttrs.getNightSwimSpeedAddition(), oldAttrs.getNightSwimSpeedMultiplier(),
                            oldAttrs.getNightReachDistanceAddition(), oldAttrs.getNightReachDistanceMultiplier(),
                            // Пещерные атрибуты
                            oldAttrs.getCaveHealthAddition(), oldAttrs.getCaveHealthMultiplier(),
                            oldAttrs.getCaveArmorAddition(), oldAttrs.getCaveArmorMultiplier(),
                            oldAttrs.getCaveDamageAddition(), oldAttrs.getCaveDamageMultiplier(),
                            oldAttrs.getCaveSpeedAddition(), oldAttrs.getCaveSpeedMultiplier(),
                            oldAttrs.getCaveKnockbackResistanceAddition(), oldAttrs.getCaveKnockbackResistanceMultiplier(),
                            oldAttrs.getCaveAttackKnockbackAddition(), oldAttrs.getCaveAttackKnockbackMultiplier(),
                            oldAttrs.getCaveAttackSpeedAddition(), oldAttrs.getCaveAttackSpeedMultiplier(),
                            oldAttrs.getCaveFollowRangeAddition(), oldAttrs.getCaveFollowRangeMultiplier(),
                            oldAttrs.getCaveFlyingSpeedAddition(), oldAttrs.getCaveFlyingSpeedMultiplier(),
                            oldAttrs.getCaveArmorToughnessAddition(), oldAttrs.getCaveArmorToughnessMultiplier(),
                            oldAttrs.getCaveLuckAddition(), oldAttrs.getCaveLuckMultiplier(),
                            oldAttrs.getCaveSwimSpeedAddition(), oldAttrs.getCaveSwimSpeedMultiplier(),
                            oldAttrs.getCaveReachDistanceAddition(), oldAttrs.getCaveReachDistanceMultiplier(),
                            oldAttrs.isBlacklisted()
                        );
                        
                        // Создаем новый конфиг с обновленными атрибутами
                        IndividualMobConfig newConfig = new IndividualMobConfig(config.isBlacklisted(), newAttrs);
                        
                        // Обновляем конфиг в копии
                        mobConfigsCopy.put(selectedEntity, newConfig);
                        
                        // Если ночное масштабирование отключено и мы на вкладке ночных настроек,
                        // переключаемся на базовые настройки
                        if (!isChecked && currentSettingsType == SettingsType.NIGHT) {
                            currentSettingsType = SettingsType.BASIC;
                        }
                        
                        // Перерисовываем интерфейс
                        initMobSettingsContent();
                    } catch (Exception e) {
                        LOGGER.error("error updating enableNightScaling", e);
                    }
                });
        this.addRenderableWidget(nightCheckbox);
        this.checkBoxes.add(nightCheckbox);
        
        // Чекбокс для пещерного масштабирования
        CheckBox caveCheckbox = new CheckBox(startX, checkboxY + 25, 200, 20,
                getTranslatedText("gui.mobscaler.enable_cave_scaling"),
                config.getAttributes().getEnableCaveScaling(),
                button -> {
                    CheckBox cb = (CheckBox) button;
                    boolean isChecked = cb.isChecked();
                    
                    // Обновляем состояние в конфигурации
                    try {
                        // Создаем новый объект атрибутов с обновленным значением enableCaveScaling
                        IndividualMobAttributes oldAttrs = config.getAttributes();
                        IndividualMobAttributes newAttrs = new IndividualMobAttributes(
                            oldAttrs.getEnableNightScaling(),
                            isChecked, // Новое значение для enableCaveScaling
                            oldAttrs.getCaveHeight(),
                            oldAttrs.getGravityMultiplier(),
                            oldAttrs.isGravityEnabled(),
                            // Остальные параметры остаются без изменений
                            oldAttrs.getHealthAddition(), oldAttrs.getHealthMultiplier(),
                            oldAttrs.getArmorAddition(), oldAttrs.getArmorMultiplier(),
                            oldAttrs.getDamageAddition(), oldAttrs.getDamageMultiplier(),
                            oldAttrs.getSpeedAddition(), oldAttrs.getSpeedMultiplier(),
                            oldAttrs.getKnockbackResistanceAddition(), oldAttrs.getKnockbackResistanceMultiplier(),
                            oldAttrs.getAttackKnockbackAddition(), oldAttrs.getAttackKnockbackMultiplier(),
                            oldAttrs.getAttackSpeedAddition(), oldAttrs.getAttackSpeedMultiplier(),
                            oldAttrs.getFollowRangeAddition(), oldAttrs.getFollowRangeMultiplier(),
                            oldAttrs.getFlyingSpeedAddition(), oldAttrs.getFlyingSpeedMultiplier(),
                            oldAttrs.getArmorToughnessAddition(), oldAttrs.getArmorToughnessMultiplier(),
                            oldAttrs.getLuckAddition(), oldAttrs.getLuckMultiplier(),
                            oldAttrs.getSwimSpeedAddition(), oldAttrs.getSwimSpeedMultiplier(),
                            oldAttrs.getReachDistanceAddition(), oldAttrs.getReachDistanceMultiplier(),
                            // Ночные атрибуты
                            oldAttrs.getNightHealthAddition(), oldAttrs.getNightHealthMultiplier(),
                            oldAttrs.getNightArmorAddition(), oldAttrs.getNightArmorMultiplier(),
                            oldAttrs.getNightDamageAddition(), oldAttrs.getNightDamageMultiplier(),
                            oldAttrs.getNightSpeedAddition(), oldAttrs.getNightSpeedMultiplier(),
                            oldAttrs.getNightKnockbackResistanceAddition(), oldAttrs.getNightKnockbackResistanceMultiplier(),
                            oldAttrs.getNightAttackKnockbackAddition(), oldAttrs.getNightAttackKnockbackMultiplier(),
                            oldAttrs.getNightAttackSpeedAddition(), oldAttrs.getNightAttackSpeedMultiplier(),
                            oldAttrs.getNightFollowRangeAddition(), oldAttrs.getNightFollowRangeMultiplier(),
                            oldAttrs.getNightFlyingSpeedAddition(), oldAttrs.getNightFlyingSpeedMultiplier(),
                            oldAttrs.getNightArmorToughnessAddition(), oldAttrs.getNightArmorToughnessMultiplier(),
                            oldAttrs.getNightLuckAddition(), oldAttrs.getNightLuckMultiplier(),
                            oldAttrs.getNightSwimSpeedAddition(), oldAttrs.getNightSwimSpeedMultiplier(),
                            oldAttrs.getNightReachDistanceAddition(), oldAttrs.getNightReachDistanceMultiplier(),
                            // Пещерные атрибуты
                            oldAttrs.getCaveHealthAddition(), oldAttrs.getCaveHealthMultiplier(),
                            oldAttrs.getCaveArmorAddition(), oldAttrs.getCaveArmorMultiplier(),
                            oldAttrs.getCaveDamageAddition(), oldAttrs.getCaveDamageMultiplier(),
                            oldAttrs.getCaveSpeedAddition(), oldAttrs.getCaveSpeedMultiplier(),
                            oldAttrs.getCaveKnockbackResistanceAddition(), oldAttrs.getCaveKnockbackResistanceMultiplier(),
                            oldAttrs.getCaveAttackKnockbackAddition(), oldAttrs.getCaveAttackKnockbackMultiplier(),
                            oldAttrs.getCaveAttackSpeedAddition(), oldAttrs.getCaveAttackSpeedMultiplier(),
                            oldAttrs.getCaveFollowRangeAddition(), oldAttrs.getCaveFollowRangeMultiplier(),
                            oldAttrs.getCaveFlyingSpeedAddition(), oldAttrs.getCaveFlyingSpeedMultiplier(),
                            oldAttrs.getCaveArmorToughnessAddition(), oldAttrs.getCaveArmorToughnessMultiplier(),
                            oldAttrs.getCaveLuckAddition(), oldAttrs.getCaveLuckMultiplier(),
                            oldAttrs.getCaveSwimSpeedAddition(), oldAttrs.getCaveSwimSpeedMultiplier(),
                            oldAttrs.getCaveReachDistanceAddition(), oldAttrs.getCaveReachDistanceMultiplier(),
                            oldAttrs.isBlacklisted()
                        );
                        
                        // Создаем новый конфиг с обновленными атрибутами
                        IndividualMobConfig newConfig = new IndividualMobConfig(config.isBlacklisted(), newAttrs);
                        
                        // Обновляем конфиг в копии
                        mobConfigsCopy.put(selectedEntity, newConfig);
                        
                        // Если пещерное масштабирование отключено и мы на вкладке пещерных настроек,
                        // переключаемся на базовые настройки
                        if (!isChecked && currentSettingsType == SettingsType.CAVE) {
                            currentSettingsType = SettingsType.BASIC;
                        }
                        
                        // Перерисовываем интерфейс
                        initMobSettingsContent();
        } catch (Exception e) {
                        LOGGER.error("error updating enableCaveScaling", e);
                    }
                });
        this.addRenderableWidget(caveCheckbox);
        this.checkBoxes.add(caveCheckbox);
        
        // Определяем префикс для полей в зависимости от типа настроек
        String prefix = "";
        switch (currentSettingsType) {
            case NIGHT:
                prefix = "night";
                break;
            case CAVE:
                prefix = "cave";
                break;
            default:
                prefix = "";
                break;
        }

        // Массив атрибутов: [имя_поля, отображаемое_имя]
        List<String[]> attributesToDisplay = new ArrayList<>();
        
        // Выбираем атрибуты в зависимости от текущего режима отображения
        if (currentAttributeDisplayType == AttributeDisplayType.BASIC) {
            // Базовые атрибуты для настроек
            attributesToDisplay.add(new String[]{"Health", getTranslatedText("gui.mobscaler.health").getString()});
            attributesToDisplay.add(new String[]{"Armor", getTranslatedText("gui.mobscaler.armor").getString()});
            attributesToDisplay.add(new String[]{"Damage", getTranslatedText("gui.mobscaler.damage").getString()});
            attributesToDisplay.add(new String[]{"Speed", getTranslatedText("gui.mobscaler.speed").getString()});
            attributesToDisplay.add(new String[]{"KnockbackResistance", getTranslatedText("gui.mobscaler.knockback_resistance").getString()});
            attributesToDisplay.add(new String[]{"AttackKnockback", getTranslatedText("gui.mobscaler.attack_knockback").getString()});
            attributesToDisplay.add(new String[]{"AttackSpeed", getTranslatedText("gui.mobscaler.attack_speed").getString()});
            attributesToDisplay.add(new String[]{"FollowRange", getTranslatedText("gui.mobscaler.follow_range").getString()});
            attributesToDisplay.add(new String[]{"FlyingSpeed", getTranslatedText("gui.mobscaler.flying_speed").getString()});
            attributesToDisplay.add(new String[]{"ArmorToughness", getTranslatedText("gui.mobscaler.armor_toughness").getString()});
        } else {
            // Дополнительные атрибуты
            attributesToDisplay.add(new String[]{"Luck", getTranslatedText("gui.mobscaler.luck").getString()});
            attributesToDisplay.add(new String[]{"SwimSpeed", getTranslatedText("gui.mobscaler.swim_speed").getString()});
            attributesToDisplay.add(new String[]{"ReachDistance", getTranslatedText("gui.mobscaler.reach_distance").getString()});
        }

        // Добавляем поля для атрибутов
        int fieldsStartY = checkboxY -30; // 
        int fieldSpacing = 25;
        int currentY = fieldsStartY;
        int rightPanelX = startX + 332; // Размещаем атрибуты правее кнопок

        // Добавляем заголовок для атрибутов (только один раз)
        String settingsTypeText = currentSettingsType == SettingsType.BASIC ? getTranslatedText("gui.mobscaler.settings.basic").getString() : 
                                 (currentSettingsType == SettingsType.NIGHT ? getTranslatedText("gui.mobscaler.settings.night").getString() : getTranslatedText("gui.mobscaler.settings.cave").getString());
        
        String attributeTypeText = currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                                  getTranslatedText("gui.mobscaler.basic").getString() : getTranslatedText("gui.mobscaler.advanced").getString();
        addLabel(getTranslatedText("gui.mobscaler.attributes").getString() + " " + settingsTypeText + " (" + attributeTypeText + ")", rightPanelX, fieldsStartY - 20);
        
        for (String[] attr : attributesToDisplay) {
            String fieldName = prefix + attr[0];
            String displayName = attr[1];

            // Добавляем название атрибута
            addLabel(displayName, rightPanelX, currentY + 5);

            // Добавляем поле Addition
            EditBox additionField = createAttributeField(
                rightPanelX + 170, currentY, 
                fieldName + "Addition",
                config.getAttributes()
            );
            addRenderableWidget(additionField);
            textFields.add(additionField);
            FieldMapping additionMapping = new FieldMapping(fieldName + "Addition", displayName);
            additionMapping.setEditBox(additionField);
            fieldMappings.add(additionMapping);

            // Добавляем знак умножения
            addLabel("×", rightPanelX + 215, currentY + 5);

            // Добавляем поле Multiplier
            EditBox multiplierField = createAttributeField(
                rightPanelX + 230, currentY,
                fieldName + "Multiplier",
                config.getAttributes()
            );
            addRenderableWidget(multiplierField);
            textFields.add(multiplierField);
            FieldMapping multiplierMapping = new FieldMapping(fieldName + "Multiplier", displayName);
            multiplierMapping.setEditBox(multiplierField);
            fieldMappings.add(multiplierMapping);

            currentY += fieldSpacing;
        }
        
        // Добавляем информацию о текущем состоянии моба
        int formulaX = startX;
        // Фиксированное положение заголовка формул
        int formulaY = buttonY + 120;
        addLabel(getTranslatedText("gui.mobscaler.formulas").getString(), formulaX, formulaY);
        
        // Добавляем формулы расчета для здоровья, брони и урона
        double healthAddition = 0;
        double healthMultiplier = 1.0;
        double armorAddition = 0;
        double armorMultiplier = 1.0;
        double armorToughnessAddition = 0;
        double armorToughnessMultiplier = 1.0;
        double damageAddition = 0;
        double damageMultiplier = 1.0;
        
        // Получаем значения в зависимости от типа настроек
        switch (currentSettingsType) {
            case BASIC:
                healthAddition = config.getAttributes().getHealthAddition();
                healthMultiplier = config.getAttributes().getHealthMultiplier();
                armorAddition = config.getAttributes().getArmorAddition();
                armorMultiplier = config.getAttributes().getArmorMultiplier();
                armorToughnessAddition = config.getAttributes().getArmorToughnessAddition();
                armorToughnessMultiplier = config.getAttributes().getArmorToughnessMultiplier();
                damageAddition = config.getAttributes().getDamageAddition();
                damageMultiplier = config.getAttributes().getDamageMultiplier();
                break;
            case NIGHT:
                healthAddition = config.getAttributes().getNightHealthAddition();
                healthMultiplier = config.getAttributes().getNightHealthMultiplier();
                armorAddition = config.getAttributes().getNightArmorAddition();
                armorMultiplier = config.getAttributes().getNightArmorMultiplier();
                armorToughnessAddition = config.getAttributes().getNightArmorToughnessAddition();
                armorToughnessMultiplier = config.getAttributes().getNightArmorToughnessMultiplier();
                damageAddition = config.getAttributes().getNightDamageAddition();
                damageMultiplier = config.getAttributes().getNightDamageMultiplier();
                break;
            case CAVE:
                healthAddition = config.getAttributes().getCaveHealthAddition();
                healthMultiplier = config.getAttributes().getCaveHealthMultiplier();
                armorAddition = config.getAttributes().getCaveArmorAddition();
                armorMultiplier = config.getAttributes().getCaveArmorMultiplier();
                armorToughnessAddition = config.getAttributes().getCaveArmorToughnessAddition();
                armorToughnessMultiplier = config.getAttributes().getCaveArmorToughnessMultiplier();
                damageAddition = config.getAttributes().getCaveDamageAddition();
                damageMultiplier = config.getAttributes().getCaveDamageMultiplier();
                break;
        }
        
        // Используем фиксированные позиции для всех формул
        int formulaLabelX = formulaX;
        int formulaValueX = formulaX + 65;
        
        // Здоровье
        addLabel(getTranslatedText("gui.mobscaler.health").getString() + ":", formulaLabelX, formulaY + 25);
        addAttributeFormula(formulaValueX, formulaY + 25, selectedEntity, "Health", healthAddition, healthMultiplier);
        
        // Броня
        addLabel(getTranslatedText("gui.mobscaler.armor").getString() + ":", formulaLabelX, formulaY + 50);
        addAttributeFormula(formulaValueX, formulaY + 50, selectedEntity, "Armor", armorAddition, armorMultiplier);
        
        // Урон
        addLabel(getTranslatedText("gui.mobscaler.damage").getString() + ":", formulaLabelX, formulaY + 75);
        addAttributeFormula(formulaValueX, formulaY + 75, selectedEntity, "Damage", damageAddition, damageMultiplier);
        
        // Урон с мечом (атака 8)
        addLabel(getTranslatedText("gui.mobscaler.sword_damage").getString() + ":", formulaLabelX, formulaY + 100);
        try {
            double baseValue = getBaseAttributeValue(selectedEntity, "Damage");
            double finalDamage = (baseValue + damageAddition) * damageMultiplier;
            double swordDamage = 8.0; // Значение атаки меча
            
            double armorValue = getBaseAttributeValue(selectedEntity, "Armor");
            double finalArmor = (armorValue + armorAddition) * armorMultiplier;
            
            double armorToughnessValue = getBaseAttributeValue(selectedEntity, "ArmorToughness");
            double finalArmorToughness = (armorToughnessValue + armorToughnessAddition) * armorToughnessMultiplier;
            
            // Расчет урона по формуле Minecraft (упрощенно)
            // Урон = Урон меча * (1 - min(20, max(Броня/5, Броня - Урон/(2 + Жесткость/4)))/25)
            double damageReduction = Math.min(20, Math.max(finalArmor/5, finalArmor - swordDamage/(2 + finalArmorToughness/4)))/25;
            double effectiveDamage = swordDamage * (1 - damageReduction);
            
            String formula = String.format("%s: %.1f | %s: %.1f | %s: %.1f | %s: %.1f", 
            getTranslatedText("gui.mobscaler.mob").getString(),
            finalDamage, 
            getTranslatedText("gui.mobscaler.sword").getString(),
            swordDamage,
            getTranslatedText("gui.mobscaler.armor").getString(),
            finalArmor,
            getTranslatedText("gui.mobscaler.result").getString(),
            effectiveDamage);
            addLabel(formula, formulaValueX, formulaY + 125);
        } catch (Exception e) {
            LOGGER.error("Error calculating sword damage: ", e);
            addLabel("Error calculating sword damage", formulaValueX, formulaY + 125);
        }
        // Поле для высоты пещеры (показываем только если включено пещерное масштабирование)
        if (config.getAttributes().getEnableCaveScaling()) {
            addLabel(getTranslatedText("gui.mobscaler.cave_height").getString(), startX + 210, checkboxY + 25);
            EditBox caveHeightBox = new EditBox(this.font, startX + 280, checkboxY + 20, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 259) { // Код клавиши Backspace
                    String text = this.getValue();
                    if (!text.isEmpty()) {
                        this.setValue(text.substring(0, text.length() - 1));
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
            caveHeightBox.setValue(String.format("%.1f", config.getAttributes().getCaveHeight()).replace(",", "."));
            this.addRenderableWidget(caveHeightBox);
            this.textFields.add(caveHeightBox);

            // Добавляем маппинг для высоты пещеры
            FieldMapping caveHeightMapping = new FieldMapping("caveHeight", getTranslatedText("gui.mobscaler.cave_height").getString());
            caveHeightMapping.setEditBox(caveHeightBox);
            this.fieldMappings.add(caveHeightMapping);
        }
        
        // Добавляем поле для множителя гравитации
        addLabel(getTranslatedText("gui.mobscaler.gravity_multiplier").getString(), startX + 210, checkboxY + 50);
        EditBox gravityMultiplierBox = new EditBox(this.font, startX + 280, checkboxY + 45, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 259) { // Код клавиши Backspace
                    String text = this.getValue();
                    if (!text.isEmpty()) {
                        this.setValue(text.substring(0, text.length() - 1));
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        gravityMultiplierBox.setValue(String.format("%.1f", config.getAttributes().getGravityMultiplier()).replace(",", "."));
        this.addRenderableWidget(gravityMultiplierBox);
        this.textFields.add(gravityMultiplierBox);

        // Добавляем маппинг для множителя гравитации
        FieldMapping gravityMapping = new FieldMapping("gravityMultiplier", getTranslatedText("gui.mobscaler.gravity_multiplier").getString());
        gravityMapping.setEditBox(gravityMultiplierBox);
        this.fieldMappings.add(gravityMapping);
        
        // Чекбокс для включения гравитации
        CheckBox gravityCheckbox = new CheckBox(startX, checkboxY + 50, 200, 20,
                getTranslatedText("gui.mobscaler.enable_gravity"),
                config.getAttributes().isGravityEnabled(),
                button -> {
                    CheckBox cb = (CheckBox) button;
                    boolean isChecked = cb.isChecked();
                    
                    // Обновляем состояние в конфигурации
                    try {
                        // Создаем новый объект атрибутов с обновленным значением gravityEnabled
                        IndividualMobAttributes oldAttrs = config.getAttributes();
                        IndividualMobAttributes newAttrs = new IndividualMobAttributes(
                            oldAttrs.getEnableNightScaling(),
                            oldAttrs.getEnableCaveScaling(),
                            oldAttrs.getCaveHeight(),
                            oldAttrs.getGravityMultiplier(),
                            isChecked, // Новое значение для gravityEnabled
                            // Остальные параметры остаются без изменений
                            oldAttrs.getHealthAddition(), oldAttrs.getHealthMultiplier(),
                            oldAttrs.getArmorAddition(), oldAttrs.getArmorMultiplier(),
                            oldAttrs.getDamageAddition(), oldAttrs.getDamageMultiplier(),
                            oldAttrs.getSpeedAddition(), oldAttrs.getSpeedMultiplier(),
                            oldAttrs.getKnockbackResistanceAddition(), oldAttrs.getKnockbackResistanceMultiplier(),
                            oldAttrs.getAttackKnockbackAddition(), oldAttrs.getAttackKnockbackMultiplier(),
                            oldAttrs.getAttackSpeedAddition(), oldAttrs.getAttackSpeedMultiplier(),
                            oldAttrs.getFollowRangeAddition(), oldAttrs.getFollowRangeMultiplier(),
                            oldAttrs.getFlyingSpeedAddition(), oldAttrs.getFlyingSpeedMultiplier(),
                            oldAttrs.getArmorToughnessAddition(), oldAttrs.getArmorToughnessMultiplier(),
                            oldAttrs.getLuckAddition(), oldAttrs.getLuckMultiplier(),
                            oldAttrs.getSwimSpeedAddition(), oldAttrs.getSwimSpeedMultiplier(),
                            oldAttrs.getReachDistanceAddition(), oldAttrs.getReachDistanceMultiplier(),
                            // Ночные атрибуты
                            oldAttrs.getNightHealthAddition(), oldAttrs.getNightHealthMultiplier(),
                            oldAttrs.getNightArmorAddition(), oldAttrs.getNightArmorMultiplier(),
                            oldAttrs.getNightDamageAddition(), oldAttrs.getNightDamageMultiplier(),
                            oldAttrs.getNightSpeedAddition(), oldAttrs.getNightSpeedMultiplier(),
                            oldAttrs.getNightKnockbackResistanceAddition(), oldAttrs.getNightKnockbackResistanceMultiplier(),
                            oldAttrs.getNightAttackKnockbackAddition(), oldAttrs.getNightAttackKnockbackMultiplier(),
                            oldAttrs.getNightAttackSpeedAddition(), oldAttrs.getNightAttackSpeedMultiplier(),
                            oldAttrs.getNightFollowRangeAddition(), oldAttrs.getNightFollowRangeMultiplier(),
                            oldAttrs.getNightFlyingSpeedAddition(), oldAttrs.getNightFlyingSpeedMultiplier(),
                            oldAttrs.getNightArmorToughnessAddition(), oldAttrs.getNightArmorToughnessMultiplier(),
                            oldAttrs.getNightLuckAddition(), oldAttrs.getNightLuckMultiplier(),
                            oldAttrs.getNightSwimSpeedAddition(), oldAttrs.getNightSwimSpeedMultiplier(),
                            oldAttrs.getNightReachDistanceAddition(), oldAttrs.getNightReachDistanceMultiplier(),
                            // Пещерные атрибуты
                            oldAttrs.getCaveHealthAddition(), oldAttrs.getCaveHealthMultiplier(),
                            oldAttrs.getCaveArmorAddition(), oldAttrs.getCaveArmorMultiplier(),
                            oldAttrs.getCaveDamageAddition(), oldAttrs.getCaveDamageMultiplier(),
                            oldAttrs.getCaveSpeedAddition(), oldAttrs.getCaveSpeedMultiplier(),
                            oldAttrs.getCaveKnockbackResistanceAddition(), oldAttrs.getCaveKnockbackResistanceMultiplier(),
                            oldAttrs.getCaveAttackKnockbackAddition(), oldAttrs.getCaveAttackKnockbackMultiplier(),
                            oldAttrs.getCaveAttackSpeedAddition(), oldAttrs.getCaveAttackSpeedMultiplier(),
                            oldAttrs.getCaveFollowRangeAddition(), oldAttrs.getCaveFollowRangeMultiplier(),
                            oldAttrs.getCaveFlyingSpeedAddition(), oldAttrs.getCaveFlyingSpeedMultiplier(),
                            oldAttrs.getCaveArmorToughnessAddition(), oldAttrs.getCaveArmorToughnessMultiplier(),
                            oldAttrs.getCaveLuckAddition(), oldAttrs.getCaveLuckMultiplier(),
                            oldAttrs.getCaveSwimSpeedAddition(), oldAttrs.getCaveSwimSpeedMultiplier(),
                            oldAttrs.getCaveReachDistanceAddition(), oldAttrs.getCaveReachDistanceMultiplier(),
                            oldAttrs.isBlacklisted()
                        );
                        
                        // Создаем новый конфиг с обновленными атрибутами
                        IndividualMobConfig newConfig = new IndividualMobConfig(config.isBlacklisted(), newAttrs);
                        
                        // Обновляем конфиг в копии
                        mobConfigsCopy.put(selectedEntity, newConfig);
                        
                        // Перерисовываем интерфейс
                        initMobSettingsContent();
                    } catch (Exception e) {
                        LOGGER.error("Ошибка при обновлении gravityEnabled: ", e);
                    }
                });
        this.addRenderableWidget(gravityCheckbox);
        this.checkBoxes.add(gravityCheckbox);
    }
        
    private EditBox createAttributeField(int x, int y, String fieldName, IndividualMobAttributes config) {
        EditBox editBox = new EditBox(font, x, y, FIELD_WIDTH, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 259) { // Backspace
                    String currentText = this.getValue();
                    if (!currentText.isEmpty()) {
                        this.setValue(currentText.substring(0, currentText.length() - 1));
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        
        editBox.setMaxLength(10);
        
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = IndividualMobAttributes.class.getMethod(getterName);
            double value = (double) getter.invoke(config);
            // Форматируем значение с точкой вместо запятой
            editBox.setValue(String.format(Locale.US, "%.2f", value));
        } catch (Exception e) {
            LOGGER.error("error getting attribute value: " + fieldName, e);
            editBox.setValue("1.00");
        }
        
        return editBox;
    }

    private class AddMobDialog extends Screen {
        private final int dialogWidth = 400;
        private final int dialogHeight = 300;
        private EditBox mobIdField;
        private List<String> suggestions = new ArrayList<>();
        private int selectedSuggestion = -1;
        private int scrollOffset = 0;
        private static final int MAX_VISIBLE_SUGGESTIONS = 8;
        
        public AddMobDialog() {
            super(Component.literal(getTranslatedText("gui.mobscaler.add_mob").getString()));
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Создаем поле ввода с автодополнением
            mobIdField = new EditBox(this.font, dialogX + 20, dialogY + 60, dialogWidth - 40, 20, Component.literal(""));
            mobIdField.setMaxLength(50);
            mobIdField.setValue("");
            mobIdField.setResponder(this::updateSuggestions);
            this.addRenderableWidget(mobIdField);
            
            // Кнопка создания
            Button createButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.create"),
                button -> {
                    String mobId = mobIdField.getValue().trim();
                    if (!mobId.isEmpty()) {
                        createNewMob(mobId);
                    }
                },
                ADD_BUTTON_COLOR,
                0xFF55FF55
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> Minecraft.getInstance().setScreen(MobScalerScreen.this),
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(cancelButton);
            
            // Начальное обновление подсказок
            updateSuggestions("");
        }
        
        private void updateSuggestions(String input) {
            suggestions.clear();
            selectedSuggestion = -1;
            scrollOffset = 0;
            
            // Получаем список всех зарегистрированных сущностей
            net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.forEach(entityType -> {
                String entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
                if (entityId.toLowerCase().contains(input.toLowerCase())) {
                    suggestions.add(entityId);
                }
            });
            
            // Сортируем подсказки
            Collections.sort(suggestions);
        }
        
        private void createNewMob(String mobId) {
            if (mobId == null || mobId.isEmpty()) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.error.empty_id").getString()), false);
                return;
            }
            
            if (mobConfigsCopy.containsKey(mobId)) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.error.mob_exists").getString()), false);
                return;
            }
            
            try {
                // Создаем новый конфиг с дефолтными значениями
                IndividualMobConfig newConfig = IndividualMobConfig.getDefault();
                
                // Добавляем новый конфиг в копию
                mobConfigsCopy.put(mobId, newConfig);
                
                // Добавляем новый конфиг в менеджер
                IndividualMobConfigManager.getIndividualMobConfigs().put(mobId, newConfig);
                
                // Сохраняем изменения в файл конфигурации
                IndividualMobConfigManager.saveIndividualConfigs();
                
                // Перезагружаем конфигурацию
                MobScalerConfig.init();
                
                // Обновляем интерфейс
                this.init();
                
                // Выбираем новую сущность
                selectedEntity = mobId;
                
                // Обновляем интерфейс для отображения настроек
                initMobSettingsContent();
                
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.mob_created").getString()), false);
                    
                // Закрываем диалог добавления сущности
                Minecraft.getInstance().setScreen(MobScalerScreen.this);
            } catch (Exception e) {
                LOGGER.error("error creating new entity", e);
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(getTranslatedText("gui.mobscaler.error.create_mob").getString()), false);
            }
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            // Рисуем фон диалога
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Основной фон
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Заголовок
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + 30, HEADER_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.add_mob").getString(), this.width / 2, dialogY + 10, TEXT_COLOR);
            
            // Подсказка
            drawString(poseStack, this.font, getTranslatedText("gui.mobscaler.enter_mob_id").getString(), dialogX + 15, dialogY + 40, TEXT_COLOR);
            
            // Рисуем подсказки
            if (!suggestions.isEmpty()) {
                int suggestionY = dialogY + 80;
                int maxY = dialogY + dialogHeight - 60;
                
                // Фон для списка подсказок
                fill(poseStack, dialogX + 20, suggestionY, dialogX + dialogWidth - 20, maxY, 0x80000000);
                
                for (int i = scrollOffset; i < Math.min(scrollOffset + MAX_VISIBLE_SUGGESTIONS, suggestions.size()); i++) {
                    String suggestion = suggestions.get(i);
                    boolean isSelected = i == selectedSuggestion;
                    
                    // Подсветка выбранной подсказки
                    if (isSelected) {
                        fill(poseStack, dialogX + 20, suggestionY, dialogX + dialogWidth - 20, suggestionY + 12, HIGHLIGHT_COLOR);
                    }
                    
                    drawString(poseStack, this.font, suggestion, dialogX + 25, suggestionY, TEXT_COLOR);
                    suggestionY += 12;
                }
            }
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // ESC
                Minecraft.getInstance().setScreen(MobScalerScreen.this);
                return true;
            }
            
            if (!suggestions.isEmpty()) {
                if (keyCode == 264) { // Стрелка вниз
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        scrollOffset++;
                    }
                    return true;
                }
                if (keyCode == 265) { // Стрелка вверх
                    selectedSuggestion = Math.max(selectedSuggestion - 1, -1);
                    if (selectedSuggestion < scrollOffset) {
                        scrollOffset = Math.max(0, scrollOffset - 1);
                    }
                    return true;
                }
                if (keyCode == 257 && selectedSuggestion >= 0) { // Enter
                    mobIdField.setValue(suggestions.get(selectedSuggestion));
                    return true;
                }
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            if (!suggestions.isEmpty()) {
                int suggestionY = dialogY + 80;
                int maxY = dialogY + dialogHeight - 60;
                
                if (mouseX >= dialogX + 20 && mouseX <= dialogX + dialogWidth - 20 &&
                    mouseY >= suggestionY && mouseY <= maxY) {
                    int clickedIndex = scrollOffset + (int)((mouseY - suggestionY) / 12);
                    if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
                        mobIdField.setValue(suggestions.get(clickedIndex));
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!suggestions.isEmpty()) {
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int)delta, 
                    Math.max(0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS)));
                return true;
            }
            return false;
        }
    }

    private class DeleteMobConfirmationDialog extends Screen {
        private final String mobId;
        private final Runnable onConfirm;
        private final int dialogWidth = 300;
        private final int dialogHeight = 100;

        public DeleteMobConfirmationDialog(String mobId, Runnable onConfirm) {
            super(Component.literal(getTranslatedText("gui.mobscaler.confirm.delete").getString()));
            this.mobId = mobId;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            
            // Кнопка подтверждения
            Button confirmButton = new StyledButton(
                centerX - buttonWidth - 5, 
                centerY + 10, 
                buttonWidth, 
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.delete"),
                button -> {
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(MobScalerScreen.this);
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(confirmButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5, 
                centerY + 10, 
                buttonWidth, 
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> Minecraft.getInstance().setScreen(MobScalerScreen.this),
                BUTTON_COLOR,
                BUTTON_HOVER_COLOR
            );
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            // Рисуем фон диалога
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + 20, HEADER_COLOR);
            
            // Рисуем текст
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.confirm.delete").getString(), this.width / 2, dialogY + 6, TEXT_COLOR);
            drawCenteredString(poseStack, this.font, getTranslatedText("gui.mobscaler.confirm.delete.mob").getString() + " " + mobId + "?", 
                             this.width / 2, dialogY + 40, TEXT_COLOR);
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    private void saveMobChanges() {
        try {
            if (selectedEntity != null) {
                IndividualMobConfig config = mobConfigsCopy.get(selectedEntity);
                if (config != null) {
                    
                    // Получаем значения чекбоксов
                    boolean enableNightScaling = config.getAttributes().getEnableNightScaling();
                    boolean enableCaveScaling = config.getAttributes().getEnableCaveScaling();
                    boolean enableGravity = config.getAttributes().isGravityEnabled();
                    boolean isBlacklisted = config.isBlacklisted(); // Используем текущее значение
                    
                    for (CheckBox checkbox : checkBoxes) {
                        String label = checkbox.label.getString();
                        if (label.contains(getTranslatedText("gui.mobscaler.enable_night_scaling").getString())) {
                            enableNightScaling = checkbox.isChecked();
                        } else if (label.contains("пещерное")) {
                            enableCaveScaling = checkbox.isChecked();
                        } else if (label.contains("Черный список")) {
                            isBlacklisted = checkbox.isChecked();
                        } else if (label.contains(getTranslatedText("gui.mobscaler.enable_gravity").getString())) {
                            enableGravity = checkbox.isChecked();
                        }
                    
                        
                    }
                    
                    // Создаем новый объект атрибутов с обновленными значениями enableNightScaling и enableCaveScaling
                        IndividualMobAttributes oldAttrs = config.getAttributes();
                        IndividualMobAttributes newAttrs = new IndividualMobAttributes(
                        enableNightScaling,
                        enableCaveScaling,
                            oldAttrs.getCaveHeight(),
                            oldAttrs.getGravityMultiplier(),
                        enableGravity,
                            // Остальные параметры остаются без изменений
                            oldAttrs.getHealthAddition(), oldAttrs.getHealthMultiplier(),
                            oldAttrs.getArmorAddition(), oldAttrs.getArmorMultiplier(),
                            oldAttrs.getDamageAddition(), oldAttrs.getDamageMultiplier(),
                            oldAttrs.getSpeedAddition(), oldAttrs.getSpeedMultiplier(),
                            oldAttrs.getKnockbackResistanceAddition(), oldAttrs.getKnockbackResistanceMultiplier(),
                            oldAttrs.getAttackKnockbackAddition(), oldAttrs.getAttackKnockbackMultiplier(),
                            oldAttrs.getAttackSpeedAddition(), oldAttrs.getAttackSpeedMultiplier(),
                            oldAttrs.getFollowRangeAddition(), oldAttrs.getFollowRangeMultiplier(),
                            oldAttrs.getFlyingSpeedAddition(), oldAttrs.getFlyingSpeedMultiplier(),
                            oldAttrs.getArmorToughnessAddition(), oldAttrs.getArmorToughnessMultiplier(),
                            oldAttrs.getLuckAddition(), oldAttrs.getLuckMultiplier(),
                            oldAttrs.getSwimSpeedAddition(), oldAttrs.getSwimSpeedMultiplier(),
                            oldAttrs.getReachDistanceAddition(), oldAttrs.getReachDistanceMultiplier(),
                            // Ночные атрибуты
                            oldAttrs.getNightHealthAddition(), oldAttrs.getNightHealthMultiplier(),
                            oldAttrs.getNightArmorAddition(), oldAttrs.getNightArmorMultiplier(),
                            oldAttrs.getNightDamageAddition(), oldAttrs.getNightDamageMultiplier(),
                            oldAttrs.getNightSpeedAddition(), oldAttrs.getNightSpeedMultiplier(),
                            oldAttrs.getNightKnockbackResistanceAddition(), oldAttrs.getNightKnockbackResistanceMultiplier(),
                            oldAttrs.getNightAttackKnockbackAddition(), oldAttrs.getNightAttackKnockbackMultiplier(),
                            oldAttrs.getNightAttackSpeedAddition(), oldAttrs.getNightAttackSpeedMultiplier(),
                            oldAttrs.getNightFollowRangeAddition(), oldAttrs.getNightFollowRangeMultiplier(),
                            oldAttrs.getNightFlyingSpeedAddition(), oldAttrs.getNightFlyingSpeedMultiplier(),
                            oldAttrs.getNightArmorToughnessAddition(), oldAttrs.getNightArmorToughnessMultiplier(),
                            oldAttrs.getNightLuckAddition(), oldAttrs.getNightLuckMultiplier(),
                            oldAttrs.getNightSwimSpeedAddition(), oldAttrs.getNightSwimSpeedMultiplier(),
                            oldAttrs.getNightReachDistanceAddition(), oldAttrs.getNightReachDistanceMultiplier(),
                            // Пещерные атрибуты
                            oldAttrs.getCaveHealthAddition(), oldAttrs.getCaveHealthMultiplier(),
                            oldAttrs.getCaveArmorAddition(), oldAttrs.getCaveArmorMultiplier(),
                            oldAttrs.getCaveDamageAddition(), oldAttrs.getCaveDamageMultiplier(),
                            oldAttrs.getCaveSpeedAddition(), oldAttrs.getCaveSpeedMultiplier(),
                            oldAttrs.getCaveKnockbackResistanceAddition(), oldAttrs.getCaveKnockbackResistanceMultiplier(),
                            oldAttrs.getCaveAttackKnockbackAddition(), oldAttrs.getCaveAttackKnockbackMultiplier(),
                            oldAttrs.getCaveAttackSpeedAddition(), oldAttrs.getCaveAttackSpeedMultiplier(),
                            oldAttrs.getCaveFollowRangeAddition(), oldAttrs.getCaveFollowRangeMultiplier(),
                            oldAttrs.getCaveFlyingSpeedAddition(), oldAttrs.getCaveFlyingSpeedMultiplier(),
                            oldAttrs.getCaveArmorToughnessAddition(), oldAttrs.getCaveArmorToughnessMultiplier(),
                            oldAttrs.getCaveLuckAddition(), oldAttrs.getCaveLuckMultiplier(),
                            oldAttrs.getCaveSwimSpeedAddition(), oldAttrs.getCaveSwimSpeedMultiplier(),
                            oldAttrs.getCaveReachDistanceAddition(), oldAttrs.getCaveReachDistanceMultiplier(),
                        isBlacklisted
                        );
                        
                        // Создаем новый конфиг с обновленными атрибутами
                    IndividualMobConfig newConfig = new IndividualMobConfig(isBlacklisted, newAttrs);
                        
                    // Обновляем локальную копию
                        mobConfigsCopy.put(selectedEntity, newConfig);

                    // Обновляем значения из полей ввода
                    for (FieldMapping mapping : fieldMappings) {
                        String fieldName = mapping.getFieldName();
                        EditBox editBox = mapping.getEditBox();
                        
                        if (editBox != null) {
                            try {
                                String value = editBox.getValue().trim().replace(",", ".");
                                if (!value.isEmpty()) {
                                    double numValue = Double.parseDouble(value);
                                    
                                    // Преобразуем имя поля, чтобы первая буква была маленькой
                                    // Это нужно для корректного доступа к полям через рефлексию
                                    String normalizedFieldName = fieldName.substring(0, 1).toLowerCase() + 
                                                               fieldName.substring(1);
                                    
                                    try {
                                        // Получаем доступ к полю напрямую в newAttrs
                                        Field field = newAttrs.getClass().getDeclaredField(normalizedFieldName);
                                        field.setAccessible(true);
                                        field.setDouble(newAttrs, numValue);
                                        
                    } catch (Exception e) {
                                        LOGGER.error("error saving field {}: {} ({})", 
                                            normalizedFieldName, e.getMessage(), e.getClass().getSimpleName());
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("error saving field {}: {} ({})", 
                                    fieldName, e.getMessage(), e.getClass().getSimpleName());
                            }
                        }
                    }
                    
                    // Обновляем конфиг с новыми атрибутами
                    newConfig = new IndividualMobConfig(isBlacklisted, newAttrs);
                    
                    // Обновляем локальную копию
                    mobConfigsCopy.put(selectedEntity, newConfig);
                    
                    // Обновляем глобальную конфигурацию
                    IndividualMobManager.addIndividualMobConfig(selectedEntity, newConfig);
                    
                    // Обновляем конфигурацию в IndividualMobConfigManager
                    IndividualMobConfigManager.getIndividualMobConfigs().put(selectedEntity, newConfig);
                    
                    // Сохраняем изменения в файл
                    IndividualMobConfigManager.saveIndividualConfigs();
                    
                    // Обновляем конфигурацию
                    MobScalerConfig.init();
                    
                    // Отображаем сообщение об успешном сохранении
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.mob_settings_saved"), false);
                    }
                    
                }
            }
        } catch (Exception e) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.error.save_mob_settings", e.getMessage()), false);
            }
        }
    }

    /**
     * Инициализирует содержимое вкладки игрока
     */
    private void initPlayerTabContent() {
        // Очищаем предыдущие виджеты
        this.clearWidgets();
        
        // Добавляем кнопки вкладок
        addTabButtons();
        
        // Добавляем кнопку закрытия
        addCloseButton();
        
        // Добавляем кнопку сохранения
        addSaveButton();
        
        // Заголовок
        int startX = 20;
        int startY = 40;
        int spacing = 25;
        
        // Выбор измерения
        addLabel(getTranslatedText("gui.mobscaler.select_dimension").getString(), startX, startY);
        startY += spacing;
        
        // Получаем список измерений из конфига
        List<String> dimensions = new ArrayList<>(PlayerConfigManager.getPlayerConfig().getPlayerModifiers().keySet());
        
        // Создаем кнопки для каждого измерения
        int buttonWidth = 180;
        int buttonHeight = 20;
        int buttonY = startY;
        
        for (String dimension : dimensions) {
            // Кнопка измерения
            Button dimensionButton = new StyledButton(startX, buttonY, buttonWidth, buttonHeight, 
                    Component.literal(getDimensionDisplayName(dimension)), button -> {
                selectedDimension = dimension;
                this.init();
            }, selectedDimension != null && selectedDimension.equals(dimension) ? BUTTON_HOVER_COLOR : BUTTON_COLOR, BUTTON_HOVER_COLOR);
            this.addRenderableWidget(dimensionButton);
            this.contentButtons.add(dimensionButton);
            
            // Кнопка удаления измерения
            Button deleteButton = new StyledButton(startX + buttonWidth + 5, buttonY, 20, buttonHeight, 
                    Component.literal("X"), button -> {
                showDeletePlayerDimensionDialog(dimension);
            }, DELETE_BUTTON_COLOR, 0xFFFF5555);
            this.addRenderableWidget(deleteButton);
            this.contentButtons.add(deleteButton);
            
            buttonY += buttonHeight + 5;
        }
        
        // Добавляем кнопку добавления нового измерения
        Button addDimensionButton = new StyledButton(startX, buttonY + dimensions.size() * (buttonHeight + 5), 
                buttonWidth, buttonHeight, 
                getTranslatedText("gui.mobscaler.add_dimension_button"), button -> {
            showAddPlayerDimensionDialog();
        }, ADD_BUTTON_COLOR, BUTTON_HOVER_COLOR);
        this.addRenderableWidget(addDimensionButton);
        this.contentButtons.add(addDimensionButton);
        
        // Если измерение выбрано, показываем настройки для него
        if (selectedDimension != null) {
            initPlayerSettingsContent();
        }
    }

    /**
     * Показывает диалог подтверждения удаления измерения игрока
     */
    private void showDeletePlayerDimensionDialog(String dimensionId) {
        Minecraft.getInstance().setScreen(new DeletePlayerDimensionConfirmationDialog(dimensionId, () -> {
            deletePlayerDimension(dimensionId);
        }));
    }

    /**
     * Удаляет измерение игрока
     */
    private void deletePlayerDimension(String dimensionId) {
        try {
            // Получаем текущие настройки
            Map<String, PlayerConfig.PlayerModifiers> playerModifiers = PlayerConfigManager.getPlayerConfig().getPlayerModifiers();
            
            // Проверяем, что измерение существует
            if (!playerModifiers.containsKey(dimensionId)) {
                Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.dimension_not_found"), false);
                return;
            }
            
            // Удаляем измерение
            playerModifiers.remove(dimensionId);
            
            // Сохраняем изменения
            PlayerConfigManager.saveConfigs();
            
            // Сбрасываем выбранное измерение, если оно было удалено
            if (selectedDimension != null && selectedDimension.equals(dimensionId)) {
                selectedDimension = null;
            }
            
            // Убеждаемся, что мы на вкладке игрока
            currentTab = TabType.PLAYER;
            
            // Возвращаемся к основному экрану и показываем настройки игрока
            Minecraft.getInstance().setScreen(MobScalerScreen.this);
            
            // Инициализируем интерфейс заново, чтобы применить изменения
            init();
            
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.dimension_deleted"), false);
        } catch (Exception e) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.delete_dimension", e.getMessage()), false);
        }
    }

    /**
     * Диалог подтверждения удаления измерения игрока
     */
    private class DeletePlayerDimensionConfirmationDialog extends Screen {
        private final String dimensionId;
        private final Runnable onConfirm;
        private final int dialogWidth = 300;
        private final int dialogHeight = 100;
        
        public DeletePlayerDimensionConfirmationDialog(String dimensionId, Runnable onConfirm) {
            super(getTranslatedText("gui.mobscaler.confirm.delete.title"));
            this.dimensionId = dimensionId;
            this.onConfirm = onConfirm;
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            
            // Кнопка подтверждения
            Button confirmButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + 20,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.delete"),
                button -> {
                    onConfirm.run();
                    this.onClose();
                },
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(confirmButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + 20,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> {
                    this.onClose();
                    // Сначала убедимся, что мы возвращаемся к главному экрану
                    minecraft.setScreen(MobScalerScreen.this);
                    // Убеждаемся, что мы на вкладке игрока
                    currentTab = TabType.PLAYER;
                    // Инициализируем интерфейс заново, чтобы применить изменения
                    init();
                },
                BUTTON_COLOR,
                BUTTON_HOVER_COLOR
            );
            this.addRenderableWidget(cancelButton);
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Рисуем фон диалога
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Рисуем заголовок
            drawCenteredString(poseStack, this.font, this.title, this.width / 2, dialogY + 10, TEXT_COLOR);
            
            // Рисуем сообщение
            drawCenteredString(poseStack, this.font, 
                getTranslatedText("gui.mobscaler.confirm.delete.player_dimension", dimensionId).getString(), 
                this.width / 2, dialogY + 30, TEXT_COLOR);
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    /**
     * Возвращает отображаемое имя измерения
     */
    private String getDimensionDisplayName(String dimensionId) {
        // Преобразуем minecraft:overworld в "Обычный мир" и т.д.
        if (dimensionId.equals("minecraft:overworld")) {
            return getTranslatedText("gui.mobscaler.dimension.overworld").getString();
        } else if (dimensionId.equals("minecraft:the_nether")) {
            return getTranslatedText("gui.mobscaler.dimension.the_nether").getString();
        } else if (dimensionId.equals("minecraft:the_end")) {
            return getTranslatedText("gui.mobscaler.dimension.the_end").getString();
        } else {
            // Для других измерений убираем префикс и заменяем подчеркивания на пробелы
            String name = dimensionId;
            if (name.contains(":")) {
                name = name.substring(name.indexOf(':') + 1);
            }
            return name.replace('_', ' ');
        }
    }

    /**
     * Инициализирует содержимое настроек игрока для выбранного измерения
     */
    private void initPlayerSettingsContent() {
        PlayerConfig.PlayerModifiers modifiers = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(selectedDimension);
        
        int startX = 250; // Увеличиваем отступ слева для перемещения текста правее
        int startY = 40;  // Уменьшаем начальную позицию Y для перемещения текста выше
        int spacing = 25;
        
        // Заголовок
        addLabel(getTranslatedText("gui.mobscaler.player.settings").getString() + " " + getDimensionDisplayName(selectedDimension), startX, startY);
        startY += spacing;
        
        // Подсказка о сохранении
        addLabel(getTranslatedText("gui.mobscaler.save_reminder").getString(), startX, startY);
        startY += spacing;
        
        // Чекбокс для включения ночного скейлинга - перемещаем его на ту же линию, где начинаются атрибуты
        CheckBox nightScalingBox = new CheckBox(startX, startY, 200, 20, 
                getTranslatedText("gui.mobscaler.enable_night_scaling"), 
                modifiers.isNightScalingEnabled(), 
                button -> {
                    CheckBox cb = (CheckBox) button;
                    // Если чекбокс отключен, сбрасываем режим ночных настроек
                    if (!cb.isChecked() && showNightSettings) {
                        showNightSettings = false;
                    }
                    
                    // Сохраняем состояние чекбокса в конфиг
                    try {
                        Field enableNightScalingField = modifiers.getClass().getDeclaredField("enableNightScaling");
                        enableNightScalingField.setAccessible(true);
                        enableNightScalingField.setBoolean(modifiers, cb.isChecked());
                    } catch (Exception e) {
                        LOGGER.error("Ошибка при обновлении enableNightScaling: ", e);
                    }
                    
                    // Обновляем интерфейс при изменении чекбокса
                    this.init();
                });
        this.addRenderableWidget(nightScalingBox);
        this.checkBoxes.add(nightScalingBox);

        Button attributeTypeButton = new StyledButton(startX + 270, startY, 80, 20,
        Component.literal(currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                getTranslatedText("gui.mobscaler.advanced").getString() : getTranslatedText("gui.mobscaler.basic").getString()), 
        button -> {
            // Переключаемся между основными и дополнительными атрибутами
            currentAttributeDisplayType = currentAttributeDisplayType == AttributeDisplayType.BASIC ? 
                    AttributeDisplayType.ADVANCED : AttributeDisplayType.BASIC;
            
            
            // Удаляем все виджеты
            this.clearWidgets();
            
            // Очищаем списки
            this.fieldMappings.clear();
            this.textFields.clear();
            this.checkBoxes.clear();
            this.contentButtons.clear();
            this.tabButtons.clear();
            this.labels.clear();
            
            // Переинициализируем весь интерфейс
            initTabContent();
        },
        currentAttributeDisplayType == AttributeDisplayType.ADVANCED ? 0xFF4B0082 : 0xFF006400, // Индиго для доп., темно-зеленый для базовых
        currentAttributeDisplayType == AttributeDisplayType.ADVANCED ? 0xFF800080 : 0xFF008000); // Пурпурный для доп., зеленый для базовых

this.addRenderableWidget(attributeTypeButton);
this.contentButtons.add(attributeTypeButton);


        // Добавляем кнопку "Ночь" справа от чекбокса, активную только при включенном ночном режиме
        Button nightButton = new StyledButton(startX + 170, startY, 80, 20, 
                getTranslatedText(showNightSettings ? "gui.mobscaler.day" : "gui.mobscaler.night"), 
                button -> {
                    // Переключаемся на ночные настройки
                    showNightSettings = !showNightSettings;
                    this.init();
                }, 
                showNightSettings ? 0xFFFFD700 : 0xFF191970, // Желтый для дня, темно-синий для ночи
                showNightSettings ? 0xFFFFA500 : 0xFF0000CD); // Оранжевый для дня при наведении, синий для ночи при наведении
    
        // Кнопка активна только если включен ночной режим
        nightButton.active = modifiers.isNightScalingEnabled();
    
        this.addRenderableWidget(nightButton);
        this.contentButtons.add(nightButton);
    
        startY += spacing;
    
        // Заголовок для настроек
        if (showNightSettings && modifiers.isNightScalingEnabled()) {
            addLabel(getTranslatedText("gui.mobscaler.night_settings").getString(), startX, startY);
        } else {
            addLabel(getTranslatedText("gui.mobscaler.day_settings").getString(), startX, startY);
        }
        startY += spacing;
    
        // Выбираем, какие настройки показывать - дневные или ночные
        if (showNightSettings && modifiers.isNightScalingEnabled()) {
            if (currentAttributeDisplayType == AttributeDisplayType.BASIC) {
            // Добавляем поля для ночных настроек в формате "Атрибут ___ х ___"
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.health").getString(), 
                                  "player." + selectedDimension + ".nightHealthAddition", modifiers.getNightHealthAddition(),
                                  "player." + selectedDimension + ".nightHealthMultiplier", modifiers.getNightHealthMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.armor").getString(), 
                                  "player." + selectedDimension + ".nightArmorAddition", modifiers.getNightArmorAddition(),
                                  "player." + selectedDimension + ".nightArmorMultiplier", modifiers.getNightArmorMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.damage").getString(), 
                                  "player." + selectedDimension + ".nightDamageAddition", modifiers.getNightDamageAddition(),
                                  "player." + selectedDimension + ".nightDamageMultiplier", modifiers.getNightDamageMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.speed").getString(), 
                                  "player." + selectedDimension + ".nightSpeedAddition", modifiers.getNightSpeedAddition(),
                                  "player." + selectedDimension + ".nightSpeedMultiplier", modifiers.getNightSpeedMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.knockback_resistance").getString(), 
                                  "player." + selectedDimension + ".nightKnockbackResistanceAddition", modifiers.getNightKnockbackResistanceAddition(),
                                  "player." + selectedDimension + ".nightKnockbackResistanceMultiplier", modifiers.getNightKnockbackResistanceMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.attack_knockback").getString(), 
                                  "player." + selectedDimension + ".nightAttackKnockbackAddition", modifiers.getNightAttackKnockbackAddition(),
                                  "player." + selectedDimension + ".nightAttackKnockbackMultiplier", modifiers.getNightAttackKnockbackMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.attack_speed").getString(), 
                                  "player." + selectedDimension + ".nightAttackSpeedAddition", modifiers.getNightAttackSpeedAddition(),
                                  "player." + selectedDimension + ".nightAttackSpeedMultiplier", modifiers.getNightAttackSpeedMultiplier());
            startY += spacing;

            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.armor_toughness").getString(), 
                                  "player." + selectedDimension + ".nightArmorToughnessAddition", modifiers.getNightArmorToughnessAddition(),
                                  "player." + selectedDimension + ".nightArmorToughnessMultiplier", modifiers.getNightArmorToughnessMultiplier());
            }
            else if (currentAttributeDisplayType == AttributeDisplayType.ADVANCED) {
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.follow_range").getString(), 
                                  "player." + selectedDimension + ".nightFollowRangeAddition", modifiers.getNightFollowRangeAddition(),
                                  "player." + selectedDimension + ".nightFollowRangeMultiplier", modifiers.getNightFollowRangeMultiplier());
            startY += spacing;

            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.flying_speed").getString(), 
                                  "player." + selectedDimension + ".nightFlyingSpeedAddition", modifiers.getNightFlyingSpeedAddition(),
                                  "player." + selectedDimension + ".nightFlyingSpeedMultiplier", modifiers.getNightFlyingSpeedMultiplier());
            startY += spacing;

            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.luck").getString(), 
                                  "player." + selectedDimension + ".nightLuckAddition", modifiers.getNightLuckAddition(),
                                  "player." + selectedDimension + ".nightLuckMultiplier", modifiers.getNightLuckMultiplier());
            startY += spacing;
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.reach_distance").getString(), 
                                  "player." + selectedDimension + ".nightReachDistanceAddition", modifiers.getNightReachDistanceAddition(),
                                  "player." + selectedDimension + ".nightReachDistanceMultiplier", modifiers.getNightReachDistanceMultiplier());
            startY += spacing;
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.swim_speed").getString(), 
                                  "player." + selectedDimension + ".nightSwimSpeedAddition", modifiers.getNightSwimSpeedAddition(),
                                  "player." + selectedDimension + ".nightSwimSpeedMultiplier", modifiers.getNightSwimSpeedMultiplier());
            }       
            
        } 
        else {
            // Добавляем поля для дневных настроек в формате "Атрибут ___ х ___"
            if (currentAttributeDisplayType == AttributeDisplayType.BASIC) {
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.health").getString(), 
                                  "player." + selectedDimension + ".healthAddition", modifiers.getHealthAddition(),
                                  "player." + selectedDimension + ".healthMultiplier", modifiers.getHealthMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.armor").getString(), 
                                  "player." + selectedDimension + ".armorAddition", modifiers.getArmorAddition(),
                                  "player." + selectedDimension + ".armorMultiplier", modifiers.getArmorMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.damage").getString(), 
                                  "player." + selectedDimension + ".damageAddition", modifiers.getDamageAddition(),
                                  "player." + selectedDimension + ".damageMultiplier", modifiers.getDamageMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.speed").getString(), 
                                  "player." + selectedDimension + ".speedAddition", modifiers.getSpeedAddition(),
                                  "player." + selectedDimension + ".speedMultiplier", modifiers.getSpeedMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.knockback_resistance").getString(), 
                                  "player." + selectedDimension + ".knockbackResistanceAddition", modifiers.getKnockbackResistanceAddition(),
                                  "player." + selectedDimension + ".knockbackResistanceMultiplier", modifiers.getKnockbackResistanceMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.attack_knockback").getString(), 
                                  "player." + selectedDimension + ".attackKnockbackAddition", modifiers.getAttackKnockbackAddition(),
                                  "player." + selectedDimension + ".attackKnockbackMultiplier", modifiers.getAttackKnockbackMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.attack_speed").getString(), 
                                  "player." + selectedDimension + ".attackSpeedAddition", modifiers.getAttackSpeedAddition(),
                                  "player." + selectedDimension + ".attackSpeedMultiplier", modifiers.getAttackSpeedMultiplier());
            startY += spacing;
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.armor_toughness").getString(), 
                                  "player." + selectedDimension + ".armorToughnessAddition", modifiers.getArmorToughnessAddition(),
                                  "player." + selectedDimension + ".armorToughnessMultiplier", modifiers.getArmorToughnessMultiplier());
            }
            else if (currentAttributeDisplayType == AttributeDisplayType.ADVANCED) {
                addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.follow_range").getString(), 
                                  "player." + selectedDimension + ".followRangeAddition", modifiers.getFollowRangeAddition(),
                                  "player." + selectedDimension + ".followRangeMultiplier", modifiers.getFollowRangeMultiplier());
            startY += spacing;

            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.flying_speed").getString(), 
                                  "player." + selectedDimension + ".flyingSpeedAddition", modifiers.getFlyingSpeedAddition(),
                                  "player." + selectedDimension + ".flyingSpeedMultiplier", modifiers.getFlyingSpeedMultiplier());
            startY += spacing;
            
            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.luck").getString(), 
                                  "player." + selectedDimension + ".luckAddition", modifiers.getLuckAddition(),
                                  "player." + selectedDimension + ".luckMultiplier", modifiers.getLuckMultiplier());
            startY += spacing;

            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.reach_distance").getString(), 
                                  "player." + selectedDimension + ".reachDistanceAddition", modifiers.getReachDistanceAddition(),
                                  "player." + selectedDimension + ".reachDistanceMultiplier", modifiers.getReachDistanceMultiplier());
            startY += spacing;

            addPlayerAttributePair(startX, startY, getTranslatedText("gui.mobscaler.swim_speed").getString(), 
                                  "player." + selectedDimension + ".swimSpeedAddition", modifiers.getSwimSpeedAddition(),
                                  "player." + selectedDimension + ".swimSpeedMultiplier", modifiers.getSwimSpeedMultiplier());      
            }
        }
        // Добавляем поле для множителя гравитации
        startY += spacing;
        addLabel(getTranslatedText("gui.mobscaler.gravity").getString(), startX, startY + 5);
        
        // Создаем поле для ввода множителя гравитации
        EditBox gravityMultiplierBox = new EditBox(this.font, startX + 100, startY, 35, 20, Component.literal(""));
        gravityMultiplierBox.setMaxLength(10);
        gravityMultiplierBox.setValue(String.valueOf(modifiers.getGravityMultiplier()));
        this.addRenderableWidget(gravityMultiplierBox);
        
        // Добавляем маппинг для поля гравитации
        FieldMapping gravityMapping = new FieldMapping("player." + selectedDimension + ".gravityMultiplier", getTranslatedText("gui.mobscaler.gravity").getString());
        gravityMapping.setEditBox(gravityMultiplierBox);
        this.fieldMappings.add(gravityMapping);
    }

    /**
     * Добавляет пару полей для атрибута игрока в формате "Атрибут ___ х ___"
     */
    private void addPlayerAttributePair(int x, int y, String label, 
                                       String additionPath, double additionValue,
                                       String multiplierPath, double multiplierValue) {
        // Добавляем метку атрибута
        addLabel(label, x, y + 5);
        
        // Создаем поле для значения сложения
        EditBox additionBox = new EditBox(this.font, x + 150, y, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    // Обрабатываем нажатие Enter
                    setFocused(false);
                        return true;
                    }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        
        additionBox.setValue(String.format("%.1f", additionValue).replace(",", "."));
        this.addRenderableWidget(additionBox);
        this.textFields.add(additionBox);
        
        // Добавляем маппинг поля сложения
        FieldMapping additionMapping = new FieldMapping(additionPath, label);
        additionMapping.setEditBox(additionBox);
        this.fieldMappings.add(additionMapping);
        
        // Добавляем символ умножения между полями
        addLabel("×", x + 195, y + 5);
        
        // Создаем поле для значения умножения
        EditBox multiplierBox = new EditBox(this.font, x + 215, y, 35, 20, Component.literal("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    // Обрабатываем нажатие Enter
                    setFocused(false);
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            
            @Override
            public void insertText(String text) {
                // Заменяем запятые на точки при вводе
                text = text.replace(",", ".");
                // Проверяем, что после вставки текста значение останется допустимым числом
                String newValue = new StringBuilder(getValue()).insert(getCursorPosition(), text).toString();
                if (newValue.isEmpty() || newValue.matches("^-?\\d*\\.?\\d*$")) {
                    super.insertText(text);
                }
            }
        };
        
        multiplierBox.setValue(String.format("%.1f", multiplierValue).replace(",", "."));
        this.addRenderableWidget(multiplierBox);
        this.textFields.add(multiplierBox);
        
        // Добавляем маппинг поля умножения
        FieldMapping multiplierMapping = new FieldMapping(multiplierPath, label);
        multiplierMapping.setEditBox(multiplierBox);
        this.fieldMappings.add(multiplierMapping);
    }

    /**
     * Сохраняет настройки игрока
     */
    private void savePlayerSettings() {
        // Получаем текущие настройки
        Map<String, PlayerConfig.PlayerModifiers> playerModifiers = PlayerConfigManager.getPlayerConfig().getPlayerModifiers();
        
        // Создаем временную карту для хранения значений атрибутов
        Map<String, Double> attributeValues = new HashMap<>();
        Map<String, Boolean> booleanValues = new HashMap<>();
        
        // Проходим по всем маппингам полей
        for (FieldMapping mapping : fieldMappings) {
            String fieldName = mapping.getFieldName();
            
            // Проверяем, что это поле относится к настройкам игрока
            if (fieldName.startsWith("player.")) {
                // Получаем значение из поля ввода
                if (mapping.getEditBox() != null) {
                    double value = mapping.getValue();
                    attributeValues.put(fieldName, value);
                }
            }
        }
        
        // Проходим по всем чекбоксам
        for (CheckBox checkbox : checkBoxes) {
            String label = checkbox.label.getString();
            if (label.equals(getTranslatedText("gui.mobscaler.enable_night_scaling").getString()) && selectedDimension != null) {
                String fieldName = "player." + selectedDimension + ".enableNightScaling";
                booleanValues.put(fieldName, checkbox.isChecked());
            }
        }
        
        // Обновляем настройки для выбранного измерения
        if (selectedDimension != null && playerModifiers.containsKey(selectedDimension)) {
            PlayerConfig.PlayerModifiers currentModifiers = playerModifiers.get(selectedDimension);
            
            // Создаем новый объект PlayerModifiers с обновленными значениями
            // Так как поля в PlayerModifiers final, нам нужно создать новый объект
            
            try {
                // Обновляем значения в текущем объекте PlayerModifiers
                // Используем прямое обновление полей через рефлексию
                
                // Обновляем настройки ночного режима
                if (booleanValues.containsKey("player." + selectedDimension + ".enableNightScaling")) {
                    boolean enableNightScaling = booleanValues.get("player." + selectedDimension + ".enableNightScaling");
                    // Используем рефлексию для прямого доступа к полю
                    try {
                        Field field = PlayerConfig.PlayerModifiers.class.getDeclaredField("enableNightScaling");
                        field.setAccessible(true);
                        field.set(currentModifiers, enableNightScaling);
                    } catch (Exception ex) {
                        Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.error.player_save", ex.getMessage()), false);
                    }
                }
                
                // Обновляем числовые атрибуты
                for (Map.Entry<String, Double> entry : attributeValues.entrySet()) {
                    String fieldPath = entry.getKey();
                    double value = entry.getValue();
                    
                    // Извлекаем имя поля из пути
                    String fieldName = fieldPath.substring(fieldPath.lastIndexOf('.') + 1);
                    
                    try {
                        // Используем прямой доступ к полю через рефлексию
                        Field field = PlayerConfig.PlayerModifiers.class.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        field.set(currentModifiers, value);
                    } catch (Exception ex) {
                        Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.error.player_save", ex.getMessage()), false);
                    }
                }
                
                // Сохраняем настройки
                PlayerConfigManager.saveConfigs();
                
                // Показываем сообщение об успешном сохранении
                Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.settings_saved"), false);
            } catch (Exception e) {
                Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.error.player_save", e.getMessage()), false);
            }
        }
    }

    /**
     * Показывает диалог добавления нового измерения для игрока
     */
    private void showAddPlayerDimensionDialog() {
        Minecraft.getInstance().setScreen(new AddPlayerDimensionDialog());
    }

    /**
     * Создает новое измерение для игрока
     */
    private void createNewPlayerDimension(String dimensionId) {
        if (dimensionId.isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.dimension_id_empty"), false);
            return;
        }
        
        Map<String, PlayerConfig.PlayerModifiers> playerModifiers = PlayerConfigManager.getPlayerConfig().getPlayerModifiers();
        
        if (playerModifiers.containsKey(dimensionId)) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.dimension_exists"), false);
            return;
        }
        
        try {
            // Создаем новый конфиг для измерения игрока
            PlayerConfig.PlayerModifiers newModifiers = new PlayerConfig.PlayerModifiers();
            
            // Добавляем новое измерение в конфиг
            playerModifiers.put(dimensionId, newModifiers);
            
            // Сохраняем изменения
            PlayerConfigManager.saveConfigs();
            
            // Обновляем интерфейс
            selectedDimension = dimensionId;
            
            // Убедиться, что мы находимся на вкладке игрока
            currentTab = TabType.PLAYER;
            
            // Возвращаемся к основному экрану и показываем настройки нового измерения
            Minecraft.getInstance().setScreen(MobScalerScreen.this);
            
            // Полностью обновляем интерфейс, чтобы применить изменения
            init();
            
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.dimension_created"), false);
        } catch (Exception e) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.dimension_creation", e.getMessage()), false);
        }
    }

    /**
     * Диалог добавления нового измерения для игрока
     */
    private class AddPlayerDimensionDialog extends Screen {
        private final int dialogWidth = 400;
        private final int dialogHeight = 300;
        private EditBox dimensionIdField;
        private List<String> suggestions = new ArrayList<>();
        private int selectedSuggestion = -1;
        private int scrollOffset = 0;
        private static final int MAX_VISIBLE_SUGGESTIONS = 8;
        
        public AddPlayerDimensionDialog() {
            super(getTranslatedText("gui.mobscaler.add_dimension_button"));
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Создаем поле ввода с автодополнением
            dimensionIdField = new EditBox(this.font, dialogX + 20, dialogY + 60, dialogWidth - 40, 20, Component.literal(""));
            dimensionIdField.setMaxLength(50);
            dimensionIdField.setValue("");
            dimensionIdField.setResponder(this::updateSuggestions);
            this.addRenderableWidget(dimensionIdField);
            
            // Кнопка создания
            Button createButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.add"),
                button -> {
                    createNewPlayerDimension(dimensionIdField.getValue().trim());
                    this.onClose();
                },
                ADD_BUTTON_COLOR,
                0xFF4CAF50
            );
            this.addRenderableWidget(createButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> this.onClose(),
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(cancelButton);
            
            // Устанавливаем фокус на поле ввода
            setInitialFocus(dimensionIdField);
        }
        
        private void updateSuggestions(String input) {
            suggestions.clear();
            selectedSuggestion = -1;
            scrollOffset = 0;
            
            if (input.isEmpty()) {
                return;
            }
            
            // Получаем список всех зарегистрированных измерений из игры
            List<String> allDimensions = new ArrayList<>();
            
            // Добавляем стандартные измерения
            allDimensions.add("minecraft:overworld");
            allDimensions.add("minecraft:the_nether");
            allDimensions.add("minecraft:the_end");
            
            // Получаем существующие измерения
            Map<String, PlayerConfig.PlayerModifiers> existingDimensions = PlayerConfigManager.getPlayerConfig().getPlayerModifiers();
            
            try {
                // Получаем все зарегистрированные измерения из реестра
                if (Minecraft.getInstance().level != null) {
                    for (ServerLevel level : Minecraft.getInstance().getSingleplayerServer().getAllLevels()) {
                        String dimensionKey = level.dimension().location().toString();
                        if (dimensionKey.toLowerCase().contains(input.toLowerCase())) {
                            suggestions.add(dimensionKey);
                        }
                    }
                }
                
                // Сортируем подсказки
                Collections.sort(suggestions);
            } catch (Exception e) {
                Minecraft.getInstance().player.displayClientMessage(
                    getTranslatedText("gui.mobscaler.error.dimension_list_error"), false);
            }
            
            // Фильтруем измерения по введенному тексту
            for (String dimensionId : allDimensions) {
                if (dimensionId.toLowerCase().contains(input.toLowerCase()) && 
                    !existingDimensions.containsKey(dimensionId)) {
                    suggestions.add(dimensionId);
                }
            }
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // Escape
                this.onClose();
                return true;
            }
            
            if (keyCode == 257 || keyCode == 335) { // Enter или NumpadEnter
                if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                    dimensionIdField.setValue(suggestions.get(selectedSuggestion));
                    selectedSuggestion = -1;
                    return true;
                } else if (!dimensionIdField.getValue().trim().isEmpty()) {
                    createNewPlayerDimension(dimensionIdField.getValue().trim());
                    this.onClose();
                    return true;
                }
            }
            
            if (keyCode == 264) { // Стрелка вниз
                if (!suggestions.isEmpty()) {
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        scrollOffset++;
                    }
                    return true;
                }
            }
            
            if (keyCode == 265) { // Стрелка вверх
                if (!suggestions.isEmpty() && selectedSuggestion > -1) {
                    selectedSuggestion--;
                    if (selectedSuggestion < scrollOffset) {
                        scrollOffset = Math.max(0, scrollOffset - 1);
                    }
                    return true;
                }
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Рисуем фон диалога
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Рисуем заголовок
            drawCenteredString(poseStack, this.font, this.title, this.width / 2, dialogY + 20, TEXT_COLOR);
            
            // Рисуем подсказку
            drawString(poseStack, this.font, getTranslatedText("gui.mobscaler.enter_dimension_id"), dialogX + 20, dialogY + 45, TEXT_COLOR);
            
            // Рисуем предложения
            if (!suggestions.isEmpty()) {
                int suggestionX = dialogX + 20;
                int suggestionY = dialogY + 85;
                int suggestionWidth = dialogWidth - 40;
                int suggestionHeight = 20;
                
                // Фон для списка предложений
                fill(poseStack, suggestionX, suggestionY, suggestionX + suggestionWidth, 
                     suggestionY + Math.min(suggestions.size(), MAX_VISIBLE_SUGGESTIONS) * suggestionHeight, 0x80000000);
                
                // Отрисовка предложений
                for (int i = scrollOffset; i < Math.min(scrollOffset + MAX_VISIBLE_SUGGESTIONS, suggestions.size()); i++) {
                    boolean isSelected = i == selectedSuggestion;
                    
                    // Выделение выбранного предложения
                    if (isSelected) {
                        fill(poseStack, suggestionX, suggestionY + (i - scrollOffset) * suggestionHeight, 
                             suggestionX + suggestionWidth, suggestionY + (i - scrollOffset + 1) * suggestionHeight, 
                             HIGHLIGHT_COLOR);
                    }
                    
                    drawString(poseStack, this.font, suggestions.get(i), 
                               suggestionX + 5, suggestionY + (i - scrollOffset) * suggestionHeight + 6, 
                               isSelected ? 0xFFFFFF00 : TEXT_COLOR);
                }
            }
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!suggestions.isEmpty()) {
                int dialogX = (this.width - dialogWidth) / 2;
                int dialogY = (this.height - dialogHeight) / 2;
                int suggestionX = dialogX + 20;
                int suggestionY = dialogY + 85;
                int suggestionWidth = dialogWidth - 40;
                int suggestionHeight = 20;
                
                if (mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth) {
                    int index = (int)((mouseY - suggestionY) / suggestionHeight) + scrollOffset;
                    if (index >= 0 && index < suggestions.size()) {
                        dimensionIdField.setValue(suggestions.get(index));
                        suggestions.clear();
                        return true;
                    }
                }
            }
            
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!suggestions.isEmpty() && suggestions.size() > MAX_VISIBLE_SUGGESTIONS) {
                if (delta > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else {
                    scrollOffset = Math.min(scrollOffset + 1, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
                }
                return true;
            }
            
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        @Override
        public void onClose() {
            super.onClose();
            // Возвращаемся к основному экрану и показываем вкладку игрока
            Minecraft.getInstance().setScreen(MobScalerScreen.this);
            // Убеждаемся, что мы на вкладке игрока
            currentTab = TabType.PLAYER;
            // Инициализируем интерфейс заново, чтобы применить изменения
            init();
        }
    }


    // Метод для получения базового значения атрибута моба
    private double getBaseAttributeValue(String entityId, String attributeName) {
        try {
            // Получаем тип сущности из регистра
            net.minecraft.world.entity.EntityType<?> entityType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                new net.minecraft.resources.ResourceLocation(entityId)
            );
            
            if (entityType != null) {
                // Создаем временную сущность для получения атрибутов
                net.minecraft.world.entity.Entity entity = entityType.create(Minecraft.getInstance().level);
                if (entity instanceof LivingEntity livingEntity) {
                    // Определяем, какой атрибут нам нужен
                    net.minecraft.world.entity.ai.attributes.Attribute attribute = null;
                    if (attributeName.contains("Health")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH;
                    } else if (attributeName.contains("Armor")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.ARMOR;
                    } else if (attributeName.contains("Damage")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
                    } else if (attributeName.contains("Speed")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
                    } else if (attributeName.contains("KnockbackResistance")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE;
                    } else if (attributeName.contains("AttackKnockback")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK;
                    } else if (attributeName.contains("AttackSpeed")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED;
                    } else if (attributeName.contains("FollowRange")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE;
                    } else if (attributeName.contains("FlyingSpeed")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED;
                    } else if (attributeName.contains("ArmorToughness")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS;
                    } else if (attributeName.contains("Luck")) {
                        attribute = net.minecraft.world.entity.ai.attributes.Attributes.LUCK;
                    }
                    
                    if (attribute != null) {
                        AttributeInstance attr = livingEntity.getAttribute(attribute);
                        if (attr != null) {
                            return attr.getBaseValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.formula_error", attributeName), false);
        }
        
        // Возвращаем значение по умолчанию, если не удалось получить базовое значение
        if (attributeName.contains("Health")) {
            return 20.0; // Стандартное здоровье
        } else if (attributeName.contains("Armor")) {
            return 0.0; // Стандартная броня
        }
        
        return 0.0;
    }
    
    // Метод для отображения формулы расчета
    private void addAttributeFormula(int x, int y, String entityId, String attributeName, double addition, double multiplier) {
        try {
            double baseValue = getBaseAttributeValue(entityId, attributeName);
            double result = (baseValue + addition) * multiplier;
            
            String formula = String.format("(%.1f + %.1f) × %.2f = %.1f", baseValue, addition, multiplier, result);
            addLabel(formula, x, y);
        } catch (Exception e) {
            Minecraft.getInstance().player.displayClientMessage(
                getTranslatedText("gui.mobscaler.error.formula_error", attributeName), false);
        }
    }
    
    // Метод для отображения диалога добавления мода в черный список
    private void showAddModToBlacklistDialog() {
        Minecraft.getInstance().setScreen(new AddModToBlacklistDialog());
    }
    
    // Метод для отображения диалога добавления моба в черный список
    private void showAddEntityToBlacklistDialog() {
        Minecraft.getInstance().setScreen(new AddEntityToBlacklistDialog());
    }
    
    // Метод для удаления мода из черного списка
    private void removeModFromBlacklist(String modId) {
        if (selectedDimension != null) {
            DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
            if (config != null) {
                try {
                    Field modBlacklistField = config.getClass().getDeclaredField("modBlacklist");
                    modBlacklistField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<String> modBlacklist = (List<String>) modBlacklistField.get(config);
                    modBlacklist.remove(modId);
                    
                    // Обновляем интерфейс
                    initDimensionSettingsContent();
                    
                    // Показываем сообщение об успешном удалении
                    Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.blacklist.mod_removed", modId), false);
                } catch (Exception e) {
                    Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.blacklist.mod_error", modId), false);
                }
            }
        }
    }
    
    // Метод для удаления моба из черного списка
    private void removeEntityFromBlacklist(String entityId) {
        if (selectedDimension != null) {
            DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
            if (config != null) {
                try {
                    Field entityBlacklistField = config.getClass().getDeclaredField("entityBlacklist");
                    entityBlacklistField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<String> entityBlacklist = (List<String>) entityBlacklistField.get(config);
                    entityBlacklist.remove(entityId);
                    
                    // Обновляем интерфейс
                    initDimensionSettingsContent();
                    
                    // Показываем сообщение об успешном удалении
                    Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.blacklist.entity_removed", entityId), false);
                } catch (Exception e) {
                    Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.blacklist.entity_error_delete", entityId), false);
                }
            }
        }
    }
    
    // Класс диалога для добавления мода в черный список
    private class AddModToBlacklistDialog extends Screen {
        private final int dialogWidth = 400;
        private final int dialogHeight = 300;
        private EditBox modIdField;
        private List<String> suggestions = new ArrayList<>();
        private int selectedSuggestion = -1;
        private int scrollOffset = 0;
        private static final int MAX_VISIBLE_SUGGESTIONS = 8;
        
        public AddModToBlacklistDialog() {
            super(getTranslatedText("gui.mobscaler.blacklist.add_mod_title"));
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Создаем поле ввода с автодополнением
            modIdField = new EditBox(this.font, dialogX + 20, dialogY + 60, dialogWidth - 40, 20, Component.literal(""));
            modIdField.setMaxLength(50);
            modIdField.setValue("");
            modIdField.setResponder(this::updateSuggestions);
            this.addRenderableWidget(modIdField);
            
            // Кнопка добавления
            Button addButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.add"),
                button -> {
                    String modId = modIdField.getValue().trim();
                    if (!modId.isEmpty()) {
                        addModToBlacklist(modId);
                    }
                },
                ADD_BUTTON_COLOR,
                0xFF55FF55
            );
            this.addRenderableWidget(addButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> Minecraft.getInstance().setScreen(MobScalerScreen.this),
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(cancelButton);
            
            // Инициализируем список предложений
            updateSuggestions("");
        }
        
        private void updateSuggestions(String input) {
            suggestions.clear();
            selectedSuggestion = -1;
            scrollOffset = 0;
            
            // Получаем список всех модов
            net.minecraftforge.fml.ModList modList = net.minecraftforge.fml.ModList.get();
            for (net.minecraftforge.forgespi.language.IModInfo mod : modList.getMods()) {
                String modId = mod.getModId();
                if (modId.toLowerCase().contains(input.toLowerCase())) {
                    suggestions.add(modId);
                }
            }
            
            // Сортируем предложения
            suggestions.sort(String::compareTo);
        }
        
        private void addModToBlacklist(String modId) {
            if (selectedDimension != null) {
                DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
                if (config != null) {
                    try {
                        Field modBlacklistField = config.getClass().getDeclaredField("modBlacklist");
                        modBlacklistField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        List<String> modBlacklist = (List<String>) modBlacklistField.get(config);
                        
                        // Проверяем, не добавлен ли уже этот мод
                        if (!modBlacklist.contains(modId)) {
                            modBlacklist.add(modId);
                            
                            // Показываем сообщение об успешном добавлении
                            Minecraft.getInstance().player.displayClientMessage(
                                    getTranslatedText("gui.mobscaler.blacklist.mod_added", modId), false);
                        } else {
                            // Показываем сообщение, что мод уже в черном списке
                            Minecraft.getInstance().player.displayClientMessage(
                                    getTranslatedText("gui.mobscaler.blacklist.mod_already", modId), false);
                        }
                        
                        // Сохраняем текущее измерение
                        String currentDimension = selectedDimension;
                        
                        // Возвращаемся к основному экрану
                        Minecraft.getInstance().setScreen(MobScalerScreen.this);
                        
                        // Восстанавливаем выбранное измерение
                        selectedDimension = currentDimension;
                        currentTab = TabType.DIMENSIONS;
                        
                        // Обновляем интерфейс
                        initDimensionSettingsContent();
                    } catch (Exception e) {
                        Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.blacklist.mod_error", modId), false);
                    }
                }
            }
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Рисуем фон диалога
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Рисуем заголовок
            drawCenteredString(poseStack, this.font, this.title, this.width / 2, dialogY + 20, TEXT_COLOR);
            
            // Рисуем подсказку
            drawString(poseStack, this.font, getTranslatedText("gui.mobscaler.enter_mod_id"), dialogX + 20, dialogY + 45, TEXT_COLOR);
            
            // Рисуем список предложений
            if (!suggestions.isEmpty()) {
                int suggestionX = dialogX + 20;
                int suggestionY = dialogY + 85;
                int suggestionWidth = dialogWidth - 40;
                int suggestionHeight = 20;
                
                // Рисуем фон для списка предложений
                fill(poseStack, suggestionX, suggestionY, 
                     suggestionX + suggestionWidth, 
                     suggestionY + Math.min(suggestions.size(), MAX_VISIBLE_SUGGESTIONS) * suggestionHeight, 
                     0x80000000);
                
                // Рисуем предложения
                for (int i = 0; i < Math.min(suggestions.size() - scrollOffset, MAX_VISIBLE_SUGGESTIONS); i++) {
                    int index = i + scrollOffset;
                    String suggestion = suggestions.get(index);
                    
                    // Выделяем выбранное предложение
                    if (index == selectedSuggestion) {
                        fill(poseStack, suggestionX, suggestionY + i * suggestionHeight, 
                             suggestionX + suggestionWidth, suggestionY + (i + 1) * suggestionHeight, 
                             HIGHLIGHT_COLOR);
                    }
                    
                    drawString(poseStack, this.font, suggestion, 
                               suggestionX + 5, suggestionY + i * suggestionHeight + 6, 
                               TEXT_COLOR);
                }
            }
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // Escape
                Minecraft.getInstance().setScreen(MobScalerScreen.this);
                return true;
            }
            
            if (!suggestions.isEmpty()) {
                if (keyCode == 264) { // Down arrow
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        scrollOffset++;
                    }
                    return true;
                } else if (keyCode == 265) { // Up arrow
                    selectedSuggestion = Math.max(selectedSuggestion - 1, 0);
                    if (selectedSuggestion < scrollOffset) {
                        scrollOffset--;
                    }
                    return true;
                } else if (keyCode == 257 || keyCode == 335) { // Enter or numpad enter
                    if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                        modIdField.setValue(suggestions.get(selectedSuggestion));
                    }
                    return true;
                }
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!suggestions.isEmpty()) {
                int dialogX = (this.width - dialogWidth) / 2;
                int dialogY = (this.height - dialogHeight) / 2;
                int suggestionX = dialogX + 20;
                int suggestionY = dialogY + 85;
                int suggestionWidth = dialogWidth - 40;
                int suggestionHeight = 20;
                
                if (mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth) {
                    for (int i = 0; i < Math.min(suggestions.size() - scrollOffset, MAX_VISIBLE_SUGGESTIONS); i++) {
                        if (mouseY >= suggestionY + i * suggestionHeight && 
                            mouseY <= suggestionY + (i + 1) * suggestionHeight) {
                            int index = i + scrollOffset;
                            modIdField.setValue(suggestions.get(index));
                            return true;
                        }
                    }
                }
            }
            
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!suggestions.isEmpty() && suggestions.size() > MAX_VISIBLE_SUGGESTIONS) {
                if (delta > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else {
                    scrollOffset = Math.min(scrollOffset + 1, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
                }
                return true;
            }
            
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
    }
    
    // Класс диалога для добавления моба в черный список
    private class AddEntityToBlacklistDialog extends Screen {
        private final int dialogWidth = 400;
        private final int dialogHeight = 300;
        private EditBox entityIdField;
        private List<String> suggestions = new ArrayList<>();
        private int selectedSuggestion = -1;
        private int scrollOffset = 0;
        private static final int MAX_VISIBLE_SUGGESTIONS = 8;
        
        public AddEntityToBlacklistDialog() {
            super(getTranslatedText("gui.mobscaler.blacklist.add_entity_title"));
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int buttonWidth = 100;
            int buttonHeight = 20;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Создаем поле ввода с автодополнением
            entityIdField = new EditBox(this.font, dialogX + 20, dialogY + 60, dialogWidth - 40, 20, Component.literal(""));
            entityIdField.setMaxLength(50);
            entityIdField.setValue("");
            entityIdField.setResponder(this::updateSuggestions);
            this.addRenderableWidget(entityIdField);
            
            // Кнопка добавления
            Button addButton = new StyledButton(
                centerX - buttonWidth - 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.add"),
                button -> {
                    String entityId = entityIdField.getValue().trim();
                    if (!entityId.isEmpty()) {
                        addEntityToBlacklist(entityId);
                    }
                },
                ADD_BUTTON_COLOR,
                0xFF55FF55
            );
            this.addRenderableWidget(addButton);
            
            // Кнопка отмены
            Button cancelButton = new StyledButton(
                centerX + 5,
                centerY + dialogHeight/2 - 40,
                buttonWidth,
                buttonHeight,
                getTranslatedText("gui.mobscaler.buttons.cancel"),
                button -> Minecraft.getInstance().setScreen(MobScalerScreen.this),
                DELETE_BUTTON_COLOR,
                0xFFFF5555
            );
            this.addRenderableWidget(cancelButton);
            
            // Инициализируем список предложений
            updateSuggestions("");
        }
        
        private void updateSuggestions(String input) {
            suggestions.clear();
            selectedSuggestion = -1;
            scrollOffset = 0;
            
            // Получаем список всех типов сущностей
            for (ResourceLocation entityId : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKeys()) {
                String entityIdStr = entityId.toString();
                if (entityIdStr.toLowerCase().contains(input.toLowerCase())) {
                    suggestions.add(entityIdStr);
                }
            }
            
            // Сортируем предложения
            suggestions.sort(String::compareTo);
        }
        
        private void addEntityToBlacklist(String entityId) {
            if (selectedDimension != null) {
                DimensionConfig config = dimensionConfigsCopy.get(selectedDimension);
                if (config != null) {
                    try {
                        Field entityBlacklistField = config.getClass().getDeclaredField("entityBlacklist");
                        entityBlacklistField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        List<String> entityBlacklist = (List<String>) entityBlacklistField.get(config);
                        
                        // Проверяем, не добавлен ли уже этот моб
                        if (!entityBlacklist.contains(entityId)) {
                            entityBlacklist.add(entityId);
                            
                            // Показываем сообщение об успешном добавлении
                            Minecraft.getInstance().player.displayClientMessage(
                                    getTranslatedText("gui.mobscaler.blacklist.entity_added", entityId), false);
                        } else {
                            // Показываем сообщение, что моб уже в черном списке
                            Minecraft.getInstance().player.displayClientMessage(
                                    getTranslatedText("gui.mobscaler.blacklist.entity_already", entityId), false);
                        }
                        
                        // Сохраняем текущее измерение
                        String currentDimension = selectedDimension;
                        
                        // Возвращаемся к основному экрану
                        Minecraft.getInstance().setScreen(MobScalerScreen.this);
                        
                        // Восстанавливаем выбранное измерение и вкладку
                        selectedDimension = currentDimension;
                        currentTab = TabType.DIMENSIONS;
                        
                        // Обновляем интерфейс
                        initDimensionSettingsContent();
                    } catch (Exception e) {
                        Minecraft.getInstance().player.displayClientMessage(
                            getTranslatedText("gui.mobscaler.blacklist.entity_error", entityId), false);
                    }
                }
            }
        }
        
        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(poseStack);
            
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;
            
            // Рисуем фон диалога
            fill(poseStack, dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, BACKGROUND_COLOR);
            
            // Рисуем заголовок
            drawCenteredString(poseStack, this.font, this.title, this.width / 2, dialogY + 20, TEXT_COLOR);
            
            // Рисуем подсказку
            drawString(poseStack, this.font, getTranslatedText("gui.mobscaler.enter_mob_id"), dialogX + 20, dialogY + 45, TEXT_COLOR);
            
            // Рисуем список предложений
            if (!suggestions.isEmpty()) {
                int suggestionX = dialogX + 20;
                int suggestionY = dialogY + 85;
                int suggestionWidth = dialogWidth - 40;
                int suggestionHeight = 20;
                
                // Рисуем фон для списка предложений
                fill(poseStack, suggestionX, suggestionY, 
                     suggestionX + suggestionWidth, 
                     suggestionY + Math.min(suggestions.size(), MAX_VISIBLE_SUGGESTIONS) * suggestionHeight, 
                     0x80000000);
                
                // Рисуем предложения
                for (int i = 0; i < Math.min(suggestions.size() - scrollOffset, MAX_VISIBLE_SUGGESTIONS); i++) {
                    int index = i + scrollOffset;
                    String suggestion = suggestions.get(index);
                    
                    // Выделяем выбранное предложение
                    if (index == selectedSuggestion) {
                        fill(poseStack, suggestionX, suggestionY + i * suggestionHeight, 
                             suggestionX + suggestionWidth, suggestionY + (i + 1) * suggestionHeight, 
                             HIGHLIGHT_COLOR);
                    }
                    
                    drawString(poseStack, this.font, suggestion, 
                               suggestionX + 5, suggestionY + i * suggestionHeight + 6, 
                               TEXT_COLOR);
                }
            }
            
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // Escape
                Minecraft.getInstance().setScreen(MobScalerScreen.this);
                return true;
            }
            
            if (!suggestions.isEmpty()) {
                if (keyCode == 264) { // Down arrow
                    selectedSuggestion = Math.min(selectedSuggestion + 1, suggestions.size() - 1);
                    if (selectedSuggestion >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        scrollOffset++;
                    }
                    return true;
                } else if (keyCode == 265) { // Up arrow
                    selectedSuggestion = Math.max(selectedSuggestion - 1, 0);
                    if (selectedSuggestion < scrollOffset) {
                        scrollOffset--;
                    }
                    return true;
                } else if (keyCode == 257 || keyCode == 335) { // Enter or numpad enter
                    if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                        entityIdField.setValue(suggestions.get(selectedSuggestion));
                    }
                    return true;
                }
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!suggestions.isEmpty()) {
                int dialogX = (this.width - dialogWidth) / 2;
                int dialogY = (this.height - dialogHeight) / 2;
                int suggestionX = dialogX + 20;
                int suggestionY = dialogY + 85;
                int suggestionWidth = dialogWidth - 40;
                int suggestionHeight = 20;
                
                if (mouseX >= suggestionX && mouseX <= suggestionX + suggestionWidth) {
                    for (int i = 0; i < Math.min(suggestions.size() - scrollOffset, MAX_VISIBLE_SUGGESTIONS); i++) {
                        if (mouseY >= suggestionY + i * suggestionHeight && 
                            mouseY <= suggestionY + (i + 1) * suggestionHeight) {
                            int index = i + scrollOffset;
                            entityIdField.setValue(suggestions.get(index));
                            return true;
                        }
                    }
                }
            }
            
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!suggestions.isEmpty() && suggestions.size() > MAX_VISIBLE_SUGGESTIONS) {
                if (delta > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else {
                    scrollOffset = Math.min(scrollOffset + 1, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
                }
                return true;
            }
            
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
    }

    // Добавляем класс для прокручиваемой панели черного списка
    private class BlacklistScrollPanel extends AbstractWidget {
        private final List<String> items;
        private final boolean isMod; // true для модов, false для мобов
        private int scrollOffset = 0;
        private final int itemHeight = 20;
        private final int maxVisibleItems;
        
        public BlacklistScrollPanel(int x, int y, int width, int height, List<String> items, boolean isMod) {
            super(x, y, width, height, Component.literal(""));
            this.items = items;
            this.isMod = isMod;
            this.maxVisibleItems = height / itemHeight;
        }
        
        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            // Рисуем фон панели
            fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, 0x80000000);
            
            // Рисуем элементы списка
            int visibleItems = Math.min(items.size() - scrollOffset, maxVisibleItems);
            for (int i = 0; i < visibleItems; i++) {
                int index = i + scrollOffset;
                String item = items.get(index);
                
                // Обрезаем длинные имена
                String displayName = item;
                if (font.width(displayName) > this.width - 30) {
                    displayName = font.plainSubstrByWidth(displayName, this.width - 30) + "...";
                }
                
                // Рисуем элемент
                drawString(poseStack, font, displayName, this.x + 5, this.y + i * itemHeight + 5, TEXT_COLOR);
                
                // Рисуем кнопку удаления
                int buttonX = this.x + this.width - 20;
                int buttonY = this.y + i * itemHeight;
                
                // Проверяем, наведена ли мышь на кнопку удаления
                boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + 15 && 
                                   mouseY >= buttonY && mouseY <= buttonY + 15;
                
                // Рисуем фон кнопки
                fill(poseStack, buttonX, buttonY, buttonX + 15, buttonY + 15, 
                    isHovered ? 0xFFFF0000 : 0xFFAA0000);
                
                // Рисуем X
                drawCenteredString(poseStack, font, "X", buttonX + 7, buttonY + 3, 0xFFFFFFFF);
            }
            
            // Рисуем полосу прокрутки, если элементов больше, чем может поместиться
            if (items.size() > maxVisibleItems) {
                int scrollBarX = this.x + this.width - 5;
                int scrollBarWidth = 3;
                
                // Фон полосы прокрутки
                fill(poseStack, scrollBarX, this.y, scrollBarX + scrollBarWidth, this.y + this.height, 0x80AAAAAA);
                
                // Ползунок прокрутки
                float scrollRatio = (float) scrollOffset / (items.size() - maxVisibleItems);
                int thumbHeight = Math.max(20, this.height * maxVisibleItems / items.size());
                int thumbY = this.y + (int) (scrollRatio * (this.height - thumbHeight));
                
                fill(poseStack, scrollBarX, thumbY, scrollBarX + scrollBarWidth, thumbY + thumbHeight, 0xFFFFFFFF);
            }
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && mouseX >= this.x && mouseX <= this.x + this.width && 
                mouseY >= this.y && mouseY <= this.y + this.height) {
                
                // Проверяем, кликнули ли на кнопку удаления
                int visibleItems = Math.min(items.size() - scrollOffset, maxVisibleItems);
                for (int i = 0; i < visibleItems; i++) {
                    int index = i + scrollOffset;
                    int buttonX = this.x + this.width - 20;
                    int buttonY = this.y + i * itemHeight;
                    
                    if (mouseX >= buttonX && mouseX <= buttonX + 15 && 
                        mouseY >= buttonY && mouseY <= buttonY + 15) {
                        
                        // Удаляем элемент
                        String item = items.get(index);
                        if (isMod) {
                            removeModFromBlacklist(item);
                        } else {
                            removeEntityFromBlacklist(item);
                        }
                        return true;
                    }
                }
                
                return true;
            }
            return false;
        }
        
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (mouseX >= this.x && mouseX <= this.x + this.width && 
                mouseY >= this.y && mouseY <= this.y + this.height) {
                
                if (items.size() > maxVisibleItems) {
                    int maxScroll = items.size() - maxVisibleItems;
                    scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(delta), maxScroll));
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
            // Реализация для доступности
            narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Черный список"));
        }
    }
} 