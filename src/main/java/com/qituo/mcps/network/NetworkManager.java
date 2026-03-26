package com.qituo.mcps.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.qituo.mcps.core.MCPSMod;

public class NetworkManager {
    private DatagramSocket udpSocket;
    private ScheduledExecutorService executorService;
    private ConcurrentHashMap<String, NetworkConnection> connections;
    private ConcurrentHashMap<String, ServerInfo> serverCluster;
    private BandwidthMonitor bandwidthMonitor;
    private PredictionManager predictionManager;
    private int port;
    private AtomicLong totalBytesSent;
    private AtomicLong totalBytesReceived;
    
    public void initialize(int port) {
        this.port = port;
        this.executorService = Executors.newScheduledThreadPool(10);
        this.connections = new ConcurrentHashMap<>();
        this.serverCluster = new ConcurrentHashMap<>();
        this.bandwidthMonitor = new BandwidthMonitor();
        this.predictionManager = new PredictionManager();
        this.totalBytesSent = new AtomicLong(0);
        this.totalBytesReceived = new AtomicLong(0);
        
        try {
            this.udpSocket = new DatagramSocket(port);
            startUDPServer();
            startBandwidthMonitoring();
        } catch (SocketException e) {
            MCPSMod.LOGGER.error("Failed to initialize UDP socket: " + e.getMessage());
        }
        
        MCPSMod.LOGGER.info("NetworkManager initialized on port: " + port);
    }
    
    private void startUDPServer() {
        executorService.submit(() -> {
            byte[] buffer = new byte[65536];
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    
                    // 处理UDP数据包
                    executorService.submit(() -> {
                        try {
                            byte[] data = decompressData(packet.getData(), 0, packet.getLength());
                            String message = new String(data);
                            MCPSMod.LOGGER.debug("Received UDP message: " + message);
                            totalBytesReceived.addAndGet(data.length);
                        } catch (Exception e) {
                            MCPSMod.LOGGER.error("Failed to process UDP packet: " + e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        MCPSMod.LOGGER.error("UDP server error: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    // 发送UDP消息
    public void sendUDPMessage(String host, int port, String message) {
        executorService.submit(() -> {
            try {
                byte[] data = compressData(message.getBytes());
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
                udpSocket.send(packet);
                totalBytesSent.addAndGet(data.length);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Failed to send UDP message: " + e.getMessage());
            }
        });
    }
    
    // 建立TCP连接
    public NetworkConnection connect(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            NetworkConnection connection = new NetworkConnection(socket);
            connections.put(host + ":" + port, connection);
            
            // 启动连接的读写线程
            executorService.submit(() -> connection.readLoop());
            executorService.submit(() -> connection.writeLoop());
            
            MCPSMod.LOGGER.info("Connected to " + host + ":" + port);
            return connection;
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Failed to connect to " + host + ":" + port + ": " + e.getMessage());
            return null;
        }
    }
    
    // 关闭连接
    public void disconnect(String host, int port) {
        String key = host + ":" + port;
        NetworkConnection connection = connections.remove(key);
        if (connection != null) {
            connection.close();
            MCPSMod.LOGGER.info("Disconnected from " + host + ":" + port);
        }
    }
    
    // 高效数据压缩
    public byte[] compressData(byte[] data) throws IOException {
        // 对于小数据包，直接返回原数据
        if (data.length < 100) {
            return data;
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        byte[] compressed = baos.toByteArray();
        
        // 如果压缩后体积更大，返回原数据
        return compressed.length < data.length ? compressed : data;
    }
    
    // 数据解压缩
    public byte[] decompressData(byte[] data, int offset, int length) throws IOException {
        // 检查是否是压缩数据
        if (length < 2 || data[offset] != (byte)0x1f || data[offset + 1] != (byte)0x8b) {
            byte[] result = new byte[length];
            System.arraycopy(data, offset, result, 0, length);
            return result;
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);
        try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }
    
    // 序列化对象
    public byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
        }
        return compressData(baos.toByteArray());
    }
    
    // 反序列化对象
    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        byte[] decompressed = decompressData(data, 0, data.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(decompressed);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
    
    // 网络负载均衡
    public String getBestServer(String[] servers) {
        // 简单的负载均衡策略：选择响应时间最短的服务器
        long minResponseTime = Long.MAX_VALUE;
        String bestServer = null;
        
        for (String server : servers) {
            long responseTime = pingServer(server);
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
                bestServer = server;
            }
        }
        
        return bestServer;
    }
    
    // 服务器ping测试
    private long pingServer(String server) {
        long start = System.currentTimeMillis();
        try {
            String[] parts = server.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            
            Socket socket = new Socket(host, port);
            socket.close();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - start;
    }
    
    // 断线重连机制
    public void enableAutoReconnect(String host, int port, int maxRetries) {
        String key = host + ":" + port;
        NetworkConnection connection = connections.get(key);
        if (connection != null) {
            connection.enableAutoReconnect(maxRetries);
        }
    }
    
    // 启动带宽监控
    private void startBandwidthMonitoring() {
        executorService.scheduleAtFixedRate(() -> {
            bandwidthMonitor.update(totalBytesSent.get(), totalBytesReceived.get());
            MCPSMod.LOGGER.debug("Bandwidth usage: " + bandwidthMonitor.getFormattedUsage());
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    // 添加服务器到集群
    public void addServerToCluster(String serverAddress, int port, String serverId) {
        ServerInfo serverInfo = new ServerInfo(serverAddress, port, serverId);
        serverCluster.put(serverId, serverInfo);
        MCPSMod.LOGGER.info("Added server " + serverId + " to cluster: " + serverAddress + ":" + port);
    }
    
    // 从集群移除服务器
    public void removeServerFromCluster(String serverId) {
        if (serverCluster.remove(serverId) != null) {
            MCPSMod.LOGGER.info("Removed server " + serverId + " from cluster");
        }
    }
    
    // 同步服务器数据
    public void syncServerData(String sourceServerId, String targetServerId, Object data) {
        ServerInfo sourceServer = serverCluster.get(sourceServerId);
        ServerInfo targetServer = serverCluster.get(targetServerId);
        
        if (sourceServer != null && targetServer != null) {
            executorService.submit(() -> {
                try {
                    NetworkConnection connection = connect(targetServer.getAddress(), targetServer.getPort());
                    if (connection != null) {
                        NetworkMessage message = new NetworkMessage("SYNC_DATA", serialize(data));
                        connection.sendMessage(message);
                        disconnect(targetServer.getAddress(), targetServer.getPort());
                    }
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Failed to sync data between servers: " + e.getMessage());
                }
            });
        }
    }
    
    // 预测元素行为
    public void predictEntityBehavior(String entityId, Object currentState) {
        predictionManager.predictEntityBehavior(entityId, currentState);
    }
    
    // 获取预测的元素状态
    public Object getPredictedEntityState(String entityId) {
        return predictionManager.getPredictedEntityState(entityId);
    }
    
    // 关闭网络管理器
    public void shutdown() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        
        for (NetworkConnection connection : connections.values()) {
            connection.close();
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        MCPSMod.LOGGER.info("NetworkManager shutdown");
    }
    
    private class NetworkConnection {
        private Socket socket;
        private boolean autoReconnect;
        private int maxRetries;
        private int retryCount;
        private String host;
        private int port;
        private ConcurrentHashMap<Long, NetworkMessage> pendingMessages;
        private AtomicLong messageId;
        
        public NetworkConnection(Socket socket) {
            this.socket = socket;
            this.autoReconnect = false;
            this.maxRetries = 5;
            this.retryCount = 0;
            this.host = socket.getInetAddress().getHostAddress();
            this.port = socket.getPort();
            this.pendingMessages = new ConcurrentHashMap<>();
            this.messageId = new AtomicLong(0);
        }
        
        public void enableAutoReconnect(int maxRetries) {
            this.autoReconnect = true;
            this.maxRetries = maxRetries;
        }
        
        public void readLoop() {
            try {
                byte[] buffer = new byte[1024];
                while (!Thread.currentThread().isInterrupted() && socket.isConnected()) {
                    int read = socket.getInputStream().read(buffer);
                    if (read == -1) {
                        break;
                    }
                    
                    // 处理读取的数据
                    byte[] data = new byte[read];
                    System.arraycopy(buffer, 0, data, 0, read);
                    byte[] decompressed = decompressData(data, 0, read);
                    
                    // 解析消息
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(decompressed);
                         ObjectInputStream ois = new ObjectInputStream(bais)) {
                        NetworkMessage message = (NetworkMessage) ois.readObject();
                        MCPSMod.LOGGER.debug("Received TCP message: " + message.getType());
                        
                        // 处理消息
                        processMessage(message);
                    }
                    
                    totalBytesReceived.addAndGet(data.length);
                }
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Read loop error: " + e.getMessage());
            } finally {
                if (autoReconnect && retryCount < maxRetries) {
                    retryCount++;
                    MCPSMod.LOGGER.info("Attempting to reconnect to " + host + ":" + port + " (" + retryCount + "/" + maxRetries + ")");
                    reconnect();
                } else {
                    close();
                }
            }
        }
        
        public void writeLoop() {
            // 实现写入逻辑
        }
        
        public void sendMessage(NetworkMessage message) {
            try {
                byte[] data = serialize(message);
                socket.getOutputStream().write(data);
                socket.getOutputStream().flush();
                totalBytesSent.addAndGet(data.length);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Failed to send message: " + e.getMessage());
            }
        }
        
        private void processMessage(NetworkMessage message) {
            switch (message.getType()) {
                case "SYNC_DATA":
                    // 处理同步数据
                    break;
                case "PREDICTION":
                    // 处理预测数据
                    break;
                default:
                    MCPSMod.LOGGER.debug("Unknown message type: " + message.getType());
            }
        }
        
        public void reconnect() {
            try {
                socket = new Socket(host, port);
                retryCount = 0;
                MCPSMod.LOGGER.info("Reconnected to " + host + ":" + port);
                
                // 重新启动读写线程
                executorService.submit(() -> readLoop());
                executorService.submit(() -> writeLoop());
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Failed to reconnect: " + e.getMessage());
                if (autoReconnect && retryCount < maxRetries) {
                    retryCount++;
                    executorService.schedule(() -> reconnect(), 5000, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        }
        
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    // 带宽监控类
    private class BandwidthMonitor {
        private long lastBytesSent;
        private long lastBytesReceived;
        private long lastTimestamp;
        
        public void update(long bytesSent, long bytesReceived) {
            long currentTime = System.currentTimeMillis();
            if (lastTimestamp > 0) {
                long timeDiff = currentTime - lastTimestamp;
                if (timeDiff > 0) {
                    double uploadSpeed = (bytesSent - lastBytesSent) / (timeDiff / 1000.0) / 1024.0;
                    double downloadSpeed = (bytesReceived - lastBytesReceived) / (timeDiff / 1000.0) / 1024.0;
                    MCPSMod.LOGGER.debug("Bandwidth: Upload " + String.format("%.2f", uploadSpeed) + " KB/s, Download " + String.format("%.2f", downloadSpeed) + " KB/s");
                }
            }
            lastBytesSent = bytesSent;
            lastBytesReceived = bytesReceived;
            lastTimestamp = currentTime;
        }
        
        public String getFormattedUsage() {
            return String.format("Total: Sent %.2f MB, Received %.2f MB", totalBytesSent.get() / (1024.0 * 1024.0), totalBytesReceived.get() / (1024.0 * 1024.0));
        }
    }
    
    // 预测管理器类
    private class PredictionManager {
        private ConcurrentHashMap<String, EntityPrediction> entityPredictions;
        
        public PredictionManager() {
            this.entityPredictions = new ConcurrentHashMap<>();
        }
        
        public void predictEntityBehavior(String entityId, Object currentState) {
            EntityPrediction prediction = new EntityPrediction(currentState);
            entityPredictions.put(entityId, prediction);
        }
        
        public Object getPredictedEntityState(String entityId) {
            EntityPrediction prediction = entityPredictions.get(entityId);
            return prediction != null ? prediction.getPredictedState() : null;
        }
        
        private class EntityPrediction {
            private Object currentState;
            private Object predictedState;
            private long timestamp;
            
            public EntityPrediction(Object currentState) {
                this.currentState = currentState;
                this.timestamp = System.currentTimeMillis();
                this.predictedState = predictState(currentState);
            }
            
            private Object predictState(Object currentState) {
                // 简单的预测逻辑，实际应用中需要根据具体实体类型实现
                return currentState;
            }
            
            public Object getPredictedState() {
                return predictedState;
            }
        }
    }
    
    // 服务器信息类
    private class ServerInfo {
        private String address;
        private int port;
        private String serverId;
        private long lastPing;
        private boolean online;
        
        public ServerInfo(String address, int port, String serverId) {
            this.address = address;
            this.port = port;
            this.serverId = serverId;
            this.lastPing = System.currentTimeMillis();
            this.online = true;
        }
        
        public String getAddress() {
            return address;
        }
        
        public int getPort() {
            return port;
        }
        
        public String getServerId() {
            return serverId;
        }
        
        public boolean isOnline() {
            return online;
        }
        
        public void setOnline(boolean online) {
            this.online = online;
        }
        
        public void updateLastPing() {
            this.lastPing = System.currentTimeMillis();
        }
    }
    
    // 网络消息类
    public static class NetworkMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String type;
        private Object content;
        private long timestamp;
        private long messageId;
        
        public NetworkMessage(String type, Object content) {
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.messageId = System.nanoTime();
        }
        
        public String getType() {
            return type;
        }
        
        public Object getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public long getMessageId() {
            return messageId;
        }
    }
}