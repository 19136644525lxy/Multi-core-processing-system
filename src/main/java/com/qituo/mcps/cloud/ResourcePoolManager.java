package com.qituo.mcps.cloud;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.qituo.mcps.core.MCPSMod;

public class ResourcePoolManager {
    private ConcurrentHashMap<String, ServerResource> serverResources;
    private ConcurrentHashMap<String, ResourceRequest> resourceRequests;
    private AtomicInteger resourceAllocationCount;
    
    public void initialize() {
        this.serverResources = new ConcurrentHashMap<>();
        this.resourceRequests = new ConcurrentHashMap<>();
        this.resourceAllocationCount = new AtomicInteger(0);
        
        MCPSMod.LOGGER.info("ResourcePoolManager initialized");
    }
    
    public void updateResourceStatus() {
        try {
            // 更新本地服务器资源状态
            String serverId = getServerId();
            ServerResource localResource = new ServerResource();
            localResource.setServerId(serverId);
            localResource.setCpuUsage(getCpuUsage());
            localResource.setMemoryUsage(getMemoryUsage());
            localResource.setDiskUsage(getDiskUsage());
            localResource.setAvailableCpu(getAvailableCpu());
            localResource.setAvailableMemory(getAvailableMemory());
            localResource.setAvailableDisk(getAvailableDisk());
            
            serverResources.put(serverId, localResource);
            
            // 处理资源请求
            processResourceRequests();
            
            MCPSMod.LOGGER.debug("Updated resource status for server: " + serverId);
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error updating resource status: " + e.getMessage());
        }
    }
    
    private double getCpuUsage() {
        // 实现CPU使用率获取逻辑
        return Runtime.getRuntime().availableProcessors() * 0.75; // 模拟数据
    }
    
    private double getMemoryUsage() {
        // 实现内存使用率获取逻辑
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (double) (totalMemory - freeMemory) / totalMemory * 100;
    }
    
    private double getDiskUsage() {
        // 实现磁盘使用率获取逻辑
        return 65.5; // 模拟数据
    }
    
    private int getAvailableCpu() {
        // 实现可用CPU获取逻辑
        return Runtime.getRuntime().availableProcessors() / 2; // 模拟数据
    }
    
    private long getAvailableMemory() {
        // 实现可用内存获取逻辑
        return Runtime.getRuntime().freeMemory();
    }
    
    private long getAvailableDisk() {
        // 实现可用磁盘空间获取逻辑
        return 1024 * 1024 * 1024; // 模拟数据，1GB
    }
    
    private String getServerId() {
        // 实现服务器ID获取逻辑
        return "server-" + System.getenv().getOrDefault("COMPUTERNAME", "localhost");
    }
    
    private void processResourceRequests() {
        // 处理资源请求
        for (ResourceRequest request : resourceRequests.values()) {
            if (!request.isProcessed()) {
                ServerResource bestServer = findBestServerForRequest(request);
                if (bestServer != null) {
                    allocateResources(bestServer, request);
                    request.setProcessed(true);
                    MCPSMod.LOGGER.info("Allocated resources for request: " + request.getRequestId());
                }
            }
        }
    }
    
    private ServerResource findBestServerForRequest(ResourceRequest request) {
        // 查找最适合处理请求的服务器
        ServerResource bestServer = null;
        double bestScore = Double.MAX_VALUE;
        
        for (ServerResource server : serverResources.values()) {
            if (server.getAvailableCpu() >= request.getCpuRequired() &&
                server.getAvailableMemory() >= request.getMemoryRequired() &&
                server.getAvailableDisk() >= request.getDiskRequired()) {
                double score = calculateServerScore(server, request);
                if (score < bestScore) {
                    bestScore = score;
                    bestServer = server;
                }
            }
        }
        
        return bestServer;
    }
    
    private double calculateServerScore(ServerResource server, ResourceRequest request) {
        // 计算服务器适合度分数
        double cpuScore = server.getCpuUsage() / 100.0;
        double memoryScore = server.getMemoryUsage() / 100.0;
        double diskScore = server.getDiskUsage() / 100.0;
        
        return (cpuScore + memoryScore + diskScore) / 3.0;
    }
    
    private void allocateResources(ServerResource server, ResourceRequest request) {
        // 分配资源
        server.setAvailableCpu(server.getAvailableCpu() - request.getCpuRequired());
        server.setAvailableMemory(server.getAvailableMemory() - request.getMemoryRequired());
        server.setAvailableDisk(server.getAvailableDisk() - request.getDiskRequired());
        
        resourceAllocationCount.incrementAndGet();
    }
    
    public void requestResources(String requestId, int cpuRequired, long memoryRequired, long diskRequired) {
        ResourceRequest request = new ResourceRequest();
        request.setRequestId(requestId);
        request.setCpuRequired(cpuRequired);
        request.setMemoryRequired(memoryRequired);
        request.setDiskRequired(diskRequired);
        request.setProcessed(false);
        
        resourceRequests.put(requestId, request);
        MCPSMod.LOGGER.info("Received resource request: " + requestId);
    }
    
    public ServerResource getServerResource(String serverId) {
        return serverResources.get(serverId);
    }
    
    public int getResourceAllocationCount() {
        return resourceAllocationCount.get();
    }
    
    public void shutdown() {
        serverResources.clear();
        resourceRequests.clear();
        MCPSMod.LOGGER.info("ResourcePoolManager shutdown");
    }
    
    private static class ServerResource {
        private String serverId;
        private double cpuUsage;
        private double memoryUsage;
        private double diskUsage;
        private int availableCpu;
        private long availableMemory;
        private long availableDisk;
        
        public String getServerId() {
            return serverId;
        }
        
        public void setServerId(String serverId) {
            this.serverId = serverId;
        }
        
        public double getCpuUsage() {
            return cpuUsage;
        }
        
        public void setCpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        
        public double getMemoryUsage() {
            return memoryUsage;
        }
        
        public void setMemoryUsage(double memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
        
        public double getDiskUsage() {
            return diskUsage;
        }
        
        public void setDiskUsage(double diskUsage) {
            this.diskUsage = diskUsage;
        }
        
        public int getAvailableCpu() {
            return availableCpu;
        }
        
        public void setAvailableCpu(int availableCpu) {
            this.availableCpu = availableCpu;
        }
        
        public long getAvailableMemory() {
            return availableMemory;
        }
        
        public void setAvailableMemory(long availableMemory) {
            this.availableMemory = availableMemory;
        }
        
        public long getAvailableDisk() {
            return availableDisk;
        }
        
        public void setAvailableDisk(long availableDisk) {
            this.availableDisk = availableDisk;
        }
    }
    
    private static class ResourceRequest {
        private String requestId;
        private int cpuRequired;
        private long memoryRequired;
        private long diskRequired;
        private boolean processed;
        
        public String getRequestId() {
            return requestId;
        }
        
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        
        public int getCpuRequired() {
            return cpuRequired;
        }
        
        public void setCpuRequired(int cpuRequired) {
            this.cpuRequired = cpuRequired;
        }
        
        public long getMemoryRequired() {
            return memoryRequired;
        }
        
        public void setMemoryRequired(long memoryRequired) {
            this.memoryRequired = memoryRequired;
        }
        
        public long getDiskRequired() {
            return diskRequired;
        }
        
        public void setDiskRequired(long diskRequired) {
            this.diskRequired = diskRequired;
        }
        
        public boolean isProcessed() {
            return processed;
        }
        
        public void setProcessed(boolean processed) {
            this.processed = processed;
        }
    }
}