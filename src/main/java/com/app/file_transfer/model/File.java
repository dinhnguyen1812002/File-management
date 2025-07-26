package com.app.file_transfer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;
    private String fileType;
    private long fileSize;
    private String password; // Thêm trường mật khẩu

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @ManyToMany(mappedBy = "receivedFiles", cascade = CascadeType.PERSIST)
    private List<User> recipients = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder folder;

    // Constructors, getters, setters
}
