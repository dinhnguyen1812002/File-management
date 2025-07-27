package com.app.file_transfer.services;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
@Service
public class FileStorageService {
    private final Path fileStorageLocation;


    public FileStorageService() {
        this.fileStorageLocation = Paths.get("./uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
    String originalFileName = file.getOriginalFilename();

    // Tách tên file và phần mở rộng
    String fileName = "";
    String extension = "";
    int dotIndex = originalFileName.lastIndexOf('.');
    if (dotIndex > 0) {
        fileName = originalFileName.substring(0, dotIndex);
        extension = originalFileName.substring(dotIndex);
    } else {
        fileName = originalFileName;
        extension = "";
    }

    String newFileName = originalFileName;
    Path targetLocation = this.fileStorageLocation.resolve(newFileName);
    int count = 0;

    // Kiểm tra file đã tồn tại, nếu có thì thêm (count)
    while (Files.exists(targetLocation)) {
        count++;
        newFileName = fileName + "(" + count + ")" + extension;
        targetLocation = this.fileStorageLocation.resolve(newFileName);
    }

    try {
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return newFileName;
    } catch (IOException ex) {
        throw new RuntimeException("Could not store file " + newFileName + ". Please try again!", ex);
    }
}


    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }

    public MediaType getMediaTypeForFileName(String fileName) {
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        // Map common file extensions to MediaType
        switch (extension) {
            // Documents
            case "pdf":
                return MediaType.APPLICATION_PDF;
            case "doc":
            case "docx":
                return MediaType.parseMediaType("application/msword");
            case "xls":
            case "xlsx":
                return MediaType.parseMediaType("application/vnd.ms-excel");
            case "ppt":
            case "pptx":
                return MediaType.parseMediaType("application/vnd.ms-powerpoint");

            // Images
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;

            // Videos
            case "mp4":
                return MediaType.parseMediaType("video/mp4");
            case "avi":
                return MediaType.parseMediaType("video/x-msvideo");
            case "wmv":
                return MediaType.parseMediaType("video/x-ms-wmv");
            case "flv":
                return MediaType.parseMediaType("video/x-flv");
            case "webm":
                return MediaType.parseMediaType("video/webm");

            // Audio
            case "mp3":
                return MediaType.parseMediaType("audio/mpeg");
            case "wav":
                return MediaType.parseMediaType("audio/wav");
            case "ogg":
                return MediaType.parseMediaType("audio/ogg");

            // Default
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public boolean isPreviewableDocument(String fileName) {
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        return extension.equals("pdf") || 
               extension.equals("doc") || 
               extension.equals("docx") || 
               extension.equals("xls") || 
               extension.equals("xlsx") || 
               extension.equals("ppt") || 
               extension.equals("pptx");
    }

     public boolean isImage(String fileName) {
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        return extension.equals("jpg") || 
               extension.equals("jpeg") || 
               extension.equals("png") || 
               extension.equals("gif") || 
               extension.equals("svg") ;
              
    }
    public boolean isStreamableVideo(String fileName) {
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        return extension.equals("mp4") || 
               extension.equals("webm") || 
               extension.equals("ogg");
    }
}
