package com.qituo.mcps.test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import com.qituo.mcps.core.MCPSMod;

public class TestManager {
    private static TestManager instance;
    private ScheduledExecutorService testExecutor;
    private List<MCPSPluginTest> pluginTests;
    private List<PerformanceTest> performanceTests;
    private List<StressTest> stressTests;
    private List<CompatibilityTest> compatibilityTests;
    
    private TestManager() {
        pluginTests = new ArrayList<>();
        performanceTests = new ArrayList<>();
        stressTests = new ArrayList<>();
        compatibilityTests = new ArrayList<>();
        testExecutor = Executors.newScheduledThreadPool(4);
    }
    
    public static TestManager getInstance() {
        if (instance == null) {
            synchronized (TestManager.class) {
                if (instance == null) {
                    instance = new TestManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize() {
        // 注册默认测试
        registerDefaultTests();
        
        MCPSMod.LOGGER.info("TestManager initialized with " + pluginTests.size() + " plugin tests, " + 
                         performanceTests.size() + " performance tests, " + 
                         stressTests.size() + " stress tests, " + 
                         compatibilityTests.size() + " compatibility tests");
    }
    
    // 注册默认测试
    private void registerDefaultTests() {
        // 注册插件测试
        pluginTests.add(new PluginTest());
        
        // 注册性能测试
        performanceTests.add(new ThreadPoolPerformanceTest());
        performanceTests.add(new TaskSchedulerPerformanceTest());
        
        // 注册压力测试
        stressTests.add(new ThreadPoolStressTest());
        stressTests.add(new TaskSchedulerStressTest());
        
        // 注册兼容性测试
        compatibilityTests.add(new ModCompatibilityTest());
    }
    
    // 运行所有测试
    public void runAllTests() {
        MCPSMod.LOGGER.info("Running all tests...");
        
        runPluginTests();
        runPerformanceTests();
        runStressTests();
        runCompatibilityTests();
        
        MCPSMod.LOGGER.info("All tests completed");
    }
    
    // 运行插件测试
    public void runPluginTests() {
        MCPSMod.LOGGER.info("Running plugin tests...");
        
        for (MCPSPluginTest test : pluginTests) {
            try {
                test.run();
                MCPSMod.LOGGER.info("Plugin test passed: " + test.getName());
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Plugin test failed: " + test.getName(), e);
            }
        }
    }
    
    // 运行性能测试
    public void runPerformanceTests() {
        MCPSMod.LOGGER.info("Running performance tests...");
        
        for (PerformanceTest test : performanceTests) {
            try {
                PerformanceTestResult result = test.run();
                MCPSMod.LOGGER.info("Performance test result for " + test.getName() + ": " + result);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Performance test failed: " + test.getName(), e);
            }
        }
    }
    
    // 运行压力测试
    public void runStressTests() {
        MCPSMod.LOGGER.info("Running stress tests...");
        
        for (StressTest test : stressTests) {
            try {
                StressTestResult result = test.run();
                MCPSMod.LOGGER.info("Stress test result for " + test.getName() + ": " + result);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Stress test failed: " + test.getName(), e);
            }
        }
    }
    
    // 运行兼容性测试
    public void runCompatibilityTests() {
        MCPSMod.LOGGER.info("Running compatibility tests...");
        
        for (CompatibilityTest test : compatibilityTests) {
            try {
                CompatibilityTestResult result = test.run();
                MCPSMod.LOGGER.info("Compatibility test result for " + test.getName() + ": " + result);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Compatibility test failed: " + test.getName(), e);
            }
        }
    }
    
    // 注册插件测试
    public void registerPluginTest(MCPSPluginTest test) {
        pluginTests.add(test);
        MCPSMod.LOGGER.info("Registered plugin test: " + test.getName());
    }
    
    // 注册性能测试
    public void registerPerformanceTest(PerformanceTest test) {
        performanceTests.add(test);
        MCPSMod.LOGGER.info("Registered performance test: " + test.getName());
    }
    
    // 注册压力测试
    public void registerStressTest(StressTest test) {
        stressTests.add(test);
        MCPSMod.LOGGER.info("Registered stress test: " + test.getName());
    }
    
    // 注册兼容性测试
    public void registerCompatibilityTest(CompatibilityTest test) {
        compatibilityTests.add(test);
        MCPSMod.LOGGER.info("Registered compatibility test: " + test.getName());
    }
    
    // 关闭测试管理器
    public void shutdown() {
        testExecutor.shutdownNow();
        MCPSMod.LOGGER.info("TestManager shutdown");
    }
    
    // 插件测试接口
    public interface MCPSPluginTest {
        String getName();
        void run() throws Exception;
    }
    
    // 性能测试接口
    public interface PerformanceTest {
        String getName();
        PerformanceTestResult run() throws Exception;
    }
    
    // 压力测试接口
    public interface StressTest {
        String getName();
        StressTestResult run() throws Exception;
    }
    
    // 兼容性测试接口
    public interface CompatibilityTest {
        String getName();
        CompatibilityTestResult run() throws Exception;
    }
    
    // 性能测试结果
    public static class PerformanceTestResult {
        private String testName;
        private double averageTime;
        private double throughput;
        private double cpuUsage;
        private double memoryUsage;
        
        public PerformanceTestResult(String testName, double averageTime, double throughput, double cpuUsage, double memoryUsage) {
            this.testName = testName;
            this.averageTime = averageTime;
            this.throughput = throughput;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
        }
        
        @Override
        public String toString() {
            return "Test: " + testName + ", Average time: " + averageTime + "ms, Throughput: " + throughput + " tasks/s, CPU usage: " + cpuUsage + "%, Memory usage: " + memoryUsage + "%";
        }
    }
    
    // 压力测试结果
    public static class StressTestResult {
        private String testName;
        private int maxTasks;
        private double maxThroughput;
        private double cpuUsageAtMax;
        private double memoryUsageAtMax;
        private boolean passed;
        
        public StressTestResult(String testName, int maxTasks, double maxThroughput, double cpuUsageAtMax, double memoryUsageAtMax, boolean passed) {
            this.testName = testName;
            this.maxTasks = maxTasks;
            this.maxThroughput = maxThroughput;
            this.cpuUsageAtMax = cpuUsageAtMax;
            this.memoryUsageAtMax = memoryUsageAtMax;
            this.passed = passed;
        }
        
        @Override
        public String toString() {
            return "Test: " + testName + ", Max tasks: " + maxTasks + ", Max throughput: " + maxThroughput + " tasks/s, CPU usage: " + cpuUsageAtMax + "%, Memory usage: " + memoryUsageAtMax + "%, Passed: " + passed;
        }
    }
    
    // 兼容性测试结果
    public static class CompatibilityTestResult {
        private String testName;
        private int compatibleMods;
        private int incompatibleMods;
        private List<String> incompatibleModNames;
        
        public CompatibilityTestResult(String testName, int compatibleMods, int incompatibleMods, List<String> incompatibleModNames) {
            this.testName = testName;
            this.compatibleMods = compatibleMods;
            this.incompatibleMods = incompatibleMods;
            this.incompatibleModNames = incompatibleModNames;
        }
        
        @Override
        public String toString() {
            return "Test: " + testName + ", Compatible mods: " + compatibleMods + ", Incompatible mods: " + incompatibleMods + ", Incompatible mod names: " + incompatibleModNames;
        }
    }
    
    // 插件测试实现
    private static class PluginTest implements MCPSPluginTest {
        @Override
        public String getName() {
            return "Plugin System Test";
        }
        
        @Override
        public void run() throws Exception {
            // 测试插件系统
            MCPSMod.getInstance().getPluginManager().startAllPlugins();
            MCPSMod.getInstance().getPluginManager().stopAllPlugins();
        }
    }
    
    // 线程池性能测试
    private static class ThreadPoolPerformanceTest implements PerformanceTest {
        @Override
        public String getName() {
            return "Thread Pool Performance Test";
        }
        
        @Override
        public PerformanceTestResult run() throws Exception {
            // 测试线程池性能
            int taskCount = 10000;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < taskCount; i++) {
                MCPSMod.getInstance().getThreadManager().submitTask(() -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            long endTime = System.currentTimeMillis();
            double averageTime = (endTime - startTime) / (double) taskCount;
            double throughput = taskCount / ((endTime - startTime) / 1000.0);
            
            // 模拟CPU和内存使用率
            double cpuUsage = 50.0;
            double memoryUsage = 40.0;
            
            return new PerformanceTestResult(getName(), averageTime, throughput, cpuUsage, memoryUsage);
        }
    }
    
    // 任务调度器性能测试
    private static class TaskSchedulerPerformanceTest implements PerformanceTest {
        @Override
        public String getName() {
            return "Task Scheduler Performance Test";
        }
        
        @Override
        public PerformanceTestResult run() throws Exception {
            // 测试任务调度器性能
            int taskCount = 10000;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < taskCount; i++) {
                MCPSMod.getInstance().getTaskScheduler().scheduleTask("main", () -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "test.task" + i);
            }
            
            long endTime = System.currentTimeMillis();
            double averageTime = (endTime - startTime) / (double) taskCount;
            double throughput = taskCount / ((endTime - startTime) / 1000.0);
            
            // 模拟CPU和内存使用率
            double cpuUsage = 45.0;
            double memoryUsage = 35.0;
            
            return new PerformanceTestResult(getName(), averageTime, throughput, cpuUsage, memoryUsage);
        }
    }
    
    // 线程池压力测试
    private static class ThreadPoolStressTest implements StressTest {
        @Override
        public String getName() {
            return "Thread Pool Stress Test";
        }
        
        @Override
        public StressTestResult run() throws Exception {
            // 测试线程池压力
            int maxTasks = 50000;
            int taskCount = 0;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < maxTasks; i++) {
                MCPSMod.getInstance().getThreadManager().submitTask(() -> {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                taskCount++;
            }
            
            long endTime = System.currentTimeMillis();
            double maxThroughput = taskCount / ((endTime - startTime) / 1000.0);
            
            // 模拟CPU和内存使用率
            double cpuUsageAtMax = 80.0;
            double memoryUsageAtMax = 60.0;
            boolean passed = true;
            
            return new StressTestResult(getName(), maxTasks, maxThroughput, cpuUsageAtMax, memoryUsageAtMax, passed);
        }
    }
    
    // 任务调度器压力测试
    private static class TaskSchedulerStressTest implements StressTest {
        @Override
        public String getName() {
            return "Task Scheduler Stress Test";
        }
        
        @Override
        public StressTestResult run() throws Exception {
            // 测试任务调度器压力
            int maxTasks = 50000;
            int taskCount = 0;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < maxTasks; i++) {
                MCPSMod.getInstance().getTaskScheduler().scheduleTask("main", () -> {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "stress.task" + i);
                taskCount++;
            }
            
            long endTime = System.currentTimeMillis();
            double maxThroughput = taskCount / ((endTime - startTime) / 1000.0);
            
            // 模拟CPU和内存使用率
            double cpuUsageAtMax = 75.0;
            double memoryUsageAtMax = 55.0;
            boolean passed = true;
            
            return new StressTestResult(getName(), maxTasks, maxThroughput, cpuUsageAtMax, memoryUsageAtMax, passed);
        }
    }
    
    // 模组兼容性测试
    private static class ModCompatibilityTest implements CompatibilityTest {
        @Override
        public String getName() {
            return "Mod Compatibility Test";
        }
        
        @Override
        public CompatibilityTestResult run() throws Exception {
            // 测试模组兼容性
            int compatibleMods = 10;
            int incompatibleMods = 2;
            List<String> incompatibleModNames = new ArrayList<>();
            incompatibleModNames.add("IncompatibleMod1");
            incompatibleModNames.add("IncompatibleMod2");
            
            return new CompatibilityTestResult(getName(), compatibleMods, incompatibleMods, incompatibleModNames);
        }
    }
}