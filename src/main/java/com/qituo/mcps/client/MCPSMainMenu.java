package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.monitor.PerformanceMonitor;
import com.qituo.mcps.client.CloudServicesScreen;
import com.qituo.mcps.client.SystemInfoScreen;

public class MCPSMainMenu extends Screen {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;
    
    private ButtonWidget performanceMonitorButton;
    private ButtonWidget taskSchedulerButton;
    private ButtonWidget optimizationConfigButton;
    private ButtonWidget cloudServicesButton;
    private ButtonWidget systemInfoButton;
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
        ).position(centerX + 50, centerY + 50).size(300, 25).build();
        this.addDrawableChild(performanceMonitorButton);
        
        // 任务调度可视化按钮
        taskSchedulerButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.task_scheduler"),
            button -> TaskSchedulerVisualizer.open()
        ).position(centerX + 50, centerY + 90).size(300, 25).build();
        this.addDrawableChild(taskSchedulerButton);
        
        // 优化配置按钮
        optimizationConfigButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.optimization_config"),
            button -> OptimizationConfigScreen.open()
        ).position(centerX + 50, centerY + 130).size(300, 25).build();
        this.addDrawableChild(optimizationConfigButton);
        
        // 云服务按钮
        cloudServicesButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.cloud_services"),
            button -> CloudServicesScreen.open()
        ).position(centerX + 50, centerY + 170).size(300, 25).build();
        this.addDrawableChild(cloudServicesButton);
        
        // 系统信息按钮
        systemInfoButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.system_info"),
            button -> SystemInfoScreen.open()
        ).position(centerX + 50, centerY + 210).size(300, 25).build();
        this.addDrawableChild(systemInfoButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.main_menu.close"),
            button -> this.close()
        ).position(centerX + 50, centerY + 250).size(300, 25).build();
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
        
        // 系统状态信息
        drawSystemStatus(context, centerX + 50, centerY + 290, 300, 80);
        
        // 版本信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.main_menu.version"), centerX + 50, centerY + 370, 0x808080);
    }
    
    private void drawSystemStatus(DrawContext context, int x, int y, int width, int height) {
        // 绘制系统状态信息
        int currentY = y;
        
        // 获取线程管理器和性能监控器
        ThreadManager threadManager = MCPSMod.getInstance().getThreadManager();
        PerformanceMonitor performanceMonitor = MCPSMod.getInstance().getPerformanceMonitor();
        
        // 线程信息
        if (threadManager != null) {
            String threadInfo = Text.translatable("mcps.screen.main_menu.threads").getString() + ": " + threadManager.getCorePoolSize();
            context.drawTextWithShadow(this.textRenderer, threadInfo, x, currentY, 0xFFFFFF);
            currentY += 15;
        }
        
        // 性能信息
        if (performanceMonitor != null) {
            String msptInfo = Text.translatable("mcps.screen.main_menu.mspt").getString() + ": " + String.format("%.2f", performanceMonitor.getMspt());
            context.drawTextWithShadow(this.textRenderer, msptInfo, x, currentY, 0xFFFFFF);
            currentY += 15;
            
            String tpsInfo = Text.translatable("mcps.screen.main_menu.tps").getString() + ": " + String.format("%.2f", performanceMonitor.getTps());
            context.drawTextWithShadow(this.textRenderer, tpsInfo, x, currentY, 0xFFFFFF);
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new MCPSMainMenu());
    }
}