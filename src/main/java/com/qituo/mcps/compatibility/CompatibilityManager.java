package com.qituo.mcps.compatibility;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import java.util.HashMap;
import java.util.Map;
import com.qituo.mcps.core.MCPSMod;

public class CompatibilityManager {
    private Map<String, ModCompatibility> modCompatibilities;
    private boolean isVanillaCompatible;
    
    public void initialize() {
        modCompatibilities = new HashMap<>();
        isVanillaCompatible = true;
        
        // 检查基础游戏兼容性
        checkVanillaCompatibility();
        
        // 检查已安装的模组兼容性
        checkInstalledMods();
        
        MCPSMod.LOGGER.info("CompatibilityManager initialized");
        logCompatibilityStatus();
    }
    
    private void checkVanillaCompatibility() {
        // 检查与基础游戏的兼容性
        // 这里可以添加具体的检查逻辑
        isVanillaCompatible = true;
    }
    
    private void checkInstalledMods() {
        // 检查已安装的模组
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            String modId = mod.getMetadata().getId();
            if (!modId.equals(MCPSMod.MOD_ID)) {
                ModCompatibility compatibility = checkModCompatibility(mod);
                modCompatibilities.put(modId, compatibility);
            }
        });
    }
    
    private ModCompatibility checkModCompatibility(ModContainer mod) {
        String modId = mod.getMetadata().getId();
        String modVersion = mod.getMetadata().getVersion().toString();
        
        // 这里可以添加具体的模组兼容性检查逻辑
        // 例如检查已知的冲突模组
        
        ModCompatibility compatibility = new ModCompatibility(modId, modVersion);
        compatibility.setCompatible(true); // 默认假设兼容
        
        // 检查已知的冲突模组
        if (isKnownIncompatible(modId)) {
            compatibility.setCompatible(false);
            compatibility.setReason("Known incompatible mod");
        }
        
        return compatibility;
    }
    
    private boolean isKnownIncompatible(String modId) {
        // 这里可以添加已知的冲突模组列表
        String[] incompatibleMods = {
            // 示例: "some_incompatible_mod"
        };
        
        for (String incompatibleMod : incompatibleMods) {
            if (modId.equals(incompatibleMod)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isModCompatible(String modId) {
        ModCompatibility compatibility = modCompatibilities.get(modId);
        return compatibility != null && compatibility.isCompatible();
    }
    
    public boolean isVanillaCompatible() {
        return isVanillaCompatible;
    }
    
    public void registerCompatibilityHook(String modId, CompatibilityHook hook) {
        ModCompatibility compatibility = modCompatibilities.get(modId);
        if (compatibility != null) {
            compatibility.addHook(hook);
            MCPSMod.LOGGER.info("Registered compatibility hook for mod: " + modId);
        }
    }
    
    public void runCompatibilityHooks() {
        modCompatibilities.values().forEach(compatibility -> {
            if (compatibility.isCompatible()) {
                compatibility.runHooks();
            }
        });
    }
    
    public void logCompatibilityStatus() {
        MCPSMod.LOGGER.info("=== Compatibility Status ===");
        MCPSMod.LOGGER.info("Vanilla compatibility: " + (isVanillaCompatible ? "COMPATIBLE" : "INCOMPATIBLE"));
        
        int compatibleCount = 0;
        int incompatibleCount = 0;
        
        for (ModCompatibility compatibility : modCompatibilities.values()) {
            if (compatibility.isCompatible()) {
                compatibleCount++;
            } else {
                incompatibleCount++;
                MCPSMod.LOGGER.warn("Incompatible mod: " + compatibility.getModId() + " - " + compatibility.getReason());
            }
        }
        
        MCPSMod.LOGGER.info("Mod compatibility: " + compatibleCount + " compatible, " + incompatibleCount + " incompatible");
    }
    
    public static class ModCompatibility {
        private final String modId;
        private final String version;
        private boolean compatible;
        private String reason;
        private CompatibilityHook hook;
        
        public ModCompatibility(String modId, String version) {
            this.modId = modId;
            this.version = version;
            this.compatible = true;
            this.reason = "";
        }
        
        public String getModId() {
            return modId;
        }
        
        public String getVersion() {
            return version;
        }
        
        public boolean isCompatible() {
            return compatible;
        }
        
        public void setCompatible(boolean compatible) {
            this.compatible = compatible;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public void addHook(CompatibilityHook hook) {
            this.hook = hook;
        }
        
        public void runHooks() {
            if (hook != null) {
                try {
                    hook.run();
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Error running compatibility hook for mod: " + modId, e);
                }
            }
        }
    }
    
    @FunctionalInterface
    public interface CompatibilityHook {
        void run();
    }
    
    // 事件系统
    public static final Event<CompatibilityCheckCallback> COMPATIBILITY_CHECK = EventFactory.createArrayBacked(CompatibilityCheckCallback.class,
        (listeners) -> (manager) -> {
            for (CompatibilityCheckCallback listener : listeners) {
                listener.onCompatibilityCheck(manager);
            }
        });
    
    @FunctionalInterface
    public interface CompatibilityCheckCallback {
        void onCompatibilityCheck(CompatibilityManager manager);
    }
}