package com.tradelab.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoPageController {

    @GetMapping("/")
    public String demoPage() {
        return "forward:/index.html";
    }
}
