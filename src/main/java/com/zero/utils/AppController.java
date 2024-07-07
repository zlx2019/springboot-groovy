package com.zero.utils;

import com.zero.ScriptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zero.
 * @date 2024/7/7 20:48
 */
@RestController
@RequestMapping("/test")
public class AppController {

    @Autowired
    private ScriptTemplate scriptTemplate;

    @GetMapping("/mem")
    public Object mem(){
        for (int i = 0; i < 100; i++) {
            Object call = scriptTemplate.call("testMem", "foo");
        }
        return null;
    }
}
