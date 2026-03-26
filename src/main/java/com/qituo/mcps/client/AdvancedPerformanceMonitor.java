package com.qituo.mcps.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import com.qituo.mcps.diagnostic.RealTimePerformanceAnalyzer;
import com.qituo.mcps.diagnostic.BottleneckDetector;
import com.qituo.mcps.diagnostic.OptimizationSuggestionSystem;

public class AdvancedPerformanceMonitor extends Screen {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    
    private RealTimePerformanceAnalyzer analyzer;
    private BottleneckDetector bottleneckDetector;
    private OptimizationSuggestionSystem suggestionSystem;
    
    private ButtonWidget analyzeButton;
    private ButtonWidget detectBottlenecksButton;
    private ButtonWidget generateSuggestionsButton;
    private ButtonWidget resetMetricsButton;
    private ButtonWidget closeButton;
    
    private TextFieldWidget intervalField;
    
    public AdvancedPerformanceMonitor() {
        super(Text.translatable("mcps.screen.performance_monitor.title"));
        this.analyzer = RealTimePerformanceAnalyzer.getInstance();
        this.bottleneckDetector = BottleneckDetector.getInstance();
        this.suggestionSystem = OptimizationSuggestionSystem.getInstance();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - WIDTH) / 2;
        int centerY = (this.height - HEIGHT) / 2;
        
        // 分析按钮
        analyzeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.performance_monitor.analyze"),
            button -> {
                analyzer.analyze();
                this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.performance_monitor.analyze.complete"));
            }
        ).position(centerX + 20, centerY + 40).size(150, 20).build();
        this.addDrawableChild(analyzeButton);
        
        // 检测瓶颈按钮
        detectBottlenecksButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.performance_monitor.detect_bottlenecks"),
            button -> {
                bottleneckDetector.detectBottlenecks();
                this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.performance_monitor.detect_bottlenecks.complete"));
            }
        ).position(centerX + 20, centerY + 70).size(150, 20).build();
        this.addDrawableChild(detectBottlenecksButton);
        
        // 生成建议按钮
        generateSuggestionsButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.performance_monitor.generate_suggestions"),
            button -> {
                suggestionSystem.generateSuggestions();
                this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.performance_monitor.generate_suggestions.complete"));
            }
        ).position(centerX + 20, centerY + 100).size(150, 20).build();
        this.addDrawableChild(generateSuggestionsButton);
        
        // 重置指标按钮
        resetMetricsButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.performance_monitor.reset_metrics"),
            button -> {
                analyzer.resetMetrics();
                this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.performance_monitor.reset_metrics.complete"));
            }
        ).position(centerX + 20, centerY + 130).size(150, 20).build();
        this.addDrawableChild(resetMetricsButton);
        
        // 分析间隔输入框
        intervalField = new TextFieldWidget(this.textRenderer, centerX + 20, centerY + 185, 150, 20, Text.translatable("mcps.screen.performance_monitor.analysis_interval"));
        intervalField.setText(String.valueOf(analyzer.getAnalysisInterval()));
        this.addDrawableChild(intervalField);
        
        // 应用间隔按钮
        ButtonWidget applyIntervalButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.performance_monitor.apply"),
            button -> {
                try {
                    int interval = Integer.parseInt(intervalField.getText());
                    analyzer.setAnalysisInterval(interval);
                    this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.performance_monitor.interval.updated"));
                } catch (NumberFormatException e) {
                    this.client.inGameHud.getChatHud().addMessage(Text.translatable("mcps.screen.performance_monitor.interval.invalid"));
                }
            }
        ).position(centerX + 20, centerY + 210).size(150, 20).build();
        this.addDrawableChild(applyIntervalButton);
        
        // 关闭按钮
        closeButton = ButtonWidget.builder(
            Text.translatable("mcps.screen.performance_monitor.close"),
            button -> this.close()
        ).position(centerX + 20, centerY + 240).size(150, 20).build();
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
        
        // 性能指标显示
        int[] y = {centerY + 40};
        analyzer.getMetrics().forEach((name, metric) -> {
            if (metric.hasData()) {
                String text = name + ": " + String.format("%.2f", metric.getAverage()) + "ms";
                context.drawTextWithShadow(this.textRenderer, text, centerX + 200, y[0], 0xFFFFFF);
                y[0] += 20;
            }
        });
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public static void open() {
        MinecraftClient.getInstance().setScreen(new AdvancedPerformanceMonitor());
    }
}