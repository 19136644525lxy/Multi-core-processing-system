package com.qituo.mcps.core;

import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

public class ConcurrencyUtils {
    // 线程安全的计数器
    public static class Counter {
        private final AtomicLong value;
        
        public Counter() {
            this(0);
        }
        
        public Counter(long initialValue) {
            this.value = new AtomicLong(initialValue);
        }
        
        public long increment() {
            return value.incrementAndGet();
        }
        
        public long decrement() {
            return value.decrementAndGet();
        }
        
        public long get() {
            return value.get();
        }
        
        public void set(long newValue) {
            value.set(newValue);
        }
    }
    
    // 线程安全的布尔标志
    public static class AtomicBooleanFlag {
        private final AtomicBoolean flag;
        
        public AtomicBooleanFlag() {
            this(false);
        }
        
        public AtomicBooleanFlag(boolean initialValue) {
            this.flag = new AtomicBoolean(initialValue);
        }
        
        public boolean get() {
            return flag.get();
        }
        
        public boolean set(boolean newValue) {
            return flag.getAndSet(newValue);
        }
        
        public boolean compareAndSet(boolean expected, boolean newValue) {
            return flag.compareAndSet(expected, newValue);
        }
    }
    
    // 读写锁包装器
    public static class ReadWriteLockWrapper {
        private final ReentrantReadWriteLock lock;
        private final Lock readLock;
        private final Lock writeLock;
        
        public ReadWriteLockWrapper() {
            this.lock = new ReentrantReadWriteLock();
            this.readLock = lock.readLock();
            this.writeLock = lock.writeLock();
        }
        
        public void readLock() {
            readLock.lock();
        }
        
        public void readUnlock() {
            readLock.unlock();
        }
        
        public void writeLock() {
            writeLock.lock();
        }
        
        public void writeUnlock() {
            writeLock.unlock();
        }
        
        public boolean isWriteLocked() {
            return lock.isWriteLocked();
        }
        
        public int getReadLockCount() {
            return lock.getReadLockCount();
        }
    }
    
    // 信号量包装器
    public static class SemaphoreWrapper {
        private final Semaphore semaphore;
        
        public SemaphoreWrapper(int permits) {
            this.semaphore = new Semaphore(permits);
        }
        
        public void acquire() throws InterruptedException {
            semaphore.acquire();
        }
        
        public void release() {
            semaphore.release();
        }
        
        public int availablePermits() {
            return semaphore.availablePermits();
        }
        
        public boolean tryAcquire() {
            return semaphore.tryAcquire();
        }
    }
    
    // 倒计时器
    public static class CountDownLatchWrapper {
        private final CountDownLatch latch;
        
        public CountDownLatchWrapper(int count) {
            this.latch = new CountDownLatch(count);
        }
        
        public void countDown() {
            latch.countDown();
        }
        
        public void await() throws InterruptedException {
            latch.await();
        }
        
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
        
        public long getCount() {
            return latch.getCount();
        }
    }
    
    // 线程安全的延迟加载器
    public static class LazyLoader<T> {
        private final AtomicReference<T> instance = new AtomicReference<>();
        private final Supplier<T> supplier;
        private final ReadWriteLockWrapper lock = new ReadWriteLockWrapper();
        
        public LazyLoader(Supplier<T> supplier) {
            this.supplier = supplier;
        }
        
        public T get() {
            T value = instance.get();
            if (value == null) {
                lock.writeLock();
                try {
                    value = instance.get();
                    if (value == null) {
                        value = supplier.get();
                        instance.set(value);
                    }
                } finally {
                    lock.writeUnlock();
                }
            }
            return value;
        }
    }
    
    // 线程安全的对象池
    public static class ObjectPool<T> {
        private final BlockingQueue<T> pool;
        private final Supplier<T> objectFactory;
        private final int maxSize;
        private final AtomicInteger currentSize;
        
        public ObjectPool(Supplier<T> objectFactory, int maxSize) {
            this.objectFactory = objectFactory;
            this.maxSize = maxSize;
            this.currentSize = new AtomicInteger(0);
            this.pool = new LinkedBlockingQueue<>(maxSize);
        }
        
        public T acquire() throws InterruptedException {
            T object = pool.poll();
            if (object == null && currentSize.get() < maxSize) {
                if (currentSize.incrementAndGet() <= maxSize) {
                    object = objectFactory.get();
                } else {
                    currentSize.decrementAndGet();
                    object = pool.take();
                }
            } else if (object == null) {
                object = pool.take();
            }
            return object;
        }
        
        public void release(T object) {
            pool.offer(object);
        }
        
        public int size() {
            return pool.size();
        }
        
        public int getCurrentSize() {
            return currentSize.get();
        }
    }
    
    // 函数式接口
    @FunctionalInterface
    public interface Supplier<T> {
        T get();
    }
    
    // 执行线程安全的操作
    public static <T> T executeWithReadLock(ReadWriteLockWrapper lock, java.util.function.Supplier<T> supplier) {
        lock.readLock();
        try {
            return supplier.get();
        } finally {
            lock.readUnlock();
        }
    }
    
    public static void executeWithReadLock(ReadWriteLockWrapper lock, Runnable runnable) {
        lock.readLock();
        try {
            runnable.run();
        } finally {
            lock.readUnlock();
        }
    }
    
    public static <T> T executeWithWriteLock(ReadWriteLockWrapper lock, java.util.function.Supplier<T> supplier) {
        lock.writeLock();
        try {
            return supplier.get();
        } finally {
            lock.writeUnlock();
        }
    }
    
    public static void executeWithWriteLock(ReadWriteLockWrapper lock, Runnable runnable) {
        lock.writeLock();
        try {
            runnable.run();
        } finally {
            lock.writeUnlock();
        }
    }
}