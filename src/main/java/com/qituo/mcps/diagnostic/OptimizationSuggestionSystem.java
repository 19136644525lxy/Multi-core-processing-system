package com.qituo.mcps.diagnostic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.platform.PlatformManager;

public class OptimizationSuggestionSystem {
    private static OptimizationSuggestionSystem instance;
    private List<OptimizationSuggestion> suggestions;
    private String configDirectory;
    
    private OptimizationSuggestionSystem() {
        this.suggestions = new ArrayList<>();
        this.configDirectory = System.getProperty("user.dir") + File.separator + "config" + File.separator + "mcps";
        initializeConfigDirectory();
    }
    
    public static OptimizationSuggestionSystem getInstance() {
        if (instance == null) {
            instance = new OptimizationSuggestionSystem();
        }
        return instance;
    }
    
    private void initializeConfigDirectory() {
        File dir = new File(configDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void generateSuggestions() {
        suggestions.clear();
        
        // 基于硬件的优化建议
        generateHardwareBasedSuggestions();
        
        // 基于游戏内容的优化建议
        generateGameContentBasedSuggestions();
        
        // 生成个性化优化配置文件
        generatePersonalizedConfig();
        
        // 输出优化建议
        outputSuggestions();
    }
    
    private void generateHardwareBasedSuggestions() {
        PlatformManager platformManager = PlatformManager.getInstance();
        
        // 基于CPU的建议
        int cpuCores = Runtime.getRuntime().availableProcessors();
        if (cpuCores < 4) {
            suggestions.add(new OptimizationSuggestion("CPU", "Consider upgrading to a CPU with at least 4 cores for better performance", 70));
        } else if (cpuCores < 8) {
            suggestions.add(new OptimizationSuggestion("CPU", "A CPU with 8+ cores would provide better performance for multi-threaded tasks", 50));
        }
        
        // 基于内存的建议
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // MB
        if (maxMemory < 4096) {
            suggestions.add(new OptimizationSuggestion("Memory", "Consider increasing your Java heap size to at least 4GB", 80));
        } else if (maxMemory < 8192) {
            suggestions.add(new OptimizationSuggestion("Memory", "Increasing Java heap size to 8GB would provide better performance", 40));
        }
        
        // 基于GPU的建议
        // 简化实现，实际应该检测GPU型号和性能
    }
    
    private void generateGameContentBasedSuggestions() {
        // 基于游戏内容的建议
        // 简化实现，实际应该分析游戏世界的复杂度
        
        // 基于实体数量的建议
        suggestions.add(new OptimizationSuggestion("Game Content", "Reduce the number of entities in loaded chunks", 60));
        
        // 基于红石复杂度的建议
        suggestions.add(new OptimizationSuggestion("Game Content", "Simplify complex redstone circuits to reduce tick time", 50));
        
        // 基于区块加载的建议
        suggestions.add(new OptimizationSuggestion("Game Content", "Use chunk pre-generation to reduce lag when exploring new areas", 40));
    }
    
    private void generatePersonalizedConfig() {
        try {
            StringBuilder config = new StringBuilder();
            config.append("# MCPS Personalized Optimization Config\n");
            config.append("# Generated based on hardware and game content analysis\n\n");
            
            // 基于硬件的配置
            int cpuCores = Runtime.getRuntime().availableProcessors();
            config.append("# Thread pool configuration\n");
            config.append("thread_pool_size=" + Math.max(4, cpuCores - 2) + "\n");
            config.append("thread_priority=5\n\n");
            
            // 基于内存的配置
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // MB
            config.append("# Memory configuration\n");
            config.append("memory_allocation=" + Math.min(maxMemory, 8192) + "\n\n");
            
            // 基于游戏内容的配置
            config.append("# Game content configuration\n");
            config.append("entity_processing_limit=100\n");
            config.append("redstone_update_limit=50\n");
            config.append("chunk_load_distance=10\n");
            
            Path path = Paths.get(configDirectory, "personalized_optimization.cfg");
            Files.write(path, config.toString().getBytes());
            
            MCPSMod.LOGGER.info("Personalized optimization config generated at: " + path);
        } catch (IOException e) {
            MCPSMod.LOGGER.error("Error generating personalized config: " + e.getMessage());
        }
    }
    
    private void outputSuggestions() {
        if (suggestions.isEmpty()) {
            MCPSMod.LOGGER.info("No optimization suggestions available.");
            return;
        }
        
        MCPSMod.LOGGER.info("=== Optimization Suggestions ===");
        
        for (OptimizationSuggestion suggestion : suggestions) {
            MCPSMod.LOGGER.info(suggestion.toString());
        }
        
        MCPSMod.LOGGER.info("================================");
    }
    
    public List<OptimizationSuggestion> getSuggestions() {
        return suggestions;
    }
    
    public String getConfigDirectory() {
        return configDirectory;
    }
    
    public static class OptimizationSuggestion {
        private String category;
        private String description;
        private int priority; // 0-100, higher is more important
        
        public OptimizationSuggestion(String category, String description, int priority) {
            this.category = category;
            this.description = description;
            this.priority = priority;
        }
        
        public String getCategory() {
            return category;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getPriorityLevel() {
            if (priority >= 80) {
                return "High";
            } else if (priority >= 50) {
                return "Medium";
            } else {
                return "Low";
            }
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s (Priority: %s)", 
                category, description, getPriorityLevel());
        }
    }
}