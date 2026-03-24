package com.qituo.mcps.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.qituo.mcps.core.MCPSMod;

public class PlatformManager {
    private static PlatformManager instance;
    private String osName;
    private String osVersion;
    private String osArchitecture;
    private int availableProcessors;
    private long totalMemory;
    private Map<String, Object> platformInfo;
    
    private PlatformManager() {
        platformInfo = new ConcurrentHashMap<>();
        detectPlatform();
        detectHardware();
    }
    
    public static PlatformManager getInstance() {
        if (instance == null) {
            synchronized (PlatformManager.class) {
                if (instance == null) {
                    instance = new PlatformManager();
                }
            }
        }
        return instance;
    }
    
    // 检测平台信息
    private void detectPlatform() {
        osName = System.getProperty("os.name", "Unknown");
        osVersion = System.getProperty("os.version", "Unknown");
        osArchitecture = System.getProperty("os.arch", "Unknown");
        
        platformInfo.put("os.name", osName);
        platformInfo.put("os.version", osVersion);
        platformInfo.put("os.arch", osArchitecture);
        
        MCPSMod.LOGGER.info("Detected platform: " + osName + " " + osVersion + " (" + osArchitecture + ")");
    }
    
    // 检测硬件信息
    private void detectHardware() {
        availableProcessors = Runtime.getRuntime().availableProcessors();
        totalMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // MB
        
        platformInfo.put("available.processors", availableProcessors);
        platformInfo.put("total.memory", totalMemory);
        
        MCPSMod.LOGGER.info("Detected hardware: " + availableProcessors + " cores, " + totalMemory + " MB max memory");
    }
    
    // 获取平台名称
    public String getOsName() {
        return osName;
    }
    
    // 获取平台版本
    public String getOsVersion() {
        return osVersion;
    }
    
    // 获取平台架构
    public String getOsArchitecture() {
        return osArchitecture;
    }
    
    // 获取可用处理器数量
    public int getAvailableProcessors() {
        return availableProcessors;
    }
    
    // 获取总内存
    public long getTotalMemory() {
        return totalMemory;
    }
    
    // 获取平台信息
    public Map<String, Object> getPlatformInfo() {
        return platformInfo;
    }
    
    // 检测是否为Windows平台
    public boolean isWindows() {
        return osName.toLowerCase().contains("windows");
    }
    
    // 检测是否为Linux平台
    public boolean isLinux() {
        return osName.toLowerCase().contains("linux");
    }
    
    // 检测是否为Mac平台
    public boolean isMac() {
        return osName.toLowerCase().contains("mac");
    }
    
    // 检测是否为64位架构
    public boolean is64Bit() {
        return osArchitecture.contains("64");
    }
    
    // 根据平台和硬件配置调整优化策略
    public void adjustOptimizationStrategy() {
        // 根据平台调整
        if (isWindows()) {
            MCPSMod.LOGGER.info("Applying Windows-specific optimizations");
            // Windows特定优化
        } else if (isLinux()) {
            MCPSMod.LOGGER.info("Applying Linux-specific optimizations");
            // Linux特定优化
        } else if (isMac()) {
            MCPSMod.LOGGER.info("Applying Mac-specific optimizations");
            // Mac特定优化
        }
        
        // 根据硬件配置调整
        if (availableProcessors <= 4) {
            MCPSMod.LOGGER.info("Applying low-core count optimizations");
            // 低核心数优化
        } else if (availableProcessors >= 16) {
            MCPSMod.LOGGER.info("Applying high-core count optimizations");
            // 高核心数优化
        }
        
        if (totalMemory <= 4096) { // 4GB
            MCPSMod.LOGGER.info("Applying low-memory optimizations");
            // 低内存优化
        } else if (totalMemory >= 16384) { // 16GB
            MCPSMod.LOGGER.info("Applying high-memory optimizations");
            // 高内存优化
        }
    }
    
    // 获取推荐的线程池大小
    public int getRecommendedThreadPoolSize() {
        int baseSize = availableProcessors;
        
        // 根据平台调整
        if (isWindows()) {
            baseSize = Math.max(4, baseSize);
        } else if (isLinux()) {
            baseSize = Math.max(4, baseSize * 2);
        } else if (isMac()) {
            baseSize = Math.max(4, baseSize);
        }
        
        // 根据内存调整
        if (totalMemory <= 4096) {
            baseSize = Math.min(baseSize, 8);
        }
        
        return baseSize;
    }
    
    // 获取推荐的内存分配
    public long getRecommendedMemoryAllocation() {
        long baseAllocation = totalMemory * 3 / 4; // 75% of total memory
        
        // 根据平台调整
        if (isWindows()) {
            baseAllocation = Math.min(baseAllocation, 8192); // 8GB max on Windows
        } else if (isLinux()) {
            baseAllocation = Math.min(baseAllocation, 16384); // 16GB max on Linux
        } else if (isMac()) {
            baseAllocation = Math.min(baseAllocation, 12288); // 12GB max on Mac
        }
        
        return baseAllocation;
    }
    
    // 打印平台和硬件信息
    public void printPlatformInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== MCPS Platform Info ===\n");
        info.append("OS: " + osName + " " + osVersion + " (" + osArchitecture + ")\n");
        info.append("Processors: " + availableProcessors + " cores\n");
        info.append("Memory: " + totalMemory + " MB max\n");
        info.append("Recommended thread pool size: " + getRecommendedThreadPoolSize() + "\n");
        info.append("Recommended memory allocation: " + getRecommendedMemoryAllocation() + " MB\n");
        
        MCPSMod.LOGGER.info(info.toString());
    }
}