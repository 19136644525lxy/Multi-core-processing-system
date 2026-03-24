package com.qituo.mcps.api;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.qituo.mcps.core.MCPSMod;

public class PluginManager {
    private static PluginManager instance;
    private Map<String, MCPSPlugin> plugins;
    
    private PluginManager() {
        plugins = new ConcurrentHashMap<>();
    }
    
    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }
    
    // 注册插件
    public void registerPlugin(MCPSPlugin plugin) {
        String name = plugin.getName();
        if (!plugins.containsKey(name)) {
            plugins.put(name, plugin);
            plugin.initialize();
            plugin.start();
            MCPSMod.LOGGER.info("Registered plugin: " + name + " v" + plugin.getVersion());
        } else {
            MCPSMod.LOGGER.warn("Plugin already registered: " + name);
        }
    }
    
    // 注销插件
    public void unregisterPlugin(String name) {
        MCPSPlugin plugin = plugins.remove(name);
        if (plugin != null) {
            plugin.stop();
            MCPSMod.LOGGER.info("Unregistered plugin: " + name);
        }
    }
    
    // 启动所有插件
    public void startAllPlugins() {
        for (MCPSPlugin plugin : plugins.values()) {
            try {
                plugin.start();
                MCPSMod.LOGGER.info("Started plugin: " + plugin.getName());
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error starting plugin " + plugin.getName() + ": " + e.getMessage());
            }
        }
    }
    
    // 停止所有插件
    public void stopAllPlugins() {
        for (MCPSPlugin plugin : plugins.values()) {
            try {
                plugin.stop();
                MCPSMod.LOGGER.info("Stopped plugin: " + plugin.getName());
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error stopping plugin " + plugin.getName() + ": " + e.getMessage());
            }
        }
    }
    
    // 重载所有插件
    public void reloadAllPlugins() {
        for (MCPSPlugin plugin : plugins.values()) {
            try {
                plugin.reload();
                MCPSMod.LOGGER.info("Reloaded plugin: " + plugin.getName());
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error reloading plugin " + plugin.getName() + ": " + e.getMessage());
            }
        }
    }
    
    // 获取插件
    public MCPSPlugin getPlugin(String name) {
        return plugins.get(name);
    }
    
    // 获取所有插件
    public Map<String, MCPSPlugin> getAllPlugins() {
        return plugins;
    }
    
    // 获取插件数量
    public int getPluginCount() {
        return plugins.size();
    }
    
    // 检查插件是否已注册
    public boolean isPluginRegistered(String name) {
        return plugins.containsKey(name);
    }
}