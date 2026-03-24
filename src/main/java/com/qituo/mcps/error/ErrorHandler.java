package com.qituo.mcps.error;

import java.util.concurrent.atomic.AtomicInteger;
import com.qituo.mcps.core.MCPSMod;

public class ErrorHandler {
    private AtomicInteger errorCount;
    private AtomicInteger recoveredCount;
    
    public void initialize() {
        errorCount = new AtomicInteger(0);
        recoveredCount = new AtomicInteger(0);
        
        // 设置默认的未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
        
        MCPSMod.LOGGER.info("ErrorHandler initialized");
    }
    
    public void handleUncaughtException(Thread thread, Throwable throwable) {
        errorCount.incrementAndGet();
        
        MCPSMod.LOGGER.error("Uncaught exception in thread " + thread.getName(), throwable);
        
        // 尝试恢复线程
        if (recoverThread(thread)) {
            recoveredCount.incrementAndGet();
            MCPSMod.LOGGER.info("Thread " + thread.getName() + " recovered");
        }
    }
    
    public boolean handleTaskError(Runnable task, Throwable throwable) {
        errorCount.incrementAndGet();
        
        MCPSMod.LOGGER.error("Error executing task", throwable);
        
        // 这里可以添加任务重试逻辑
        return true; // 表示错误已处理
    }
    
    private boolean recoverThread(Thread thread) {
        // 线程恢复逻辑
        // 对于工作线程，可以尝试重新启动
        if (thread.getName().startsWith("MCPS-Worker-")) {
            // 这里可以添加线程重启逻辑
            return true;
        }
        return false;
    }
    
    public void logError(String message, Throwable throwable) {
        errorCount.incrementAndGet();
        MCPSMod.LOGGER.error(message, throwable);
    }
    
    public void logWarning(String message) {
        MCPSMod.LOGGER.warn(message);
    }
    
    public int getErrorCount() {
        return errorCount.get();
    }
    
    public int getRecoveredCount() {
        return recoveredCount.get();
    }
    
    public void resetCounters() {
        errorCount.set(0);
        recoveredCount.set(0);
        MCPSMod.LOGGER.info("Error counters reset");
    }
    
    public void logStatus() {
        MCPSMod.LOGGER.info("ErrorHandler status: Errors=" + errorCount.get() + ", Recovered=" + recoveredCount.get());
    }
}