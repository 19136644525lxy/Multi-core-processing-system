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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
import java.util.zip.DataFormatException;
import com.qituo.mcps.core.MCPSMod;

public class StorageManager {
    private ExecutorService ioExecutor;
    private ExecutorService compressionExecutor;
    private ConcurrentHashMap<String, ChunkData> chunkCache;
    private AtomicInteger cacheSize;
    private int maxCacheSize;
    private ReentrantReadWriteLock cacheLock;
    private String worldDirectory;
    private CompressionLevel compressionLevel;
    private Map<String, StorageDevice> storageDevices;
    private AtomicLong totalCompressionSavings;
    private BlockingQueue<StorageOperation> storageOperationQueue;
    private ExecutorService storageOperationExecutor;
    private boolean distributedStorageEnabled;
    
    public void initialize(String worldDir) {
        // 如果worldDir为空，使用默认的世界目录
        this.worldDirectory = worldDir.isEmpty() ? "world" : worldDir;
        this.ioExecutor = Executors.newFixedThreadPool(4);
        this.compressionExecutor = Executors.newFixedThreadPool(2);
        this.chunkCache = new ConcurrentHashMap<>();
        this.cacheSize = new AtomicInteger(0);
        this.maxCacheSize = 1000; // 最大缓存1000个区块
        this.cacheLock = new ReentrantReadWriteLock();
        this.compressionLevel = CompressionLevel.MEDIUM;
        this.storageDevices = new ConcurrentHashMap<>();
        this.totalCompressionSavings = new AtomicLong(0);
        this.storageOperationQueue = new LinkedBlockingQueue<>(1000);
        this.storageOperationExecutor = Executors.newFixedThreadPool(2);
        this.distributedStorageEnabled = false;
        
        // 确保世界目录存在
        File worldDirFile = new File(worldDirectory);
        if (!worldDirFile.exists()) {
            worldDirFile.mkdirs();
        }
        
        // 初始化存储设备
        initializeStorageDevices();
        
        // 启动存储操作处理线程
        startStorageOperationProcessor();
        
        MCPSMod.LOGGER.info("StorageManager initialized with world directory: " + worldDirectory);
        MCPSMod.LOGGER.info("Storage optimization features enabled: compression, async operations");
    }
    
    // 初始化存储设备
    private void initializeStorageDevices() {
        // 添加默认存储设备
        StorageDevice defaultDevice = new StorageDevice("default", worldDirectory, 100);
        storageDevices.put("default", defaultDevice);
        
        // 可以添加其他存储设备，如SSD、HDD等
        // 例如：StorageDevice ssdDevice = new StorageDevice("ssd", "/path/to/ssd", 200);
        // storageDevices.put("ssd", ssdDevice);
    }
    
    // 启动存储操作处理线程
    private void startStorageOperationProcessor() {
        for (int i = 0; i < 2; i++) {
            storageOperationExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        StorageOperation operation = storageOperationQueue.take();
                        processStorageOperation(operation);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        MCPSMod.LOGGER.error("Error processing storage operation: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    // 处理存储操作
    private void processStorageOperation(StorageOperation operation) {
        switch (operation.getType()) {
            case SAVE_CHUNK:
                try {
                    saveChunkToDisk(operation.getChunkX(), operation.getChunkZ(), operation.getChunkData());
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Error saving chunk: " + e.getMessage());
                }
                break;
            case LOAD_CHUNK:
                try {
                    ChunkData chunkData = loadChunkFromDisk(operation.getChunkX(), operation.getChunkZ());
                    if (chunkData != null) {
                        cacheChunk(getChunkKey(operation.getChunkX(), operation.getChunkZ()), chunkData);
                    }
                    operation.getCallback().onChunkLoaded(chunkData);
                } catch (Exception e) {
                    MCPSMod.LOGGER.error("Error loading chunk: " + e.getMessage());
                    operation.getCallback().onChunkLoaded(null);
                }
                break;
        }
    }
    
    // 异步加载区块
    public void loadChunkAsync(int chunkX, int chunkZ, ChunkLoadCallback callback) {
        String chunkKey = getChunkKey(chunkX, chunkZ);
        
        // 先检查缓存
        cacheLock.readLock().lock();
        try {
            ChunkData cachedChunk = chunkCache.get(chunkKey);
            if (cachedChunk != null) {
                cachedChunk.updateLastAccessTime();
                callback.onChunkLoaded(cachedChunk);
                return;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        // 创建存储操作并添加到队列
        StorageOperation operation = new StorageOperation(StorageOperationType.LOAD_CHUNK, chunkX, chunkZ, null, callback);
        try {
            storageOperationQueue.offer(operation, 500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MCPSMod.LOGGER.error("Error adding load operation to queue: " + e.getMessage());
            callback.onChunkLoaded(null);
        }
    }
    
    // 异步保存区块
    public void saveChunkAsync(int chunkX, int chunkZ, ChunkData chunkData) {
        String chunkKey = getChunkKey(chunkX, chunkZ);
        
        // 更新缓存
        cacheChunk(chunkKey, chunkData);
        
        // 创建存储操作并添加到队列
        StorageOperation operation = new StorageOperation(StorageOperationType.SAVE_CHUNK, chunkX, chunkZ, chunkData, null);
        try {
            storageOperationQueue.offer(operation, 500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            MCPSMod.LOGGER.error("Error adding save operation to queue: " + e.getMessage());
        }
    }
    
    // 批量保存区块
    public void saveChunksAsync(List<ChunkData> chunks) {
        for (ChunkData chunk : chunks) {
            saveChunkAsync(chunk.getChunkX(), chunk.getChunkZ(), chunk);
        }
    }
    
    // 批量加载区块
    public void loadChunksAsync(List<ChunkCoordinate> coordinates, ChunkBatchLoadCallback callback) {
        AtomicInteger loadedCount = new AtomicInteger(0);
        Map<ChunkCoordinate, ChunkData> loadedChunks = new ConcurrentHashMap<>();
        
        for (ChunkCoordinate coord : coordinates) {
            loadChunkAsync(coord.getX(), coord.getZ(), chunkData -> {
                loadedChunks.put(coord, chunkData);
                if (loadedCount.incrementAndGet() == coordinates.size()) {
                    callback.onChunksLoaded(loadedChunks);
                }
            });
        }
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
        // 根据数据大小和类型选择压缩策略
        if (data.length < 1024) {
            // 小数据不压缩
            return data;
        }
        
        // 根据压缩级别选择压缩算法
        switch (compressionLevel) {
            case LOW:
                return compressWithDeflater(data, Deflater.BEST_SPEED);
            case MEDIUM:
                return compressWithDeflater(data, Deflater.DEFAULT_COMPRESSION);
            case HIGH:
                return compressWithGzip(data);
            case VERY_HIGH:
                return compressWithDeflater(data, Deflater.BEST_COMPRESSION);
            default:
                return compressWithDeflater(data, Deflater.DEFAULT_COMPRESSION);
        }
    }
    
    // 使用Deflater压缩
    private byte[] compressWithDeflater(byte[] data, int level) throws IOException {
        Deflater deflater = new Deflater(level);
        deflater.setInput(data);
        deflater.finish();
        
        byte[] buffer = new byte[1024];
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }
        
        deflater.end();
        byte[] compressed = baos.toByteArray();
        baos.close();
        
        // 记录压缩节省的空间
        long savings = data.length - compressed.length;
        if (savings > 0) {
            totalCompressionSavings.addAndGet(savings);
        }
        
        return compressed;
    }
    
    // 使用GZIP压缩
    private byte[] compressWithGzip(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos);
        gzos.write(data);
        gzos.close();
        
        byte[] compressed = baos.toByteArray();
        
        // 记录压缩节省的空间
        long savings = data.length - compressed.length;
        if (savings > 0) {
            totalCompressionSavings.addAndGet(savings);
        }
        
        return compressed;
    }
    
    // 解压缩数据
    private byte[] decompressData(byte[] data) throws IOException {
        // 检查是否压缩
        if (data.length < 2) {
            return data;
        }
        
        // 检查是否是GZIP格式
        if (data[0] == (byte) 0x1f && data[1] == (byte) 0x8b) {
            return decompressWithGzip(data);
        }
        
        // 尝试使用Deflater解压缩
        try {
            return decompressWithInflater(data);
        } catch (Exception e) {
            // 如果解压缩失败，返回原始数据
            return data;
        }
    }
    
    // 使用Inflater解压缩
    private byte[] decompressWithInflater(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        
        byte[] buffer = new byte[1024];
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            baos.write(buffer, 0, count);
        }
        
        inflater.end();
        byte[] decompressed = baos.toByteArray();
        baos.close();
        
        return decompressed;
    }
    
    // 使用GZIP解压缩
    private byte[] decompressWithGzip(byte[] data) throws IOException {
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
        private long creationTime;
        
        public ChunkData(int chunkX, int chunkZ, byte[] data) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.data = data;
            this.lastAccessTime = System.currentTimeMillis();
            this.creationTime = System.currentTimeMillis();
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
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    // 压缩级别枚举
    public enum CompressionLevel {
        LOW,        // 低压缩率，高速度
        MEDIUM,     // 中等压缩率和速度
        HIGH,       // 高压缩率，低速度
        VERY_HIGH   // 极高压缩率，极低速度
    }
    
    // 存储操作类型枚举
    private enum StorageOperationType {
        SAVE_CHUNK,
        LOAD_CHUNK
    }
    
    // 存储操作类
    private static class StorageOperation {
        private StorageOperationType type;
        private int chunkX;
        private int chunkZ;
        private ChunkData chunkData;
        private ChunkLoadCallback callback;
        
        public StorageOperation(StorageOperationType type, int chunkX, int chunkZ, ChunkData chunkData, ChunkLoadCallback callback) {
            this.type = type;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunkData = chunkData;
            this.callback = callback;
        }
        
        public StorageOperationType getType() {
            return type;
        }
        
        public int getChunkX() {
            return chunkX;
        }
        
        public int getChunkZ() {
            return chunkZ;
        }
        
        public ChunkData getChunkData() {
            return chunkData;
        }
        
        public ChunkLoadCallback getCallback() {
            return callback;
        }
    }
    
    // 存储设备类
    private static class StorageDevice {
        private String id;
        private String path;
        private int priority; // 优先级，值越高优先级越高
        private AtomicLong usedSpace;
        private long maxSpace;
        
        public StorageDevice(String id, String path, long maxSpace) {
            this.id = id;
            this.path = path;
            this.priority = 100;
            this.usedSpace = new AtomicLong(0);
            this.maxSpace = maxSpace * 1024 * 1024 * 1024; // 转换为字节
        }
        
        public String getId() {
            return id;
        }
        
        public String getPath() {
            return path;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public long getUsedSpace() {
            return usedSpace.get();
        }
        
        public long getMaxSpace() {
            return maxSpace;
        }
        
        public long getAvailableSpace() {
            return maxSpace - usedSpace.get();
        }
        
        public void addUsedSpace(long space) {
            usedSpace.addAndGet(space);
        }
        
        public void removeUsedSpace(long space) {
            usedSpace.addAndGet(-space);
        }
        
        public boolean hasSpace(long requiredSpace) {
            return getAvailableSpace() >= requiredSpace;
        }
    }
    
    // 区块坐标类
    public static class ChunkCoordinate {
        private int x;
        private int z;
        
        public ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        public int getX() {
            return x;
        }
        
        public int getZ() {
            return z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkCoordinate that = (ChunkCoordinate) o;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
        
        @Override
        public String toString() {
            return "ChunkCoordinate{" +
                    "x=" + x +
                    ", z=" + z +
                    '}';
        }
    }
    
    // 区块批量加载回调接口
    public interface ChunkBatchLoadCallback {
        void onChunksLoaded(Map<ChunkCoordinate, ChunkData> chunks);
    }
    
    // 设置压缩级别
    public void setCompressionLevel(CompressionLevel level) {
        this.compressionLevel = level;
        MCPSMod.LOGGER.info("Set compression level to " + level);
    }
    
    // 获取压缩节省的总空间
    public long getTotalCompressionSavings() {
        return totalCompressionSavings.get();
    }
    
    // 启用分布式存储
    public void enableDistributedStorage() {
        this.distributedStorageEnabled = true;
        MCPSMod.LOGGER.info("Distributed storage enabled");
    }
    
    // 禁用分布式存储
    public void disableDistributedStorage() {
        this.distributedStorageEnabled = false;
        MCPSMod.LOGGER.info("Distributed storage disabled");
    }
    
    // 添加存储设备
    public void addStorageDevice(String id, String path, long maxSpaceGB) {
        StorageDevice device = new StorageDevice(id, path, maxSpaceGB);
        storageDevices.put(id, device);
        MCPSMod.LOGGER.info("Added storage device: " + id + " at " + path);
    }
    
    // 获取存储设备信息
    public Map<String, StorageDevice> getStorageDevices() {
        return storageDevices;
    }
    
    // 获取存储操作队列大小
    public int getStorageOperationQueueSize() {
        return storageOperationQueue.size();
    }
    
    // 关闭存储管理器
    public void shutdown() {
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
        
        if (compressionExecutor != null && !compressionExecutor.isShutdown()) {
            compressionExecutor.shutdown();
        }
        
        if (storageOperationExecutor != null && !storageOperationExecutor.isShutdown()) {
            storageOperationExecutor.shutdown();
        }
        
        // 清理缓存
        clearCache();
        
        MCPSMod.LOGGER.info("StorageManager shutdown");
        MCPSMod.LOGGER.info("Total compression savings: " + (totalCompressionSavings.get() / (1024 * 1024)) + " MB");
    }
}