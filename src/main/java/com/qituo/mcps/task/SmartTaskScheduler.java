package com.qituo.mcps.task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import java.util.stream.Collectors;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.monitor.PerformanceMonitor;

public class SmartTaskScheduler extends TaskScheduler {
    private PerformanceMonitor performanceMonitor;
    private ConcurrentHashMap<String, TaskPerformanceData> taskPerformanceData;
    private ScheduledExecutorService analyzerExecutor;
    private int analysisInterval = 5; // 分析间隔（秒）
    private int predictionHistorySize = 100; // 预测历史大小
    private double loadThreshold = 0.7; // 系统负载阈值
    private double underLoadThreshold = 0.3; // 系统低负载阈值

    public SmartTaskScheduler(ThreadManager threadManager, PerformanceMonitor performanceMonitor) {
        super(threadManager);
        this.performanceMonitor = performanceMonitor;
        this.taskPerformanceData = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() {
        super.initialize();
        
        // 初始化性能数据收集
        for (String queueName : Arrays.asList("world_generation", "entity_ai", "block_updates", "redstone", "pathfinding")) {
            taskPerformanceData.put(queueName, new TaskPerformanceData(queueName, predictionHistorySize));
        }
        
        // 启动性能分析器
        analyzerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MCPS-Smart-Task-Analyzer");
            t.setDaemon(true);
            return t;
        });
        
        analyzerExecutor.scheduleAtFixedRate(this::analyzePerformance, 5, analysisInterval, TimeUnit.SECONDS);
        
        MCPSMod.LOGGER.info("SmartTaskScheduler initialized with performance analysis");
    }

    @Override
    public int scheduleTask(String queueName, Runnable task, String taskName) {
        // 动态调整任务分配
        String optimalQueue = selectOptimalQueue(queueName);
        
        // 计算任务优先级
        int priority = calculateTaskPriority(optimalQueue, taskName);
        
        // 调度任务
        int taskId = super.scheduleTask(optimalQueue, task, taskName, priority);
        
        // 预测任务执行时间
        long predictedTime = predictTaskExecutionTime(optimalQueue);
        MCPSMod.LOGGER.debug("Scheduled task '" + taskName + "' to queue '" + optimalQueue + "' with priority " + priority + " and predicted execution time: " + predictedTime + "ms");
        
        return taskId;
    }

    @Override
    public <T> Future<T> scheduleTaskWithResult(String queueName, Callable<T> task, String taskName) {
        // 动态调整任务分配
        String optimalQueue = selectOptimalQueue(queueName);
        
        // 计算任务优先级
        int priority = calculateTaskPriority(optimalQueue, taskName);
        
        // 调度任务
        Future<T> future = super.scheduleTaskWithResult(optimalQueue, task, taskName, priority);
        
        // 预测任务执行时间
        long predictedTime = predictTaskExecutionTime(optimalQueue);
        MCPSMod.LOGGER.debug("Scheduled task '" + taskName + "' to queue '" + optimalQueue + "' with priority " + priority + " and predicted execution time: " + predictedTime + "ms");
        
        return future;
    }
    
    // 计算任务优先级
    private int calculateTaskPriority(String queueName, String taskName) {
        // 基础优先级
        int basePriority = 5;
        
        // 根据队列类型调整优先级
        switch (queueName) {
            case "entity_ai":
                basePriority = 7; // 实体AI优先级较高
                break;
            case "redstone":
                basePriority = 6; // 红石优先级中等偏高
                break;
            case "world_generation":
                basePriority = 4; // 世界生成优先级中等
                break;
            case "block_updates":
                basePriority = 5; // 方块更新优先级中等
                break;
            case "pathfinding":
                basePriority = 6; // 寻路优先级中等偏高
                break;
        }
        
        // 根据任务名称调整优先级
        if (taskName.contains("critical")) {
            basePriority += 3; // 关键任务优先级更高
        } else if (taskName.contains("high")) {
            basePriority += 2; // 高优先级任务
        } else if (taskName.contains("low")) {
            basePriority -= 2; // 低优先级任务
        }
        
        // 根据系统负载调整优先级
        double systemLoad = calculateSystemLoad();
        if (systemLoad > 0.8) {
            // 系统负载高，提高关键任务的优先级
            if (basePriority > 5) {
                basePriority += 1;
            }
        }
        
        // 确保优先级在合理范围内
        return Math.max(1, Math.min(10, basePriority));
    }

    // 分析性能数据
    private void analyzePerformance() {
        // 分析每个队列的性能数据
        for (Map.Entry<String, TaskPerformanceData> entry : taskPerformanceData.entrySet()) {
            String queueName = entry.getKey();
            TaskPerformanceData data = entry.getValue();
            
            // 计算队列负载
            int queueSize = getQueueSize(queueName);
            double load = calculateQueueLoad(queueName);
            
            data.updateLoad(load);
            data.updateQueueSize(queueSize);
            
            // 分析系统整体负载
            double systemLoad = calculateSystemLoad();
            
            // 动态调整线程池大小
            adjustThreadPoolSize(systemLoad);
            
            MCPSMod.LOGGER.debug("Queue '" + queueName + "' load: " + String.format("%.2f", load) + ", size: " + queueSize + ", system load: " + String.format("%.2f", systemLoad));
        }
    }

    // 选择最优队列
    private String selectOptimalQueue(String preferredQueue) {
        // 如果首选队列存在且负载较低，使用首选队列
        if (taskPerformanceData.containsKey(preferredQueue)) {
            double preferredLoad = taskPerformanceData.get(preferredQueue).getCurrentLoad();
            if (preferredLoad < loadThreshold) {
                return preferredQueue;
            }
        }
        
        // 否则选择负载最低的队列
        String optimalQueue = preferredQueue;
        double minLoad = Double.MAX_VALUE;
        
        for (Map.Entry<String, TaskPerformanceData> entry : taskPerformanceData.entrySet()) {
            String queueName = entry.getKey();
            double load = entry.getValue().getCurrentLoad();
            
            if (load < minLoad) {
                minLoad = load;
                optimalQueue = queueName;
            }
        }
        
        return optimalQueue;
    }

    // 预测任务执行时间
    private long predictTaskExecutionTime(String queueName) {
        TaskPerformanceData data = taskPerformanceData.get(queueName);
        if (data == null) {
            return 100; // 默认预测时间
        }
        
        return data.predictExecutionTime();
    }

    // 计算队列负载
    private double calculateQueueLoad(String queueName) {
        TaskPerformanceData data = taskPerformanceData.get(queueName);
        if (data == null) {
            return 0.0;
        }
        
        // 基于队列大小和历史执行时间计算负载
        int queueSize = getQueueSize(queueName);
        long avgExecutionTime = data.getAverageExecutionTime();
        
        // 计算队列预计处理时间
        long estimatedTime = queueSize * avgExecutionTime;
        
        // 归一化到0-1之间
        return Math.min(1.0, (double) estimatedTime / 10000.0); // 10秒作为满负载
    }

    // 计算系统整体负载
    private double calculateSystemLoad() {
        ThreadManager threadManager = MCPSMod.getInstance().getThreadManager();
        if (threadManager == null) {
            return 0.0;
        }
        
        int activeTasks = threadManager.getActiveTaskCount();
        int poolSize = threadManager.getCorePoolSize();
        
        return (double) activeTasks / poolSize;
    }

    // 动态调整线程池大小
    private void adjustThreadPoolSize(double systemLoad) {
        ThreadManager threadManager = MCPSMod.getInstance().getThreadManager();
        if (threadManager == null) {
            return;
        }
        
        int currentPoolSize = threadManager.getCorePoolSize();
        int newPoolSize = currentPoolSize;
        
        // 如果系统负载高于阈值，增加线程池大小
        if (systemLoad > loadThreshold && currentPoolSize < Runtime.getRuntime().availableProcessors() * 2) {
            threadManager.addThreads(1);
            MCPSMod.LOGGER.info("Increased thread pool size due to high system load: " + String.format("%.2f", systemLoad));
        }
        // 如果系统负载低于阈值，减少线程池大小
        else if (systemLoad < underLoadThreshold && currentPoolSize > 2) {
            threadManager.removeThreads(1);
            MCPSMod.LOGGER.info("Decreased thread pool size due to low system load: " + String.format("%.2f", systemLoad));
        }
    }

    // 停止智能任务调度器
    public void stop() {
        if (analyzerExecutor != null) {
            analyzerExecutor.shutdown();
            try {
                if (!analyzerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    analyzerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                analyzerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        MCPSMod.LOGGER.info("SmartTaskScheduler stopped");
    }

    // 任务性能数据类
    private static class TaskPerformanceData {
        private String queueName;
        private int historySize;
        private LinkedList<Long> executionTimes;
        private LinkedList<Double> loadHistory;
        private LinkedList<Integer> queueSizeHistory;
        private AtomicInteger executionCount;
        private AtomicLong totalExecutionTime;

        public TaskPerformanceData(String queueName, int historySize) {
            this.queueName = queueName;
            this.historySize = historySize;
            this.executionTimes = new LinkedList<>();
            this.loadHistory = new LinkedList<>();
            this.queueSizeHistory = new LinkedList<>();
            this.executionCount = new AtomicInteger(0);
            this.totalExecutionTime = new AtomicLong(0);
        }

        public void updateExecutionTime(long time) {
            executionTimes.add(time);
            if (executionTimes.size() > historySize) {
                executionTimes.removeFirst();
            }
            
            executionCount.incrementAndGet();
            totalExecutionTime.addAndGet(time);
        }

        public void updateLoad(double load) {
            loadHistory.add(load);
            if (loadHistory.size() > historySize) {
                loadHistory.removeFirst();
            }
        }

        public void updateQueueSize(int size) {
            queueSizeHistory.add(size);
            if (queueSizeHistory.size() > historySize) {
                queueSizeHistory.removeFirst();
            }
        }

        public long getAverageExecutionTime() {
            if (executionTimes.isEmpty()) {
                return 100; // 默认值
            }
            
            long sum = 0;
            for (Long time : executionTimes) {
                sum += time;
            }
            
            return sum / executionTimes.size();
        }

        public double getCurrentLoad() {
            if (loadHistory.isEmpty()) {
                return 0.0;
            }
            
            double sum = 0;
            for (Double load : loadHistory) {
                sum += load;
            }
            
            return sum / loadHistory.size();
        }

        public int getCurrentQueueSize() {
            if (queueSizeHistory.isEmpty()) {
                return 0;
            }
            
            return queueSizeHistory.getLast();
        }

        // 预测任务执行时间（使用线性回归模型）
        public long predictExecutionTime() {
            if (executionTimes.isEmpty()) {
                return 100; // 默认值
            }
            
            // 如果历史数据较少，使用加权移动平均
            if (executionTimes.size() < 5) {
                // 使用加权移动平均，最近的执行时间权重更高
                double weight = 0.5; // 最近时间的权重
                double weightedSum = 0;
                double weightSum = 0;
                
                int i = 0;
                for (Long time : executionTimes) {
                    double currentWeight = Math.pow(weight, i);
                    weightedSum += time * currentWeight;
                    weightSum += currentWeight;
                    i++;
                }
                
                return (long) (weightedSum / weightSum);
            }
            
            // 使用线性回归模型预测
            return predictWithLinearRegression();
        }
        
        // 使用线性回归模型预测任务执行时间
        private long predictWithLinearRegression() {
            int n = executionTimes.size();
            if (n < 2) {
                return getAverageExecutionTime();
            }
            
            // 准备数据
            double[] x = new double[n];
            double[] y = new double[n];
            
            for (int i = 0; i < n; i++) {
                x[i] = i; // 时间索引
                y[i] = executionTimes.get(i); // 执行时间
            }
            
            // 计算线性回归系数
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < n; i++) {
                sumX += x[i];
                sumY += y[i];
                sumXY += x[i] * y[i];
                sumX2 += x[i] * x[i];
            }
            
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            
            // 预测下一个任务的执行时间
            double predictedTime = slope * n + intercept;
            
            // 确保预测值为正数
            return (long) Math.max(1, predictedTime);
        }

        // 预测队列大小（简单的移动平均）
        public int predictQueueSize() {
            if (queueSizeHistory.isEmpty()) {
                return 0;
            }
            
            int sum = 0;
            for (Integer size : queueSizeHistory) {
                sum += size;
            }
            
            return sum / queueSizeHistory.size();
        }
    }
}