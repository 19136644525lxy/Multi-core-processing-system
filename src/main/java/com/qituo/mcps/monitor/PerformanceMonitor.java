package com.qituo.mcps.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.gpu.GPUManager;

public class PerformanceMonitor {
    private ScheduledExecutorService scheduler;
    private ConcurrentHashMap<String, AtomicLong> metrics;
    private ConcurrentHashMap<String, Long> lastValues;
    private AtomicLong mspt;
    private AtomicLong tps;
    private AtomicLong tickCount;
    private AtomicLong lastTickTime;
    private ConcurrentHashMap<Integer, AtomicLong> gpuUtilization; // 使用AtomicLong存储double的原始表示
    private ConcurrentHashMap<Integer, AtomicLong> gpuJobs;
    private AtomicLong totalGpuExecutionTime;
    private AtomicLong totalGpuJobs;
    
    public void initialize() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MCPS-Performance-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        metrics = new ConcurrentHashMap<>();
        lastValues = new ConcurrentHashMap<>();
        mspt = new AtomicLong(0);
        tps = new AtomicLong(20);
        tickCount = new AtomicLong(0);
        lastTickTime = new AtomicLong(System.currentTimeMillis());
        gpuUtilization = new ConcurrentHashMap<>();
        gpuJobs = new ConcurrentHashMap<>();
        totalGpuExecutionTime = new AtomicLong(0);
        totalGpuJobs = new AtomicLong(0);
        
        // 每秒钟打印一次性能指标
        scheduler.scheduleAtFixedRate(this::printMetrics, 1, 1, TimeUnit.SECONDS);
        
        // 每50毫秒更新一次MSPT和TPS
        scheduler.scheduleAtFixedRate(this::updateMsptAndTps, 0, 50, TimeUnit.MILLISECONDS);
        
        // 每10秒打印一次GPU性能指标
        scheduler.scheduleAtFixedRate(this::printGpuMetrics, 10, 10, TimeUnit.SECONDS);
        
        MCPSMod.LOGGER.info("PerformanceMonitor initialized");
    }
    
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        MCPSMod.LOGGER.info("PerformanceMonitor stopped");
    }
    
    public void recordMetric(String name, long value) {
        metrics.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    public void recordTaskTime(String taskName, long durationMs) {
        recordMetric("task." + taskName + ".time", durationMs);
        recordMetric("task." + taskName + ".count", 1);
    }
    
    public void recordThreadActivity(String threadName, long operations) {
        recordMetric("thread." + threadName + ".operations", operations);
    }
    
    // 记录GPU利用率
    public void recordGpuUtilization(int deviceId, double utilization) {
        // 将double转换为long存储（乘以10000以保留4位小数）
        long value = (long)(utilization * 10000);
        gpuUtilization.computeIfAbsent(deviceId, k -> new AtomicLong(0)).set(value);
    }
    
    // 记录GPU任务数
    public void recordGpuJobs(int deviceId, int jobs) {
        gpuJobs.computeIfAbsent(deviceId, k -> new AtomicLong(0)).set(jobs);
    }
    
    // 记录GPU执行时间
    public void recordGpuExecutionTime(long time) {
        totalGpuExecutionTime.addAndGet(time);
    }
    
    // 记录GPU任务数
    public void recordGpuJobCount(int count) {
        totalGpuJobs.addAndGet(count);
    }
    
    private void printMetrics() {
        StringBuilder sb = new StringBuilder();
        sb.append("[MCPS Performance]");
        
        for (String key : metrics.keySet()) {
            long current = metrics.get(key).get();
            long last = lastValues.getOrDefault(key, 0L);
            long delta = current - last;
            
            sb.append(" ").append(key).append(": ").append(delta);
            lastValues.put(key, current);
        }
        
        if (metrics.size() > 0) {
            MCPSMod.LOGGER.info(sb.toString());
        }
        
        // 重置计数器
        metrics.clear();
    }
    
    // 打印GPU性能指标
    private void printGpuMetrics() {
        if (gpuUtilization.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("[MCPS GPU Performance]");
            
            for (Map.Entry<Integer, AtomicLong> entry : gpuUtilization.entrySet()) {
                int deviceId = entry.getKey();
                // 将long转换回double（除以10000以恢复原始值）
                double utilization = entry.getValue().get() / 10000.0;
                long jobs = gpuJobs.getOrDefault(deviceId, new AtomicLong(0)).get();
                
                sb.append(" GPU " + deviceId + ": " + String.format("%.2f%%", utilization) + " (" + jobs + " jobs)");
            }
            
            sb.append(" Total GPU jobs: " + totalGpuJobs.get() + ", Total execution time: " + totalGpuExecutionTime.get() + "ms");
            
            MCPSMod.LOGGER.info(sb.toString());
        }
    }
    
    public void logPerformanceSnapshot() {
        ThreadManager threadManager = MCPSMod.getInstance().getThreadManager();
        if (threadManager != null) {
            MCPSMod.LOGGER.info("[MCPS Snapshot] Threads: " + threadManager.getCorePoolSize() + ", Active tasks: " + threadManager.getActiveTaskCount() + ", Queue size: " + threadManager.getQueueSize());
        }
        
        // 打印GPU状态
        GPUManager gpuManager = MCPSMod.getInstance().getGpuManager();
        if (gpuManager != null) {
            Map<String, Object> gpuStats = gpuManager.getGpuPerformanceStats();
            MCPSMod.LOGGER.info("[MCPS GPU Snapshot] " + gpuStats);
        }
    }
    
    private void updateMsptAndTps() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastTickTime.get();
        
        // 计算MSPT
        mspt.set(elapsedTime);
        
        // 计算TPS
        tickCount.incrementAndGet();
        if (currentTime - lastTickTime.get() >= 1000) {
            long ticks = tickCount.getAndSet(0);
            tps.set(ticks);
            lastTickTime.set(currentTime);
        }
    }
    
    public double getMspt() {
        return mspt.get() / 1000.0; // 转换为秒
    }
    
    public double getTps() {
        return tps.get();
    }
    
    // 获取GPU利用率
    public double getGpuUtilization(int deviceId) {
        AtomicLong utilization = gpuUtilization.get(deviceId);
        return utilization != null ? utilization.get() / 10000.0 : 0.0;
    }
    
    // 获取GPU任务数
    public long getGpuJobs(int deviceId) {
        AtomicLong jobs = gpuJobs.get(deviceId);
        return jobs != null ? jobs.get() : 0;
    }
    
    // 获取所有GPU设备的利用率
    public Map<Integer, Double> getAllGpuUtilization() {
        Map<Integer, Double> utilizationMap = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, AtomicLong> entry : gpuUtilization.entrySet()) {
            utilizationMap.put(entry.getKey(), entry.getValue().get() / 10000.0);
        }
        return utilizationMap;
    }
    
    // 获取总GPU执行时间
    public long getTotalGpuExecutionTime() {
        return totalGpuExecutionTime.get();
    }
    
    // 获取总GPU任务数
    public long getTotalGpuJobs() {
        return totalGpuJobs.get();
    }
}