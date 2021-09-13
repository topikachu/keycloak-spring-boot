package com.example.keycloaksampleapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FooController {

    @GetMapping("/admin/realms/{realm}/{resource}")
    public ResponseEntity foo(@PathVariable String realm, @PathVariable String resource) {
        return ResponseEntity.ok().build();
    }
}
