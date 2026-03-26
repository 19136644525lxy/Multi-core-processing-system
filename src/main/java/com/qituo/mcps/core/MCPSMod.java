package com.qituo.mcps.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.monitor.PerformanceMonitor;
import com.qituo.mcps.task.SmartTaskScheduler;
import com.qituo.mcps.core.ResourceManager;
import com.qituo.mcps.thread.ThreadCommunication;
import com.qituo.mcps.core.GameLogicParallelizer;
import com.qituo.mcps.error.ErrorHandler;
import com.qituo.mcps.compatibility.CompatibilityManager;
import com.qituo.mcps.config.ConfigManager;
import com.qituo.mcps.config.MCPSConfig;
import com.qituo.mcps.ai.AIManager;
import com.qituo.mcps.ai.NPCBehaviorManager;
import com.qituo.mcps.gpu.GPUManager;
import com.qituo.mcps.cluster.ClusterManager;
import com.qituo.mcps.network.NetworkManager;
import com.qituo.mcps.render.RenderManager;
import com.qituo.mcps.storage.StorageManager;
import com.qituo.mcps.api.APIDocumentation;
import com.qituo.mcps.api.EventManager;
import com.qituo.mcps.api.PluginManager;
import com.qituo.mcps.cloud.CloudCommands;
import com.qituo.mcps.cloud.CloudManager;
import com.qituo.mcps.diagnostic.DiagnosticManager;
import com.qituo.mcps.diagnostic.PerformanceCommands;
import com.qituo.mcps.diagnostic.RealTimePerformanceAnalyzer;
import com.qituo.mcps.diagnostic.BottleneckDetector;
import com.qituo.mcps.diagnostic.OptimizationSuggestionSystem;
import com.qituo.mcps.integration.CompatibilityTestTool;
import com.qituo.mcps.integration.EcosystemCommands;
import com.qituo.mcps.integration.IntegrationHelper;
import com.qituo.mcps.integration.ModInteractionFramework;
import com.qituo.mcps.integration.ModLoadingOptimizer;
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
    private GameLogicParallelizer gameLogicProcessor;
    private ErrorHandler errorHandler;
    private CompatibilityManager compatibilityManager;
    private ConfigManager configManager;
    private AIManager aiManager;
    private NPCBehaviorManager npcBehaviorManager;
    private GPUManager gpuManager;
    private ClusterManager clusterManager;
    private NetworkManager networkManager;
    private RenderManager renderManager;
    private StorageManager storageManager;
    private EventManager eventManager;
    private PluginManager pluginManager;
    private CloudManager cloudManager;
    private DiagnosticManager diagnosticManager;
    private PlatformManager platformManager;
    private TestManager testManager;
    private APIDocumentation apiDocumentation;
    private CompatibilityTestTool compatibilityTestTool;
    private IntegrationHelper integrationHelper;
    private ModInteractionFramework modInteractionFramework;
    private ModLoadingOptimizer modLoadingOptimizer;
    private RealTimePerformanceAnalyzer performanceAnalyzer;
    private BottleneckDetector bottleneckDetector;
    private OptimizationSuggestionSystem optimizationSuggestionSystem;
    
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
        
        // 初始化NPC行为管理器
        npcBehaviorManager = NPCBehaviorManager.getInstance();
        npcBehaviorManager.initialize(threadManager, taskScheduler);
        
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
        
        // 初始化云管理器
        cloudManager = new CloudManager();
        cloudManager.initialize();
        
        // 初始化游戏逻辑处理器
        gameLogicProcessor = new GameLogicParallelizer();
        
        // 初始化模组生态系统
        apiDocumentation = APIDocumentation.getInstance();
        compatibilityTestTool = CompatibilityTestTool.getInstance();
        integrationHelper = IntegrationHelper.getInstance();
        modInteractionFramework = ModInteractionFramework.getInstance();
        modLoadingOptimizer = ModLoadingOptimizer.getInstance();
        
        // 注册MCPS模组到交互框架
        modInteractionFramework.registerMod(MOD_ID, "1.0.0", "Multi-core processing system for Minecraft");
        
        // 初始化性能分析工具
        performanceAnalyzer = RealTimePerformanceAnalyzer.getInstance();
        bottleneckDetector = BottleneckDetector.getInstance();
        optimizationSuggestionSystem = OptimizationSuggestionSystem.getInstance();
        
        
        // 注册云备份命令
        CommandRegistrationCallback.EVENT.register(CloudCommands::register);
        
        // 注册生态系统命令
        CommandRegistrationCallback.EVENT.register(EcosystemCommands::register);
        
        // 注册性能分析命令
        CommandRegistrationCallback.EVENT.register(PerformanceCommands::register);
        
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
            npcBehaviorManager.stop();
            gpuManager.stop();
            clusterManager.stop();
            networkManager.shutdown();
            renderManager.shutdown();
            storageManager.shutdown();
            cloudManager.shutdown();
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
    
    public GameLogicParallelizer getGameLogicProcessor() {
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
    
    public NPCBehaviorManager getNpcBehaviorManager() {
        return npcBehaviorManager;
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
    
    public CloudManager getCloudManager() {
        return cloudManager;
    }
}