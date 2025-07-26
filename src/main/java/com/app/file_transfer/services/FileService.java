package com.app.file_transfer.services;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    
}
