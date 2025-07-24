package com.app.file_transfer.services;

import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Folder createFolder(String name, Long parentId, String username) {
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
}