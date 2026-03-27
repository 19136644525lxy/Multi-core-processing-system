package com.qituo.mcps.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.gpu.GPUManager;
import com.qituo.mcps.task.TaskScheduler;

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
    
    // 新增：任务队列监控
    private ConcurrentHashMap<String, AtomicLong> queueSizes;
    private ConcurrentHashMap<String, AtomicLong> queueProcessingTimes;
    
    // 新增：系统负载监控
    private AtomicLong systemLoad;
    private AtomicLong cpuUsage;
    
    // 新增：网络性能监控
    private AtomicLong networkBytesSent;
    private AtomicLong networkBytesReceived;
    private AtomicLong networkPacketsSent;
    private AtomicLong networkPacketsReceived;
    
    // 新增：存储性能监控
    private AtomicLong storageReadBytes;
    private AtomicLong storageWriteBytes;
    private AtomicLong storageReadOperations;
    private AtomicLong storageWriteOperations;
    
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
        
        // 初始化新增的性能指标
        queueSizes = new ConcurrentHashMap<>();
        queueProcessingTimes = new ConcurrentHashMap<>();
        systemLoad = new AtomicLong(0);
        cpuUsage = new AtomicLong(0);
        networkBytesSent = new AtomicLong(0);
        networkBytesReceived = new AtomicLong(0);
        networkPacketsSent = new AtomicLong(0);
        networkPacketsReceived = new AtomicLong(0);
        storageReadBytes = new AtomicLong(0);
        storageWriteBytes = new AtomicLong(0);
        storageReadOperations = new AtomicLong(0);
        storageWriteOperations = new AtomicLong(0);
        
        // 每秒钟打印一次性能指标
        scheduler.scheduleAtFixedRate(this::printMetrics, 1, 1, TimeUnit.SECONDS);
        
        // 每50毫秒更新一次MSPT和TPS
        scheduler.scheduleAtFixedRate(this::updateMsptAndTps, 0, 50, TimeUnit.MILLISECONDS);
        
        // 每10秒打印一次GPU性能指标
        scheduler.scheduleAtFixedRate(this::printGpuMetrics, 10, 10, TimeUnit.SECONDS);
        
        // 每5秒打印一次任务队列性能指标
        scheduler.scheduleAtFixedRate(this::printQueueMetrics, 5, 5, TimeUnit.SECONDS);
        
        // 每10秒打印一次系统性能指标
        scheduler.scheduleAtFixedRate(this::printSystemMetrics, 10, 10, TimeUnit.SECONDS);
        
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
    
    // 记录任务队列性能指标
    public void recordQueueSize(String queueName, int size) {
        queueSizes.computeIfAbsent(queueName, k -> new AtomicLong(0)).set(size);
    }
    
    public void recordQueueProcessingTime(String queueName, long time) {
        queueProcessingTimes.computeIfAbsent(queueName, k -> new AtomicLong(0)).addAndGet(time);
    }
    
    // 记录系统负载和CPU使用率
    public void recordSystemLoad(double load) {
        // 将double转换为long存储（乘以10000以保留4位小数）
        long value = (long)(load * 10000);
        systemLoad.set(value);
    }
    
    public void recordCpuUsage(double usage) {
        // 将double转换为long存储（乘以10000以保留4位小数）
        long value = (long)(usage * 10000);
        cpuUsage.set(value);
    }
    
    // 记录网络性能指标
    public void recordNetworkBytesSent(long bytes) {
        networkBytesSent.addAndGet(bytes);
    }
    
    public void recordNetworkBytesReceived(long bytes) {
        networkBytesReceived.addAndGet(bytes);
    }
    
    public void recordNetworkPacketsSent(long packets) {
        networkPacketsSent.addAndGet(packets);
    }
    
    public void recordNetworkPacketsReceived(long packets) {
        networkPacketsReceived.addAndGet(packets);
    }
    
    // 记录存储性能指标
    public void recordStorageReadBytes(long bytes) {
        storageReadBytes.addAndGet(bytes);
    }
    
    public void recordStorageWriteBytes(long bytes) {
        storageWriteBytes.addAndGet(bytes);
    }
    
    public void recordStorageReadOperations(long operations) {
        storageReadOperations.addAndGet(operations);
    }
    
    public void recordStorageWriteOperations(long operations) {
        storageWriteOperations.addAndGet(operations);
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
    
    // 打印任务队列性能指标
    private void printQueueMetrics() {
        if (!queueSizes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[MCPS Queue Performance]");
            
            for (Map.Entry<String, AtomicLong> entry : queueSizes.entrySet()) {
                String queueName = entry.getKey();
                long size = entry.getValue().get();
                long processingTime = queueProcessingTimes.getOrDefault(queueName, new AtomicLong(0)).get();
                
                sb.append(" " + queueName + ": size=" + size + ", processingTime=" + processingTime + "ms");
            }
            
            MCPSMod.LOGGER.info(sb.toString());
        }
    }
    
    // 打印系统性能指标
    private void printSystemMetrics() {
        StringBuilder sb = new StringBuilder();
        sb.append("[MCPS System Performance]");
        
        // 系统负载和CPU使用率
        double load = systemLoad.get() / 10000.0;
        double cpu = cpuUsage.get() / 10000.0;
        sb.append(" Load: " + String.format("%.2f%%", load) + ", CPU: " + String.format("%.2f%%", cpu));
        
        // 网络性能
        sb.append(" Network: sent=" + (networkBytesSent.get() / 1024) + "KB, received=" + (networkBytesReceived.get() / 1024) + "KB");
        sb.append(" Packets: sent=" + networkPacketsSent.get() + ", received=" + networkPacketsReceived.get());
        
        // 存储性能
        sb.append(" Storage: read=" + (storageReadBytes.get() / 1024) + "KB, write=" + (storageWriteBytes.get() / 1024) + "KB");
        sb.append(" Operations: read=" + storageReadOperations.get() + ", write=" + storageWriteOperations.get());
        
        MCPSMod.LOGGER.info(sb.toString());
        
        // 重置网络和存储指标
        networkBytesSent.set(0);
        networkBytesReceived.set(0);
        networkPacketsSent.set(0);
        networkPacketsReceived.set(0);
        storageReadBytes.set(0);
        storageWriteBytes.set(0);
        storageReadOperations.set(0);
        storageWriteOperations.set(0);
    }
    
    public void logPerformanceSnapshot() {
        ThreadManager threadManager = MCPSMod.getInstance().getThreadManager();
        if (threadManager != null) {
            MCPSMod.LOGGER.info("[MCPS Snapshot] Threads: " + threadManager.getCorePoolSize() + ", Active tasks: " + threadManager.getActiveTaskCount() + ", Queue size: " + threadManager.getQueueSize() + ", Energy saving mode: " + threadManager.isEnergySavingMode());
        }
        
        // 打印GPU状态
        GPUManager gpuManager = MCPSMod.getInstance().getGpuManager();
        if (gpuManager != null) {
            Map<String, Object> gpuStats = gpuManager.getGpuPerformanceStats();
            MCPSMod.LOGGER.info("[MCPS GPU Snapshot] " + gpuStats);
        }
        
        // 打印系统负载
        MCPSMod.LOGGER.info("[MCPS Snapshot] MSPT: " + String.format("%.2f", getMspt()) + ", TPS: " + getTps());
        
        // 打印内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        MCPSMod.LOGGER.info("[MCPS Snapshot] Memory: " + usedMemory + "MB / " + totalMemory + "MB (max: " + maxMemory + "MB)");
        
        // 打印任务队列状态
        if (!queueSizes.isEmpty()) {
            StringBuilder sb = new StringBuilder("[MCPS Queue Snapshot]");
            for (Map.Entry<String, AtomicLong> entry : queueSizes.entrySet()) {
                String queueName = entry.getKey();
                long size = entry.getValue().get();
                sb.append(" " + queueName + ": " + size);
            }
            MCPSMod.LOGGER.info(sb.toString());
        }
        
        // 打印系统性能状态
        double load = systemLoad.get() / 10000.0;
        double cpu = cpuUsage.get() / 10000.0;
        MCPSMod.LOGGER.info("[MCPS System Snapshot] Load: " + String.format("%.2f%%", load) + ", CPU: " + String.format("%.2f%%", cpu));
        
        // 打印网络性能状态
        MCPSMod.LOGGER.info("[MCPS Network Snapshot] Sent: " + (networkBytesSent.get() / 1024) + "KB, Received: " + (networkBytesReceived.get() / 1024) + "KB");
        
        // 打印存储性能状态
        MCPSMod.LOGGER.info("[MCPS Storage Snapshot] Read: " + (storageReadBytes.get() / 1024) + "KB, Write: " + (storageWriteBytes.get() / 1024) + "KB");
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