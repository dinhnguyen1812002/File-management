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


    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("file", new File());
        return "upload";
    }
    
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "folderId", required = false) Long folderId,
                                   @AuthenticationPrincipal UserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {

        User uploader = userRepository.findByUsername(currentUser.getUsername());
        String fileName = fileStorageService.storeFile(file);

        File newFile = new File();
        newFile.setFileName(fileName);
        newFile.setFileType(file.getContentType());
        newFile.setFileSize(file.getSize());
        newFile.setFilePath("./uploads/" + fileName);
        newFile.setUploader(uploader);

        if (folderId != null) {
            Folder parentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            if (!parentFolder.getUser().equals(uploader)) {
                throw new SecurityException("User does not have permission to upload to this folder.");
            }
            newFile.setFolder(parentFolder);
        }

        fileRepository.save(newFile);

        redirectAttributes.addFlashAttribute("message", "File uploaded successfully!");
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
                                @AuthenticationPrincipal UserDetails currentUser) {
        String username = currentUser.getUsername();
        User user = userRepository.findByUsername(username);

        Folder currentFolder = null;

        if (folderId != null) {
            currentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            if (!currentFolder.getUser().equals(user)) {
                throw new SecurityException("User does not have permission to access this folder.");
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
                               RedirectAttributes redirectAttributes) {
        folderService.createFolder(folderName, parentId, currentUser.getUsername());
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
   
}
