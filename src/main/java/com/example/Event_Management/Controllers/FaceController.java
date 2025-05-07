//package com.example.Event_Management.Controllers;
//
//import com.example.Event_Management.services.FaceService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@Controller
//public class FaceController {
//    @Autowired
//    private FaceService faceService;
//
//    @GetMapping("/index2")
//    public String index() {
//        return "index2";
//    }
//
//    @PostMapping("/store")
//    public String storeFace(@RequestBody Map<String, String> data) {
//        String base64 = data.get("image");
//        String name = data.get("name");
//        faceService.storeFace(base64, name);
//        return "Stored";
//    }
//
//    @PostMapping("/recognize")
//    public String recognizeFace(@RequestBody Map<String, String> data) {
//        String base64 = data.get("image");
//        return faceService.recognizeFace(base64);
//    }
//}