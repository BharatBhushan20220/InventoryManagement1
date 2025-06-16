package com.example.InventoryManagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @GetMapping("/home")
    public String welCome(){
        return "Welcome to Inventory management Project !!";
    }
}
