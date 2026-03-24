package com.qituo.mcps.api;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import com.qituo.mcps.core.MCPSMod;

public class EventManager {
    private static EventManager instance;
    private List<MCPSListener> listeners;
    
    private EventManager() {
        listeners = new CopyOnWriteArrayList<>();
    }
    
    public static EventManager getInstance() {
        if (instance == null) {
            synchronized (EventManager.class) {
                if (instance == null) {
                    instance = new EventManager();
                }
            }
        }
        return instance;
    }
    
    // 注册事件监听器
    public void registerListener(MCPSListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            MCPSMod.LOGGER.info("Registered event listener: " + listener.getClass().getName());
        }
    }
    
    // 注销事件监听器
    public void unregisterListener(MCPSListener listener) {
        if (listeners.remove(listener)) {
            MCPSMod.LOGGER.info("Unregistered event listener: " + listener.getClass().getName());
        }
    }
    
    // 触发模组初始化事件
    public void fireModInitialized() {
        for (MCPSListener listener : listeners) {
            try {
                listener.onModInitialized();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onModInitialized: " + e.getMessage());
            }
        }
    }
    
    // 触发服务器启动事件
    public void fireServerStarting() {
        for (MCPSListener listener : listeners) {
            try {
                listener.onServerStarting();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onServerStarting: " + e.getMessage());
            }
        }
    }
    
    // 触发服务器停止事件
    public void fireServerStopping() {
        for (MCPSListener listener : listeners) {
            try {
                listener.onServerStopping();
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onServerStopping: " + e.getMessage());
            }
        }
    }
    
    // 触发线程池大小变化事件
    public void fireThreadPoolSizeChanged(int oldSize, int newSize) {
        for (MCPSListener listener : listeners) {
            try {
                listener.onThreadPoolSizeChanged(oldSize, newSize);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onThreadPoolSizeChanged: " + e.getMessage());
            }
        }
    }
    
    // 触发任务完成事件
    public void fireTaskCompleted(String taskName, long executionTime) {
        for (MCPSListener listener : listeners) {
            try {
                listener.onTaskCompleted(taskName, executionTime);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onTaskCompleted: " + e.getMessage());
            }
        }
    }
    
    // 触发系统负载变化事件
    public void fireSystemLoadChanged(double oldLoad, double newLoad) {
        for (MCPSListener listener : listeners) {
            try {
                listener.onSystemLoadChanged(oldLoad, newLoad);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onSystemLoadChanged: " + e.getMessage());
            }
        }
    }
    
    // 触发GPU设备状态变化事件
    public void fireGPUDeviceStatusChanged(int deviceId, boolean available) {
        for (MCPSListener listener : listeners) {
            try {
                listener.onGPUDeviceStatusChanged(deviceId, available);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onGPUDeviceStatusChanged: " + e.getMessage());
            }
        }
    }
    
    // 触发集群节点状态变化事件
    public void fireClusterNodeStatusChanged(String nodeId, boolean connected) {
        for (MCPSListener listener : listeners) {
            try {
                listener.onClusterNodeStatusChanged(nodeId, connected);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error in onClusterNodeStatusChanged: " + e.getMessage());
            }
        }
    }
    
    // 获取监听器数量
    public int getListenerCount() {
        return listeners.size();
    }
}