package com.app.file_transfer.services;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    // Get all files uploaded by a specific user
    public List<File> getFilesUploadedByUser(String username) {
        User user = userRepository.findByUsername(username);
        return fileRepository.findByUploader(user);
    }

    // Get all files shared with a specific user
    public List<File> getFilesReceivedByUser(String username) {
        User user = userRepository.findByUsername(username);
        return fileRepository.findByRecipientsContains(user);
    }
}
