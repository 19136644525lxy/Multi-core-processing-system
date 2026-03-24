package com.qituo.mcps.monitor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.qituo.mcps.core.MCPSMod;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Environment(EnvType.CLIENT)
public class MonitoringScreen extends Screen {
    private final Screen parent;
    private AtomicLong lastUpdateTime;
    private AtomicLong mspt;
    private AtomicLong tps;
    private AtomicInteger activeThreads;
    private AtomicInteger queueSize;
    private AtomicInteger gpuDevices;
    private AtomicInteger serverNodes;
    
    public MonitoringScreen(Screen parent) {
        super(Text.translatable("mcps.monitoring.title"));
        this.parent = parent;
        this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        this.mspt = new AtomicLong(0);
        this.tps = new AtomicLong(20);
        this.activeThreads = new AtomicInteger(0);
        this.queueSize = new AtomicInteger(0);
        this.gpuDevices = new AtomicInteger(0);
        this.serverNodes = new AtomicInteger(0);
    }
    
    @Override
    protected void init() {
        super.init();
        // 启动更新线程
        startUpdateThread();
    }
    
    private void startUpdateThread() {
        Thread updateThread = new Thread(() -> {
            while (this.client != null && this.client.currentScreen == this) {
                try {
                    updateMetrics();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "MCPS-Monitoring-Update");
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    private void updateMetrics() {
        // 从MCPSMod的性能监控器获取MSPT和TPS数据
        if (MCPSMod.getInstance() != null) {
            // 从性能监控器获取MSPT
            double serverMspt = MCPSMod.getInstance().getPerformanceMonitor().getMspt();
            mspt.set((long) (serverMspt * 1000)); // 转换为毫秒
            
            // 从性能监控器获取TPS
            double serverTps = MCPSMod.getInstance().getPerformanceMonitor().getTps();
            tps.set((long) serverTps);
            
            // 获取其他数据
            activeThreads.set(MCPSMod.getInstance().getThreadManager().getActiveTaskCount());
            queueSize.set(MCPSMod.getInstance().getThreadManager().getQueueSize());
            gpuDevices.set(MCPSMod.getInstance().getGpuManager().getGPUDevices().size());
            serverNodes.set(MCPSMod.getInstance().getClusterManager().getServerNodes().size());
        } else {
            // 如果无法从MCPSMod获取数据，使用本地计算
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUpdateTime.get();
            mspt.set(elapsedTime);
            lastUpdateTime.set(currentTime);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        
        MatrixStack matrices = context.getMatrices();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        // 标题
        Text title = Text.translatable("mcps.monitoring.title").formatted(Formatting.GOLD, Formatting.BOLD);
        context.drawText(client.textRenderer, title, width / 2 - client.textRenderer.getWidth(title) / 2, 20, 0xFFFFFF, true);
        
        // 主面板
        int panelX = width / 2 - 180;
        int panelY = 50;
        int panelWidth = 360;
        int panelHeight = 220;
        
        // 绘制面板背景
        context.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0x80000000); // 边框
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80222222); // 背景
        
        // 监控表
        int x = panelX + 20;
        int y = panelY + 20;
        int lineHeight = 24;
        int columnWidth = 140;
        
        // 表头
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.header.metric").formatted(Formatting.WHITE, Formatting.BOLD), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.header.value").formatted(Formatting.WHITE, Formatting.BOLD), x + columnWidth + 20, y, 0xFFFFFF, true);
        y += lineHeight;
        
        // 分隔线
        context.fill(x, y, x + columnWidth + 20 + 80, y + 1, 0x80FFFFFF);
        y += 10;
        
        // MSPT
        double msptValue = mspt.get() / 50.0;
        Formatting msptFormatting = msptValue < 50 ? Formatting.GREEN : (msptValue < 100 ? Formatting.YELLOW : Formatting.RED);
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.metric.mspt").formatted(Formatting.WHITE), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.literal(String.format("%.2f", msptValue)).formatted(msptFormatting), x + columnWidth + 20, y, 0xFFFFFF, true);
        y += lineHeight;
        
        // TPS
        long tpsValue = tps.get();
        Formatting tpsFormatting = tpsValue >= 18 ? Formatting.GREEN : (tpsValue >= 15 ? Formatting.YELLOW : Formatting.RED);
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.metric.tps").formatted(Formatting.WHITE), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.literal(String.valueOf(tpsValue)).formatted(tpsFormatting), x + columnWidth + 20, y, 0xFFFFFF, true);
        y += lineHeight;
        
        // 活跃线程数
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.metric.active_threads").formatted(Formatting.WHITE), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.literal(String.valueOf(activeThreads.get())).formatted(Formatting.AQUA), x + columnWidth + 20, y, 0xFFFFFF, true);
        y += lineHeight;
        
        // 任务队列大小
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.metric.queue_size").formatted(Formatting.WHITE), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.literal(String.valueOf(queueSize.get())).formatted(Formatting.LIGHT_PURPLE), x + columnWidth + 20, y, 0xFFFFFF, true);
        y += lineHeight;
        
        // GPU设备数
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.metric.gpu_devices").formatted(Formatting.WHITE), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.literal(String.valueOf(gpuDevices.get())).formatted(Formatting.RED), x + columnWidth + 20, y, 0xFFFFFF, true);
        y += lineHeight;
        
        // 服务器节点数
        context.drawText(client.textRenderer, Text.translatable("mcps.monitoring.metric.server_nodes").formatted(Formatting.WHITE), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, Text.literal(String.valueOf(serverNodes.get())).formatted(Formatting.GREEN), x + columnWidth + 20, y, 0xFFFFFF, true);
        
        // 底部信息
        Text bottomText = Text.translatable("mcps.monitoring.footer").formatted(Formatting.GRAY);
        context.drawText(client.textRenderer, bottomText, width / 2 - client.textRenderer.getWidth(bottomText) / 2, height - 30, 0xFFFFFF, true);
        
        // 实时更新指示器
        int indicatorX = panelX + panelWidth - 20;
        int indicatorY = panelY + 5;
        context.fill(indicatorX, indicatorY, indicatorX + 10, indicatorY + 10, 0xFF00FF00); // 绿色指示灯
    }
    
    @Override
    public void close() {
        this.client.setScreen(parent);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}