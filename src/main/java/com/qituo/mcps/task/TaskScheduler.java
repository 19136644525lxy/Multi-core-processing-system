package com.qituo.mcps.task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;

public class TaskScheduler {
    private ThreadManager threadManager;
    private ConcurrentHashMap<String, TaskQueue> taskQueues;
    private AtomicInteger taskIdGenerator;
    
    public TaskScheduler(ThreadManager threadManager) {
        this.threadManager = threadManager;
        this.taskQueues = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicInteger(0);
    }
    
    public void initialize() {
        // 创建不同类型的任务队列
        createTaskQueue("world_generation");
        createTaskQueue("entity_ai");
        createTaskQueue("block_updates");
        createTaskQueue("redstone");
        createTaskQueue("pathfinding");
        
        MCPSMod.LOGGER.info("TaskScheduler initialized with " + taskQueues.size() + " task queues");
    }
    
    private void createTaskQueue(String name) {
        TaskQueue queue = new TaskQueue(name);
        taskQueues.put(name, queue);
        
        // 为每个队列启动处理线程
        threadManager.submitTask(() -> {
            processTaskQueue(queue);
        });
    }
    
    private void processTaskQueue(TaskQueue queue) {
        Thread.currentThread().setName("MCPS-Task-Processor-" + queue.getName());
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ScheduledTask task = queue.take();
                if (task != null) {
                    long startTime = System.currentTimeMillis();
                    
                    try {
                        task.execute();
                    } catch (Exception e) {
                        MCPSMod.LOGGER.error("Error executing task: " + task.getName(), e);
                    }
                    
                    long duration = System.currentTimeMillis() - startTime;
                    MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime(queue.getName(), duration);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public int scheduleTask(String queueName, Runnable task, String taskName) {
        return scheduleTask(queueName, task, taskName, 5); // 默认优先级5
    }
    
    public int scheduleTask(String queueName, Runnable task) {
        return scheduleTask(queueName, task, "unnamed", 5); // 默认优先级5
    }
    
    public int scheduleTask(String queueName, Runnable task, String taskName, int priority) {
        TaskQueue queue = taskQueues.get(queueName);
        if (queue == null) {
            MCPSMod.LOGGER.warn("Task queue not found: " + queueName);
            return -1;
        }
        
        int taskId = taskIdGenerator.incrementAndGet();
        ScheduledTask scheduledTask = new ScheduledTask(taskId, taskName, task, priority);
        queue.offer(scheduledTask);
        
        return taskId;
    }
    
    public <T> Future<T> scheduleTaskWithResult(String queueName, Callable<T> task, String taskName) {
        return scheduleTaskWithResult(queueName, task, taskName, 5); // 默认优先级5
    }
    
    public <T> Future<T> scheduleTaskWithResult(String queueName, Callable<T> task, String taskName, int priority) {
        TaskQueue queue = taskQueues.get(queueName);
        if (queue == null) {
            MCPSMod.LOGGER.warn("Task queue not found: " + queueName);
            return null;
        }
        
        int taskId = taskIdGenerator.incrementAndGet();
        CompletableFuture<T> future = new CompletableFuture<>();
        
        ScheduledTask scheduledTask = new ScheduledTask(taskId, taskName, () -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
                MCPSMod.LOGGER.error("Error executing task: " + taskName, e);
            }
        }, priority);
        
        queue.offer(scheduledTask);
        return future;
    }
    
    public int getQueueSize(String queueName) {
        TaskQueue queue = taskQueues.get(queueName);
        return queue != null ? queue.size() : 0;
    }
    
    public int getQueueSize() {
        int totalSize = 0;
        for (TaskQueue queue : taskQueues.values()) {
            totalSize += queue.size();
        }
        return totalSize;
    }
    
    private static class TaskQueue {
        private final String name;
        private final BlockingQueue<ScheduledTask> queue;
        
        public TaskQueue(String name) {
            this.name = name;
            // 使用优先级队列，按照任务优先级排序
            this.queue = new PriorityBlockingQueue<ScheduledTask>(11, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
        }
        
        public String getName() {
            return name;
        }
        
        public void offer(ScheduledTask task) {
            queue.offer(task);
        }
        
        public ScheduledTask take() throws InterruptedException {
            return queue.take();
        }
        
        public int size() {
            return queue.size();
        }
    }
    
    private static class ScheduledTask implements Comparable<ScheduledTask> {
        private final int id;
        private final String name;
        private final Runnable task;
        private final int priority;
        
        public ScheduledTask(int id, String name, Runnable task) {
            this(id, name, task, 5); // 默认优先级5
        }
        
        public ScheduledTask(int id, String name, Runnable task, int priority) {
            this.id = id;
            this.name = name;
            this.task = task;
            this.priority = priority;
        }
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public void execute() {
            task.run();
        }
        
        @Override
        public int compareTo(ScheduledTask other) {
            // 按优先级降序排序
            int priorityCompare = Integer.compare(other.priority, this.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 优先级相同时，按任务ID升序排序
            return Integer.compare(this.id, other.id);
        }
    }
}