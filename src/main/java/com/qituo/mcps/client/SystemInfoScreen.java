package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.monitor.PerformanceMonitor;

public class SystemInfoScreen extends Screen {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;
    
    private ButtonWidget closeButton;
    
    public SystemInfoScreen() {
        super(Text.translatable("mcps.screen.system_info.title"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.system_info.close"),
            button -> this.close()
        ).position(centerX + 50, centerY + 320).size(300, 25).build();
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
        
        // 系统信息
        drawSystemInfo(context, centerX + 50, centerY + 50, 300, 250);
    }
    
    private void drawSystemInfo(DrawContext context, int x, int y, int width, int height) {
        // 绘制系统信息
        int currentY = y;
        
        // 获取线程管理器和性能监控器
        ThreadManager threadManager = MCPSMod.getInstance().getThreadManager();
        PerformanceMonitor performanceMonitor = MCPSMod.getInstance().getPerformanceMonitor();
        
        // JVM信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.system_info.jvm"), x, currentY, 0xFFFFFF);
        currentY += 15;
        
        String javaVersion = System.getProperty("java.version");
        context.drawTextWithShadow(this.textRenderer, "  " + Text.translatable("mcps.screen.system_info.java_version").getString() + ": " + javaVersion, x, currentY, 0xAAAAAA);
        currentY += 15;
        
        String jvmName = System.getProperty("java.vm.name");
        context.drawTextWithShadow(this.textRenderer, "  " + Text.translatable("mcps.screen.system_info.jvm_name").getString() + ": " + jvmName, x, currentY, 0xAAAAAA);
        currentY += 15;
        
        // 系统信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.system_info.system"), x, currentY, 0xFFFFFF);
        currentY += 15;
        
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        context.drawTextWithShadow(this.textRenderer, "  " + Text.translatable("mcps.screen.system_info.os").getString() + ": " + osName + " " + osVersion, x, currentY, 0xAAAAAA);
        currentY += 15;
        
        int processors = Runtime.getRuntime().availableProcessors();
        context.drawTextWithShadow(this.textRenderer, "  " + Text.translatable("mcps.screen.system_info.processors").getString() + ": " + processors, x, currentY, 0xAAAAAA);
        currentY += 15;
        
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        context.drawTextWithShadow(this.textRenderer, "  " + Text.translatable("mcps.screen.system_info.max_memory").getString() + ": " + maxMemory + " MB", x, currentY, 0xAAAAAA);
        currentY += 15;
        
        // MCPS信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.system_info.mcps"), x, currentY, 0xFFFFFF);
        currentY += 15;
        
        if (threadManager != null) {
            String threadInfo = "  " + Text.translatable("mcps.screen.system_info.thread_pool").getString() + ": " + threadManager.getCorePoolSize();
            context.drawTextWithShadow(this.textRenderer, threadInfo, x, currentY, 0xAAAAAA);
            currentY += 15;
        }
        
        if (performanceMonitor != null) {
            String msptInfo = "  " + Text.translatable("mcps.screen.main_menu.mspt").getString() + ": " + String.format("%.2f", performanceMonitor.getMspt());
            context.drawTextWithShadow(this.textRenderer, msptInfo, x, currentY, 0xAAAAAA);
            currentY += 15;
            
            String tpsInfo = "  " + Text.translatable("mcps.screen.main_menu.tps").getString() + ": " + String.format("%.2f", performanceMonitor.getTps());
            context.drawTextWithShadow(this.textRenderer, tpsInfo, x, currentY, 0xAAAAAA);
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new SystemInfoScreen());
    }
}