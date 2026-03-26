package com.qituo.mcps.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.qituo.mcps.monitor.MonitoringScreen;

public class MCPSConfig {
    // 线程池设置
    public static int corePoolSize = 20;
    public static int maxPoolSize = 40;
    public static int keepAliveTime = 60;
    
    // 任务调度器设置
    public static int queueSize = 20000;
    public static boolean priorityEnabled = true;
    
    // 性能监控设置
    public static boolean monitorEnabled = true;
    public static int monitorInterval = 1000;
    
    // 错误处理设置
    public static boolean errorHandlerEnabled = true;
    public static int maxRetries = 3;
    
    // 兼容性设置
    public static boolean compatibilityCheckEnabled = true;
    public static boolean hooksEnabled = true;
    
    // GPU加速设置
    public static boolean gpuAccelerationEnabled = true;
    public static int gpuTaskBatchSize = 10;
    
    // 集群设置
    public static boolean clusterEnabled = true;
    public static int maxServerNodes = 10;
    public static int serverDiscoveryInterval = 30000;
    
    // AI优化设置
    public static boolean aiOptimizationEnabled = true;
    public static int aiModelUpdateInterval = 60000;
    
    // 内存管理设置
    public static boolean objectPoolingEnabled = true;
    public static int objectPoolSize = 1000;
    
    // 网络设置
    public static boolean networkOptimizationEnabled = true;
    public static boolean dataCompressionEnabled = true;
    
    // 渲染设置
    public static boolean renderOptimizationEnabled = true;
    public static int renderThreadPriority = 5;
    
    // 云备份设置
    public static boolean cloudBackupEnabled = true;
    public static int backupInterval = 60; // 分钟
    public static int maxBackups = 10;
    public static String cloudStorageEndpoint = "http://localhost:8080/api/backup";
    
    @Environment(EnvType.CLIENT)
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("mcps.config.title"))
            .setSavingRunnable(() -> {
                // 保存配置
                ConfigManager.getInstance().saveConfig();
            });
        
        // 线程池设置
        ConfigCategory threadPoolCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.thread_pool"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        threadPoolCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.thread_pool.core_size"), corePoolSize)
            .setDefaultValue(20)
            .setMin(1)
            .setMax(100)
            .setTooltip(Text.translatable("mcps.config.thread_pool.core_size.tooltip"))
            .build());
        
        threadPoolCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.thread_pool.max_size"), maxPoolSize)
            .setDefaultValue(40)
            .setMin(1)
            .setMax(200)
            .setTooltip(Text.translatable("mcps.config.thread_pool.max_size.tooltip"))
            .build());
        
        threadPoolCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.thread_pool.keep_alive"), keepAliveTime)
            .setDefaultValue(60)
            .setMin(1)
            .setMax(300)
            .setTooltip(Text.translatable("mcps.config.thread_pool.keep_alive.tooltip"))
            .build());
        
        // 性能监控设置
        ConfigCategory monitorCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.monitor"));
        
        monitorCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.monitor.enabled"), monitorEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.monitor.enabled.tooltip"))
            .build());
        
        monitorCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.monitor.interval"), monitorInterval)
            .setDefaultValue(1000)
            .setMin(100)
            .setMax(10000)
            .setTooltip(Text.translatable("mcps.config.monitor.interval.tooltip"))
            .build());
        
        // GPU加速设置
        ConfigCategory gpuCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.gpu"));
        
        gpuCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.gpu.enabled"), gpuAccelerationEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.gpu.enabled.tooltip"))
            .build());
        
        gpuCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.gpu.batch_size"), gpuTaskBatchSize)
            .setDefaultValue(10)
            .setMin(1)
            .setMax(100)
            .setTooltip(Text.translatable("mcps.config.gpu.batch_size.tooltip"))
            .build());
        
        // 集群设置
        ConfigCategory clusterCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.cluster"));
        
        clusterCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.cluster.enabled"), clusterEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.cluster.enabled.tooltip"))
            .build());
        
        clusterCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.cluster.max_nodes"), maxServerNodes)
            .setDefaultValue(10)
            .setMin(1)
            .setMax(100)
            .setTooltip(Text.translatable("mcps.config.cluster.max_nodes.tooltip"))
            .build());
        
        // AI优化设置
        ConfigCategory aiCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.ai"));
        
        aiCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.ai.enabled"), aiOptimizationEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.ai.enabled.tooltip"))
            .build());
        
        // 内存管理设置
        ConfigCategory memoryCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.memory"));
        
        memoryCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.memory.object_pooling"), objectPoolingEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.memory.object_pooling.tooltip"))
            .build());
        
        // 网络设置
        ConfigCategory networkCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.network"));
        
        networkCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.network.optimization"), networkOptimizationEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.network.optimization.tooltip"))
            .build());
        
        networkCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.network.compression"), dataCompressionEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.network.compression.tooltip"))
            .build());
        
        // 渲染设置
        ConfigCategory renderCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.render"));
        
        renderCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.render.optimization"), renderOptimizationEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.render.optimization.tooltip"))
            .build());
        
        // 云备份设置
        ConfigCategory cloudCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.cloud"));
        
        cloudCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("mcps.config.cloud.backup.enabled"), cloudBackupEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.translatable("mcps.config.cloud.backup.enabled.tooltip"))
            .build());
        
        cloudCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.cloud.backup.interval"), backupInterval)
            .setDefaultValue(60)
            .setMin(1)
            .setMax(1440)
            .setTooltip(Text.translatable("mcps.config.cloud.backup.interval.tooltip"))
            .build());
        
        cloudCategory.addEntry(entryBuilder.startIntField(Text.translatable("mcps.config.cloud.backup.max_backups"), maxBackups)
            .setDefaultValue(10)
            .setMin(1)
            .setMax(100)
            .setTooltip(Text.translatable("mcps.config.cloud.backup.max_backups.tooltip"))
            .build());
        
        // 监控界面
        ConfigCategory monitoringCategory = builder.getOrCreateCategory(Text.translatable("mcps.config.category.monitoring"));
        
        monitoringCategory.addEntry(entryBuilder.startTextDescription(Text.translatable("mcps.config.monitoring.open"))
            .setTooltip(Text.translatable("mcps.config.monitoring.open.tooltip"))
            .build());
        
        return builder.build();
    }
}