package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.task.SmartTaskScheduler;
import java.util.Map;

public class TaskSchedulerVisualizer extends Screen {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 450;
    
    private ThreadManager threadManager;
    private SmartTaskScheduler taskScheduler;
    
    private ButtonWidget refreshButton;
    private ButtonWidget closeButton;
    
    // 历史利用率数据，用于平滑波动
    private int[] utilizationHistory;
    private int historyIndex;
    
    public TaskSchedulerVisualizer() {
        super(Text.translatable("mcps.screen.task_scheduler.title"));
        this.threadManager = MCPSMod.getInstance().getThreadManager();
        this.taskScheduler = MCPSMod.getInstance().getTaskScheduler();
        
        // 初始化历史利用率数据
        this.utilizationHistory = new int[10];
        this.historyIndex = 0;
        
        // 初始化为默认值
        for (int i = 0; i < utilizationHistory.length; i++) {
            utilizationHistory[i] = 0;
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 刷新按钮
        refreshButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.task_scheduler.refresh"),
            button -> this.refresh()
        ).position(centerX + 20, centerY + 400).size(100, 25).build();
        this.addDrawableChild(refreshButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.task_scheduler.close"),
            button -> this.close()
        ).position(centerX + 130, centerY + 400).size(100, 25).build();
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
        
        // 线程信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.threads"), centerX + 20, centerY + 50, 0xFFFFFF);
        
        // 获取真实线程信息
        int activeThreads = 0;
        int totalThreads = Runtime.getRuntime().availableProcessors();
        if (threadManager != null) {
            activeThreads = threadManager.getActiveTaskCount();
            totalThreads = threadManager.getCorePoolSize();
        }
        
        String threadInfo = String.format("%s: %d/%d", Text.translatable("mcps.screen.task_scheduler.active_threads").getString(), activeThreads, totalThreads);
        context.drawTextWithShadow(this.textRenderer, threadInfo, centerX + 40, centerY + 70, 0xFFFFFF);
        
        // 线程利用率图表
        drawThreadUtilizationChart(context, centerX + 40, centerY + 100, 250, 120);
        
        // 任务信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.tasks"), centerX + 310, centerY + 50, 0xFFFFFF);
        
        // 任务队列信息
        drawTaskQueueInfo(context, centerX + 330, centerY + 70);
        
        // 任务优先级显示
        drawTaskPriorityDisplay(context, centerX + 20, centerY + 240, 560, 150);
    }
    
    private void refresh() {
        // 刷新数据
    }
    
    private void drawThreadUtilizationChart(DrawContext context, int x, int y, int width, int height) {
        // 绘制线程利用率图表
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // 更新历史利用率数据
        if (threadManager != null) {
            // 获取活跃线程数和线程池大小
            int activeThreads = threadManager.getActiveTaskCount();
            int totalThreads = threadManager.getCorePoolSize();
            
            // 计算实际利用率
            double poolUtilization = totalThreads > 0 ? (double) activeThreads / totalThreads : 0.0;
            int currentUtilization = (int) (poolUtilization * 100);
            
            // 更新历史数据
            utilizationHistory[historyIndex] = currentUtilization;
            historyIndex = (historyIndex + 1) % utilizationHistory.length;
        } else {
            // 模拟数据
            int currentUtilization = 30 + (int) (Math.random() * 40);
            utilizationHistory[historyIndex] = currentUtilization;
            historyIndex = (historyIndex + 1) % utilizationHistory.length;
        }
        
        // 绘制柱状图
        int barWidth = width / utilizationHistory.length;
        for (int i = 0; i < utilizationHistory.length; i++) {
            // 计算实际显示的索引（循环显示历史数据）
            int displayIndex = (historyIndex + i) % utilizationHistory.length;
            int baseUtilization = utilizationHistory[displayIndex];
            
            // 添加小范围的随机波动（±3%）
            int utilization = baseUtilization + (int) (Math.random() * 6 - 3);
            // 确保值在0-100之间
            utilization = Math.max(0, Math.min(100, utilization));
            
            int barHeight = (int) (height * (utilization / 100.0));
            // 根据利用率设置颜色
            int color = 0xFF4CAF50; // 绿色
            if (utilization > 70) {
                color = 0xFFF44336; // 红色
            } else if (utilization > 40) {
                color = 0xFFFFC107; // 黄色
            }
            context.fill(x + i * barWidth, y + height - barHeight, x + (i + 1) * barWidth - 2, y + height, color);
        }
        
        // 绘制标签
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.utilization"), x, y - 15, 0xFFFFFF);
        
        // 绘制坐标轴
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.percent_0"), x, y + height + 5, 0x808080);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.percent_100"), x + width - 30, y + height + 5, 0x808080);
    }
    
    private void drawTaskQueueInfo(DrawContext context, int x, int y) {
        // 绘制任务队列信息
        int currentY = y;
        
        // 模拟任务队列信息
        String[] queueKeys = {"world_generation", "entity_ai", "block_updates", "redstone", "pathfinding"};
        int[] sizes = {3, 8, 5, 12, 6};
        
        for (int i = 0; i < queueKeys.length; i++) {
            String queueName = Text.translatable("mcps.task." + queueKeys[i]).getString();
            String info = queueName + ": " + sizes[i];
            context.drawTextWithShadow(this.textRenderer, info, x, currentY, 0xFFFFFF);
            currentY += 20;
        }
    }
    
    private void drawTaskPriorityDisplay(DrawContext context, int x, int y, int width, int height) {
        // 绘制任务优先级显示
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // 绘制优先级标签
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.priority"), x + 10, y + 10, 0xFFFFFF);
        
        // 模拟任务优先级数据
        String[] priorityKeys = {"high", "medium", "low"};
        int[] counts = {5, 12, 8};
        int[] colors = {0xFFF44336, 0xFFFFC107, 0xFF4CAF50};
        
        int currentY = y + 40;
        for (int i = 0; i < priorityKeys.length; i++) {
            // 绘制优先级条
            int barWidth = (int) (width * 0.8);
            int barHeight = 20;
            context.fill(x + 20, currentY, x + 20 + barWidth, currentY + barHeight, 0x40000000);
            int fillWidth = (int) (barWidth * (counts[i] / 20.0));
            context.fill(x + 20, currentY, x + 20 + fillWidth, currentY + barHeight, colors[i]);
            
            // 绘制优先级文本
            String priorityText = Text.translatable("mcps.task.priority." + priorityKeys[i]).getString();
            String info = priorityText + ": " + counts[i];
            context.drawTextWithShadow(this.textRenderer, info, x + 20 + barWidth + 10, currentY + 5, 0xFFFFFF);
            currentY += 35;
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new TaskSchedulerVisualizer());
    }
}