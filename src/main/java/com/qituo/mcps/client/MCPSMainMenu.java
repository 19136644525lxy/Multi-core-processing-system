package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MCPSMainMenu extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 350;
    
    private ButtonWidget performanceMonitorButton;
    private ButtonWidget taskSchedulerButton;
    private ButtonWidget optimizationConfigButton;
    private ButtonWidget closeButton;
    
    public MCPSMainMenu() {
        super(Text.translatable("mcps.screen.main_menu.title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 性能监控按钮
        performanceMonitorButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.performance_monitor"),
            button -> AdvancedPerformanceMonitor.open()
        ).position(centerX + 50, centerY + 50).size(200, 20).build();
        this.addDrawableChild(performanceMonitorButton);
        
        // 任务调度可视化按钮
        taskSchedulerButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.task_scheduler"),
            button -> TaskSchedulerVisualizer.open()
        ).position(centerX + 50, centerY + 90).size(200, 20).build();
        this.addDrawableChild(taskSchedulerButton);
        
        // 优化配置按钮
        optimizationConfigButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.optimization_config"),
            button -> OptimizationConfigScreen.open()
        ).position(centerX + 50, centerY + 130).size(200, 20).build();
        this.addDrawableChild(optimizationConfigButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.close"),
            button -> this.close()
        ).position(centerX + 50, centerY + 170).size(200, 20).build();
        this.addDrawableChild(closeButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 标题
        context.drawTextWithShadow(this.textRenderer, this.title, centerX + 50, centerY + 20, 0xFFFFFF);
        
        // 版本信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.main_menu.version"), centerX + 50, centerY + 210, 0x808080);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new MCPSMainMenu());
    }
}