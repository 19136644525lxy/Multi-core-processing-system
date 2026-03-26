package com.qituo.mcps.cloud;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.qituo.mcps.config.MCPSConfig;
import com.qituo.mcps.core.MCPSMod;

public class CloudManager {
    private CloudPerformanceMonitor performanceMonitor;
    private ResourcePoolManager resourcePoolManager;
    private CloudBackupManager backupManager;
    private ScheduledExecutorService executorService;
    private boolean initialized;
    
    public void initialize() {
        this.executorService = Executors.newScheduledThreadPool(5);
        this.performanceMonitor = new CloudPerformanceMonitor();
        this.resourcePoolManager = new ResourcePoolManager();
        this.backupManager = new CloudBackupManager();
        
        performanceMonitor.initialize();
        resourcePoolManager.initialize();
        backupManager.initialize();
        
        startCloudServices();
        
        initialized = true;
        MCPSMod.LOGGER.info("CloudManager initialized");
    }
    
    private void startCloudServices() {
        // 启动性能监控服务
        executorService.scheduleAtFixedRate(() -> {
            performanceMonitor.collectPerformanceData();
        }, 0, 60, TimeUnit.SECONDS);
        
        // 启动资源池管理服务
        executorService.scheduleAtFixedRate(() -> {
            resourcePoolManager.updateResourceStatus();
        }, 0, 30, TimeUnit.SECONDS);
        
        // 启动备份服务
        if (MCPSConfig.cloudBackupEnabled) {
            executorService.scheduleAtFixedRate(() -> {
                backupManager.performBackup();
            }, 0, MCPSConfig.backupInterval, TimeUnit.MINUTES);
        }
    }
    
    public CloudPerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public ResourcePoolManager getResourcePoolManager() {
        return resourcePoolManager;
    }
    
    public CloudBackupManager getBackupManager() {
        return backupManager;
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
        }
        
        if (resourcePoolManager != null) {
            resourcePoolManager.shutdown();
        }
        
        if (backupManager != null) {
            backupManager.shutdown();
        }
        
        MCPSMod.LOGGER.info("CloudManager shutdown");
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}