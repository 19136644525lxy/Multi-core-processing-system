package com.qituo.mcps.cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.qituo.mcps.core.MCPSMod;

public class CloudPerformanceMonitor {
    private ConcurrentHashMap<String, PerformanceData> performanceDataMap;
    private String cloudApiEndpoint;
    private AtomicLong dataCollectionCount;
    
    public void initialize() {
        this.performanceDataMap = new ConcurrentHashMap<>();
        this.cloudApiEndpoint = "http://localhost:8080/api/performance";
        this.dataCollectionCount = new AtomicLong(0);
        
        MCPSMod.LOGGER.info("CloudPerformanceMonitor initialized");
    }
    
    public void collectPerformanceData() {
        try {
            // 收集服务器性能数据
            PerformanceData data = new PerformanceData();
            data.setCpuUsage(getCpuUsage());
            data.setMemoryUsage(getMemoryUsage());
            data.setDiskUsage(getDiskUsage());
            data.setNetworkUsage(getNetworkUsage());
            data.setTps(getTPS());
            data.setMspt(getMSPT());
            
            // 存储性能数据
            String serverId = getServerId();
            performanceDataMap.put(serverId, data);
            
            // 上传到云端
            uploadPerformanceData(serverId, data);
            
            dataCollectionCount.incrementAndGet();
            MCPSMod.LOGGER.debug("Collected performance data for server: " + serverId);
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error collecting performance data: " + e.getMessage());
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
    
    private double getNetworkUsage() {
        // 实现网络使用率获取逻辑
        return 30.2; // 模拟数据
    }
    
    private double getTPS() {
        // 实现TPS获取逻辑
        return 19.8; // 模拟数据
    }
    
    private double getMSPT() {
        // 实现MSPT获取逻辑
        return 45.2; // 模拟数据
    }
    
    private String getServerId() {
        // 实现服务器ID获取逻辑
        return "server-" + System.getenv().getOrDefault("COMPUTERNAME", "localhost");
    }
    
    private void uploadPerformanceData(String serverId, PerformanceData data) {
        try {
            URL url = new URL(cloudApiEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            String jsonData = "{" +
                "\"serverId\":\"" + serverId + "\","
                + "\"cpuUsage\":" + data.getCpuUsage() + ","
                + "\"memoryUsage\":" + data.getMemoryUsage() + ","
                + "\"diskUsage\":" + data.getDiskUsage() + ","
                + "\"networkUsage\":" + data.getNetworkUsage() + ","
                + "\"tps\":" + data.getTps() + ","
                + "\"mspt\":" + data.getMspt() + ","
                + "\"timestamp\":" + System.currentTimeMillis()
                + "}";
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                MCPSMod.LOGGER.debug("Performance data uploaded successfully for server: " + serverId);
            } else {
                MCPSMod.LOGGER.warn("Failed to upload performance data, response code: " + responseCode);
            }
            
            connection.disconnect();
        } catch (IOException e) {
            MCPSMod.LOGGER.warn("Cloud API not available, storing data locally: " + e.getMessage());
        }
    }
    
    public PerformanceData getPerformanceData(String serverId) {
        return performanceDataMap.get(serverId);
    }
    
    public long getDataCollectionCount() {
        return dataCollectionCount.get();
    }
    
    public void shutdown() {
        performanceDataMap.clear();
        MCPSMod.LOGGER.info("CloudPerformanceMonitor shutdown");
    }
    
    private static class PerformanceData {
        private double cpuUsage;
        private double memoryUsage;
        private double diskUsage;
        private double networkUsage;
        private double tps;
        private double mspt;
        private long timestamp;
        
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
        
        public double getNetworkUsage() {
            return networkUsage;
        }
        
        public void setNetworkUsage(double networkUsage) {
            this.networkUsage = networkUsage;
        }
        
        public double getTps() {
            return tps;
        }
        
        public void setTps(double tps) {
            this.tps = tps;
        }
        
        public double getMspt() {
            return mspt;
        }
        
        public void setMspt(double mspt) {
            this.mspt = mspt;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}