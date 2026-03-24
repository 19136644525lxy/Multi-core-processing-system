package com.qituo.mcps.api;

public interface MCPSListener {
    // 当模组初始化完成时调用
    default void onModInitialized() {}
    
    // 当服务器启动时调用
    default void onServerStarting() {}
    
    // 当服务器停止时调用
    default void onServerStopping() {}
    
    // 当线程池大小发生变化时调用
    default void onThreadPoolSizeChanged(int oldSize, int newSize) {}
    
    // 当任务执行完成时调用
    default void onTaskCompleted(String taskName, long executionTime) {}
    
    // 当系统负载发生变化时调用
    default void onSystemLoadChanged(double oldLoad, double newLoad) {}
    
    // 当GPU设备状态发生变化时调用
    default void onGPUDeviceStatusChanged(int deviceId, boolean available) {}
    
    // 当集群节点状态发生变化时调用
    default void onClusterNodeStatusChanged(String nodeId, boolean connected) {}
}