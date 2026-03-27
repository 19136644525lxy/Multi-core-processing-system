package com.qituo.mcps.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.qituo.mcps.core.MCPSMod;

public class ThreadManager {
    private ThreadPoolExecutor executorService;
    private int corePoolSize;
    private int maxPoolSize;
    private List<WorkerThread> workerThreads;
    private PriorityBlockingQueue<Runnable> taskQueue;
    private AtomicInteger taskCounter;
    private AtomicBoolean energySavingMode;
    private ScheduledExecutorService monitorExecutor;
    private ConcurrentHashMap<String, ThreadUsageData> threadUsageData;
    private static AtomicLong totalTaskExecutionTime;
    private static AtomicLong totalTasksExecuted;
    
    // 静态初始化块
    static {
        totalTaskExecutionTime = new AtomicLong(0);
        totalTasksExecuted = new AtomicLong(0);
    }
    
    public void initialize() {
        // 根据系统CPU核心数确定线程池大小
        corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        maxPoolSize = corePoolSize * 2;
        taskQueue = new PriorityBlockingQueue<>(11, (o1, o2) -> {
            if (o1 instanceof PrioritizedTask && o2 instanceof PrioritizedTask) {
                return Integer.compare(((PrioritizedTask) o2).getPriority(), ((PrioritizedTask) o1).getPriority());
            }
            return 0;
        });
        workerThreads = new ArrayList<>();
        taskCounter = new AtomicInteger(0);
        energySavingMode = new AtomicBoolean(false);
        threadUsageData = new ConcurrentHashMap<>();
        
        // 创建线程池
        executorService = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L,
            TimeUnit.SECONDS,
            taskQueue,
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "MCPS-Worker-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        ) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                threadUsageData.computeIfAbsent(t.getName(), k -> new ThreadUsageData()).startExecution();
            }
            
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                String threadName = Thread.currentThread().getName();
                ThreadUsageData data = threadUsageData.get(threadName);
                if (data != null) {
                    data.endExecution();
                }
                taskCounter.decrementAndGet();
                totalTasksExecuted.incrementAndGet();
            }
        };
        
        // 创建监控线程池
        monitorExecutor = Executors.newScheduledThreadPool(1);
        
        // 启动自适应线程池监控
        startAdaptiveThreadPoolMonitor();
        
        // 启动能源管理监控
        startEnergyManagementMonitor();
        
        MCPSMod.LOGGER.info("ThreadManager initialized with " + corePoolSize + " core threads");
    }
    
    public void start() {
        // 启动所有工作线程
        for (int i = 0; i < corePoolSize; i++) {
            WorkerThread worker = new WorkerThread(i);
            workerThreads.add(worker);
            executorService.execute(worker);
        }
        
        MCPSMod.LOGGER.info("ThreadManager started with " + workerThreads.size() + " worker threads");
    }
    
    public void stop() {
        // 关闭监控线程池
        monitorExecutor.shutdownNow();
        
        // 关闭线程池，立即中断所有线程
        executorService.shutdownNow();
        
        workerThreads.clear();
        MCPSMod.LOGGER.info("ThreadManager stopped");
    }
    
    public Future<?> submitTask(Runnable task) {
        taskCounter.incrementAndGet();
        return executorService.submit(task);
    }
    
    public <T> Future<T> submitTask(Callable<T> task) {
        taskCounter.incrementAndGet();
        return executorService.submit(task);
    }
    
    // 提交带优先级的任务
    public Future<?> submitTask(Runnable task, int priority, String taskName) {
        taskCounter.incrementAndGet();
        return executorService.submit(new PrioritizedTask(task, priority, taskName));
    }
    
    // 提交带优先级的任务（默认名称）
    public Future<?> submitTask(Runnable task, int priority) {
        return submitTask(task, priority, "unnamed");
    }
    
    // 资源预留：为关键任务预留资源
    public Future<?> submitCriticalTask(Runnable task) {
        taskCounter.incrementAndGet();
        // 确保至少有一个线程可用
        ensureMinimumThreads(1);
        // 关键任务使用最高优先级
        return executorService.submit(new PrioritizedTask(task, 10, "critical_task"));
    }
    
    // 提交高优先级任务
    public Future<?> submitHighPriorityTask(Runnable task, String taskName) {
        taskCounter.incrementAndGet();
        return executorService.submit(new PrioritizedTask(task, 8, taskName));
    }
    
    // 提交低优先级任务
    public Future<?> submitLowPriorityTask(Runnable task, String taskName) {
        taskCounter.incrementAndGet();
        return executorService.submit(new PrioritizedTask(task, 2, taskName));
    }
    
    // 自适应线程池监控
    private void startAdaptiveThreadPoolMonitor() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                int activeThreads = executorService.getActiveCount();
                int queueSize = taskQueue.size();
                int currentPoolSize = executorService.getPoolSize();
                
                // 计算当前负载
                double load = currentPoolSize > 0 ? (double) activeThreads / currentPoolSize : 0;
                
                // 计算队列压力
                double queuePressure = queueSize > 0 ? (double) queueSize / (currentPoolSize * 5) : 0; // 假设每个线程可以处理5个任务
                
                // 智能调整线程池大小
                if ((load > 0.8 || queuePressure > 1.0) && currentPoolSize < maxPoolSize) {
                    // 负载较高或队列压力大，增加线程
                    int newPoolSize = Math.min(currentPoolSize + Math.max(1, (int) Math.ceil(queuePressure)), maxPoolSize);
                    executorService.setCorePoolSize(newPoolSize);
                    MCPSMod.LOGGER.info("Increased thread pool size to " + newPoolSize + " due to high load: " + load + ", queue pressure: " + queuePressure);
                } else if (load < 0.3 && currentPoolSize > corePoolSize) {
                    // 负载较低，减少线程
                    int newPoolSize = Math.max(currentPoolSize - 1, corePoolSize);
                    executorService.setCorePoolSize(newPoolSize);
                    MCPSMod.LOGGER.info("Decreased thread pool size to " + newPoolSize + " due to low load: " + load);
                }
                
                // 检查能源管理模式
                if (energySavingMode.get() && load < 0.2) {
                    // 低负载时，进一步减少线程
                    int newPoolSize = Math.max(corePoolSize / 2, 2);
                    executorService.setCorePoolSize(newPoolSize);
                    MCPSMod.LOGGER.info("Energy saving mode: reduced thread pool size to " + newPoolSize);
                }
                
                // 输出线程利用率
                printThreadUsageStats();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in adaptive thread pool monitor: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    // 打印线程使用统计信息
    private void printThreadUsageStats() {
        if (threadUsageData.isEmpty()) {
            return;
        }
        
        StringBuilder stats = new StringBuilder("Thread usage stats:");
        double totalUtilization = 0;
        int threadCount = 0;
        
        for (Map.Entry<String, ThreadUsageData> entry : threadUsageData.entrySet()) {
            String threadName = entry.getKey();
            ThreadUsageData data = entry.getValue();
            long totalTime = data.getTotalExecutionTime();
            long count = data.getExecutionCount();
            double avgTime = data.getAverageExecutionTime();
            
            stats.append(" \"").append(threadName).append("\": ");
            stats.append("executions: " + count + ", ");
            stats.append("avg time: " + String.format("%.2f", avgTime) + "ms, ");
            stats.append("total time: " + totalTime + "ms");
            
            totalUtilization += totalTime;
            threadCount++;
        }
        
        if (threadCount > 0) {
            double avgUtilization = totalUtilization / threadCount;
            stats.append(" | Avg utilization per thread: " + String.format("%.2f", avgUtilization) + "ms");
        }
        
        stats.append(" | Total tasks executed: " + totalTasksExecuted.get());
        stats.append(" | Total execution time: " + totalTaskExecutionTime.get() + "ms");
        
        MCPSMod.LOGGER.debug(stats.toString());
    }
    
    // 能源管理监控
    private void startEnergyManagementMonitor() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                int activeThreads = executorService.getActiveCount();
                int queueSize = taskQueue.size();
                
                // 检测系统负载
                if (activeThreads == 0 && queueSize == 0) {
                    // 系统空闲，启用能源管理模式
                    if (!energySavingMode.get()) {
                        energySavingMode.set(true);
                        // 减少线程池大小
                        executorService.setCorePoolSize(Math.max(corePoolSize / 2, 2));
                        MCPSMod.LOGGER.info("System idle, enabled energy saving mode");
                    }
                } else {
                    // 系统有负载，禁用能源管理模式
                    if (energySavingMode.get()) {
                        energySavingMode.set(false);
                        // 恢复线程池大小
                        executorService.setCorePoolSize(corePoolSize);
                        MCPSMod.LOGGER.info("System active, disabled energy saving mode");
                    }
                }
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in energy management monitor: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    // 确保最小线程数
    private void ensureMinimumThreads(int minThreads) {
        int currentPoolSize = executorService.getPoolSize();
        if (currentPoolSize < minThreads) {
            executorService.setCorePoolSize(minThreads);
            MCPSMod.LOGGER.info("Ensured minimum threads: " + minThreads);
        }
    }
    
    // 热插拔支持：动态添加线程
    public void addThreads(int count) {
        int currentPoolSize = executorService.getPoolSize();
        int newPoolSize = Math.min(currentPoolSize + count, maxPoolSize);
        executorService.setCorePoolSize(newPoolSize);
        MCPSMod.LOGGER.info("Added " + (newPoolSize - currentPoolSize) + " threads, new pool size: " + newPoolSize);
    }
    
    // 热插拔支持：动态移除线程
    public void removeThreads(int count) {
        int currentPoolSize = executorService.getPoolSize();
        int newPoolSize = Math.max(currentPoolSize - count, corePoolSize);
        executorService.setCorePoolSize(newPoolSize);
        MCPSMod.LOGGER.info("Removed " + (currentPoolSize - newPoolSize) + " threads, new pool size: " + newPoolSize);
    }
    
    // 获取当前线程池大小
    public int getCurrentPoolSize() {
        return executorService.getPoolSize();
    }
    
    // 获取核心线程池大小
    public int getCorePoolSize() {
        return corePoolSize;
    }
    
    // 获取最大线程池大小
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    // 获取活跃任务数
    public int getActiveTaskCount() {
        return taskCounter.get();
    }
    
    // 获取队列大小
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    // 获取能源管理模式状态
    public boolean isEnergySavingMode() {
        return energySavingMode.get();
    }
    
    // 手动启用/禁用能源管理模式
    public void setEnergySavingMode(boolean enabled) {
        energySavingMode.set(enabled);
        if (enabled) {
            // 启用能源管理模式，减少线程池大小
            executorService.setCorePoolSize(Math.max(corePoolSize / 2, 2));
            MCPSMod.LOGGER.info("Manually enabled energy saving mode");
        } else {
            // 禁用能源管理模式，恢复线程池大小
            executorService.setCorePoolSize(corePoolSize);
            MCPSMod.LOGGER.info("Manually disabled energy saving mode");
        }
    }
    
    // 获取线程使用情况
    public Map<String, ThreadUsageData> getThreadUsageData() {
        return threadUsageData;
    }
    
    // 获取总任务执行时间
    public long getTotalTaskExecutionTime() {
        return totalTaskExecutionTime.get();
    }
    
    // 获取总任务执行次数
    public long getTotalTasksExecuted() {
        return totalTasksExecuted.get();
    }
    
    // 获取线程池状态
    public Map<String, Object> getThreadPoolStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("corePoolSize", corePoolSize);
        status.put("maxPoolSize", maxPoolSize);
        status.put("currentPoolSize", executorService.getPoolSize());
        status.put("activeThreads", executorService.getActiveCount());
        status.put("queueSize", taskQueue.size());
        status.put("completedTasks", executorService.getCompletedTaskCount());
        status.put("totalTasksExecuted", totalTasksExecuted.get());
        status.put("totalExecutionTime", totalTaskExecutionTime.get());
        status.put("energySavingMode", energySavingMode.get());
        return status;
    }
    
    // 优化线程创建和销毁
    public void optimizeThreadManagement() {
        int currentPoolSize = executorService.getPoolSize();
        int activeThreads = executorService.getActiveCount();
        int queueSize = taskQueue.size();
        
        // 基于当前负载和队列大小优化线程池
        if (activeThreads == 0 && queueSize == 0 && currentPoolSize > corePoolSize / 2) {
            // 系统空闲，减少线程池大小
            int newPoolSize = Math.max(corePoolSize / 2, 2);
            executorService.setCorePoolSize(newPoolSize);
            MCPSMod.LOGGER.info("Optimized thread management: reduced pool size to " + newPoolSize + " due to idle system");
        } else if (activeThreads == currentPoolSize && queueSize > currentPoolSize * 2) {
            // 系统繁忙，增加线程池大小
            int newPoolSize = Math.min(currentPoolSize + 2, maxPoolSize);
            executorService.setCorePoolSize(newPoolSize);
            MCPSMod.LOGGER.info("Optimized thread management: increased pool size to " + newPoolSize + " due to high demand");
        }
    }
    
    // 获取线程池利用率
    public double getThreadPoolUtilization() {
        int currentPoolSize = executorService.getPoolSize();
        if (currentPoolSize == 0) {
            return 0.0;
        }
        int activeThreads = executorService.getActiveCount();
        return (double) activeThreads / currentPoolSize;
    }
    
    // 优先级任务类
    public static class PrioritizedTask implements Runnable, Comparable<PrioritizedTask> {
        private final Runnable task;
        private final int priority;
        private final String name;
        
        public PrioritizedTask(Runnable task, int priority, String name) {
            this.task = task;
            this.priority = priority;
            this.name = name;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                task.run();
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                totalTaskExecutionTime.addAndGet(duration);
            }
        }
        
        @Override
        public int compareTo(PrioritizedTask other) {
            return Integer.compare(other.priority, this.priority);
        }
    }
    
    // 线程使用数据类
    private static class ThreadUsageData {
        private AtomicLong totalExecutionTime;
        private AtomicLong executionCount;
        private AtomicLong lastExecutionStart;
        
        public ThreadUsageData() {
            totalExecutionTime = new AtomicLong(0);
            executionCount = new AtomicLong(0);
            lastExecutionStart = new AtomicLong(0);
        }
        
        public void startExecution() {
            lastExecutionStart.set(System.currentTimeMillis());
        }
        
        public void endExecution() {
            long duration = System.currentTimeMillis() - lastExecutionStart.get();
            totalExecutionTime.addAndGet(duration);
            executionCount.incrementAndGet();
        }
        
        public long getTotalExecutionTime() {
            return totalExecutionTime.get();
        }
        
        public long getExecutionCount() {
            return executionCount.get();
        }
        
        public double getAverageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0;
        }
    }
    
    private class WorkerThread implements Runnable {
        private final int id;
        
        public WorkerThread(int id) {
            this.id = id;
        }
        
        @Override
        public void run() {
            Thread.currentThread().setName("MCPS-Worker-" + id);
            MCPSMod.LOGGER.info("Worker thread " + id + " started");
            
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // 这里可以添加具体的工作任务处理逻辑
                    Thread.sleep(100); // 临时占位
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            MCPSMod.LOGGER.info("Worker thread " + id + " stopped");
        }
    }
}