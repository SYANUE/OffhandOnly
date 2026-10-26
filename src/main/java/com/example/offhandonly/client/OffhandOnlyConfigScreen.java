package com.example.offhandonly.client;

import com.example.offhandonly.OffhandOnlyConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * 配置界面（纯原版控件，不依赖 Cloth Config）。
 *
 * <p>布局按 240 逻辑像素高度排的（GUI 缩放 2 时 854x480 窗口的默认高度），
 * 再小就会挤，但控件本身仍然可点。
 */
public class OffhandOnlyConfigScreen extends Screen {

    private static final int PANEL_WIDTH = 310;
    private static final int COLUMN_WIDTH = 150;
    private static final int COLUMN_GAP = 10;

    private final Screen parent;
    private TextFieldWidget whitelistField;

    public OffhandOnlyConfigScreen(Screen parent) {
        super(Text.translatable("offhandonly.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 界面重建（例如窗口缩放）前，先把上一次输入的白名单落盘，避免丢失
        this.commitWhitelist();

        OffhandOnlyConfig config = OffhandOnlyConfig.get();
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = left + COLUMN_WIDTH + COLUMN_GAP;
        int y = 30;

        this.addDrawableChild(toggle(left, y, COLUMN_WIDTH, "offhandonly.config.attack",
                () -> config.disableAttack, v -> config.disableAttack = v));
        this.addDrawableChild(toggle(right, y, COLUMN_WIDTH, "offhandonly.config.block_breaking",
                () -> config.disableBlockBreaking, v -> config.disableBlockBreaking = v));

        y += 21;
        this.addDrawableChild(toggle(left, y, COLUMN_WIDTH, "offhandonly.config.offhand_mining",
                () -> config.offhandMining, v -> config.offhandMining = v));
        this.addDrawableChild(toggle(right, y, COLUMN_WIDTH, "offhandonly.config.offhand_attack",
                () -> config.offhandAttack, v -> config.offhandAttack = v));

        y += 21;
        this.addDrawableChild(toggle(left, y, COLUMN_WIDTH, "offhandonly.config.offhand_empty_attack",
                () -> config.offhandEmptyHandAttack, v -> config.offhandEmptyHandAttack = v));
        this.addDrawableChild(toggle(right, y, COLUMN_WIDTH, "offhandonly.config.offhand_interact",
                () -> config.forceOffhandOnly, v -> config.forceOffhandOnly = v));

        y += 21;
        this.addDrawableChild(toggle(left, y, COLUMN_WIDTH, "offhandonly.config.survival_only",
                () -> config.survivalOnly, v -> config.survivalOnly = v));
        this.addDrawableChild(toggle(right, y, COLUMN_WIDTH, "offhandonly.config.render_main_hand",
                () -> config.renderMainHand, v -> config.renderMainHand = v));

        y += 21;
        this.addDrawableChild(toggle(left, y, COLUMN_WIDTH, "offhandonly.config.render_offhand",
                () -> config.renderOffhand, v -> config.renderOffhand = v));
        this.addDrawableChild(toggle(right, y, COLUMN_WIDTH, "offhandonly.config.debug",
                () -> config.debug, v -> config.debug = v));

        y += 23;
        this.addDrawableChild(new PercentSlider(left, y, PANEL_WIDTH, 20,
                OffhandOnlyConfig.MIN_SPEED, OffhandOnlyConfig.MAX_SPEED,
                config.offhandMiningSpeed, "offhandonly.config.mining_speed",
                v -> config.offhandMiningSpeed = v));

        y += 22;
        this.addDrawableChild(new PercentSlider(left, y, PANEL_WIDTH, 20,
                OffhandOnlyConfig.MIN_DAMAGE, OffhandOnlyConfig.MAX_DAMAGE,
                config.offhandAttackDamage, "offhandonly.config.attack_damage",
                v -> config.offhandAttackDamage = v));

        this.whitelistField = new TextFieldWidget(this.textRenderer, left, 192, PANEL_WIDTH, 20,
                Text.translatable("offhandonly.config.whitelist"));
        this.whitelistField.setMaxLength(512);
        this.whitelistField.setText(String.join(", ", config.playerWhitelist));
        this.addDrawableChild(this.whitelistField);

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 26, 200, 20)
                .build());
    }

    private ButtonWidget toggle(int x, int y, int width, String key,
                                BooleanSupplier getter, Consumer<Boolean> setter) {
        return ButtonWidget.builder(label(key, getter.getAsBoolean()), button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(label(key, getter.getAsBoolean()));
            OffhandOnlyConfig.save();
        }).dimensions(x, y, width, 20).build();
    }

    private static Text label(String key, boolean on) {
        return Text.translatable(key)
                .append(Text.literal(": "))
                .append(Text.translatable(on ? "offhandonly.config.on" : "offhandonly.config.off"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("offhandonly.config.whitelist_hint"),
                this.width / 2 - PANEL_WIDTH / 2, 181, 0xA0A0A0);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.commitWhitelist();
        OffhandOnlyConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private void commitWhitelist() {
        if (this.whitelistField == null) {
            return;
        }
        List<String> parsed = new ArrayList<>();
        for (String part : this.whitelistField.getText().split("[,;\\s]+")) {
            String entry = part.trim();
            if (!entry.isEmpty()) {
                parsed.add(entry);
            }
        }
        OffhandOnlyConfig.get().playerWhitelist = parsed;
    }

    /** 百分比滑块，步进 5%。 */
    private static final class PercentSlider extends SliderWidget {

        private final double min;
        private final double max;
        private final String labelKey;
        private final DoubleConsumer setter;

        PercentSlider(int x, int y, int width, int height, double min, double max, double initial,
                      String labelKey, DoubleConsumer setter) {
            super(x, y, width, height, Text.empty(), normalize(initial, min, max));
            this.min = min;
            this.max = max;
            this.labelKey = labelKey;
            this.setter = setter;
            this.updateMessage();
        }

        private static double normalize(double value, double min, double max) {
            double clamped = Math.max(min, Math.min(max, value));
            return (clamped - min) / (max - min);
        }

        private double current() {
            double raw = this.min + this.value * (this.max - this.min);
            return Math.round(raw * 20.0D) / 20.0D;
        }

        @Override
        protected void updateMessage() {
            String percent = String.format(Locale.ROOT, "%.0f%%", current() * 100.0D);
            this.setMessage(Text.translatable(this.labelKey, percent));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(current());
        }
    }
}
