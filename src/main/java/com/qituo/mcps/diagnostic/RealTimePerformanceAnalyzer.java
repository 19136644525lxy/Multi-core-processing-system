package com.qituo.mcps.diagnostic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import com.qituo.mcps.core.MCPSMod;

public class RealTimePerformanceAnalyzer {
    private static RealTimePerformanceAnalyzer instance;
    private Map<String, PerformanceMetric> metrics;
    private long lastAnalysisTime;
    private int analysisInterval = 1000; // 分析间隔，单位毫秒
    
    private RealTimePerformanceAnalyzer() {
        this.metrics = new HashMap<>();
        this.lastAnalysisTime = System.currentTimeMillis();
    }
    
    public static RealTimePerformanceAnalyzer getInstance() {
        if (instance == null) {
            instance = new RealTimePerformanceAnalyzer();
        }
        return instance;
    }
    
    public void recordMetric(String name, long value) {
        PerformanceMetric metric = metrics.computeIfAbsent(name, k -> new PerformanceMetric());
        metric.recordValue(value);
    }
    
    public void analyze() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnalysisTime < analysisInterval) {
            return;
        }
        
        MCPSMod.LOGGER.info("=== Real-time Performance Analysis ===");
        
        for (Map.Entry<String, PerformanceMetric> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            PerformanceMetric metric = entry.getValue();
            
            if (metric.hasData()) {
                MCPSMod.LOGGER.info(metricName + ":");
                MCPSMod.LOGGER.info("  Average: " + metric.getAverage() + "ms");
                MCPSMod.LOGGER.info("  Min: " + metric.getMin() + "ms");
                MCPSMod.LOGGER.info("  Max: " + metric.getMax() + "ms");
                MCPSMod.LOGGER.info("  Count: " + metric.getCount());
                
                // 检测性能瓶颈
                if (metric.getAverage() > 100) {
                    MCPSMod.LOGGER.warn("  ⚠️  Potential bottleneck detected!");
                }
            }
        }
        
        MCPSMod.LOGGER.info("====================================");
        lastAnalysisTime = currentTime;
    }
    
    public void resetMetrics() {
        metrics.clear();
        MCPSMod.LOGGER.info("Performance metrics reset");
    }
    
    public Map<String, PerformanceMetric> getMetrics() {
        return metrics;
    }
    
    public void setAnalysisInterval(int interval) {
        this.analysisInterval = interval;
    }
    
    public int getAnalysisInterval() {
        return analysisInterval;
    }
    
    public static class PerformanceMetric {
        private AtomicLong sum;
        private AtomicLong count;
        private AtomicLong min;
        private AtomicLong max;
        
        public PerformanceMetric() {
            this.sum = new AtomicLong(0);
            this.count = new AtomicLong(0);
            this.min = new AtomicLong(Long.MAX_VALUE);
            this.max = new AtomicLong(Long.MIN_VALUE);
        }
        
        public void recordValue(long value) {
            sum.addAndGet(value);
            count.incrementAndGet();
            min.updateAndGet(current -> Math.min(current, value));
            max.updateAndGet(current -> Math.max(current, value));
        }
        
        public double getAverage() {
            long cnt = count.get();
            return cnt > 0 ? (double) sum.get() / cnt : 0;
        }
        
        public long getMin() {
            return min.get() == Long.MAX_VALUE ? 0 : min.get();
        }
        
        public long getMax() {
            return max.get() == Long.MIN_VALUE ? 0 : max.get();
        }
        
        public long getCount() {
            return count.get();
        }
        
        public boolean hasData() {
            return count.get() > 0;
        }
        
        public void reset() {
            sum.set(0);
            count.set(0);
            min.set(Long.MAX_VALUE);
            max.set(Long.MIN_VALUE);
        }
    }
}