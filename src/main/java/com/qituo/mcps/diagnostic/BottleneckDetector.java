package com.qituo.mcps.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.qituo.mcps.core.MCPSMod;

public class BottleneckDetector {
    private static BottleneckDetector instance;
    private List<Bottleneck> detectedBottlenecks;
    private int severityThreshold = 70; // 严重程度阈值，超过此值视为严重瓶颈
    
    private BottleneckDetector() {
        this.detectedBottlenecks = new ArrayList<>();
    }
    
    public static BottleneckDetector getInstance() {
        if (instance == null) {
            instance = new BottleneckDetector();
        }
        return instance;
    }
    
    public void detectBottlenecks() {
        detectedBottlenecks.clear();
        
        // 分析实时性能数据
        RealTimePerformanceAnalyzer analyzer = RealTimePerformanceAnalyzer.getInstance();
        Map<String, RealTimePerformanceAnalyzer.PerformanceMetric> metrics = analyzer.getMetrics();
        
        for (Map.Entry<String, RealTimePerformanceAnalyzer.PerformanceMetric> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            RealTimePerformanceAnalyzer.PerformanceMetric metric = entry.getValue();
            
            if (metric.hasData()) {
                double average = metric.getAverage();
                int severity = calculateSeverity(average);
                
                if (severity > 0) {
                    Bottleneck bottleneck = new Bottleneck(metricName, average, severity);
                    detectedBottlenecks.add(bottleneck);
                }
            }
        }
        
        // 分析线程使用情况
        analyzeThreadUsage();
        
        // 分析内存使用情况
        analyzeMemoryUsage();
        
        // 生成瓶颈报告
        generateBottleneckReport();
    }
    
    private int calculateSeverity(double averageTime) {
        if (averageTime < 50) {
            return 0; // 无瓶颈
        } else if (averageTime < 100) {
            return 30; // 轻微瓶颈
        } else if (averageTime < 200) {
            return 50; // 中等瓶颈
        } else if (averageTime < 500) {
            return 70; // 严重瓶颈
        } else {
            return 90; // 极端瓶颈
        }
    }
    
    private void analyzeThreadUsage() {
        // 分析线程使用情况，检测线程竞争和死锁
        // 简化实现
    }
    
    private void analyzeMemoryUsage() {
        // 分析内存使用情况，检测内存泄漏
        // 简化实现
    }
    
    private void generateBottleneckReport() {
        if (detectedBottlenecks.isEmpty()) {
            MCPSMod.LOGGER.info("No bottlenecks detected.");
            return;
        }
        
        MCPSMod.LOGGER.info("=== Bottleneck Detection Report ===");
        
        for (Bottleneck bottleneck : detectedBottlenecks) {
            MCPSMod.LOGGER.info(bottleneck.toString());
            
            // 生成优化建议
            List<String> suggestions = generateOptimizationSuggestions(bottleneck);
            if (!suggestions.isEmpty()) {
                MCPSMod.LOGGER.info("  Optimization suggestions:");
                for (String suggestion : suggestions) {
                    MCPSMod.LOGGER.info("    - " + suggestion);
                }
            }
        }
        
        MCPSMod.LOGGER.info("==================================");
    }
    
    private List<String> generateOptimizationSuggestions(Bottleneck bottleneck) {
        List<String> suggestions = new ArrayList<>();
        
        String bottleneckName = bottleneck.getName();
        
        if (bottleneckName.contains("redstone")) {
            suggestions.add("Consider reducing the complexity of redstone circuits");
            suggestions.add("Use redstone alternatives like observer-based circuits");
        } else if (bottleneckName.contains("entity")) {
            suggestions.add("Reduce the number of entities in the area");
            suggestions.add("Optimize entity AI behavior");
        } else if (bottleneckName.contains("chunk")) {
            suggestions.add("Increase render distance gradually");
            suggestions.add("Use chunk pre-generation");
        } else if (bottleneckName.contains("network")) {
            suggestions.add("Check your network connection");
            suggestions.add("Reduce server view distance");
        } else {
            suggestions.add("Consider upgrading your hardware");
            suggestions.add("Optimize your Minecraft settings");
        }
        
        return suggestions;
    }
    
    public List<Bottleneck> getDetectedBottlenecks() {
        return detectedBottlenecks;
    }
    
    public void setSeverityThreshold(int threshold) {
        this.severityThreshold = threshold;
    }
    
    public int getSeverityThreshold() {
        return severityThreshold;
    }
    
    public static class Bottleneck {
        private String name;
        private double averageTime;
        private int severity;
        
        public Bottleneck(String name, double averageTime, int severity) {
            this.name = name;
            this.averageTime = averageTime;
            this.severity = severity;
        }
        
        public String getName() {
            return name;
        }
        
        public double getAverageTime() {
            return averageTime;
        }
        
        public int getSeverity() {
            return severity;
        }
        
        public String getSeverityLevel() {
            if (severity < 30) {
                return "Low";
            } else if (severity < 50) {
                return "Medium";
            } else if (severity < 70) {
                return "High";
            } else {
                return "Critical";
            }
        }
        
        @Override
        public String toString() {
            return String.format("Bottleneck: %s (Average: %.2fms, Severity: %d%% - %s)", 
                name, averageTime, severity, getSeverityLevel());
        }
    }
}