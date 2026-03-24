package com.qituo.mcps.api;

import java.util.Map;
import java.util.List;

public interface PluginConfig {
    // 获取字符串配置
    String getString(String key, String defaultValue);
    
    // 获取整数配置
    int getInt(String key, int defaultValue);
    
    // 获取长整数配置
    long getLong(String key, long defaultValue);
    
    // 获取布尔值配置
    boolean getBoolean(String key, boolean defaultValue);
    
    // 获取浮点数配置
    double getDouble(String key, double defaultValue);
    
    // 获取字符串列表配置
    List<String> getStringList(String key, List<String> defaultValue);
    
    // 获取整数列表配置
    List<Integer> getIntList(String key, List<Integer> defaultValue);
    
    // 获取所有配置
    Map<String, Object> getAll();
    
    // 设置配置
    void set(String key, Object value);
    
    // 移除配置
    void remove(String key);
    
    // 检查配置是否存在
    boolean contains(String key);
    
    // 加载配置
    void load();
    
    // 保存配置
    void save();
}