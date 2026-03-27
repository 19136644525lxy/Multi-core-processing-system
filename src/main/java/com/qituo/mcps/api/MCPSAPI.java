package com.qituo.mcps.api;

import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.task.SmartTaskScheduler;
import com.qituo.mcps.core.ResourceManager;
import com.qituo.mcps.network.NetworkManager;
import com.qituo.mcps.render.RenderManager;
import com.qituo.mcps.storage.StorageManager;
import com.qituo.mcps.ai.AIManager;
import com.qituo.mcps.gpu.GPUManager;
import com.qituo.mcps.cluster.ClusterManager;
import com.qituo.mcps.monitor.PerformanceMonitor;
import com.qituo.mcps.cloud.CloudManager;
import com.qituo.mcps.diagnostic.DiagnosticManager;
import com.qituo.mcps.diagnostic.RealTimePerformanceAnalyzer;
import com.qituo.mcps.diagnostic.BottleneckDetector;
import com.qituo.mcps.diagnostic.OptimizationSuggestionSystem;

public class MCPSAPI {
    private static MCPSAPI instance;
    
    private MCPSAPI() {
    }
    
    public static MCPSAPI getInstance() {
        if (instance == null) {
            synchronized (MCPSAPI.class) {
                if (instance == null) {
                    instance = new MCPSAPI();
                }
            }
        }
        return instance;
    }
    
    // 获取线程管理器
    public ThreadManager getThreadManager() {
        return MCPSMod.getInstance().getThreadManager();
    }
    
    // 获取任务调度器
    public SmartTaskScheduler getTaskScheduler() {
        return MCPSMod.getInstance().getTaskScheduler();
    }
    
    // 获取资源管理器
    public ResourceManager getResourceManager() {
        return MCPSMod.getInstance().getResourceManager();
    }
    
    // 获取网络管理器
    public NetworkManager getNetworkManager() {
        return MCPSMod.getInstance().getNetworkManager();
    }
    
    // 获取渲染管理器
    public RenderManager getRenderManager() {
        return MCPSMod.getInstance().getRenderManager();
    }
    
    // 获取存储管理器
    public StorageManager getStorageManager() {
        return MCPSMod.getInstance().getStorageManager();
    }
    
    // 获取AI管理器
    public AIManager getAiManager() {
        return MCPSMod.getInstance().getAiManager();
    }
    
    // 获取GPU管理器
    public GPUManager getGpuManager() {
        return MCPSMod.getInstance().getGpuManager();
    }
    
    // 获取集群管理器
    public ClusterManager getClusterManager() {
        return MCPSMod.getInstance().getClusterManager();
    }
    
    // 注册事件监听器
    public void registerEventListener(MCPSListener listener) {
        MCPSMod.getInstance().getEventManager().registerListener(listener);
    }
    
    // 注册插件
    public void registerPlugin(MCPSPlugin plugin) {
        MCPSMod.getInstance().getPluginManager().registerPlugin(plugin);
    }
    
    // 获取性能监控器
    public PerformanceMonitor getPerformanceMonitor() {
        return MCPSMod.getInstance().getPerformanceMonitor();
    }
    
    // 获取云管理器
    public CloudManager getCloudManager() {
        return MCPSMod.getInstance().getCloudManager();
    }
    
    // 获取诊断管理器
    public DiagnosticManager getDiagnosticManager() {
        return MCPSMod.getInstance().getDiagnosticManager();
    }
    
    // 获取实时性能分析器
    public RealTimePerformanceAnalyzer getPerformanceAnalyzer() {
        return RealTimePerformanceAnalyzer.getInstance();
    }
    
    // 获取瓶颈检测器
    public BottleneckDetector getBottleneckDetector() {
        return BottleneckDetector.getInstance();
    }
    
    // 获取优化建议系统
    public OptimizationSuggestionSystem getOptimizationSuggestionSystem() {
        return OptimizationSuggestionSystem.getInstance();
    }
    
    // 执行性能分析
    public void analyzePerformance() {
        RealTimePerformanceAnalyzer.getInstance().analyze();
    }
    
    // 检测瓶颈
    public void detectBottlenecks() {
        BottleneckDetector.getInstance().detectBottlenecks();
    }
    
    // 生成优化建议
    public void generateOptimizationSuggestions() {
        OptimizationSuggestionSystem.getInstance().generateSuggestions();
    }
    
    // 执行备份
    public void performBackup() {
        MCPSMod.getInstance().getCloudManager().getBackupManager().performBackup();
    }
    
    // 执行手动备份
    public void performManualBackup() {
        MCPSMod.getInstance().getCloudManager().getBackupManager().performManualBackup();
    }
    
    // 执行增量备份
    public void performIncrementalBackup(String baseBackupId) {
        MCPSMod.getInstance().getCloudManager().getBackupManager().performIncrementalBackup(baseBackupId);
    }
    
    // 从备份恢复
    public void restoreFromBackup(String backupId) {
        MCPSMod.getInstance().getCloudManager().getBackupManager().restoreFromBackup(backupId);
    }
    
    // 验证备份
    public boolean verifyBackup(String backupId) {
        return MCPSMod.getInstance().getCloudManager().getBackupManager().verifyBackup(backupId);
    }
    
    // 请求资源
    public void requestResources(String requestId, int cpuRequired, long memoryRequired, long diskRequired) {
        MCPSMod.getInstance().getCloudManager().getResourcePoolManager().requestResources(requestId, cpuRequired, memoryRequired, diskRequired);
    }
}