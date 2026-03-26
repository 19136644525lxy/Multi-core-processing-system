package com.qituo.mcps.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.qituo.mcps.config.MCPSConfig;
import com.qituo.mcps.core.MCPSMod;

public class CloudBackupManager {
    private String backupDirectory;
    private String cloudStorageEndpoint;
    private ConcurrentHashMap<String, BackupInfo> backupHistory;
    private AtomicLong backupCount;
    private int maxBackups;
    
    public void initialize() {
        this.backupDirectory = System.getProperty("user.dir") + File.separator + "backups";
        this.cloudStorageEndpoint = MCPSConfig.cloudStorageEndpoint;
        this.backupHistory = new ConcurrentHashMap<>();
        this.backupCount = new AtomicLong(0);
        this.maxBackups = MCPSConfig.maxBackups;
        
        // 创建备份目录
        File dir = new File(backupDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        MCPSMod.LOGGER.info("CloudBackupManager initialized");
    }
    
    public void performBackup() {
        try {
            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String backupFileName = "backup-" + timestamp + ".zip";
            String backupPath = backupDirectory + File.separator + backupFileName;
            
            // 创建备份文件
            createBackup(backupPath);
            
            // 上传到云端
            uploadBackup(backupFileName, backupPath);
            
            // 记录备份信息
            BackupInfo backupInfo = new BackupInfo();
            backupInfo.setBackupId(backupFileName);
            backupInfo.setTimestamp(System.currentTimeMillis());
            backupInfo.setSize(new File(backupPath).length());
            backupInfo.setType("automatic");
            
            backupHistory.put(backupFileName, backupInfo);
            backupCount.incrementAndGet();
            
            // 清理旧备份
            cleanupOldBackups();
            
            MCPSMod.LOGGER.info("Backup performed: " + backupFileName);
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error performing backup: " + e.getMessage());
        }
    }
    
    public void performManualBackup() {
        try {
            // 生成备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String backupFileName = "backup-manual-" + timestamp + ".zip";
            String backupPath = backupDirectory + File.separator + backupFileName;
            
            // 创建备份文件
            createBackup(backupPath);
            
            // 上传到云端
            uploadBackup(backupFileName, backupPath);
            
            // 记录备份信息
            BackupInfo backupInfo = new BackupInfo();
            backupInfo.setBackupId(backupFileName);
            backupInfo.setTimestamp(System.currentTimeMillis());
            backupInfo.setSize(new File(backupPath).length());
            backupInfo.setType("manual");
            
            backupHistory.put(backupFileName, backupInfo);
            backupCount.incrementAndGet();
            
            // 清理旧备份
            cleanupOldBackups();
            
            MCPSMod.LOGGER.info("Manual backup performed: " + backupFileName);
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error performing manual backup: " + e.getMessage());
        }
    }
    
    private void createBackup(String backupPath) throws IOException {
        // 创建备份文件
        try (FileOutputStream fos = new FileOutputStream(backupPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // 备份服务器目录
            File serverDir = new File(System.getProperty("user.dir"));
            addDirectoryToZip(serverDir, zos, "");
        }
    }
    
    private void addDirectoryToZip(File directory, ZipOutputStream zos, String basePath) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // 跳过不需要备份的目录和文件
                if (shouldSkipFile(file)) {
                    continue;
                }
                
                String entryPath = basePath + file.getName();
                if (file.isDirectory()) {
                    zos.putNextEntry(new ZipEntry(entryPath + "/"));
                    addDirectoryToZip(file, zos, entryPath + "/");
                } else {
                    zos.putNextEntry(new ZipEntry(entryPath));
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                    zos.closeEntry();
                }
            }
        }
    }
    
    private boolean shouldSkipFile(File file) {
        String fileName = file.getName();
        return fileName.equals("backups") || 
               fileName.equals("logs") || 
               fileName.equals("build") || 
               fileName.endsWith(".zip") || 
               fileName.equals(".git");
    }
    
    private void uploadBackup(String backupFileName, String backupPath) {
        try {
            URL url = new URL(cloudStorageEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/zip");
            connection.setRequestProperty("X-Backup-Name", backupFileName);
            connection.setDoOutput(true);
            
            try (FileInputStream fis = new FileInputStream(backupPath);
                 OutputStream os = connection.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                MCPSMod.LOGGER.debug("Backup uploaded successfully: " + backupFileName);
            } else {
                MCPSMod.LOGGER.warn("Failed to upload backup, response code: " + responseCode);
            }
            
            connection.disconnect();
        } catch (IOException e) {
            MCPSMod.LOGGER.warn("Cloud storage not available, keeping backup locally: " + e.getMessage());
        }
    }
    
    public void restoreFromBackup(String backupId) {
        try {
            // 下载备份文件（如果不在本地）
            String backupPath = backupDirectory + File.separator + backupId;
            if (!new File(backupPath).exists()) {
                downloadBackup(backupId, backupPath);
            }
            
            // 恢复备份
            restoreBackup(backupPath);
            
            MCPSMod.LOGGER.info("Restored from backup: " + backupId);
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error restoring from backup: " + e.getMessage());
        }
    }
    
    private void downloadBackup(String backupId, String backupPath) throws IOException {
        // 从云端下载备份文件
        URL url = new URL(cloudStorageEndpoint + "/" + backupId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        try (FileOutputStream fos = new FileOutputStream(backupPath);
             OutputStream os = fos) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = connection.getInputStream().read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        
        connection.disconnect();
    }
    
    private void restoreBackup(String backupPath) throws IOException {
        // 实现备份恢复逻辑
        // 注意：实际恢复操作需要谨慎处理，这里仅作为示例
        MCPSMod.LOGGER.info("Restoring backup: " + backupPath);
        // 解压备份文件到服务器目录
    }
    
    private void cleanupOldBackups() {
        // 清理旧备份，保持备份数量不超过maxBackups
        if (backupHistory.size() > maxBackups) {
            // 按时间排序，删除最旧的备份
            backupHistory.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().getTimestamp(), e2.getValue().getTimestamp()))
                .limit(backupHistory.size() - maxBackups)
                .forEach(entry -> {
                    String backupId = entry.getKey();
                    String backupPath = backupDirectory + File.separator + backupId;
                    File backupFile = new File(backupPath);
                    if (backupFile.exists()) {
                        backupFile.delete();
                    }
                    backupHistory.remove(backupId);
                    MCPSMod.LOGGER.info("Cleaned up old backup: " + backupId);
                });
        }
    }
    
    public BackupInfo getBackupInfo(String backupId) {
        return backupHistory.get(backupId);
    }
    
    public long getBackupCount() {
        return backupCount.get();
    }
    
    public void setMaxBackups(int maxBackups) {
        this.maxBackups = maxBackups;
    }
    
    public void shutdown() {
        backupHistory.clear();
        MCPSMod.LOGGER.info("CloudBackupManager shutdown");
    }
    
    private static class BackupInfo {
        private String backupId;
        private long timestamp;
        private long size;
        private String type;
        
        public String getBackupId() {
            return backupId;
        }
        
        public void setBackupId(String backupId) {
            this.backupId = backupId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public long getSize() {
            return size;
        }
        
        public void setSize(long size) {
            this.size = size;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}