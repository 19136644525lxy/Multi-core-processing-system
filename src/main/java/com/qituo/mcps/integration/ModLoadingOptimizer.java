package com.qituo.mcps.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.qituo.mcps.core.MCPSMod;

public class ModLoadingOptimizer {
    private static ModLoadingOptimizer instance;
    private ExecutorService executorService;
    private List<ModLoadingTask> loadingTasks;
    private List<ModLoadingTask> lazyLoadingTasks;
    
    private ModLoadingOptimizer() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.loadingTasks = new ArrayList<>();
        this.lazyLoadingTasks = new ArrayList<>();
    }
    
    public static ModLoadingOptimizer getInstance() {
        if (instance == null) {
            instance = new ModLoadingOptimizer();
        }
        return instance;
    }
    
    public void registerLoadingTask(ModLoadingTask task) {
        loadingTasks.add(task);
        MCPSMod.LOGGER.info("Registered loading task: " + task.getTaskName());
    }
    
    public void registerLazyLoadingTask(ModLoadingTask task) {
        lazyLoadingTasks.add(task);
        MCPSMod.LOGGER.info("Registered lazy loading task: " + task.getTaskName());
    }
    
    public void loadMods() {
        long startTime = System.currentTimeMillis();
        MCPSMod.LOGGER.info("Starting mod loading...");
        
        // 并行加载常规任务
        List<Future<?>> futures = new ArrayList<>();
        for (ModLoadingTask task : loadingTasks) {
            futures.add(executorService.submit(() -> {
                try {
                    task.execute();
                    MCPSMod.LOGGER.info("Completed loading task: " + task.getTaskName());
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Error executing loading task: " + task.getTaskName(), e);
                }
            }));
        }
        
        // 等待所有加载任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error waiting for loading task", e);
            }
        }
        
        long endTime = System.currentTimeMillis();
        MCPSMod.LOGGER.info("Mod loading completed in " + (endTime - startTime) + "ms");
    }
    
    public void loadLazyTasks() {
        MCPSMod.LOGGER.info("Starting lazy loading tasks...");
        
        for (ModLoadingTask task : lazyLoadingTasks) {
            executorService.submit(() -> {
                try {
                    task.execute();
                    MCPSMod.LOGGER.info("Completed lazy loading task: " + task.getTaskName());
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Error executing lazy loading task: " + task.getTaskName(), e);
                }
            });
        }
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    public interface ModLoadingTask {
        String getTaskName();
        void execute() throws Exception;
    }
}