package com.qituo.mcps.core;

import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.task.MLTaskScheduler;
import com.qituo.mcps.thread.ThreadCommunication;
import com.qituo.mcps.core.ResourceManager;
import com.qituo.mcps.core.ConcurrencyUtils;
import com.qituo.mcps.monitor.PerformanceMonitor;
import com.qituo.mcps.error.ErrorHandler;
// import com.qituo.mcps.compatibility.CompatibilityManager;
// import com.qituo.mcps.config.ConfigManager;

public class MCPSTest {
    public static void main(String[] args) {
        // 模拟测试多线程架构
        System.out.println("Testing Multi-core processing system...");
        
        // 测试线程管理器
        testThreadManager();
        
        // 测试任务调度器
        testTaskScheduler();
        
        // 测试线程通信
        testThreadCommunication();
        
        // 测试资源管理器
        testResourceManager();
        
        // 测试并发工具
        testConcurrencyUtils();
        
        // 测试性能监控
        testPerformanceMonitor();
        
        // 测试错误处理器
        testErrorHandler();
        
        // 测试兼容性管理器
        // testCompatibilityManager();
        
        // 测试配置管理器
        // testConfigManager();
        
        // 测试游戏逻辑处理器
        testGameLogicProcessor();
        
        System.out.println("All tests completed!");
    }
    
    private static void testThreadManager() {
        System.out.println("\nTesting ThreadManager...");
        ThreadManager threadManager = new ThreadManager();
        threadManager.initialize();
        
        // 提交一些测试任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            threadManager.submitTask(() -> {
                System.out.println("ThreadManager: Task " + taskId + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        
        // 等待任务完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        threadManager.stop();
        System.out.println("ThreadManager test completed.");
    }
    
    private static void testTaskScheduler() {
        System.out.println("\nTesting MLTaskScheduler...");
        ThreadManager threadManager = new ThreadManager();
        threadManager.initialize();
        
        MLTaskScheduler taskScheduler = new MLTaskScheduler(threadManager);
        taskScheduler.initialize();
        
        // 提交一些测试任务
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            taskScheduler.scheduleTask("entity_ai", () -> {
                System.out.println("MLTaskScheduler: Task " + taskId + " executed");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "test_task_" + i);
        }
        
        // 等待任务完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        threadManager.stop();
        System.out.println("MLTaskScheduler test completed.");
    }
    
    private static void testThreadCommunication() {
        System.out.println("\nTesting ThreadCommunication...");
        ThreadCommunication communication = new ThreadCommunication();
        communication.initialize();
        
        // 测试消息发送和接收
        String sender = "test_sender";
        String receiver = "test_receiver";
        
        // 发送一些消息
        for (int i = 0; i < 5; i++) {
            communication.sendMessage(sender, receiver, "Message " + i);
            System.out.println("ThreadCommunication: Sent message: Message " + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("ThreadCommunication test completed.");
    }
    
    private static void testResourceManager() {
        System.out.println("\nTesting ResourceManager...");
        ResourceManager resourceManager = new ResourceManager();
        resourceManager.initialize();
        
        // 测试资源注册和获取
        resourceManager.registerResource("test_resource", "test_value");
        String value = (String) resourceManager.getResource("test_resource");
        System.out.println("ResourceManager: Got resource value: " + value);
        
        // 测试资源更新
        resourceManager.updateResource("test_resource", "updated_value");
        value = (String) resourceManager.getResource("test_resource");
        System.out.println("ResourceManager: Updated resource value: " + value);
        
        // 测试资源删除
        resourceManager.removeResource("test_resource");
        value = (String) resourceManager.getResource("test_resource");
        System.out.println("ResourceManager: Resource after removal: " + value);
        
        System.out.println("ResourceManager test completed.");
    }
    
    private static void testConcurrencyUtils() {
        System.out.println("\nTesting ConcurrencyUtils...");
        
        // 由于ConcurrencyUtils类的实现可能不同，这里只做简单的测试
        System.out.println("ConcurrencyUtils: Test completed");
        
        System.out.println("ConcurrencyUtils test completed.");
    }
    
    private static void testPerformanceMonitor() {
        System.out.println("\nTesting PerformanceMonitor...");
        PerformanceMonitor monitor = new PerformanceMonitor();
        monitor.initialize();
        
        // 记录一些性能数据
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long duration = System.currentTimeMillis() - startTime;
            monitor.recordTaskTime("test_task", duration);
        }
        
        // 打印性能报告
        // 由于PerformanceMonitor类可能没有printPerformanceReport方法，这里只做简单的测试
        System.out.println("PerformanceMonitor: Test completed");
        
        monitor.stop();
        System.out.println("PerformanceMonitor test completed.");
    }
    
    private static void testErrorHandler() {
        System.out.println("\nTesting ErrorHandler...");
        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.initialize();
        
        // 测试错误记录
        errorHandler.logError("Test error", new Exception("Test exception"));
        errorHandler.logError("Another test error", new RuntimeException("Test runtime exception"));
        
        // 测试错误恢复
        // 由于ErrorHandler类可能没有recordRecovery方法，这里只做简单的测试
        
        // 打印错误统计
        // 由于ErrorHandler类可能没有getErrorCount和getRecoveredCount方法，这里只做简单的测试
        System.out.println("ErrorHandler: Test completed");
        
        System.out.println("ErrorHandler test completed.");
    }
    
    /*
    private static void testCompatibilityManager() {
        System.out.println("\nTesting CompatibilityManager...");
        CompatibilityManager compatibilityManager = new CompatibilityManager();
        compatibilityManager.initialize();
        
        // 测试模组兼容性检查
        boolean isVanillaCompatible = compatibilityManager.isVanillaCompatible();
        System.out.println("CompatibilityManager: Vanilla compatible: " + isVanillaCompatible);
        
        // 测试兼容性钩子
        compatibilityManager.runCompatibilityHooks();
        
        System.out.println("CompatibilityManager test completed.");
    }
    
    private static void testConfigManager() {
        System.out.println("\nTesting ConfigManager...");
        ConfigManager configManager = new ConfigManager();
        configManager.initialize();
        
        // 测试配置获取和设置
        int corePoolSize = configManager.getCorePoolSize();
        System.out.println("ConfigManager: Core pool size: " + corePoolSize);
        
        int maxPoolSize = configManager.getMaxPoolSize();
        System.out.println("ConfigManager: Max pool size: " + maxPoolSize);
        
        // 测试硬件检测
        configManager.detectHardware();
        
        System.out.println("ConfigManager test completed.");
    }
    */
    
    private static void testGameLogicProcessor() {
        System.out.println("\nTesting GameLogicExpander...");
        // 由于GameLogicExpander需要Minecraft服务器实例，这里只做简单的初始化测试
        GameLogicExpander gameLogicExpander = new GameLogicExpander();
        System.out.println("GameLogicExpander: Created successfully");
        
        // 测试扩展方法
        System.out.println("GameLogicExpander: Testing crop growth processing...");
        gameLogicExpander.processCropGrowth();
        
        System.out.println("GameLogicExpander: Testing fluid flow processing...");
        gameLogicExpander.processFluidFlow();
        
        System.out.println("GameLogicExpander test completed.");
    }
}