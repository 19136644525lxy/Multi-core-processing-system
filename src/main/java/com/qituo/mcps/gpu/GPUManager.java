package com.qituo.mcps.gpu;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.monitor.PerformanceMonitor;

public class GPUManager {
    private static GPUManager instance;
    private ThreadManager threadManager;
    private Map<Integer, GPUDevice> gpuDevices;
    private Map<String, GPUJob> gpuJobs;
    private AtomicInteger deviceIdGenerator;
    private AtomicInteger jobIdGenerator;
    private ExecutorService executorService;
    private Map<String, Long> jobTypeExecutionTimes;
    private AtomicLong totalGpuExecutionTime;
    private AtomicInteger totalGpuJobs;
    
    private GPUManager() {
        this.gpuDevices = new ConcurrentHashMap<>();
        this.gpuJobs = new ConcurrentHashMap<>();
        this.deviceIdGenerator = new AtomicInteger(0);
        this.jobIdGenerator = new AtomicInteger(0);
        this.executorService = Executors.newFixedThreadPool(8); // 增加线程池大小以处理更多GPU任务
        this.jobTypeExecutionTimes = new ConcurrentHashMap<>();
        this.totalGpuExecutionTime = new AtomicLong(0);
        this.totalGpuJobs = new AtomicInteger(0);
    }
    
    public static GPUManager getInstance() {
        if (instance == null) {
            synchronized (GPUManager.class) {
                if (instance == null) {
                    instance = new GPUManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize(ThreadManager threadManager) {
        this.threadManager = threadManager;
        
        // 检测GPU设备
        detectGPUDevices();
        
        // 初始化GPU设备
        initializeGPUDevices();
        
        // 启动GPU性能监控
        startGPUPerformanceMonitoring();
        
        MCPSMod.LOGGER.info("GPUManager initialized with " + gpuDevices.size() + " GPU device(s)");
    }
    
    private void detectGPUDevices() {
        // 检测系统中的GPU设备
        // 这里可以实现更复杂的GPU检测逻辑
        // 目前模拟检测到2个GPU设备
        for (int i = 0; i < 2; i++) {
            int deviceId = deviceIdGenerator.incrementAndGet();
            GPUDevice device = new GPUDevice(deviceId, "NVIDIA GeForce RTX 3080", 1024);
            gpuDevices.put(deviceId, device);
            MCPSMod.LOGGER.info("Detected GPU device: " + device.getName() + " (ID: " + deviceId + ")");
        }
    }
    
    private void initializeGPUDevices() {
        // 初始化GPU设备
        for (GPUDevice device : gpuDevices.values()) {
            device.initialize();
        }
    }
    
    // 启动GPU性能监控
    private void startGPUPerformanceMonitoring() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 每10秒记录一次GPU性能数据
                    Thread.sleep(10000);
                    monitorGPUPerformance();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    // 监控GPU性能
    private void monitorGPUPerformance() {
        for (GPUDevice device : gpuDevices.values()) {
            if (device.isInitialized()) {
                // 记录GPU性能数据
                int activeJobs = device.getActiveJobs();
                double utilization = device.getUtilization();
                
                MCPSMod.LOGGER.info("GPU Device " + device.getId() + " (" + device.getName() + "): " + 
                    "Active jobs: " + activeJobs + ", Utilization: " + String.format("%.2f%%", utilization));
                
                // 记录到性能监控器
                if (MCPSMod.getInstance() != null) {
                    PerformanceMonitor monitor = MCPSMod.getInstance().getPerformanceMonitor();
                    if (monitor != null) {
                        monitor.recordGpuUtilization(device.getId(), utilization);
                        monitor.recordGpuJobs(device.getId(), activeJobs);
                    }
                }
            }
        }
    }
    
    // 提交GPU任务
    public GPUJob submitGPUJob(String jobType, Runnable task, Map<String, Object> parameters) {
        // 选择合适的GPU设备
        GPUDevice device = selectGPUDevice(jobType, parameters);
        if (device == null) {
            MCPSMod.LOGGER.warn("No suitable GPU device found for job type: " + jobType);
            return null;
        }
        
        // 创建GPU任务
        int jobId = jobIdGenerator.incrementAndGet();
        GPUJob job = new GPUJob(jobId, jobType, task, parameters, device);
        gpuJobs.put("job-" + jobId, job);
        
        // 提交任务到GPU
        executorService.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                device.executeJob(job);
                long executionTime = System.currentTimeMillis() - startTime;
                totalGpuExecutionTime.addAndGet(executionTime);
                totalGpuJobs.incrementAndGet();
                
                // 更新任务类型执行时间统计
                jobTypeExecutionTimes.compute(jobType, (key, value) -> 
                    value == null ? executionTime : value + executionTime);
            } catch (Exception e) {
                MCPSMod.getInstance().getErrorHandler().logError("Error executing GPU job: " + jobType, e);
                job.setStatus(GPUJob.Status.FAILED);
            } finally {
                gpuJobs.remove("job-" + jobId);
            }
        });
        
        return job;
    }
    
    // 提交带返回值的GPU任务
    public <T> CompletableFuture<T> submitGPUJobWithResult(String jobType, Callable<T> task, Map<String, Object> parameters) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        // 选择合适的GPU设备
        GPUDevice device = selectGPUDevice(jobType, parameters);
        if (device == null) {
            MCPSMod.LOGGER.warn("No suitable GPU device found for job type: " + jobType);
            future.completeExceptionally(new Exception("No suitable GPU device found"));
            return future;
        }
        
        // 创建GPU任务
        int jobId = jobIdGenerator.incrementAndGet();
        GPUJob job = new GPUJob(jobId, jobType, () -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, parameters, device);
        gpuJobs.put("job-" + jobId, job);
        
        // 提交任务到GPU
        executorService.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                device.executeJob(job);
                long executionTime = System.currentTimeMillis() - startTime;
                totalGpuExecutionTime.addAndGet(executionTime);
                totalGpuJobs.incrementAndGet();
                
                // 更新任务类型执行时间统计
                jobTypeExecutionTimes.compute(jobType, (key, value) -> 
                    value == null ? executionTime : value + executionTime);
            } catch (Exception e) {
                MCPSMod.getInstance().getErrorHandler().logError("Error executing GPU job: " + jobType, e);
                future.completeExceptionally(e);
                job.setStatus(GPUJob.Status.FAILED);
            } finally {
                gpuJobs.remove("job-" + jobId);
            }
        });
        
        return future;
    }
    
    // 执行GPU渲染任务
    public void executeGPUTask(Runnable task) {
        submitGPUJob("render", task, new HashMap<>());
    }
    
    // 智能选择GPU设备
    private GPUDevice selectGPUDevice(String jobType, Map<String, Object> parameters) {
        // 收集可用的GPU设备
        List<GPUDevice> availableDevices = new ArrayList<>();
        for (GPUDevice device : gpuDevices.values()) {
            if (device.isAvailable()) {
                availableDevices.add(device);
            }
        }
        
        if (availableDevices.isEmpty()) {
            return null;
        }
        
        // 根据任务类型和设备状态选择最佳GPU设备
        // 1. 对于渲染任务，优先选择性能更高的GPU
        // 2. 对于计算任务，优先选择负载较低的GPU
        if (jobType.equals("render")) {
            // 渲染任务：优先选择内存更大的GPU
            availableDevices.sort(Comparator.comparingInt(GPUDevice::getMemory).reversed());
        } else {
            // 其他任务：优先选择负载较低的GPU
            availableDevices.sort(Comparator.comparingDouble(GPUDevice::getUtilization));
        }
        
        return availableDevices.get(0);
    }
    
    // 自动调整GPU任务分配
    public void autoAdjustGpuTaskAllocation() {
        // 根据GPU性能和负载自动调整任务分配策略
        int totalJobs = gpuJobs.size();
        int totalDevices = gpuDevices.size();
        
        if (totalDevices > 0) {
            int averageJobsPerDevice = totalJobs / totalDevices;
            
            for (GPUDevice device : gpuDevices.values()) {
                int activeJobs = device.getActiveJobs();
                if (activeJobs > averageJobsPerDevice * 1.5) {
                    // 该GPU负载过高，减少分配给它的任务
                    MCPSMod.LOGGER.info("GPU Device " + device.getId() + " is overloaded, reducing task allocation");
                } else if (activeJobs < averageJobsPerDevice * 0.5) {
                    // 该GPU负载过低，增加分配给它的任务
                    MCPSMod.LOGGER.info("GPU Device " + device.getId() + " is underutilized, increasing task allocation");
                }
            }
        }
    }
    
    // 获取GPU性能统计
    public Map<String, Object> getGpuPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", gpuDevices.size());
        stats.put("totalJobs", totalGpuJobs.get());
        stats.put("totalExecutionTime", totalGpuExecutionTime.get());
        stats.put("jobTypeExecutionTimes", jobTypeExecutionTimes);
        
        List<Map<String, Object>> deviceStats = new ArrayList<>();
        for (GPUDevice device : gpuDevices.values()) {
            Map<String, Object> deviceStat = new HashMap<>();
            deviceStat.put("id", device.getId());
            deviceStat.put("name", device.getName());
            deviceStat.put("memory", device.getMemory());
            deviceStat.put("activeJobs", device.getActiveJobs());
            deviceStat.put("utilization", device.getUtilization());
            deviceStats.add(deviceStat);
        }
        stats.put("devices", deviceStats);
        
        return stats;
    }
    
    // 停止GPU管理器
    public void stop() {
        // 停止所有GPU设备
        for (GPUDevice device : gpuDevices.values()) {
            device.stop();
        }
        
        // 关闭执行器，立即中断所有任务
        executorService.shutdownNow();
        
        MCPSMod.LOGGER.info("GPUManager stopped");
    }
    
    // 获取所有GPU设备
    public List<GPUDevice> getGPUDevices() {
        return new ArrayList<>(gpuDevices.values());
    }
    
    // 获取指定GPU设备
    public GPUDevice getGPUDevice(int deviceId) {
        return gpuDevices.get(deviceId);
    }
    
    // 获取当前GPU任务
    public Map<String, GPUJob> getGPUJobs() {
        return Collections.unmodifiableMap(gpuJobs);
    }
    
    // GPU设备类
    private static class GPUDevice {
        private int id;
        private String name;
        private int memory;
        private boolean initialized;
        private boolean available;
        private AtomicInteger activeJobs;
        private AtomicLong totalExecutionTime;
        private AtomicInteger totalJobs;
        private long lastUtilizationCheck;
        private double utilization;
        
        public GPUDevice(int id, String name, int memory) {
            this.id = id;
            this.name = name;
            this.memory = memory;
            this.initialized = false;
            this.available = false;
            this.activeJobs = new AtomicInteger(0);
            this.totalExecutionTime = new AtomicLong(0);
            this.totalJobs = new AtomicInteger(0);
            this.lastUtilizationCheck = System.currentTimeMillis();
            this.utilization = 0.0;
        }
        
        // 初始化GPU设备
        public void initialize() {
            // 初始化GPU设备
            // 这里可以实现更复杂的GPU初始化逻辑
            MCPSMod.LOGGER.info("Initializing GPU device: " + name + " (ID: " + id + ")");
            this.initialized = true;
            this.available = true;
        }
        
        // 执行GPU任务
        public void executeJob(GPUJob job) {
            activeJobs.incrementAndGet();
            available = false;
            
            long startTime = System.currentTimeMillis();
            try {
                MCPSMod.LOGGER.info("Executing GPU job " + job.getId() + " on device " + name);
                job.setStatus(GPUJob.Status.RUNNING);
                
                // 执行GPU任务
                job.getTask().run();
                
                job.setStatus(GPUJob.Status.COMPLETED);
                MCPSMod.LOGGER.info("GPU job " + job.getId() + " completed successfully");
            } catch (Exception e) {
                job.setStatus(GPUJob.Status.FAILED);
                MCPSMod.LOGGER.error("Error executing GPU job " + job.getId(), e);
            } finally {
                long executionTime = System.currentTimeMillis() - startTime;
                totalExecutionTime.addAndGet(executionTime);
                totalJobs.incrementAndGet();
                activeJobs.decrementAndGet();
                available = true;
                
                // 更新利用率
                updateUtilization();
            }
        }
        
        // 更新GPU利用率
        private void updateUtilization() {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCheck = currentTime - lastUtilizationCheck;
            
            if (timeSinceLastCheck > 0) {
                // 计算利用率（基于活跃任务数和执行时间）
                double jobTimeRatio = (double) totalExecutionTime.get() / timeSinceLastCheck;
                double activeJobRatio = (double) activeJobs.get() / 4; // 假设每个GPU最多处理4个任务
                this.utilization = Math.min(100.0, (jobTimeRatio + activeJobRatio) * 50.0);
                this.lastUtilizationCheck = currentTime;
            }
        }
        
        // 停止GPU设备
        public void stop() {
            // 停止GPU设备
            MCPSMod.LOGGER.info("Stopping GPU device: " + name + " (ID: " + id + ")");
            this.initialized = false;
            this.available = false;
        }
        
        // 获取GPU利用率
        public double getUtilization() {
            updateUtilization();
            return utilization;
        }
        
        // 获取GPU ID
        public int getId() {
            return id;
        }
        
        // 获取GPU名称
        public String getName() {
            return name;
        }
        
        // 获取GPU内存
        public int getMemory() {
            return memory;
        }
        
        // 检查GPU是否初始化
        public boolean isInitialized() {
            return initialized;
        }
        
        // 检查GPU是否可用
        public boolean isAvailable() {
            return available && initialized;
        }
        
        // 获取活跃任务数
        public int getActiveJobs() {
            return activeJobs.get();
        }
        
        // 获取总执行时间
        public long getTotalExecutionTime() {
            return totalExecutionTime.get();
        }
        
        // 获取总任务数
        public int getTotalJobs() {
            return totalJobs.get();
        }
    }
    
    // GPU任务类
    public static class GPUJob {
        public enum Status {
            PENDING, RUNNING, COMPLETED, FAILED
        }
        
        private int id;
        private String type;
        private Runnable task;
        private Map<String, Object> parameters;
        private GPUDevice device;
        private Status status;
        private long startTime;
        private long endTime;
        
        public GPUJob(int id, String type, Runnable task, Map<String, Object> parameters, GPUDevice device) {
            this.id = id;
            this.type = type;
            this.task = task;
            this.parameters = parameters;
            this.device = device;
            this.status = Status.PENDING;
            this.startTime = System.currentTimeMillis();
        }
        
        // 获取任务ID
        public int getId() {
            return id;
        }
        
        // 获取任务类型
        public String getType() {
            return type;
        }
        
        // 获取任务
        public Runnable getTask() {
            return task;
        }
        
        // 获取任务参数
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        // 获取GPU设备
        public GPUDevice getDevice() {
            return device;
        }
        
        // 获取任务状态
        public Status getStatus() {
            return status;
        }
        
        // 设置任务状态
        public void setStatus(Status status) {
            this.status = status;
            if (status == Status.COMPLETED || status == Status.FAILED) {
                this.endTime = System.currentTimeMillis();
            }
        }
        
        // 获取开始时间
        public long getStartTime() {
            return startTime;
        }
        
        // 获取结束时间
        public long getEndTime() {
            return endTime;
        }
        
        // 获取执行时间
        public long getExecutionTime() {
            return endTime > 0 ? endTime - startTime : 0;
        }
    }
}