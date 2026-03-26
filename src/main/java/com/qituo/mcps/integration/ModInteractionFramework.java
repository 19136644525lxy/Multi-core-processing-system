package com.qituo.mcps.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.qituo.mcps.core.MCPSMod;

public class ModInteractionFramework {
    private static ModInteractionFramework instance;
    private ConcurrentHashMap<String, List<ModEventListener>> eventListeners;
    private ConcurrentHashMap<String, ModInfo> modRegistry;
    
    private ModInteractionFramework() {
        this.eventListeners = new ConcurrentHashMap<>();
        this.modRegistry = new ConcurrentHashMap<>();
    }
    
    public static ModInteractionFramework getInstance() {
        if (instance == null) {
            instance = new ModInteractionFramework();
        }
        return instance;
    }
    
    public void registerMod(String modId, String version, String description) {
        ModInfo modInfo = new ModInfo(modId, version, description);
        modRegistry.put(modId, modInfo);
        MCPSMod.LOGGER.info("Registered mod: " + modId + " v" + version);
    }
    
    public void registerEventListener(String eventType, ModEventListener listener) {
        eventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        MCPSMod.LOGGER.info("Registered event listener for: " + eventType);
    }
    
    public void fireEvent(String eventType, Object eventData) {
        List<ModEventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            for (ModEventListener listener : listeners) {
                try {
                    listener.onEvent(eventType, eventData);
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Error handling event: " + eventType, e);
                }
            }
        }
    }
    
    public ModInfo getModInfo(String modId) {
        return modRegistry.get(modId);
    }
    
    public List<ModInfo> getAllMods() {
        return new ArrayList<>(modRegistry.values());
    }
    
    public interface ModEventListener {
        void onEvent(String eventType, Object eventData) throws Exception;
    }
    
    public static class ModInfo {
        private String modId;
        private String version;
        private String description;
        
        public ModInfo(String modId, String version, String description) {
            this.modId = modId;
            this.version = version;
            this.description = description;
        }
        
        public String getModId() {
            return modId;
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getDescription() {
            return description;
        }
    }
}