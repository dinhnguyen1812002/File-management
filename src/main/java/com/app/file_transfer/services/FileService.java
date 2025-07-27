package com.app.file_transfer.services;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

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

    // Set or update password for a file
    public void setFilePassword(Long fileId, String password, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to set password for this file.");
        }

        file.setPassword(passwordEncoder.encode(password));
        fileRepository.save(file);
    }

    // Verify file password
    public boolean verifyFilePassword(Long fileId, String password) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (file.getPassword() == null || file.getPassword().isEmpty()) {
            return true; // No password set
        }

        return passwordEncoder.matches(password, file.getPassword());
    }

    // Share a file with multiple users
    public void shareFile(Long fileId, List<String> recipientUsernames, String senderUsername) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User sender = userRepository.findByUsername(senderUsername);

        if (!file.getUploader().equals(sender)) {
            throw new SecurityException("User does not have permission to share this file.");
        }

        for (String username : recipientUsernames) {
            User recipient = userRepository.findByUsername(username);
            if (recipient != null && !file.getRecipients().contains(recipient)) {
                file.getRecipients().add(recipient);
                recipient.getReceivedFiles().add(file);
                userRepository.save(recipient);
            }
        }
        fileRepository.save(file);
    }

    // Delete a file
    public void deleteFile(Long fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        User user = userRepository.findByUsername(username);

        // Check if user has permission to delete this file
        if (!file.getUploader().equals(user)) {
            throw new SecurityException("User does not have permission to delete this file.");
        }

        // Remove file from recipients
        for (User recipient : file.getRecipients()) {
            recipient.getReceivedFiles().remove(file);
            userRepository.save(recipient);
        }

        // Remove file from folder
        if (file.getFolder() != null) {
            file.getFolder().getFiles().remove(file);
        }

        // Delete file from database
        fileRepository.delete(file);
    }

    // Delete multiple files
    @Transactional(rollbackOn = Exception.class)
    public void deleteFiles(List<Long> fileIds, String username) {
        User user = userRepository.findByUsername(username);
        List<String> errors = new ArrayList<>();

        for (Long fileId : fileIds) {
            try {
                File file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
                String fileName = file.getFileName();
                deleteFile(fileId, username);
                // Ensure physical deletion
               fileStorageService.deleteFile(fileName);
            
            } catch (Exception e) {
                errors.add("Error deleting file " + fileId + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Some files could not be deleted: " + String.join("; ", errors));
        }
    }
}
