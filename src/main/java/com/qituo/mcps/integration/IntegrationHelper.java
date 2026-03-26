package com.qituo.mcps.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.qituo.mcps.core.MCPSMod;

public class IntegrationHelper {
    private static IntegrationHelper instance;
    private List<ModDependency> dependencies;
    private List<ModIntegration> integrations;
    
    private IntegrationHelper() {
        this.dependencies = new ArrayList<>();
        this.integrations = new ArrayList<>();
    }
    
    public static IntegrationHelper getInstance() {
        if (instance == null) {
            instance = new IntegrationHelper();
        }
        return instance;
    }
    
    public void registerDependency(String modId, String version) {
        ModDependency dependency = new ModDependency(modId, version);
        dependencies.add(dependency);
        MCPSMod.LOGGER.info("Registered dependency: " + modId + " v" + version);
    }
    
    public void registerIntegration(ModIntegration integration) {
        integrations.add(integration);
        MCPSMod.LOGGER.info("Registered integration: " + integration.getModId());
    }
    
    public boolean checkDependencies() {
        boolean allDependenciesMet = true;
        
        for (ModDependency dependency : dependencies) {
            if (!isDependencyMet(dependency)) {
                MCPSMod.LOGGER.warn("Dependency not met: " + dependency.getModId() + " v" + dependency.getVersion());
                allDependenciesMet = false;
            }
        }
        
        return allDependenciesMet;
    }
    
    private boolean isDependencyMet(ModDependency dependency) {
        // 检查模组是否安装且版本符合要求
        // 简化实现，实际需要扫描mods目录
        return true;
    }
    
    public void runIntegrations() {
        for (ModIntegration integration : integrations) {
            try {
                integration.integrate();
                MCPSMod.LOGGER.info("Successfully integrated: " + integration.getModId());
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error integrating mod: " + integration.getModId(), e);
            }
        }
    }
    
    public List<ModDependency> getDependencies() {
        return dependencies;
    }
    
    public List<ModIntegration> getIntegrations() {
        return integrations;
    }
    
    public static class ModDependency {
        private String modId;
        private String version;
        
        public ModDependency(String modId, String version) {
            this.modId = modId;
            this.version = version;
        }
        
        public String getModId() {
            return modId;
        }
        
        public String getVersion() {
            return version;
        }
    }
    
    public interface ModIntegration {
        String getModId();
        void integrate() throws Exception;
    }
}