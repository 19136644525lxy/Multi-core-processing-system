package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.task.SmartTaskScheduler;
import com.qituo.mcps.thread.ThreadManager;
import java.util.Map;

public class TaskSchedulerVisualizer extends Screen {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 400;
    
    private SmartTaskScheduler taskScheduler;
    private ThreadManager threadManager;
    
    private ButtonWidget refreshButton;
    private ButtonWidget closeButton;
    
    public TaskSchedulerVisualizer() {
        super(Text.translatable("mcps.screen.task_scheduler.title"));
        this.taskScheduler = new SmartTaskScheduler(null, null);
        this.threadManager = new ThreadManager();
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
        ).position(centerX + 20, centerY + 350).size(100, 20).build();
        this.addDrawableChild(refreshButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.task_scheduler.close"),
            button -> this.close()
        ).position(centerX + 130, centerY + 350).size(100, 20).build();
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
        
        int activeThreads = 0;
        int totalThreads = Runtime.getRuntime().availableProcessors();
        String threadInfo = String.format("%s: %d/%d", Text.translatable("mcps.screen.task_scheduler.active_threads").getString(), activeThreads, totalThreads);
        context.drawTextWithShadow(this.textRenderer, threadInfo, centerX + 40, centerY + 70, 0xFFFFFF);
        
        // 线程利用率图表
        drawThreadUtilizationChart(context, centerX + 40, centerY + 90, 200, 100);
        
        // 任务信息
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.tasks"), centerX + 260, centerY + 50, 0xFFFFFF);
        
        // 任务队列信息
        drawTaskQueueInfo(context, centerX + 280, centerY + 70);
        
        // 任务优先级显示
        drawTaskPriorityDisplay(context, centerX + 20, centerY + 210, 460, 120);
    }
    
    private void refresh() {
        // 刷新数据
    }
    
    private void drawThreadUtilizationChart(DrawContext context, int x, int y, int width, int height) {
        // 绘制线程利用率图表
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // 模拟线程利用率数据
        int[] utilization = new int[10];
        for (int i = 0; i < utilization.length; i++) {
            utilization[i] = (int) (Math.random() * 100);
        }
        
        // 绘制柱状图
        int barWidth = width / utilization.length;
        for (int i = 0; i < utilization.length; i++) {
            int barHeight = (int) (height * (utilization[i] / 100.0));
            context.fill(x + i * barWidth, y + height - barHeight, x + (i + 1) * barWidth - 2, y + height, 0xFF4CAF50);
        }
        
        // 绘制标签
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mcps.screen.task_scheduler.utilization"), x, y - 15, 0xFFFFFF);
    }
    
    private void drawTaskQueueInfo(DrawContext context, int x, int y) {
        // 绘制任务队列信息
        int currentY = y;
        
        // 模拟任务队列信息
        String[] queueNames = {"High Priority", "Medium Priority", "Low Priority"};
        int[] sizes = {5, 12, 8};
        
        for (int i = 0; i < queueNames.length; i++) {
            String info = queueNames[i] + ": " + sizes[i];
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
        String[] priorities = {"High", "Medium", "Low"};
        int[] counts = {5, 12, 8};
        
        int currentY = y + 40;
        for (int i = 0; i < priorities.length; i++) {
            String info = priorities[i] + ": " + counts[i];
            context.drawTextWithShadow(this.textRenderer, info, x + 20, currentY, 0xFFFFFF);
            currentY += 30;
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