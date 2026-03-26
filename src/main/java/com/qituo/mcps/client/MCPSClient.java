package com.qituo.mcps.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.qituo.mcps.render.FogManager;

public class MCPSClient implements ClientModInitializer {
    private KeyBinding openMainMenuKey;
    
    @Override
    public void onInitializeClient() {
        // 初始化雾管理器
        FogManager.initialize();
        
        // 注册按键绑定
        openMainMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mcps.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.mcps"
        ));
        
        // 注册客户端 tick 事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMainMenuKey.wasPressed()) {
                // 打开主菜单界面
                client.setScreen(new MCPSMainMenu());
            }
        });
    }
}