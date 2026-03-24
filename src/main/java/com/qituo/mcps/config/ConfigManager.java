package com.qituo.mcps.config;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import com.qituo.mcps.core.MCPSMod;

public class ConfigManager {
    private static ConfigManager instance;
    private static final String CONFIG_FILE = "mcps.properties";
    private Properties config;
    private File configFile;
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize() {
        config = new Properties();
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE);
        
        // 加载配置文件
        loadConfig();
        
        // 应用默认配置
        applyDefaults();
        
        // 保存配置
        saveConfig();
        
        MCPSMod.LOGGER.info("ConfigManager initialized");
        logConfig();
    }
    
    private void loadConfig() {
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                config.load(input);
                MCPSMod.LOGGER.info("Config loaded from file");
            } catch (IOException e) {
                MCPSMod.LOGGER.error("Error loading config file", e);
            }
        } else {
            MCPSMod.LOGGER.info("Config file not found, using defaults");
        }
    }
    
    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(configFile)) {
            config.store(output, "Multi-core processing system configuration");
            MCPSMod.LOGGER.info("Config saved to file");
        } catch (IOException e) {
            MCPSMod.LOGGER.error("Error saving config file", e);
        }
    }
    
    private void applyDefaults() {
        // 线程池配置
        setDefault("thread.corePoolSize", String.valueOf(calculateOptimalCorePoolSize()));
        setDefault("thread.maxPoolSize", String.valueOf(calculateOptimalMaxPoolSize()));
        setDefault("thread.keepAliveTime", "60"); // 60 seconds
        
        // 任务调度配置
        setDefault("scheduler.queueSize", "10000");
        setDefault("scheduler.priorityEnabled", "true");
        
        // 性能监控配置
        setDefault("monitor.enabled", "true");
        setDefault("monitor.updateInterval", "1000"); // 1 second
        
        // 错误处理配置
        setDefault("errorHandler.enabled", "true");
        setDefault("errorHandler.maxRetries", "3");
        
        // 兼容性配置
        setDefault("compatibility.checkEnabled", "true");
        setDefault("compatibility.hooksEnabled", "true");
    }
    
    private void setDefault(String key, String defaultValue) {
        if (!config.containsKey(key)) {
            config.setProperty(key, defaultValue);
        }
    }
    
    private int calculateOptimalCorePoolSize() {
        // 根据CPU核心数计算最优核心线程数
        int processors = Runtime.getRuntime().availableProcessors();
        // 对于游戏服务器，通常使用CPU核心数或核心数-1
        return Math.max(4, processors);
    }
    
    private int calculateOptimalMaxPoolSize() {
        // 计算最大线程数
        int processors = Runtime.getRuntime().availableProcessors();
        // 最大线程数通常是核心数的2倍
        return Math.max(8, processors * 2);
    }
    
    public int getCorePoolSize() {
        return Integer.parseInt(config.getProperty("thread.corePoolSize"));
    }
    
    public int getMaxPoolSize() {
        return Integer.parseInt(config.getProperty("thread.maxPoolSize"));
    }
    
    public long getKeepAliveTime() {
        return Long.parseLong(config.getProperty("thread.keepAliveTime"));
    }
    
    public int getQueueSize() {
        return Integer.parseInt(config.getProperty("scheduler.queueSize"));
    }
    
    public boolean isPriorityEnabled() {
        return Boolean.parseBoolean(config.getProperty("scheduler.priorityEnabled"));
    }
    
    public boolean isMonitorEnabled() {
        return Boolean.parseBoolean(config.getProperty("monitor.enabled"));
    }
    
    public int getMonitorUpdateInterval() {
        return Integer.parseInt(config.getProperty("monitor.updateInterval"));
    }
    
    public boolean isErrorHandlerEnabled() {
        return Boolean.parseBoolean(config.getProperty("errorHandler.enabled"));
    }
    
    public int getMaxRetries() {
        return Integer.parseInt(config.getProperty("errorHandler.maxRetries"));
    }
    
    public boolean isCompatibilityCheckEnabled() {
        return Boolean.parseBoolean(config.getProperty("compatibility.checkEnabled"));
    }
    
    public boolean isCompatibilityHooksEnabled() {
        return Boolean.parseBoolean(config.getProperty("compatibility.hooksEnabled"));
    }
    
    public void updateConfig(String key, String value) {
        config.setProperty(key, value);
        saveConfig();
    }
    
    public void logConfig() {
        MCPSMod.LOGGER.info("=== MCPS Configuration ===");
        MCPSMod.LOGGER.info("Thread pool: core=" + getCorePoolSize() + ", max=" + getMaxPoolSize() + ", keepAlive=" + getKeepAliveTime() + "s");
        MCPSMod.LOGGER.info("Scheduler: queueSize=" + getQueueSize() + ", priorityEnabled=" + isPriorityEnabled());
        MCPSMod.LOGGER.info("Monitor: enabled=" + isMonitorEnabled() + ", interval=" + getMonitorUpdateInterval() + "ms");
        MCPSMod.LOGGER.info("Error Handler: enabled=" + isErrorHandlerEnabled() + ", maxRetries=" + getMaxRetries());
        MCPSMod.LOGGER.info("Compatibility: checkEnabled=" + isCompatibilityCheckEnabled() + ", hooksEnabled=" + isCompatibilityHooksEnabled());
    }
    
    public void applyToThreadPool(ThreadPoolExecutor executor) {
        // 应用配置到线程池
        executor.setCorePoolSize(getCorePoolSize());
        executor.setMaximumPoolSize(getMaxPoolSize());
        executor.setKeepAliveTime(getKeepAliveTime(), java.util.concurrent.TimeUnit.SECONDS);
        
        MCPSMod.LOGGER.info("Applied configuration to thread pool");
    }
    
    public void detectAndOptimize() {
        // 检测硬件配置并优化
        int processors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory();
        
        MCPSMod.LOGGER.info("Detected hardware: " + processors + " cores, " + (maxMemory / (1024 * 1024)) + "MB max memory");
        
        // 根据硬件配置调整参数
        if (processors <= 4) {
            // 低配置系统
            updateConfig("thread.corePoolSize", String.valueOf(Math.max(2, processors)));
            updateConfig("thread.maxPoolSize", String.valueOf(Math.max(4, processors * 2)));
            updateConfig("scheduler.queueSize", "5000");
        } else if (processors <= 8) {
            // 中等配置系统
            updateConfig("thread.corePoolSize", String.valueOf(processors));
            updateConfig("thread.maxPoolSize", String.valueOf(processors * 2));
            updateConfig("scheduler.queueSize", "10000");
        } else {
            // 高配置系统
            updateConfig("thread.corePoolSize", String.valueOf(processors - 2));
            updateConfig("thread.maxPoolSize", String.valueOf(processors * 2));
            updateConfig("scheduler.queueSize", "20000");
        }
        
        // 根据内存大小调整
        if (maxMemory < 4 * 1024 * 1024 * 1024L) { // 小于4GB
            updateConfig("scheduler.queueSize", "5000");
        }
        
        MCPSMod.LOGGER.info("Hardware optimization applied");
        logConfig();
    }
}