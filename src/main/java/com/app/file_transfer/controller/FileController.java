package com.app.file_transfer.controller;


import com.app.file_transfer.model.File;
import com.app.file_transfer.model.Folder;
import com.app.file_transfer.model.User;
import com.app.file_transfer.repository.FileRepository;
import com.app.file_transfer.repository.FolderRepository;
import com.app.file_transfer.repository.UserRepository;
import com.app.file_transfer.services.FileService;
import com.app.file_transfer.services.FileStorageService;
import com.app.file_transfer.services.FolderService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpSession;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.UrlResource;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private FileService fileService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("file", new File());
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile[] files,
                                   @RequestParam(value = "folderId", required = false) Long folderId,
                                   @AuthenticationPrincipal UserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {

        User uploader = userRepository.findByUsername(currentUser.getUsername());
        Folder parentFolder = null;

        if (folderId != null) {
            parentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            if (!parentFolder.getUser().equals(uploader)) {
                throw new SecurityException("User does not have permission to upload to this folder.");
            }
        }

        int successCount = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String fileName = fileStorageService.storeFile(file);

            File newFile = new File();
            newFile.setFileName(fileName);
            newFile.setFileType(file.getContentType());
            newFile.setFileSize(file.getSize());
            newFile.setFilePath("./uploads/" + fileName);
            newFile.setUploader(uploader);

            if (parentFolder != null) {
                newFile.setFolder(parentFolder);
            }

            fileRepository.save(newFile);
            successCount++;
        }

        if (successCount > 0) {
            redirectAttributes.addFlashAttribute("message", successCount + " file(s) uploaded successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "No files were uploaded. Please select at least one file.");
        }

        return "redirect:/files/dashboard" + (folderId != null ? "?folderId=" + folderId : "");
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByUsername(currentUser.getUsername());
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getRecipients().contains(user) && !file.getUploader().equals(user)) {
            throw new RuntimeException("You are not authorized to download this file.");
        }

        Resource resource = fileStorageService.loadFileAsResource(file.getFileName());

        // Encode the filename in RFC 5987 to handle special characters
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(
                    Objects.requireNonNull(resource.getFilename()),
                            StandardCharsets.UTF_8)
                    .replace("+", "%20"); // Replace spaces encoded as "+" with "%20"
        } catch (Exception e) {
            throw new RuntimeException("Error encoding file name.", e);
        }

        // Set Content-Disposition header with encoded filename
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }


    @GetMapping("/send/{fileId}")
    public String showSendFileForm(@PathVariable Long fileId, Model model ,
                                   @AuthenticationPrincipal UserDetails currentUser) {
        User user= userRepository.findByUsername(currentUser.getUsername());
        File file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("File not found"));
        if (!file.getUploader().equals(user)){
            return "404";
        }
        model.addAttribute("file", file);
        model.addAttribute("users", userRepository.findAll());
        return "sendFile";  // Returns the Thymeleaf template for sending files
    }

    // Process the sending of a file to another user
    @PostMapping("/send/{fileId}")
    public String sendFile(@PathVariable Long fileId,
                           @RequestParam Long recipientId,
                           RedirectAttributes redirectAttributes,
                           @AuthenticationPrincipal UserDetails currentUser) {
        // Fetch the current logged-in user
        User user = userRepository.findByUsername(currentUser.getUsername());

        // Fetch the file and check ownership
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        // Check if the current user is the owner of the file
        if (!file.getUploader().equals(user)) {
            // If the user is not the owner, throw a 404 exception
            return "404";
        }

        // Fetch the recipient from the database
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Add the file to the recipient's received files list and vice versa
        file.getRecipients().add(recipient);
        recipient.getReceivedFiles().add(file);

        // Save the updated user and file to the database
        userRepository.save(recipient);
        fileRepository.save(file);

        // Add a success message to the redirect attributes
        redirectAttributes.addFlashAttribute("message", "File sent successfully to " + recipient.getUsername());

        return "redirect:/files/dashboard";  // Redirect back to the dashboard page
    }

    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(value = "folderId", required = false) Long folderId,
                                Model model,
                                @AuthenticationPrincipal UserDetails currentUser,
                                HttpSession session) {
        String username = currentUser.getUsername();
        User user = userRepository.findByUsername(username);

        Folder currentFolder = null;

        if (folderId != null) {
            currentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            if (!currentFolder.getUser().equals(user)) {
                throw new SecurityException("User does not have permission to access this folder.");
            }

            // Check if folder is password protected
            if (currentFolder.getPassword() != null && !currentFolder.getPassword().isEmpty()) {
                // Check if user has already entered the correct password
                String unlockStatus = (String) session.getAttribute("folder_" + folderId + "_unlocked");

                if (!"true".equals(unlockStatus)) {
                    // User hasn't entered password yet, show password form
                    model.addAttribute("passwordProtectedFolder", currentFolder);
                    model.addAttribute("parentFolder", currentFolder.getParent());
                    return "folder_password";
                }
            }
        }

        List<Folder> subFolders = folderService.getSubFolders(folderId, username);
        List<File> files = (currentFolder == null)
                ? fileRepository.findByUploaderAndFolderIsNull(user)
                : fileRepository.findByUploaderAndFolder(user, currentFolder);


        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("subFolders", subFolders);
        model.addAttribute("files", files);

        return "dashboard";
    }

    @PostMapping("/folders/create")
    public String createFolder(@RequestParam String folderName,
                               @RequestParam(value = "parentId", required = false) Long parentId,
                               @AuthenticationPrincipal UserDetails currentUser,
                               @RequestParam(value = "password", required = false) String password,
                               RedirectAttributes redirectAttributes) {
        folderService.createFolder(folderName, parentId, currentUser.getUsername(),password);
        redirectAttributes.addFlashAttribute("message", "Folder '" + folderName + "' created successfully.");
        return "redirect:/files/dashboard" + (parentId != null ? "?folderId=" + parentId : "");
    }
    @GetMapping("/list")
    public String listAllFile(@AuthenticationPrincipal UserDetails user, Model model){
        List<File> uploadedFiles = fileService.getFilesUploadedByUser(user.getUsername());

        List<File> receivedFiles = fileService.getFilesReceivedByUser(user.getUsername());

        model.addAttribute("uploadedFiles",uploadedFiles );

        model.addAttribute("receivedFiles",receivedFiles );

        return "fileList";
    }

    @PostMapping("/download-multiple")
    public ResponseEntity<Resource> downloadMultipleFiles(@RequestParam("fileIds") List<Long> fileIds,
                                                         @AuthenticationPrincipal UserDetails currentUser) throws IOException {
        User user = userRepository.findByUsername(currentUser.getUsername());

        if (fileIds == null || fileIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files selected for download");
        }

        // If only one file is selected, redirect to the single file download endpoint
        if (fileIds.size() == 1) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/files/download/" + fileIds.get(0))
                    .build();
        }

        // Create a temporary zip file
        Path tempFile = Files.createTempFile("download_", ".zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempFile.toFile()))) {
            for (Long fileId : fileIds) {
                File file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId));

                // Check if user has permission to download this file
                if (!file.getRecipients().contains(user) && !file.getUploader().equals(user)) {
                    continue; // Skip files the user doesn't have permission to download
                }

                Resource resource = fileStorageService.loadFileAsResource(file.getFileName());

                // Add file to zip
                ZipEntry zipEntry = new ZipEntry(file.getFileName());
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = Files.readAllBytes(resource.getFile().toPath());
                zipOut.write(bytes, 0, bytes.length);
                zipOut.closeEntry();
            }
        }

        // Create a resource from the zip file
        Resource zipResource = new UrlResource(tempFile.toUri());

        // Set up the response
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"files.zip\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .body(zipResource);
    }

    @PostMapping("/folders/password")
    public String setFolderPassword(@RequestParam Long folderId,
                                   @RequestParam String password,
                                   @AuthenticationPrincipal UserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {
        try {
            folderService.setFolderPassword(folderId, password, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("message", "Folder password set successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        return "redirect:/files/dashboard" + (parentId != null ? "?folderId=" + parentId : "");
    }

    @PostMapping("/folders/verify-password")
    public String verifyFolderPassword(@RequestParam Long folderId,
                                      @RequestParam String password,
                                      RedirectAttributes redirectAttributes,
                                      HttpSession session) {
        boolean isValid = folderService.verifyFolderPassword(folderId, password);

        if (isValid) {
            // Store in session that this folder has been unlocked
            session.setAttribute("folder_" + folderId + "_unlocked", "true");
            return "redirect:/files/dashboard?folderId=" + folderId;
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid password");
            return "redirect:/files/dashboard";
        }
    }

    @PostMapping("/folders/verify-password-ajax")
    @ResponseBody
    public ResponseEntity<?> verifyFolderPasswordAjax(@RequestParam Long folderId,
                                                     @RequestParam String password,
                                                     HttpSession session) {
        boolean isValid = folderService.verifyFolderPassword(folderId, password);

        if (isValid) {
            // Store in session that this folder has been unlocked
            session.setAttribute("folder_" + folderId + "_unlocked", "true");
            return ResponseEntity.ok().body(Map.of("success", true, "redirectUrl", "/files/dashboard?folderId=" + folderId));
        } else {
            return ResponseEntity.ok().body(Map.of("success", false, "error", "Invalid password"));
        }
    }

}
