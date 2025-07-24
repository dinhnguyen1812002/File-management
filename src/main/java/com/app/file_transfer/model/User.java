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
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String avatar;
    private String email;
    private String bio;
    @OneToMany(mappedBy = "uploader")
    private List<File> uploadedFiles = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "file_recipients",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "file_id")
    )
    private List<File> receivedFiles = new ArrayList<>();
    // Getters, Setters, and Constructors


}
