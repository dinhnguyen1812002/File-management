package com.app.file_transfer.services;

import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FileStorageService fileStorageService;

    private final Path avatarStorageLocation;
    
    public UserService() {
        this.avatarStorageLocation = Paths.get("./uploads/avatars").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.avatarStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the avatar files will be stored.", ex);
        }
    }


    public User registerNewUser(String username, String rawPassword, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword)); // Encode password
        user.setEmail(email);
        return userRepository.save(user);
    }
    public boolean isUsernameTaken(String username) {
        return userRepository.findByUsername(username) != null;
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User updateUserAvatar(String username, MultipartFile avatarFile) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        try {
            // Generate a unique filename for the avatar
            String originalFilename = avatarFile.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String avatarFilename = "avatar_" + UUID.randomUUID().toString() + fileExtension;
            
            // Save the avatar file
            Path targetLocation = this.avatarStorageLocation.resolve(avatarFilename);
            Files.copy(avatarFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Update the user's avatar field
            user.setAvatar(avatarFilename);
            return userRepository.save(user);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store avatar file. Please try again!", ex);
        }
    }
    
    public User updateUserProfile(String username, String email, String bio) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        user.setEmail(email);
        user.setBio(bio);
        return userRepository.save(user);
    }
}
