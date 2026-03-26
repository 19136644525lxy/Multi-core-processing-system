package com.qituo.mcps.client;

import net.fabricmc.api.ClientModInitializer;
import com.qituo.mcps.render.FogManager;

public class MCPSClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 初始化雾管理器
        FogManager.initialize();
    }
}