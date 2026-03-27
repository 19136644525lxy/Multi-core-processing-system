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
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import com.qituo.mcps.config.MCPSConfig;
import com.qituo.mcps.core.MCPSMod;

public class CloudBackupManager {
    private String backupDirectory;
    private String cloudStorageEndpoint;
    private ConcurrentHashMap<String, BackupInfo> backupHistory;
    private AtomicLong backupCount;
    private int maxBackups;
    private SecretKey encryptionKey;
    private ConcurrentHashMap<String, BackupPlan> backupPlans;
    private ConcurrentHashMap<String, FileHash> fileHashes;
    
    public void initialize() {
        this.backupDirectory = System.getProperty("user.dir") + File.separator + "backups";
        this.cloudStorageEndpoint = MCPSConfig.cloudStorageEndpoint;
        this.backupHistory = new ConcurrentHashMap<>();
        this.backupCount = new AtomicLong(0);
        this.maxBackups = MCPSConfig.maxBackups;
        this.backupPlans = new ConcurrentHashMap<>();
        this.fileHashes = new ConcurrentHashMap<>();
        
        // 初始化加密密钥
        initializeEncryptionKey();
        
        // 创建备份目录
        File dir = new File(backupDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 加载文件哈希值
        loadFileHashes();
        
        // 初始化默认备份计划
        initializeDefaultBackupPlans();
        
        MCPSMod.LOGGER.info("CloudBackupManager initialized");
    }
    
    private void initializeEncryptionKey() {
        try {
            // 生成或加载加密密钥
            File keyFile = new File(backupDirectory + File.separator + "encryption.key");
            if (keyFile.exists()) {
                // 加载现有密钥
                byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
                encryptionKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                // 生成新密钥
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(256);
                encryptionKey = keyGenerator.generateKey();
                // 保存密钥
                Files.write(keyFile.toPath(), encryptionKey.getEncoded());
            }
        } catch (Exception e) {
            MCPSMod.LOGGER.warn("Failed to initialize encryption key, backups will not be encrypted: " + e.getMessage());
        }
    }
    
    private void loadFileHashes() {
        try {
            // 加载文件哈希值，用于增量备份
            File hashFile = new File(backupDirectory + File.separator + "file_hashes.txt");
            if (hashFile.exists()) {
                List<String> lines = Files.readAllLines(hashFile.toPath());
                for (String line : lines) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        FileHash fileHash = new FileHash();
                        fileHash.setFilePath(parts[0]);
                        fileHash.setHash(parts[1]);
                        fileHashes.put(parts[0], fileHash);
                    }
                }
            }
        } catch (Exception e) {
            MCPSMod.LOGGER.warn("Failed to load file hashes: " + e.getMessage());
        }
    }
    
    private void saveFileHashes() {
        try {
            // 保存文件哈希值
            File hashFile = new File(backupDirectory + File.separator + "file_hashes.txt");
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, FileHash> entry : fileHashes.entrySet()) {
                sb.append(entry.getKey()).append(" ").append(entry.getValue().getHash()).append("\n");
            }
            Files.write(hashFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            MCPSMod.LOGGER.warn("Failed to save file hashes: " + e.getMessage());
        }
    }
    
    private void initializeDefaultBackupPlans() {
        // 创建默认备份计划
        BackupPlan dailyPlan = new BackupPlan();
        dailyPlan.setPlanId("daily");
        dailyPlan.setName("Daily Backup");
        dailyPlan.setInterval(24); // 24 hours
        dailyPlan.setEnabled(true);
        backupPlans.put("daily", dailyPlan);
        
        BackupPlan weeklyPlan = new BackupPlan();
        weeklyPlan.setPlanId("weekly");
        weeklyPlan.setName("Weekly Backup");
        weeklyPlan.setInterval(168); // 7 days
        weeklyPlan.setEnabled(true);
        backupPlans.put("weekly", weeklyPlan);
        
        BackupPlan monthlyPlan = new BackupPlan();
        monthlyPlan.setPlanId("monthly");
        monthlyPlan.setName("Monthly Backup");
        monthlyPlan.setInterval(720); // 30 days
        monthlyPlan.setEnabled(true);
        backupPlans.put("monthly", monthlyPlan);
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
    
    private void createIncrementalBackup(String backupPath, String baseBackupId) throws IOException {
        // 创建增量备份文件
        try (FileOutputStream fos = new FileOutputStream(backupPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // 备份服务器目录（仅包含更改的文件）
            File serverDir = new File(System.getProperty("user.dir"));
            addChangedFilesToZip(serverDir, zos, "");
        }
        
        // 保存文件哈希值
        saveFileHashes();
    }
    
    private void addChangedFilesToZip(File directory, ZipOutputStream zos, String basePath) throws IOException {
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
                    addChangedFilesToZip(file, zos, entryPath + "/");
                } else {
                    // 计算文件哈希值
                    String fileHash = calculateFileHash(file);
                    String relativePath = file.getAbsolutePath().replace(System.getProperty("user.dir"), "");
                    
                    // 检查文件是否更改
                    FileHash existingHash = fileHashes.get(relativePath);
                    if (existingHash == null || !existingHash.getHash().equals(fileHash)) {
                        // 文件已更改，添加到备份
                        zos.putNextEntry(new ZipEntry(entryPath));
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                        }
                        zos.closeEntry();
                        
                        // 更新文件哈希值
                        FileHash newHash = new FileHash();
                        newHash.setFilePath(relativePath);
                        newHash.setHash(fileHash);
                        fileHashes.put(relativePath, newHash);
                    }
                }
            }
        }
    }
    
    private String calculateFileHash(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    digest.update(buffer, 0, length);
                }
            }
            byte[] hashBytes = digest.digest();
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to calculate file hash", e);
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
    
    public boolean verifyBackup(String backupId) {
        try {
            String backupPath = backupDirectory + File.separator + backupId;
            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                return false;
            }
            
            // 验证备份文件完整性
            // 这里可以添加更复杂的验证逻辑，例如检查文件大小、计算哈希值等
            return backupFile.length() > 0;
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error verifying backup: " + e.getMessage());
            return false;
        }
    }
    
    public void performIncrementalBackup(String baseBackupId) {
        try {
            // 生成增量备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String backupFileName = "backup-incremental-" + timestamp + ".zip";
            String backupPath = backupDirectory + File.separator + backupFileName;
            
            // 创建增量备份
            createIncrementalBackup(backupPath, baseBackupId);
            
            // 上传到云端
            uploadBackup(backupFileName, backupPath);
            
            // 记录备份信息
            BackupInfo backupInfo = new BackupInfo();
            backupInfo.setBackupId(backupFileName);
            backupInfo.setTimestamp(System.currentTimeMillis());
            backupInfo.setSize(new File(backupPath).length());
            backupInfo.setType("incremental");
            backupInfo.setBaseBackupId(baseBackupId);
            
            backupHistory.put(backupFileName, backupInfo);
            backupCount.incrementAndGet();
            
            // 清理旧备份
            cleanupOldBackups();
            
            MCPSMod.LOGGER.info("Incremental backup performed: " + backupFileName);
        } catch (Exception e) {
            MCPSMod.LOGGER.error("Error performing incremental backup: " + e.getMessage());
        }
    }
    
    public void addBackupPlan(String planId, String name, int interval, boolean enabled) {
        BackupPlan plan = new BackupPlan();
        plan.setPlanId(planId);
        plan.setName(name);
        plan.setInterval(interval);
        plan.setEnabled(enabled);
        plan.setLastRunTime(System.currentTimeMillis());
        
        backupPlans.put(planId, plan);
        MCPSMod.LOGGER.info("Added backup plan: " + planId);
    }
    
    public void updateBackupPlan(String planId, String name, int interval, boolean enabled) {
        BackupPlan plan = backupPlans.get(planId);
        if (plan != null) {
            plan.setName(name);
            plan.setInterval(interval);
            plan.setEnabled(enabled);
            MCPSMod.LOGGER.info("Updated backup plan: " + planId);
        }
    }
    
    public void removeBackupPlan(String planId) {
        backupPlans.remove(planId);
        MCPSMod.LOGGER.info("Removed backup plan: " + planId);
    }
    
    public BackupPlan getBackupPlan(String planId) {
        return backupPlans.get(planId);
    }
    
    public List<BackupPlan> getBackupPlans() {
        return new ArrayList<>(backupPlans.values());
    }
    
    public void checkBackupPlans() {
        long currentTime = System.currentTimeMillis();
        for (BackupPlan plan : backupPlans.values()) {
            if (plan.isEnabled() && (currentTime - plan.getLastRunTime() >= plan.getInterval() * 60 * 60 * 1000)) {
                // 执行备份
                performBackup();
                plan.setLastRunTime(currentTime);
                MCPSMod.LOGGER.info("Executed backup plan: " + plan.getPlanId());
            }
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
        backupPlans.clear();
        fileHashes.clear();
        MCPSMod.LOGGER.info("CloudBackupManager shutdown");
    }
    
    private static class BackupInfo {
        private String backupId;
        private long timestamp;
        private long size;
        private String type;
        private String baseBackupId;
        
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
        
        public String getBaseBackupId() {
            return baseBackupId;
        }
        
        public void setBaseBackupId(String baseBackupId) {
            this.baseBackupId = baseBackupId;
        }
    }
    
    private static class FileHash {
        private String filePath;
        private String hash;
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getHash() {
            return hash;
        }
        
        public void setHash(String hash) {
            this.hash = hash;
        }
    }
    
    private static class BackupPlan {
        private String planId;
        private String name;
        private int interval; // in hours
        private boolean enabled;
        private long lastRunTime;
        
        public String getPlanId() {
            return planId;
        }
        
        public void setPlanId(String planId) {
            this.planId = planId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getInterval() {
            return interval;
        }
        
        public void setInterval(int interval) {
            this.interval = interval;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getLastRunTime() {
            return lastRunTime;
        }
        
        public void setLastRunTime(long lastRunTime) {
            this.lastRunTime = lastRunTime;
        }
    }
}