package com.qituo.mcps.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.monitor.PerformanceMonitor;
import com.qituo.mcps.task.SmartTaskScheduler;
import com.qituo.mcps.core.ResourceManager;
import com.qituo.mcps.thread.ThreadCommunication;
import com.qituo.mcps.core.GameLogicExpander;
import com.qituo.mcps.error.ErrorHandler;
import com.qituo.mcps.compatibility.CompatibilityManager;
import com.qituo.mcps.config.ConfigManager;
import com.qituo.mcps.config.MCPSConfig;
import com.qituo.mcps.ai.AIManager;
import com.qituo.mcps.gpu.GPUManager;
import com.qituo.mcps.cluster.ClusterManager;
import com.qituo.mcps.network.NetworkManager;
import com.qituo.mcps.render.RenderManager;
import com.qituo.mcps.storage.StorageManager;
import com.qituo.mcps.api.EventManager;
import com.qituo.mcps.api.PluginManager;
import com.qituo.mcps.diagnostic.DiagnosticManager;
import com.qituo.mcps.platform.PlatformManager;
import com.qituo.mcps.test.TestManager;

public class MCPSMod implements ModInitializer {
    public static final String MOD_ID = "mcps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static MCPSMod instance;
    private ThreadManager threadManager;
    private PerformanceMonitor performanceMonitor;
    private SmartTaskScheduler taskScheduler;
    private ResourceManager resourceManager;
    private ThreadCommunication threadCommunication;
    private GameLogicExpander gameLogicProcessor;
    private ErrorHandler errorHandler;
    private CompatibilityManager compatibilityManager;
    private ConfigManager configManager;
    private AIManager aiManager;
    private GPUManager gpuManager;
    private ClusterManager clusterManager;
    private NetworkManager networkManager;
    private RenderManager renderManager;
    private StorageManager storageManager;
    private EventManager eventManager;
    private PluginManager pluginManager;
    private DiagnosticManager diagnosticManager;
    private PlatformManager platformManager;
    private TestManager testManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        
        // 初始化配置管理器
        configManager = new ConfigManager();
        configManager.initialize();
        configManager.detectAndOptimize();
        
        // 初始化错误处理器
        errorHandler = new ErrorHandler();
        errorHandler.initialize();
        
        // 初始化线程管理器
        threadManager = new ThreadManager();
        threadManager.initialize();
        
        // 初始化性能监控器
        performanceMonitor = new PerformanceMonitor();
        performanceMonitor.initialize();
        
        // 初始化线程通信系统
        threadCommunication = new ThreadCommunication();
        threadCommunication.initialize();
        
        // 初始化智能任务调度器
        taskScheduler = new SmartTaskScheduler(threadManager, performanceMonitor);
        taskScheduler.initialize();
        
        // 初始化资源管理器
        resourceManager = new ResourceManager();
        resourceManager.initialize();
        
        // 初始化兼容性管理器
        compatibilityManager = new CompatibilityManager();
        compatibilityManager.initialize();
        
        // 初始化AI管理器
        aiManager = AIManager.getInstance();
        aiManager.initialize(threadManager, taskScheduler);
        
        // 初始化GPU管理器
        gpuManager = GPUManager.getInstance();
        gpuManager.initialize(threadManager);
        
        // 初始化集群管理器
        clusterManager = ClusterManager.getInstance();
        clusterManager.initialize(threadManager);
        
        // 初始化网络管理器
        networkManager = new NetworkManager();
        networkManager.initialize(25565);
        
        // 初始化渲染管理器
        renderManager = new RenderManager();
        renderManager.initialize();
        
        // 初始化事件管理器
        eventManager = EventManager.getInstance();
        
        // 初始化插件管理器
        pluginManager = PluginManager.getInstance();
        
        // 初始化诊断管理器
        diagnosticManager = DiagnosticManager.getInstance();
        diagnosticManager.initialize();
        
        // 初始化平台管理器
        platformManager = PlatformManager.getInstance();
        platformManager.printPlatformInfo();
        platformManager.adjustOptimizationStrategy();
        
        // 初始化测试管理器
        testManager = TestManager.getInstance();
        testManager.initialize();
        
        // 初始化游戏逻辑处理器
        gameLogicProcessor = new GameLogicExpander();
        
        // 命令已移除，使用按键绑定打开监控界面
        
        // 注册服务器生命周期事件
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Multi-core processing system starting...");
            
            // 初始化存储管理器，使用默认的世界目录
            storageManager = new StorageManager();
            storageManager.initialize("");
            
            // 启动线程管理器
            threadManager.start();
            
            // 初始化游戏逻辑处理器
            gameLogicProcessor.initialize(server);
            
            // 运行兼容性钩子
            compatibilityManager.runCompatibilityHooks();
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Multi-core processing system stopping...");
            threadManager.stop();
            performanceMonitor.stop();
            taskScheduler.stop();
            aiManager.stop();
            gpuManager.stop();
            clusterManager.stop();
            networkManager.shutdown();
            renderManager.shutdown();
            storageManager.shutdown();
            pluginManager.stopAllPlugins();
            diagnosticManager.shutdown();
            testManager.shutdown();
        });
        
        LOGGER.info("Multi-core processing system initialized");
    }
    
    public static MCPSMod getInstance() {
        return instance;
    }
    
    public ThreadManager getThreadManager() {
        return threadManager;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public SmartTaskScheduler getTaskScheduler() {
        return taskScheduler;
    }
    
    public ResourceManager getResourceManager() {
        return resourceManager;
    }
    
    public ThreadCommunication getThreadCommunication() {
        return threadCommunication;
    }
    
    public GameLogicProcessor getGameLogicProcessor() {
        return gameLogicProcessor;
    }
    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    public CompatibilityManager getCompatibilityManager() {
        return compatibilityManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AIManager getAiManager() {
        return aiManager;
    }
    
    public GPUManager getGpuManager() {
        return gpuManager;
    }
    
    public ClusterManager getClusterManager() {
        return clusterManager;
    }
    
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
    
    public RenderManager getRenderManager() {
        return renderManager;
    }
    
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    public DiagnosticManager getDiagnosticManager() {
        return diagnosticManager;
    }
    
    public PlatformManager getPlatformManager() {
        return platformManager;
    }
    
    public TestManager getTestManager() {
        return testManager;
    }
}