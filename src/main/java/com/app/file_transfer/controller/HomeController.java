package com.app.file_transfer.controller;



import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.file_transfer.services.UserDetailsImpl;

@Controller
@RequestMapping("/")
public class HomeController {
    // @GetMapping
    // public String home(){
    //     return "home";
    // }


   @GetMapping("/")
public String home(Model model, @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails instanceof UserDetailsImpl user) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("avatar", user.getAvatar());
    }
    return "home";
}

    @GetMapping("/upload")
    public String uploadForm(){
        return "upload";
    }
}
