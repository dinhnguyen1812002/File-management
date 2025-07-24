package com.app.file_transfer.repository;

import com.app.file_transfer.model.File;
import com.app.file_transfer.model.User;

import com.app.file_transfer.model.Folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    // Find all files uploaded by a specific user
    List<File> findByUploader(User uploader);

    List<File> findByRecipientsContains(User user);

    List<File> findByUploaderAndFolderIsNull(User uploader);

    List<File> findByUploaderAndFolder(User uploader, Folder folder);
}