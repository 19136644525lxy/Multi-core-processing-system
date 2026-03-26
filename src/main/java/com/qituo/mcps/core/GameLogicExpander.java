package com.qituo.mcps.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameLogicExpander extends GameLogicProcessor {
    protected List<World> worlds;
    private CopyOnWriteArrayList<BlockEntity> blockEntities;
    private CopyOnWriteArrayList<HostileEntity> hostileEntities;
    private CopyOnWriteArrayList<AnimalEntity> animalEntities;
    private CopyOnWriteArrayList<ExperienceOrbEntity> experienceOrbs;
    private AtomicInteger entityProcessCount;
    
    @Override
    public void initialize(MinecraftServer server) {
        super.initialize(server);
        
        worlds = new CopyOnWriteArrayList<>();
        blockEntities = new CopyOnWriteArrayList<>();
        hostileEntities = new CopyOnWriteArrayList<>();
        animalEntities = new CopyOnWriteArrayList<>();
        experienceOrbs = new CopyOnWriteArrayList<>();
        entityProcessCount = new AtomicInteger(0);
        
        // 注册额外的tick事件
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTickEndExpanded);
        
        MCPSMod.LOGGER.info("GameLogicExpander initialized");
    }
    
    protected void onServerTickEndExpanded(MinecraftServer server) {
        // 收集所有世界
        collectWorlds(server);
        
        // 并行处理各种游戏系统
        processBlockEntities();
        processHostileEntities();
        processAnimalEntities();
        processExperienceOrbs();
        processWeather();
        processDayNightCycle();
        processChunks();
        processRedstone();
    }
    
    private void collectWorlds(MinecraftServer server) {
        worlds.clear();
        for (RegistryKey<World> dimension : server.getWorldRegistryKeys()) {
            World world = server.getWorld(dimension);
            if (world != null) {
                worlds.add(world);
            }
        }
    }
    
    private void processBlockEntities() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("block_updates", () -> {
            long startTime = System.currentTimeMillis();
            
            // 收集所有方块实体
            blockEntities.clear();
            // 简化处理，暂时不收集方块实体
            
            // 并行处理方块实体
            blockEntities.parallelStream().forEach(blockEntity -> {
                try {
                    // 处理方块实体的逻辑
                    // 例如熔炉、 chest、漏斗等
                    blockEntity.markDirty();
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error processing block entity", e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("block_entities", duration);
        }, "block_entities_tick");
    }
    
    private void processHostileEntities() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("entity_ai", () -> {
            long startTime = System.currentTimeMillis();
            
            // 收集所有敌对生物
            hostileEntities.clear();
            // 简化处理，暂时不收集敌对生物
            
            // 分级处理敌对生物AI
            processEntitiesByDistance(hostileEntities);
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("hostile_entities", duration);
        }, "hostile_entities_tick");
    }
    
    private void processAnimalEntities() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("entity_ai", () -> {
            long startTime = System.currentTimeMillis();
            
            // 收集所有动物
            animalEntities.clear();
            // 简化处理，暂时不收集动物
            
            // 分级处理动物AI
            processEntitiesByDistance(animalEntities);
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("animal_entities", duration);
        }, "animal_entities_tick");
    }
    
    private void processExperienceOrbs() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("entity_ai", () -> {
            long startTime = System.currentTimeMillis();
            
            // 收集所有经验球
            experienceOrbs.clear();
            // 简化处理，暂时不收集经验球
            
            // 并行处理经验球
            experienceOrbs.parallelStream().forEach(orb -> {
                try {
                    // 处理经验球逻辑
                    // 例如经验球移动、吸引等
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error processing experience orb", e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("experience_orbs", duration);
        }, "experience_orbs_tick");
    }
    
    private void processWeather() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("world_generation", () -> {
            long startTime = System.currentTimeMillis();
            
            // 并行处理天气系统
            worlds.parallelStream().forEach(world -> {
                try {
                    // 处理天气逻辑
                    // 例如雨、雷暴等
                    if (world.isRaining()) {
                        // 雨的逻辑
                    }
                    if (world.isThundering()) {
                        // 雷暴的逻辑
                    }
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error processing weather", e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("weather", duration);
        }, "weather_tick");
    }
    
    private void processDayNightCycle() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("world_generation", () -> {
            long startTime = System.currentTimeMillis();
            
            // 并行处理昼夜循环
            worlds.parallelStream().forEach(world -> {
                try {
                    // 处理昼夜循环逻辑
                    // 例如时间流逝、月亮相位等
                    long time = world.getTimeOfDay();
                    // 时间相关逻辑
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error processing day-night cycle", e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("day_night_cycle", duration);
        }, "day_night_cycle_tick");
    }
    
    // 并行处理区块加载和生成
    private void processChunks() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("world_generation", () -> {
            long startTime = System.currentTimeMillis();
            
            // 并行处理区块加载和生成
            worlds.parallelStream().forEach(world -> {
                try {
                    // 处理区块加载和生成逻辑
                    // 例如预加载区块、生成新区块等
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error processing chunks", e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("chunk_processing", duration);
        }, "chunk_processing_tick");
    }
    
    // 并行处理红石计算
    private void processRedstone() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("block_updates", () -> {
            long startTime = System.currentTimeMillis();
            
            // 并行处理红石计算
            worlds.parallelStream().forEach(world -> {
                try {
                    // 处理红石逻辑
                    // 例如红石信号传播、红石机关等
                } catch (Exception e) {
                    MCPSMod.getInstance().getErrorHandler().logError("Error processing redstone", e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("redstone_processing", duration);
        }, "redstone_processing_tick");
    }
    
    // 分级处理实体
    private <T extends Entity> void processEntitiesByDistance(List<T> entities) {
        if (entities.isEmpty()) return;
        
        // 这里应该有一个玩家位置作为参考点
        // 暂时使用(0, 0, 0)作为参考点
        Vec3d referencePos = new Vec3d(0, 0, 0);
        
        // 分级处理
        List<T> closeEntities = new CopyOnWriteArrayList<>();
        List<T> mediumEntities = new CopyOnWriteArrayList<>();
        List<T> farEntities = new CopyOnWriteArrayList<>();
        
        // 分类实体
        for (T entity : entities) {
            double distance = entity.getPos().distanceTo(referencePos);
            if (distance < 32) {
                closeEntities.add(entity);
            } else if (distance < 64) {
                mediumEntities.add(entity);
            } else {
                farEntities.add(entity);
            }
        }
        
        // 并行处理近距离实体（完整处理）
        closeEntities.parallelStream().forEach(entity -> {
            try {
                // 完整AI处理
                processEntityAI(entity, true);
            } catch (Exception e) {
                MCPSMod.getInstance().getErrorHandler().logError("Error processing close entity", e);
            }
        });
        
        // 并行处理中距离实体（简化处理）
        mediumEntities.parallelStream().forEach(entity -> {
            try {
                // 简化AI处理
                processEntityAI(entity, false);
            } catch (Exception e) {
                MCPSMod.getInstance().getErrorHandler().logError("Error processing medium entity", e);
            }
        });
        
        // 并行处理远距离实体（非常简化处理）
        farEntities.parallelStream().forEach(entity -> {
            try {
                // 非常简化AI处理
                processEntityAI(entity, false);
            } catch (Exception e) {
                MCPSMod.getInstance().getErrorHandler().logError("Error processing far entity", e);
            }
        });
        
        entityProcessCount.addAndGet(entities.size());
    }
    
    // 处理实体AI
    private <T extends Entity> void processEntityAI(T entity, boolean fullProcessing) {
        if (entity instanceof HostileEntity) {
            HostileEntity hostile = (HostileEntity) entity;
            if (fullProcessing) {
                // 完整的敌对生物AI处理
                // 例如寻路、攻击、躲避等
            } else {
                // 简化的敌对生物AI处理
                // 例如基本移动、简单攻击
            }
        } else if (entity instanceof AnimalEntity) {
            AnimalEntity animal = (AnimalEntity) entity;
            if (fullProcessing) {
                // 完整的动物AI处理
                // 例如寻路、繁殖、躲避等
            } else {
                // 简化的动物AI处理
                // 例如基本移动、简单行为
            }
        }
    }
    
    // 扩展方法：处理农作物生长
    public void processCropGrowth() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("block_updates", () -> {
            long startTime = System.currentTimeMillis();
            
            // 处理农作物生长逻辑
            // 例如小麦、胡萝卜、马铃薯等
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("crop_growth", duration);
        }, "crop_growth_tick");
    }
    
    // 扩展方法：处理液体流动
    public void processFluidFlow() {
        MCPSMod.getInstance().getTaskScheduler().scheduleTask("block_updates", () -> {
            long startTime = System.currentTimeMillis();
            
            // 处理液体流动逻辑
            // 例如水、岩浆等
            
            long duration = System.currentTimeMillis() - startTime;
            MCPSMod.getInstance().getPerformanceMonitor().recordTaskTime("fluid_flow", duration);
        }, "fluid_flow_tick");
    }
    
    // 获取实体处理计数
    public int getEntityProcessCount() {
        return entityProcessCount.get();
    }
    
    // 重置实体处理计数
    public void resetEntityProcessCount() {
        entityProcessCount.set(0);
    }
}