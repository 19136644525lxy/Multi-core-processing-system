package com.qituo.mcps.diagnostic;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.qituo.mcps.core.MCPSMod;

public class DiagnosticManager {
    private static DiagnosticManager instance;
    private ScheduledExecutorService monitorExecutor;
    private Map<String, PerformanceMetric> performanceMetrics;
    private List<DiagnosticListener> listeners;
    
    private DiagnosticManager() {
        performanceMetrics = new ConcurrentHashMap<>();
        listeners = new ArrayList<>();
        monitorExecutor = Executors.newScheduledThreadPool(2);
    }
    
    public static DiagnosticManager getInstance() {
        if (instance == null) {
            synchronized (DiagnosticManager.class) {
                if (instance == null) {
                    instance = new DiagnosticManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize() {
        // 启动实时监控
        startRealTimeMonitoring();
        
        // 启动性能分析
        startPerformanceAnalysis();
        
        MCPSMod.LOGGER.info("DiagnosticManager initialized");
    }
    
    // 启动实时监控
    private void startRealTimeMonitoring() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                collectSystemMetrics();
                collectThreadMetrics();
                collectMemoryMetrics();
                collectNetworkMetrics();
                
                // 通知监听器
                for (DiagnosticListener listener : listeners) {
                    try {
                        listener.onMetricsCollected(performanceMetrics);
                    } catch (Exception e) {
                        MCPSMod.LOGGER.error("Error in DiagnosticListener: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in real-time monitoring: " + e.getMessage());
            }
        }, 1, 5, TimeUnit.SECONDS);
    }
    
    // 启动性能分析
    private void startPerformanceAnalysis() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                analyzePerformance();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in performance analysis: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);
    }
    
    // 收集系统指标
    private void collectSystemMetrics() {
        // 系统负载
        double systemLoad = MCPSMod.getInstance().getTaskScheduler().predictSystemLoad();
        performanceMetrics.put("system.load", new PerformanceMetric("system.load", systemLoad, "System load"));
        
        // 任务队列大小
        int queueSize = MCPSMod.getInstance().getTaskScheduler().getQueueSize();
        performanceMetrics.put("task.queue.size", new PerformanceMetric("task.queue.size", queueSize, "Task queue size"));
    }
    
    // 收集线程指标
    private void collectThreadMetrics() {
        // 活跃线程数
        int activeThreads = MCPSMod.getInstance().getThreadManager().getCurrentPoolSize();
        performanceMetrics.put("thread.active", new PerformanceMetric("thread.active", activeThreads, "Active threads"));
        
        // 核心线程数
        int coreThreads = MCPSMod.getInstance().getThreadManager().getCorePoolSize();
        performanceMetrics.put("thread.core", new PerformanceMetric("thread.core", coreThreads, "Core threads"));
        
        // 最大线程数
        int maxThreads = MCPSMod.getInstance().getThreadManager().getMaxPoolSize();
        performanceMetrics.put("thread.max", new PerformanceMetric("thread.max", maxThreads, "Max threads"));
    }
    
    // 收集内存指标
    private void collectMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024); // MB
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // MB
        
        performanceMetrics.put("memory.total", new PerformanceMetric("memory.total", totalMemory, "Total memory (MB)"));
        performanceMetrics.put("memory.free", new PerformanceMetric("memory.free", freeMemory, "Free memory (MB)"));
        performanceMetrics.put("memory.used", new PerformanceMetric("memory.used", usedMemory, "Used memory (MB)"));
        performanceMetrics.put("memory.max", new PerformanceMetric("memory.max", maxMemory, "Max memory (MB)"));
    }
    
    // 收集网络指标
    private void collectNetworkMetrics() {
        // 这里可以添加网络指标收集逻辑
        // 例如：网络连接数、带宽使用等
    }
    
    // 分析性能
    private void analyzePerformance() {
        // 分析系统负载
        double systemLoad = performanceMetrics.get("system.load").getValue();
        if (systemLoad > 0.8) {
            MCPSMod.LOGGER.warn("High system load detected: " + systemLoad);
            // 生成性能分析报告
            generatePerformanceReport();
        }
        
        // 分析内存使用
        long usedMemory = (long) performanceMetrics.get("memory.used").getValue();
        long maxMemory = (long) performanceMetrics.get("memory.max").getValue();
        double memoryUsage = (double) usedMemory / maxMemory;
        if (memoryUsage > 0.8) {
            MCPSMod.LOGGER.warn("High memory usage detected: " + (memoryUsage * 100) + "%");
        }
    }
    
    // 生成性能分析报告
    public void generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== MCPS Performance Report ===\n");
        report.append("Timestamp: " + System.currentTimeMillis() + "\n");
        
        for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
            PerformanceMetric metric = entry.getValue();
            report.append(metric.getDescription() + ": " + metric.getValue() + "\n");
        }
        
        MCPSMod.LOGGER.info(report.toString());
    }
    
    // 诊断错误
    public void diagnoseError(String errorMessage, Exception e) {
        MCPSMod.LOGGER.error("Error: " + errorMessage, e);
        
        // 生成错误诊断报告
        StringBuilder report = new StringBuilder();
        report.append("=== MCPS Error Diagnostic Report ===\n");
        report.append("Error message: " + errorMessage + "\n");
        report.append("Exception: " + e.getMessage() + "\n");
        
        // 添加系统状态
        report.append("System state:\n");
        for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
            PerformanceMetric metric = entry.getValue();
            report.append("  " + metric.getDescription() + ": " + metric.getValue() + "\n");
        }
        
        MCPSMod.LOGGER.error(report.toString());
        
        // 通知监听器
        for (DiagnosticListener listener : listeners) {
            try {
                listener.onErrorDiagnosed(errorMessage, e, performanceMetrics);
            } catch (Exception ex) {
                MCPSMod.LOGGER.error("Error in DiagnosticListener: " + ex.getMessage());
            }
        }
    }
    
    // 注册诊断监听器
    public void registerListener(DiagnosticListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            MCPSMod.LOGGER.info("Registered diagnostic listener: " + listener.getClass().getName());
        }
    }
    
    // 注销诊断监听器
    public void unregisterListener(DiagnosticListener listener) {
        if (listeners.remove(listener)) {
            MCPSMod.LOGGER.info("Unregistered diagnostic listener: " + listener.getClass().getName());
        }
    }
    
    // 获取性能指标
    public Map<String, PerformanceMetric> getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    // 关闭诊断管理器
    public void shutdown() {
        monitorExecutor.shutdownNow();
        MCPSMod.LOGGER.info("DiagnosticManager shutdown");
    }
    
    // 性能指标类
    public static class PerformanceMetric {
        private String name;
        private double value;
        private String description;
        
        public PerformanceMetric(String name, double value, String description) {
            this.name = name;
            this.value = value;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public double getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 诊断监听器接口
    public interface DiagnosticListener {
        void onMetricsCollected(Map<String, PerformanceMetric> metrics);
        void onErrorDiagnosed(String errorMessage, Exception e, Map<String, PerformanceMetric> metrics);
    }
}