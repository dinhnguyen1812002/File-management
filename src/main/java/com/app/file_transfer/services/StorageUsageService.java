package com.app.file_transfer.services;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageUsageService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    // Storage limit in bytes (5GB default)
    private static final long DEFAULT_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024; // 5GB

    /**
     * Get storage usage statistics for a user
     */
    public StorageUsageStats getStorageUsageStats(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        List<File> userFiles = fileRepository.findByUploader(user);
        
        long totalUsedBytes = 0;
        long documentBytes = 0;
        long imageBytes = 0;
        long videoBytes = 0;
        long audioBytes = 0;
        long otherBytes = 0;
        int totalFiles = userFiles.size();
        int sharedFiles = 0;

        for (File file : userFiles) {
            long fileSize = file.getFileSize();
            totalUsedBytes += fileSize;

            // Count shared files
            if (!file.getRecipients().isEmpty()) {
                sharedFiles++;
            }

            // Categorize by file type
            String fileType = file.getFileType();
            if (fileType != null) {
                fileType = fileType.toLowerCase();
                
                if (isDocumentType(fileType)) {
                    documentBytes += fileSize;
                } else if (isImageType(fileType)) {
                    imageBytes += fileSize;
                } else if (isVideoType(fileType)) {
                    videoBytes += fileSize;
                } else if (isAudioType(fileType)) {
                    audioBytes += fileSize;
                } else {
                    otherBytes += fileSize;
                }
            } else {
                otherBytes += fileSize;
            }
        }

        return new StorageUsageStats(
                totalUsedBytes,
                DEFAULT_STORAGE_LIMIT,
                documentBytes,
                imageBytes,
                videoBytes,
                audioBytes,
                otherBytes,
                totalFiles,
                sharedFiles
        );
    }

    /**
     * Get storage usage breakdown by file type
     */
    public Map<String, Long> getStorageBreakdown(String username) {
        StorageUsageStats stats = getStorageUsageStats(username);
        Map<String, Long> breakdown = new HashMap<>();
        
        breakdown.put("documents", stats.getDocumentBytes());
        breakdown.put("images", stats.getImageBytes());
        breakdown.put("videos", stats.getVideoBytes());
        breakdown.put("audio", stats.getAudioBytes());
        breakdown.put("others", stats.getOtherBytes());
        
        return breakdown;
    }

    /**
     * Check if user has exceeded storage limit
     */
    public boolean isStorageLimitExceeded(String username) {
        StorageUsageStats stats = getStorageUsageStats(username);
        return stats.getTotalUsedBytes() > stats.getStorageLimitBytes();
    }

    /**
     * Get storage usage percentage
     */
    public double getStorageUsagePercentage(String username) {
        StorageUsageStats stats = getStorageUsageStats(username);
        return (double) stats.getTotalUsedBytes() / stats.getStorageLimitBytes() * 100;
    }

    /**
     * Format bytes to human readable format
     */
    public String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Check if user can upload a file of given size
     */
    public boolean canUploadFile(String username, long fileSize) {
        StorageUsageStats stats = getStorageUsageStats(username);
        return (stats.getTotalUsedBytes() + fileSize) <= stats.getStorageLimitBytes();
    }

    /**
     * Get remaining storage space
     */
    public long getRemainingStorage(String username) {
        StorageUsageStats stats = getStorageUsageStats(username);
        return Math.max(0, stats.getStorageLimitBytes() - stats.getTotalUsedBytes());
    }

    /**
     * Get storage warning level
     * @return 0 = OK, 1 = Warning (>80%), 2 = Critical (>95%)
     */
    public int getStorageWarningLevel(String username) {
        double percentage = getStorageUsagePercentage(username);
        if (percentage > 95) return 2; // Critical
        if (percentage > 80) return 1; // Warning
        return 0; // OK
    }

    // Helper methods to categorize file types
    private boolean isDocumentType(String mimeType) {
        return mimeType.contains("pdf") ||
               mimeType.contains("document") ||
               mimeType.contains("text") ||
               mimeType.contains("spreadsheet") ||
               mimeType.contains("presentation") ||
               mimeType.contains("msword") ||
               mimeType.contains("excel") ||
               mimeType.contains("powerpoint");
    }

    private boolean isImageType(String mimeType) {
        return mimeType.startsWith("image/");
    }

    private boolean isVideoType(String mimeType) {
        return mimeType.startsWith("video/");
    }

    private boolean isAudioType(String mimeType) {
        return mimeType.startsWith("audio/");
    }

    /**
     * Inner class to hold storage usage statistics
     */
    public static class StorageUsageStats {
        private final long totalUsedBytes;
        private final long storageLimitBytes;
        private final long documentBytes;
        private final long imageBytes;
        private final long videoBytes;
        private final long audioBytes;
        private final long otherBytes;
        private final int totalFiles;
        private final int sharedFiles;

        public StorageUsageStats(long totalUsedBytes, long storageLimitBytes, 
                               long documentBytes, long imageBytes, long videoBytes, 
                               long audioBytes, long otherBytes, int totalFiles, int sharedFiles) {
            this.totalUsedBytes = totalUsedBytes;
            this.storageLimitBytes = storageLimitBytes;
            this.documentBytes = documentBytes;
            this.imageBytes = imageBytes;
            this.videoBytes = videoBytes;
            this.audioBytes = audioBytes;
            this.otherBytes = otherBytes;
            this.totalFiles = totalFiles;
            this.sharedFiles = sharedFiles;
        }

        // Getters
        public long getTotalUsedBytes() { return totalUsedBytes; }
        public long getStorageLimitBytes() { return storageLimitBytes; }
        public long getDocumentBytes() { return documentBytes; }
        public long getImageBytes() { return imageBytes; }
        public long getVideoBytes() { return videoBytes; }
        public long getAudioBytes() { return audioBytes; }
        public long getOtherBytes() { return otherBytes; }
        public int getTotalFiles() { return totalFiles; }
        public int getSharedFiles() { return sharedFiles; }

        public double getUsagePercentage() {
            return (double) totalUsedBytes / storageLimitBytes * 100;
        }

        public long getRemainingBytes() {
            return storageLimitBytes - totalUsedBytes;
        }
    }
}
