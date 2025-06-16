package com.example.InventoryManagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControllerIM {

    @GetMapping("/home")
    public String sayHello(){
        return "Welcome to Inventory Project !!";
    }
}
