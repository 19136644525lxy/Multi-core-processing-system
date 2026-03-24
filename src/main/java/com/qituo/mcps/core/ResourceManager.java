package com.qituo.mcps.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import com.qituo.mcps.core.MCPSMod;

public class ResourceManager {
    private ConcurrentHashMap<String, Resource> resources;
    private ConcurrentHashMap<String, ObjectPool<?>> objectPools;
    private MemoryMonitor memoryMonitor;
    
    public void initialize() {
        resources = new ConcurrentHashMap<>();
        objectPools = new ConcurrentHashMap<>();
        memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();
        
        MCPSMod.LOGGER.info("ResourceManager initialized");
    }
    
    public <T> void registerResource(String name, T resource) {
        Resource<T> newResource = new Resource<>(resource);
        Resource<T> existing = resources.putIfAbsent(name, newResource);
        
        if (existing != null) {
            MCPSMod.LOGGER.warn("Resource already exists: " + name);
        } else {
            MCPSMod.LOGGER.info("Resource registered: " + name);
        }
    }
    
    public <T> T getResource(String name) {
        Resource<T> resource = resources.get(name);
        if (resource == null) {
            return null;
        }
        
        resource.readLock().lock();
        try {
            return resource.getValue();
        } finally {
            resource.readLock().unlock();
        }
    }
    
    public <T> void updateResource(String name, T newValue) {
        Resource<T> resource = resources.get(name);
        if (resource == null) {
            MCPSMod.LOGGER.warn("Resource not found: " + name);
            return;
        }
        
        resource.writeLock().lock();
        try {
            resource.setValue(newValue);
        } finally {
            resource.writeLock().unlock();
        }
    }
    
    public void removeResource(String name) {
        Resource<?> resource = resources.remove(name);
        if (resource != null) {
            MCPSMod.LOGGER.info("Resource removed: " + name);
        }
    }
    
    public boolean hasResource(String name) {
        return resources.containsKey(name);
    }
    
    public int getResourceCount() {
        return resources.size();
    }
    
    // 对象池化功能
    public <T> void createObjectPool(String poolName, int initialSize, ObjectFactory<T> factory) {
        ObjectPool<T> pool = new ObjectPool<>(initialSize, factory);
        ObjectPool<?> existing = objectPools.putIfAbsent(poolName, pool);
        
        if (existing != null) {
            MCPSMod.LOGGER.warn("Object pool already exists: " + poolName);
        } else {
            MCPSMod.LOGGER.info("Object pool created: " + poolName + " with initial size: " + initialSize);
        }
    }
    
    public <T> T borrowObject(String poolName) {
        ObjectPool<T> pool = (ObjectPool<T>) objectPools.get(poolName);
        if (pool == null) {
            MCPSMod.LOGGER.warn("Object pool not found: " + poolName);
            return null;
        }
        
        return pool.borrowObject();
    }
    
    public <T> void returnObject(String poolName, T object) {
        ObjectPool<T> pool = (ObjectPool<T>) objectPools.get(poolName);
        if (pool == null) {
            MCPSMod.LOGGER.warn("Object pool not found: " + poolName);
            return;
        }
        
        pool.returnObject(object);
    }
    
    // 内存分配策略
    public <T> T allocateMemory(String allocationType, int size) {
        // 根据分配类型选择不同的内存分配策略
        switch (allocationType) {
            case "small":
                return (T) new byte[size];
            case "medium":
                return (T) new byte[size];
            case "large":
                return (T) new byte[size];
            default:
                return (T) new byte[size];
        }
    }
    
    // 内存监控
    public MemoryStats getMemoryStats() {
        return memoryMonitor.getStats();
    }
    
    public void logMemoryStats() {
        memoryMonitor.logStats();
    }
    
    private static class Resource<T> {
        private T value;
        private final ReentrantReadWriteLock lock;
        
        public Resource(T value) {
            this.value = value;
            this.lock = new ReentrantReadWriteLock();
        }
        
        public T getValue() {
            return value;
        }
        
        public void setValue(T value) {
            this.value = value;
        }
        
        public Lock readLock() {
            return lock.readLock();
        }
        
        public Lock writeLock() {
            return lock.writeLock();
        }
    }
    
    public interface ObjectFactory<T> {
        T create();
        void reset(T object);
    }
    
    private static class ObjectPool<T> {
        private BlockingQueue<T> pool;
        private ObjectFactory<T> factory;
        
        public ObjectPool(int initialSize, ObjectFactory<T> factory) {
            this.pool = new ArrayBlockingQueue<>(initialSize);
            this.factory = factory;
            
            // 初始化对象池
            for (int i = 0; i < initialSize; i++) {
                pool.offer(factory.create());
            }
        }
        
        public T borrowObject() {
            T object = pool.poll();
            if (object == null) {
                // 如果池为空，创建新对象
                object = factory.create();
            }
            return object;
        }
        
        public void returnObject(T object) {
            // 重置对象状态
            factory.reset(object);
            // 尝试将对象放回池中
            if (!pool.offer(object)) {
                // 如果池已满，丢弃对象
                MCPSMod.LOGGER.debug("Object pool full, discarding object");
            }
        }
    }
    
    private static class MemoryMonitor {
        private AtomicLong allocatedMemory;
        private AtomicLong freedMemory;
        
        public MemoryMonitor() {
            this.allocatedMemory = new AtomicLong(0);
            this.freedMemory = new AtomicLong(0);
        }
        
        public void start() {
            // 启动内存监控线程
            Thread monitorThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(10000); // 每10秒记录一次
                        logStats();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "MCPS-Memory-Monitor");
            monitorThread.setDaemon(true);
            monitorThread.start();
        }
        
        public void allocate(long bytes) {
            allocatedMemory.addAndGet(bytes);
        }
        
        public void free(long bytes) {
            freedMemory.addAndGet(bytes);
        }
        
        public MemoryStats getStats() {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            return new MemoryStats(
                totalMemory,
                freeMemory,
                maxMemory,
                totalMemory - freeMemory,
                allocatedMemory.get(),
                freedMemory.get()
            );
        }
        
        public void logStats() {
            MemoryStats stats = getStats();
            MCPSMod.LOGGER.info("[Memory Stats] Total: " + (stats.totalMemory / 1024 / 1024) + "MB, Used: " + (stats.usedMemory / 1024 / 1024) + "MB, Free: " + (stats.freeMemory / 1024 / 1024) + "MB, Max: " + (stats.maxMemory / 1024 / 1024) + "MB");
        }
    }
    
    public static class MemoryStats {
        public final long totalMemory;
        public final long freeMemory;
        public final long maxMemory;
        public final long usedMemory;
        public final long allocatedMemory;
        public final long freedMemory;
        
        public MemoryStats(long totalMemory, long freeMemory, long maxMemory, long usedMemory, long allocatedMemory, long freedMemory) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.usedMemory = usedMemory;
            this.allocatedMemory = allocatedMemory;
            this.freedMemory = freedMemory;
        }
    }
}