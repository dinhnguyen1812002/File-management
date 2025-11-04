package com.app.file_transfer.controller;

import com.app.file_transfer.model.User;
import com.app.file_transfer.services.FileStorageService;
import com.app.file_transfer.services.PreviewService;
import com.app.file_transfer.services.StorageUsageService;
import com.app.file_transfer.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StorageUsageService storageUsageService;
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

        // Get storage usage statistics
        StorageUsageService.StorageUsageStats storageStats = storageUsageService.getStorageUsageStats(currentUser.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("storageStats", storageStats);
        model.addAttribute("storageUsagePercentage", storageStats.getUsagePercentage());
        model.addAttribute("totalUsedFormatted", storageUsageService.formatBytes(storageStats.getTotalUsedBytes()));
        model.addAttribute("storageLimitFormatted", storageUsageService.formatBytes(storageStats.getStorageLimitBytes()));
        model.addAttribute("documentsFormatted", storageUsageService.formatBytes(storageStats.getDocumentBytes()));
        model.addAttribute("imagesFormatted", storageUsageService.formatBytes(storageStats.getImageBytes()));
        model.addAttribute("videosFormatted", storageUsageService.formatBytes(storageStats.getVideoBytes()));
        model.addAttribute("audioFormatted", storageUsageService.formatBytes(storageStats.getAudioBytes()));
        model.addAttribute("othersFormatted", storageUsageService.formatBytes(storageStats.getOtherBytes()));

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

        // Properly encode filename for Content-Disposition header to handle Unicode characters
        String encodedFileName = PreviewService.encodeFilenameForHeader(resource.getFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @GetMapping("/api/storage-usage")
    @ResponseBody
    public ResponseEntity<?> getStorageUsageApi(@AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StorageUsageService.StorageUsageStats stats = storageUsageService.getStorageUsageStats(currentUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("totalUsedBytes", stats.getTotalUsedBytes());
            response.put("storageLimitBytes", stats.getStorageLimitBytes());
            response.put("usagePercentage", stats.getUsagePercentage());
            response.put("totalUsedFormatted", storageUsageService.formatBytes(stats.getTotalUsedBytes()));
            response.put("storageLimitFormatted", storageUsageService.formatBytes(stats.getStorageLimitBytes()));
            response.put("remainingFormatted", storageUsageService.formatBytes(stats.getRemainingBytes()));
            response.put("totalFiles", stats.getTotalFiles());
            response.put("sharedFiles", stats.getSharedFiles());

            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("documents", Map.of("bytes", stats.getDocumentBytes(), "formatted", storageUsageService.formatBytes(stats.getDocumentBytes())));
            breakdown.put("images", Map.of("bytes", stats.getImageBytes(), "formatted", storageUsageService.formatBytes(stats.getImageBytes())));
            breakdown.put("videos", Map.of("bytes", stats.getVideoBytes(), "formatted", storageUsageService.formatBytes(stats.getVideoBytes())));
            breakdown.put("audio", Map.of("bytes", stats.getAudioBytes(), "formatted", storageUsageService.formatBytes(stats.getAudioBytes())));
            breakdown.put("others", Map.of("bytes", stats.getOtherBytes(), "formatted", storageUsageService.formatBytes(stats.getOtherBytes())));

            response.put("breakdown", breakdown);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get storage usage: " + e.getMessage()));
        }
    }
}
