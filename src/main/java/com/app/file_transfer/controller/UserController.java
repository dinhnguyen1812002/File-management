package com.app.file_transfer.controller;

import com.app.file_transfer.model.User;
import com.app.file_transfer.services.FileStorageService;
import com.app.file_transfer.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Paths;

@Controller
@RequestMapping
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FileStorageService fileStorageService;
@GetMapping("/login")
    public String Login(){
    return "login";
}
    @GetMapping("/register")
    public String register(){
        return "register";
    }
    @PostMapping("/register")
    public String registerUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("email") String email,
            Model model) {

        // Check if the username already exists
        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken.");
            return "register";
        }

        // Register the new user
        userService.registerNewUser(username, password, email);
        model.addAttribute("success", "User registered successfully!");

        return "redirect:/"; // Stay on the same page after registration
    }
    
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        if(currentUser == null ){
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(currentUser.getUsername());
        model.addAttribute("user", user);
        return "profile";
    }
    
    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam("email") String email,
            @RequestParam("bio")  String bio,
            RedirectAttributes redirectAttributes) {
        
        userService.updateUserProfile(currentUser.getUsername(), email, bio);
        redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
        return "redirect:/profile";
    }
    
    @PostMapping("/profile/avatar")
    public String updateAvatar(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam("avatar") MultipartFile avatarFile,
            RedirectAttributes redirectAttributes) {
        
        if (avatarFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/profile";
        }
        
        userService.updateUserAvatar(currentUser.getUsername(), avatarFile);
        redirectAttributes.addFlashAttribute("message", "Avatar updated successfully!");
        return "redirect:/profile";
    }
    
    @GetMapping("/avatars/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFileAsResource(Paths.get("avatars", filename).toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
