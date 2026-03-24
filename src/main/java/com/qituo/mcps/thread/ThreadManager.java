package com.qituo.mcps.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import com.qituo.mcps.core.MCPSMod;

public class ThreadManager {
    private ThreadPoolExecutor executorService;
    private int corePoolSize;
    private int maxPoolSize;
    private List<WorkerThread> workerThreads;
    private BlockingQueue<Runnable> taskQueue;
    private AtomicInteger taskCounter;
    private AtomicBoolean energySavingMode;
    private ScheduledExecutorService monitorExecutor;
    
    public void initialize() {
        // 根据系统CPU核心数确定线程池大小
        corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        maxPoolSize = corePoolSize * 2;
        taskQueue = new LinkedBlockingQueue<>();
        workerThreads = new ArrayList<>();
        taskCounter = new AtomicInteger(0);
        energySavingMode = new AtomicBoolean(false);
        
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
        );
        
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
    
    // 资源预留：为关键任务预留资源
    public Future<?> submitCriticalTask(Runnable task) {
        taskCounter.incrementAndGet();
        // 确保至少有一个线程可用
        ensureMinimumThreads(1);
        return executorService.submit(task);
    }
    
    // 自适应线程池监控
    private void startAdaptiveThreadPoolMonitor() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                int activeThreads = executorService.getActiveCount();
                int queueSize = taskQueue.size();
                int currentPoolSize = executorService.getPoolSize();
                
                // 计算当前负载
                double load = (double) activeThreads / currentPoolSize;
                
                // 自适应调整线程池大小
                if (load > 0.8 && currentPoolSize < maxPoolSize) {
                    // 负载较高，增加线程
                    int newPoolSize = Math.min(currentPoolSize + 2, maxPoolSize);
                    executorService.setCorePoolSize(newPoolSize);
                    MCPSMod.LOGGER.info("Increased thread pool size to " + newPoolSize + " due to high load: " + load);
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
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in adaptive thread pool monitor: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
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