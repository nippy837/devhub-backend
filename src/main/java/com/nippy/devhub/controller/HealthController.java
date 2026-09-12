package com.nippy.devhub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> healthCheck(){
        Map<String, String> res = new HashMap<>();
        res.put("status", "ok");
        return res;
    }
}
