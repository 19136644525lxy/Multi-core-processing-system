package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.config.MCPSConfig;
import com.qituo.mcps.platform.PlatformManager;

public class OptimizationConfigScreen extends Screen {
    private static final int WIDTH = 450;
    private static final int HEIGHT = 400;
    
    private MCPSConfig config;
    private PlatformManager platformManager;
    
    private CheckboxWidget enableParallelProcessingCheckbox;
    private CheckboxWidget enableGpuAccelerationCheckbox;
    private CheckboxWidget enableCloudBackupCheckbox;
    
    private TextFieldWidget threadPoolSizeField;
    private TextFieldWidget renderDistanceField;
    private TextFieldWidget entityProcessingLimitField;
    
    private ButtonWidget applyButton;
    private ButtonWidget resetButton;
    private ButtonWidget closeButton;
    private ButtonWidget presetLowButton;
    private ButtonWidget presetMediumButton;
    private ButtonWidget presetHighButton;
    
    public OptimizationConfigScreen() {
        super(Text.translatable("mcps.screen.optimization_config.title"));
        this.platformManager = PlatformManager.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 启用并行处理
        enableParallelProcessingCheckbox = new CheckboxWidget(
            centerX + 20, centerY + 40,
            20, 20,
            Text.translatable("mcps.screen.optimization_config.enable_parallel"),
            true
        );
        this.addDrawableChild(enableParallelProcessingCheckbox);
        
        // 启用GPU加速
        enableGpuAccelerationCheckbox = new CheckboxWidget(
            centerX + 20, centerY + 70,
            20, 20,
            Text.translatable("mcps.screen.optimization_config.enable_gpu"),
            true
        );
        this.addDrawableChild(enableGpuAccelerationCheckbox);
        
        // 启用云备份
        enableCloudBackupCheckbox = new CheckboxWidget(
            centerX + 20, centerY + 100,
            20, 20,
            Text.translatable("mcps.screen.optimization_config.enable_cloud_backup"),
            true
        );
        this.addDrawableChild(enableCloudBackupCheckbox);
        
        // 线程池大小
        threadPoolSizeField = new TextFieldWidget(this.textRenderer, centerX + 150, centerY + 135, 100, 20, Text.translatable("mcps.screen.optimization_config.thread_pool_size"));
        threadPoolSizeField.setText("4");
        this.addDrawableChild(threadPoolSizeField);
        
        // 渲染距离
        renderDistanceField = new TextFieldWidget(this.textRenderer, centerX + 150, centerY + 165, 100, 20, Text.translatable("mcps.screen.optimization_config.render_distance"));
        renderDistanceField.setText("10");
        this.addDrawableChild(renderDistanceField);
        
        // 实体处理限制
        entityProcessingLimitField = new TextFieldWidget(this.textRenderer, centerX + 150, centerY + 195, 100, 20, Text.translatable("mcps.screen.optimization_config.entity_limit"));
        entityProcessingLimitField.setText("100");
        this.addDrawableChild(entityProcessingLimitField);
        
        // 预设按钮
        presetLowButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.optimization_config.preset.low"),
            button -> applyPreset("low")
        ).position(centerX + 20, centerY + 240).size(80, 20).build();
        this.addDrawableChild(presetLowButton);
        
        presetMediumButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.optimization_config.preset.medium"),
            button -> applyPreset("medium")
        ).position(centerX + 110, centerY + 240).size(80, 20).build();
        this.addDrawableChild(presetMediumButton);
        
        presetHighButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.optimization_config.preset.high"),
            button -> applyPreset("high")
        ).position(centerX + 200, centerY + 240).size(80, 20).build();
        this.addDrawableChild(presetHighButton);
        
        // 应用按钮
        applyButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.optimization_config.apply"),
            button -> this.applyConfig()
        ).position(centerX + 20, centerY + 280).size(80, 20).build();
        this.addDrawableChild(applyButton);
        
        // 重置按钮
        resetButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.optimization_config.reset"),
            button -> this.resetConfig()
        ).position(centerX + 110, centerY + 280).size(80, 20).build();
        this.addDrawableChild(resetButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.optimization_config.close"),
            button -> this.close()
        ).position(centerX + 200, centerY + 280).size(80, 20).build();
        this.addDrawableChild(closeButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 标题
        context.drawTextWithShadow(this.textRenderer, this.title, centerX + 20, centerY + 20, 0xFFFFFF);
        
        // 硬件信息
        String hardwareInfo = String.format("CPU: %d cores, Memory: %.2f GB", 
            Runtime.getRuntime().availableProcessors(), 
            Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0)
        );
        context.drawTextWithShadow(this.textRenderer, hardwareInfo, centerX + 20, centerY + 320, 0xFFFFFF);
    }
    
    private void applyPreset(String preset) {
        switch (preset) {
            case "low":
                threadPoolSizeField.setText(String.valueOf(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
                renderDistanceField.setText("8");
                entityProcessingLimitField.setText("50");
                break;
            case "medium":
                threadPoolSizeField.setText(String.valueOf(Runtime.getRuntime().availableProcessors() - 1));
                renderDistanceField.setText("12");
                entityProcessingLimitField.setText("100");
                break;
            case "high":
                threadPoolSizeField.setText(String.valueOf(Runtime.getRuntime().availableProcessors()));
                renderDistanceField.setText("16");
                entityProcessingLimitField.setText("200");
                break;
        }
    }
    
    private void applyConfig() {
        try {
            // 这里可以添加配置保存逻辑
            this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.optimization_config.saved"));
        } catch (NumberFormatException e) {
            this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.optimization_config.invalid"));
        }
    }
    
    private void resetConfig() {
        // 重置为默认值
        threadPoolSizeField.setText("4");
        renderDistanceField.setText("10");
        entityProcessingLimitField.setText("100");
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new OptimizationConfigScreen());
    }
}