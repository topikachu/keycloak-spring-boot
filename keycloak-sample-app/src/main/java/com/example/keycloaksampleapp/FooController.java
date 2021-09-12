package com.example.keycloaksampleapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
public class FooController {

    @GetMapping("/foo")
    public ResponseEntity foo(){
        return ResponseEntity.ok().build();
    }
}
