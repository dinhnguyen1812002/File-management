package com.app.file_transfer.services;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class PreviewService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Check if a file can be previewed
     */
    public boolean isPreviewable(String fileName) {
        return fileStorageService.isImage(fileName) ||
               fileStorageService.isPreviewableDocument(fileName) ||
               fileStorageService.isStreamableVideo(fileName);
    }

    /**
     * Get preview type for a file
     */
    public PreviewType getPreviewType(String fileName) {
        if (fileStorageService.isImage(fileName)) {
            return PreviewType.IMAGE;
        } else if (isPdfFile(fileName)) {
            return PreviewType.PDF;
        } else if (fileStorageService.isStreamableVideo(fileName)) {
            return PreviewType.VIDEO;
        } else if (fileStorageService.isPreviewableDocument(fileName)) {
            return PreviewType.DOCUMENT;
        }
        return PreviewType.UNSUPPORTED;
    }

    /**
     * Get preview information for a file
     */
    public PreviewInfo getPreviewInfo(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        User user = userRepository.findByUsername(username);
        
        // Check permissions
        if (!hasPreviewPermission(file, user)) {
            throw new SecurityException("You don't have permission to preview this file");
        }

        PreviewType previewType = getPreviewType(file.getFileName());
        
        if (previewType == PreviewType.UNSUPPORTED) {
            throw new IllegalArgumentException("This file type cannot be previewed");
        }

        return new PreviewInfo(
                file.getId(),
                file.getFileName(),
                file.getFileType(),
                file.getFileSize(),
                previewType,
                getPreviewUrl(fileId, previewType),
                getThumbnailUrl(fileId, previewType)
        );
    }

    /**
     * Generate preview response for a file
     */
    public ResponseEntity<Resource> generatePreviewResponse(Long fileId, String username) {
        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));

            User user = userRepository.findByUsername(username);
            
            if (!hasPreviewPermission(file, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!isPreviewable(file.getFileName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Resource resource = fileStorageService.loadFileAsResource(file.getFileName());
            MediaType mediaType = fileStorageService.getMediaTypeForFileName(file.getFileName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);

            // Properly encode filename for Content-Disposition header to handle Unicode characters
            String encodedFileName = encodeFilenameForHeader(file.getFileName());
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename*=UTF-8''" + encodedFileName);

            // Add cache headers for better performance
            headers.setCacheControl("public, max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file metadata for preview
     */
    public Map<String, Object> getFileMetadata(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        User user = userRepository.findByUsername(username);
        
        if (!hasPreviewPermission(file, user)) {
            throw new SecurityException("You don't have permission to access this file");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", file.getId());
        metadata.put("fileName", file.getFileName());
        metadata.put("fileType", file.getFileType());
        metadata.put("fileSize", file.getFileSize());
        metadata.put("fileSizeFormatted", formatFileSize(file.getFileSize()));
        metadata.put("previewType", getPreviewType(file.getFileName()).toString().toLowerCase());
        metadata.put("isPreviewable", isPreviewable(file.getFileName()));
        metadata.put("uploader", file.getUploader().getUsername());


        return metadata;
    }

    // Helper methods
    private boolean hasPreviewPermission(File file, User user) {
        return file.getUploader().equals(user) || file.getRecipients().contains(user);
    }

    private boolean isPdfFile(String fileName) {
        String extension = getFileExtension(fileName);
        return "pdf".equalsIgnoreCase(extension);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    private String getPreviewUrl(Long fileId, PreviewType previewType) {
        switch (previewType) {
            case VIDEO:
                return "/files/stream/" + fileId;
            case IMAGE:
            case PDF:
            case DOCUMENT:
                return "/files/preview/" + fileId;
            default:
                return null;
        }
    }

    private String getThumbnailUrl(Long fileId, PreviewType previewType) {
        // For now, return null. Can be implemented later for thumbnail generation
        return null;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Encode filename for HTTP Content-Disposition header to handle Unicode characters
     */
    public static String encodeFilenameForHeader(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
        } catch (Exception e) {
            // Fallback to original filename if encoding fails
            return filename.replaceAll("[^\\x00-\\x7F]", "_");
        }
    }

    // Enums and inner classes
    public enum PreviewType {
        IMAGE, PDF, VIDEO, DOCUMENT, UNSUPPORTED
    }

    public static class PreviewInfo {
        private final Long fileId;
        private final String fileName;
        private final String fileType;
        private final long fileSize;
        private final PreviewType previewType;
        private final String previewUrl;
        private final String thumbnailUrl;

        public PreviewInfo(Long fileId, String fileName, String fileType, long fileSize, 
                          PreviewType previewType, String previewUrl, String thumbnailUrl) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.previewType = previewType;
            this.previewUrl = previewUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        // Getters
        public Long getFileId() { return fileId; }
        public String getFileName() { return fileName; }
        public String getFileType() { return fileType; }
        public long getFileSize() { return fileSize; }
        public PreviewType getPreviewType() { return previewType; }
        public String getPreviewUrl() { return previewUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
    }
}
