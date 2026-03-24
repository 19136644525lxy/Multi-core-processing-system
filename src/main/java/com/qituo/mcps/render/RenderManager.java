package com.qituo.mcps.render;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.gpu.GPUManager;

public class RenderManager {
    private ExecutorService renderExecutor;
    private ConcurrentHashMap<String, RenderTask> renderTasks;
    private AtomicInteger activeRenderTasks;
    private AtomicLong lastFrameTime;
    private int viewDistance;
    private int maxViewDistance;
    private int minViewDistance;
    private boolean gpuAcceleration;
    private int renderThreads;
    private Map<String, Long> renderTaskTimes;
    private AtomicInteger frameCount;
    private AtomicLong totalFrameTime;
    private int renderQualityLevel; // 渲染质量级别 0-5
    
    public void initialize() {
        // 创建渲染线程池
        int processorCount = Runtime.getRuntime().availableProcessors();
        this.renderThreads = Math.max(2, processorCount / 2);
        this.renderExecutor = Executors.newFixedThreadPool(renderThreads);
        this.renderTasks = new ConcurrentHashMap<>();
        this.activeRenderTasks = new AtomicInteger(0);
        this.lastFrameTime = new AtomicLong(System.currentTimeMillis());
        this.viewDistance = 10; // 默认视距
        this.maxViewDistance = 32; // 最大视距
        this.minViewDistance = 2; // 最小视距
        this.gpuAcceleration = true; // 启用GPU加速
        this.renderTaskTimes = new ConcurrentHashMap<>();
        this.frameCount = new AtomicInteger(0);
        this.totalFrameTime = new AtomicLong(0);
        this.renderQualityLevel = 3; // 默认中等质量
        
        MCPSMod.LOGGER.info("RenderManager initialized with " + renderThreads + " render threads");
    }
    
    // 提交渲染任务
    public void submitRenderTask(String taskId, Runnable task) {
        submitRenderTask(taskId, task, 1);
    }
    
    // 提交渲染任务（带优先级）
    public void submitRenderTask(String taskId, Runnable task, int priority) {
        RenderTask renderTask = new RenderTask(taskId, task, priority);
        renderTasks.put(taskId, renderTask);
        activeRenderTasks.incrementAndGet();
        
        renderExecutor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // 检查是否启用GPU加速
                if (gpuAcceleration && MCPSMod.getInstance() != null) {
                    GPUManager gpuManager = MCPSMod.getInstance().getGpuManager();
                    if (gpuManager != null && gpuManager.getGPUDevices().size() > 0) {
                        // 使用GPU执行任务
                        Map<String, Object> parameters = new HashMap<>();
                        parameters.put("taskId", taskId);
                        gpuManager.submitGPUJob("render", task, parameters);
                    } else {
                        // 回退到CPU执行
                        task.run();
                    }
                } else {
                    // CPU执行
                    task.run();
                }
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error executing render task " + taskId + ": " + e.getMessage());
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                renderTaskTimes.put(taskId, duration);
                renderTasks.remove(taskId);
                activeRenderTasks.decrementAndGet();
            }
        });
    }
    
    // 分解并执行渲染帧任务
    public void renderFrame() {
        long startTime = System.currentTimeMillis();
        frameCount.incrementAndGet();
        
        // 分解渲染任务
        List<Runnable> frameTasks = decomposeFrameTasks();
        
        // 并行执行渲染任务
        frameTasks.parallelStream().forEach(task -> {
            try {
                task.run();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in frame task: " + e.getMessage());
            }
        });
        
        // 同步帧
        syncFrame();
        
        long frameTime = System.currentTimeMillis() - startTime;
        totalFrameTime.addAndGet(frameTime);
        
        // 每100帧记录一次平均帧率
        if (frameCount.get() % 100 == 0) {
            long avgFrameTime = totalFrameTime.get() / frameCount.get();
            double fps = 1000.0 / avgFrameTime;
            MCPSMod.LOGGER.info("Average FPS: " + String.format("%.2f", fps) + ", Average frame time: " + avgFrameTime + "ms, View distance: " + viewDistance + ", Render quality: " + renderQualityLevel);
        }
    }
    
    // 分解帧渲染任务
    private List<Runnable> decomposeFrameTasks() {
        List<Runnable> tasks = new ArrayList<>();
        
        // 1. 天空渲染
        tasks.add(() -> {
            submitRenderTask("sky_render", this::renderSky, 2);
        });
        
        // 2. 地形渲染
        tasks.add(() -> {
            submitRenderTask("terrain_render", this::renderTerrain, 3);
        });
        
        // 3. 实体渲染
        tasks.add(() -> {
            submitRenderTask("entity_render", this::renderEntities, 2);
        });
        
        // 4. 粒子渲染
        tasks.add(() -> {
            submitRenderTask("particle_render", this::renderParticles, 1);
        });
        
        // 5. GUI渲染
        tasks.add(() -> {
            submitRenderTask("gui_render", this::renderGUI, 4);
        });
        
        return tasks;
    }
    
    // 渲染天空
    private void renderSky() {
        // 天空渲染逻辑
    }
    
    // 渲染地形
    private void renderTerrain() {
        // 分解地形渲染为多个子任务
        List<Runnable> terrainTasks = decomposeTerrainTasks();
        
        // 并行执行地形渲染任务
        terrainTasks.parallelStream().forEach(Runnable::run);
    }
    
    // 分解地形渲染任务（按优先级）
    private List<Runnable> decomposeTerrainTasks() {
        List<Runnable> tasks = new ArrayList<>();
        Map<Double, List<Runnable>> priorityTasks = new TreeMap<>();
        
        int renderDistance = viewDistance;
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                final int blockX = x;
                final int blockZ = z;
                double distance = calculateDistanceToPlayer(x, z);
                
                // 根据距离设置优先级
                double priority = 1.0 / (distance + 1);
                
                if (!priorityTasks.containsKey(priority)) {
                    priorityTasks.put(priority, new ArrayList<>());
                }
                priorityTasks.get(priority).add(() -> {
                    renderChunk(blockX, blockZ);
                });
            }
        }
        
        // 按优先级添加任务
        for (List<Runnable> priorityTaskList : priorityTasks.values()) {
            tasks.addAll(priorityTaskList);
        }
        
        return tasks;
    }
    
    // 计算区块与玩家的距离
    private double calculateDistanceToPlayer(int chunkX, int chunkZ) {
        // 简化的距离计算，实际应使用玩家当前位置
        return Math.sqrt(chunkX * chunkX + chunkZ * chunkZ);
    }
    
    // 渲染单个区块
    private void renderChunk(int x, int z) {
        // 计算区块与玩家的距离
        double distance = calculateDistanceToPlayer(x, z);
        
        // 根据距离调整渲染细节
        if (distance < 10) {
            // 近距离：完整渲染
            renderChunkWithFullDetail(x, z);
        } else if (distance < 20) {
            // 中距离：中等细节
            renderChunkWithMediumDetail(x, z);
        } else {
            // 远距离：低细节
            renderChunkWithLowDetail(x, z);
        }
    }
    
    // 完整渲染区块
    private void renderChunkWithFullDetail(int x, int z) {
        // 完整渲染逻辑
    }
    
    // 中等细节渲染区块
    private void renderChunkWithMediumDetail(int x, int z) {
        // 中等细节渲染逻辑
    }
    
    // 低细节渲染区块
    private void renderChunkWithLowDetail(int x, int z) {
        // 低细节渲染逻辑
    }
    
    // 渲染实体
    private void renderEntities() {
        // 分级处理实体渲染
        processEntitiesByDistance();
    }
    
    // 分级处理实体渲染
    private void processEntitiesByDistance() {
        // 近距离实体：完整渲染
        // 中距离实体：简化渲染
        // 远距离实体：非常简化渲染或不渲染
        
        // 模拟实体分级处理
        List<Runnable> entityTasks = new ArrayList<>();
        
        // 近距离实体
        entityTasks.add(() -> {
            // 完整渲染近距离实体
        });
        
        // 中距离实体
        entityTasks.add(() -> {
            // 简化渲染中距离实体
        });
        
        // 远距离实体
        entityTasks.add(() -> {
            // 非常简化渲染远距离实体
        });
        
        // 并行执行实体渲染任务
        entityTasks.parallelStream().forEach(Runnable::run);
    }
    
    // 渲染粒子
    private void renderParticles() {
        // 粒子渲染逻辑
    }
    
    // 渲染GUI
    private void renderGUI() {
        // GUI渲染逻辑
    }
    
    // 同步帧
    public void syncFrame() {
        long currentTime = System.currentTimeMillis();
        long frameTime = currentTime - lastFrameTime.get();
        lastFrameTime.set(currentTime);
        
        // 计算理想的帧间隔时间（60 FPS）
        long idealFrameTime = 1000 / 60;
        
        // 如果帧时间小于理想帧间隔，进行适当的延迟
        if (frameTime < idealFrameTime) {
            try {
                Thread.sleep(idealFrameTime - frameTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 根据帧时间动态调整视距
        adjustViewDistance(frameTime);
        
        // 自动调整渲染设置
        autoAdjustRenderSettings();
    }
    
    // 调整视距
    private void adjustViewDistance(long frameTime) {
        // 目标帧时间（60 FPS）
        long targetFrameTime = 1000 / 60;
        
        // 考虑雾剔除状态
        boolean fogCullingEnabled = FogManager.isFogCullingEnabled();
        
        // 如果启用了雾剔除，视距调整更加保守
        if (fogCullingEnabled) {
            // 如果帧时间超过目标时间的1.2倍，减少视距
            if (frameTime > targetFrameTime * 1.2 && viewDistance > minViewDistance) {
                viewDistance--;
                MCPSMod.LOGGER.debug("Decreased view distance to " + viewDistance + " due to high frame time with fog culling");
            } 
            // 如果帧时间低于目标时间的0.7倍，增加视距
            else if (frameTime < targetFrameTime * 0.7 && viewDistance < maxViewDistance) {
                viewDistance++;
                MCPSMod.LOGGER.debug("Increased view distance to " + viewDistance + " due to low frame time with fog culling");
            }
        } else {
            // 原有逻辑
            if (frameTime > targetFrameTime * 1.5 && viewDistance > minViewDistance) {
                viewDistance--;
                MCPSMod.LOGGER.debug("Decreased view distance to " + viewDistance + " due to high frame time");
            } 
            else if (frameTime < targetFrameTime * 0.8 && viewDistance < maxViewDistance) {
                viewDistance++;
                MCPSMod.LOGGER.debug("Increased view distance to " + viewDistance + " due to low frame time");
            }
        }
    }
    
    // 自动调整渲染设置
    public void autoAdjustRenderSettings() {
        // 根据系统性能自动调整渲染设置
        if (MCPSMod.getInstance() != null) {
            int fps = getCurrentFPS();
            boolean fogCullingEnabled = FogManager.isFogCullingEnabled();
            
            if (fogCullingEnabled) {
                // 启用雾剔除时的特殊处理
                if (fps < 25) {
                    // 降低渲染质量
                    MCPSMod.LOGGER.info("Low FPS detected with fog culling, reducing render quality");
                    reduceRenderQuality();
                } else if (fps > 100) {
                    // 提高渲染质量
                    MCPSMod.LOGGER.info("High FPS detected with fog culling, increasing render quality");
                    increaseRenderQuality();
                }
            } else {
                // 原有逻辑
                if (fps < 30) {
                    // 降低渲染质量
                    MCPSMod.LOGGER.info("Low FPS detected, reducing render quality");
                    reduceRenderQuality();
                } else if (fps > 120) {
                    // 提高渲染质量
                    MCPSMod.LOGGER.info("High FPS detected, increasing render quality");
                    increaseRenderQuality();
                }
            }
        }
    }
    
    // 降低渲染质量
    private void reduceRenderQuality() {
        // 降低渲染质量级别
        if (renderQualityLevel > 0) {
            renderQualityLevel--;
            MCPSMod.LOGGER.info("Render quality reduced to level " + renderQualityLevel);
        }
        
        // 减少视距
        if (viewDistance > minViewDistance) {
            viewDistance--;
        }
        
        // 可以添加其他质量降低措施
    }
    
    // 提高渲染质量
    private void increaseRenderQuality() {
        // 提高渲染质量级别
        if (renderQualityLevel < 5) {
            renderQualityLevel++;
            MCPSMod.LOGGER.info("Render quality increased to level " + renderQualityLevel);
        }
        
        // 增加视距
        if (viewDistance < maxViewDistance) {
            viewDistance++;
        }
        
        // 可以添加其他质量提高措施
    }
    
    // 获取当前FPS
    public int getCurrentFPS() {
        if (frameCount.get() == 0) return 0;
        long avgFrameTime = totalFrameTime.get() / frameCount.get();
        if (avgFrameTime == 0) return 0;
        return (int) (1000.0 / avgFrameTime);
    }
    
    // 获取当前视距
    public int getViewDistance() {
        return viewDistance;
    }
    
    // 设置最大视距
    public void setMaxViewDistance(int maxViewDistance) {
        this.maxViewDistance = maxViewDistance;
    }
    
    // 设置最小视距
    public void setMinViewDistance(int minViewDistance) {
        this.minViewDistance = minViewDistance;
    }
    
    // 启用/禁用GPU加速
    public void setGPUAcceleration(boolean enabled) {
        this.gpuAcceleration = enabled;
        MCPSMod.LOGGER.info("GPU acceleration " + (enabled ? "enabled" : "disabled"));
    }
    
    // 获取活跃渲染任务数
    public int getActiveRenderTasks() {
        return activeRenderTasks.get();
    }
    
    // 获取渲染任务数
    public int getRenderTaskCount() {
        return renderTasks.size();
    }
    
    // 获取渲染任务时间
    public Map<String, Long> getRenderTaskTimes() {
        return renderTaskTimes;
    }
    
    // 获取当前渲染质量级别
    public int getCurrentRenderQualityLevel() {
        return renderQualityLevel;
    }
    
    // 关闭渲染管理器
    public void shutdown() {
        if (renderExecutor != null && !renderExecutor.isShutdown()) {
            renderExecutor.shutdown();
        }
        
        MCPSMod.LOGGER.info("RenderManager shutdown");
    }
    
    private static class RenderTask {
        private String id;
        private Runnable task;
        private long submissionTime;
        private int priority;
        
        public RenderTask(String id, Runnable task, int priority) {
            this.id = id;
            this.task = task;
            this.submissionTime = System.currentTimeMillis();
            this.priority = priority;
        }
        
        public String getId() {
            return id;
        }
        
        public Runnable getTask() {
            return task;
        }
        
        public long getSubmissionTime() {
            return submissionTime;
        }
        
        public int getPriority() {
            return priority;
        }
    }
}