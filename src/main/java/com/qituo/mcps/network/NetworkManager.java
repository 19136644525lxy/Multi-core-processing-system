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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.qituo.mcps.core.MCPSMod;

public class NetworkManager {
    private DatagramSocket udpSocket;
    private ScheduledExecutorService executorService;
    private ConcurrentHashMap<String, NetworkConnection> connections;
    private int port;
    
    public void initialize(int port) {
        this.port = port;
        this.executorService = Executors.newScheduledThreadPool(10);
        this.connections = new ConcurrentHashMap<>();
        
        try {
            this.udpSocket = new DatagramSocket(port);
            startUDPServer();
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
    
    // 数据压缩
    public byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return baos.toByteArray();
    }
    
    // 数据解压缩
    public byte[] decompressData(byte[] data, int offset, int length) throws IOException {
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
        
        public NetworkConnection(Socket socket) {
            this.socket = socket;
            this.autoReconnect = false;
            this.maxRetries = 5;
            this.retryCount = 0;
            this.host = socket.getInetAddress().getHostAddress();
            this.port = socket.getPort();
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
                    String message = new String(decompressed);
                    MCPSMod.LOGGER.debug("Received TCP message: " + message);
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
    
    // 网络消息类
    public static class NetworkMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String type;
        private String content;
        private long timestamp;
        
        public NetworkMessage(String type, String content) {
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getType() {
            return type;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}