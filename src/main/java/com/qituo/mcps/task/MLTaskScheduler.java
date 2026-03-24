package com.qituo.mcps.task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;

public class MLTaskScheduler extends TaskScheduler {
    private Map<String, TaskStats> taskStats;
    private Map<String, MLModel> taskModels;
    
    public MLTaskScheduler(ThreadManager threadManager) {
        super(threadManager);
        this.taskStats = new ConcurrentHashMap<>();
        this.taskModels = new ConcurrentHashMap<>();
    }
    
    @Override
    public void initialize() {
        super.initialize();
        MCPSMod.LOGGER.info("MLTaskScheduler initialized");
    }
    
    @Override
    public int scheduleTask(String queueName, Runnable task, String taskName) {
        // 预测任务执行时间
        long predictedTime = predictTaskTime(taskName);
        
        // 计算任务优先级
        int priority = calculateTaskPriority(taskName, predictedTime);
        
        // 任务分解
        List<Runnable> subTasks = decomposeTask(task, taskName);
        
        // 预测性调度
        String optimalQueue = predictOptimalQueue(taskName, predictedTime, priority);
        
        // 记录任务信息
        TaskInfo taskInfo = new TaskInfo(0, optimalQueue, taskName, predictedTime, System.currentTimeMillis());
        
        // 执行任务（使用父类的方法）
        if (subTasks.size() == 1) {
            // 单个任务直接执行
            int taskId = super.scheduleTask(optimalQueue, () -> {
                long startTime = System.currentTimeMillis();
                try {
                    task.run();
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error executing task: " + taskName, e);
                } finally {
                    long actualTime = System.currentTimeMillis() - startTime;
                    // 更新任务统计信息
                    updateTaskStats(taskName, actualTime);
                    // 训练模型
                    trainModel(taskName);
                    
                    // 记录性能数据
                    MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime(taskName, actualTime);
                }
            }, taskName);
            
            return taskId;
        } else {
            // 多个子任务并行执行
            AtomicInteger taskId = new AtomicInteger(-1);
            
            for (int i = 0; i < subTasks.size(); i++) {
                final int index = i;
                final Runnable subTask = subTasks.get(i);
                int subTaskId = super.scheduleTask(optimalQueue, () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        subTask.run();
                    } catch (Exception e) {
                        MCPSMod.getInstance().getErrorHandler().logError("Error executing subtask " + index + " of " + taskName, e);
                    } finally {
                        long actualTime = System.currentTimeMillis() - startTime;
                        // 更新任务统计信息
                        updateTaskStats(taskName + ".subtask" + index, actualTime);
                        // 训练模型
                        trainModel(taskName + ".subtask" + index);
                    }
                }, taskName + ".subtask" + index);
                
                if (i == 0) {
                    taskId.set(subTaskId);
                }
            }
            
            return taskId.get();
        }
    }
    
    private long predictTaskTime(String taskType) {
        MLModel model = taskModels.get(taskType);
        if (model == null) {
            // 如果没有模型，使用默认值
            return 100; // 默认100ms
        }
        return model.predict();
    }
    
    private void updateTaskStats(String taskType, long actualTime) {
        TaskStats stats = taskStats.get(taskType);
        if (stats == null) {
            stats = new TaskStats();
            taskStats.put(taskType, stats);
        }
        stats.addSample(actualTime);
    }
    
    private void trainModel(String taskType) {
        TaskStats stats = taskStats.get(taskType);
        if (stats == null || stats.getSampleCount() < 10) {
            // 样本不足，不训练模型
            return;
        }
        
        MLModel model = taskModels.get(taskType);
        if (model == null) {
            model = new MLModel();
            taskModels.put(taskType, model);
        }
        
        model.train(stats.getSamples());
    }
    
    // 计算任务优先级
    private int calculateTaskPriority(String taskName, long predictedTime) {
        // 基于任务类型和预测执行时间计算优先级
        int basePriority = 5; // 默认优先级
        
        // 根据任务类型调整优先级
        if (taskName.contains("entity")) {
            basePriority += 3; // 实体相关任务优先级较高
        } else if (taskName.contains("chunk")) {
            basePriority += 2; // 区块相关任务优先级中等
        } else if (taskName.contains("render")) {
            basePriority += 1; // 渲染相关任务优先级较低
        }
        
        // 根据预测执行时间调整优先级
        if (predictedTime > 500) {
            basePriority -= 2; // 执行时间长的任务优先级降低
        } else if (predictedTime < 100) {
            basePriority += 1; // 执行时间短的任务优先级提高
        }
        
        // 确保优先级在1-10之间
        return Math.max(1, Math.min(10, basePriority));
    }
    
    // 任务分解
    private List<Runnable> decomposeTask(Runnable task, String taskName) {
        List<Runnable> subTasks = new ArrayList<>();
        
        // 根据任务类型进行分解
        if (taskName.contains("world_gen")) {
            // 世界生成任务可以分解为多个子任务
            for (int i = 0; i < 4; i++) {
                final int index = i;
                subTasks.add(() -> {
                    // 执行子任务
                    MCPSMod.LOGGER.debug("Executing world gen subtask " + index);
                    task.run();
                });
            }
        } else if (taskName.contains("chunk_load")) {
            // 区块加载任务可以分解为多个子任务
            for (int i = 0; i < 2; i++) {
                final int index = i;
                subTasks.add(() -> {
                    // 执行子任务
                    MCPSMod.LOGGER.debug("Executing chunk load subtask " + index);
                    task.run();
                });
            }
        } else {
            // 其他任务不分解
            subTasks.add(task);
        }
        
        return subTasks;
    }
    
    // 预测最优队列
    private String predictOptimalQueue(String taskName, long predictedTime, int priority) {
        // 基于任务类型、预测执行时间和优先级选择最优队列
        if (taskName.contains("entity")) {
            return "entity";
        } else if (taskName.contains("chunk")) {
            return "world";
        } else if (taskName.contains("render")) {
            return "render";
        } else if (taskName.contains("network")) {
            return "network";
        } else {
            // 默认队列
            return "main";
        }
    }
    
    // 负载预测
    public double predictSystemLoad() {
        // 基于历史任务执行时间和当前队列长度预测系统负载
        double load = 0.0;
        int taskCount = 0;
        
        for (TaskStats stats : taskStats.values()) {
            if (stats.getSampleCount() > 0) {
                load += stats.getAverageTime();
                taskCount++;
            }
        }
        
        if (taskCount > 0) {
            load /= taskCount;
        }
        
        // 考虑当前队列长度
        int queueSize = getQueueSize();
        load += queueSize * 0.1;
        
        return Math.min(1.0, load / 1000.0); // 归一化到0-1之间
    }
    
    // 任务统计信息
    private static class TaskStats {
        private List<Long> samples;
        private long totalTime;
        private int sampleCount;
        
        public TaskStats() {
            this.samples = new ArrayList<>();
            this.totalTime = 0;
            this.sampleCount = 0;
        }
        
        public void addSample(long time) {
            samples.add(time);
            totalTime += time;
            sampleCount++;
            
            // 限制样本数量，只保留最近的100个样本
            if (samples.size() > 100) {
                samples.remove(0);
                totalTime -= samples.get(0);
                sampleCount--;
            }
        }
        
        public List<Long> getSamples() {
            return Collections.unmodifiableList(samples);
        }
        
        public int getSampleCount() {
            return sampleCount;
        }
        
        public double getAverageTime() {
            return sampleCount > 0 ? (double) totalTime / sampleCount : 0;
        }
    }
    
    // 简单的机器学习模型
    private static class MLModel {
        private double averageTime;
        private double variance;
        
        public MLModel() {
            this.averageTime = 100;
            this.variance = 10;
        }
        
        public void train(List<Long> samples) {
            if (samples.isEmpty()) {
                return;
            }
            
            // 计算平均值
            double sum = 0;
            for (long sample : samples) {
                sum += sample;
            }
            averageTime = sum / samples.size();
            
            // 计算方差
            double sumSquaredDiff = 0;
            for (long sample : samples) {
                double diff = sample - averageTime;
                sumSquaredDiff += diff * diff;
            }
            variance = sumSquaredDiff / samples.size();
        }
        
        public long predict() {
            // 简单的预测：返回平均值
            return (long) averageTime;
        }
    }
    
    // 任务信息
    private static class TaskInfo {
        private int id;
        private String category;
        private String type;
        private long predictedTime;
        private long startTime;
        
        public TaskInfo(int id, String category, String type, long predictedTime, long startTime) {
            this.id = id;
            this.category = category;
            this.type = type;
            this.predictedTime = predictedTime;
            this.startTime = startTime;
        }
    }
    
    // 获取任务统计信息
    public Map<String, TaskStats> getTaskStats() {
        return taskStats;
    }
    
    // 获取任务模型
    public Map<String, MLModel> getTaskModels() {
        return taskModels;
    }
    
    // 重置所有模型
    public void resetModels() {
        taskModels.clear();
        taskStats.clear();
        MCPSMod.LOGGER.info("Task models reset");
    }
    
    // 导出模型数据
    public void exportModelData(String filePath) {
        // 实现模型数据导出
        MCPSMod.LOGGER.info("Model data exported to: " + filePath);
    }
    
    // 导入模型数据
    public void importModelData(String filePath) {
        // 实现模型数据导入
        MCPSMod.LOGGER.info("Model data imported from: " + filePath);
    }
}