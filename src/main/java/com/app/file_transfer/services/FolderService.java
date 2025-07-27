package com.app.file_transfer.services;

import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FolderService {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Folder createFolder(String name, Long parentId, String username, String password) {
        User user = userRepository.findByUsername(username);
        Folder parentFolder = null;
        if (parentId != null) {
            parentFolder = folderRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
            // Ensure the user owns the parent folder
            if (!parentFolder.getUser().equals(user)) {
                throw new SecurityException("User does not have permission to create a folder here.");
            }
        }

        Folder newFolder = new Folder();
        newFolder.setName(name);
        newFolder.setUser(user);
        newFolder.setPassword(passwordEncoder.encode(password));
        newFolder.setParent(parentFolder);
        return folderRepository.save(newFolder);
    }

    public List<Folder> getSubFolders(Long parentId, String username) {
        User user = userRepository.findByUsername(username);
        Folder parentFolder = (parentId == null) ? null : folderRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));

        if (parentId != null && !parentFolder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to view this folder.");
        }

        return folderRepository.findByUserAndParent(user, parentFolder);
    }

    // Set or update password for a folder
    public void setFolderPassword(Long folderId, String password, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to set password for this folder.");
        }

        folder.setPassword(passwordEncoder.encode(password));
        folderRepository.save(folder);
    }

    // Verify folder password
    public boolean verifyFolderPassword(Long folderId, String password) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (folder.getPassword() == null || folder.getPassword().isEmpty()) {
            return true; // No password set
        }

        return passwordEncoder.matches(password, folder.getPassword());
    }

    // Delete a folder
    public void deleteFolder(Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findByUsername(username);

        // Check if user has permission to delete this folder
        if (!folder.getUser().equals(user)) {
            throw new SecurityException("User does not have permission to delete this folder.");
        }

        // Delete folder from database
        // Note: Due to cascade settings, this will also delete all subfolders and files
        folderRepository.delete(folder);
    }

    // Delete multiple folders
    public void deleteFolders(List<Long> folderIds, String username) {
        User user = userRepository.findByUsername(username);

        for (Long folderId : folderIds) {
            try {
                deleteFolder(folderId, username);
            } catch (Exception e) {
                // Log error but continue with other folders
                System.err.println("Error deleting folder " + folderId + ": " + e.getMessage());
            }
        }
    }
}
