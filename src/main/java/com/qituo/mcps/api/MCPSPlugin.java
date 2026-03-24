package com.qituo.mcps.api;

public interface MCPSPlugin {
    // 获取插件名称
    String getName();
    
    // 获取插件版本
    String getVersion();
    
    // 获取插件描述
    String getDescription();
    
    // 初始化插件
    void initialize();
    
    // 启动插件
    void start();
    
    // 停止插件
    void stop();
    
    // 重载插件
    void reload();
    
    // 获取插件配置
    PluginConfig getConfig();
    
    // 保存插件配置
    void saveConfig();
}