package com.app.file_transfer.services;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
@Service
public class FileStorageService {
    private final Path fileStorageLocation;

    @Autowired
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
}
