package com.linkCondenser.LinkCondenser.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class init {

    @GetMapping("/main")
    public String postTest() {
        return "POST Working";
    }
}