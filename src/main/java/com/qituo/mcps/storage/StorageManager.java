package com.qituo.mcps.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.qituo.mcps.core.MCPSMod;

public class StorageManager {
    private ExecutorService ioExecutor;
    private ConcurrentHashMap<String, ChunkData> chunkCache;
    private AtomicInteger cacheSize;
    private int maxCacheSize;
    private ReentrantReadWriteLock cacheLock;
    private String worldDirectory;
    
    public void initialize(String worldDir) {
        // 如果worldDir为空，使用默认的世界目录
        this.worldDirectory = worldDir.isEmpty() ? "world" : worldDir;
        this.ioExecutor = Executors.newFixedThreadPool(4);
        this.chunkCache = new ConcurrentHashMap<>();
        this.cacheSize = new AtomicInteger(0);
        this.maxCacheSize = 1000; // 最大缓存1000个区块
        this.cacheLock = new ReentrantReadWriteLock();
        
        // 确保世界目录存在
        File worldDirFile = new File(worldDirectory);
        if (!worldDirFile.exists()) {
            worldDirFile.mkdirs();
        }
        
        MCPSMod.LOGGER.info("StorageManager initialized with world directory: " + worldDirectory);
    }
    
    // 异步加载区块
    public void loadChunkAsync(int chunkX, int chunkZ, ChunkLoadCallback callback) {
        String chunkKey = getChunkKey(chunkX, chunkZ);
        
        // 先检查缓存
        cacheLock.readLock().lock();
        try {
            ChunkData cachedChunk = chunkCache.get(chunkKey);
            if (cachedChunk != null) {
                callback.onChunkLoaded(cachedChunk);
                return;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        // 异步加载区块
        ioExecutor.submit(() -> {
            try {
                ChunkData chunkData = loadChunkFromDisk(chunkX, chunkZ);
                if (chunkData != null) {
                    // 缓存区块
                    cacheChunk(chunkKey, chunkData);
                }
                callback.onChunkLoaded(chunkData);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error loading chunk " + chunkKey + ": " + e.getMessage());
                callback.onChunkLoaded(null);
            }
        });
    }
    
    // 异步保存区块
    public void saveChunkAsync(int chunkX, int chunkZ, ChunkData chunkData) {
        String chunkKey = getChunkKey(chunkX, chunkZ);
        
        // 更新缓存
        cacheChunk(chunkKey, chunkData);
        
        // 异步保存到磁盘
        ioExecutor.submit(() -> {
            try {
                saveChunkToDisk(chunkX, chunkZ, chunkData);
            } catch (Exception e) {
                MCPSMod.LOGGER.error("Error saving chunk " + chunkKey + ": " + e.getMessage());
            }
        });
    }
    
    // 从磁盘加载区块
    private ChunkData loadChunkFromDisk(int chunkX, int chunkZ) throws IOException, ClassNotFoundException {
        String chunkFile = getChunkFilePath(chunkX, chunkZ);
        Path path = Paths.get(chunkFile);
        
        if (Files.exists(path)) {
            byte[] data = Files.readAllBytes(path);
            // 解压缩和反序列化数据
            byte[] decompressed = decompressData(data);
            return deserializeChunk(decompressed);
        }
        
        return null;
    }
    
    // 保存区块到磁盘
    private void saveChunkToDisk(int chunkX, int chunkZ, ChunkData chunkData) throws IOException {
        String chunkFile = getChunkFilePath(chunkX, chunkZ);
        Path path = Paths.get(chunkFile);
        
        // 确保父目录存在
        Files.createDirectories(path.getParent());
        
        // 序列化和压缩数据
        byte[] serialized = serializeChunk(chunkData);
        byte[] compressed = compressData(serialized);
        
        // 写入文件
        Files.write(path, compressed);
    }
    
    // 缓存区块
    private void cacheChunk(String chunkKey, ChunkData chunkData) {
        cacheLock.writeLock().lock();
        try {
            // 如果缓存已满，移除最旧的区块
            if (cacheSize.get() >= maxCacheSize) {
                removeOldestChunk();
            }
            
            // 添加到缓存
            chunkCache.put(chunkKey, chunkData);
            cacheSize.incrementAndGet();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    // 移除最旧的区块
    private void removeOldestChunk() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (String key : chunkCache.keySet()) {
            ChunkData chunk = chunkCache.get(key);
            if (chunk.getLastAccessTime() < oldestTime) {
                oldestTime = chunk.getLastAccessTime();
                oldestKey = key;
            }
        }
        
        if (oldestKey != null) {
            chunkCache.remove(oldestKey);
            cacheSize.decrementAndGet();
        }
    }
    
    // 获取区块文件路径
    private String getChunkFilePath(int chunkX, int chunkZ) {
        // 优化存储格式：使用层级目录结构
        int regionX = chunkX >> 5;
        int regionZ = chunkZ >> 5;
        return worldDirectory + "/region/r." + regionX + "." + regionZ + ".mca";
    }
    
    // 获取区块键
    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }
    
    // 压缩数据
    private byte[] compressData(byte[] data) throws IOException {
        // 使用GZIP压缩
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos);
        gzos.write(data);
        gzos.close();
        return baos.toByteArray();
    }
    
    // 解压缩数据
    private byte[] decompressData(byte[] data) throws IOException {
        // 使用GZIP解压缩
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
        java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bais);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = gzis.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        gzis.close();
        return baos.toByteArray();
    }
    
    // 序列化区块
    private byte[] serializeChunk(ChunkData chunkData) throws IOException {
        // 使用Java序列化
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(chunkData);
        oos.close();
        return baos.toByteArray();
    }
    
    // 反序列化区块
    private ChunkData deserializeChunk(byte[] data) throws IOException, ClassNotFoundException {
        // 使用Java反序列化
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
        ChunkData chunkData = (ChunkData) ois.readObject();
        ois.close();
        return chunkData;
    }
    
    // 获取缓存大小
    public int getCacheSize() {
        return cacheSize.get();
    }
    
    // 设置最大缓存大小
    public void setMaxCacheSize(int maxSize) {
        this.maxCacheSize = maxSize;
        MCPSMod.LOGGER.info("Set max cache size to " + maxSize);
    }
    
    // 清理缓存
    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            chunkCache.clear();
            cacheSize.set(0);
            MCPSMod.LOGGER.info("Cache cleared");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    // 关闭存储管理器
    public void shutdown() {
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
        
        // 清理缓存
        clearCache();
        
        MCPSMod.LOGGER.info("StorageManager shutdown");
    }
    
    // 区块加载回调接口
    public interface ChunkLoadCallback {
        void onChunkLoaded(ChunkData chunkData);
    }
    
    // 区块数据类
    public static class ChunkData implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private int chunkX;
        private int chunkZ;
        private byte[] data;
        private long lastAccessTime;
        
        public ChunkData(int chunkX, int chunkZ, byte[] data) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.data = data;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public int getChunkX() {
            return chunkX;
        }
        
        public int getChunkZ() {
            return chunkZ;
        }
        
        public byte[] getData() {
            return data;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}