package com.app.file_transfer.controller.api;

import com.app.file_transfer.model.File;
import com.app.file_transfer.services.FileService;
import com.app.file_transfer.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class FileStorageController {

    @Autowired
    private FileStorageService fileStorageService;

    public ResponseEntity<?> uploadFile(MultipartFile file){

        String uploadFile= fileStorageService.storeFile(file);


        return ResponseEntity.ok().build();
    }


}
