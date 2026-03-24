package com.qituo.mcps.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.text.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.qituo.mcps.core.MCPSMod;

@Environment(EnvType.CLIENT)
public class FogManager {
    private static boolean fogCullingEnabled = false;
    private static float fogStart = 0.0f;
    private static float fogEnd = 32.0f;
    
    public static void initialize() {
        registerCommands();
        MCPSMod.LOGGER.info("FogManager initialized");
    }
    
    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerFogCommands(dispatcher);
        });
    }
    
    private static void registerFogCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> fogCommand = LiteralArgumentBuilder.<FabricClientCommandSource>literal("fog")
            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("cull")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("enable")
                    .executes(context -> {
                        setFogCullingEnabled(true);
                        context.getSource().sendFeedback(Text.translatable("mcps.fog.culling.enabled"));
                        return 1;
                    })
                )
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("disable")
                    .executes(context -> {
                        setFogCullingEnabled(false);
                        context.getSource().sendFeedback(Text.translatable("mcps.fog.culling.disabled"));
                        return 1;
                    })
                )
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggle")
                    .executes(context -> {
                        setFogCullingEnabled(!fogCullingEnabled);
                        context.getSource().sendFeedback(Text.translatable("mcps.fog.culling.toggle", fogCullingEnabled ? Text.translatable("mcps.fog.status.enabled") : Text.translatable("mcps.fog.status.disabled")));
                        return 1;
                    })
                )
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("status")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.translatable("mcps.fog.culling.status", fogCullingEnabled ? Text.translatable("mcps.fog.status.enabled") : Text.translatable("mcps.fog.status.disabled")));
                        return 1;
                    })
                )
            )
            .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("distance")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("reset")
                    .executes(context -> {
                        resetFogDistance();
                        context.getSource().sendFeedback(Text.translatable("mcps.fog.distance.reset"));
                        return 1;
                    })
                )
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("status")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.translatable("mcps.fog.distance.status", fogStart, fogEnd));
                        return 1;
                    })
                )
            );
        
        dispatcher.register(fogCommand);
    }
    
    public static void setFogCullingEnabled(boolean enabled) {
        fogCullingEnabled = enabled;
        MCPSMod.LOGGER.info("Fog culling " + (enabled ? "enabled" : "disabled"));
    }
    
    public static boolean isFogCullingEnabled() {
        return fogCullingEnabled;
    }
    
    public static void setFogDistance(float start, float end) {
        fogStart = start;
        fogEnd = end;
        MCPSMod.LOGGER.info("Fog distance set to " + start + " - " + end);
    }
    
    public static void resetFogDistance() {
        fogStart = 0.0f;
        fogEnd = 32.0f;
        MCPSMod.LOGGER.info("Fog distance reset to default");
    }
    
    public static float getFogStart() {
        return fogStart;
    }
    
    public static float getFogEnd() {
        return fogEnd;
    }
    
    // 应用雾效果
    public static void applyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog) {
        if (fogCullingEnabled) {
            // 这里可以添加自定义的雾效果逻辑
            // 例如，根据距离调整雾的密度
        }
    }
}