package com.qituo.mcps.api;

import com.qituo.mcps.core.MCPSMod;
import com.qituo.mcps.thread.ThreadManager;
import com.qituo.mcps.task.SmartTaskScheduler;
import com.qituo.mcps.core.ResourceManager;
import com.qituo.mcps.network.NetworkManager;
import com.qituo.mcps.render.RenderManager;
import com.qituo.mcps.storage.StorageManager;
import com.qituo.mcps.ai.AIManager;
import com.qituo.mcps.gpu.GPUManager;
import com.qituo.mcps.cluster.ClusterManager;

public class MCPSAPI {
    private static MCPSAPI instance;
    
    private MCPSAPI() {
    }
    
    public static MCPSAPI getInstance() {
        if (instance == null) {
            synchronized (MCPSAPI.class) {
                if (instance == null) {
                    instance = new MCPSAPI();
                }
            }
        }
        return instance;
    }
    
    // 获取线程管理器
    public ThreadManager getThreadManager() {
        return MCPSMod.getInstance().getThreadManager();
    }
    
    // 获取任务调度器
    public SmartTaskScheduler getTaskScheduler() {
        return MCPSMod.getInstance().getTaskScheduler();
    }
    
    // 获取资源管理器
    public ResourceManager getResourceManager() {
        return MCPSMod.getInstance().getResourceManager();
    }
    
    // 获取网络管理器
    public NetworkManager getNetworkManager() {
        return MCPSMod.getInstance().getNetworkManager();
    }
    
    // 获取渲染管理器
    public RenderManager getRenderManager() {
        return MCPSMod.getInstance().getRenderManager();
    }
    
    // 获取存储管理器
    public StorageManager getStorageManager() {
        return MCPSMod.getInstance().getStorageManager();
    }
    
    // 获取AI管理器
    public AIManager getAiManager() {
        return MCPSMod.getInstance().getAiManager();
    }
    
    // 获取GPU管理器
    public GPUManager getGpuManager() {
        return MCPSMod.getInstance().getGpuManager();
    }
    
    // 获取集群管理器
    public ClusterManager getClusterManager() {
        return MCPSMod.getInstance().getClusterManager();
    }
    
    // 注册事件监听器
    public void registerEventListener(MCPSListener listener) {
        MCPSMod.getInstance().getEventManager().registerListener(listener);
    }
    
    // 注册插件
    public void registerPlugin(MCPSPlugin plugin) {
        MCPSMod.getInstance().getPluginManager().registerPlugin(plugin);
    }
}